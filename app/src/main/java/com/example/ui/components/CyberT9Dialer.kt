package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextOnNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class KeypadKey(
    val digit: String,
    val letters: String
)

@Composable
fun CyberT9Keypad(
    dialerInput: String,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onCallClick: (String) -> Unit,
    onWhatsAppClick: (String) -> Unit,
    onSaveContactClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf(KeypadKey("1", "VOICEMAIL"), KeypadKey("2", "ABC"), KeypadKey("3", "DEF")),
        listOf(KeypadKey("4", "GHI"), KeypadKey("5", "JKL"), KeypadKey("6", "MNO")),
        listOf(KeypadKey("7", "PQRS"), KeypadKey("8", "TUV"), KeypadKey("9", "WXYZ")),
        listOf(KeypadKey("*", ""), KeypadKey("0", "+"), KeypadKey("#", ""))
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xE608101E))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0x4000FF66),
                        Color(0x3300F0FF),
                        Color(0x2000FF66)
                    )
                ),
                RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display Typed Number
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (dialerInput.isBlank()) {
                    Text(
                        text = "Enter phone number or T9 name...",
                        color = TextMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = dialerInput,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            if (dialerInput.isNotEmpty()) {
                IconButton(
                    onClick = onBackspaceClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Backspace",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4x3 Grid Keypad
        keys.forEach { rowKeys ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowKeys.forEach { key ->
                    T9KeyButton(
                        key = key,
                        onClick = { onDigitClick(key.digit) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row: Call, WhatsApp, Add Contact
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Save Contact
            IconButton(
                onClick = { if (dialerInput.isNotBlank()) onSaveContactClick(dialerInput) },
                enabled = dialerInput.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF131D30))
                    .border(1.dp, Color(0x33475569), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Contact",
                    tint = if (dialerInput.isNotBlank()) CyberCyan else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Central Neon Call Button
            Surface(
                onClick = { if (dialerInput.isNotBlank()) onCallClick(dialerInput) },
                enabled = dialerInput.isNotBlank(),
                shape = CircleShape,
                color = if (dialerInput.isNotBlank()) NeonGreen else Color(0xFF1A3326),
                modifier = Modifier
                    .size(58.dp)
                    .testTag("t9_call_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = if (dialerInput.isNotBlank()) TextOnNeon else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // WhatsApp Message Button
            IconButton(
                onClick = { if (dialerInput.isNotBlank()) onWhatsAppClick(dialerInput) },
                enabled = dialerInput.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x2425D366))
                    .border(1.dp, Color(0x5525D366), CircleShape)
            ) {
                Text(
                    text = "💬",
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun T9KeyButton(
    key: KeypadKey,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "key_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = if (isPressed) Color(0xFF1F3554) else Color(0xFF101B2E),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPressed) NeonGreen else Color(0x33475569)
        ),
        modifier = Modifier
            .size(62.dp)
            .scale(scale)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(2.dp)
        ) {
            Text(
                text = key.digit,
                color = if (isPressed) NeonGreen else TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 22.sp
            )
            if (key.letters.isNotBlank()) {
                Text(
                    text = key.letters,
                    color = TextMuted,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
