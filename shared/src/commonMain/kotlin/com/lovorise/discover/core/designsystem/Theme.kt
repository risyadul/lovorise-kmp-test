package com.lovorise.discover.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LovoriseColorScheme = lightColorScheme(
    primary = LovoriseColors.Pink,
    onPrimary = LovoriseColors.White,
    primaryContainer = LovoriseColors.PinkSoft,
    onPrimaryContainer = LovoriseColors.Pink,
    secondary = LovoriseColors.Slate,
    onSecondary = LovoriseColors.White,
    background = LovoriseColors.White,
    onBackground = LovoriseColors.Ink,
    surface = LovoriseColors.White,
    onSurface = LovoriseColors.Ink,
    surfaceVariant = LovoriseColors.SurfaceDim,
    onSurfaceVariant = LovoriseColors.Slate,
    outline = LovoriseColors.Border,
    outlineVariant = LovoriseColors.Border,
    error = LovoriseColors.Pink,
)

private val LovoriseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LovoriseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LovoriseColorScheme,
        typography = lovoriseTypography(),
        shapes = LovoriseShapes,
        content = content,
    )
}
