package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallerType
import com.example.ui.components.CallerBadge
import com.example.ui.components.DangerButton
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGlassCard
import com.example.ui.components.NeonOutlinedButton
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.utils.PhoneActionHelper
import com.example.utils.SpamDetector
import com.example.utils.SpamRiskTier
import com.example.viewmodel.CallerViewModel

@Composable
fun CallerPopupScreen(
    callerViewModel: CallerViewModel,
    phoneNumber: String,
    callerName: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(phoneNumber) {
        callerViewModel.lookupCaller(phoneNumber)
    }

    val activeCaller by callerViewModel.activeCaller.collectAsStateWithLifecycle()
    val analysis by callerViewModel.activeSpamAnalysis.collectAsStateWithLifecycle()

    val isSpam = analysis?.score?.let { it >= 50 } == true || activeCaller?.isSpam == true

    // Pulsing ringing animation
    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    var showReportDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    var editingTag by remember(activeCaller) { mutableStateOf(activeCaller?.tag ?: "WORK") }
    var editingNote by remember(activeCaller) { mutableStateOf(activeCaller?.note ?: "") }

    // Tag / Note Editor Dialog
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            containerColor = DarkSurfaceVariant,
            title = {
                Text(
                    text = "Add Custom Tag & Note",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Select Category Tag:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val presetTags = listOf("WORK", "FAMILY", "FRIEND", "DELIVERY", "CLIENT", "SPAM")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetTags.take(3).forEach { tag ->
                            val isSelected = editingTag.equals(tag, ignoreCase = true)
                            Surface(
                                onClick = { editingTag = tag },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyberCyan else Color(0xFF142033),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyberCyan else Color(0x33475569))
                            ) {
                                Text(
                                    text = tag,
                                    color = if (isSelected) Color(0xFF04121F) else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetTags.drop(3).forEach { tag ->
                            val isSelected = editingTag.equals(tag, ignoreCase = true)
                            Surface(
                                onClick = { editingTag = tag },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyberCyan else Color(0xFF142033),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyberCyan else Color(0x33475569))
                            ) {
                                Text(
                                    text = tag,
                                    color = if (isSelected) Color(0xFF04121F) else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Private Note:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editingNote,
                        onValueChange = { editingNote = it },
                        placeholder = { Text("e.g. Electrician, met at conference...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0x33475569),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        callerViewModel.updateCallerNoteAndTag(phoneNumber, editingNote.ifBlank { null }, editingTag)
                        showTagDialog = false
                    }
                ) {
                    Text("Save Tag", color = NeonGreen, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isSpam) {
                        listOf(Color(0xFF26050E), DarkBackground, DarkBackground)
                    } else {
                        listOf(Color(0xFF07191C), DarkBackground, DarkBackground)
                    }
                )
            )
            .padding(20.dp)
            .testTag("caller_popup_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Status badge & Favorite Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSpam) SpamRed.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSpam) SpamRed else NeonGreen
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSpam) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isSpam) SpamRed else NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSpam) "⚠️ SPAM / FRAUD ALERT" else "⚡ VERIFIED CALLER ID",
                            color = if (isSpam) SpamRed else NeonGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Favorite Star Icon Button
                IconButton(
                    onClick = { callerViewModel.toggleFavorite(phoneNumber) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF142033))
                        .border(1.dp, Color(0x33475569), CircleShape)
                ) {
                    Icon(
                        imageVector = if (activeCaller?.isFavorite == true) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star Contact",
                        tint = if (activeCaller?.isFavorite == true) WarningAmber else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big Avatar with animated glow ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .background(
                        if (isSpam) Color(0x33FF3366) else Color(0x2600FF66),
                        CircleShape
                    )
                    .border(
                        2.dp,
                        if (isSpam) SpamRed else NeonGreen,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isSpam) Icons.Default.Warning else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isSpam) SpamRed else NeonGreen,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Caller Display Name
            Text(
                text = activeCaller?.name ?: callerName ?: "Unknown Caller",
                color = if (isSpam) SpamRed else TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Phone Number
            Text(
                text = activeCaller?.phoneNumber ?: phoneNumber,
                color = CyberCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Location, Carrier & Tags
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "📍 ${activeCaller?.city ?: "Unknown City"} • 📡 ${activeCaller?.carrier ?: "Cellular"}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                if (activeCaller?.tag != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyberCyan.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "🏷️ ${activeCaller?.tag}",
                            color = CyberCyan,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Middle: Detailed Spam / Caller Intelligence Card
            NeonGlassCard(
                modifier = Modifier.fillMaxWidth(),
                isSpamAlert = isSpam,
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMMUNITY THREAT RADAR",
                            color = TextMuted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "Score: ${analysis?.score ?: 0}/100",
                            color = if (isSpam) SpamRed else NeonGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { ((analysis?.score ?: 0) / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isSpam) SpamRed else NeonGreen,
                        trackColor = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = analysis?.warningReason ?: "Clean number with zero fraud reports.",
                        color = if (isSpam) Color(0xFFFF99AA) else TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (!activeCaller?.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📝 Note: ${activeCaller?.note}",
                            color = CyberCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Action Icons (Tag, WhatsApp, Copy, Save, Share)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionRoundButton(
                            icon = Icons.Default.Bookmark,
                            label = "Tag / Note",
                            onClick = { showTagDialog = true }
                        )
                        QuickActionRoundButton(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy",
                            onClick = { PhoneActionHelper.copyToClipboard(context, phoneNumber) }
                        )
                        QuickActionRoundButton(
                            icon = Icons.Default.PersonAdd,
                            label = "Save",
                            onClick = { PhoneActionHelper.openSaveContact(context, phoneNumber, activeCaller?.name) }
                        )
                        QuickActionRoundButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = { PhoneActionHelper.shareNumber(context, phoneNumber, activeCaller?.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Call Action Buttons: Decline/Hang up and Answer/Dial, plus Block/Report
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Secondary row: Quick Block / Report button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonOutlinedButton(
                        onClick = {
                            callerViewModel.blockNumber(phoneNumber, !(activeCaller?.isBlocked ?: false))
                            Toast.makeText(context, "Block status updated", Toast.LENGTH_SHORT).show()
                        },
                        borderColor = if (activeCaller?.isBlocked == true) NeonGreen else SpamRed,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = if (activeCaller?.isBlocked == true) NeonGreen else SpamRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeCaller?.isBlocked == true) "Unblock" else "Block Number",
                            fontSize = 13.sp
                        )
                    }

                    NeonOutlinedButton(
                        onClick = {
                            callerViewModel.reportSpam(phoneNumber, "User reported fraud / robocall", activeCaller?.name)
                            Toast.makeText(context, "Reported as Fraud Spam!", Toast.LENGTH_SHORT).show()
                        },
                        borderColor = WarningAmber,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Report Fraud",
                            color = WarningAmber,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large Main Answer & Decline Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline / Dismiss
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(SpamRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline Call",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Decline", color = TextSecondary, fontSize = 12.sp)
                    }

                    // Open WhatsApp Quick
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            onClick = { PhoneActionHelper.openWhatsApp(context, phoneNumber) },
                            shape = CircleShape,
                            color = Color(0xFF25D366),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("💬", fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "WhatsApp", color = TextSecondary, fontSize = 12.sp)
                    }

                    // Answer / Open Dialer
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                PhoneActionHelper.openDialer(context, phoneNumber)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Answer Call",
                                tint = Color(0xFF041208),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Answer", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF131D30))
                .border(1.dp, Color(0x33475569), CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CyberCyan,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
