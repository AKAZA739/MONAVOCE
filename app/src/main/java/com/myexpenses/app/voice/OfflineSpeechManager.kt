package com.myexpenses.app.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.ModelDownloadListener
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OfflineSpeechStatus(
    val isChecked: Boolean = false,
    val isOnDeviceApiSupported: Boolean = false,
    val isStandardRecognizerAvailable: Boolean = false,
    val installedOfflineLanguages: List<String> = emptyList(),
    val downloadableOfflineLanguages: List<String> = emptyList(),
    val pendingDownloadLanguages: List<String> = emptyList(),
    val onlineSupportedLanguages: List<String> = emptyList(),
    val hasArabicOffline: Boolean = false,
    val hasSherpaArabicModel: Boolean = false,
    val hasEnglishOffline: Boolean = false,
    val canDownloadArabic: Boolean = false,
    val canDownloadEnglish: Boolean = false,
    val hasArabicOnlineSupport: Boolean = false,
    val downloadProgress: Int? = null,
    val errorNote: String? = null
)

sealed class SpeechUiState {
    data object Idle : SpeechUiState()
    data class Listening(val isOfflineEngine: Boolean, val partialText: String = "", val rmsLevel: Float = 0f) : SpeechUiState()
    data class Result(val text: String, val isOfflineEngine: Boolean) : SpeechUiState()
    data class OfflineModelMissing(val languageTag: String, val canTriggerDownload: Boolean) : SpeechUiState()
    data class Error(val code: Int, val messageAr: String, val messageEn: String) : SpeechUiState()
}

