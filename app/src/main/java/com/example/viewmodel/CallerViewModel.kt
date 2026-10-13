package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.QuickCallApp
import com.example.data.model.CallAnalytics
import com.example.data.model.CallRecord
import com.example.data.model.CallType
import com.example.data.model.CallerInfo
import com.example.data.model.ScanResult
import com.example.data.model.SpamReport
import com.example.data.repository.CallerRepository
import com.example.scanner.AiSecurityAudit
import com.example.utils.SpamAnalysis
import com.example.utils.SpamDetector
import com.example.utils.T9Matcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CallFilter {
    ALL,
    INCOMING,
    MISSED,
    SPAM
}

class CallerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CallerRepository = (application as QuickCallApp).repository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CallFilter.ALL)
    val selectedFilter: StateFlow<CallFilter> = _selectedFilter.asStateFlow()

    private val _activeCaller = MutableStateFlow<CallerInfo?>(null)
    val activeCaller: StateFlow<CallerInfo?> = _activeCaller.asStateFlow()

    private val _activeSpamAnalysis = MutableStateFlow<SpamAnalysis?>(null)
    val activeSpamAnalysis: StateFlow<SpamAnalysis?> = _activeSpamAnalysis.asStateFlow()

    private val _aiSecurityAudit = MutableStateFlow<AiSecurityAudit?>(null)
    val aiSecurityAudit: StateFlow<AiSecurityAudit?> = _aiSecurityAudit.asStateFlow()

    private val _isAuditingAi = MutableStateFlow(false)
    val isAuditingAi: StateFlow<Boolean> = _isAuditingAi.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CallerInfo>>(emptyList())
    val searchResults: StateFlow<List<CallerInfo>> = _searchResults.asStateFlow()

    // Interactive T9 Dialer State
    private val _dialerInput = MutableStateFlow("")
    val dialerInput: StateFlow<String> = _dialerInput.asStateFlow()

    val recentCalls: StateFlow<List<CallRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val spamReports: StateFlow<List<SpamReport>> = repository.spamReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCallers: StateFlow<List<CallerInfo>> = repository.allCallers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val blockedCallers: StateFlow<List<CallerInfo>> = repository.blockedCallers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteCallers: StateFlow<List<CallerInfo>> = repository.favoriteCallers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val scanHistory: StateFlow<List<ScanResult>> = repository.scanHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // T9 Predictive Filtered Contacts
    val t9Results: StateFlow<List<CallerInfo>> = combine(allCallers, _dialerInput) { callers, digits ->
        if (digits.isBlank()) {
            callers.take(15)
        } else {
            callers.filter { caller ->
                T9Matcher.matches(digits, caller.name, caller.phoneNumber)
            }.take(20)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Real-Time Smart Call Analytics
    val analytics: StateFlow<CallAnalytics> = combine(recentCalls, blockedCallers) { calls, blocked ->
        val total = calls.size
        val spam = calls.count { it.isSpam || it.callType == CallType.SPAM_BLOCKED } + blocked.size
        val incoming = calls.count { it.callType == CallType.INCOMING }
        val outgoing = calls.count { it.callType == CallType.OUTGOING }
        val missed = calls.count { it.callType == CallType.MISSED }
        val minutesSaved = spam * 3
        val safeRatio = if (total > 0) ((total - spam).coerceAtLeast(0).toFloat() / total) else 1.0f
        val riskPercent = if (total > 0) ((spam.toFloat() / total) * 100).toInt().coerceIn(0, 100) else 0

        CallAnalytics(
            totalCalls = total,
            spamBlocked = spam,
            incomingCount = incoming,
            outgoingCount = outgoing,
            missedCount = missed,
            minutesSaved = minutesSaved,
            safeVerifiedRatio = safeRatio,
            spamRiskPercent = riskPercent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CallAnalytics()
    )

    init {
        syncDeviceData()
    }

    fun syncDeviceData() {
        viewModelScope.launch {
            _isSyncing.value = true
            val contactsCount = repository.syncDeviceContacts()
            val callsCount = repository.syncSystemCallLogs()
            _isSyncing.value = false
            if (contactsCount > 0 || callsCount > 0) {
                _statusMessage.value = "Synced $contactsCount contacts and $callsCount recent calls from device"
            }
        }
    }

    fun onFilterSelected(filter: CallFilter) {
        _selectedFilter.value = filter
    }

    // T9 Keypad Actions
    fun onDialerDigit(digit: String) {
        if (_dialerInput.value.length < 20) {
            _dialerInput.value += digit
        }
    }

    fun onDialerBackspace() {
        if (_dialerInput.value.isNotEmpty()) {
            _dialerInput.value = _dialerInput.value.dropLast(1)
        }
    }

    fun onDialerClear() {
        _dialerInput.value = ""
    }

    fun setDialerInput(text: String) {
        _dialerInput.value = text
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            val list = allCallers.value.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.phoneNumber.contains(query) ||
                        it.category.contains(query, ignoreCase = true) ||
                        (it.tag != null && it.tag.contains(query, ignoreCase = true)) ||
                        (it.note != null && it.note.contains(query, ignoreCase = true))
            }
            _searchResults.value = list
        }
    }

    fun lookupCaller(phoneNumber: String) {
        viewModelScope.launch {
            val caller = repository.getCaller(phoneNumber)
            _activeCaller.value = caller
            _activeSpamAnalysis.value = SpamDetector.analyze(caller)
            _aiSecurityAudit.value = null
        }
    }

    fun auditCallerWithAi(phoneNumber: String) {
        viewModelScope.launch {
            _isAuditingAi.value = true
            _statusMessage.value = "Running AI Deep Security Audit..."
            val audit = repository.auditCallerWithAi(phoneNumber)
            _aiSecurityAudit.value = audit
            _isAuditingAi.value = false
            _statusMessage.value = "AI Audit complete: ${audit.riskLevel}"
        }
    }

    fun clearAiAudit() {
        _aiSecurityAudit.value = null
    }

    fun toggleFavorite(phoneNumber: String) {
        viewModelScope.launch {
            val current = _activeCaller.value
            val newFav = if (current?.phoneNumber == phoneNumber) !current.isFavorite else true
            repository.toggleFavorite(phoneNumber, newFav)
            if (_activeCaller.value?.phoneNumber == phoneNumber) {
                _activeCaller.value = _activeCaller.value?.copy(isFavorite = newFav)
            }
            _statusMessage.value = if (newFav) "Added to Favorites ⭐" else "Removed from Favorites"
        }
    }

    fun updateCallerNoteAndTag(phoneNumber: String, note: String?, tag: String?) {
        viewModelScope.launch {
            repository.updateNoteAndTag(phoneNumber, note, tag)
            if (_activeCaller.value?.phoneNumber == phoneNumber) {
                _activeCaller.value = _activeCaller.value?.copy(note = note, tag = tag)
            }
            _statusMessage.value = "Updated caller note and tag"
        }
    }

    fun blockNumber(phoneNumber: String, block: Boolean = true) {
        viewModelScope.launch {
            repository.setBlockedStatus(phoneNumber, block)
            _statusMessage.value = if (block) "Number $phoneNumber added to blocked list" else "Number $phoneNumber unblocked"
            if (_activeCaller.value?.phoneNumber == phoneNumber) {
                _activeCaller.value = _activeCaller.value?.copy(isBlocked = block)
            }
        }
    }

    fun reportSpam(phoneNumber: String, reason: String, callerName: String? = null) {
        viewModelScope.launch {
            repository.reportSpam(phoneNumber, reason, callerName, "Community Report")
            _statusMessage.value = "Reported $phoneNumber as spam: $reason"
            lookupCaller(phoneNumber)
        }
    }

    fun saveScannedNumber(result: ScanResult) {
        viewModelScope.launch {
            repository.saveScannedNumber(result)
            _statusMessage.value = "Saved ${result.formattedNumber} to Scan Vault"
        }
    }

    fun saveScannedNumbers(results: List<ScanResult>) {
        viewModelScope.launch {
            repository.saveScannedNumbers(results)
            _statusMessage.value = "Saved ${results.size} numbers to Scan Vault"
        }
    }

    fun deleteScannedNumber(phoneNumber: String) {
        viewModelScope.launch {
            repository.deleteScannedNumber(phoneNumber)
            _statusMessage.value = "Removed from Scan Vault"
        }
    }

    fun clearScanHistory() {
        viewModelScope.launch {
            repository.clearScanHistory()
            _statusMessage.value = "Scan Vault cleared"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun deleteCallRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteCallRecord(id)
        }
    }

    fun clearAllCallHistory() {
        viewModelScope.launch {
            repository.clearCallRecords()
            _statusMessage.value = "Call history cleared"
        }
    }
}
