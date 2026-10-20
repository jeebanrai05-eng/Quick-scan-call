package com.example.utils

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Handles the secure Android Package Installer flow:
 * 1. Checks if the app has permission to install unknown apps (Android 8.0+).
 * 2. If not, guides the user to the system Settings screen to grant it.
 * 3. Uses FileProvider and Intent.ACTION_VIEW with FLAG_GRANT_READ_URI_PERMISSION to launch the installer safely.
 * 4. Provides helper to copy the downloaded APK to the user's public Downloads directory
 *    so it survives uninstallation of the old debug app.
 */
object ApkInstaller {

    private const val TAG = "ApkInstaller"

    /**
     * Checks if the app can install packages (Android 8.0+ O_MR1 / O).
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Creates an Intent to prompt the user to grant "Install unknown apps" permission for this app.
     */
    fun createInstallPermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Launches the Android Package Installer for the specified verified [apkFile].
     * Never silently installs; delegates to Android's built-in package manager UI.
     */
    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(IllegalStateException("APK file does not exist at ${apkFile.absolutePath}"))
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Verify there is an activity to handle this intent
            val resolvedActivities = context.packageManager.queryIntentActivities(installIntent, 0)
            if (resolvedActivities.isEmpty()) {
                Log.w(TAG, "No package installer activity directly resolved, attempting standard launch")
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start APK installation", e)
            Result.failure(e)
        }
    }

    /**
     * Exports the downloaded APK to the public Downloads folder so the user can easily install it
     * even after uninstalling the previous debug version.
     */
    fun saveApkToDownloads(
        context: Context,
        apkFile: File,
        outputFileName: String = "QuickCallScan-v1.0.5.apk"
    ): Result<Uri> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(IllegalStateException("Source APK file not found"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, outputFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(IllegalStateException("Failed to create Downloads entry"))
                resolver.openOutputStream(uri)?.use { out ->
                    apkFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                Result.success(uri)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, outputFileName)
                apkFile.copyTo(targetFile, overwrite = true)
                Result.success(Uri.fromFile(targetFile))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy APK to Downloads", e)
            Result.failure(e)
        }
    }

    /**
     * Opens the device's Downloads directory or download manager.
     */
    fun openDownloadsFolder(context: Context) {
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(downloadsDir.path), "*/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Log.w(TAG, "Could not launch Downloads app directly")
            }
        }
    }
}
