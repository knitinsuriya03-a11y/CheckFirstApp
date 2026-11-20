package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Obsidian & Cosmic Violet Reference Theme Palette
val ObsidianVoid = Color(0xFF07060C)         // Deep cosmic-black background canvas
val ObsidianBar = Color(0xFF0B0A13)          // Top bar, bottom bar, navigation rail/drawer
val ObsidianSurface = Color(0xFF111019)      // Primary elevated card surface
val ObsidianSurfaceRaised = Color(0xFF181624) // Nested panels, secondary pills, inputs
val ObsidianSurfaceGlow = Color(0xFF171226)  // Subtle violet-tinted featured surface

val CosmicViolet = Color(0xFF8B3DFF)         // Signature electric violet primary CTA & brand accent
val CosmicVioletPressed = Color(0xFF7229E8)  // Deeper electric violet
val CosmicVioletGlow = Color(0xFFA855F7)     // Luminous violet accent, chart stroke, active icons
val CosmicVioletLight = Color(0xFFC49BFF)    // Soft lavender highlight for eyebrows & links
val CosmicVioletTint = Color(0xFF21163A)     // Translucent violet badge/chip/selected container
val CosmicVioletBorder = Color(0xFF3E296B)   // Violet accent hairline border
val CosmicHorizonCore = Color(0xFFEBD6FF)    // Crisp luminous horizon arc highlight

val TextPrimary = Color(0xFFF8F7FC)          // Crisp near-white primary text (>16:1 on card)
val TextBody = Color(0xFFD6D3E6)             // Comfortable silver-white reading copy (>11:1)
val TextMuted = Color(0xFF9C96B8)            // Secondary metadata & helper labels (>6:1)
val TextSubtle = Color(0xFF7D7899)           // Captions & disclaimers (>4.5:1 AA)

val HairlineBorder = Color(0xFF262338)       // Crisp subtle card & input border
val HairlineTrack = Color(0xFF1D1A2C)        // Dividers, chart gridlines, progress tracks
val SecondaryPillBg = Color(0xFF1D1B2A)      // Reference "Learn More" dark pill fill
val SecondaryPillBorder = Color(0xFF312D45)  // Reference "Learn More" dark pill hairline

// Semantic Risk & Status Tokens (Dark-Native High Contrast)
val DangerRed = Color(0xFFFF6B81)
val DangerBg = Color(0xFF2C1119)
val DangerBorder = Color(0xFF5D2233)
val DangerCardBg = Color(0xFF1C0D14)

val CautionAmber = Color(0xFFFBBF24)
val CautionIconAmber = Color(0xFFF59E0B)
val CautionChartAmber = Color(0xFFFBBF24)
val CautionBg = Color(0xFF291E0B)
val CautionBorder = Color(0xFF594014)
val CautionCardBg = Color(0xFF1A140B)
val HighlightMarkBg = Color(0xFF4A3310)
val HighlightMarkText = Color(0xFFFDE68A)

val ClearEmerald = Color(0xFF34D399)
val ClearBg = Color(0xFF0C2920)
val ClearBorder = Color(0xFF1B5240)
val ClearCardBg = Color(0xFF0B1C17)

val EmergencyBannerBg = Color(0xFF241019)
val EmergencyBannerBorder = Color(0xFF5B2336)
val EmergencySymbolBg = Color(0xFF3D1725)
val EmergencySymbolTint = Color(0xFFFF7A8F)

val HeroEyebrowLight = Color(0xFFC49BFF)
val HeroCopyLight = Color(0xFFC9C4DF)
val PromiseCheckBg = Color(0xFF2A1B4D)
val PromiseCheckTint = Color(0xFFC49BFF)

@Immutable
data class CheckFirstExtendedColors(
    // Semantic dark-theme surfaces & tokens
    val canvas: Color = ObsidianVoid,
    val barSurface: Color = ObsidianBar,
    val surface: Color = ObsidianSurface,
    val surfaceRaised: Color = ObsidianSurfaceRaised,
    val surfaceGlow: Color = ObsidianSurfaceGlow,
    val accent: Color = CosmicViolet,
    val accentGlow: Color = CosmicVioletGlow,
    val accentLight: Color = CosmicVioletLight,
    val accentTint: Color = CosmicVioletTint,
    val accentBorder: Color = CosmicVioletBorder,
    val horizonCore: Color = CosmicHorizonCore,
    val pillSecondary: Color = SecondaryPillBg,
    val pillSecondaryBorder: Color = SecondaryPillBorder,

    // Legacy-named aliases mapped cleanly to dark obsidian-violet hierarchy for zero breakage
    val navy: Color = TextPrimary,
    val navySurface: Color = ObsidianSurfaceRaised,
    val teal: Color = CosmicVioletGlow,
    val mint: Color = CosmicVioletTint,
    val mintSoft: Color = ObsidianSurfaceGlow,
    val mintBorder: Color = CosmicVioletBorder,
    val ink: Color = TextBody,
    val muted: Color = TextMuted,
    val subtle: Color = TextSubtle,
    val line: Color = HairlineBorder,
    val track: Color = HairlineTrack,

    // Risk & Emergency semantic tokens
    val danger: Color = DangerRed,
    val dangerBg: Color = DangerBg,
    val dangerBorder: Color = DangerBorder,
    val dangerCardBg: Color = DangerCardBg,
    val caution: Color = CautionAmber,
    val cautionIcon: Color = CautionIconAmber,
    val cautionChart: Color = CautionChartAmber,
    val cautionBg: Color = CautionBg,
    val cautionBorder: Color = CautionBorder,
    val cautionCardBg: Color = CautionCardBg,
    val clear: Color = ClearEmerald,
    val clearBg: Color = ClearBg,
    val clearBorder: Color = ClearBorder,
    val clearCardBg: Color = ClearCardBg,
    val emergencyBg: Color = EmergencyBannerBg,
    val emergencyBorder: Color = EmergencyBannerBorder,
    val emergencySymbolBg: Color = EmergencySymbolBg,
    val emergencySymbolTint: Color = EmergencySymbolTint,
)

val LocalCheckFirstColors = staticCompositionLocalOf { CheckFirstExtendedColors() }
