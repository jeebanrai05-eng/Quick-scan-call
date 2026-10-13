package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.CarrierResolver
import com.example.utils.PhoneActionHelper

data class UssdShortcut(
    val title: String,
    val code: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun UtilityShortcutsCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("NTC") }

    val ntcShortcuts = listOf(
        UssdShortcut("Data & Packs", "*1415#", "Buy 4G Internet & Voice", Icons.Default.SimCard, CarrierResolver.NtcBlue),
        UssdShortcut("Check Number", "*400#", "View My NTC Mobile No.", Icons.Default.SimCard, CarrierResolver.NtcBlue),
        UssdShortcut("Check Balance", "*1414#", "Account Balance & Expiry", Icons.Default.SimCard, CarrierResolver.NtcBlue),
        UssdShortcut("Balance Transfer", "*422#", "Transfer Balance to NTC", Icons.Default.SimCard, CarrierResolver.NtcBlue)
    )

    val ncellShortcuts = listOf(
        UssdShortcut("All Packs", "*17123#", "Internet, Voice & SMS", Icons.Default.SimCard, CarrierResolver.NcellPurple),
        UssdShortcut("Check Number", "*103#", "View My Ncell Mobile No.", Icons.Default.SimCard, CarrierResolver.NcellPurple),
        UssdShortcut("Check Balance", "*901#", "Account Balance & Validity", Icons.Default.SimCard, CarrierResolver.NcellPurple),
        UssdShortcut("Saapati Loan", "*9988#", "Emergency Airtime Saapati", Icons.Default.SimCard, CarrierResolver.NcellPurple)
    )

    val emergencyShortcuts = listOf(
        UssdShortcut("Police Control", "100", "Nepal Police Emergency", Icons.Default.LocalPolice, SpamRed),
        UssdShortcut("Ambulance", "102", "Emergency Medical Services", Icons.Default.LocalHospital, Color(0xFFFF5252)),
        UssdShortcut("Fire Brigade", "101", "Damkal Fire Emergency", Icons.Default.Emergency, Color(0xFFFF9100)),
        UssdShortcut("Traffic Police", "103", "Highway & Valley Traffic", Icons.Default.LocalPolice, CyberCyan)
    )

    val activeList = when (selectedCategory) {
        "NTC" -> ntcShortcuts
        "NCELL" -> ncellShortcuts
        else -> emergencyShortcuts
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "QUICK UTILITY & USSD HUB",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "1-Tap Balance, Packs & Emergency Dialers",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Category selector chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("NTC", "NCELL", "SOS").forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val catColor = when (cat) {
                            "NTC" -> CarrierResolver.NtcBlue
                            "NCELL" -> CarrierResolver.NcellPurple
                            else -> SpamRed
                        }

                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) catColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) catColor else Color.White.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal shortcuts carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeList) { shortcut ->
                    Surface(
                        onClick = { PhoneActionHelper.openDialer(context, shortcut.code) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F1A2B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, shortcut.color.copy(alpha = 0.4f)),
                        modifier = Modifier.width(160.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(shortcut.color.copy(alpha = 0.2f))
                                        .border(1.dp, shortcut.color.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = shortcut.icon,
                                        contentDescription = null,
                                        tint = shortcut.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = shortcut.color.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = shortcut.code,
                                        color = shortcut.color,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = shortcut.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = shortcut.subtitle,
                                color = TextMuted,
                                fontSize = 10.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
