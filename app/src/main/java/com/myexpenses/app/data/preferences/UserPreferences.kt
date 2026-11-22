package com.myexpenses.app.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CurrencyOption(
    val code: String,
    val nameAr: String,
    val nameEn: String,
    val symbolAr: String,
    val symbolEn: String
)

object SupportedCurrencies {
    val list: List<CurrencyOption> = listOf(
        CurrencyOption("YER", "الريال اليمني", "Yemeni Rial", "ريال", "YER"),
        CurrencyOption("SAR", "الريال السعودي", "Saudi Riyal", "ر.س", "SAR"),
        CurrencyOption("USD", "الدولار الأمريكي", "US Dollar", "دولار", "$"),
        CurrencyOption("AED", "الدرهم الإماراتي", "UAE Dirham", "د.إ", "AED"),
        CurrencyOption("EGP", "الجنيه المصري", "Egyptian Pound", "ج.م", "EGP"),
        CurrencyOption("KWD", "الدينار الكويتي", "Kuwaiti Dinar", "د.ك", "KWD"),
        CurrencyOption("JOD", "الدينار الأردني", "Jordanian Dinar", "د.أ", "JOD"),
        CurrencyOption("EUR", "اليورو", "Euro", "يورو", "€")
    )

    fun findByCode(code: String): CurrencyOption =
        list.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: list.first()
}

data class UserPreferences(
    val currencyCode: String = "YER",
    val appLanguage: String = "system", // "system", "ar", "en"
    val speechLanguage: String = "ar",  // "ar", "en"
    val arabicLocaleTag: String = "ar-SA", // "ar-SA", "ar-YE", "ar-EG"
    val themeMode: String = "dark",     // "dark", "light", "system"
    val voiceFeedbackEnabled: Boolean = true,
    val autoSaveClearExpenses: Boolean = true,
    /** Privacy-first default: false (never send audio to cloud unless user opts in) */
    val allowOnlineSpeechFallback: Boolean = false,
    val quickNotificationEnabled: Boolean = false
) {
    val currency: CurrencyOption
        get() = SupportedCurrencies.findByCode(currencyCode)
}

class UserPreferencesRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(readPrefs())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private fun readPrefs(): UserPreferences = UserPreferences(
        currencyCode = prefs.getString(KEY_CURRENCY, "YER") ?: "YER",
        appLanguage = prefs.getString(KEY_APP_LANG, "system") ?: "system",
        speechLanguage = prefs.getString(KEY_SPEECH_LANG, "ar") ?: "ar",
        arabicLocaleTag = prefs.getString(KEY_ARABIC_LOCALE, "ar-SA") ?: "ar-SA",
        themeMode = prefs.getString(KEY_THEME, "dark") ?: "dark",
        voiceFeedbackEnabled = prefs.getBoolean(KEY_VOICE_FEEDBACK, true),
        autoSaveClearExpenses = prefs.getBoolean(KEY_AUTO_SAVE, true),
        allowOnlineSpeechFallback = prefs.getBoolean(KEY_ONLINE_FALLBACK, false),
        quickNotificationEnabled = prefs.getBoolean(KEY_QUICK_NOTIF, false)
    )

    private fun update(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        _preferences.value = readPrefs()
    }

    fun setCurrencyCode(code: String) = update { putString(KEY_CURRENCY, code) }
    fun setAppLanguage(lang: String) = update { putString(KEY_APP_LANG, lang) }
    fun setSpeechLanguage(lang: String) = update { putString(KEY_SPEECH_LANG, lang) }
    fun setArabicLocaleTag(tag: String) = update { putString(KEY_ARABIC_LOCALE, tag) }
    fun setThemeMode(mode: String) = update { putString(KEY_THEME, mode) }
    fun setVoiceFeedbackEnabled(enabled: Boolean) = update { putBoolean(KEY_VOICE_FEEDBACK, enabled) }
    fun setAutoSaveClearExpenses(enabled: Boolean) = update { putBoolean(KEY_AUTO_SAVE, enabled) }
    fun setAllowOnlineSpeechFallback(allowed: Boolean) = update { putBoolean(KEY_ONLINE_FALLBACK, allowed) }
    fun setQuickNotificationEnabled(enabled: Boolean) = update { putBoolean(KEY_QUICK_NOTIF, enabled) }

    companion object {
        private const val PREFS_NAME = "my_expenses_prefs"
        private const val KEY_CURRENCY = "currency_code"
        private const val KEY_APP_LANG = "app_language"
        private const val KEY_SPEECH_LANG = "speech_language"
        private const val KEY_ARABIC_LOCALE = "arabic_locale_tag"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_VOICE_FEEDBACK = "voice_feedback_enabled"
        private const val KEY_AUTO_SAVE = "auto_save_clear"
        private const val KEY_ONLINE_FALLBACK = "allow_online_speech_fallback"
        private const val KEY_QUICK_NOTIF = "quick_notification_enabled"
    }
}
