package com.myexpenses.app.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 100% Offline On-Device Arabic Speech-to-Text Engine powered by Sherpa-ONNX
 * running NVIDIA NeMo FastConformer Arabic (Gulf/MSA int8 RNN-T).
 *
 * Zero internet access required. Runs entirely locally on CPU (arm64-v8a / armeabi-v7a).
 */
class SherpaOfflineArabicEngine(context: Context) {

    private val appContext = context.applicationContext
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initMutex = Mutex()

    @Volatile
    private var recognizer: OfflineRecognizer? = null

    @Volatile
    private var stopRequested: Boolean = false

    @Volatile
    private var cancelRequested: Boolean = false

    private var recordingJob: Job? = null

    fun modelDirectory(): File {
        val extDir = appContext.getExternalFilesDir(MODEL_DIR_NAME)
        if (extDir != null && hasValidModelFiles(extDir)) return extDir
        val intDir = File(appContext.filesDir, MODEL_DIR_NAME)
        if (hasValidModelFiles(intDir)) return intDir
        return extDir ?: intDir
    }

    fun isModelInstalled(): Boolean {
        val extDir = appContext.getExternalFilesDir(MODEL_DIR_NAME)
        val intDir = File(appContext.filesDir, MODEL_DIR_NAME)
        return (extDir != null && hasValidModelFiles(extDir)) || hasValidModelFiles(intDir)
    }

    private fun hasValidModelFiles(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        val enc = File(dir, FILE_ENCODER)
        val dec = File(dir, FILE_DECODER)
        val join = File(dir, FILE_JOINER)
        val tok = File(dir, FILE_TOKENS)
        return enc.isFile && enc.length() > 10_000_000L &&
            dec.isFile && dec.length() > 100_000L &&
            join.isFile && join.length() > 100_000L &&
            tok.isFile && tok.length() > 100L
    }

    /**
     * Imports the offline Arabic FastConformer ONNX model files directly from a user-selected .zip archive
     * (e.g. MyExpenses-Offline-Arabic-Model.zip), enabling 100% offline Arabic without ADB.
     */
    suspend fun importModelFromZipUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val expectedFiles = setOf(FILE_ENCODER, FILE_DECODER, FILE_JOINER, FILE_TOKENS)
            val targetDir = (appContext.getExternalFilesDir(MODEL_DIR_NAME) ?: File(appContext.filesDir, MODEL_DIR_NAME)).apply {
                if (!exists()) mkdirs()
            }

            val inputStream = appContext.contentResolver.openInputStream(uri)
                ?: error("Unable to open selected archive")

            var extractedCount = 0
            val buffer = ByteArray(64 * 1024)
            ZipInputStream(inputStream.buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val fileName = File(entry.name).name
                        if (fileName in expectedFiles) {
                            val outFile = File(targetDir, fileName)
                            val tmpFile = File(targetDir, "$fileName.tmp")
                            FileOutputStream(tmpFile).use { fos ->
                                while (true) {
                                    val read = zis.read(buffer)
                                    if (read <= 0) break
                                    fos.write(buffer, 0, read)
                                }
                                fos.flush()
                            }
                            if (outFile.exists()) outFile.delete()
                            if (!tmpFile.renameTo(outFile)) {
                                tmpFile.copyTo(outFile, overwrite = true)
                                tmpFile.delete()
                            }
                            extractedCount++
                            Log.i(TAG, "Imported offline model entry: $fileName (${outFile.length()} bytes)")
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            require(extractedCount == expectedFiles.size && hasValidModelFiles(targetDir)) {
                "Archive does not contain all required Arabic model files ($FILE_ENCODER, $FILE_DECODER, $FILE_JOINER, $FILE_TOKENS)"
            }

            initMutex.withLock {
                cancel()
                runCatching { recognizer?.release() }
                recognizer = null
            }
            warmUpAsync()
        }
    }

    /**
     * Pre-warms the ONNX model asynchronously in the background so QuickVoiceActivity opens instantaneously.
     */
    fun warmUpAsync() {
        if (!isModelInstalled() || recognizer != null) return
        engineScope.launch {
            runCatching { ensureInitialized() }
                .onFailure { Log.e(TAG, "Warm-up failed: ${it.message}", it) }
        }
    }

