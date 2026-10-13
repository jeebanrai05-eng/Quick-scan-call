package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGlassCard
import com.example.ui.components.NeonOutlinedButton
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.utils.PermissionHelper
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.UpdateViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel? = null,
    onNavigateToUpdate: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val updateUiState = updateViewModel?.uiState?.collectAsStateWithLifecycle()?.value

    LaunchedEffect(Unit) {
        settingsViewModel.refreshPermissionStates()
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            settingsViewModel.clearMessage()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "PREFERENCES & PERMISSIONS",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "App Settings",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // 1. Overlay & Caller Protection Settings
        item {
            SettingsSectionHeader(title = "Caller ID & Overlay Shield")
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Floating Overlay Switch
                    SettingsToggleRow(
                        title = "Floating Popup Overlay",
                        subtitle = "Display Truecaller-style window on top of incoming calls",
                        icon = Icons.Default.Layers,
                        isChecked = uiState.floatingOverlayEnabled,
                        onCheckedChange = { settingsViewModel.setFloatingOverlay(it) }
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Auto Open Dialer Switch
                    SettingsToggleRow(
                        title = "Auto Open Dialer on Scan",
                        subtitle = "Immediately load dialed number into phone dialer",
                        icon = Icons.Default.PhoneInTalk,
                        isChecked = uiState.autoOpenDialerEnabled,
                        onCheckedChange = { settingsViewModel.setAutoOpenDialer(it) }
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Strict Spam Blocking
                    SettingsToggleRow(
                        title = "Strict Spam Blocking",
                        subtitle = "Silently block callers with fraud rating > 80%",
                        icon = Icons.Default.Security,
                        isChecked = uiState.strictSpamBlocking,
                        onCheckedChange = { settingsViewModel.setStrictSpamBlocking(it) }
                    )
                }
            }
        }

        // 2. System Permissions Status
        item {
            SettingsSectionHeader(title = "System Permissions Status")
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PermissionStatusItem(
                        name = "Display Over Other Apps",
                        description = "Required for the floating caller overlay",
                        icon = Icons.Default.Layers,
                        isGranted = uiState.hasOverlayPermission,
                        onFixClick = { PermissionHelper.openOverlaySettings(context) }
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    PermissionStatusItem(
                        name = "Camera Hardware Access",
                        description = "Required for live OCR number recognition",
                        icon = Icons.Default.CameraAlt,
                        isGranted = uiState.hasCameraPermission,
                        onFixClick = { PermissionHelper.openAppSettings(context) }
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    PermissionStatusItem(
                        name = "Phone State & Call Log",
                        description = "Required to detect incoming callers and log history",
                        icon = Icons.Default.PhoneInTalk,
                        isGranted = uiState.hasPhonePermission,
                        onFixClick = { PermissionHelper.openAppSettings(context) }
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    PermissionStatusItem(
                        name = "Contacts Directory Access",
                        description = "Matches incoming calls against your saved contacts",
                        icon = Icons.Default.ContactPhone,
                        isGranted = uiState.hasContactsPermission,
                        onFixClick = { PermissionHelper.openAppSettings(context) }
                    )
                }
            }
        }

        // 3. Offline Intelligence Database
        item {
            SettingsSectionHeader(title = "Offline Intelligence Database")
        }

        item {
            NeonGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "100% Privacy Protected",
                        color = NeonGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your contact numbers and scanned images never leave your device. All OCR detection and spam threat analysis runs offline on your processor.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NeonOutlinedButton(
                        onClick = { settingsViewModel.syncDeviceData() },
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = NeonGreen
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sync Device Call Logs & Contacts",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. System Shortcuts & Widget Integration
        item {
            SettingsSectionHeader(title = "OUTSIDE SHORTCUTS & WIDGET (बाहिर Shortcut)")
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Home Screen Pin Shortcut Button
                    NeonOutlinedButton(
                        onClick = { com.example.utils.ShortcutHelper.pinQuickScannerShortcut(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📌", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pin Quick Scan to Home Screen",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = DarkGlassBorder)

                    // 2. App Icon Long-Press Shortcut Tip
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📱", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "App Icon Long-Press Shortcuts",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Long press app icon on phone screen to launch 'Quick Scan' or 'T9 Dialer' directly.",
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    HorizontalDivider(color = DarkGlassBorder)

                    // 3. Quick Settings Notification Tile Tip
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "System Quick Settings Tile",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pull down Android notification shade and tap Edit to add the 'Quick Scan' tile for 1-tap scan from anywhere!",
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    HorizontalDivider(color = DarkGlassBorder)

                    // 4. Desktop App Widget Tip
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧩", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Phone Desktop Widget",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pinch or hold your phone Home Screen, choose Widgets, and place the 'Quick OCR Scan' widget.",
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }

        // 5. Software Updates & GitHub Releases
        item {
            SettingsSectionHeader(title = "Software Updates & Releases")
        }

        item {
            NeonGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GitHub Self-Updater",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val currentVer = updateUiState?.currentVersionName ?: "1.0.1"
                                Text(
                                    text = "Installed: v$currentVer • Automated release checks",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        val status = updateUiState?.status
                        if (status is com.example.viewmodel.UpdateStatus.UpdateAvailable) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NeonGreen.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen)
                            ) {
                                Text(
                                    text = "v${status.updateInfo.versionName} Available!",
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkGlassBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeonOutlinedButton(
                            onClick = onNavigateToUpdate,
                            borderColor = NeonGreen,
                            contentColor = NeonGreen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_check_updates_button")
                        ) {
                            Text("Check for Updates →", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 6. App Info
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val verName = updateUiState?.currentVersionName ?: "1.0.1"
                    val verCode = updateUiState?.currentVersionCode ?: 2L
                    Text(
                        text = "Quick Call Scan & Detect Caller",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "v$verName (build #$verCode) • ML Kit OCR • Room DB • AMOLED Dark",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isChecked) NeonGreen else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonGreen,
                checkedTrackColor = Color(0xFF0F2E1E),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}

@Composable
private fun PermissionStatusItem(
    name: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onFixClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) CyberCyan else WarningAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        if (isGranted) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = NeonGreen.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "GRANTED",
                    color = NeonGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        } else {
            Surface(
                onClick = onFixClick,
                shape = RoundedCornerShape(6.dp),
                color = WarningAmber.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber)
            ) {
                Text(
                    text = "GRANT →",
                    color = WarningAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
