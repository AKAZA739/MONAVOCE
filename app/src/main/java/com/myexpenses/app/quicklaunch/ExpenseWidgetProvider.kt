package com.myexpenses.app.quicklaunch

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.myexpenses.app.MainActivity
import com.myexpenses.app.R
import com.myexpenses.app.data.local.ExpenseDatabase
import com.myexpenses.app.data.preferences.UserPreferencesRepository
import com.myexpenses.app.domain.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class ExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateAllWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ExpenseWidgetProvider::class.java))
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                updateAllWidgets(context, manager, ids)
            }
        }

        private suspend fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val db = ExpenseDatabase.getInstance(context)
            val prefs = UserPreferencesRepository(context).preferences.value
            val range = DateTimeUtils.todayRange()
            val total = db.expenseDao().getTotalInRange(range.startMillis, range.endMillis)
            val count = db.expenseDao().getCountInRange(range.startMillis, range.endMillis)

            val isArabic = when (prefs.appLanguage) {
                "ar" -> true
                "en" -> false
                else -> Locale.getDefault().language == "ar"
            }

            val formattedTotal = DateTimeUtils.formatCurrency(total, prefs.currency, isArabic)
            val subtitle = if (count > 0) {
                if (isArabic) "$count عمليات اليوم • اضغط للتحدث" else "$count entries today • Tap mic"
            } else {
                context.getString(R.string.widget_tap_to_record)
            }

            val voicePendingIntent = PendingIntent.getActivity(
                context,
                301,
                QuickLaunchHelper.createQuickVoiceIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val appPendingIntent = PendingIntent.getActivity(
                context,
                302,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_expense).apply {
                    setTextViewText(R.id.widget_total_amount, formattedTotal)
                    setTextViewText(R.id.widget_count, subtitle)
                    setOnClickPendingIntent(R.id.widget_mic_button, voicePendingIntent)
                    setOnClickPendingIntent(R.id.widget_summary_container, appPendingIntent)
                }
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
