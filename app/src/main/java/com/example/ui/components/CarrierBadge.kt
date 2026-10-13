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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.CarrierResolver

@Composable
fun CarrierBadge(
    phoneNumber: String,
    modifier: Modifier = Modifier
) {
    val origin = remember(phoneNumber) { CarrierResolver.resolve(phoneNumber) }

    val (badgeText, badgeColor) = when (origin.carrierCode) {
        "NTC" -> "NTC Telecom" to origin.brandColor
        "NCELL" -> "Ncell 4G/5G" to origin.brandColor
        "SMART" -> "Smart Cell" to origin.brandColor
        "LANDLINE" -> origin.region to origin.brandColor
        "JIO" -> "Jio 5G" to origin.brandColor
        "AIRTEL" -> "Airtel" to origin.brandColor
        "US" -> "USA / Canada" to origin.brandColor
        "UAE" -> "UAE (Dubai)" to origin.brandColor
        "KSA" -> "Saudi Arabia" to origin.brandColor
        "QATAR" -> "Qatar" to origin.brandColor
        else -> origin.carrier to origin.brandColor
    }

    Box(
        modifier = modifier
            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(badgeColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = badgeText,
                color = if (badgeColor == Color.White) Color.White else badgeColor,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        }
    }
}
