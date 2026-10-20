package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallRecord
import com.example.data.model.CallType
import com.example.data.model.ScanSource
import com.example.ui.components.CyberT9Keypad
import com.example.ui.components.FavoritesCarousel
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGlassCard
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.SmartAnalyticsCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenGlow
import com.example.ui.theme.SpamRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextOnNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.utils.PermissionHelper
import com.example.utils.PhoneActionHelper
import com.example.viewmodel.CallerViewModel
import com.example.viewmodel.ScannerViewModel

import com.example.viewmodel.UpdateStatus
import com.example.viewmodel.UpdateViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    callerViewModel: CallerViewModel,
    scannerViewModel: ScannerViewModel,
    updateViewModel: UpdateViewModel? = null,
    onNavigateToScanner: (ScanSource) -> Unit,
    onNavigateToCallerPopup: (String, String) -> Unit,
    onNavigateToRecents: () -> Unit,
    onNavigateToUpdate: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val recentCalls by callerViewModel.recentCalls.collectAsStateWithLifecycle()
    val blockedCallers by callerViewModel.blockedCallers.collectAsStateWithLifecycle()
    val favorites by callerViewModel.favoriteCallers.collectAsStateWithLifecycle()
    val analytics by callerViewModel.analytics.collectAsStateWithLifecycle()
    val dialerInput by callerViewModel.dialerInput.collectAsStateWithLifecycle()
    val t9Results by callerViewModel.t9Results.collectAsStateWithLifecycle()
    val isSyncing by callerViewModel.isSyncing.collectAsStateWithLifecycle()
    val statusMessage by callerViewModel.statusMessage.collectAsStateWithLifecycle()

    val updateUiState = updateViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    var isUpdateBannerDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        updateViewModel?.checkForUpdates(silent = true)
    }

    var showQuickDialerDialog by remember { mutableStateOf(false) }

    var hasPhonePermissions by remember { mutableStateOf(PermissionHelper.hasPhonePermissions(context)) }
    var hasContactsPermissions by remember { mutableStateOf(PermissionHelper.hasContactsPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasPhonePermissions = PermissionHelper.hasPhonePermissions(context)
        hasContactsPermissions = PermissionHelper.hasContactsPermission(context)
        if (hasPhonePermissions || hasContactsPermissions) {
            callerViewModel.syncDeviceData()
            if (hasPhonePermissions) {
                com.example.service.CallDetectionManager.startListening(context)
            }
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            callerViewModel.clearStatusMessage()
        }
    }

    // Photo picker launcher for screenshot / document scan
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scannerViewModel.scanImageUri(context, uri, ScanSource.SCREENSHOT)
            onNavigateToScanner(ScanSource.SCREENSHOT)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .drawBehind {
                    // High-performance 120fps ambient background glow without recomposing
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonGreenGlow, Color.Transparent),
                            center = Offset(size.width * 0.9f, 60.dp.toPx()),
                            radius = 280.dp.toPx()
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x1F00E5FF), Color.Transparent),
                            center = Offset(size.width * 0.1f, 320.dp.toPx()),
                            radius = 260.dp.toPx()
                        )
                    )
                }
                .padding(horizontal = 16.dp)
                .testTag("home_screen"),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Top Cyber Bar & Radar Status with Real Sync Button
            item(key = "header", contentType = "header") {
                HomeHeader(
                    isSyncing = isSyncing,
                    onSyncClick = {
                        if (!hasPhonePermissions || !hasContactsPermissions) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.READ_CONTACTS
                                )
                            )
                        } else {
                            callerViewModel.syncDeviceData()
                        }
                    }
                )
            }

            // Self-Update Notice Banner when v1.0.2 or newer release is discovered
            val currentUpdateStatus = updateUiState?.status
            if (!isUpdateBannerDismissed && currentUpdateStatus is UpdateStatus.UpdateAvailable) {
                item(key = "update_available_banner", contentType = "update_banner") {
                    HomeUpdateBanner(
                        versionName = currentUpdateStatus.updateInfo.versionName,
                        onUpdateClick = onNavigateToUpdate,
                        onDismiss = { isUpdateBannerDismissed = true }
                    )
                }
            }

            // 2. Permission Banner if phone call log / contacts not granted
            if (!hasPhonePermissions || !hasContactsPermissions) {
                item(key = "permission_banner", contentType = "banner") {
                    PermissionRequestBanner(
                        onGrantClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.READ_CONTACTS
                                )
                            )
                        }
                    )
                }
            }

            // 3. Speed Dial / Pinned Favorite Contacts Carousel
            if (favorites.isNotEmpty()) {
                item(key = "favorites_carousel", contentType = "favorites") {
                    FavoritesCarousel(
                        favorites = favorites,
                        onItemClick = { caller ->
                            onNavigateToCallerPopup(caller.phoneNumber, caller.name)
                        },
                        onCallClick = { number ->
                            PhoneActionHelper.openDialer(context, number)
                        }
                    )
                }
            }

            // 4. Hero Scanner Card with AI OCR Launcher & Shared Transition
            item(key = "hero_scanner", contentType = "hero") {
                HeroScanLauncher(
                    onLiveScanClick = { onNavigateToScanner(ScanSource.LIVE_CAMERA) },
                    onVisitingCardClick = { onNavigateToScanner(ScanSource.VISITING_CARD) },
                    onPaperClick = { onNavigateToScanner(ScanSource.PAPER) },
                    onBannerClick = { onNavigateToScanner(ScanSource.SHOP_BANNER) },
                    onScreenshotClick = { photoPickerLauncher.launch("image/*") },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }

            // 5. Smart Call Analytics & Protection Insights Card
            item(key = "analytics_card", contentType = "analytics") {
                SmartAnalyticsCard(analytics = analytics)
            }

            // 6. Recent Call Detections Header
            item(key = "recents_header", contentType = "section_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Call Activity",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "View All →",
                        color = NeonGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToRecents() }
                    )
                }
            }

            // 7. Recent calls items with 120 FPS recycling keys
            if (recentCalls.isEmpty()) {
                item(key = "empty_recents", contentType = "empty_state") {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No call logs loaded yet",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Sync Data' to load incoming call history directly from your device",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = recentCalls.take(6),
                    key = { it.id },
                    contentType = { "call_record_item" }
                ) { record ->
                    HomeCallRecordItem(
                        record = record,
                        onItemClick = { onNavigateToCallerPopup(record.phoneNumber, record.callerName ?: "Caller") },
                        onCallClick = { PhoneActionHelper.openDialer(context, record.phoneNumber) },
                        onWhatsAppClick = { PhoneActionHelper.openWhatsApp(context, record.phoneNumber) }
                    )
                }
            }
        }

        // Floating Cyber T9 Dialer FAB on Bottom Right
        Surface(
            onClick = { showQuickDialerDialog = true },
            shape = CircleShape,
            color = NeonGreen,
            tonalElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp)
                .size(56.dp)
                .testTag("fab_open_dialer")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Dialpad,
                    contentDescription = "Open T9 Dialer",
                    tint = TextOnNeon,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Interactive Cyber T9 Modal Dialog
        if (showQuickDialerDialog) {
            Dialog(
                onDismissRequest = { showQuickDialerDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .clickable { showQuickDialerDialog = false },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = false) {} // Prevent click-through dismissal
                            .padding(12.dp)
                    ) {
                        // Dismiss handle & close
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CYBER T9 SMART DIALER",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            IconButton(
                                onClick = { showQuickDialerDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Matched contacts horizontal preview chips
                        if (t9Results.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                items(t9Results.take(6), key = { it.normalizedNumber }) { match ->
                                    Surface(
                                        onClick = {
                                            callerViewModel.setDialerInput(match.phoneNumber)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF131D30),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = match.name,
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = match.phoneNumber.takeLast(4),
                                                color = CyberCyan,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        CyberT9Keypad(
                            dialerInput = dialerInput,
                            onDigitClick = { digit -> callerViewModel.onDialerDigit(digit) },
                            onBackspaceClick = { callerViewModel.onDialerBackspace() },
                            onCallClick = { number ->
                                PhoneActionHelper.openDialer(context, number)
                                showQuickDialerDialog = false
                            },
                            onWhatsAppClick = { number ->
                                PhoneActionHelper.openWhatsApp(context, number)
                                showQuickDialerDialog = false
                            },
                            onSaveContactClick = { number ->
                                PhoneActionHelper.openSaveContact(context, number)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    isSyncing: Boolean,
    onSyncClick: () -> Unit
) {
    // 120 FPS Pulse animation executed purely in the RenderNode draw phase
    val infiniteTransition = rememberInfiniteTransition(label = "beacon_transition")
    val beaconScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_scale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0x2B00FF66), RoundedCornerShape(16.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer {
                            scaleX = beaconScale
                            scaleY = beaconScale
                        }
                        .background(NeonGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CALL SHIELD: ACTIVE",
                    color = NeonGreen,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Quick Call Scan",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
        }

        // Real Modern Sync Button
        Surface(
            onClick = onSyncClick,
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF142033),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        color = CyberCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(15.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSyncing) "Syncing" else "Sync Data",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PermissionRequestBanner(
    onGrantClick: () -> Unit
) {
    NeonGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .background(CyberCyan.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Enable Real Call Logs",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Allow access to read caller history & contacts",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                onClick = onGrantClick,
                shape = RoundedCornerShape(10.dp),
                color = NeonGreen
            ) {
                Text(
                    text = "Grant",
                    color = TextOnNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun HeroScanLauncher(
    onLiveScanClick: () -> Unit,
    onVisitingCardClick: () -> Unit,
    onPaperClick: () -> Unit,
    onBannerClick: () -> Unit,
    onScreenshotClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val cardModifier = Modifier
        .fillMaxWidth()
        .testTag("hero_scan_launcher")

    val iconBoxModifier = Modifier
        .size(48.dp)
        .background(NeonGreen.copy(alpha = 0.15f), CircleShape)
        .border(1.dp, NeonGreen, CircleShape)

    val actionBtnModifier = Modifier
        .fillMaxWidth()
        .testTag("btn_start_live_scan")

    NeonGlassCard(
        modifier = cardModifier,
        cornerRadius = 24.dp
    ) {
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
                        text = "AI Optical Number Scanner",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Live ML Kit camera recognition at 120 FPS",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = iconBoxModifier
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Primary Launch Live Camera Button
            NeonPrimaryButton(
                onClick = onLiveScanClick,
                modifier = actionBtnModifier
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LAUNCH LIVE CAMERA SCANNER",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TARGET SCAN MODES",
                color = TextMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Preset Target Modes Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScanPresetChip(
                    icon = Icons.Default.Description,
                    label = "Visiting Card",
                    modifier = Modifier.weight(1f),
                    onClick = onVisitingCardClick
                )
                ScanPresetChip(
                    icon = Icons.Default.Description,
                    label = "Paper / Note",
                    modifier = Modifier.weight(1f),
                    onClick = onPaperClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScanPresetChip(
                    icon = Icons.Default.Storefront,
                    label = "Shop Banner",
                    modifier = Modifier.weight(1f),
                    onClick = onBannerClick
                )
                ScanPresetChip(
                    icon = Icons.Default.Image,
                    label = "Screenshot",
                    modifier = Modifier.weight(1f),
                    onClick = onScreenshotClick
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Standalone Outside Shortcut Pinning Action
            Surface(
                onClick = { com.example.utils.ShortcutHelper.pinQuickScannerShortcut(context) },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F1A2E),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_pin_shortcut")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📌", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pin Quick Scan Shortcut to Phone Screen",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanPresetChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF131D30),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33475569))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HomeCallRecordItem(
    record: CallRecord,
    onItemClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    val isSpam = record.isSpam || record.callType == CallType.SPAM_BLOCKED
    val name = record.callerName ?: "Unknown Caller"
    val initials = if (name.isNotBlank() && name != "Unknown Caller") {
        name.take(2).uppercase()
    } else {
        "#"
    }

    val (typeIcon, typeColor) = when (record.callType) {
        CallType.INCOMING -> Icons.Default.CallReceived to CyberCyan
        CallType.OUTGOING -> Icons.Default.CallMade to NeonGreen
        CallType.MISSED -> Icons.Default.CallMissed to WarningAmber
        CallType.REJECTED -> Icons.Default.CallReceived to TextMuted
        CallType.SPAM_BLOCKED -> Icons.Default.Warning to SpamRed
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        cornerRadius = 16.dp,
        borderColor = if (isSpam) SpamRed.copy(alpha = 0.45f) else DarkGlassBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Modern Initials Gradient Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSpam) {
                                Brush.linearGradient(listOf(Color(0xFF5A0E1F), Color(0xFF28070E)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF1E3250), Color(0xFF0F1A2B)))
                            }
                        )
                        .border(
                            1.dp,
                            if (isSpam) SpamRed.copy(alpha = 0.6f) else CyberCyan.copy(alpha = 0.4f),
                            CircleShape
                        )
                ) {
                    Text(
                        text = initials,
                        color = if (isSpam) SpamRed else CyberCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            color = if (isSpam) SpamRed else TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = record.phoneNumber,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    if (isSpam && !record.spamReason.isNullOrBlank()) {
                        Text(
                            text = "⚠️ ${record.spamReason}",
                            color = SpamRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // WhatsApp
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x2425D366))
                ) {
                    Text("💬", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Call
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = NeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeUpdateBanner(
    versionName: String,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeonGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_update_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
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
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .background(NeonGreen.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, NeonGreen, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "UPDATE AVAILABLE: $versionName",
                            color = NeonGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Speed templates, batch scanner & fraud radar improvements",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            NeonPrimaryButton(
                onClick = onUpdateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = "Review & Install Update ($versionName) →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