    private suspend fun ensureInitialized(): OfflineRecognizer {
        recognizer?.let { return it }
        return initMutex.withLock {
            recognizer?.let { return it }
            val dir = modelDirectory()
            require(hasValidModelFiles(dir)) {
                "Offline Arabic ONNX model files missing in ${dir.absolutePath}"
            }

            val startMs = System.currentTimeMillis()
            val numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(
                    sampleRate = SAMPLE_RATE,
                    featureDim = 80
                ),
                modelConfig = OfflineModelConfig(
                    transducer = OfflineTransducerModelConfig(
                        encoder = File(dir, FILE_ENCODER).absolutePath,
                        decoder = File(dir, FILE_DECODER).absolutePath,
                        joiner = File(dir, FILE_JOINER).absolutePath
                    ),
                    tokens = File(dir, FILE_TOKENS).absolutePath,
                    numThreads = numThreads,
                    debug = false,
                    provider = "cpu",
                    modelType = "nemo_transducer"
                ),
                decodingMethod = "greedy_search"
            )

            Log.i(TAG, "Loading Sherpa-ONNX Arabic model from ${dir.absolutePath} (threads=$numThreads)...")
            val loaded = OfflineRecognizer(assetManager = null, config = config)
            recognizer = loaded
            Log.i(TAG, "Sherpa-ONNX Arabic model loaded in ${System.currentTimeMillis() - startMs} ms")
            loaded
        }
    }

    /**
     * Records 16kHz mono PCM audio with adaptive silence auto-stop, then decodes locally with Sherpa-ONNX.
     */
    @SuppressLint("MissingPermission")
    fun startListening(
        onStatusText: (String) -> Unit,
        onRmsLevel: (Float) -> Unit,
        onResult: (String) -> Unit,
        onError: (code: Int, msgAr: String, msgEn: String) -> Unit
    ) {
        cancel()
        stopRequested = false
        cancelRequested = false

        recordingJob = engineScope.launch {
            var audioRecord: AudioRecord? = null
            try {
                // Load model if not already warmed up
                if (recognizer == null) {
                    onStatusText("جاري تجهيز المحرك العربي المحلي...")
                }
                val engine = ensureInitialized()
                if (cancelRequested || !isActive) return@launch

                val minBufBytes = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferBytes = max(minBufBytes * 2, SAMPLE_RATE * 2 / 4)

                audioRecord = createAudioRecord(bufferBytes)
                if (audioRecord == null || audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    onError(
                        -10,
                        "تعذر فتح الميكروفون للتسجيل المحلي.",
                        "Failed to initialize microphone for offline recording."
                    )
                    return@launch
                }

                val chunkSamples = SAMPLE_RATE / 20 // 50 ms frames (800 samples)
                val shortBuf = ShortArray(chunkSamples)
                val captured = FloatArray(MAX_RECORDING_SAMPLES)
                var capturedSamples = 0

                audioRecord.startRecording()
                onStatusText("")
                Log.i(TAG, "Sherpa offline recording started (16kHz PCM)")

                var speechDetected = false
                var firstSpeechSampleIdx = 0
                var lastSpeechSampleIdx = 0
                var noiseFloorSum = 0f
                var noiseFloorFrames = 0
                var totalFrames = 0

                while (isActive && !cancelRequested && !stopRequested && capturedSamples < captured.size) {
                    val samplesToRead = minOf(chunkSamples, captured.size - capturedSamples)
                    val read = audioRecord.read(shortBuf, 0, samplesToRead)
                    check(read >= 0) { "AudioRecord read failed (code=$read)" }
                    if (read == 0) continue

                    var sumSq = 0.0
                    for (i in 0 until read) {
                        val f = shortBuf[i] / 32768.0f
                        captured[capturedSamples + i] = f
                        sumSq += (f * f).toDouble()
                    }
                    capturedSamples += read
                    val rms = sqrt(sumSq / read).toFloat()
                    totalFrames++

                    // Calibrate ambient noise floor on first 5 frames (~250ms)
                    if (noiseFloorFrames < 5) {
                        noiseFloorSum += rms
                        noiseFloorFrames++
                    }
                    val noiseFloor = if (noiseFloorFrames > 0) noiseFloorSum / noiseFloorFrames else 0.01f
                    val speechThreshold = max(0.018f, noiseFloor * 2.4f)

                    // Normalize RMS for smooth UI orb animation
                    val uiRms = ((rms - noiseFloor * 0.8f) / 0.14f).coerceIn(0f, 1f)
                    onRmsLevel(uiRms)

                    if (rms >= speechThreshold) {
                        if (!speechDetected) {
                            speechDetected = true
                            firstSpeechSampleIdx = max(0, capturedSamples - read - SAMPLE_RATE / 3)
                            Log.d(TAG, "Speech onset detected at frame=$totalFrames (rms=$rms, thr=$speechThreshold)")
                        }
                        lastSpeechSampleIdx = capturedSamples
                    } else if (speechDetected) {
                        if (capturedSamples - lastSpeechSampleIdx >= END_OF_SPEECH_SILENCE_SAMPLES) {
                            Log.i(TAG, "Auto-stopping after post-speech silence (frames=$totalFrames)")
                            break
                        }
                    } else if (capturedSamples >= INITIAL_SILENCE_SAMPLES) {
                        Log.i(TAG, "No speech detected within initial timeout")
                        break
                    }
                }

                runCatching {
                    audioRecord.stop()
                    audioRecord.release()
                }
                audioRecord = null

                if (!isActive || cancelRequested) return@launch

                // If user tapped stop manually even on quiet voice, still decode what was captured
                if (!speechDetected && !stopRequested) {
                    onError(
                        6,
                        "لم أسمع شيئًا، اضغط الميكروفون وتحدث بالعربية.",
                        "No speech heard. Tap the mic and speak in Arabic."
                    )
                    return@launch
                }

                val startIdx = if (speechDetected) firstSpeechSampleIdx else 0
                val endIdx = if (speechDetected) {
                    (lastSpeechSampleIdx + SAMPLE_RATE / 3).coerceAtMost(capturedSamples)
                } else {
                    capturedSamples
                }
                val sliceLen = (endIdx - startIdx).coerceAtLeast(0)
                if (sliceLen < SAMPLE_RATE / 5) {
                    onError(
                        7,
                        "التسجيل قصير جدًا، حاول التحدث بوضوح.",
                        "Recording too short. Please speak clearly."
                    )
                    return@launch
                }

                onStatusText("جاري التحليل محليًا...")
                onRmsLevel(0f)

                val samples = captured.copyOfRange(startIdx, endIdx)

                val decodeStart = SystemClock.elapsedRealtime()
                val stream = engine.createStream()
                val rawText = try {
                    stream.acceptWaveform(samples, SAMPLE_RATE)
                    engine.decode(stream)
                    engine.getResult(stream).text.trim()
                } finally {
                    stream.release()
                }
                val decodeMs = SystemClock.elapsedRealtime() - decodeStart
                val trailingAudioMs = (capturedSamples - lastSpeechSampleIdx) * 1000 / SAMPLE_RATE
                Log.i(
                    TAG,
                    "Sherpa-ONNX offline decode done in ${decodeMs}ms " +
                        "(audio=${sliceLen * 1000 / SAMPLE_RATE}ms, " +
                        "trailingAudio=${if (speechDetected) "${trailingAudioMs}ms" else "unknown"})"
                )

                if (!isActive || cancelRequested) return@launch

                if (rawText.isNotBlank()) {
                    onResult(rawText)
                } else {
                    onError(
                        7,
                        "لم أتمكن من تمييز الكلام، حاول التحدث بوضوح.",
                        "Could not recognize speech. Please speak clearly."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sherpa offline recognition error: ${e.message}", e)
                if (isActive && !cancelRequested) {
                    onError(
                        -11,
                        "حدث خطأ في المحرك العربي المحلي: ${e.localizedMessage ?: "خطأ غير متوقع"}",
                        "Offline Arabic engine error: ${e.localizedMessage ?: "Unexpected error"}"
                    )
                }
            } finally {
                runCatching {
                    audioRecord?.stop()
                    audioRecord?.release()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(bufferBytes: Int): AudioRecord? {
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )
        for (src in sources) {
            val rec = runCatching {
                AudioRecord(
                    src,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferBytes
                )
            }.getOrNull()
            if (rec != null && rec.state == AudioRecord.STATE_INITIALIZED) {
                return rec
            }
            runCatching { rec?.release() }
        }
        return null
    }

    fun stopListening() {
        stopRequested = true
    }

    fun cancel() {
        cancelRequested = true
        stopRequested = true
        recordingJob?.cancel()
        recordingJob = null
    }

    fun release() {
        cancel()
        runCatching { recognizer?.release() }
        recognizer = null
    }

    companion object {
        private const val TAG = "MyExpensesSherpa"
        const val SAMPLE_RATE = 16000
        private const val MAX_RECORDING_SAMPLES = SAMPLE_RATE * 12
        private const val INITIAL_SILENCE_SAMPLES = SAMPLE_RATE * 6500 / 1000
        private const val END_OF_SPEECH_SILENCE_SAMPLES = SAMPLE_RATE * 900 / 1000
        const val MODEL_DIR_NAME = "offline_asr_ar"
        const val FILE_ENCODER = "encoder.int8.onnx"
        const val FILE_DECODER = "decoder.int8.onnx"
        const val FILE_JOINER = "joiner.int8.onnx"
        const val FILE_TOKENS = "tokens.txt"
    }
}
