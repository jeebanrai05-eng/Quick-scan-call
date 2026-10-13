package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppUpdateInfo
import com.example.data.repository.DownloadProgress
import com.example.data.repository.UpdateRepository
import com.example.utils.ApkInstaller
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed class UpdateStatus {
    data object Idle : UpdateStatus()
    data object Checking : UpdateStatus()
    data class UpToDate(val currentVersion: String, val lastCheckedTime: Long) : UpdateStatus()
    data class UpdateAvailable(val updateInfo: AppUpdateInfo, val currentVersion: String) : UpdateStatus()
    data class Downloading(val percent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File, val updateInfo: AppUpdateInfo) : UpdateStatus()
    data class PermissionRequired(val apkFile: File) : UpdateStatus()
    data class Error(val message: String, val canRetry: Boolean = true) : UpdateStatus()
}

data class UpdateUiState(
    val status: UpdateStatus = UpdateStatus.Idle,
    val currentVersionName: String = "",
    val currentVersionCode: Long = 1L,
    val endpointUrl: String = UpdateRepository.DEFAULT_VERSION_URL,
    val isCustomEndpoint: Boolean = false,
    val isCheckingAutomatically: Boolean = false
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UpdateRepository(application.applicationContext)
    private var downloadJob: Job? = null

    private val _uiState = MutableStateFlow(
        UpdateUiState(
            currentVersionName = repository.getCurrentVersionName(),
            currentVersionCode = repository.getCurrentVersionCode()
        )
    )
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    fun updateEndpointUrl(newUrl: String) {
        repository.updateUrl = newUrl.trim()
        _uiState.update {
            it.copy(
                endpointUrl = newUrl.trim(),
                isCustomEndpoint = newUrl.trim() != UpdateRepository.DEFAULT_VERSION_URL
            )
        }
    }

    /**
     * Checks remote version.json for updates.
     * @param silent If true, suppresses modal if already up-to-date to avoid annoying repeated popups.
     */
    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = UpdateStatus.Checking) }

            val result = repository.checkForUpdate()
            result.onSuccess { info ->
                val currentCode = repository.getCurrentVersionCode()
                if (info.versionCode > currentCode) {
                    _uiState.update {
                        it.copy(
                            status = UpdateStatus.UpdateAvailable(
                                updateInfo = info,
                                currentVersion = repository.getCurrentVersionName()
                            )
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            status = UpdateStatus.UpToDate(
                                currentVersion = repository.getCurrentVersionName(),
                                lastCheckedTime = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        status = UpdateStatus.Error(
                            message = exception.localizedMessage ?: "Failed to connect to update server. Check your network.",
                            canRetry = true
                        )
                    )
                }
            }
        }
    }

    /**
     * Starts downloading the update APK.
     */
    fun startDownload(updateInfo: AppUpdateInfo) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(status = UpdateStatus.Downloading(percent = 0, downloadedBytes = 0, totalBytes = updateInfo.fileSize))
            }

            repository.downloadApk(updateInfo.apkUrl, updateInfo.sha256).collect { progress ->
                when (progress) {
                    is DownloadProgress.Progress -> {
                        _uiState.update {
                            it.copy(
                                status = UpdateStatus.Downloading(
                                    percent = progress.percent,
                                    downloadedBytes = progress.bytesDownloaded,
                                    totalBytes = progress.totalBytes
                                )
                            )
                        }
                    }
                    is DownloadProgress.Completed -> {
                        val apkFile = progress.apkFile
                        // Check if Android has permission to install unknown apps
                        if (ApkInstaller.canRequestPackageInstalls(getApplication())) {
                            _uiState.update {
                                it.copy(status = UpdateStatus.ReadyToInstall(apkFile, updateInfo))
                            }
                        } else {
                            _uiState.update {
                                it.copy(status = UpdateStatus.PermissionRequired(apkFile))
                            }
                        }
                    }
                    is DownloadProgress.Error -> {
                        _uiState.update {
                            it.copy(status = UpdateStatus.Error(progress.error, canRetry = true))
                        }
                    }
                }
            }
        }
    }

    /**
     * Cancels any in-progress APK download.
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        viewModelScope.launch {
            repository.clearCachedUpdates()
            _uiState.update { it.copy(status = UpdateStatus.Idle) }
        }
    }

    /**
     * Triggers the Android Package Installer for the prepared APK.
     */
    fun proceedToInstall(apkFile: File) {
        val context = getApplication<Application>().applicationContext
        if (!ApkInstaller.canRequestPackageInstalls(context)) {
            _uiState.update { it.copy(status = UpdateStatus.PermissionRequired(apkFile)) }
            return
        }

        val result = ApkInstaller.installApk(context, apkFile)
        result.onFailure { error ->
            _uiState.update {
                it.copy(status = UpdateStatus.Error("Installation failed: ${error.localizedMessage ?: error.message}", canRetry = false))
            }
        }
    }

    /**
     * Rechecks permission state once user returns from Settings.
     */
    fun onResumeFromPermissionSettings(apkFile: File, updateInfo: AppUpdateInfo) {
        if (ApkInstaller.canRequestPackageInstalls(getApplication())) {
            _uiState.update { it.copy(status = UpdateStatus.ReadyToInstall(apkFile, updateInfo)) }
        }
    }

    /**
     * Dismisses the update dialog or status back to Idle.
     */
    fun dismiss() {
        _uiState.update { it.copy(status = UpdateStatus.Idle) }
    }
}
