package com.example.ui.components

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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallAnalytics
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun SmartAnalyticsCard(
    analytics: CallAnalytics,
    modifier: Modifier = Modifier
) {
    val total = analytics.totalCalls.coerceAtLeast(1)
    val incomingRatio = analytics.incomingCount.toFloat() / total
    val outgoingRatio = analytics.outgoingCount.toFloat() / total
    val missedRatio = analytics.missedCount.toFloat() / total
    val spamRatio = analytics.spamBlocked.toFloat() / total

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header with Shield Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF131D30), CircleShape)
                            .border(1.dp, NeonGreen.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SMART CALL TELEMETRY",
                            color = NeonGreen,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Real-Time Protection Stats",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (analytics.spamRiskPercent < 15) NeonGreen.copy(alpha = 0.15f) else SpamRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (analytics.spamRiskPercent < 15) NeonGreen.copy(alpha = 0.4f) else SpamRed.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = "${(analytics.safeVerifiedRatio * 100).toInt()}% CLEAN",
                        color = if (analytics.spamRiskPercent < 15) NeonGreen else SpamRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-segment call distribution progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E293B))
            ) {
                if (incomingRatio > 0) {
                    Box(
                        modifier = Modifier
                            .weight(incomingRatio.coerceAtLeast(0.05f))
                            .height(8.dp)
                            .background(CyberCyan)
                    )
                }
                if (outgoingRatio > 0) {
                    Box(
                        modifier = Modifier
                            .weight(outgoingRatio.coerceAtLeast(0.05f))
                            .height(8.dp)
                            .background(NeonGreen)
                    )
                }
                if (missedRatio > 0) {
                    Box(
                        modifier = Modifier
                            .weight(missedRatio.coerceAtLeast(0.05f))
                            .height(8.dp)
                            .background(WarningAmber)
                    )
                }
                if (spamRatio > 0) {
                    Box(
                        modifier = Modifier
                            .weight(spamRatio.coerceAtLeast(0.05f))
                            .height(8.dp)
                            .background(SpamRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats row 1: Incoming & Outgoing & Missed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricChip(
                    icon = Icons.Default.CallReceived,
                    iconColor = CyberCyan,
                    label = "Incoming",
                    value = "${analytics.incomingCount}"
                )
                MetricChip(
                    icon = Icons.Default.CallMade,
                    iconColor = NeonGreen,
                    label = "Outgoing",
                    value = "${analytics.outgoingCount}"
                )
                MetricChip(
                    icon = Icons.Default.CallMissed,
                    iconColor = WarningAmber,
                    label = "Missed",
                    value = "${analytics.missedCount}"
                )
                MetricChip(
                    icon = Icons.Default.Shield,
                    iconColor = SpamRed,
                    label = "Blocked",
                    value = "${analytics.spamBlocked}"
                )
            }

            if (analytics.minutesSaved > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF101B2E), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Saved ~${analytics.minutesSaved} mins from scammers & robocallers",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            text = label,
            color = TextMuted,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
