package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ScanSource
import com.example.ui.components.BatchScanReviewSheet
import com.example.ui.components.DetectedNumberCard
import com.example.ui.components.DocumentSummaryCard
import com.example.ui.components.EditScannedNumberDialog
import com.example.ui.components.ImageEnhanceStudioSheet
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.PulseLaserScannerOverlay
import com.example.ui.components.ScanVaultSheet
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextOnNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.PermissionHelper
import com.example.utils.PhoneActionHelper
import com.example.viewmodel.ScannerEngineMode
import com.example.viewmodel.ScannerViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ScannerScreen(
    scannerViewModel: ScannerViewModel,
    initialSource: ScanSource = ScanSource.LIVE_CAMERA,
    onNavigateBack: () -> Unit = {},
    onNavigateToCallerPopup: (String, String) -> Unit = { _, _ -> },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val uiState by scannerViewModel.uiState.collectAsStateWithLifecycle()
    val vaultItems by scannerViewModel.scanVault.collectAsStateWithLifecycle()

    BackHandler {
        if (uiState.showEnhanceStudio) {
            scannerViewModel.closeEnhanceStudio()
        } else if (uiState.showBatchReviewSheet) {
            scannerViewModel.toggleBatchReviewSheet(false)
        } else if (uiState.showVaultSheet) {
            scannerViewModel.toggleVaultSheet()
        } else {
            onNavigateBack()
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(PermissionHelper.hasCameraPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scannerViewModel.scanImageUri(context, uri, ScanSource.SCREENSHOT)
        }
    }

    var cameraControl: Camera? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var activePreviewView: PreviewView? by remember { mutableStateOf(null) }
    var isCameraHardwareAvailable by remember { mutableStateOf(true) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { cameraExecutor.shutdown() }
        }
    }

    LaunchedEffect(uiState.isTorchOn) {
        runCatching {
            cameraControl?.cameraControl?.enableTorch(uiState.isTorchOn)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            scannerViewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.autoDialTriggeredFor) {
        val numberToDial = uiState.autoDialTriggeredFor
        if (numberToDial != null) {
            PhoneActionHelper.openDialer(context, numberToDial)
            scannerViewModel.resetAutoDialTrigger()
        }
    }

    LaunchedEffect(uiState.detectedNumbers.size) {
        if (uiState.detectedNumbers.isNotEmpty()) {
            runCatching {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionRequestView(
            onNavigateBack = onNavigateBack,
            onRequestPermission = {
                runCatching { permissionLauncher.launch(Manifest.permission.CAMERA) }
            },
            onPickGalleryClick = {
                runCatching { photoPickerLauncher.launch("image/*") }
            }
        )
        return
    }

    // Holographic Beacon Animation
    val infiniteTransition = rememberInfiniteTransition(label = "hud_beacon")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("scanner_screen")
    ) {
        // 1. Live Camera Preview (Crash-Proof Binding)
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                activePreviewView = previewView

                val hasHardwareCamera = runCatching {
                    val cm = ctx.getSystemService(android.content.Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                    cm?.cameraIdList?.isNotEmpty() == true
                }.getOrDefault(false)

                if (hasHardwareCamera) {
                    try {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val executor = ContextCompat.getMainExecutor(ctx)

                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        if (!cameraExecutor.isShutdown) {
                                            analysis.setAnalyzer(cameraExecutor, scannerViewModel.createCameraAnalyzer())
                                        }
                                    }

                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                val selector = when {
                                    runCatching { cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false) ->
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    runCatching { cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false) ->
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    else -> CameraSelector.DEFAULT_BACK_CAMERA
                                }

                                runCatching { cameraProvider.unbindAll() }

                                cameraControl = try {
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        selector,
                                        preview,
                                        imageAnalysis,
                                        capture
                                    )
                                } catch (_: Throwable) {
                                    imageCapture = null
                                    try {
                                        runCatching { cameraProvider.unbindAll() }
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            selector,
                                            preview,
                                            imageAnalysis
                                        )
                                    } catch (_: Throwable) {
                                        isCameraHardwareAvailable = false
                                        null
                                    }
                                }
                            } catch (_: Throwable) {
                                isCameraHardwareAvailable = false
                            }
                        }, executor)
                    } catch (_: Throwable) {
                        isCameraHardwareAvailable = false
                    }
                } else {
                    isCameraHardwareAvailable = false
                }

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Standby Viewfinder if Camera hardware unavailable
        if (!isCameraHardwareAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99050B14)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(32.dp)
                        .background(Color(0xEE091322), RoundedCornerShape(20.dp))
                        .border(1.2.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Camera Sensor Standby",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Hardware preview unavailable on this device. You can scan phone numbers and documents anytime via Gallery or tap 'Enhance & Scan'.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NeonPrimaryButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "IMPORT IMAGE / SCREENSHOT", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }

        // 2. Holographic Laser & Reticle Overlay
        val isTargetLocked = uiState.detectedNumbers.isNotEmpty()
        PulseLaserScannerOverlay(
            modifier = Modifier.fillMaxSize(),
            isScanning = uiState.isScanning,
            hasTargetLocked = isTargetLocked,
            laserColor = if (uiState.engineMode == ScannerEngineMode.AI_DEEP_RESTORE) CyberCyan else NeonGreen
        )

        // 3. Floating Top HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, start = 14.dp, end = 14.dp)
                .align(Alignment.TopCenter)
        ) {
            // Top Row: Back + Engine Selector + Vault Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xD9070E1A))
                        .border(1.2.dp, Color(0x6600FF66), CircleShape)
                        .testTag("scanner_btn_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Engine Segmented Dock: Turbo OCR vs AI Vision Pro
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xD9070E1A))
                        .border(
                            1.2.dp,
                            Brush.horizontalGradient(listOf(Color(0x6600FF66), Color(0x6600F0FF))),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isTurbo = uiState.engineMode == ScannerEngineMode.TURBO_MULTI_PASS

                        Surface(
                            onClick = { if (!isTurbo) scannerViewModel.toggleEngineMode() },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isTurbo) NeonGreen else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = if (isTurbo) TextOnNeon else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "TURBO OCR",
                                    color = if (isTurbo) TextOnNeon else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Surface(
                            onClick = { if (isTurbo) scannerViewModel.toggleEngineMode() },
                            shape = RoundedCornerShape(20.dp),
                            color = if (!isTurbo) CyberCyan else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (!isTurbo) Color(0xFF021020) else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI VISION PRO",
                                    color = if (!isTurbo) Color(0xFF021020) else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Scan History Vault Button
                IconButton(
                    onClick = { scannerViewModel.toggleVaultSheet() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xD9070E1A))
                        .border(1.2.dp, CyberCyan.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CollectionsBookmark,
                        contentDescription = "Vault",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Guidance & Focus Telemetry Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xB3000000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isTargetLocked) NeonGreen else CyberCyan.copy(alpha = beaconAlpha)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uiState.cameraGuidance.message,
                            color = if (isTargetLocked) NeonGreen else TextSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // Batch Mode Toggle Chip
                Surface(
                    onClick = { scannerViewModel.toggleBatchMode() },
                    shape = RoundedCornerShape(14.dp),
                    color = if (uiState.isBatchMode) Color(0x3300FF66) else Color(0x80000000),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (uiState.isBatchMode) NeonGreen else Color(0x22FFFFFF)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = if (uiState.isBatchMode) NeonGreen else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.isBatchMode) "BATCH ON (${uiState.batchPages.size})" else "BATCH OFF",
                            color = if (uiState.isBatchMode) NeonGreen else TextSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Preset Target Source Chips (Visiting Card, Poster, Shop Banner, etc.)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val sources = listOf(
                    ScanSource.LIVE_CAMERA to "Auto",
                    ScanSource.VISITING_CARD to "Card",
                    ScanSource.SHOP_BANNER to "Banner",
                    ScanSource.PAPER to "Document",
                    ScanSource.POSTER to "Poster"
                )

                items(sources) { (source, label) ->
                    val isSelected = uiState.activeScanSource == source
                    Surface(
                        onClick = { scannerViewModel.setScanSource(source) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0x3300FF66) else Color(0x80000000),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NeonGreen else Color(0x22FFFFFF)
                        )
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) NeonGreen else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 4. Live Holographic Text Subtitle Stream
        if (uiState.latestScannedText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .align(Alignment.Center)
                    .padding(top = 180.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xB3000000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2200F0FF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OCR >",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uiState.latestScannedText.take(65).replace("\n", " "),
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 5. Processing Indicator
        AnimatedVisibility(
            visible = uiState.isAiProcessing || uiState.isProcessingImage,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color(0xF008101E), RoundedCornerShape(24.dp))
                        .border(1.5.dp, Brush.linearGradient(listOf(NeonGreen, CyberCyan)), RoundedCornerShape(24.dp))
                        .padding(28.dp)
                ) {
                    CircularProgressIndicator(
                        color = CyberCyan,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = if (uiState.isAiProcessing) "🧠 AI NEURAL RESTORATION" else "⚡ TURBO MULTI-PASS OCR",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Extracting Phone Numbers & Entities...",
                        color = TextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Auto-repairing damaged, tilted, or blurry digits",
                        color = TextMuted,
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        // 6. Floating Action Dock (Torch, Capture & Process, Enhance Studio, Gallery)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 75.dp, start = 14.dp, end = 14.dp)
        ) {
            // A. Batch review trigger bar if batch pages captured
            if (uiState.isBatchMode && uiState.batchPages.isNotEmpty()) {
                Surface(
                    onClick = { scannerViewModel.toggleBatchReviewSheet(true) },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xF20F243A),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, CyberCyan),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📑 BATCH: ${uiState.batchPages.size} Page(s) Captured",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "REVIEW & EXPORT ↗",
                            color = NeonGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // B. Primary Action Bar
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xD9060C17),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33475569)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flash / Torch Toggle
                    IconButton(
                        onClick = { scannerViewModel.toggleTorch() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isTorchOn) Color(0x3300FF66) else Color(0x22FFFFFF))
                            .border(1.dp, if (uiState.isTorchOn) NeonGreen else Color(0x22FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (uiState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight",
                            tint = if (uiState.isTorchOn) NeonGreen else Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Enhancement Studio Trigger
                    IconButton(
                        onClick = {
                            val bitmap = activePreviewView?.bitmap
                            if (bitmap != null) {
                                scannerViewModel.openEnhanceStudio(bitmap)
                            } else {
                                photoPickerLauncher.launch("image/*")
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Enhance Studio",
                            tint = CyberCyan,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Center Hero Capture & Process Button
                    Surface(
                        onClick = {
                            val previewBitmap = runCatching { activePreviewView?.bitmap }.getOrNull()
                            if (previewBitmap != null) {
                                if (uiState.engineMode == ScannerEngineMode.AI_DEEP_RESTORE) {
                                    scannerViewModel.scanBitmapWithAi(previewBitmap, uiState.activeScanSource)
                                } else {
                                    scannerViewModel.scanBitmapMultiPass(previewBitmap, uiState.activeScanSource, pageIndex = uiState.batchPages.size + 1)
                                }
                            } else {
                                val capture = imageCapture
                                if (capture != null) {
                                    val executor = ContextCompat.getMainExecutor(context)
                                    capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bmp = runCatching { image.toBitmap() }.getOrNull()
                                            image.close()
                                            if (bmp != null) {
                                                if (uiState.engineMode == ScannerEngineMode.AI_DEEP_RESTORE) {
                                                    scannerViewModel.scanBitmapWithAi(bmp, uiState.activeScanSource)
                                                } else {
                                                    scannerViewModel.scanBitmapMultiPass(bmp, uiState.activeScanSource, pageIndex = uiState.batchPages.size + 1)
                                                }
                                            }
                                        }
                                        override fun onError(exception: ImageCaptureException) {
                                            photoPickerLauncher.launch("image/*")
                                        }
                                    })
                                } else {
                                    photoPickerLauncher.launch("image/*")
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F1E33),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(listOf(NeonGreen, CyberCyan))
                        ),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.engineMode == ScannerEngineMode.AI_DEEP_RESTORE) Icons.Default.AutoAwesome else Icons.Default.Bolt,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.engineMode == ScannerEngineMode.AI_DEEP_RESTORE) "CAPTURE & AI REPAIR" else "CAPTURE & SCAN",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Gallery / Screenshot Picker
                    IconButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color(0x22FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Pick Image",
                            tint = CyberCyan,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // C. Auto-Dialer Quick Toggle Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xB3000000),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Auto Instant Dialer",
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Instantly dials when number recognized",
                                color = TextMuted,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    Switch(
                        checked = uiState.autoOpenDialer,
                        onCheckedChange = { scannerViewModel.toggleAutoDial() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = Color(0xFF0F2E1E),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }

            // D. Document Intelligence Card (Business, Person, Email, Address)
            if (uiState.latestDocument != null) {
                DocumentSummaryCard(
                    documentData = uiState.latestDocument!!,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // E. Detected Numbers Drawer / Cards
            AnimatedVisibility(
                visible = uiState.detectedNumbers.isNotEmpty(),
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DETECTED PHONE NUMBERS (${uiState.detectedNumbers.size})",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Surface(
                            onClick = { scannerViewModel.clearDetected() },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x22FFFFFF)
                        ) {
                            Text(
                                text = "Clear",
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.detectedNumbers) { result ->
                            DetectedNumberCard(
                                result = result,
                                onCallClick = {
                                    PhoneActionHelper.openDialer(context, result.phoneNumber)
                                },
                                onDetailsClick = {
                                    onNavigateToCallerPopup(
                                        result.phoneNumber,
                                        result.callerInfo?.name ?: result.contactName ?: "Recognized Caller"
                                    )
                                },
                                onEditClick = {
                                    scannerViewModel.openEditDialog(result)
                                },
                                onDeleteClick = {
                                    scannerViewModel.deleteDetectedNumber(result.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // Floating Sheets & Modals
        // ==========================================

        // Image Enhancement Studio Modal
        if (uiState.showEnhanceStudio) {
            Dialog(
                onDismissRequest = { scannerViewModel.closeEnhanceStudio() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    ImageEnhanceStudioSheet(
                        previewBitmap = uiState.enhancedBitmap ?: uiState.capturedBitmap,
                        contrast = uiState.enhanceContrast,
                        brightness = uiState.enhanceBrightness,
                        isSharpen = uiState.isSharpenActive,
                        isDenoise = uiState.isDenoiseActive,
                        isGrayscale = uiState.isGrayscaleActive,
                        isInvert = uiState.isInvertActive,
                        isBinarize = uiState.isBinarizeActive,
                        onContrastChange = { scannerViewModel.setEnhanceContrast(it) },
                        onBrightnessChange = { scannerViewModel.setEnhanceBrightness(it) },
                        onRotateClick = { scannerViewModel.rotateCapturedImage(90f) },
                        onAutoStraightenClick = { scannerViewModel.autoPerspectiveCorrect() },
                        onToggleSharpen = { scannerViewModel.toggleSharpen() },
                        onToggleDenoise = { scannerViewModel.toggleDenoise() },
                        onToggleGrayscale = { scannerViewModel.toggleGrayscale() },
                        onToggleInvert = { scannerViewModel.toggleInvert() },
                        onToggleBinarize = { scannerViewModel.toggleBinarize() },
                        onApplyAndRescan = { scannerViewModel.applyEnhancementsAndRescan() },
                        onClose = { scannerViewModel.closeEnhanceStudio() }
                    )
                }
            }
        }

        // Multi-Page Batch Review Modal
        if (uiState.showBatchReviewSheet) {
            Dialog(
                onDismissRequest = { scannerViewModel.toggleBatchReviewSheet(false) },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    BatchScanReviewSheet(
                        batchPages = uiState.batchPages,
                        allDetectedNumbers = uiState.detectedNumbers,
                        onClose = { scannerViewModel.toggleBatchReviewSheet(false) },
                        onRemovePage = { scannerViewModel.removeBatchPage(it) },
                        onClearAll = { scannerViewModel.clearBatch() },
                        onExportVcf = {
                            val vcf = scannerViewModel.generateVCardContent()
                            PhoneActionHelper.shareExportContent(context, vcf, "Quick Call Contacts", "contacts.vcf", "text/x-vcard")
                        },
                        onExportCsv = {
                            val csv = scannerViewModel.generateCsvContent()
                            PhoneActionHelper.shareExportContent(context, csv, "Quick Call Numbers", "numbers.csv", "text/csv")
                        },
                        onShareText = {
                            val summary = scannerViewModel.getFormattedBatchSummaryText()
                            PhoneActionHelper.shareNumber(context, summary, "Quick Call Scan Summary")
                        }
                    )
                }
            }
        }

        // Vault Sheet Modal
        if (uiState.showVaultSheet) {
            Dialog(
                onDismissRequest = { scannerViewModel.toggleVaultSheet() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    ScanVaultSheet(
                        vaultItems = vaultItems,
                        onClose = { scannerViewModel.toggleVaultSheet() },
                        onDeleteItem = { scannerViewModel.deleteFromVault(it) },
                        onClearAll = { scannerViewModel.clearVault() }
                    )
                }
            }
        }

        // Inline Edit Scanned Result Dialog
        if (uiState.showEditDialog && uiState.editingResult != null) {
            EditScannedNumberDialog(
                initialResult = uiState.editingResult!!,
                onSave = { updated -> scannerViewModel.updateScannedResult(updated) },
                onDismiss = { scannerViewModel.closeEditDialog() }
            )
        }
    }
}

@Composable
private fun CameraPermissionRequestView(
    onNavigateBack: () -> Unit = {},
    onRequestPermission: () -> Unit,
    onPickGalleryClick: () -> Unit = {}
) {
    BackHandler {
        onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xD9070E1A))
                .border(1.2.dp, Color(0x6600FF66), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = NeonGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(84.dp)
                    .background(Color(0xFF1E293B), CircleShape)
                    .border(2.dp, NeonGreen, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Camera Permission Required",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To scan phone numbers from visiting cards, banners, notes, and screens in real-time, Quick Call Scan needs camera access.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            NeonPrimaryButton(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "GRANT CAMERA ACCESS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                onClick = onPickGalleryClick,
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF131D30),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SCAN FROM GALLERY / SCREENSHOT",
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
