package com.myexpenses.app.quicklaunch

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.myexpenses.app.R
import java.util.concurrent.Executors

object QuickLaunchHelper {

    private const val CHANNEL_ID = "quick_voice_shortcut_channel"
    private const val NOTIFICATION_ID = 7001

    fun createQuickVoiceIntent(context: Context): Intent {
        return Intent(context, QuickVoiceActivity::class.java).apply {
            action = "com.myexpenses.app.ACTION_QUICK_VOICE"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    /**
     * One-tap system prompt to add the Quick Settings Tile (Android 13+ / API 33+).
     */
    fun requestAddQuickSettingsTile(
        context: Context,
        onResult: (addedOrRequested: Boolean) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
            if (statusBarManager != null) {
                statusBarManager.requestAddTileService(
                    ComponentName(context, ExpenseQuickTileService::class.java),
                    context.getString(R.string.tile_label),
                    Icon.createWithResource(context, R.drawable.ic_mic_tile),
                    Executors.newSingleThreadExecutor()
                ) { resultCode ->
                    val ok = resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                        resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
                    onResult(ok)
                }
                return
            }
        }
        onResult(false)
    }

    /**
     * Pins a 1-tap Voice Record shortcut directly to the user's Home Screen.
     */
    fun requestPinVoiceShortcut(context: Context): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val shortcut = ShortcutInfoCompat.Builder(context, "pinned_quick_voice_v1")
            .setShortLabel(context.getString(R.string.shortcut_voice_short))
            .setLongLabel(context.getString(R.string.shortcut_voice_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_mic_shortcut))
            .setIntent(createQuickVoiceIntent(context))
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    /**
     * Optional persistent silent notification for 1-tap voice recording from notification shade / lock screen.
     */
    fun updateQuickNotification(context: Context, enabled: Boolean) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!enabled) {
            manager.cancel(NOTIFICATION_ID)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.quick_notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.quick_notif_channel_desc)
            setShowBadge(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            context,
            101,
            createQuickVoiceIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic_tile)
            .setContentTitle(context.getString(R.string.quick_notif_title))
            .setContentText(context.getString(R.string.quick_notif_body))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_mic_tile,
                context.getString(R.string.quick_notif_action),
                pendingIntent
            )
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    fun openVoiceInputSettings(context: Context) {
        val intents = listOf(
            // Direct shortcut to Google's "Offline Speech Recognition" language pack manager
            Intent("com.google.android.speech.embedded.MANAGE_LANGUAGES"),
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent("com.android.settings.TTS_SETTINGS"),
            Intent(Settings.ACTION_LOCALE_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        }
    }

    fun openAssistantSettings(context: Context) {
        val intents = listOf(
            Intent().setClassName(
                "com.android.settings",
                "com.android.settings.Settings\$FunctionKeySettingsActivity"
            ),
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                runCatching {
                    context.startActivity(intent)
                    return
                }
            }
        }
    }
}
