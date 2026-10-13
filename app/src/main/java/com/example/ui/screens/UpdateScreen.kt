package com.example.ui.screens

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppUpdateInfo
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGlassCard
import com.example.ui.components.NeonOutlinedButton
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextOnNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.utils.ApkInstaller
import com.example.viewmodel.UpdateStatus
import com.example.viewmodel.UpdateViewModel

@Composable
fun UpdateScreen(
    updateViewModel: UpdateViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by updateViewModel.uiState.collectAsStateWithLifecycle()
    var isEditingEndpoint by remember { mutableStateOf(false) }
    var endpointInput by remember(uiState.endpointUrl) { mutableStateOf(uiState.endpointUrl) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .testTag("update_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(DarkSurfaceVariant, CircleShape)
                    .border(1.dp, DarkGlassBorder, CircleShape)
                    .testTag("update_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "SOFTWARE UPDATE",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "GitHub Release Updater",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Installed Version Hero Card
        NeonGlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current Installed Version",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "v${uiState.currentVersionName}",
                            color = NeonGreen,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(build #${uiState.currentVersionCode})",
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberCyan.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VERIFIED",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Status Card
        when (val status = uiState.status) {
            is UpdateStatus.Idle -> {
                IdleStatusCard(
                    onCheckClick = { updateViewModel.checkForUpdates() }
                )
            }
            is UpdateStatus.Checking -> {
                CheckingStatusCard()
            }
            is UpdateStatus.UpToDate -> {
                UpToDateStatusCard(
                    version = status.currentVersion,
                    onRecheck = { updateViewModel.checkForUpdates() }
                )
            }
            is UpdateStatus.UpdateAvailable -> {
                UpdateAvailableCard(
                    updateInfo = status.updateInfo,
                    onDownloadClick = { updateViewModel.startDownload(status.updateInfo) }
                )
            }
            is UpdateStatus.Downloading -> {
                DownloadingCard(
                    percent = status.percent,
                    downloadedBytes = status.downloadedBytes,
                    totalBytes = status.totalBytes,
                    onCancel = { updateViewModel.cancelDownload() }
                )
            }
            is UpdateStatus.ReadyToInstall -> {
                ReadyToInstallCard(
                    updateInfo = status.updateInfo,
                    onInstallClick = { updateViewModel.proceedToInstall(status.apkFile) }
                )
            }
            is UpdateStatus.PermissionRequired -> {
                PermissionRequiredCard(
                    onGrantClick = {
                        val intent = ApkInstaller.createInstallPermissionIntent(context)
                        context.startActivity(intent)
                    }
                )
            }
            is UpdateStatus.Error -> {
                ErrorStatusCard(
                    error = status.message,
                    canRetry = status.canRetry,
                    onRetry = { updateViewModel.checkForUpdates() }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Server Endpoint Configuration
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Release Channel & Endpoint",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GitHub Pages / Raw Repository Manifest",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    NeonOutlinedButton(
                        onClick = {
                            if (isEditingEndpoint) {
                                updateViewModel.updateEndpointUrl(endpointInput)
                            }
                            isEditingEndpoint = !isEditingEndpoint
                        },
                        borderColor = CyberCyan,
                        contentColor = CyberCyan,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isEditingEndpoint) "Done" else "Configure",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isEditingEndpoint) {
                    OutlinedTextField(
                        value = endpointInput,
                        onValueChange = { endpointInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("version.json URL", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkGlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                endpointInput = com.example.data.repository.UpdateRepository.DEFAULT_VERSION_URL
                                updateViewModel.updateEndpointUrl(endpointInput)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                        ) {
                            Text("Reset Default", fontSize = 11.sp)
                        }
                    }
                } else {
                    Text(
                        text = uiState.endpointUrl,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Security Assurance Details
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Security & Integrity Guarantees",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                SecurityBulletPoint(text = "Zero Secret Embedding: GitHub tokens are never baked into this app.")
                SecurityBulletPoint(text = "Package Integrity: Verified for matching applicationId before install.")
                SecurityBulletPoint(text = "Downgrade Protection: Blocks older or malicious APK versions.")
                SecurityBulletPoint(text = "Keystore Signature Match: Requires identical APK signing certificates.")
                SecurityBulletPoint(text = "Standard Android Flow: User confirms installation in system package manager.")
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun IdleStatusCard(onCheckClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Check for New Releases",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap below to query your GitHub repository for updates and bug fixes.",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCheckClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkBackground),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("check_updates_now_button")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Check for Updates Now", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun CheckingStatusCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = CyberCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connecting to GitHub Releases...",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Checking version.json manifest",
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun UpToDateStatusCard(version: String, onRecheck: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "You're on the Latest Version!",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Quick Call Scan & Detect Caller v$version is completely up to date.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            NeonOutlinedButton(
                onClick = onRecheck,
                borderColor = CyberCyan,
                contentColor = CyberCyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check Again", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun UpdateAvailableCard(
    updateInfo: AppUpdateInfo,
    onDownloadClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "New Update Available!",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Version ${updateInfo.versionName} (Build #${updateInfo.versionCode})",
                            color = NeonGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = DarkGlassBorder)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "What's New in this Release:",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = updateInfo.releaseNotes.ifBlank { "Performance improvements and bug fixes." },
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDownloadClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = TextOnNeon),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("download_update_button")
            ) {
                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download & Install Update", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun DownloadingCard(
    percent: Int,
    downloadedBytes: Long,
    totalBytes: Long,
    onCancel: () -> Unit
) {
    val animatedPercent by animateFloatAsState(
        targetValue = if (percent >= 0) percent / 100f else 0f,
        label = "DownloadProgress"
    )

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Downloading APK...",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (totalBytes > 0) {
                            "${downloadedBytes / (1024 * 1024)} MB of ${totalBytes / (1024 * 1024)} MB"
                        } else {
                            "${downloadedBytes / 1024} KB downloaded"
                        },
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = if (percent >= 0) "$percent%" else "...",
                    color = CyberCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { if (percent >= 0) animatedPercent else 0.5f },
                color = CyberCyan,
                trackColor = DarkSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SpamRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpamRed.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel Download", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ReadyToInstallCard(
    updateInfo: AppUpdateInfo,
    onInstallClick: () -> Unit
) {
    NeonGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(46.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "APK Verified & Ready!",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cryptographic signature matches this application. Tap to launch the Android Package Installer.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onInstallClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = TextOnNeon),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("install_now_button")
            ) {
                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Package Installer Now", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun PermissionRequiredCard(onGrantClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(46.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Install Unknown Apps Permission",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Android requires your permission to install APK updates directly from Quick Call Scan. Tap below to enable 'Allow from this source'.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = DarkBackground),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("grant_install_permission_button")
            ) {
                Text("Open Android Settings to Allow", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun ErrorStatusCard(
    error: String,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = SpamRed,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Update Check Failed",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (canRetry) {
                Spacer(modifier = Modifier.height(16.dp))
                NeonOutlinedButton(
                    onClick = onRetry,
                    borderColor = CyberCyan,
                    contentColor = CyberCyan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try Again", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SecurityBulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
