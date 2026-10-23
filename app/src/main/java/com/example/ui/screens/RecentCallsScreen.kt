package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallRecord
import com.example.data.model.CallType
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextOnNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.utils.PhoneActionHelper
import com.example.viewmodel.CallFilter
import com.example.viewmodel.CallerViewModel

@Composable
fun RecentCallsScreen(
    callerViewModel: CallerViewModel,
    onNavigateToCallerPopup: (String, String) -> Unit
) {
    val context = LocalContext.current
    val recentCalls by callerViewModel.recentCalls.collectAsStateWithLifecycle()
    val selectedFilter by callerViewModel.selectedFilter.collectAsStateWithLifecycle()

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Cached calculation for 120 FPS frame stability
    val filteredList = remember(selectedFilter, recentCalls) {
        when (selectedFilter) {
            CallFilter.ALL -> recentCalls
            CallFilter.INCOMING -> recentCalls.filter { it.callType == CallType.INCOMING }
            CallFilter.MISSED -> recentCalls.filter { it.callType == CallType.MISSED }
            CallFilter.SPAM -> recentCalls.filter { it.isSpam || it.callType == CallType.SPAM_BLOCKED }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Call Logs?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all call history records from the offline database.", color = TextSecondary) },
            containerColor = DarkSurfaceVariant,
            confirmButton = {
                TextButton(onClick = {
                    callerViewModel.clearAllCallHistory()
                    showClearConfirmDialog = false
                }) {
                    Text("Clear All", color = SpamRed, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
            .testTag("recent_calls_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Screen Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Call History",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "${recentCalls.size} total calls recorded",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (recentCalls.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = SpamRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Modern Cyber Filter Tabs with Spring Transitions
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(CallFilter.values(), key = { it.name }) { filter ->
                val isSelected = selectedFilter == filter
                val targetBg = if (isSelected) NeonGreen else Color(0xFF131D30)
                val targetBorder = if (isSelected) NeonGreen else Color(0x33475569)
                val targetTextColor = if (isSelected) TextOnNeon else TextSecondary

                val animatedBg by animateColorAsState(
                    targetValue = targetBg,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tab_bg"
                )

                Surface(
                    onClick = { callerViewModel.onFilterSelected(filter) },
                    shape = RoundedCornerShape(12.dp),
                    color = animatedBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, targetBorder)
                ) {
                    val count = when (filter) {
                        CallFilter.ALL -> recentCalls.size
                        CallFilter.INCOMING -> recentCalls.count { it.callType == CallType.INCOMING }
                        CallFilter.MISSED -> recentCalls.count { it.callType == CallType.MISSED }
                        CallFilter.SPAM -> recentCalls.count { it.isSpam || it.callType == CallType.SPAM_BLOCKED }
                    }

                    Text(
                        text = "${filter.name} ($count)",
                        color = targetTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 120 FPS High-Performance Recycled LazyColumn
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No call records for this filter",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredList,
                    key = { it.id },
                    contentType = { "recent_call_card" }
                ) { record ->
                    RecentCallCard(
                        record = record,
                        onCallClick = { PhoneActionHelper.openDialer(context, record.phoneNumber) },
                        onWhatsAppClick = { PhoneActionHelper.openWhatsApp(context, record.phoneNumber) },
                        onItemClick = {
                            onNavigateToCallerPopup(record.phoneNumber, record.callerName ?: "Caller")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentCallCard(
    record: CallRecord,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onItemClick: () -> Unit
) {
    val isSpam = record.isSpam || record.callType == CallType.SPAM_BLOCKED
    val name = record.callerName ?: "Unknown Caller"
    val initials = if (name.isNotBlank() && name != "Unknown Caller") {
        name.take(2).uppercase()
    } else {
        "#"
    }

    val (callTypeIcon, callTypeColor, callTypeLabel) = when (record.callType) {
        CallType.INCOMING -> Triple(Icons.AutoMirrored.Filled.CallReceived, CyberCyan, "Incoming")
        CallType.OUTGOING -> Triple(Icons.AutoMirrored.Filled.CallMade, NeonGreen, "Outgoing")
        CallType.MISSED -> Triple(Icons.AutoMirrored.Filled.CallMissed, WarningAmber, "Missed")
        CallType.REJECTED -> Triple(Icons.Default.CallEnd, TextMuted, "Declined")
        CallType.SPAM_BLOCKED -> Triple(Icons.Default.Warning, SpamRed, "Spam Blocked")
    }

    val timeString = runCatching {
        DateUtils.getRelativeTimeSpanString(
            record.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }.getOrDefault("Recently")

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("recent_card_${record.phoneNumber}"),
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
            // Left: Initials Avatar & Information
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
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
                            text = record.callerName ?: record.phoneNumber,
                            color = if (isSpam) SpamRed else TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = callTypeIcon,
                            contentDescription = callTypeLabel,
                            tint = callTypeColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    if (record.callerName != null) {
                        Text(
                            text = record.phoneNumber,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$callTypeLabel • $timeString",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        if (record.durationSeconds > 0) {
                            val durationText = if (record.durationSeconds >= 60) {
                                "${record.durationSeconds / 60}m ${record.durationSeconds % 60}s"
                            } else {
                                "${record.durationSeconds}s"
                            }
                            Text(
                                text = " • $durationText",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Right: Quick Action Palette
            Row(verticalAlignment = Alignment.CenterVertically) {
                // WhatsApp
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x2425D366))
                ) {
                    Text("💬", fontSize = 15.sp)
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
