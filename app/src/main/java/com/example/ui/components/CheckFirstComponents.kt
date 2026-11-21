package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.domain.AiInsightUiState
import com.example.domain.RiskLevel
import com.example.firebase.onGoogleSignInClicked
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalCheckFirstColors

val PillShape = RoundedCornerShape(999.dp)
val CardShape = RoundedCornerShape(18.dp)
val InputShape = RoundedCornerShape(14.dp)

@Composable
fun checkFirstTextFieldColors(): TextFieldColors {
    val c = LocalCheckFirstColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = c.surfaceRaised,
        unfocusedContainerColor = c.surfaceRaised,
        disabledContainerColor = c.surface,
        focusedBorderColor = c.accent,
        unfocusedBorderColor = c.line,
        focusedTextColor = c.navy,
        unfocusedTextColor = c.navy,
        focusedLabelColor = c.accentLight,
        unfocusedLabelColor = c.muted,
        focusedPlaceholderColor = c.subtle,
        unfocusedPlaceholderColor = c.subtle,
        focusedSupportingTextColor = c.muted,
        unfocusedSupportingTextColor = c.subtle,
        cursorColor = c.accentLight,
    )
}

@Composable
fun checkFirstFilterChipColors(): SelectableChipColors {
    val c = LocalCheckFirstColors.current
    return FilterChipDefaults.filterChipColors(
        containerColor = c.surfaceRaised,
        labelColor = c.muted,
        selectedContainerColor = c.accentTint,
        selectedLabelColor = c.accentLight,
    )
}

@Composable
fun RiskBadge(
    risk: RiskLevel,
    modifier: Modifier = Modifier,
    textOverride: String? = null,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    val (bg, border, fg, defaultLabel) = when (risk) {
        RiskLevel.DANGER -> Quad(c.dangerBg, c.dangerBorder, c.danger, s.badgeDanger)
        RiskLevel.CAUTION -> Quad(c.cautionBg, c.cautionBorder, c.caution, s.badgeCaution)
        RiskLevel.CLEAR -> Quad(c.clearBg, c.clearBorder, c.clear, s.badgeClear)
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = PillShape,
        border = BorderStroke(1.dp, border),
    ) {
        Text(
            text = textOverride ?: defaultLabel,
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun CheckFirstCard(
    modifier: Modifier = Modifier,
    containerColor: Color = LocalCheckFirstColors.current.surface,
    borderColor: Color = LocalCheckFirstColors.current.line,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = CardShape,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/**
 * Signature obsidian hero surface inspired by the reference's luminous violet horizon arc.
 */
@Composable
fun CosmicHorizonCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(22.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalCheckFirstColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = c.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    c.line,
                    c.accentBorder,
                    c.accentGlow.copy(alpha = 0.85f),
                    c.accentBorder,
                    c.line,
                ),
            ),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                c.accent.copy(alpha = 0.34f),
                                c.accentTint.copy(alpha = 0.32f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.5f, -size.height * 0.08f),
                            radius = size.width * 0.85f,
                        ),
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                c.accentGlow.copy(alpha = 0.55f),
                                c.horizonCore.copy(alpha = 0.90f),
                                c.accentGlow.copy(alpha = 0.55f),
                                Color.Transparent,
                            ),
                        ),
                        start = Offset(size.width * 0.12f, 1.dp.toPx()),
                        end = Offset(size.width * 0.88f, 1.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                    )
                },
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun PageTitle(
    eyebrow: String?,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LocalCheckFirstColors.current
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        if (!eyebrow.isNullOrBlank()) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelMedium,
                color = c.accentLight,
            )
            Spacer(Modifier.height(5.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = c.navy,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = c.muted,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.height(10.dp))
            trailing()
        }
    }
}

@Composable
fun SectionHeading(
    title: String,
    note: String? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    Column(modifier = modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, color = c.navy)
        if (!note.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(text = note, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}

@Composable
fun PrivacyChip(text: String, modifier: Modifier = Modifier) {
    val c = LocalCheckFirstColors.current
    Surface(
        modifier = modifier,
        color = c.accentTint,
        shape = PillShape,
        border = BorderStroke(1.dp, c.accentBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = c.accentLight,
                modifier = Modifier.size(14.dp),
            )
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = c.accentLight)
        }
    }
}

