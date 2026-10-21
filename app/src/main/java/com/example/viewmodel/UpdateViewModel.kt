package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
    data class ForceUpdateRequired(val updateInfo: AppUpdateInfo, val currentVersion: String) : UpdateStatus()
    data class Downloading(val percent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File, val updateInfo: AppUpdateInfo) : UpdateStatus()
    data class DebugToReleaseNotice(val apkFile: File, val updateInfo: AppUpdateInfo, val releaseCert: String) : UpdateStatus()
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
                    val isForce = currentCode < info.minimumSupportedVersion
                    val newStatus = if (isForce) {
                        UpdateStatus.ForceUpdateRequired(
                            updateInfo = info,
                            currentVersion = repository.getCurrentVersionName()
                        )
                    } else {
                        UpdateStatus.UpdateAvailable(
                            updateInfo = info,
                            currentVersion = repository.getCurrentVersionName()
                        )
                    }
                    _uiState.update { it.copy(status = newStatus) }

                    // Post system notification to notify user of the update
                    com.example.utils.UpdateNotificationHelper.showUpdateNotification(
                        context = getApplication(),
                        updateInfo = info,
                        isForceUpdate = isForce
                    )
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
                    is DownloadProgress.DebugToReleaseReady -> {
                        val apkFile = progress.apkFile
                        _uiState.update {
                            it.copy(
                                status = UpdateStatus.DebugToReleaseNotice(
                                    apkFile = apkFile,
                                    updateInfo = updateInfo,
                                    releaseCert = progress.releaseCert
                                )
                            )
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
     * Saves the downloaded APK to the public Downloads folder so it is preserved
     * even if the user uninstalls the current debug version of the app.
     */
    fun saveApkToDownloads(apkFile: File, versionName: String): Boolean {
        val context = getApplication<Application>().applicationContext
        val fileName = "QuickCallScan-v$versionName.apk"
        val result = ApkInstaller.saveApkToDownloads(context, apkFile, fileName)
        return if (result.isSuccess) {
            Toast.makeText(context, "APK saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
            true
        } else {
            Toast.makeText(context, "Failed to save APK: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Opens the device's Downloads folder.
     */
    fun openDownloads() {
        val context = getApplication<Application>().applicationContext
        ApkInstaller.openDownloadsFolder(context)
    }

    /**
     * Opens the GitHub release page directly in the device's default web browser.
     */
    fun openReleaseInBrowser(apkUrl: String) {
        val context = getApplication<Application>().applicationContext
        try {
            val url = if (apkUrl.contains("/download/")) {
                apkUrl.substringBefore("/download/")
            } else {
                apkUrl
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
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