class OfflineSpeechManager(
    context: Context,
    private val sherpaArabicEngine: SherpaOfflineArabicEngine? = null
) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)

    private var activeRecognizer: SpeechRecognizer? = null
    private var probeRecognizer: SpeechRecognizer? = null
    private var downloadRecognizer: SpeechRecognizer? = null
    private var activeIsOffline: Boolean = true
    private var activeUsesSherpa: Boolean = false
    private var isCurrentlyListening: Boolean = false
    private var activeSessionId: Long = 0L
    private var pendingStartRequest: (() -> Unit)? = null
    private var isProbing: Boolean = false

    private val _state = MutableStateFlow<SpeechUiState>(SpeechUiState.Idle)
    val state: StateFlow<SpeechUiState> = _state.asStateFlow()

    private val _offlineStatus = MutableStateFlow(OfflineSpeechStatus())
    val offlineStatus: StateFlow<OfflineSpeechStatus> = _offlineStatus.asStateFlow()

    fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Checks local on-device AND standard recognition support strictly on the Main Thread
     * to avoid destroying an active SpeechRecognizer session or throwing main-thread violations.
     */
    fun refreshOfflineSupport() {
        runOnMain {
            if (activeRecognizer != null || isProbing) {
                Log.d(TAG, "Skipping probe because recognizer is active or already probing")
                return@runOnMain
            }

            val standardAvailable = SpeechRecognizer.isRecognitionAvailable(appContext)
            val onDeviceAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)

            Log.i(TAG, "Speech availability: standard=$standardAvailable, onDevice=$onDeviceAvailable, sdk=${Build.VERSION.SDK_INT}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (onDeviceAvailable || standardAvailable)) {
                isProbing = true
                probeEngine(
                    useOnDevice = onDeviceAvailable,
                    standardAvailable = standardAvailable,
                    onDeviceAvailable = onDeviceAvailable,
                    accumulatedInstalled = linkedSetOf(),
                    accumulatedSupported = linkedSetOf(),
                    accumulatedPending = linkedSetOf(),
                    accumulatedOnline = linkedSetOf()
                )
            } else {
                _offlineStatus.value = OfflineSpeechStatus(
                    isChecked = true,
                    isOnDeviceApiSupported = onDeviceAvailable,
                    isStandardRecognizerAvailable = standardAvailable
                )
                flushPendingStart()
            }
        }
    }

    private fun probeEngine(
        useOnDevice: Boolean,
        standardAvailable: Boolean,
        onDeviceAvailable: Boolean,
        accumulatedInstalled: MutableSet<String>,
        accumulatedSupported: MutableSet<String>,
        accumulatedPending: MutableSet<String>,
        accumulatedOnline: MutableSet<String>
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            isProbing = false
            flushPendingStart()
            return
        }

        releaseProbeOnMain()

        runCatching {
            val probe = if (useOnDevice) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
            } else {
                SpeechRecognizer.createSpeechRecognizer(appContext)
            }
            probeRecognizer = probe

            val probeIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            }

            probe.checkRecognitionSupport(
                probeIntent,
                mainExecutor,
                object : RecognitionSupportCallback {
                    override fun onSupportResult(support: RecognitionSupport) {
                        accumulatedInstalled.addAll(support.installedOnDeviceLanguages)
                        accumulatedSupported.addAll(support.supportedOnDeviceLanguages)
                        accumulatedPending.addAll(support.pendingOnDeviceLanguages)
                        accumulatedOnline.addAll(support.onlineLanguages)

                        Log.i(
                            TAG,
                            "checkRecognitionSupport(onDevice=$useOnDevice): " +
                                "installed=${support.installedOnDeviceLanguages}, " +
                                "downloadable=${support.supportedOnDeviceLanguages}, " +
                                "pending=${support.pendingOnDeviceLanguages}, " +
                                "onlineCount=${support.onlineLanguages.size}"
                        )

                        releaseProbeOnMain()

                        // If we just checked On-Device (e.g. AiAi), ALSO check standard RecognitionService (e.g. GoogleTTS)
                        if (useOnDevice && standardAvailable) {
                            probeEngine(
                                useOnDevice = false,
                                standardAvailable = standardAvailable,
                                onDeviceAvailable = onDeviceAvailable,
                                accumulatedInstalled = accumulatedInstalled,
                                accumulatedSupported = accumulatedSupported,
                                accumulatedPending = accumulatedPending,
                                accumulatedOnline = accumulatedOnline
                            )
                        } else {
                            finishProbe(
                                onDeviceAvailable = onDeviceAvailable,
                                standardAvailable = standardAvailable,
                                installed = accumulatedInstalled.toList().sorted(),
                                downloadable = accumulatedSupported.toList().sorted(),
                                pending = accumulatedPending.toList().sorted(),
                                online = accumulatedOnline.toList().sorted(),
                                errorNote = null
                            )
                        }
                    }

                    override fun onError(error: Int) {
                        Log.w(TAG, "checkRecognitionSupport(onDevice=$useOnDevice) error=$error")
                        releaseProbeOnMain()

                        if (useOnDevice && standardAvailable) {
                            probeEngine(
                                useOnDevice = false,
                                standardAvailable = standardAvailable,
                                onDeviceAvailable = onDeviceAvailable,
                                accumulatedInstalled = accumulatedInstalled,
                                accumulatedSupported = accumulatedSupported,
                                accumulatedPending = accumulatedPending,
                                accumulatedOnline = accumulatedOnline
                            )
                        } else {
                            finishProbe(
                                onDeviceAvailable = onDeviceAvailable,
                                standardAvailable = standardAvailable,
                                installed = accumulatedInstalled.toList().sorted(),
                                downloadable = accumulatedSupported.toList().sorted(),
                                pending = accumulatedPending.toList().sorted(),
                                online = accumulatedOnline.toList().sorted(),
                                errorNote = if (accumulatedInstalled.isEmpty()) "Support check code: $error" else null
                            )
                        }
                    }
                }
            )
        }.onFailure { err ->
            Log.e(TAG, "Probe failed (onDevice=$useOnDevice): ${err.message}", err)
            releaseProbeOnMain()
            finishProbe(
                onDeviceAvailable = onDeviceAvailable,
                standardAvailable = standardAvailable,
                installed = accumulatedInstalled.toList().sorted(),
                downloadable = accumulatedSupported.toList().sorted(),
                pending = accumulatedPending.toList().sorted(),
                online = accumulatedOnline.toList().sorted(),
                errorNote = err.localizedMessage
            )
        }
    }

    private fun finishProbe(
        onDeviceAvailable: Boolean,
        standardAvailable: Boolean,
        installed: List<String>,
        downloadable: List<String>,
        pending: List<String>,
        online: List<String>,
        errorNote: String?
    ) {
        isProbing = false
        val hasSherpaAr = sherpaArabicEngine?.isModelInstalled() == true
        val mergedInstalled = buildList {
            if (hasSherpaAr) add("ar-Gulf (Sherpa-ONNX)")
            addAll(installed)
        }
        _offlineStatus.value = OfflineSpeechStatus(
            isChecked = true,
            isOnDeviceApiSupported = onDeviceAvailable || hasSherpaAr,
            isStandardRecognizerAvailable = standardAvailable,
            installedOfflineLanguages = mergedInstalled,
            downloadableOfflineLanguages = downloadable,
            pendingDownloadLanguages = pending,
            onlineSupportedLanguages = online,
            hasArabicOffline = hasSherpaAr || installed.any { it.startsWith("ar", ignoreCase = true) },
            hasSherpaArabicModel = hasSherpaAr,
            hasEnglishOffline = installed.any { it.startsWith("en", ignoreCase = true) },
            canDownloadArabic = downloadable.any { it.startsWith("ar", ignoreCase = true) },
            canDownloadEnglish = downloadable.any { it.startsWith("en", ignoreCase = true) },
            hasArabicOnlineSupport = online.any { it.startsWith("ar", ignoreCase = true) },
            errorNote = errorNote
        )
        Log.i(
            TAG,
            "Final offline status: hasSherpaAr=$hasSherpaAr, hasArabicOffline=${_offlineStatus.value.hasArabicOffline}, " +
                "canDownloadArabic=${_offlineStatus.value.canDownloadArabic}, " +
                "hasEnglishOffline=${_offlineStatus.value.hasEnglishOffline}"
        )
        flushPendingStart()
    }

    private fun flushPendingStart() {
        val pending = pendingStartRequest ?: return
        mainHandler.post {
            if (pendingStartRequest !== pending) return@post
            pendingStartRequest = null
            pending()
        }
    }

    /**
     * Triggers on-device offline language pack download on Android 13+/14+ safely on Main Thread.
     */
    fun triggerOfflineModelDownload(languageCode: String, arabicLocalePreference: String = "ar-SA"): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val onDeviceAvailable = SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
        val standardAvailable = SpeechRecognizer.isRecognitionAvailable(appContext)
        if (!onDeviceAvailable && !standardAvailable) return false

        val localeTag = resolveTargetLocaleTag(languageCode, arabicLocalePreference)
        runOnMain {
            releaseDownloadRecognizerOnMain()
            runCatching {
                val recognizer = if (onDeviceAvailable) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(appContext)
                }
                downloadRecognizer = recognizer

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    recognizer.triggerModelDownload(
                        intent,
                        mainExecutor,
                        object : ModelDownloadListener {
                            override fun onProgress(completedPercent: Int) {
                                Log.i(TAG, "Model download progress ($localeTag): $completedPercent%")
                                _offlineStatus.value = _offlineStatus.value.copy(downloadProgress = completedPercent)
                            }

                            override fun onSuccess() {
                                Log.i(TAG, "Model download SUCCESS ($localeTag)")
                                _offlineStatus.value = _offlineStatus.value.copy(downloadProgress = null)
                                releaseDownloadRecognizerOnMain()
                                refreshOfflineSupport()
                            }

                            override fun onScheduled() {
                                Log.i(TAG, "Model download SCHEDULED ($localeTag)")
                                _offlineStatus.value = _offlineStatus.value.copy(downloadProgress = 0)
                                releaseDownloadRecognizerOnMain()
                            }

                            override fun onError(error: Int) {
                                Log.w(TAG, "Model download ERROR ($localeTag): code=$error")
                                _offlineStatus.value = _offlineStatus.value.copy(
                                    downloadProgress = null,
                                    errorNote = "Model download error: $error"
                                )
                                releaseDownloadRecognizerOnMain()
                            }
                        }
                    )
                } else {
                    recognizer.triggerModelDownload(intent)
                    mainHandler.postDelayed({
                        releaseDownloadRecognizerOnMain()
                        refreshOfflineSupport()
                    }, 4000L)
                }
            }.onFailure { err ->
                Log.e(TAG, "triggerModelDownload failed: ${err.message}", err)
                releaseDownloadRecognizerOnMain()
            }
        }
        return true
    }

    /**
     * Starts speech recognition honoring offline-first privacy settings.
     * Safe to call from any thread; marshals cleanly to the Main Thread and avoids
     * racing against checkRecognitionSupport().
     */
    fun startListening(
        languageCode: String,
        allowOnlineFallback: Boolean,
        arabicLocalePreference: String = "ar-SA"
    ) {
        runOnMain {
            if (!hasAudioPermission()) {
                _state.value = SpeechUiState.Error(
                    code = SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                    messageAr = "يرجى منح صلاحية الميكروفون للتسجيل الصوتي.",
                    messageEn = "Please grant microphone permission to use voice recording."
                )
                return@runOnMain
            }

            startListeningInternal(languageCode, allowOnlineFallback, arabicLocalePreference)
        }
    }

    private fun startListeningInternal(
        languageCode: String,
        allowOnlineFallback: Boolean,
        arabicLocalePreference: String
    ) {
        val wantArabic = languageCode.startsWith("ar", ignoreCase = true)
        val sherpa = sherpaArabicEngine?.takeIf { wantArabic && it.isModelInstalled() }

        // Only Android's recognizer shares a service with the support probe.
        if (sherpa == null && isProbing) {
            Log.i(TAG, "Deferring Android recognition until the support probe finishes")
            pendingStartRequest = {
                startListeningInternal(languageCode, allowOnlineFallback, arabicLocalePreference)
            }
            return
        }

        pendingStartRequest = null
        stopAndReleaseInternal()
        if (sherpa != null) {
            launchSherpaArabicOffline(sherpa)
            return
        }

        val status = _offlineStatus.value
        val targetTag = resolveTargetLocaleTag(languageCode, arabicLocalePreference)

        val onDeviceReady = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
        val standardReady = SpeechRecognizer.isRecognitionAvailable(appContext)

        Log.i(
            TAG,
            "startListening: lang=$languageCode, targetTag=$targetTag, allowOnline=$allowOnlineFallback, " +
                "checked=${status.isChecked}, hasArOff=${status.hasArabicOffline}, hasEnOff=${status.hasEnglishOffline}"
        )

        val languageInstalled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            status.isChecked && status.errorNote == null
        ) {
            if (wantArabic) status.hasArabicOffline else status.hasEnglishOffline
        } else {
            null
        }

        when (selectSystemRecognitionMode(onDeviceReady, languageInstalled, standardReady, allowOnlineFallback)) {
            SystemRecognitionMode.ON_DEVICE -> launchRecognizer(
                useOnDevice = true,
                localeTag = targetTag,
                allowOnlineRetry = allowOnlineFallback && standardReady
            )
            SystemRecognitionMode.STANDARD -> launchRecognizer(useOnDevice = false, localeTag = targetTag)
            SystemRecognitionMode.UNAVAILABLE -> {
                val canDownload = if (wantArabic) status.canDownloadArabic else status.canDownloadEnglish
                _state.value = SpeechUiState.OfflineModelMissing(targetTag, canDownload)
            }
        }
    }

    private fun launchRecognizer(
        useOnDevice: Boolean,
        localeTag: String,
        allowOnlineRetry: Boolean = false
    ) {
        try {
            val recognizer = when {
                !useOnDevice -> SpeechRecognizer.createSpeechRecognizer(appContext)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
                else -> error("On-device speech recognition requires Android 12 or later")
            }
            activeRecognizer = recognizer
            activeIsOffline = useOnDevice
            isCurrentlyListening = true
            val sessionId = ++activeSessionId

            Log.i(TAG, "Launching recognizer: session=$sessionId, useOnDevice=$useOnDevice, localeTag=$localeTag")

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (sessionId != activeSessionId) return
                    Log.d(TAG, "onReadyForSpeech(localeTag=$localeTag, offline=$activeIsOffline)")
                    _state.value = SpeechUiState.Listening(isOfflineEngine = activeIsOffline)
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    if (sessionId != activeSessionId) return
                    val current = _state.value
                    if (current is SpeechUiState.Listening) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _state.value = current.copy(rmsLevel = normalized)
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (sessionId != activeSessionId) return
                    isCurrentlyListening = false
                    Log.w(TAG, "onError(code=$error, localeTag=$localeTag, useOnDevice=$useOnDevice)")
                    if (error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ||
                        error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
                    ) {
                        if (allowOnlineRetry) {
                            stopAndReleaseInternal()
                            launchRecognizer(useOnDevice = false, localeTag = localeTag)
                            return
                        }
                        val canDl = if (localeTag.startsWith("ar", ignoreCase = true)) {
                            _offlineStatus.value.canDownloadArabic
                        } else {
                            _offlineStatus.value.canDownloadEnglish
                        }
                        _state.value = SpeechUiState.OfflineModelMissing(localeTag, canDl)
                        return
                    }

                    val (msgAr, msgEn) = mapError(error)
                    _state.value = SpeechUiState.Error(error, msgAr, msgEn)
                }

                override fun onResults(results: Bundle?) {
                    if (sessionId != activeSessionId) return
                    isCurrentlyListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim().orEmpty()
                    Log.i(TAG, "onResults(localeTag=$localeTag, count=${matches?.size ?: 0})")
                    if (best.isNotEmpty()) {
                        _state.value = SpeechUiState.Result(best, activeIsOffline)
                    } else {
                        _state.value = SpeechUiState.Error(
                            code = SpeechRecognizer.ERROR_NO_MATCH,
                            messageAr = "لم أفهم الكلام بوضوح، حاول مرة أخرى.",
                            messageEn = "Didn't catch that clearly. Please try again."
                        )
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (sessionId != activeSessionId) return
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (partial.isNotEmpty()) {
                        val current = _state.value
                        if (current is SpeechUiState.Listening) {
                            _state.value = current.copy(partialText = partial)
                        }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, useOnDevice)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L)
            }

            _state.value = SpeechUiState.Listening(isOfflineEngine = activeIsOffline)
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "launchRecognizer exception: ${e.message}", e)
            _state.value = SpeechUiState.Error(
                code = -2,
                messageAr = "تعذر تشغيل التعرف الصوتي: ${e.localizedMessage ?: "خطأ غير متوقع"}",
                messageEn = "Failed to start speech recognizer: ${e.localizedMessage ?: "Unexpected error"}"
            )
        }
    }

    private fun launchSherpaArabicOffline(sherpa: SherpaOfflineArabicEngine) {
        activeUsesSherpa = true
        activeIsOffline = true
        isCurrentlyListening = true
        val sessionId = ++activeSessionId

        Log.i(TAG, "Launching 100% OFFLINE Sherpa-ONNX Arabic engine (session=$sessionId)")
        _state.value = SpeechUiState.Listening(isOfflineEngine = true)

        sherpa.startListening(
            onStatusText = { statusMsg ->
                runOnMain {
                    if (sessionId != activeSessionId) return@runOnMain
                    val current = _state.value
                    if (current is SpeechUiState.Listening) {
                        _state.value = current.copy(partialText = statusMsg)
                    }
                }
            },
            onRmsLevel = { rms ->
                runOnMain {
                    if (sessionId != activeSessionId) return@runOnMain
                    val current = _state.value
                    if (current is SpeechUiState.Listening) {
                        _state.value = current.copy(rmsLevel = rms)
                    }
                }
            },
            onResult = { text ->
                runOnMain {
                    if (sessionId != activeSessionId) return@runOnMain
                    isCurrentlyListening = false
                    activeUsesSherpa = false
                    _state.value = SpeechUiState.Result(text = text, isOfflineEngine = true)
                }
            },
            onError = { code, msgAr, msgEn ->
                runOnMain {
                    if (sessionId != activeSessionId) return@runOnMain
                    isCurrentlyListening = false
                    activeUsesSherpa = false
                    Log.w(TAG, "Sherpa-ONNX offline Arabic error (code=$code): $msgEn")
                    _state.value = SpeechUiState.Error(code, msgAr, msgEn)
                }
            }
        )
    }

    fun stopListening() {
        runOnMain {
            if (!isCurrentlyListening) return@runOnMain
            isCurrentlyListening = false
            if (activeUsesSherpa) {
                sherpaArabicEngine?.stopListening()
            } else {
                runCatching { activeRecognizer?.stopListening() }
            }
        }
    }

    fun cancel() {
        runOnMain {
            pendingStartRequest = null
            activeSessionId++
            stopAndReleaseInternal()
            _state.value = SpeechUiState.Idle
        }
    }

    fun resetState() {
        _state.value = SpeechUiState.Idle
    }

    fun destroy() {
        runOnMain {
            pendingStartRequest = null
            activeSessionId++
            stopAndReleaseInternal()
            releaseProbeOnMain()
            releaseDownloadRecognizerOnMain()
        }
    }

    private fun stopAndReleaseInternal() {
        isCurrentlyListening = false
        if (activeUsesSherpa) {
            sherpaArabicEngine?.cancel()
            activeUsesSherpa = false
        }
        runCatching {
            activeRecognizer?.setRecognitionListener(null)
            activeRecognizer?.cancel()
            activeRecognizer?.destroy()
        }
        activeRecognizer = null
    }

    private fun releaseProbeOnMain() {
        runCatching { probeRecognizer?.destroy() }
        probeRecognizer = null
    }

    private fun releaseDownloadRecognizerOnMain() {
        runCatching { downloadRecognizer?.destroy() }
        downloadRecognizer = null
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun resolveTargetLocaleTag(languageCode: String, arabicLocalePreference: String): String {
        val status = _offlineStatus.value
        val wantArabic = languageCode.startsWith("ar", ignoreCase = true)
        if (wantArabic) {
            // 1. Prefer user's chosen Arabic dialect if installed offline
            val exactInstalled = status.installedOfflineLanguages.firstOrNull {
                it.equals(arabicLocalePreference, ignoreCase = true)
            }
            if (exactInstalled != null) return exactInstalled

            // 2. Otherwise any installed Arabic offline pack
            val anyInstalledAr = status.installedOfflineLanguages.firstOrNull {
                it.startsWith("ar", ignoreCase = true)
            }
            if (anyInstalledAr != null) return anyInstalledAr

            // 3. Otherwise any downloadable Arabic offline pack
            val downloadableAr = status.downloadableOfflineLanguages.firstOrNull {
                it.startsWith("ar", ignoreCase = true)
            }
            if (downloadableAr != null) return downloadableAr

            // 4. Otherwise user's preferred Arabic dialect (e.g. ar-SA, ar-YE, ar-EG)
            val onlineExact = status.onlineSupportedLanguages.firstOrNull {
                it.equals(arabicLocalePreference, ignoreCase = true)
            }
            if (onlineExact != null) return onlineExact

            return arabicLocalePreference.ifBlank { "ar-SA" }
        } else {
            val installedEn = status.installedOfflineLanguages.firstOrNull {
                it.startsWith("en", ignoreCase = true)
            }
            if (installedEn != null) return installedEn
            return "en-US"
        }
    }

    private fun mapError(code: Int): Pair<String, String> = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH ->
            "لم أتمكن من تمييز الكلام، حاول التحدث بوضوح." to "Couldn't understand speech. Please speak clearly."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "لم أسمع شيئًا، اضغط الميكروفون وتحدث." to "No speech heard. Tap the mic and speak."
        SpeechRecognizer.ERROR_AUDIO ->
            "تعذر الوصول إلى الميكروفون." to "Microphone audio recording error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "صلاحية الميكروفون غير ممنوحة." to "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "التعرف الصوتي يحتاج اتصالًا بالإنترنت لأن حزمة هذه اللغة غير مثبتة محليًا." to "No internet connection and offline pack is not installed for this language."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "خدمة الصوت مشغولة، حاول بعد لحظة." to "Speech service is busy. Try again in a moment."
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED, SpeechRecognizer.ERROR_SERVER ->
            "تعذر الاتصال بمحرك التعرف الصوتي على الجهاز." to "Speech service disconnected. Please retry."
        else ->
            "تعذر إتمام التعرف الصوتي (رمز $code)." to "Speech recognition failed (code $code)."
    }

    companion object {
        private const val TAG = "MyExpensesSpeech"
    }
}
