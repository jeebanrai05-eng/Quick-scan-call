package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.model.AppUpdateInfo
import com.example.utils.ApkVerifier
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class DownloadProgress {
    data class Progress(val bytesDownloaded: Long, val totalBytes: Long, val percent: Int) : DownloadProgress()
    data class Completed(val apkFile: File) : DownloadProgress()
    data class Error(val error: String) : DownloadProgress()
}

class UpdateRepository(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val TAG = "UpdateRepository"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(AppUpdateInfo::class.java)

    /**
     * Default public HTTPS URL for version.json.
     * Can be replaced or configured in Settings.
     */
    var updateUrl: String = DEFAULT_VERSION_URL

    companion object {
        const val LATEST_RELEASE_URL =
            "https://github.com/jeebanrai05-eng/Quick-scan-call/releases/latest/download/version.json"
        const val RAW_GITHUB_URL =
            "https://raw.githubusercontent.com/jeebanrai05-eng/Quick-scan-call/main/version.json"
        const val GITHUB_PAGES_URL =
            "https://jeebanrai05-eng.github.io/Quick-scan-call/version.json"
        const val DEFAULT_VERSION_URL = LATEST_RELEASE_URL
    }

    /**
     * Gets currently installed app versionCode.
     */
    fun getCurrentVersionCode(): Long {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0).versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    /**
     * Gets currently installed app versionName.
     */
    fun getCurrentVersionName(): String {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    /**
     * Fetches version.json from the remote endpoint with automatic fallback.
     */
    suspend fun checkForUpdate(customUrl: String? = null): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        val candidateUrls = if (!customUrl.isNullOrBlank()) {
            listOf(customUrl)
        } else {
            listOf(updateUrl, LATEST_RELEASE_URL, RAW_GITHUB_URL, GITHUB_PAGES_URL).distinct()
        }

        var lastError: Exception? = null
        for (targetUrl in candidateUrls) {
            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("Accept", "application/json")
                    .header("User-Agent", "QuickCallScan-Android/${getCurrentVersionName()}")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val updateInfo = adapter.fromJson(body)
                        if (updateInfo != null) {
                            return@withContext Result.success(updateInfo)
                        }
                    }
                } else {
                    lastError = IllegalStateException("HTTP ${response.code} from $targetUrl: ${response.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed update check from $targetUrl: ${e.message}")
                lastError = e
            }
        }
        Result.failure(lastError ?: IllegalStateException("Unable to fetch update info from configured endpoints"))
    }

    /**
     * Downloads the APK file from [apkUrl] with real-time progress reporting.
     * Emits [DownloadProgress].
     */
    fun downloadApk(
        apkUrl: String,
        expectedSha256: String? = null
    ): Flow<DownloadProgress> = flow {
        val updateDir = File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
        }
        val destinationFile = File(updateDir, "update_${System.currentTimeMillis()}.apk")

        try {
            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "QuickCallScan-Android/${getCurrentVersionName()}")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = if (response.code == 404) {
                    "Release APK not yet published on GitHub (HTTP 404). Please ensure the GitHub Actions release workflow for this version has completed and published app-release.apk to GitHub Releases."
                } else {
                    "Download failed with HTTP ${response.code}: ${response.message}"
                }
                emit(DownloadProgress.Error(errorMsg))
                return@flow
            }

            val responseBody = response.body
            if (responseBody == null) {
                emit(DownloadProgress.Error("Server returned empty body"))
                return@flow
            }

            val contentLength = responseBody.contentLength()
            var bytesDownloaded = 0L

            responseBody.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    var lastPercent = -1

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesDownloaded += read

                        val percent = if (contentLength > 0) {
                            ((bytesDownloaded * 100) / contentLength).toInt().coerceIn(0, 100)
                        } else {
                            -1
                        }

                        if (percent != lastPercent) {
                            lastPercent = percent
                            emit(DownloadProgress.Progress(bytesDownloaded, contentLength, percent))
                        }
                    }
                    output.flush()
                }
            }

            // Verify integrity and authenticity before completing
            when (val verification = ApkVerifier.verifyApk(context, destinationFile, expectedSha256)) {
                is ApkVerifier.VerificationResult.Valid -> {
                    emit(DownloadProgress.Completed(destinationFile))
                }
                is ApkVerifier.VerificationResult.Error -> {
                    // Security precaution: delete invalid APK immediately
                    if (destinationFile.exists()) {
                        destinationFile.delete()
                    }
                    emit(DownloadProgress.Error("Security verification failed: ${verification.reason}"))
                }
            }
        } catch (e: Exception) {
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            emit(DownloadProgress.Error("Download interrupted: ${e.localizedMessage ?: e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Cleans up older APK update files from the cache directory.
     */
    suspend fun clearCachedUpdates() = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.cacheDir, "updates")
            if (updateDir.exists() && updateDir.isDirectory) {
                updateDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean old update APKs", e)
        }
    }
}
