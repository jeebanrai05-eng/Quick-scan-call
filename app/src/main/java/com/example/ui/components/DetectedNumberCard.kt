package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onCallClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val caller = result.callerInfo
    val isSpam = caller?.isSpam == true

    NeonGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("detected_number_card_${result.phoneNumber}"),
        isSpamAlert = isSpam
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Source Label & Caller Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "🎯 ${result.label.uppercase()}",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (caller != null) {
                    CallerBadge(callerType = caller.callerType, spamScore = caller.spamScore)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Caller name if known
            if (caller != null && caller.callerType != CallerType.UNKNOWN) {
                Text(
                    text = caller.name,
                    color = if (isSpam) SpamRed else TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "📍 ${caller.city} • 📡 ${caller.carrier}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Big Detected Phone Number
            Text(
                text = result.formattedNumber,
                color = if (isSpam) SpamRed else NeonGreen,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            // Context line
            if (result.rawText.isNotBlank() && result.rawText != result.phoneNumber) {
                Text(
                    text = "\"${result.rawText.take(60)}\"",
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call / Open dialer Button (Primary)
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
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Call Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // WhatsApp Shortcut
                Surface(
                    onClick = {
                        PhoneActionHelper.openWhatsApp(context, result.phoneNumber)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF25D366).copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "💬",
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Copy Number
                IconButton(
                    onClick = {
                        PhoneActionHelper.copyToClipboard(context, result.phoneNumber)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy number",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Save Contact
                IconButton(
                    onClick = {
                        PhoneActionHelper.openSaveContact(context, result.phoneNumber, caller?.name)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Save contact",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Share
                IconButton(
                    onClick = {
                        PhoneActionHelper.shareNumber(context, result.phoneNumber, caller?.name)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
