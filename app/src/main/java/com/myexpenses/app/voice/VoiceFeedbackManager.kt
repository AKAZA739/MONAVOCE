package com.myexpenses.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.myexpenses.app.domain.parser.TextNormalizer
import java.util.Locale

class VoiceFeedbackManager(context: Context) {

    private var tts: TextToSpeech? = null
    @Volatile
    private var isReady: Boolean = false
    private var pendingMessage: String? = null

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                isReady = (status == TextToSpeech.SUCCESS)
                if (isReady) {
                    pendingMessage?.let { msg ->
                        pendingMessage = null
                        speakInternal(msg)
                    }
                }
            }
        }
    }

    fun speak(message: String, enabled: Boolean) {
        if (!enabled || message.isBlank()) return
        if (!isReady) {
            pendingMessage = message
            return
        }
        speakInternal(message)
    }

    private fun speakInternal(message: String) {
        val engine = tts ?: return
        runCatching {
            val locale = if (TextNormalizer.containsArabic(message)) {
                Locale("ar")
            } else {
                Locale.US
            }
            val localVoice = engine.voices.orEmpty()
                .filter { voice ->
                    voice.locale.language == locale.language && !voice.isNetworkConnectionRequired &&
                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty()
                }
                .maxWithOrNull(compareBy<Voice> { it.locale == locale }.thenBy { it.quality })
                ?: return
            if (engine.setVoice(localVoice) != TextToSpeech.SUCCESS) return
            engine.speak(message, TextToSpeech.QUEUE_FLUSH, null, "my_expenses_tts")
        }
    }

    fun stop() {
        pendingMessage = null
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        isReady = false
        pendingMessage = null
    }
}
