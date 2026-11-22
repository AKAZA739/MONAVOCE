package com.myexpenses.app.quicklaunch

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myexpenses.app.MainActivity
import com.myexpenses.app.MyExpensesApp
import com.myexpenses.app.ui.components.VoiceOverlayPanel
import com.myexpenses.app.ui.theme.MyExpensesTheme
import com.myexpenses.app.ui.theme.resolveIsArabic
import com.myexpenses.app.ui.viewmodel.VoiceSessionStage
import com.myexpenses.app.ui.viewmodel.VoiceSessionViewModel

/**
 * Lightweight translucent popup activity for instant voice expense capture.
 * Can be triggered from Samsung Side Button (Press and Hold Digital Assistant / Double Press),
 * Quick Settings Tile, Home Shortcut, Widget, or Quick Notification — including when the screen
 * is OFF or LOCKED.
 */
class QuickVoiceActivity : AppCompatActivity() {

    private val voiceViewModel: VoiceSessionViewModel by viewModels {
        VoiceSessionViewModel.Factory(
            application,
            (application as MyExpensesApp).container
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show cleanly over lock screen, wake screen if off, and keep awake while user speaks
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        voiceViewModel.autoDismissOnSave = true

        setContent {
            val prefs by voiceViewModel.prefsRepo.preferences.collectAsState()
            val stage by voiceViewModel.stage.collectAsState()
            val categories by voiceViewModel.categories.collectAsState()
            val shouldAutoDismiss by voiceViewModel.shouldAutoDismiss.collectAsState()
            val isArabic = resolveIsArabic(prefs.appLanguage)

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    voiceViewModel.startListening()
                } else {
                    Toast.makeText(
                        this,
                        if (isArabic) "صلاحية الميكروفون مطلوبة للتسجيل الصوتي" else "Microphone permission is required",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Auto-start listening immediately on launch if permission is already granted
            LaunchedEffect(Unit) {
                if (voiceViewModel.speechManager.hasAudioPermission()) {
                    voiceViewModel.startListening()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            // Auto-close floating popup smoothly after clear expense is saved
            LaunchedEffect(shouldAutoDismiss) {
                if (shouldAutoDismiss) {
                    finish()
                }
            }

            MyExpensesTheme(
                themeMode = prefs.themeMode,
                appLanguage = prefs.appLanguage
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                voiceViewModel.cancelListening()
                                finish()
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                    ) {
                        VoiceOverlayPanel(
                            stage = stage,
                            preferences = prefs,
                            categories = categories,
                            isArabic = isArabic,
                            hasMicPermission = voiceViewModel.speechManager.hasAudioPermission(),
                            isStandalonePopup = true,
                            onRequestMicAndStart = {
                                if (voiceViewModel.speechManager.hasAudioPermission()) {
                                    voiceViewModel.startListening()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onStopListening = { voiceViewModel.stopListening() },
                            onSwitchLanguage = { lang ->
                                voiceViewModel.switchSpeechLanguage(lang)
                                if (voiceViewModel.speechManager.hasAudioPermission()) {
                                    voiceViewModel.startListening(overrideLanguage = lang)
                                }
                            },
                            onSubmitTypedText = { text ->
                                voiceViewModel.processTranscript(text, enableAutoDismiss = true)
                            },
                            onUpdatePendingCategory = { idx, cat ->
                                voiceViewModel.updatePendingItemCategory(idx, cat)
                            },
                            onRemovePendingItem = { idx ->
                                voiceViewModel.removePendingItem(idx)
                            },
                            onConfirmPending = {
                                voiceViewModel.confirmAndSavePending(enableAutoDismiss = true)
                            },
                            onUndoSaved = { voiceViewModel.undoLastSaved() },
                            onTriggerOfflineDownload = {
                                val started = voiceViewModel.triggerOfflineDownload()
                                Toast.makeText(
                                    this@QuickVoiceActivity,
                                    if (started) {
                                        if (isArabic) "بدأ طلب تنزيل الحزمة المحلية..." else "Offline model download requested..."
                                    } else {
                                        if (isArabic) "افتح إعدادات الصوت لتثبيت الحزمة المحلية" else "Open speech settings to install offline language"
                                    },
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onOpenSpeechSettings = { QuickLaunchHelper.openVoiceInputSettings(this@QuickVoiceActivity) },
                            onEnableOnlineFallback = { voiceViewModel.enableOnlineFallbackAndRetry() },
                            onOpenFullApp = {
                                startActivity(
                                    Intent(this@QuickVoiceActivity, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    }
                                )
                                finish()
                            },
                            onDismiss = {
                                voiceViewModel.cancelListening()
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Prevents duplicate recording sessions if the user presses the Side Button repeatedly
     * while the popup is already listening.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val currentStage = voiceViewModel.stage.value
        if (currentStage is VoiceSessionStage.Listening) {
            Log.d(TAG, "Ignoring duplicate Side Button trigger while already listening")
            return
        }
        if (voiceViewModel.speechManager.hasAudioPermission()) {
            voiceViewModel.startListening()
        }
    }

    override fun onStop() {
        super.onStop()
        voiceViewModel.cancelListening()
    }

    companion object {
        private const val TAG = "QuickVoiceActivity"
        const val EXTRA_FROM_SIDE_BUTTON = "extra_from_side_button"
    }
}
