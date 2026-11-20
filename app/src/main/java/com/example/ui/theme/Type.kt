package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
    Font(R.font.inter, FontWeight.ExtraBold),
)

fun buildCheckFirstTypography(largeText: Boolean): Typography {
    val s = if (largeText) 1.14f else 1.0f
    return Typography(
        displayLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (44f * s).sp,
            lineHeight = (50f * s).sp,
            letterSpacing = (-1.4).sp,
        ),
        displaySmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (30f * s).sp,
            lineHeight = (36f * s).sp,
            letterSpacing = (-0.9).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24f * s).sp,
            lineHeight = (30f * s).sp,
            letterSpacing = (-0.6).sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (20f * s).sp,
            lineHeight = (26f * s).sp,
            letterSpacing = (-0.4).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (18f * s).sp,
            lineHeight = (24f * s).sp,
            letterSpacing = (-0.3).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (16f * s).sp,
            lineHeight = (22f * s).sp,
            letterSpacing = (-0.2).sp,
        ),
        titleSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14f * s).sp,
            lineHeight = (20f * s).sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * s).sp,
            lineHeight = (22f * s).sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5f * s).sp,
            lineHeight = (20f * s).sp,
        ),
        bodySmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12f * s).sp,
            lineHeight = (18f * s).sp,
        ),
        labelLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (13f * s).sp,
            lineHeight = (18f * s).sp,
        ),
        labelMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (11f * s).sp,
            lineHeight = (15f * s).sp,
            letterSpacing = 0.6.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (10.5f * s).sp,
            lineHeight = (14f * s).sp,
            letterSpacing = 0.5.sp,
        ),
    )
}
