package com.myexpenses.app

import android.app.Application
import com.myexpenses.app.data.backup.BackupManager
import com.myexpenses.app.data.local.ExpenseDatabase
import com.myexpenses.app.data.preferences.UserPreferencesRepository
import com.myexpenses.app.data.repository.ExpenseRepository
import com.myexpenses.app.quicklaunch.QuickLaunchHelper
import com.myexpenses.app.voice.SherpaOfflineArabicEngine
import com.myexpenses.app.voice.VoiceCommandExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
    val database: ExpenseDatabase by lazy { ExpenseDatabase.getInstance(application) }
    val repository: ExpenseRepository by lazy { ExpenseRepository(application, database) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(application) }
    val commandExecutor: VoiceCommandExecutor by lazy { VoiceCommandExecutor(repository) }
    val backupManager: BackupManager by lazy { BackupManager(application, repository) }
    val sherpaArabicEngine: SherpaOfflineArabicEngine by lazy { SherpaOfflineArabicEngine(application) }
}

class MyExpensesApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.repository.ensureDefaultCategories()
        }
        // Pre-warm the offline Arabic Sherpa-ONNX model in background if installed
        container.sherpaArabicEngine.warmUpAsync()
        val prefs = container.preferencesRepository.preferences.value
        if (prefs.quickNotificationEnabled) {
            QuickLaunchHelper.updateQuickNotification(this, true)
        }
    }
}
