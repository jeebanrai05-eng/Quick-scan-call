package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.QuickCallApp
import com.example.data.repository.CallerRepository
import com.example.utils.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val floatingOverlayEnabled: Boolean = true,
    val autoOpenDialerEnabled: Boolean = false,
    val strictSpamBlocking: Boolean = true,
    val showSpamHud: Boolean = true,
    val hasOverlayPermission: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val hasPhonePermission: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val isSyncing: Boolean = false,
    val message: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CallerRepository = (application as QuickCallApp).repository
    private val prefs = application.getSharedPreferences("quick_call_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            floatingOverlayEnabled = prefs.getBoolean("floating_overlay", true),
            autoOpenDialerEnabled = prefs.getBoolean("auto_open_dialer", false),
            strictSpamBlocking = prefs.getBoolean("strict_spam_blocking", true),
            showSpamHud = prefs.getBoolean("show_spam_hud", true)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshPermissionStates()
    }

    fun refreshPermissionStates() {
        val app = getApplication<Application>()
        _uiState.value = _uiState.value.copy(
            hasOverlayPermission = PermissionHelper.canDrawOverlays(app),
            hasCameraPermission = PermissionHelper.hasCameraPermission(app),
            hasPhonePermission = PermissionHelper.hasPhonePermissions(app),
            hasContactsPermission = PermissionHelper.hasContactsPermission(app),
            hasNotificationPermission = PermissionHelper.hasNotificationPermission(app)
        )
    }

    fun setFloatingOverlay(enabled: Boolean) {
        prefs.edit().putBoolean("floating_overlay", enabled).apply()
        _uiState.value = _uiState.value.copy(floatingOverlayEnabled = enabled)
    }

    fun setAutoOpenDialer(enabled: Boolean) {
        prefs.edit().putBoolean("auto_open_dialer", enabled).apply()
        _uiState.value = _uiState.value.copy(autoOpenDialerEnabled = enabled)
    }

    fun setStrictSpamBlocking(enabled: Boolean) {
        prefs.edit().putBoolean("strict_spam_blocking", enabled).apply()
        _uiState.value = _uiState.value.copy(strictSpamBlocking = enabled)
    }

    fun setShowSpamHud(enabled: Boolean) {
        prefs.edit().putBoolean("show_spam_hud", enabled).apply()
        _uiState.value = _uiState.value.copy(showSpamHud = enabled)
    }

    fun syncDeviceData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, message = "Syncing real contacts and call records from device...")
            val contacts = repository.syncDeviceContacts()
            val calls = repository.syncSystemCallLogs()
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                message = "Synced $contacts contacts and $calls call records from device"
            )
        }
    }

    fun dismissFloatingOverlay() {
        val app = getApplication<Application>()
        com.example.service.CallDetectionManager.dismissFloatingOverlay(app)
        _uiState.value = _uiState.value.copy(message = "Floating overlay dismissed")
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
