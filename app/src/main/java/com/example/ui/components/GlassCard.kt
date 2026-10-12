package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassBorderGreen
import com.example.ui.theme.DarkGlassCard

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderColor: Color = DarkGlassBorder,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = DarkGlassCard,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        modifier = modifier
            .clip(shape),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            content()
        }
    }
}

@Composable
fun NeonGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    isSpamAlert: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val borderColor = if (isSpamAlert) Color(0xFFFF3366).copy(alpha = 0.5f) else DarkGlassBorderGreen
    val bgColor = if (isSpamAlert) Color(0xEB1F0B15) else DarkGlassCard

    GlassCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        borderColor = borderColor,
        borderWidth = 1.5.dp,
        backgroundColor = bgColor,
        content = content
    )
}