@Composable
fun SmallTag(text: String, modifier: Modifier = Modifier) {
    val c = LocalCheckFirstColors.current
    Surface(
        modifier = modifier,
        color = c.surfaceRaised,
        shape = PillShape,
        border = BorderStroke(1.dp, c.line),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = c.muted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun StatusPanel(
    risk: RiskLevel,
    title: String,
    reasons: List<String>,
    action: String? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    val (bg, border, iconBg, iconTint, icon) = when (risk) {
        RiskLevel.DANGER -> Quintuple(c.dangerCardBg, c.dangerBorder, c.dangerBg, c.danger, Icons.Filled.GppBad)
        RiskLevel.CAUTION -> Quintuple(c.cautionCardBg, c.cautionBorder, c.cautionBg, c.cautionIcon, Icons.Filled.WarningAmber)
        RiskLevel.CLEAR -> Quintuple(c.clearCardBg, c.clearBorder, c.clearBg, c.clear, Icons.Filled.VerifiedUser)
    }
    CheckFirstCard(
        modifier = modifier,
        containerColor = bg,
        borderColor = border,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = s.riskBadgeLabel(risk), tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                RiskBadge(risk = risk)
                Spacer(Modifier.height(6.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = c.navy)
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            reasons.forEach { reason ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.padding(top = 2.dp).size(16.dp),
                    )
                    Text(text = reason, style = MaterialTheme.typography.bodySmall, color = c.ink)
                }
            }
        }
        if (!action.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = c.surfaceRaised,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, border),
            ) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    color = c.navy,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = s.statusDisclaimer,
            style = MaterialTheme.typography.bodySmall,
            color = c.subtle,
        )
    }
}

/**
 * Shared 4-outcome verdict card driven directly by [com.example.domain.SafetyAssessment].
 * Shows verdict, top 2–3 findings, and one clear recommended action without fake probabilities.
 */
@Composable
fun SafetyVerdictPanel(
    assessment: com.example.domain.SafetyAssessment,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    val (bg, border, iconBg, iconTint, icon) = when (assessment.classification) {
        com.example.domain.SafetyClassification.HIGH_RISK ->
            Quintuple(c.dangerCardBg, c.dangerBorder, c.dangerBg, c.danger, Icons.Filled.GppBad)
        com.example.domain.SafetyClassification.CAUTION ->
            Quintuple(c.cautionCardBg, c.cautionBorder, c.cautionBg, c.cautionIcon, Icons.Filled.WarningAmber)
        com.example.domain.SafetyClassification.LIKELY_LEGITIMATE ->
            Quintuple(c.clearCardBg, c.clearBorder, c.clearBg, c.clear, Icons.Filled.VerifiedUser)
        com.example.domain.SafetyClassification.UNABLE_TO_ASSESS ->
            Quintuple(c.surfaceRaised, c.accentBorder, c.accentTint, c.accentLight, Icons.Filled.ErrorOutline)
    }
    val badgeText = when (assessment.classification) {
        com.example.domain.SafetyClassification.HIGH_RISK -> s.badgeDanger
        com.example.domain.SafetyClassification.CAUTION -> s.badgeCaution
        com.example.domain.SafetyClassification.LIKELY_LEGITIMATE -> s.badgeClear
        com.example.domain.SafetyClassification.UNABLE_TO_ASSESS -> s.verdictUnableToAssess.uppercase()
    }

    CheckFirstCard(
        modifier = modifier.testTag("safety_verdict_panel"),
        containerColor = bg,
        borderColor = border,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = assessment.verdictLabel, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = iconBg,
                    shape = PillShape,
                    border = BorderStroke(1.dp, border),
                ) {
                    Text(
                        text = badgeText,
                        color = iconTint,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = assessment.verdictLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.navy,
                )
            }
        }

        val topFindings = assessment.primaryFindings.take(3)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (topFindings.isNotEmpty()) {
                topFindings.forEach { finding ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.padding(top = 2.dp).size(16.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = finding.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = c.navy,
                            )
                            Text(
                                text = finding.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = c.ink,
                            )
                        }
                    }
                }
            } else if (assessment.missingInformation.isNotEmpty()) {
                assessment.missingInformation.take(2).forEach { info ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.padding(top = 2.dp).size(16.dp),
                        )
                        Text(text = info, style = MaterialTheme.typography.bodySmall, color = c.ink)
                    }
                }
            } else {
                Text(text = s.scanNoPatternReason, style = MaterialTheme.typography.bodySmall, color = c.ink)
            }
        }

        Spacer(Modifier.height(14.dp))
        Surface(
            color = c.surfaceRaised,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = s.scanRecommendedActionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = iconTint,
                )
                Text(
                    text = assessment.recommendedNextAction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.navy,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = s.statusDisclaimer,
            style = MaterialTheme.typography.bodySmall,
            color = c.subtle,
        )
    }
}

private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

