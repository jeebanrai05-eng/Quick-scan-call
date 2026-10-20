package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentData
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.PhoneActionHelper

@Composable
fun DocumentSummaryCard(
    documentData: DocumentData,
    modifier: Modifier = Modifier,
    onSaveAllAsContact: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(true) }

    val hasDetails = !documentData.businessName.isNullOrBlank() ||
            !documentData.contactName.isNullOrBlank() ||
            documentData.emails.isNotEmpty() ||
            documentData.websites.isNotEmpty() ||
            documentData.addresses.isNotEmpty()

    if (!hasDetails && documentData.detectedCountry == null) return

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xF207101E),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, CyberCyan.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Country badge, Confidence & Collapse toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CyberCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EXTRACTED DOCUMENT INTELLIGENCE",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (documentData.detectedCountry != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x3300F0FF)
                        ) {
                            Text(
                                text = documentData.detectedCountry,
                                color = CyberCyan,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Summary Line when collapsed or header
            if (!documentData.businessName.isNullOrBlank() || !documentData.contactName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = documentData.businessName ?: documentData.contactName ?: "",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    // 1. Business / Organization
                    if (!documentData.businessName.isNullOrBlank()) {
                        DetailRowItem(
                            icon = Icons.Default.Business,
                            label = "Organization / Shop",
                            value = documentData.businessName,
                            iconColor = NeonGreen
                        )
                    }

                    // 2. Contact Person
                    if (!documentData.contactName.isNullOrBlank()) {
                        DetailRowItem(
                            icon = Icons.Default.Person,
                            label = "Contact Person",
                            value = documentData.contactName,
                            iconColor = CyberCyan
                        )
                    }

                    // 3. Email
                    for (email in documentData.emails) {
                        DetailRowItem(
                            icon = Icons.Default.Email,
                            label = "Email",
                            value = email,
                            iconColor = Color(0xFFFACC15),
                            onClick = { PhoneActionHelper.openEmail(context, email) }
                        )
                    }

                    // 4. Website
                    for (web in documentData.websites) {
                        DetailRowItem(
                            icon = Icons.Default.Language,
                            label = "Website",
                            value = web,
                            iconColor = Color(0xFF38BDF8),
                            onClick = { PhoneActionHelper.openWebsite(context, web) }
                        )
                    }

                    // 5. Address
                    for (address in documentData.addresses) {
                        DetailRowItem(
                            icon = Icons.Default.LocationOn,
                            label = "Address / Location",
                            value = address,
                            iconColor = Color(0xFFFB7185),
                            onClick = { PhoneActionHelper.openAddressInMap(context, address) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Action Bar: Save All as Contact & Copy Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                val firstPhone = documentData.phoneNumbers.firstOrNull()?.phoneNumber ?: ""
                                PhoneActionHelper.openSaveContact(
                                    context = context,
                                    phoneNumber = firstPhone,
                                    name = documentData.contactName ?: documentData.businessName,
                                    company = documentData.businessName,
                                    email = documentData.emails.firstOrNull(),
                                    postalAddress = documentData.addresses.firstOrNull()
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = NeonGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SAVE AS CONTACT",
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                val summary = buildString {
                                    documentData.businessName?.let { appendLine("Business: $it") }
                                    documentData.contactName?.let { appendLine("Contact: $it") }
                                    documentData.phoneNumbers.forEach { appendLine("Phone: ${it.formattedNumber}") }
                                    documentData.emails.forEach { appendLine("Email: $it") }
                                    documentData.websites.forEach { appendLine("Web: $it") }
                                    documentData.addresses.forEach { appendLine("Address: $it") }
                                }
                                PhoneActionHelper.copyToClipboard(context, summary, "Document Info")
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x22FFFFFF)
                        ) {
                            Text(
                                text = "Copy Summary",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconColor: Color,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        color = Color.Transparent,
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = TextMuted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
