package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CallerType
import com.example.data.model.ScanResult
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.PhoneActionHelper

@Composable
fun DetectedNumberCard(
    result: ScanResult,
    modifier: Modifier = Modifier,
    onCallClick: (() -> Unit)? = null,
    onDetailsClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val caller = result.callerInfo
    val isSpam = caller?.isSpam == true
    var showQuickMessageSheet by remember { mutableStateOf(false) }

    if (showQuickMessageSheet) {
        Dialog(
            onDismissRequest = { showQuickMessageSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                QuickMessageSheet(
                    phoneNumber = result.phoneNumber,
                    callerName = result.contactName ?: caller?.name,
                    onDismiss = { showQuickMessageSheet = false }
                )
            }
        }
    }

    NeonGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("detected_number_card_${result.phoneNumber}"),
        isSpamAlert = isSpam
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Source Label, Country Badge, Edit / Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "🎯 ${result.label.uppercase()}",
                            color = CyberCyan,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    if (result.countryName != null || result.countryCode != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, NeonGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = result.countryName ?: result.countryCode ?: "",
                                color = NeonGreen,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    CarrierBadge(phoneNumber = result.phoneNumber)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onEditClick != null) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = CyberCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    if (onDeleteClick != null) {
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Extracted Contact Name or Business if present
            val displayName = result.contactName ?: caller?.name
            val displayBusiness = result.businessName ?: (if (displayName != null) caller?.category else null)

            if (!displayName.isNullOrBlank() || !displayBusiness.isNullOrBlank()) {
                Surface(
                    onClick = { onDetailsClick?.invoke() },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (!displayName.isNullOrBlank()) {
                                Text(
                                    text = displayName,
                                    color = if (isSpam) SpamRed else TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (!displayBusiness.isNullOrBlank()) {
                                Text(
                                    text = "🏢 $displayBusiness",
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        if (caller != null) {
                            CallerBadge(callerType = caller.callerType, spamScore = caller.spamScore)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Big Detected Phone Number
            Text(
                text = result.formattedNumber,
                color = if (isSpam) SpamRed else NeonGreen,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            // Context line
            if (result.rawText.isNotBlank() && result.rawText != result.phoneNumber) {
                Text(
                    text = "\"${result.rawText.take(65)}\"",
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call Now (Primary Button)
                NeonPrimaryButton(
                    onClick = {
                        onCallClick?.invoke() ?: PhoneActionHelper.openDialer(context, result.phoneNumber)
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("action_dial_${result.phoneNumber}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Call Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // WhatsApp Shortcut
                Surface(
                    onClick = { showQuickMessageSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF25D366).copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "💬", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.width(5.dp))

                // Speed SMS Shortcut
                IconButton(
                    onClick = { showQuickMessageSheet = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "SMS",
                        tint = CyberCyan,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                // Copy Number
                IconButton(
                    onClick = {
                        PhoneActionHelper.copyToClipboard(context, result.phoneNumber, "Phone Number")
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy number",
                        tint = TextPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                // Save to Contacts
                IconButton(
                    onClick = {
                        PhoneActionHelper.openSaveContact(
                            context = context,
                            phoneNumber = result.phoneNumber,
                            name = displayName,
                            company = result.businessName,
                            email = result.email,
                            postalAddress = result.address
                        )
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Save contact",
                        tint = TextPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                // Web Search
                IconButton(
                    onClick = {
                        val query = Uri.encode(result.phoneNumber)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
