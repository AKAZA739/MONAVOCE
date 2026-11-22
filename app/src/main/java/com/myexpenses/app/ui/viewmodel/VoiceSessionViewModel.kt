package com.myexpenses.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myexpenses.app.AppContainer
import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.DefaultCategories
import com.myexpenses.app.data.local.entity.ExpenseEntity
import com.myexpenses.app.domain.DateTimeUtils
import com.myexpenses.app.domain.parser.ParsedExpense
import com.myexpenses.app.domain.parser.TextNormalizer
import com.myexpenses.app.domain.parser.VoiceInputParser
import com.myexpenses.app.domain.parser.VoiceParseResult
import com.myexpenses.app.ui.theme.resolveIsArabic
import com.myexpenses.app.voice.OfflineSpeechManager
import com.myexpenses.app.voice.OfflineSpeechStatus
import com.myexpenses.app.voice.SpeechUiState
import com.myexpenses.app.voice.VoiceFeedbackManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class VoiceSessionStage {
    data object Ready : VoiceSessionStage()
    data class Listening(val isOfflineEngine: Boolean, val partialText: String, val rmsLevel: Float) : VoiceSessionStage()
    data class NeedsConfirmation(
        val rawTranscript: String,
        val items: List<ParsedExpense>,
        val reasonAr: String?,
        val reasonEn: String?
    ) : VoiceSessionStage()
    data class SavedSuccess(
        val savedIds: List<Long>,
        val summaryAr: String,
        val summaryEn: String,
        val rawTranscript: String
    ) : VoiceSessionStage()
    data class CommandResult(
        val messageAr: String,
        val messageEn: String,
        val navigateToDate: LocalDate? = null,
        val openHistory: Boolean = false
    ) : VoiceSessionStage()
    data class OfflineModelRequired(
        val languageTag: String,
        val canTriggerDownload: Boolean
    ) : VoiceSessionStage()
    data class Error(
        val messageAr: String,
        val messageEn: String,
        val rawTranscript: String = ""
    ) : VoiceSessionStage()
}

class VoiceSessionViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {

    val speechManager = OfflineSpeechManager(application, container.sherpaArabicEngine)
    private val feedbackManager = VoiceFeedbackManager(application)

    private val repo = container.repository
    val prefsRepo = container.preferencesRepository

