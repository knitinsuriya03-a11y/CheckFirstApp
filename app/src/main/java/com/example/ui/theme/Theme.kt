package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.LocalStrings
import com.example.ui.i18n.TamilStrings

private val CheckFirstColorScheme = darkColorScheme(
    primary = CosmicViolet,
    onPrimary = Color.White,
    primaryContainer = CosmicVioletTint,
    onPrimaryContainer = CosmicVioletLight,
    secondary = CosmicVioletGlow,
    onSecondary = Color.White,
    secondaryContainer = ObsidianSurfaceRaised,
    onSecondaryContainer = TextPrimary,
    tertiary = ClearEmerald,
    onTertiary = ObsidianVoid,
    tertiaryContainer = ClearBg,
    onTertiaryContainer = ClearEmerald,
    error = DangerRed,
    onError = ObsidianVoid,
    errorContainer = DangerBg,
    onErrorContainer = DangerRed,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceRaised,
    onSurfaceVariant = TextMuted,
    outline = HairlineBorder,
    outlineVariant = HairlineTrack,
)

private val CheckFirstShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun CheckFirstTheme(
    largeText: Boolean = false,
    tamil: Boolean = false,
    content: @Composable () -> Unit,
) {
    val typography = remember(largeText) { buildCheckFirstTypography(largeText) }
    val strings = if (tamil) TamilStrings else EnglishStrings
    CompositionLocalProvider(
        LocalCheckFirstColors provides CheckFirstExtendedColors(),
        LocalStrings provides strings,
    ) {
        MaterialTheme(
            colorScheme = CheckFirstColorScheme,
            typography = typography,
            shapes = CheckFirstShapes,
            content = content,
        )
    }
}
