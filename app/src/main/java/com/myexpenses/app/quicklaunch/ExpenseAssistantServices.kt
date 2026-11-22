package com.myexpenses.app.quicklaunch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Official Android VoiceInteractionService so My Expenses appears natively in
 * Android Digital Assistant & Samsung One UI "Side button -> Press and hold -> Digital assistant".
 *
 * Uses ZERO background microphone or CPU when idle.
 */
class ExpenseVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        Log.i(TAG, "ExpenseVoiceInteractionService ready")
    }

    companion object {
        private const val TAG = "MyExpensesAssist"
    }
}

class ExpenseVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return ExpenseVoiceInteractionSession(this)
    }
}

class ExpenseVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.i("MyExpensesAssist", "Side button / Assist triggered (showFlags=$showFlags) -> launching QuickVoiceActivity")
        val intent = QuickLaunchHelper.createQuickVoiceIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(QuickVoiceActivity.EXTRA_FROM_SIDE_BUTTON, true)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure { err ->
            Log.e("MyExpensesAssist", "Failed to launch QuickVoiceActivity: ${err.message}", err)
        }
        // Hide the empty system assist session immediately so QuickVoiceActivity takes focus cleanly
        hide()
    }
}

/**
 * Minimal RecognitionService metadata target required by Android's VoiceInteractionService contract.
 * Never activates microphone in the background.
 */
class ExpenseStubRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        runCatching { listener?.error(SpeechRecognizer.ERROR_CLIENT) }
    }

    override fun onCancel(listener: Callback?) {}

    override fun onStopListening(listener: Callback?) {}
}
