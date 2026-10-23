package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import kotlin.math.sin

/**
 * High-performance 120 FPS dynamic Sci-Fi Audio / Telemetry Waveform Visualizer.
 * Renders animated equalizer bars using mathematical sinusoids directly in DrawScope.
 */
@Composable
fun CyberWaveformVisualizer(
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    width: Dp = 100.dp,
    height: Dp = 28.dp,
    isActive: Boolean = true,
    startColor: Color = NeonGreen,
    endColor: Color = CyberCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val phaseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barSpacing = 2.dp.toPx()
        val barWidth = ((totalWidth - ((barCount - 1) * barSpacing)) / barCount).coerceAtLeast(1.5f)
        val phase = phaseState.value

        val brush = Brush.verticalGradient(
            colors = listOf(startColor, endColor),
            startY = 0f,
            endY = maxHeight
        )

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount.toFloat()
            // Harmonic wave calculation combining two frequencies
            val wave = if (isActive) {
                val sin1 = sin(phase + progress * 4f)
                val sin2 = sin(phase * 1.5f + progress * 8f)
                ((sin1 + sin2) / 2f + 1f) / 2f
            } else {
                0.15f
            }

            val barHeight = (maxHeight * wave).coerceIn(4.dp.toPx(), maxHeight)
            val x = i * (barWidth + barSpacing)
            val y = (maxHeight - barHeight) / 2f

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
