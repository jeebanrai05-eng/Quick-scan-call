package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallerType
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.VerifiedBlue
import com.example.ui.theme.WarningAmber

@Composable
fun CallerBadge(
    callerType: CallerType,
    spamScore: Int = 0,
    modifier: Modifier = Modifier
) {
    val (label, bgColor, borderColor, textColor, icon) = when {
        spamScore >= 80 || callerType == CallerType.SPAM -> {
            CallerBadgeConfig(
                label = "FRAUD SPAM ($spamScore%)",
                bgColor = SpamRed.copy(alpha = 0.2f),
                borderColor = SpamRed.copy(alpha = 0.8f),
                textColor = SpamRed,
                icon = Icons.Default.Warning
            )
        }
        spamScore in 40..79 -> {
            CallerBadgeConfig(
                label = "SUSPECTED SPAM",
                bgColor = WarningAmber.copy(alpha = 0.2f),
                borderColor = WarningAmber.copy(alpha = 0.8f),
                textColor = WarningAmber,
                icon = Icons.Default.Warning
            )
        }
        callerType == CallerType.VERIFIED -> {
            CallerBadgeConfig(
                label = "VERIFIED IDENTITY",
                bgColor = VerifiedBlue.copy(alpha = 0.2f),
                borderColor = VerifiedBlue.copy(alpha = 0.8f),
                textColor = VerifiedBlue,
                icon = Icons.Default.CheckCircle
            )
        }
        callerType == CallerType.CONTACT -> {
            CallerBadgeConfig(
                label = "SAVED CONTACT",
                bgColor = CyberCyan.copy(alpha = 0.2f),
                borderColor = CyberCyan.copy(alpha = 0.8f),
                textColor = CyberCyan,
                icon = Icons.Default.Person
            )
        }
        callerType == CallerType.BUSINESS -> {
            CallerBadgeConfig(
                label = "BUSINESS CALL",
                bgColor = NeonGreen.copy(alpha = 0.2f),
                borderColor = NeonGreen.copy(alpha = 0.8f),
                textColor = NeonGreen,
                icon = Icons.Default.Security
            )
        }
        else -> {
            CallerBadgeConfig(
                label = "UNLISTED NUMBER",
                bgColor = Color(0x3364748B),
                borderColor = Color(0x6664748B),
                textColor = Color(0xFFCBD5E1),
                icon = Icons.Default.Security
            )
        }
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

private data class CallerBadgeConfig(
    val label: String,
    val bgColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