    val categories: StateFlow<List<CategoryEntity>> = repo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultCategories.list)

    val offlineStatus: StateFlow<OfflineSpeechStatus> = speechManager.offlineStatus

    private val _stage = MutableStateFlow<VoiceSessionStage>(VoiceSessionStage.Ready)
    val stage: StateFlow<VoiceSessionStage> = _stage.asStateFlow()

    private val _shouldAutoDismiss = MutableStateFlow(false)
    val shouldAutoDismiss: StateFlow<Boolean> = _shouldAutoDismiss.asStateFlow()

    private var autoDismissJob: Job? = null
    var autoDismissOnSave: Boolean = false

    init {
        speechManager.refreshOfflineSupport()
        viewModelScope.launch {
            speechManager.state.collect { st ->
                when (st) {
                    SpeechUiState.Idle -> {
                        if (_stage.value is VoiceSessionStage.Listening) {
                            _stage.value = VoiceSessionStage.Ready
                        }
                    }
                    is SpeechUiState.Listening -> {
                        autoDismissJob?.cancel()
                        _stage.value = VoiceSessionStage.Listening(
                            isOfflineEngine = st.isOfflineEngine,
                            partialText = st.partialText,
                            rmsLevel = st.rmsLevel
                        )
                    }
                    is SpeechUiState.Result -> {
                        processTranscript(st.text, enableAutoDismiss = autoDismissOnSave)
                    }
                    is SpeechUiState.OfflineModelMissing -> {
                        _stage.value = VoiceSessionStage.OfflineModelRequired(
                            languageTag = st.languageTag,
                            canTriggerDownload = st.canTriggerDownload
                        )
                    }
                    is SpeechUiState.Error -> {
                        _stage.value = VoiceSessionStage.Error(st.messageAr, st.messageEn)
                    }
                }
            }
        }
    }

    fun startListening(overrideLanguage: String? = null) {
        autoDismissJob?.cancel()
        _shouldAutoDismiss.value = false
        val prefs = prefsRepo.preferences.value
        val lang = overrideLanguage ?: prefs.speechLanguage
        speechManager.startListening(
            languageCode = lang,
            allowOnlineFallback = prefs.allowOnlineSpeechFallback,
            arabicLocalePreference = prefs.arabicLocaleTag
        )
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun cancelListening() {
        autoDismissJob?.cancel()
        speechManager.cancel()
        _stage.value = VoiceSessionStage.Ready
    }

    fun switchSpeechLanguage(lang: String) {
        prefsRepo.setSpeechLanguage(lang)
    }

    fun enableOnlineFallbackAndRetry() {
        prefsRepo.setAllowOnlineSpeechFallback(true)
        startListening()
    }

    fun triggerOfflineDownload(): Boolean {
        val prefs = prefsRepo.preferences.value
        val ok = speechManager.triggerOfflineModelDownload(prefs.speechLanguage, prefs.arabicLocaleTag)
        viewModelScope.launch {
            delay(1500)
            speechManager.refreshOfflineSupport()
        }
        return ok
    }

    /**
     * Parses spoken OR manually typed natural language utterance.
     */
    fun processTranscript(transcript: String, enableAutoDismiss: Boolean = false) {
        autoDismissJob?.cancel()
        viewModelScope.launch {
            val prefs = prefsRepo.preferences.value
            val isArabicUi = resolveIsArabic(prefs.appLanguage)
            val useArabicFeedback = TextNormalizer.containsArabic(transcript) || isArabicUi
            val allCategories = repo.getCategories()

            when (val parsed = VoiceInputParser.parse(transcript, allCategories)) {
                is VoiceParseResult.Command -> {
                    val result = container.commandExecutor.execute(parsed.command, prefs.currency)
                    _stage.value = VoiceSessionStage.CommandResult(
                        messageAr = result.messageAr,
                        messageEn = result.messageEn,
                        navigateToDate = result.navigateToDate,
                        openHistory = result.openHistory
                    )
                    feedbackManager.speak(result.message(useArabicFeedback), prefs.voiceFeedbackEnabled)
                }

                is VoiceParseResult.Expenses -> {
                    if (!parsed.needsConfirmation && prefs.autoSaveClearExpenses) {
                        saveParsedExpenses(
                            items = parsed.items,
                            rawTranscript = parsed.rawTranscript,
                            speakArabic = useArabicFeedback,
                            enableAutoDismiss = enableAutoDismiss
                        )
                    } else {
                        _stage.value = VoiceSessionStage.NeedsConfirmation(
                            rawTranscript = parsed.rawTranscript,
                            items = parsed.items,
                            reasonAr = parsed.ambiguityReasonAr,
                            reasonEn = parsed.ambiguityReasonEn
                        )
                    }
                }

                is VoiceParseResult.Unclear -> {
                    _stage.value = VoiceSessionStage.Error(
                        messageAr = parsed.reasonAr,
                        messageEn = parsed.reasonEn,
                        rawTranscript = parsed.rawTranscript
                    )
                }
            }
        }
    }

    fun updatePendingItemCategory(index: Int, category: CategoryEntity) {
        val current = _stage.value as? VoiceSessionStage.NeedsConfirmation ?: return
        if (index !in current.items.indices) return
        val updated = current.items.toMutableList()
        updated[index] = updated[index].copy(
            categoryId = category.id,
            categoryNameAr = category.nameAr,
            categoryNameEn = category.nameEn,
            isCategoryMatched = true,
            confidence = 1f
        )
        _stage.value = current.copy(items = updated)
    }

    fun updatePendingItemAmount(index: Int, newAmount: Double) {
        val current = _stage.value as? VoiceSessionStage.NeedsConfirmation ?: return
        if (index !in current.items.indices || newAmount <= 0.0) return
        val updated = current.items.toMutableList()
        updated[index] = updated[index].copy(amount = newAmount)
        _stage.value = current.copy(items = updated)
    }

    fun removePendingItem(index: Int) {
        val current = _stage.value as? VoiceSessionStage.NeedsConfirmation ?: return
        if (index !in current.items.indices) return
        val updated = current.items.toMutableList().also { it.removeAt(index) }
        if (updated.isEmpty()) {
            _stage.value = VoiceSessionStage.Ready
        } else {
            _stage.value = current.copy(items = updated)
        }
    }

    fun confirmAndSavePending(enableAutoDismiss: Boolean = false) {
        val current = _stage.value as? VoiceSessionStage.NeedsConfirmation ?: return
        val prefs = prefsRepo.preferences.value
        val useArabic = TextNormalizer.containsArabic(current.rawTranscript) || resolveIsArabic(prefs.appLanguage)
        viewModelScope.launch {
            saveParsedExpenses(
                items = current.items,
                rawTranscript = current.rawTranscript,
                speakArabic = useArabic,
                enableAutoDismiss = enableAutoDismiss
            )
        }
    }

    private suspend fun saveParsedExpenses(
        items: List<ParsedExpense>,
        rawTranscript: String,
        speakArabic: Boolean,
        enableAutoDismiss: Boolean
    ) {
        val prefs = prefsRepo.preferences.value
        val entities = items.map { item ->
            ExpenseEntity(
                amount = item.amount,
                currencyCode = prefs.currencyCode,
                categoryId = item.categoryId,
                note = item.note,
                rawTranscript = rawTranscript,
                timestamp = item.timestampMillis,
                source = "VOICE"
            )
        }
        val ids = repo.insertExpenses(entities)

        val (summaryAr, summaryEn) = if (items.size == 1) {
            val single = items.first()
            val amtAr = DateTimeUtils.formatCurrency(single.amount, prefs.currency, true)
            val amtEn = DateTimeUtils.formatCurrency(single.amount, prefs.currency, false)
            "تم تسجيل $amtAr على ${single.categoryNameAr}" to
                "Saved $amtEn for ${single.categoryNameEn}"
        } else {
            val sum = items.sumOf { it.amount }
            val amtAr = DateTimeUtils.formatCurrency(sum, prefs.currency, true)
            val amtEn = DateTimeUtils.formatCurrency(sum, prefs.currency, false)
            "تم تسجيل ${items.size} مصروفات بإجمالي $amtAr" to
                "Saved ${items.size} expenses totaling $amtEn"
        }

        _stage.value = VoiceSessionStage.SavedSuccess(
            savedIds = ids,
            summaryAr = summaryAr,
            summaryEn = summaryEn,
            rawTranscript = rawTranscript
        )

        feedbackManager.speak(if (speakArabic) summaryAr else summaryEn, prefs.voiceFeedbackEnabled)

        if (enableAutoDismiss) {
            autoDismissJob?.cancel()
            autoDismissJob = viewModelScope.launch {
                delay(3600)
                _shouldAutoDismiss.value = true
            }
        }
    }

    fun undoLastSaved() {
        val current = _stage.value as? VoiceSessionStage.SavedSuccess ?: return
        autoDismissJob?.cancel()
        viewModelScope.launch {
            repo.deleteExpensesByIds(current.savedIds)
            _stage.value = VoiceSessionStage.Ready
        }
    }

    fun resetSession() {
        autoDismissJob?.cancel()
        _shouldAutoDismiss.value = false
        speechManager.cancel()
        _stage.value = VoiceSessionStage.Ready
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        feedbackManager.shutdown()
    }

    class Factory(
        private val application: Application,
        private val container: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VoiceSessionViewModel(application, container) as T
        }
    }
}
