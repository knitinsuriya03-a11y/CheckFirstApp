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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SmsFailed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.domain.ActivityItem
import com.example.domain.CheckFirstDemoData
import com.example.domain.Prefs
import com.example.domain.RiskLevel
import com.example.ui.components.CardShape
import com.example.ui.components.CheckFirstCard
import com.example.ui.components.CosmicHorizonCard
import com.example.ui.components.DonutSlice
import com.example.ui.components.HorizontalMetricBar
import com.example.ui.components.PillShape
import com.example.ui.components.RiskBadge
import com.example.ui.components.RiskDistributionDonutChart
import com.example.ui.components.SectionHeading
import com.example.ui.components.SevenDayBarChart
import com.example.ui.components.SmallTag
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.HeroCopyLight
import com.example.ui.theme.HeroEyebrowLight
import com.example.ui.theme.LocalCheckFirstColors
import com.example.ui.theme.PromiseCheckBg
import com.example.ui.theme.PromiseCheckTint

@Composable
fun DashboardScreen(
    prefs: Prefs,
    history: List<ActivityItem>,
    isSignedIn: Boolean,
    onNavigate: (String) -> Unit,
    onDisableAndClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    val hasRealHistory = prefs.saveHistory && history.isNotEmpty()
    val totalCount = if (hasRealHistory) history.size else 120
    val dangerCount = if (hasRealHistory) history.count { it.risk == RiskLevel.DANGER } else 30
    val cautionCount = if (hasRealHistory) history.count { it.risk == RiskLevel.CAUTION } else 54
    val clearCount = if (hasRealHistory) history.count { it.risk == RiskLevel.CLEAR } else 36

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (prefs.simpleMode) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("dashboard_simple_mode_banner"),
                color = c.accentTint,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, c.accentBorder),
            ) {
                Text(
                    text = s.dashSimpleModeBanner,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.accentLight,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }

        // Signature Cosmic Horizon Hero Banner
        CosmicHorizonCard {
            Text(
                text = s.dashEyebrow,
                style = MaterialTheme.typography.labelMedium,
                color = HeroEyebrowLight,
            )
            Spacer(Modifier.height(8.dp))
            if (s.isTamil) {
                Text(
                    text = s.dashWelcomeFullTamil,
                    style = MaterialTheme.typography.displaySmall,
                    color = c.navy,
                )
            } else {
                Text(
                    text = buildAnnotatedString {
                        append(s.dashWelcomePrefix)
                        withStyle(SpanStyle(color = c.accentGlow)) {
                            append("CheckFirst")
                        }
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = c.navy,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = s.dashHeroSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = HeroCopyLight,
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val btnVertPad = if (prefs.simpleMode) 17.dp else 14.dp
                Button(
                    onClick = { onNavigate("scanner") },
                    modifier = Modifier.fillMaxWidth().testTag("hero_scan_message_button"),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                    contentPadding = PaddingValues(vertical = btnVertPad, horizontal = 18.dp),
                ) {
                    Text(s.dashScanMessageCta, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                OutlinedButton(
                    onClick = { onNavigate("loan-risk") },
                    modifier = Modifier.fillMaxWidth().testTag("hero_loan_risk_button"),
                    shape = PillShape,
                    border = BorderStroke(1.dp, c.pillSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = c.pillSecondary,
                        contentColor = c.navy,
                    ),
                    contentPadding = PaddingValues(vertical = btnVertPad, horizontal = 18.dp),
                ) {
                    Text(s.dashCheckLoanCta, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(14.dp))
            // Hero Promise Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = c.surfaceRaised.copy(alpha = 0.85f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, c.accentBorder),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(PromiseCheckBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = PromiseCheckTint, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = s.dashPromiseTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.navy,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = s.dashPromiseSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.muted,
                        )
                    }
                }
            }
        }

        // Quick-access AI Companions Strip
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AiShortcutPill(
                title = s.dashAiChatTitle,
                subtitle = s.dashAiChatSubtitle,
                icon = Icons.AutoMirrored.Filled.Chat,
                onClick = { onNavigate("ai-chat") },
                modifier = Modifier.weight(1f).testTag("dashboard_ai_chat_card"),
            )
            AiShortcutPill(
                title = s.dashVoiceTitle,
                subtitle = s.dashVoiceSubtitle,
                icon = Icons.Filled.Mic,
                onClick = { onNavigate("live-voice") },
                modifier = Modifier.weight(1f).testTag("dashboard_live_voice_card"),
            )
        }

        SectionHeading(
            title = s.dashToolsTitle,
            note = s.dashToolsNote,
        )

        // 4 Quick Tool Cards (01-04)
        QuickToolCard(
            number = s.dashTool1Num,
            title = s.navScanner,
            desc = s.dashTool1Desc,
            cta = s.dashTool1Cta,
            icon = Icons.Filled.SmsFailed,
            onClick = { onNavigate("scanner") },
            testTag = "quick_card_scanner",
        )
        QuickToolCard(
            number = s.dashTool2Num,
            title = s.navQrAnalyzer,
            desc = s.dashTool2Desc,
            cta = s.dashTool2Cta,
            icon = Icons.Filled.QrCode2,
            onClick = { onNavigate("qr-analyzer") },
            testTag = "quick_card_qr",
        )
        QuickToolCard(
            number = s.dashTool3Num,
            title = s.navLoanCalculator,
            desc = s.dashTool3Desc,
            cta = s.dashTool3Cta,
            icon = Icons.Filled.CurrencyRupee,
            onClick = { onNavigate("loan-calculator") },
            testTag = "quick_card_loan_calc",
        )
        QuickToolCard(
            number = s.dashTool4Num,
            title = s.navLoanRisk,
            desc = s.dashTool4Desc,
            cta = s.dashTool4Cta,
            icon = Icons.Filled.GppBad,
            onClick = { onNavigate("loan-risk") },
            testTag = "quick_card_loan_risk",
        )

        // Safety Awareness Card
        CheckFirstCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(c.accentTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.dashAwarenessTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                    Surface(
                        color = c.cautionBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, c.cautionBorder),
                    ) {
                        Text(
                            text = s.dashAwarenessAlert,
                            style = MaterialTheme.typography.labelLarge,
                            color = c.caution,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        )
                    }
                    Text(s.dashAwarenessSubhead, style = MaterialTheme.typography.titleSmall, color = c.navy)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        s.dashAwarenessBullets.forEach { bullet ->
                            Text(bullet, style = MaterialTheme.typography.bodySmall, color = c.muted)
                        }
                    }
                }
            }
        }

        // Safety Tip Card (if enabled)
        if (prefs.safetyTips) {
            CheckFirstCard(containerColor = c.surfaceGlow, borderColor = c.accentBorder) {
                Text(s.dashTipEyebrow, style = MaterialTheme.typography.labelMedium, color = c.accentLight)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = s.dashTipHeadline,
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.navy,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = s.dashTipBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.muted,
                )
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { onNavigate("guide") },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.testTag("tip_open_guide_button"),
                ) {
                    Text(s.dashTipCta, color = c.accentLight, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Detailed Analytics Section (streamlined away in Simple Mode unless real user history exists)
        if (!prefs.simpleMode || hasRealHistory) {
            SectionHeading(
                title = if (hasRealHistory) s.dashAnalyticsTitleReal else s.dashAnalyticsTitleDemo,
                note = if (hasRealHistory) {
                    if (isSignedIn) s.dashAnalyticsNoteSynced else s.dashAnalyticsNoteLocal
                } else {
                    s.dashAnalyticsNoteDemo
                },
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = if (hasRealHistory) s.dashStatTotalReal else s.dashStatTotalDemo,
                    value = totalCount.toString(),
                    note = if (hasRealHistory) s.dashStatNoteOptIn else s.dashStatNoteDemo,
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    iconBg = c.accentTint,
                    iconTint = c.accentLight,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = s.dashStatDanger,
                    value = dangerCount.toString(),
                    note = if (hasRealHistory) s.dashStatDangerNoteReal else s.dashStatSampleChecks,
                    icon = Icons.Filled.GppBad,
                    iconBg = c.dangerBg,
                    iconTint = c.danger,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = s.dashStatCaution,
                    value = cautionCount.toString(),
                    note = if (hasRealHistory) s.dashStatCautionNoteReal else s.dashStatSampleChecks,
                    icon = Icons.Filled.WarningAmber,
                    iconBg = c.cautionBg,
                    iconTint = c.cautionIcon,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = s.dashStatClear,
                    value = clearCount.toString(),
                    note = s.dashStatClearNote,
                    icon = Icons.Filled.VerifiedUser,
                    iconBg = c.clearBg,
                    iconTint = c.clear,
                    modifier = Modifier.weight(1f),
                )
            }

            if (!hasRealHistory) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = c.muted, modifier = Modifier.size(16.dp))
                    Text(
                        text = s.dashDemoDisclaimer,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.muted,
                    )
                }
            }

            // Donut Chart Card
            CheckFirstCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text(s.dashDonutTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                        Text(
                            text = if (hasRealHistory) s.dashDonutSubReal else s.dashDonutSubDemo,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.muted,
                        )
                    }
                    SmallTag("$totalCount ${s.checksUnit}")
                }
                Spacer(Modifier.height(14.dp))
                RiskDistributionDonutChart(
                    slices = listOf(
                        DonutSlice(s.dashSliceDanger, dangerCount, c.danger),
                        DonutSlice(s.dashSliceCaution, cautionCount, c.cautionChart),
                        DonutSlice(s.dashSliceClear, clearCount, c.clear),
                    ),
                    totalChecks = totalCount,
                )
                Spacer(Modifier.height(12.dp))
                Text(s.dashDonutFooter, style = MaterialTheme.typography.bodySmall, color = c.subtle)
            }

            // Seven-Day Dynamics Wave Chart Card
            CheckFirstCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text(s.dashWeeklyTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                        Text(s.dashWeeklySub, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    SmallTag(s.dashWeeklyTag)
                }
                Spacer(Modifier.height(16.dp))
                val localizedWeeklyData = CheckFirstDemoData.weeklyChecks.mapIndexed { idx, (day, count) ->
                    (s.dashWeekDays.getOrElse(idx) { day }) to count
                }
                SevenDayBarChart(data = localizedWeeklyData)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = s.dashWeeklyFooter,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.subtle,
                )
            }

            // Common Warning Indicators Card
            CheckFirstCard {
                Text(s.dashWarningsTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = s.dashWarningsSub,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.muted,
                )
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CheckFirstDemoData.commonWarnings.forEachIndexed { idx, (label, count) ->
                        HorizontalMetricBar(
                            label = s.dashWarningLabels.getOrElse(idx) { label },
                            valueText = count.toString(),
                            fraction = count / 120f,
                            barColor = c.accentGlow,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = s.dashWarningsFooter,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.subtle,
                )
            }
        }

        // Recent Activity Card
        CheckFirstCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasRealHistory) {
                            if (isSignedIn) s.dashHistoryTitleSynced else s.dashHistoryTitleLocal
                        } else {
                            s.dashHistoryTitleDemo
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = c.navy,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (hasRealHistory) {
                            if (isSignedIn) s.dashHistorySubSynced else s.dashHistorySubLocal
                        } else {
                            s.dashHistorySubDemo
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = c.muted,
                    )
                }
                TextButton(onClick = { onNavigate("settings") }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(s.dashPrivacyLink, color = c.accentLight, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(10.dp))
            val itemsToShow = (if (hasRealHistory) history else CheckFirstDemoData.starterActivity).take(5)
            itemsToShow.forEach { item ->
                HorizontalDivider(color = c.track)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.surfaceRaised),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.activityTitleLabel(item.label), style = MaterialTheme.typography.titleSmall, color = c.navy)
                        Text(
                            text = "${s.activityKindLabel(item.kind)} · ${s.activityDateLabel(item.date)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.muted,
                        )
                    }
                    RiskBadge(risk = item.risk)
                }
            }
            if (hasRealHistory) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onDisableAndClearHistory,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.testTag("dashboard_clear_history_button"),
                ) {
                    Text(s.dashTurnOffClearHistory, color = c.accentLight, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AiShortcutPill(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    Surface(
        modifier = modifier.clip(CardShape).clickable(onClick = onClick),
        color = c.surfaceGlow,
        shape = CardShape,
        border = BorderStroke(1.dp, c.accentBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(c.accentTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.navy, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun QuickToolCard(
    number: String,
    title: String,
    desc: String,
    cta: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
) {
    val c = LocalCheckFirstColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().clip(CardShape).clickable(onClick = onClick).testTag(testTag),
        color = c.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(number, style = MaterialTheme.typography.labelMedium, color = c.accentLight)
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.surfaceRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = c.accentGlow, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = c.navy)
            Spacer(Modifier.height(5.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = c.muted)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(cta, style = MaterialTheme.typography.labelLarge, color = c.accentLight)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    note: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    CheckFirstCard(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = c.muted)
                Spacer(Modifier.height(6.dp))
                Text(value, style = MaterialTheme.typography.headlineMedium, color = c.navy)
                Spacer(Modifier.height(3.dp))
                Text(note, style = MaterialTheme.typography.labelSmall, color = c.subtle)
            }
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
        }
    }
}
