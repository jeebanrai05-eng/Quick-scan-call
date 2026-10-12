package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallerInfo
import com.example.ui.components.CallerBadge
import com.example.ui.components.CyberT9Keypad
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
import com.example.viewmodel.CallerViewModel

enum class SearchTab {
    T9_DIALER,
    DIRECTORY
}

@Composable
fun SearchScreen(
    callerViewModel: CallerViewModel,
    onNavigateToCallerPopup: (String, String) -> Unit
) {
    val context = LocalContext.current
    val searchQuery by callerViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by callerViewModel.searchResults.collectAsStateWithLifecycle()
    val allCallers by callerViewModel.allCallers.collectAsStateWithLifecycle()
    val dialerInput by callerViewModel.dialerInput.collectAsStateWithLifecycle()
    val t9Results by callerViewModel.t9Results.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(SearchTab.T9_DIALER) }
    var selectedTagFilter by remember { mutableStateOf("ALL") }

    val rawList = if (searchQuery.isBlank()) allCallers else searchResults
    val displayList = remember(rawList, selectedTagFilter) {
        when (selectedTagFilter) {
            "FAVORITES" -> rawList.filter { it.isFavorite }
            "WORK" -> rawList.filter { it.tag.equals("WORK", ignoreCase = true) || it.category.contains("work", true) }
            "FAMILY" -> rawList.filter { it.tag.equals("FAMILY", ignoreCase = true) }
            "DELIVERY" -> rawList.filter { it.tag.equals("DELIVERY", ignoreCase = true) }
            "SPAM" -> rawList.filter { it.isSpam }
            else -> rawList
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
            .testTag("search_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COMMUNICATION HUB",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (activeTab == SearchTab.T9_DIALER) "Smart T9 Keypad" else "Directory Lookup",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF142033),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "${allCallers.size} Contacts",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Top Modern Tab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF101B2E))
                .padding(4.dp)
        ) {
            val isT9 = activeTab == SearchTab.T9_DIALER
            val t9Bg by animateColorAsState(
                targetValue = if (isT9) NeonGreen else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "t9_tab_bg"
            )
            val dirBg by animateColorAsState(
                targetValue = if (!isT9) NeonGreen else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "dir_tab_bg"
            )

            // T9 Button
            Surface(
                onClick = { activeTab = SearchTab.T9_DIALER },
                shape = RoundedCornerShape(12.dp),
                color = t9Bg,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Dialpad,
                        contentDescription = null,
                        tint = if (isT9) TextOnNeon else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "T9 Keypad",
                        color = if (isT9) TextOnNeon else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Directory Button
            Surface(
                onClick = { activeTab = SearchTab.DIRECTORY },
                shape = RoundedCornerShape(12.dp),
                color = dirBg,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (!isT9) TextOnNeon else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Directory",
                        color = if (!isT9) TextOnNeon else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (activeTab) {
            SearchTab.T9_DIALER -> {
                // T9 Mode: Live Matched Contacts + Cyber Keypad
                Column(modifier = Modifier.fillMaxSize()) {
                    // Matched contacts preview list
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (t9Results.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (dialerInput.isBlank()) "Type digits on keypad to search contacts..." else "No contact matches for '$dialerInput'",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                items(t9Results, key = { it.normalizedNumber }) { caller ->
                                    T9MatchedContactItem(
                                        caller = caller,
                                        onItemClick = { onNavigateToCallerPopup(caller.phoneNumber, caller.name) },
                                        onCallClick = { PhoneActionHelper.openDialer(context, caller.phoneNumber) },
                                        onWhatsAppClick = { PhoneActionHelper.openWhatsApp(context, caller.phoneNumber) },
                                        onFavoriteToggle = { callerViewModel.toggleFavorite(caller.phoneNumber) }
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Cyber T9 Keypad
                    CyberT9Keypad(
                        dialerInput = dialerInput,
                        onDigitClick = { digit -> callerViewModel.onDialerDigit(digit) },
                        onBackspaceClick = { callerViewModel.onDialerBackspace() },
                        onCallClick = { number -> PhoneActionHelper.openDialer(context, number) },
                        onWhatsAppClick = { number -> PhoneActionHelper.openWhatsApp(context, number) },
                        onSaveContactClick = { number -> PhoneActionHelper.openSaveContact(context, number) },
                        modifier = Modifier.padding(bottom = 110.dp)
                    )
                }
            }

            SearchTab.DIRECTORY -> {
                // Directory Lookup Mode
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { callerViewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_field"),
                        placeholder = { Text("Search by name, number, or note...", color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = NeonGreen
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { callerViewModel.onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = DarkGlassBorder,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tag Filter Chips
                    val tags = listOf(
                        "ALL" to "All",
                        "FAVORITES" to "⭐ Starred",
                        "WORK" to "💼 Work",
                        "FAMILY" to "🏠 Family",
                        "DELIVERY" to "📦 Delivery",
                        "SPAM" to "⚠️ Spam"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(tags) { (key, label) ->
                            val isSelected = selectedTagFilter == key
                            Surface(
                                onClick = { selectedTagFilter = key },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) CyberCyan else Color(0xFF131D30),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CyberCyan else Color(0x33475569)
                                )
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF04121F) else TextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Directory Contacts List
                    if (displayList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No contacts found",
                                    color = TextSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            items(displayList, key = { it.normalizedNumber }) { caller ->
                                DirectoryContactCard(
                                    caller = caller,
                                    onItemClick = { onNavigateToCallerPopup(caller.phoneNumber, caller.name) },
                                    onCallClick = { PhoneActionHelper.openDialer(context, caller.phoneNumber) },
                                    onWhatsAppClick = { PhoneActionHelper.openWhatsApp(context, caller.phoneNumber) },
                                    onFavoriteToggle = { callerViewModel.toggleFavorite(caller.phoneNumber) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun T9MatchedContactItem(
    caller: CallerInfo,
    onItemClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val initials = if (caller.name.isNotBlank()) caller.name.take(2).uppercase() else "#"

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        cornerRadius = 14.dp,
        borderColor = if (caller.isSpam) SpamRed.copy(alpha = 0.4f) else DarkGlassBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (caller.isSpam) Color(0xFF4A101C) else Color(0xFF132238))
                        .border(1.dp, if (caller.isSpam) SpamRed else CyberCyan.copy(alpha = 0.5f), CircleShape)
                ) {
                    Text(
                        text = initials,
                        color = if (caller.isSpam) SpamRed else CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = caller.name,
                            color = if (caller.isSpam) SpamRed else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (caller.isFavorite) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("⭐", fontSize = 11.sp)
                        }
                    }
                    Text(
                        text = caller.phoneNumber,
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Text("💬", fontSize = 15.sp)
                }

                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectoryContactCard(
    caller: CallerInfo,
    onItemClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val initials = if (caller.name.isNotBlank()) caller.name.take(2).uppercase() else "#"

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("caller_card_${caller.phoneNumber}"),
        cornerRadius = 16.dp,
        borderColor = if (caller.isSpam) SpamRed.copy(alpha = 0.45f) else DarkGlassBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
                            if (caller.isSpam) {
                                Brush.linearGradient(listOf(Color(0xFF5A0E1F), Color(0xFF28070E)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF1E3250), Color(0xFF0F1A2B)))
                            }
                        )
                        .border(
                            1.dp,
                            if (caller.isSpam) SpamRed.copy(alpha = 0.6f) else CyberCyan.copy(alpha = 0.4f),
                            CircleShape
                        )
                ) {
                    Text(
                        text = initials,
                        color = if (caller.isSpam) SpamRed else CyberCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = caller.name,
                            color = if (caller.isSpam) SpamRed else TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        CallerBadge(callerType = caller.callerType)
                    }

                    Text(
                        text = caller.phoneNumber,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (caller.tag != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CyberCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "🏷️ ${caller.tag}",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (caller.note != null) {
                            Text(
                                text = "📝 ${caller.note}",
                                color = TextMuted,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }

            // Quick Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Favorite Star
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (caller.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (caller.isFavorite) WarningAmber else TextMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // WhatsApp
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .size(36.dp)
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = NeonGreen,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
