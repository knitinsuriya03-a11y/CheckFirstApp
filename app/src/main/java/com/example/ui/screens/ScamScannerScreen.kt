package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.draw.clip
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
import com.example.domain.LocalSafetyEngines
import com.example.domain.MessageScanResult
import com.example.domain.RiskLevel
import com.example.ui.components.AiInsightSection
import com.example.ui.components.CheckFirstCard
import com.example.ui.components.EmptyResultCard
import com.example.ui.components.InputShape
import com.example.ui.components.PageTitle
import com.example.ui.components.PillShape
import com.example.ui.components.PrivacyChip
import com.example.ui.components.RiskBadge
import com.example.ui.components.SmallTag
import com.example.ui.components.StatusPanel
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
    var aiInsightState by remember { mutableStateOf<AiInsightUiState>(AiInsightUiState.Idle) }

    // Keep default message language aligned when app language switches (unless user explicitly picked Mixed)
    LaunchedEffect(s.isTamil) {
        if (languageCode != "MIXED") {
            languageCode = if (s.isTamil) "TA" else "EN"
        }
    }

    // Re-derive localized scan result reactively whenever message or UI language changes
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
    val selectedLanguageDisplay = languageOptions.firstOrNull { it.first == languageCode }?.second ?: s.scanLangEnglish

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
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageTitle(
            eyebrow = s.scanEyebrow,
            title = s.navScanner,
            subtitle = s.scanSubtitle,
            trailing = { PrivacyChip(s.scanPrivacyChip) },
        )

        // Input Card
        CheckFirstCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.scanCardTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                    Text(s.scanCardSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                SmallTag(s.scanMaxTag)
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = message,
                onValueChange = {
                    if (it.length <= 5000) {
                        message = it
                        hasScanned = false
                        aiInsightState = AiInsightUiState.Idle
                    }
                },
                label = { Text(s.scanInputLabel) },
                placeholder = { Text(s.scanInputPlaceholder) },
                minLines = 5,
                maxLines = 9,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${message.length} ${s.scanCharCountSuffix}", style = MaterialTheme.typography.labelSmall, color = c.subtle)
                TextButton(
                    onClick = {
                        message = if (s.isTamil) {
                            CheckFirstDemoData.SAMPLE_SCAM_MESSAGE_TAMIL
                        } else {
                            CheckFirstDemoData.SAMPLE_SCAM_MESSAGE
                        }
                        hasScanned = false
                        aiInsightState = AiInsightUiState.Idle
                    },
                    modifier = Modifier.testTag("scanner_load_example_button"),
                ) {
                    Text(s.scanLoadExample, color = c.accentLight, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(s.scanLanguageLabel, style = MaterialTheme.typography.labelLarge, color = c.navy)
            Spacer(Modifier.height(6.dp))
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
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { runLocalScan() },
                    modifier = Modifier.weight(1f).testTag("scanner_analyze_button"),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(s.scanAnalyzeBtn, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = {
                        message = ""
                        hasScanned = false
                        aiInsightState = AiInsightUiState.Idle
                    },
                    modifier = Modifier.testTag("scanner_clear_button"),
                    shape = PillShape,
                    border = BorderStroke(1.dp, c.pillSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = c.pillSecondary,
                        contentColor = c.navy,
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp),
                ) {
                    Text(s.scanClearBtn)
                }
            }
        }

        // Result Card or Empty State
        val currentResult = result
        if (currentResult != null) {
            CheckFirstCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.scanResultTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                        Text("${s.scanResultSubPrefix} · $selectedLanguageDisplay", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    SmallTag(s.scanLocalCheckTag)
                }
                Spacer(Modifier.height(14.dp))
                StatusPanel(
                    risk = currentResult.risk,
                    title = currentResult.verdict,
                    reasons = if (currentResult.matches.isNotEmpty()) {
                        currentResult.matches.take(3).map { "${it.label}: ${it.explanation}" }
                    } else {
                        listOf(s.scanNoPatternReason)
                    },
                )
                Spacer(Modifier.height(16.dp))
                AiInsightSection(
                    state = aiInsightState,
                    buttonLabel = s.aiInsightButton,
                    onRequestInsight = { requestGeminiInsight(currentResult) },
                    testTag = "scanner_gemini_insight_button",
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "${s.scanIndicatorsTitle} (${currentResult.matches.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.navy,
                )
                Spacer(Modifier.height(8.dp))
                if (currentResult.matches.isEmpty()) {
                    Text(s.scanNoIndicators, style = MaterialTheme.typography.bodySmall, color = c.muted)
                } else {
                    currentResult.matches.forEach { item ->
                        HorizontalDivider(color = c.track)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            RiskBadge(
                                risk = item.severity,
                                textOverride = if (item.severity == RiskLevel.DANGER) s.badgeHighConcern else s.badgeReview,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.label, style = MaterialTheme.typography.titleSmall, color = c.navy)
                                Spacer(Modifier.height(2.dp))
                                Text(item.explanation, style = MaterialTheme.typography.bodySmall, color = c.muted)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = c.surfaceRaised,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, c.line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(s.scanOriginalMsgTitle, style = MaterialTheme.typography.titleSmall, color = c.navy)
                        Spacer(Modifier.height(6.dp))
                        val terms = currentResult.matches.flatMap { it.highlightTerms }
                        Text(
                            text = buildHighlightedText(message, terms),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.ink,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = {
                        message = ""
                        hasScanned = false
                        aiInsightState = AiInsightUiState.Idle
                    },
                    modifier = Modifier.fillMaxWidth().testTag("scanner_scan_another_button"),
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
        } else {
            EmptyResultCard(
                icon = Icons.Filled.DocumentScanner,
                title = s.scanEmptyTitle,
                subtitle = s.scanEmptySub,
            ) {
                Surface(
                    color = c.surfaceRaised,
                    shape = PillShape,
                    border = BorderStroke(1.dp, c.line),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(16.dp))
                        Text(s.scanEmptyBadge, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
            }
        }

        // How it works + Indicator pills
        CheckFirstCard {
            Text(s.scanHowTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.scanHowSteps.forEachIndexed { idx, stepText ->
                    StepInlineRow("0${idx + 1}", stepText)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = s.scanHowFooter,
                style = MaterialTheme.typography.bodySmall,
                color = c.subtle,
            )
        }

        CheckFirstCard {
            Text(s.scanLooksForTitle, style = MaterialTheme.typography.labelMedium, color = c.accentLight)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.scanLooksForPills.forEach { pill ->
                    Surface(
                        color = c.surfaceRaised,
                        shape = PillShape,
                        border = BorderStroke(1.dp, c.line),
                    ) {
                        Text(pill, style = MaterialTheme.typography.bodySmall, color = c.ink, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StepInlineRow(number: String, text: String) {
    val c = LocalCheckFirstColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(26.dp).clip(CircleShape).background(c.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, style = MaterialTheme.typography.labelSmall, color = c.accentLight)
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.ink)
    }
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
