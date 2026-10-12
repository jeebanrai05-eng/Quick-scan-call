package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import kotlin.math.sin

/**
 * Ultra-Modern 120 FPS Sci-Fi / Cyberpunk Holographic Scanner Overlay.
 * Designed with draw-phase animations to eliminate recomposition overhead.
 * Features:
 * - Dynamic Plasma Beam with radiant aura and particle nodes
 * - Smooth Corner Reticles with rounded cyber aesthetics
 * - Rotating Holographic Radar Ring & Angle Marks
 * - Dynamic Target Acquisition Grid & Sonar Ripple
 */
@Composable
fun PulseLaserScannerOverlay(
    modifier: Modifier = Modifier,
    isScanning: Boolean = true,
    hasTargetLocked: Boolean = false,
    laserColor: Color = NeonGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hologram_scanner_transition")

    // Laser Sweep Beam (Smooth bi-directional plasma)
    val laserProgressState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )

    // Viewfinder Breathing Scale
    val breathingScaleState = infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )

    // Rotating Radar Reticle Angle
    val radarRotationState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rotation"
    )

    // Sonar Ripple Wave Radius
    val sonarPulseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonar_pulse"
    )

    // Corner Glow Luminance
    val glowIntensityState = infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corner_glow"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val currentScale = breathingScaleState.value
        val currentProgress = laserProgressState.value
        val currentRadarAngle = radarRotationState.value
        val currentSonar = sonarPulseState.value
        val currentGlow = glowIntensityState.value

        // Viewfinder boundary dimensions (Golden ratio framing for cards & paper)
        val boxWidth = width * 0.88f * currentScale
        val boxHeight = height * 0.45f * currentScale
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2.3f
        val right = left + boxWidth
        val bottom = top + boxHeight
        val centerX = left + (boxWidth / 2f)
        val centerY = top + (boxHeight / 2f)

        // 1. Cinematic Darkened Vignette Overlay around target viewport
        drawRect(
            color = Color.Black.copy(alpha = 0.68f),
            size = Size(width, top)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.68f),
            topLeft = Offset(0f, bottom),
            size = Size(width, height - bottom)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.68f),
            topLeft = Offset(0f, top),
            size = Size(left, boxHeight)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.68f),
            topLeft = Offset(right, top),
            size = Size(width - right, boxHeight)
        )

        // 2. Holographic Target Grid Line Structure (Very subtle sci-fi mesh)
        val gridStepX = boxWidth / 6f
        val gridStepY = boxHeight / 4f
        val gridColor = Color(0x1800F0FF)

        for (i in 1..5) {
            val gx = left + (gridStepX * i)
            drawLine(
                color = gridColor,
                start = Offset(gx, top + 6.dp.toPx()),
                end = Offset(gx, bottom - 6.dp.toPx()),
                strokeWidth = 0.75.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
            )
        }
        for (j in 1..3) {
            val gy = top + (gridStepY * j)
            drawLine(
                color = gridColor,
                start = Offset(left + 6.dp.toPx(), gy),
                end = Offset(right - 6.dp.toPx(), gy),
                strokeWidth = 0.75.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
            )
        }

        // 3. Central Holographic Radar Compass Circle
        val maxSonarRadius = (boxWidth.coerceAtMost(boxHeight)) * 0.42f
        val sonarRadius = maxSonarRadius * currentSonar
        val sonarAlpha = (1f - currentSonar) * 0.45f

        // Expanding sonar pulse
        drawCircle(
            color = laserColor.copy(alpha = sonarAlpha),
            radius = sonarRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Center static inner radar ring
        drawCircle(
            color = CyberCyan.copy(alpha = 0.25f),
            radius = 32.dp.toPx(),
            center = Offset(centerX, centerY),
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        )

        // Rotating outer radar compass notches
        rotate(degrees = currentRadarAngle, pivot = Offset(centerX, centerY)) {
            val compassRadius = 48.dp.toPx()
            for (deg in 0 until 360 step 45) {
                rotate(degrees = deg.toFloat(), pivot = Offset(centerX, centerY)) {
                    drawLine(
                        color = CyberCyan.copy(alpha = 0.5f),
                        start = Offset(centerX, centerY - compassRadius),
                        end = Offset(centerX, centerY - compassRadius + 5.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }

        // Center Precision Reticle Crosshair
        val crosshairLength = 12.dp.toPx()
        val crosshairColor = if (hasTargetLocked) NeonGreen else CyberCyan.copy(alpha = 0.8f)
        drawLine(crosshairColor, Offset(centerX - crosshairLength, centerY), Offset(centerX + crosshairLength, centerY), 1.5.dp.toPx())
        drawLine(crosshairColor, Offset(centerX, centerY - crosshairLength), Offset(centerX, centerY + crosshairLength), 1.5.dp.toPx())
        drawCircle(color = crosshairColor, radius = 2.dp.toPx(), center = Offset(centerX, centerY))

        // 4. Rounded Cyber Viewfinder Boundary Border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.12f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )

        // 5. High-Precision Curvature Corner Brackets (Glowing Neon)
        val cornerArm = 38.dp.toPx()
        val cornerRadius = 22.dp.toPx()
        val cornerStroke = 4.dp.toPx()
        val activeCornerColor = if (hasTargetLocked) NeonGreen else laserColor.copy(alpha = currentGlow.coerceIn(0.5f, 1f))

        // Top-Left Rounded Bracket
        val pathTL = Path().apply {
            moveTo(left, top + cornerArm)
            lineTo(left, top + cornerRadius)
            quadraticTo(left, top, left + cornerRadius, top)
            lineTo(left + cornerArm, top)
        }
        drawPath(pathTL, activeCornerColor, style = Stroke(width = cornerStroke))

        // Top-Right Rounded Bracket
        val pathTR = Path().apply {
            moveTo(right - cornerArm, top)
            lineTo(right - cornerRadius, top)
            quadraticTo(right, top, right, top + cornerRadius)
            lineTo(right, top + cornerArm)
        }
        drawPath(pathTR, activeCornerColor, style = Stroke(width = cornerStroke))

        // Bottom-Left Rounded Bracket
        val pathBL = Path().apply {
            moveTo(left, bottom - cornerArm)
            lineTo(left, bottom - cornerRadius)
            quadraticTo(left, bottom, left + cornerRadius, bottom)
            lineTo(left + cornerArm, bottom)
        }
        drawPath(pathBL, activeCornerColor, style = Stroke(width = cornerStroke))

        // Bottom-Right Rounded Bracket
        val pathBR = Path().apply {
            moveTo(right - cornerArm, bottom)
            lineTo(right - cornerRadius, bottom)
            quadraticTo(right, bottom, right, bottom - cornerRadius)
            lineTo(right, bottom - cornerArm)
        }
        drawPath(pathBR, activeCornerColor, style = Stroke(width = cornerStroke))

        // Subtle Corner Diamond Accents
        val diamondSize = 4.dp.toPx()
        drawCircle(color = CyberCyan, radius = diamondSize, center = Offset(left + 2.dp.toPx(), top + 2.dp.toPx()))
        drawCircle(color = CyberCyan, radius = diamondSize, center = Offset(right - 2.dp.toPx(), top + 2.dp.toPx()))
        drawCircle(color = CyberCyan, radius = diamondSize, center = Offset(left + 2.dp.toPx(), bottom - 2.dp.toPx()))
        drawCircle(color = CyberCyan, radius = diamondSize, center = Offset(right - 2.dp.toPx(), bottom - 2.dp.toPx()))

        // 6. 120 FPS Plasma Laser Beam with Multi-Layer Glow & Particle Ticks
        if (isScanning) {
            val laserY = top + (boxHeight * currentProgress)

            // Layer A: Wide Ambient Volumetric Plasma Glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        laserColor.copy(alpha = 0f),
                        laserColor.copy(alpha = 0.12f),
                        laserColor.copy(alpha = 0.45f),
                        laserColor.copy(alpha = 0.12f),
                        laserColor.copy(alpha = 0f)
                    ),
                    startY = laserY - 32.dp.toPx(),
                    endY = laserY + 32.dp.toPx()
                ),
                topLeft = Offset(left, laserY - 32.dp.toPx()),
                size = Size(boxWidth, 64.dp.toPx())
            )

            // Layer B: Intense Inner Core Beam
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.1f)
                    ),
                    startY = laserY - 2.dp.toPx(),
                    endY = laserY + 2.dp.toPx()
                ),
                topLeft = Offset(left + 4.dp.toPx(), laserY - 2.dp.toPx()),
                size = Size(boxWidth - 8.dp.toPx(), 4.dp.toPx())
            )

            // Layer C: Lateral Floating Laser Node Heads
            val nodeRadius = 5.dp.toPx()
            drawCircle(Color.White, radius = nodeRadius, center = Offset(left + 8.dp.toPx(), laserY))
            drawCircle(laserColor, radius = nodeRadius + 3.dp.toPx(), center = Offset(left + 8.dp.toPx(), laserY), style = Stroke(1.5.dp.toPx()))

            drawCircle(Color.White, radius = nodeRadius, center = Offset(right - 8.dp.toPx(), laserY))
            drawCircle(laserColor, radius = nodeRadius + 3.dp.toPx(), center = Offset(right - 8.dp.toPx(), laserY), style = Stroke(1.5.dp.toPx()))

            // Moving center spark node that oscillates left-to-right on the beam
            val sparkProgress = (sin(currentProgress * Math.PI * 4) + 1f) / 2f
            val sparkX = left + 30.dp.toPx() + (boxWidth - 60.dp.toPx()) * sparkProgress.toFloat()
            drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(sparkX, laserY))
            drawCircle(CyberCyan, radius = 6.dp.toPx(), center = Offset(sparkX, laserY), style = Stroke(1.dp.toPx()))
        }
    }
}
