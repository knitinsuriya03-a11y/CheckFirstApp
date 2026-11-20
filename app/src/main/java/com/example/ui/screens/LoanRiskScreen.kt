package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.LoanRiskAnswer
import com.example.domain.LocalSafetyEngines
import com.example.domain.RiskLevel
import com.example.ui.components.CheckFirstCard
import com.example.ui.components.EmptyResultCard
import com.example.ui.components.PageTitle
import com.example.ui.components.PillShape
import com.example.ui.components.PinReminderCard
import com.example.ui.components.RiskBadge
import com.example.ui.components.SmallTag
import com.example.ui.components.StatusPanel
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalCheckFirstColors

private val LoanRiskAnswersSaver = Saver<SnapshotStateMap<Int, LoanRiskAnswer>, List<String>>(
    save = { map ->
        map.entries.map { (k, v) -> "$k:${v.name}" }
    },
    restore = { saved ->
        mutableStateMapOf<Int, LoanRiskAnswer>().apply {
            saved.forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val idx = parts[0].toIntOrNull()
                    val ans = runCatching { LoanRiskAnswer.valueOf(parts[1]) }.getOrNull()
                    if (idx != null && ans != null) put(idx, ans)
                }
            }
        }
    },
)

@Composable
fun LoanRiskScreen(
    onRecordActivity: (kind: String, label: String, risk: RiskLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    val questions = LocalSafetyEngines.loanRiskQuestions(s.isTamil)
    val shortLabels = LocalSafetyEngines.loanRiskShortLabels(s.isTamil)

    val answers = rememberSaveable(saver = LoanRiskAnswersSaver) { mutableStateMapOf() }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val answeredCount = answers.size
    val flagged = questions.indices.mapNotNull { idx ->
        val ans = answers[idx]
        if (ans == LoanRiskAnswer.YES || ans == LoanRiskAnswer.UNSURE) idx to ans else null
    }
    val computedRisk = when {
        flagged.size >= 3 -> RiskLevel.DANGER
        flagged.isNotEmpty() -> RiskLevel.CAUTION
        else -> RiskLevel.CLEAR
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageTitle(
            eyebrow = s.riskEyebrow,
            title = s.navLoanRisk,
            subtitle = s.riskSubtitle,
        )

        // Checklist Card
        CheckFirstCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.riskCardTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                    Text(s.riskCardSub, style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                SmallTag(s.riskTag)
            }
            Spacer(Modifier.height(12.dp))
            questions.forEachIndexed { idx, question ->
                HorizontalDivider(color = c.track)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(c.accentTint),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "0${idx + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = c.accentLight,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(question, style = MaterialTheme.typography.titleSmall, color = c.navy)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LoanRiskAnswer.entries.forEach { option ->
                                val selected = answers[idx] == option
                                val (bg, border, fg) = when {
                                    !selected -> Triple(c.surfaceRaised, c.line, c.muted)
                                    option == LoanRiskAnswer.YES -> Triple(c.dangerBg, c.dangerBorder, c.danger)
                                    option == LoanRiskAnswer.NO -> Triple(c.clearBg, c.clearBorder, c.clear)
                                    else -> Triple(c.cautionBg, c.cautionBorder, c.caution)
                                }
                                Surface(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .clickable {
                                            answers[idx] = option
                                            submitted = false
                                        }
                                        .testTag("loan_risk_q${idx}_${option.name.lowercase()}"),
                                    color = bg,
                                    shape = PillShape,
                                    border = BorderStroke(1.dp, border),
                                ) {
                                    Text(
                                        text = s.loanRiskAnswerLabel(option),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        ),
                                        color = fg,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    submitted = true
                    onRecordActivity("Loan risk check", "Loan offer assessment", computedRisk)
                },
                enabled = answeredCount == questions.size,
                modifier = Modifier.fillMaxWidth().testTag("loan_risk_submit_button"),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.accent,
                    contentColor = Color.White,
                    disabledContainerColor = c.surfaceRaised,
                    disabledContentColor = c.subtle,
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text("${s.riskSubmitPrefix} ($answeredCount/6 ${s.riskSubmitSuffix})", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = s.riskLenderNote,
                style = MaterialTheme.typography.bodySmall,
                color = c.subtle,
            )
        }

        // Assessment Result OR Progress Empty State
        if (submitted) {
            StatusPanel(
                risk = computedRisk,
                title = when (computedRisk) {
                    RiskLevel.DANGER -> s.riskVerdictDanger
                    RiskLevel.CAUTION -> s.riskVerdictCaution
                    RiskLevel.CLEAR -> s.riskVerdictClear
                },
                reasons = if (flagged.isNotEmpty()) {
                    flagged.map { (idx, ans) ->
                        val suffix = if (ans == LoanRiskAnswer.YES) s.riskReasonYes else s.riskReasonUnsure
                        "${questions[idx]} — $suffix"
                    }
                } else {
                    listOf(s.riskReasonNone)
                },
                action = "${flagged.size} ${s.riskActionSuffix}",
            )

            CheckFirstCard {
                Text(s.riskBreakdownTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                Spacer(Modifier.height(10.dp))
                shortLabels.forEachIndexed { idx, label ->
                    HorizontalDivider(color = c.track)
                    val ans = answers[idx]
                    val badgeRisk = when (ans) {
                        LoanRiskAnswer.YES -> RiskLevel.DANGER
                        LoanRiskAnswer.UNSURE -> RiskLevel.CAUTION
                        else -> RiskLevel.CLEAR
                    }
                    val badgeText = when (ans) {
                        LoanRiskAnswer.YES -> s.riskBadgeFlagged
                        LoanRiskAnswer.UNSURE -> s.riskBadgeCheck
                        else -> s.riskBadgeClear
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${idx + 1}. $label", style = MaterialTheme.typography.bodySmall, color = c.ink, modifier = Modifier.weight(1f))
                        RiskBadge(risk = badgeRisk, textOverride = badgeText)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = s.riskChecklistDisclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.subtle,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        answers.clear()
                        submitted = false
                    },
                    modifier = Modifier.fillMaxWidth().testTag("loan_risk_reset_button"),
                    shape = PillShape,
                    border = BorderStroke(1.dp, c.pillSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = c.pillSecondary,
                        contentColor = c.navy,
                    ),
                ) {
                    Text(s.riskResetBtn)
                }
            }
        } else {
            EmptyResultCard(
                icon = Icons.Filled.GppBad,
                title = s.riskEmptyTitle,
                subtitle = s.riskEmptySub,
            ) {
                LinearProgressIndicator(
                    progress = { answeredCount / 6f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(PillShape),
                    color = c.accentGlow,
                    trackColor = c.track,
                )
                Spacer(Modifier.height(8.dp))
                Text("$answeredCount ${s.riskProgressSuffix}", style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }

        CheckFirstCard {
            Text(s.riskPauseTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(6.dp))
            Text(
                text = s.riskPauseBody,
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
            )
        }

        PinReminderCard(
            title = s.riskImportantTitle,
            body = s.riskImportantBody,
        )

        Spacer(Modifier.height(16.dp))
    }
}
