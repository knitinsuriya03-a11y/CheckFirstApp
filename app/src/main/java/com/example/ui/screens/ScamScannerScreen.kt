package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.CheckFirstAiRepository
import com.example.domain.AiInsightUiState
import com.example.domain.CheckFirstDemoData
import com.example.domain.EvidenceStrength
import com.example.domain.LocalSafetyEngines
import com.example.domain.MessageScanResult
import com.example.domain.RiskLevel
import com.example.ui.components.AiInsightSection
import com.example.ui.components.CheckFirstCard
import com.example.ui.components.InputShape
import com.example.ui.components.PageTitle
import com.example.ui.components.PillShape
import com.example.ui.components.RiskBadge
import com.example.ui.components.SafetyVerdictPanel
import com.example.ui.components.SmallTag
import com.example.ui.components.checkFirstFilterChipColors
import com.example.ui.components.checkFirstTextFieldColors
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.HighlightMarkBg
import com.example.ui.theme.HighlightMarkText
import com.example.ui.theme.LocalCheckFirstColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScamScannerScreen(
    aiRepository: CheckFirstAiRepository,
    onRecordActivity: (kind: String, label: String, risk: RiskLevel) -> Unit,
    onShowNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    var message by rememberSaveable { mutableStateOf("") }
    var languageCode by rememberSaveable { mutableStateOf(if (s.isTamil) "TA" else "EN") }
    var hasScanned by rememberSaveable { mutableStateOf(false) }
    var showMoreDetails by rememberSaveable { mutableStateOf(false) }
    var aiInsightState by remember { mutableStateOf<AiInsightUiState>(AiInsightUiState.Idle) }

    LaunchedEffect(s.isTamil) {
        if (languageCode != "MIXED") {
            languageCode = if (s.isTamil) "TA" else "EN"
        }
    }

    val result: MessageScanResult? = remember(hasScanned, message, s.isTamil) {
        if (hasScanned && message.isNotBlank()) {
            LocalSafetyEngines.scanMessageLocal(message, tamil = s.isTamil)
        } else {
            null
        }
    }

    val languageOptions = listOf(
        "EN" to s.scanLangEnglish,
        "TA" to s.scanLangTamil,
        "MIXED" to s.scanLangMixed,
    )

    fun loadSample(text: String) {
        message = text
        hasScanned = false
        showMoreDetails = false
        aiInsightState = AiInsightUiState.Idle
    }

    fun runLocalScan() {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            onShowNotice(s.scanNoticeEmpty)
            return
        }
        if (message.length > 5000) {
            onShowNotice(s.scanNoticeTooLong)
            return
        }
        val scan = LocalSafetyEngines.scanMessageLocal(message, tamil = s.isTamil)
        hasScanned = true
        showMoreDetails = false
        aiInsightState = AiInsightUiState.Idle
        onRecordActivity("Message scan", "Message scan result", scan.risk)
    }

    fun requestGeminiInsight(local: MessageScanResult) {
        scope.launch {
            aiInsightState = AiInsightUiState.Loading
            val langArg = when (languageCode) {
                "TA" -> "Tamil"
                "MIXED" -> "Mixed Tamil and English"
                else -> if (s.isTamil) "Tamil" else "English"
            }
            val res = aiRepository.analyzeMessageWithGemini(
                message = message,
                language = langArg,
                localResult = local,
                tamilUi = s.isTamil,
            )
            aiInsightState = res.fold(
                onSuccess = { AiInsightUiState.Success(it) },
                onFailure = {
                    AiInsightUiState.Error(
                        it.localizedMessage ?: if (s.isTamil) {
                            "Gemini ஆலோசனையைப் பெற முடியவில்லை."
                        } else {
                            "Unable to fetch Gemini insights."
                        },
                    )
                },
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageTitle(
            eyebrow = s.scanEyebrow,
            title = s.navScanner,
            subtitle = s.scanSubtitle,
        )

        // Compact Input Card
        CheckFirstCard(contentPadding = PaddingValues(16.dp)) {
            OutlinedTextField(
                value = message,
                onValueChange = {
                    if (it.length <= 5000) {
                        message = it
                        hasScanned = false
                        showMoreDetails = false
                        aiInsightState = AiInsightUiState.Idle
                    }
                },
                label = { Text(s.scanInputLabel) },
                placeholder = { Text(s.scanInputPlaceholder) },
                minLines = 5,
                maxLines = 8,
                colors = checkFirstTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("scanner_message_input"),
                shape = InputShape,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = s.scanCredentialNote,
                style = MaterialTheme.typography.bodySmall,
                color = c.subtle,
            )
            Spacer(Modifier.height(10.dp))

            // Clearly labelled Sample fixtures for quick demonstration
            Text(
                text = s.scanSamplesLabel,
                style = MaterialTheme.typography.labelMedium,
                color = c.muted,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SampleChip(
                    label = s.scanSampleRefundPin,
                    testTag = "scanner_load_example_button",
                    onClick = {
                        loadSample(
                            if (s.isTamil) CheckFirstDemoData.SAMPLE_SCAM_MESSAGE_TAMIL
                            else CheckFirstDemoData.SAMPLE_SCAM_MESSAGE,
                        )
                    },
                )
                SampleChip(
                    label = s.scanSampleDigitalArrest,
                    testTag = "scanner_sample_digital_arrest",
                    onClick = {
                        loadSample(
                            if (s.isTamil) CheckFirstDemoData.SAMPLE_DIGITAL_ARREST_TAMIL
                            else CheckFirstDemoData.SAMPLE_DIGITAL_ARREST_EN,
                        )
                    },
                )
                SampleChip(
                    label = s.scanSampleGenuineOtp,
                    testTag = "scanner_sample_genuine_otp",
                    onClick = {
                        loadSample(
                            if (s.isTamil) CheckFirstDemoData.SAMPLE_GENUINE_OTP_TAMIL
                            else CheckFirstDemoData.SAMPLE_GENUINE_OTP_EN,
                        )
                    },
                )
                SampleChip(
                    label = s.scanSampleTanglish,
                    testTag = "scanner_sample_tanglish",
                    onClick = { loadSample(CheckFirstDemoData.SAMPLE_TANGLISH_SCAM) },
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                languageOptions.forEach { (code, label) ->
                    FilterChip(
                        selected = languageCode == code,
                        onClick = { languageCode = code },
                        label = { Text(label) },
                        shape = PillShape,
                        colors = checkFirstFilterChipColors(),
                        modifier = Modifier.testTag("scanner_lang_${code.lowercase()}"),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { runLocalScan() },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("scanner_analyze_button"),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                    contentPadding = PaddingValues(vertical = 13.dp),
                ) {
                    Text(s.scanAnalyzeBtn, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = {
                        message = ""
                        hasScanned = false
                        showMoreDetails = false
                        aiInsightState = AiInsightUiState.Idle
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("scanner_clear_button"),
                    shape = PillShape,
                    border = BorderStroke(1.dp, c.pillSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = c.pillSecondary,
                        contentColor = c.navy,
                    ),
                    contentPadding = PaddingValues(vertical = 13.dp, horizontal = 18.dp),
                ) {
                    Text(s.scanClearBtn)
                }
            }
        }

        // Concise Results Section (shown only after scan — no pre-scan empty cards or filler text)
        val currentResult = result
        if (currentResult != null) {
            val assessment = currentResult.assessment
            SafetyVerdictPanel(assessment = assessment)

            CheckFirstCard(contentPadding = PaddingValues(16.dp)) {
                // Expandable "More details" toggle for secondary evidence, rule metadata & highlighted text
                TextButton(
                    onClick = { showMoreDetails = !showMoreDetails },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("scanner_more_details_toggle"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (showMoreDetails) s.scanHideDetails else s.scanMoreDetails,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.accentLight,
                        )
                        Icon(
                            imageVector = if (showMoreDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (showMoreDetails) s.scanHideDetails else s.scanMoreDetails,
                            tint = c.accentLight,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                AnimatedVisibility(visible = showMoreDetails) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Assessment metadata chips (no fake accuracy percentages)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SmallTag("${s.scanConfidenceLabel}: ${assessment.confidence.displayLabel(s.isTamil)}")
                            SmallTag("${s.scanOnlineCheckLabel}: ${s.scanOnlineCheckNo}")
                            SmallTag("${s.scanRuleSetLabel}: ${assessment.ruleSetVersion}")
                        }

                        if (assessment.findings.isNotEmpty()) {
                            Text(
                                text = "${s.scanIndicatorsTitle} (${assessment.findings.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = c.navy,
                            )
                            assessment.findings.forEach { ev ->
                                HorizontalDivider(color = c.track)
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        SmallTag(ev.ruleId)
                                        RiskBadge(
                                            risk = ev.strength.toRiskLevel(),
                                            textOverride = ev.strength.displayLabel(s.isTamil),
                                        )
                                    }
                                    Text(ev.title, style = MaterialTheme.typography.titleSmall, color = c.navy)
                                    Text(
                                        "${s.scanReasonPrefix} ${ev.triggerReason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = c.ink,
                                    )
                                    Text(
                                        "${s.scanLimitationPrefix} ${ev.exceptionOrLimitation}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = c.muted,
                                    )
                                }
                            }
                        }

                        if (assessment.missingInformation.isNotEmpty()) {
                            HorizontalDivider(color = c.track)
                            Text(s.scanMissingInfoTitle, style = MaterialTheme.typography.titleSmall, color = c.navy)
                            assessment.missingInformation.forEach { missing ->
                                Text("• $missing", style = MaterialTheme.typography.bodySmall, color = c.muted)
                            }
                        }

                        // Original message preserved verbatim with matched spans highlighted
                        Surface(
                            color = c.surfaceRaised,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, c.line),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(s.scanOriginalMsgTitle, style = MaterialTheme.typography.titleSmall, color = c.navy)
                                Spacer(Modifier.height(6.dp))
                                val terms = assessment.findings.flatMap { it.matchedSpans }
                                Text(
                                    text = buildHighlightedText(message, terms),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.ink,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                AiInsightSection(
                    state = aiInsightState,
                    buttonLabel = s.aiInsightButton,
                    onRequestInsight = { requestGeminiInsight(currentResult) },
                    testTag = "scanner_gemini_insight_button",
                )

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        message = ""
                        hasScanned = false
                        showMoreDetails = false
                        aiInsightState = AiInsightUiState.Idle
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("scanner_scan_another_button"),
                    shape = PillShape,
                    border = BorderStroke(1.dp, c.pillSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = c.pillSecondary,
                        contentColor = c.navy,
                    ),
                ) {
                    Text(s.scanAnotherBtn)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SampleChip(
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val c = LocalCheckFirstColors.current
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        shape = PillShape,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = c.surfaceRaised,
            labelColor = c.accentLight,
        ),
        border = BorderStroke(1.dp, c.accentBorder),
        modifier = Modifier.heightIn(min = 48.dp).testTag(testTag),
    )
}

private fun buildHighlightedText(text: String, terms: List<String>): AnnotatedString {
    val uniqueTerms = terms.distinct().filter { it.isNotBlank() }.sortedByDescending { it.length }
    if (uniqueTerms.isEmpty()) return AnnotatedString(text)
    val pattern = uniqueTerms.joinToString("|") { Regex.escape(it) }
    val regex = Regex(pattern, RegexOption.IGNORE_CASE)
    return buildAnnotatedString {
        var lastIndex = 0
        for (match in regex.findAll(text)) {
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }
            withStyle(SpanStyle(background = HighlightMarkBg, color = HighlightMarkText, fontWeight = FontWeight.SemiBold)) {
                append(match.value)
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}
