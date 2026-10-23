package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

/**
 * Next-Gen 120 FPS Holographic Scam Radar & Trust Matrix Gauge.
 * Renders an animated circular radar display with sweep line, safety percentage,
 * and comprehensive threat metrics breakdown.
 */
@Composable
fun ScamRadarGauge(
    spamScore: Int, // 0 to 100
    reportsCount: Int,
    carrier: String,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    showDetails: Boolean = true
) {
    val trustScore = (100 - spamScore).coerceIn(0, 100)
    val isSevereSpam = spamScore >= 60
    val isSuspicious = spamScore in 25..59
    val isSafe = spamScore < 25

    val themeColor = when {
        isSevereSpam -> SpamRed
        isSuspicious -> WarningAmber
        else -> NeonGreen
    }

    val animatedTrust by animateFloatAsState(
        targetValue = trustScore.toFloat(),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "animated_trust"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Holographic Radar Circle View
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = (this.size.minDimension / 2f) - 10.dp.toPx()

                // Background track arc
                drawCircle(
                    color = Color(0xFF0F1A2E),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 8.dp.toPx())
                )

                // Secondary inner dashed radar ring
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.15f),
                    radius = radius * 0.72f,
                    center = center,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                )

                // Outer rotating radar sweep beam
                rotate(degrees = sweepAngle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                themeColor.copy(alpha = 0.05f),
                                themeColor.copy(alpha = 0.35f)
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 75f,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f)
                    )
                    // Leading sweep needle
                    drawLine(
                        color = themeColor.copy(alpha = 0.8f),
                        start = center,
                        end = Offset(center.x + radius, center.y),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Active Score Progress Arc
                val sweepProgress = (animatedTrust / 100f) * 280f
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            themeColor.copy(alpha = 0.6f),
                            themeColor,
                            CyberCyan
                        )
                    ),
                    startAngle = 130f,
                    sweepAngle = sweepProgress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Notches on perimeter
                for (angle in 0 until 360 step 30) {
                    rotate(degrees = angle.toFloat(), pivot = center) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f),
                            start = Offset(center.x, center.y - radius - 4.dp.toPx()),
                            end = Offset(center.x, center.y - radius + 2.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
            }

            // Center Holographic Readout
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${animatedTrust.toInt()}%",
                    color = themeColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = if (isSevereSpam) "THREAT DETECTED" else if (isSuspicious) "MEDIUM RISK" else "TRUST SCORE",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(themeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isSafe) Icons.Default.VerifiedUser else if (isSuspicious) Icons.Default.GppMaybe else Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isSafe) "VERIFIED SAFE" else if (isSuspicious) "SUSPICIOUS" else "HIGH RISK SPAM",
                        color = themeColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        if (showDetails) {
            Spacer(modifier = Modifier.height(14.dp))
            // Threat Matrix Breakdown Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    title = "ROBOCALL RISK",
                    value = if (isSevereSpam) "98% High" else if (isSuspicious) "45% Mod" else "2% Clean",
                    color = themeColor,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "COMMUNITY",
                    value = if (reportsCount > 0) "$reportsCount Reports" else "No Reports",
                    color = if (reportsCount > 0) WarningAmber else NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "CARRIER AUTH",
                    value = if (carrier.isNotBlank() && carrier != "Cellular") carrier else "Standard",
                    color = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricChip(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xD90D1526),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = color,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
