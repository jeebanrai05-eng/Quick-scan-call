package com.example.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale

/**
 * Validates downloaded update APK files for security and integrity before passing to Package Installer:
 * 1. Checks that the APK's packageName matches the running app's packageName.
 * 2. Checks that the APK's versionCode is strictly greater than or equal to installed version (prevents downgrade attacks).
 * 3. Verifies that the APK is signed by the same signing certificate as the currently installed app.
 * 4. Checks SHA-256 hash if provided in the update manifest.
 */
object ApkVerifier {

    private const val TAG = "ApkVerifier"

    sealed class VerificationResult {
        data object Valid : VerificationResult()
        data class Error(val reason: String) : VerificationResult()
    }

    /**
     * Verifies the APK file at [apkFile].
     */
    fun verifyApk(
        context: Context,
        apkFile: File,
        expectedSha256: String? = null
    ): VerificationResult {
        if (!apkFile.exists() || apkFile.length() <= 0) {
            return VerificationResult.Error("Downloaded APK file is empty or missing.")
        }

        val pm = context.packageManager

        // Read package archive info
        val archiveInfo: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_SIGNATURES
            )
        }

        if (archiveInfo == null) {
            return VerificationResult.Error("Failed to parse downloaded APK file. The archive may be corrupt.")
        }

        // 1. Verify Package Name
        val currentPackageName = context.packageName
        val apkPackageName = archiveInfo.packageName
        if (apkPackageName != currentPackageName) {
            return VerificationResult.Error(
                "Package name mismatch! Expected: $currentPackageName, APK: $apkPackageName"
            )
        }

        // 2. Verify Version Code (prevent accidental downgrade)
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val currentPkgInfo = pm.getPackageInfo(currentPackageName, 0)
            currentPkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(currentPackageName, 0).versionCode.toLong()
        }

        val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }

        if (apkVersionCode < currentVersionCode) {
            return VerificationResult.Error(
                "Downgrade rejected. Installed version: $currentVersionCode, APK version: $apkVersionCode"
            )
        }

        // 3. Cryptographic Signing Certificate Verification (Strict signature match)
        val signatureMatch = verifySignaturesMatch(context, archiveInfo)
        if (!signatureMatch) {
            val currentFps = getSignaturesForPackage(pm, currentPackageName).map { it.sha256Fingerprint() }
            val apkFps = getSignaturesFromPackageInfo(archiveInfo).map { it.sha256Fingerprint() }
            return VerificationResult.Error(
                "Security verification failed: Signing certificate mismatch!\n" +
                "Installed app certificate: ${currentFps.joinToString(", ")}\n" +
                "Downloaded APK certificate: ${apkFps.joinToString(", ")}"
            )
        }

        // 4. APK File Checksum Verification (Strictly separate from signing certificate)
        if (!expectedSha256.isNullOrBlank()) {
            val fileHash = calculateSha256(apkFile)
            if (!fileHash.equals(expectedSha256.trim(), ignoreCase = true)) {
                return VerificationResult.Error(
                    "APK file integrity check failed! Expected SHA-256: $expectedSha256, Computed: $fileHash"
                )
            }
        }

        return VerificationResult.Valid
    }

    private fun verifySignaturesMatch(context: Context, apkInfo: PackageInfo): Boolean {
        return try {
            val pm = context.packageManager
            val currentSignatures = getSignaturesForPackage(pm, context.packageName)
            val apkSignatures = getSignaturesFromPackageInfo(apkInfo)

            if (currentSignatures.isEmpty() || apkSignatures.isEmpty()) {
                Log.w(TAG, "Empty signatures found during verification")
                return false
            }

            // Check if at least one certificate fingerprint matches
            val currentCertFingerprints = currentSignatures.map { it.sha256Fingerprint() }.toSet()
            val apkCertFingerprints = apkSignatures.map { it.sha256Fingerprint() }.toSet()

            val matches = currentCertFingerprints.intersect(apkCertFingerprints).isNotEmpty()
            if (!matches) {
                Log.e(TAG, "Fingerprint mismatch. Current: $currentCertFingerprints vs APK: $apkCertFingerprints")
            }
            matches
        } catch (e: Exception) {
            Log.e(TAG, "Error checking signatures", e)
            false
        }
    }

    fun getInstalledSigningFingerprints(context: Context): List<String> {
        return try {
            getSignaturesForPackage(context.packageManager, context.packageName)
                .map { it.sha256Fingerprint() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getSignaturesForPackage(pm: PackageManager, packageName: String): List<ByteArray> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val pkgInfo = pm.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
            val signingInfo = pkgInfo.signingInfo
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.map { it.toByteArray() }
                } else {
                    signingInfo.signingCertificateHistory.map { it.toByteArray() }
                }
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = pkgInfo.signingInfo
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.map { it.toByteArray() }
                } else {
                    signingInfo.signingCertificateHistory.map { it.toByteArray() }
                }
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            }
        } else {
            @Suppress("DEPRECATION")
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            pkgInfo.signatures?.map { it.toByteArray() } ?: emptyList()
        }
    }

    private fun getSignaturesFromPackageInfo(apkInfo: PackageInfo): List<ByteArray> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = apkInfo.signingInfo
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.map { it.toByteArray() }
                } else {
                    signingInfo.signingCertificateHistory.map { it.toByteArray() }
                }
            } else {
                @Suppress("DEPRECATION")
                apkInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            }
        } else {
            @Suppress("DEPRECATION")
            apkInfo.signatures?.map { it.toByteArray() } ?: emptyList()
        }
    }

    private fun ByteArray.sha256Fingerprint(): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(this)
        return digest.joinToString(":") { "%02X".format(it) }
    }

    private fun calculateSha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        val digest = md.digest()
        return digest.joinToString("") { "%02x".format(it) }
    }
}
