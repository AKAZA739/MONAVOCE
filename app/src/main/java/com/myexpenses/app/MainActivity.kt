package com.myexpenses.app

import android.Manifest
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.myexpenses.app.data.backup.ValidatedBackup
import com.myexpenses.app.data.local.entity.ExpenseEntity
import com.myexpenses.app.data.preferences.SupportedCurrencies
import com.myexpenses.app.quicklaunch.ExpenseWidgetProvider
import com.myexpenses.app.quicklaunch.QuickLaunchHelper
import com.myexpenses.app.ui.components.ExpenseEditorDialog
import com.myexpenses.app.ui.components.VoiceOverlayPanel
import com.myexpenses.app.ui.screens.CategoriesScreen
import com.myexpenses.app.ui.screens.DashboardScreen
import com.myexpenses.app.ui.screens.HistoryScreen
import com.myexpenses.app.ui.screens.SettingsScreen
import com.myexpenses.app.ui.theme.AccentPeriwinkle
import com.myexpenses.app.ui.theme.MyExpensesTheme
import com.myexpenses.app.ui.theme.NeonCyan
import com.myexpenses.app.ui.theme.NeonMagenta
import com.myexpenses.app.ui.theme.resolveIsArabic
import com.myexpenses.app.ui.viewmodel.MainViewModel
import com.myexpenses.app.ui.viewmodel.VoiceSessionStage
import com.myexpenses.app.ui.viewmodel.VoiceSessionViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : AppCompatActivity() {

    private val appContainer: AppContainer
        get() = (application as MyExpensesApp).container

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(appContainer)
    }

    private val voiceViewModel: VoiceSessionViewModel by viewModels {
        VoiceSessionViewModel.Factory(application, appContainer)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        setContent {
            val prefs by mainViewModel.preferences.collectAsState()
            val dashboardState by mainViewModel.dashboardState.collectAsState()
            val categories by mainViewModel.categories.collectAsState()
            val historyExpenses by mainViewModel.filteredHistory.collectAsState()
            val searchQuery by mainViewModel.searchQuery.collectAsState()
            val selectedCategoryFilter by mainViewModel.selectedCategoryFilter.collectAsState()
            val historyDateFilter by mainViewModel.historyDateFilter.collectAsState()
            val voiceStage by voiceViewModel.stage.collectAsState()
            val offlineStatus by voiceViewModel.offlineStatus.collectAsState()

            val isArabic = resolveIsArabic(prefs.appLanguage)
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            var selectedTab by remember { mutableIntStateOf(0) }
            var showVoiceSheet by remember { mutableStateOf(false) }
            var showManualAddDialog by remember { mutableStateOf(false) }
            var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

            LaunchedEffect(voiceStage) {
                val cmd = voiceStage as? VoiceSessionStage.CommandResult
                if (cmd != null) {
                    cmd.navigateToDate?.let { date ->
                        mainViewModel.selectDate(date)
                        selectedTab = if (cmd.openHistory) 1 else 0
                    }
                }
            }

            val micPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    voiceViewModel.startListening()
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (isArabic) "تم رفض صلاحية الميكروفون. يمكنك التفعيل لاحقًا أو الإدخال يدويًا."
                            else "Microphone permission denied. You can enable it later or enter manually."
                        )
                    }
                }
            }

            val notifPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    mainViewModel.prefsRepo.setQuickNotificationEnabled(true)
                    QuickLaunchHelper.updateQuickNotification(this, true)
                } else {
                    mainViewModel.prefsRepo.setQuickNotificationEnabled(false)
                    Toast.makeText(
                        this,
                        if (isArabic) "صلاحية الإشعارات مطلوبة لتفعيل الإشعار السريع" else "Notification permission required",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val csvExportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("text/csv")
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        val res = appContainer.backupManager.exportCsv(uri)
                        val msg = res.fold(
                            onSuccess = { count ->
                                if (isArabic) "تم تصدير $count مصروف إلى CSV بنجاح" else "Exported $count expenses to CSV"
                            },
                            onFailure = { err ->
                                if (isArabic) "فشل التصدير: ${err.localizedMessage}" else "Export failed: ${err.localizedMessage}"
                            }
                        )
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            }

            val jsonBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        val res = appContainer.backupManager.exportJsonBackup(uri)
                        val msg = res.fold(
                            onSuccess = { count ->
                                if (isArabic) "تم حفظ النسخة الاحتياطية ($count مصروف)" else "Backup saved ($count expenses)"
                            },
                            onFailure = { err ->
                                if (isArabic) "فشل النسخ الاحتياطي: ${err.localizedMessage}" else "Backup failed: ${err.localizedMessage}"
                            }
                        )
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            }

            var pendingBackup by remember { mutableStateOf<ValidatedBackup?>(null) }
            var restoreBusy by remember { mutableStateOf(false) }
            val jsonRestoreLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null && !restoreBusy) {
                    restoreBusy = true
                    scope.launch {
                        val result = try {
                            appContainer.backupManager.readJsonBackup(uri)
                        } finally {
                            restoreBusy = false
                        }
                        result.onSuccess { pendingBackup = it }.onFailure {
                            snackbarHostState.showSnackbar(
                                if (isArabic) "تعذّرت قراءة نسخة احتياطية صالحة"
                                else "Could not read a valid backup"
                            )
                        }
                    }
                }
            }

            val arabicModelZipLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (isArabic) "جاري استيراد النموذج العربي المحلي..." else "Importing offline Arabic model...",
                            duration = SnackbarDuration.Short
                        )
                        val res = appContainer.sherpaArabicEngine.importModelFromZipUri(uri)
                        voiceViewModel.speechManager.refreshOfflineSupport()
                        val msg = res.fold(
                            onSuccess = {
                                if (isArabic) "تم استيراد النموذج العربي المحلي بنجاح ✓" else "Offline Arabic model imported successfully ✓"
                            },
                            onFailure = { err ->
                                if (isArabic) "تعذر استيراد النموذج: ${err.localizedMessage}" else "Model import failed: ${err.localizedMessage}"
                            }
                        )
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            }

            fun openVoiceSheetAndStart() {
                voiceViewModel.resetSession()
                showVoiceSheet = true
                if (voiceViewModel.speechManager.hasAudioPermission()) {
                    voiceViewModel.startListening()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            fun deleteWithUndo(expense: ExpenseEntity) {
                mainViewModel.deleteExpense(expense)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = if (isArabic) "تم حذف المصروف" else "Expense deleted",
                        actionLabel = if (isArabic) "تراجع" else "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        mainViewModel.undoDeleteExpense()
                    }
                }
            }

            MyExpensesTheme(
                themeMode = prefs.themeMode,
                appLanguage = prefs.appLanguage
            ) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(bottom = 84.dp)
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .statusBarsPadding()
                    ) {
                        when (selectedTab) {
                            0 -> DashboardScreen(
                                state = dashboardState,
                                currency = prefs.currency,
                                isArabic = isArabic,
                                onSelectPeriod = { mainViewModel.selectPeriod(it) },
                                onSelectChartCurrency = { mainViewModel.selectChartCurrency(it) },
                                onStepDate = { mainViewModel.stepDate(it) },
                                onSelectToday = { mainViewModel.selectDate(LocalDate.now()) },
                                onOpenCategories = { selectedTab = 2 },
                                onAddManual = { showManualAddDialog = true },
                                onEditExpense = { editingExpense = it },
                                onDeleteExpense = { deleteWithUndo(it) }
                            )

                            1 -> HistoryScreen(
                                expenses = historyExpenses,
                                categories = categories,
                                searchQuery = searchQuery,
                                selectedCategoryId = selectedCategoryFilter,
                                dateFilter = historyDateFilter,
                                currency = prefs.currency,
                                isArabic = isArabic,
                                onSearchChange = { mainViewModel.updateSearchQuery(it) },
                                onCategoryFilterChange = { mainViewModel.updateCategoryFilter(it) },
                                onDateFilterChange = { mainViewModel.updateHistoryDateFilter(it) },
                                onEditExpense = { editingExpense = it },
                                onDeleteExpense = { deleteWithUndo(it) }
                            )

                            2 -> CategoriesScreen(
                                categories = categories,
                                isArabic = isArabic,
                                onSaveCategory = { id, ar, en, icon, color, keywords ->
                                    mainViewModel.saveCategory(id, ar, en, icon, color, keywords)
                                },
                                onDeleteCategory = { mainViewModel.deleteCategory(it) }
                            )

                            3 -> SettingsScreen(
                                preferences = prefs,
                                offlineStatus = offlineStatus,
                                isArabic = isArabic,
                                onCurrencyChange = {
                                    mainViewModel.prefsRepo.setCurrencyCode(it)
                                    ExpenseWidgetProvider.requestUpdate(this@MainActivity)
                                },
                                onAppLanguageChange = { lang ->
                                    mainViewModel.prefsRepo.setAppLanguage(lang)
                                    ExpenseWidgetProvider.requestUpdate(this@MainActivity)
                                    val localeTags = when (lang) {
                                        "ar" -> LocaleListCompat.forLanguageTags("ar")
                                        "en" -> LocaleListCompat.forLanguageTags("en")
                                        else -> LocaleListCompat.getEmptyLocaleList()
                                    }
                                    AppCompatDelegate.setApplicationLocales(localeTags)
                                },
                                onSpeechLanguageChange = { mainViewModel.prefsRepo.setSpeechLanguage(it) },
                                onArabicLocaleChange = { mainViewModel.prefsRepo.setArabicLocaleTag(it) },
                                onThemeModeChange = { mainViewModel.prefsRepo.setThemeMode(it) },
                                onVoiceFeedbackToggle = { mainViewModel.prefsRepo.setVoiceFeedbackEnabled(it) },
                                onAutoSaveToggle = { mainViewModel.prefsRepo.setAutoSaveClearExpenses(it) },
                                onOnlineFallbackToggle = { mainViewModel.prefsRepo.setAllowOnlineSpeechFallback(it) },
                                onQuickNotificationToggle = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        mainViewModel.prefsRepo.setQuickNotificationEnabled(enabled)
                                        QuickLaunchHelper.updateQuickNotification(this@MainActivity, enabled)
                                    }
                                },
                                onAddQuickTile = {
                                    QuickLaunchHelper.requestAddQuickSettingsTile(this@MainActivity) { ok ->
                                        runOnUiThread {
                                            val msg = if (ok) {
                                                if (isArabic) "تمت إضافة اختصار Quick Tile بنجاح" else "Quick Settings Tile added"
                                            } else {
                                                if (isArabic) "اسحب شريط الإشعارات واضغط تعديل لإضافة \"سجّل مصروف\""
                                                else "Swipe down Quick Settings → Edit to add 'Log Expense'"
                                            }
                                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onPinHomeShortcut = {
                                    val ok = QuickLaunchHelper.requestPinVoiceShortcut(this@MainActivity)
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (ok) {
                                            if (isArabic) "تم إرسال طلب إضافة الاختصار للشاشة الرئيسية" else "Shortcut pin requested"
                                        } else {
                                            if (isArabic) "اضغط مطولًا على أيقونة التطبيق لإضافة اختصار التسجيل" else "Long-press app icon to add shortcut"
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onOpenAssistantSettings = { QuickLaunchHelper.openAssistantSettings(this@MainActivity) },
                                onRefreshOfflineStatus = { voiceViewModel.speechManager.refreshOfflineSupport() },
                                onDownloadArabicModel = {
                                    val ok = voiceViewModel.speechManager.triggerOfflineModelDownload("ar", prefs.arabicLocaleTag)
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (ok) {
                                            if (isArabic) "بدأ طلب تنزيل الحزمة العربية المحلية" else "Arabic offline model requested"
                                        } else {
                                            if (isArabic) "افتح إعدادات الصوت لتنزيل الحزمة العربية" else "Open system speech settings to download Arabic"
                                        },
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onDownloadEnglishModel = {
                                    val ok = voiceViewModel.speechManager.triggerOfflineModelDownload("en")
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (ok) {
                                            if (isArabic) "بدأ طلب تنزيل الحزمة الإنجليزية المحلية" else "English offline model requested"
                                        } else {
                                            if (isArabic) "افتح إعدادات الصوت لتنزيل الحزمة الإنجليزية" else "Open system speech settings to download English"
                                        },
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onImportOfflineArabicZip = {
                                    arabicModelZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                },
                                onOpenSpeechSettings = { QuickLaunchHelper.openVoiceInputSettings(this@MainActivity) },
                                onExportCsv = { csvExportLauncher.launch("monavoce_${LocalDate.now()}.csv") },
                                onBackupJson = { jsonBackupLauncher.launch("monavoce_backup_${LocalDate.now()}.json") },
                                onRestoreJson = {
                                    if (!restoreBusy) jsonRestoreLauncher.launch(arrayOf("application/json", "*/*"))
                                }
                            )
                        }

                        // Floating Pill Bottom Dock + Separate Circular Search Button (matching reference!)
                        FloatingObsidianDock(
                            selectedTab = selectedTab,
                            isArabic = isArabic,
                            onSelectTab = { selectedTab = it },
                            onVoiceClick = { openVoiceSheetAndStart() },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }

                // Cosmic Neon Voice Bottom Sheet
                if (showVoiceSheet) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = {
                            voiceViewModel.cancelListening()
                            showVoiceSheet = false
                        },
                        sheetState = sheetState,
                        containerColor = Color.Transparent,
                        dragHandle = null
                    ) {
                        VoiceOverlayPanel(
                            stage = voiceStage,
                            preferences = prefs,
                            categories = categories,
                            isArabic = isArabic,
                            hasMicPermission = voiceViewModel.speechManager.hasAudioPermission(),
                            isStandalonePopup = false,
                            onRequestMicAndStart = {
                                if (voiceViewModel.speechManager.hasAudioPermission()) {
                                    voiceViewModel.startListening()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
                                voiceViewModel.processTranscript(text, enableAutoDismiss = false)
                            },
                            onUpdatePendingCategory = { idx, cat ->
                                voiceViewModel.updatePendingItemCategory(idx, cat)
                            },
                            onRemovePendingItem = { idx ->
                                voiceViewModel.removePendingItem(idx)
                            },
                            onConfirmPending = {
                                voiceViewModel.confirmAndSavePending(enableAutoDismiss = false)
                            },
                            onUndoSaved = { voiceViewModel.undoLastSaved() },
                            onTriggerOfflineDownload = {
                                voiceViewModel.triggerOfflineDownload()
                            },
                            onOpenSpeechSettings = { QuickLaunchHelper.openVoiceInputSettings(this@MainActivity) },
                            onEnableOnlineFallback = { voiceViewModel.enableOnlineFallbackAndRetry() },
                            onDismiss = {
                                voiceViewModel.cancelListening()
                                showVoiceSheet = false
                            }
                        )
                    }
                }

                pendingBackup?.let { backup ->
                    AlertDialog(
                        onDismissRequest = { pendingBackup = null },
                        title = { Text(if (isArabic) "استعادة النسخة الاحتياطية؟" else "Restore backup?") },
                        text = {
                            val summary = backup.summary
                            Text(
                                if (isArabic) "سيتم استبدال سجل المصروفات والفئات الحالية بالكامل. المصروفات في النسخة: ${summary.restoredExpenses}، الفئات: ${summary.restoredCategories}."
                                else "This replaces your current expenses and categories. Expenses in backup: ${summary.restoredExpenses}. Categories: ${summary.restoredCategories}."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (restoreBusy) return@TextButton
                                pendingBackup = null
                                restoreBusy = true
                                scope.launch {
                                    val result = try {
                                        appContainer.backupManager.restoreBackup(backup)
                                    } finally {
                                        restoreBusy = false
                                    }
                                    val message = if (result.isSuccess) {
                                        if (isArabic) "تمت استعادة النسخة الاحتياطية" else "Backup restored"
                                    } else {
                                        if (isArabic) "تعذّرت الاستعادة. بقيت بياناتك الحالية محفوظة."
                                        else "Restore failed. Your current data is still saved."
                                    }
                                    snackbarHostState.showSnackbar(message)
                                }
                            }) { Text(if (isArabic) "استعادة واستبدال" else "Restore and replace") }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingBackup = null }) {
                                Text(if (isArabic) "إلغاء" else "Cancel")
                            }
                        }
                    )
                }

                if (showManualAddDialog) {
                    ExpenseEditorDialog(
                        initialExpense = null,
                        categories = categories,
                        currency = prefs.currency,
                        isArabic = isArabic,
                        defaultDate = dashboardState.selectedDate,
                        onDismiss = { showManualAddDialog = false },
                        onSave = { amount, catId, note, ts ->
                            mainViewModel.addManualExpense(amount, catId, note, ts)
                            showManualAddDialog = false
                        }
                    )
                }

                editingExpense?.let { expense ->
                    ExpenseEditorDialog(
                        initialExpense = expense,
                        categories = categories,
                        currency = SupportedCurrencies.forExpense(expense.currencyCode),
                        isArabic = isArabic,
                        onDismiss = { editingExpense = null },
                        onSave = { amount, catId, note, ts ->
                            mainViewModel.updateExpense(expense, amount, catId, note, ts)
                            editingExpense = null
                        },
                        onDelete = {
                            editingExpense = null
                            deleteWithUndo(expense)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingObsidianDock(
    selectedTab: Int,
    isArabic: Boolean,
    onSelectTab: (Int) -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dockBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    val dockBorder = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Floating Pill Dock: Home | Glowing Voice Mic | Categories | Settings
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(34.dp),
            color = dockBg,
            border = BorderStroke(1.dp, dockBorder),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockNavItem(
                    icon = Icons.Rounded.Home,
                    label = if (isArabic) "الرئيسية" else "Home",
                    selected = selectedTab == 0,
                    onClick = { onSelectTab(0) }
                )

                // Prominent Cosmic Neon Voice Button right inside the dock
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NeonCyan, AccentPeriwinkle, NeonMagenta)
                            )
                        )
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10132A))
                        .clickable(onClick = onVoiceClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = if (isArabic) "تسجيل صوتي" else "Voice",
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }

                DockNavItem(
                    icon = Icons.Rounded.Category,
                    label = if (isArabic) "الفئات" else "Categories",
                    selected = selectedTab == 2,
                    onClick = { onSelectTab(2) }
                )

                DockNavItem(
                    icon = Icons.Rounded.Settings,
                    label = if (isArabic) "الإعدادات" else "Settings",
                    selected = selectedTab == 3,
                    onClick = { onSelectTab(3) }
                )
            }
        }

        // Separate Floating Circular Search / History Button (exact signature from reference image!)
        val searchActive = selectedTab == 1
        Surface(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .clickable { onSelectTab(1) },
            shape = CircleShape,
            color = if (searchActive) MaterialTheme.colorScheme.primaryContainer else dockBg,
            border = BorderStroke(
                width = 1.dp,
                color = if (searchActive) MaterialTheme.colorScheme.primary else dockBorder
            ),
            shadowElevation = 10.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = if (isArabic) "السجل والبحث" else "History & Search",
                    tint = if (searchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun DockNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) activeColor.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (selected) activeColor else inactiveColor,
            maxLines = 1
        )
    }
}