@Composable
fun InlineErrorBanner(message: String, modifier: Modifier = Modifier) {
    val c = LocalCheckFirstColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = c.dangerBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, c.dangerBorder),
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = c.danger, modifier = Modifier.size(18.dp))
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = c.danger)
        }
    }
}

@Composable
fun PinReminderCard(title: String, body: String, modifier: Modifier = Modifier) {
    val c = LocalCheckFirstColors.current
    CheckFirstCard(
        modifier = modifier,
        containerColor = c.cautionCardBg,
        borderColor = c.cautionBorder,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.cautionBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = c.cautionIcon, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = c.caution)
                Spacer(Modifier.height(4.dp))
                Text(text = body, style = MaterialTheme.typography.bodySmall, color = c.ink)
            }
        }
    }
}

@Composable
fun EmptyResultCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val c = LocalCheckFirstColors.current
    CheckFirstCard(modifier = modifier, contentPadding = PaddingValues(26.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.accentTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = c.navy, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = c.muted, textAlign = TextAlign.Center)
            if (footer != null) {
                Spacer(Modifier.height(16.dp))
                footer()
            }
        }
    }
}

@Composable
fun AiInsightSection(
    state: AiInsightUiState,
    buttonLabel: String,
    onRequestInsight: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state) {
            is AiInsightUiState.Idle -> {
                OutlinedButton(
                    onClick = onRequestInsight,
                    modifier = Modifier.fillMaxWidth().testTag(testTag),
                    shape = PillShape,
                    border = BorderStroke(1.dp, c.accentBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = c.accentTint,
                        contentColor = c.accentLight,
                    ),
                    contentPadding = PaddingValues(vertical = 13.dp, horizontal = 16.dp),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(buttonLabel, style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    text = s.aiInsightHelper,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.subtle,
                )
            }

            is AiInsightUiState.Loading -> {
                CheckFirstCard(
                    containerColor = c.surfaceGlow,
                    borderColor = c.accentBorder,
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = c.accentGlow, strokeWidth = 2.5.dp)
                        Text(
                            text = s.aiInsightLoading,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.navy,
                        )
                    }
                }
            }

            is AiInsightUiState.Error -> {
                InlineErrorBanner(message = state.message)
                OutlinedButton(
                    onClick = onRequestInsight,
                    modifier = Modifier.fillMaxWidth().testTag("${testTag}_retry"),
                    shape = PillShape,
                    border = BorderStroke(1.dp, c.pillSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = c.pillSecondary,
                        contentColor = c.navy,
                    ),
                ) {
                    Text(s.aiInsightRetry)
                }
            }

            is AiInsightUiState.Success -> {
                val insight = state.insight
                CheckFirstCard(
                    containerColor = c.surfaceGlow,
                    borderColor = c.accentBorder,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(18.dp))
                            Text(s.aiInsightHeader, style = MaterialTheme.typography.labelMedium, color = c.accentLight)
                        }
                        if (insight.modelUsed.isNotBlank()) {
                            SmallTag(
                                text = if (insight.usedFallback) {
                                    "${s.aiInsightAutoSwitchedPrefix} ${insight.modelUsed}"
                                } else {
                                    "${s.aiInsightModelPrefix} ${insight.modelUsed}"
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(text = insight.summary, style = MaterialTheme.typography.bodyMedium, color = c.navy)
                    if (insight.keyConcerns.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(s.aiInsightWatchOut, style = MaterialTheme.typography.titleSmall, color = c.navy)
                        Spacer(Modifier.height(4.dp))
                        insight.keyConcerns.forEach { concern ->
                            Text("• $concern", style = MaterialTheme.typography.bodySmall, color = c.ink)
                        }
                    }
                    if (insight.recommendedActions.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(s.aiInsightNextSteps, style = MaterialTheme.typography.titleSmall, color = c.navy)
                        Spacer(Modifier.height(4.dp))
                        insight.recommendedActions.forEach { step ->
                            Text("• $step", style = MaterialTheme.typography.bodySmall, color = c.ink)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(text = insight.cautionNote, style = MaterialTheme.typography.bodySmall, color = c.subtle)
                }
            }
        }
    }
}

@Composable
fun GoogleSignInActionButton(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    testTag: String = "google_sign_in_button",
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isLoading = true
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onAuthError = { msg ->
                    isLoading = false
                    onAuthError(msg)
                },
                scope = scope,
                onAuthCancelled = { isLoading = false },
            )
        },
        enabled = !isLoading,
        modifier = modifier.testTag(testTag),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = c.accent,
            contentColor = Color.White,
        ),
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 18.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label ?: s.loginButtonGoogle, style = MaterialTheme.typography.labelLarge)
        }
    }
}
