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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import com.example.data.model.DocumentData
import com.example.ui.components.CallNoteDialog
import com.example.ui.components.CyberWaveformVisualizer
import com.example.ui.components.ScamRadarGauge
import com.example.ui.components.VCardExportSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.ui.components.CarrierBadge
import com.example.ui.components.NeonGlassCard
import com.example.ui.components.NeonOutlinedButton
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.utils.PhoneActionHelper
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
    val aiSecurityAudit by callerViewModel.aiSecurityAudit.collectAsStateWithLifecycle()
    val isAuditingAi by callerViewModel.isAuditingAi.collectAsStateWithLifecycle()

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
    var showVCardSheet by remember { mutableStateOf(false) }

    // Cyber Call Note & Tag Dialog
    if (showTagDialog) {
        CallNoteDialog(
            phoneNumber = phoneNumber,
            callerName = activeCaller?.name ?: callerName,
            initialNote = activeCaller?.note,
            initialTag = activeCaller?.tag,
            onDismiss = { showTagDialog = false },
            onSave = { note, tag ->
                callerViewModel.updateCallerNoteAndTag(phoneNumber, note, tag)
            }
        )
    }

    // AI Smart vCard Exporter Studio Sheet
    if (showVCardSheet) {
        val docData = DocumentData(
            businessName = activeCaller?.category?.takeIf { it != "Unknown" && it.isNotBlank() },
            contactName = activeCaller?.name ?: callerName,
            phoneNumbers = listOf(
                com.example.data.model.ScanResult(
                    rawText = phoneNumber,
                    phoneNumber = phoneNumber,
                    formattedNumber = phoneNumber,
                    countryName = activeCaller?.country ?: "US"
                )
            ),
            addresses = listOfNotNull(activeCaller?.city?.takeIf { it != "Unknown" && it.isNotBlank() })
        )
        VCardExportSheet(
            documentData = docData,
            onDismiss = { showVCardSheet = false }
        )
    }

    // Spam Report Dialog
    if (showReportDialog) {
        var reportReason by remember { mutableStateOf("Fraud / Bank Phishing Scam") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = DarkSurfaceVariant,
            title = {
                Text(
                    text = "Report Suspicious Number",
                    color = SpamRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Why are you reporting $phoneNumber?",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val reasons = listOf(
                        "Fraud / Bank Phishing Scam",
                        "Robocall / Automated Sales",
                        "Harassment / Threat Call",
                        "Lottery / Prize Scam",
                        "Fake Delivery / Courier Agent"
                    )
                    reasons.forEach { r ->
                        val isSelected = reportReason == r
                        Surface(
                            onClick = { reportReason = r },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) SpamRed.copy(alpha = 0.2f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) SpamRed else Color(0x22475569)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Text(
                                text = r,
                                color = if (isSelected) SpamRed else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        callerViewModel.reportSpam(phoneNumber, reportReason, activeCaller?.name)
                        showReportDialog = false
                        Toast.makeText(context, "Report submitted! Number blocked locally.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Submit & Block", color = SpamRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("caller_popup_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation / Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSpam) SpamRed.copy(alpha = 0.18f) else NeonGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSpam) SpamRed.copy(alpha = 0.5f) else NeonGreen.copy(alpha = 0.5f)
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
                    .size(100.dp)
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
                    modifier = Modifier.size(50.dp)
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

            Spacer(modifier = Modifier.height(6.dp))

            // Carrier Badge and Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CarrierBadge(phoneNumber = activeCaller?.phoneNumber ?: phoneNumber)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📍 ${activeCaller?.city ?: "Unknown City"}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                if (activeCaller?.tag != null) {
                    Spacer(modifier = Modifier.width(6.dp))
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

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Audio & Telemetry Waveform Visualizer
            CyberWaveformVisualizer(
                width = 130.dp,
                height = 24.dp,
                isActive = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gemini AI Deep Security Audit Card
            NeonGlassCard(
                modifier = Modifier.fillMaxWidth(),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GEMINI AI SCAM AUDIT",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }

                        if (!isAuditingAi) {
                            Surface(
                                onClick = { callerViewModel.auditCallerWithAi(phoneNumber) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF132238),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = if (aiSecurityAudit == null) "Run Audit" else "Re-Analyze",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isAuditingAi) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CyberCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Evaluating fraud heuristics & telecom databases...",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    } else if (aiSecurityAudit != null) {
                        val audit = aiSecurityAudit!!
                        val auditColor = when (audit.riskLevel) {
                            "CRITICAL FRAUD" -> SpamRed
                            "HIGH RISK" -> WarningAmber
                            "VERIFIED SAFE" -> NeonGreen
                            else -> CyberCyan
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = auditColor.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, auditColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "⚡ ${audit.riskLevel}",
                                    color = auditColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Text(
                                text = "${audit.confidencePercent}% Confidence",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Threat: ${audit.threatCategory}",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = audit.analysisSummary,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            audit.safetyRecommendations.forEach { rec ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("🛡️", fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = rec, color = Color(0xFFE2E8F0), fontSize = 11.5.sp)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Tap 'Run Audit' to verify this caller against phishing, OTP extortion, and Wangiri scam patterns.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Community Threat Holographic Radar Gauge Card
            NeonGlassCard(
                modifier = Modifier.fillMaxWidth(),
                isSpamAlert = isSpam,
                cornerRadius = 22.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ScamRadarGauge(
                        spamScore = analysis?.score ?: (activeCaller?.spamScore ?: 0),
                        reportsCount = activeCaller?.reportsCount ?: 0,
                        carrier = activeCaller?.carrier ?: "Cellular",
                        size = 180.dp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = analysis?.warningReason ?: "Clean number with zero fraud reports.",
                        color = if (isSpam) Color(0xFFFF99AA) else TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    if (!activeCaller?.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x3300F0FF)
                        ) {
                            Text(
                                text = "📝 Note: ${activeCaller?.note}",
                                color = CyberCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Action Icons (Tag/Note, vCard Studio, SMS, Copy, Save, Share)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionRoundButton(
                            icon = Icons.Default.Bookmark,
                            label = "Note",
                            onClick = { showTagDialog = true }
                        )
                        QuickActionRoundButton(
                            icon = Icons.Default.ContactPage,
                            label = "vCard",
                            onClick = { showVCardSheet = true }
                        )
                        QuickActionRoundButton(
                            icon = Icons.AutoMirrored.Filled.Message,
                            label = "SMS",
                            onClick = { PhoneActionHelper.sendSms(context, phoneNumber) }
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

            // Block & Report Row
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
                    onClick = { showReportDialog = true },
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

            Spacer(modifier = Modifier.height(20.dp))

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
