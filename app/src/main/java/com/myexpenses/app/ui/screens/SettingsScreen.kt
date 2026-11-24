package com.myexpenses.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsVoice
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myexpenses.app.BuildConfig
import com.myexpenses.app.R
import com.myexpenses.app.data.preferences.SupportedCurrencies
import com.myexpenses.app.data.preferences.UserPreferences
import com.myexpenses.app.voice.OfflineSpeechStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    offlineStatus: OfflineSpeechStatus,
    isArabic: Boolean,
    onCurrencyChange: (String) -> Unit,
    onAppLanguageChange: (String) -> Unit,
    onSpeechLanguageChange: (String) -> Unit,
    onArabicLocaleChange: (String) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onVoiceFeedbackToggle: (Boolean) -> Unit,
    onAutoSaveToggle: (Boolean) -> Unit,
    onOnlineFallbackToggle: (Boolean) -> Unit,
    onQuickNotificationToggle: (Boolean) -> Unit,
    onAddQuickTile: () -> Unit,
    onPinHomeShortcut: () -> Unit,
    onOpenAssistantSettings: () -> Unit,
    onRefreshOfflineStatus: () -> Unit,
    onDownloadArabicModel: () -> Unit,
    onDownloadEnglishModel: () -> Unit,
    onImportOfflineArabicZip: () -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onExportCsv: () -> Unit,
    onBackupJson: () -> Unit,
    onRestoreJson: () -> Unit
) {
    val appName = stringResource(R.string.app_name)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Quick Activation & Lock Screen Guide
        item {
            SettingsSectionCard(
                title = if (isArabic) "التشغيل السريع واختصارات شاشة القفل" else "Quick Launch & Lock-Screen Access",
                icon = Icons.Rounded.Bolt
            ) {
                Text(
                    text = if (isArabic) {
                        "افتح نافذة التسجيل من الإعدادات السريعة، أو اختصار الشاشة الرئيسية، أو الإشعار السريع."
                    } else {
                        "Open the recording panel from Quick Settings, a home screen shortcut, or the quick notification."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddQuickTile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "إضافة Quick Tile" else "Add Quick Tile", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onPinHomeShortcut,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "اختصار الشاشة" else "Pin Shortcut", maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingsSwitchRow(
                    title = if (isArabic) "إشعار سريع ثابت على شاشة القفل" else "Persistent Quick-Record Notification",
                    subtitle = if (isArabic) "اختياري وصامت — ضغطة واحدة من شاشة القفل أو شريط الإشعارات"
                    else "Optional & silent — 1 tap from lock screen or notification shade",
                    checked = preferences.quickNotificationEnabled,
                    onCheckedChange = onQuickNotificationToggle
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isArabic) "التشغيل من الزر الجانبي" else "Launch from the side button",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (isArabic) {
                                "إذا كان هاتفك يدعم ربط الضغط المطوّل على الزر الجانبي بالمساعد الرقمي، اختر $appName كمساعد افتراضي. تختلف خيارات الزر وشاشة القفل حسب الجهاز وإصدار النظام."
                            } else {
                                "If your phone can open the digital assistant by holding the side button, select $appName as the default assistant. Side-button and lock-screen options vary by device and Android version."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(onClick = onOpenAssistantSettings) {
                            Text(if (isArabic) "فتح إعدادات المساعد والصوت" else "Open Assistant & Voice Settings")
                        }
                    }
                }
            }
        }

        // 2. Offline Speech Recognition Status
        item {
            SettingsSectionCard(
                title = if (isArabic) "التعرف الصوتي المحلي (بدون إنترنت)" else "Offline Speech Recognition",
                icon = Icons.Rounded.SettingsVoice,
                trailingAction = {
                    IconButton(onClick = onRefreshOfflineStatus) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                    }
                }
            ) {
                val arState = when {
                    offlineStatus.hasSherpaArabicModel -> if (isArabic) "مثبتة محليًا ✓ (Sherpa-ONNX FastConformer)" else "Installed offline ✓ (Sherpa-ONNX FastConformer)"
                    !offlineStatus.isChecked -> if (isArabic) "جاري الفحص..." else "Checking..."
                    offlineStatus.hasArabicOffline -> if (isArabic) "مثبتة محليًا ✓" else "Installed offline ✓"
                    offlineStatus.canDownloadArabic -> if (isArabic) "قابلة للتنزيل محليًا" else "Available to download"
                    offlineStatus.isOnDeviceApiSupported -> if (isArabic) "غير متوفرة محليًا على هذا المحرك" else "Not available offline on this engine"
                    else -> if (isArabic) "يتطلب فحص إعدادات Google Speech" else "Check system Speech Services"
                }

                val enState = when {
                    !offlineStatus.isChecked -> if (isArabic) "جاري الفحص..." else "Checking..."
                    offlineStatus.hasEnglishOffline -> if (isArabic) "مثبتة محليًا ✓" else "Installed offline ✓"
                    offlineStatus.canDownloadEnglish -> if (isArabic) "قابلة للتنزيل محليًا" else "Available to download"
                    offlineStatus.isOnDeviceApiSupported -> if (isArabic) "غير متوفرة محليًا على هذا المحرك" else "Not available offline on this engine"
                    else -> if (isArabic) "يتطلب فحص إعدادات Google Speech" else "Check system Speech Services"
                }

                Text(
                    text = (if (isArabic) "العربية (محلي): " else "Arabic (On-Device): ") + arState,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = (if (isArabic) "الإنجليزية (محلي): " else "English (On-Device): ") + enState,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (offlineStatus.installedOfflineLanguages.isNotEmpty()) {
                    Text(
                        text = (if (isArabic) "الحزم المثبتة على الجهاز: " else "Installed offline packs: ") +
                            offlineStatus.installedOfflineLanguages.joinToString(", "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!offlineStatus.hasArabicOffline && offlineStatus.isChecked && !offlineStatus.canDownloadArabic) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) {
                            "يمكنك استيراد نموذج العربية المحلي من ملف ZIP أدناه."
                        } else {
                            "You can import the on-device Arabic model from a ZIP file below."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (offlineStatus.canDownloadArabic && !offlineStatus.hasArabicOffline) {
                        Button(onClick = onDownloadArabicModel) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isArabic) "تنزيل العربية محليًا" else "Download Arabic")
                        }
                    }
                    if (offlineStatus.canDownloadEnglish && !offlineStatus.hasEnglishOffline) {
                        Button(onClick = onDownloadEnglishModel) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isArabic) "تنزيل الإنجليزية محليًا" else "Download English")
                        }
                    }
                    OutlinedButton(onClick = onImportOfflineArabicZip) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "استيراد نموذج العربية المحلي (.zip)" else "Import Offline Arabic Model (.zip)")
                    }
                    OutlinedButton(onClick = onOpenSpeechSettings) {
                        Text(if (isArabic) "إدارة حزم اللغات المحلية" else "Manage Offline Speech Packs")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SettingsSwitchRow(
                    title = if (isArabic) "السماح بالتعرف السحابي كبديل اختياري" else "Allow Optional Online Speech Fallback",
                    subtitle = if (isArabic) "مغلق افتراضيًا. عند استخدام البديل، قد ترسل خدمة التعرف الصوتي الصوت إلى مزوّدها."
                    else "Off by default. When the fallback is used, your speech service may send audio to its provider.",
                    checked = preferences.allowOnlineSpeechFallback,
                    onCheckedChange = onOnlineFallbackToggle
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = if (isArabic) "لغة الاستماع الافتراضية" else "Default Voice Recognition Language",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = preferences.speechLanguage == "ar",
                        onClick = { onSpeechLanguageChange("ar") },
                        label = { Text("العربية") }
                    )
                    FilterChip(
                        selected = preferences.speechLanguage == "en",
                        onClick = { onSpeechLanguageChange("en") },
                        label = { Text("English") }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isArabic) "رمز اللهجة العربية للتعرف الصوتي" else "Arabic Speech Locale",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "ar-YE" to (if (isArabic) "اليمن (ar-YE)" else "Yemen (ar-YE)"),
                        "ar-SA" to (if (isArabic) "السعودية (ar-SA)" else "Saudi (ar-SA)"),
                        "ar-EG" to (if (isArabic) "مصر (ar-EG)" else "Egypt (ar-EG)")
                    ).forEach { (tag, label) ->
                        FilterChip(
                            selected = preferences.arabicLocaleTag == tag,
                            onClick = { onArabicLocaleChange(tag) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                SettingsSwitchRow(
                    title = if (isArabic) "الرد الصوتي (قراءة التأكيد والإجابات)" else "Spoken Voice Feedback (TTS)",
                    subtitle = if (isArabic) "يقرأ التأكيدات والإجابات إذا توفر صوت محلي للغة."
                    else "Reads confirmations and answers when a local voice is available.",
                    checked = preferences.voiceFeedbackEnabled,
                    onCheckedChange = onVoiceFeedbackToggle
                )

                SettingsSwitchRow(
                    title = if (isArabic) "الحفظ التلقائي للمصروفات الواضحة" else "Auto-Save Clear Expenses",
                    subtitle = if (isArabic) "يحفظ المصروف فورًا مع زر تراجع، ويطلب التأكيد فقط عند الغموض"
                    else "Saves immediately with Undo; prompts only when ambiguous",
                    checked = preferences.autoSaveClearExpenses,
                    onCheckedChange = onAutoSaveToggle
                )
            }
        }

        // 3. Currency, Language & Theme
        item {
            SettingsSectionCard(
                title = if (isArabic) "العملة واللغة والمظهر" else "Currency, Language & Theme",
                icon = null
            ) {
                Text(
                    text = if (isArabic) "العملة الافتراضية" else "Default Currency",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SupportedCurrencies.list.forEach { cur ->
                        FilterChip(
                            selected = preferences.currencyCode == cur.code,
                            onClick = { onCurrencyChange(cur.code) },
                            label = {
                                Text(if (isArabic) "${cur.nameAr} (${cur.code})" else "${cur.code} - ${cur.symbolEn}")
                            }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Text(
                    text = if (isArabic) "لغة الواجهة" else "App Language",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = preferences.appLanguage == "system",
                        onClick = { onAppLanguageChange("system") },
                        label = { Text(if (isArabic) "النظام" else "System") }
                    )
                    FilterChip(
                        selected = preferences.appLanguage == "ar",
                        onClick = { onAppLanguageChange("ar") },
                        label = { Text("العربية") }
                    )
                    FilterChip(
                        selected = preferences.appLanguage == "en",
                        onClick = { onAppLanguageChange("en") },
                        label = { Text("English") }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Text(
                    text = if (isArabic) "المظهر" else "Theme",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = preferences.themeMode == "system",
                        onClick = { onThemeModeChange("system") },
                        label = { Text(if (isArabic) "تلقائي" else "System") }
                    )
                    FilterChip(
                        selected = preferences.themeMode == "light",
                        onClick = { onThemeModeChange("light") },
                        label = { Text(if (isArabic) "فاتح" else "Light") }
                    )
                    FilterChip(
                        selected = preferences.themeMode == "dark",
                        onClick = { onThemeModeChange("dark") },
                        label = { Text(if (isArabic) "داكن" else "Dark") }
                    )
                }
            }
        }

        // 4. Privacy, CSV Export & Local Backup
        item {
            SettingsSectionCard(
                title = if (isArabic) "الخصوصية والنسخ الاحتياطي" else "Privacy, Export & Local Backup",
                icon = Icons.Rounded.Security
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isArabic) {
                            "تُحفظ مصروفاتك على جهازك. قد تتضمن ملفات التصدير والنسخ الاحتياطية ملاحظاتك والنص المفرّغ من التسجيل."
                        } else {
                            "Your expenses are stored on your device. Exports and backups may contain your notes and speech transcriptions."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onExportCsv,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isArabic) "تصدير المصروفات إلى ملف CSV (Excel)" else "Export Expenses to CSV")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackupJson,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "نسخة احتياطية" else "Backup JSON")
                    }
                    OutlinedButton(
                        onClick = onRestoreJson,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "استعادة نسخة" else "Restore JSON")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isArabic) "$appName • الإصدار ${BuildConfig.VERSION_NAME}"
                    else "$appName • ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                trailingAction?.invoke()
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
