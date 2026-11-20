package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.CardShape
import com.example.ui.components.CheckFirstCard
import com.example.ui.components.InputShape
import com.example.ui.components.PageTitle
import com.example.ui.components.PillShape
import com.example.ui.components.checkFirstTextFieldColors
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalCheckFirstColors

@Composable
fun EmergencyScreen(
    onShowNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var dateTime by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var utr by rememberSaveable { mutableStateOf("") }
    var bank by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    val summary = buildString {
        appendLine(s.emSummaryHeader)
        appendLine("${s.emFieldDateLabel}: ${dateTime.ifBlank { s.emNotProvided }}")
        appendLine("${s.emFieldAmountLabel}: ${amount.ifBlank { s.emNotProvided }}")
        appendLine("${s.emFieldUtrLabel}: ${utr.ifBlank { s.emNotProvided }}")
        appendLine("${s.emFieldBankLabel}: ${bank.ifBlank { s.emNotProvided }}")
        appendLine("${s.emFieldDescLabel}: ${description.ifBlank { s.emNotProvided }}")
        appendLine()
        append(s.emSummaryFooter)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageTitle(
            eyebrow = s.emEyebrow,
            title = s.emTitle,
            subtitle = s.emSubtitle,
        )

        // Urgent Banner
        CheckFirstCard(containerColor = c.emergencyBg, borderColor = c.emergencyBorder) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(c.emergencySymbolBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Support, contentDescription = null, tint = c.emergencySymbolTint, modifier = Modifier.size(28.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.emBannerEyebrow, style = MaterialTheme.typography.labelMedium, color = c.emergencySymbolTint)
                    Spacer(Modifier.height(3.dp))
                    Text(s.emBannerTitle, style = MaterialTheme.typography.titleLarge, color = c.navy)
                    Text(s.emBannerSub, style = MaterialTheme.typography.bodySmall, color = c.ink)
                }
            }
        }

        // Direct Emergency Contacts (1930 + cybercrime.gov.in)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(CardShape)
                    .clickable {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
                        runCatching { context.startActivity(intent) }
                            .onFailure { onShowNotice(s.emDialFallbackNotice) }
                    }
                    .testTag("emergency_call_1930_card"),
                color = c.dangerBg,
                shape = CardShape,
                border = BorderStroke(1.dp, c.dangerBorder),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(s.emHelplineLabel, style = MaterialTheme.typography.labelSmall, color = c.danger)
                    Text("1930", style = MaterialTheme.typography.displaySmall, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(s.emCallHelplineCta, style = MaterialTheme.typography.labelLarge, color = c.danger)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = c.danger, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(CardShape)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cybercrime.gov.in"))
                        runCatching { context.startActivity(intent) }
                            .onFailure { onShowNotice(s.emPortalFallbackNotice) }
                    }
                    .testTag("emergency_portal_card"),
                color = c.surfaceGlow,
                shape = CardShape,
                border = BorderStroke(1.dp, c.accentBorder),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(s.emPortalLabel, style = MaterialTheme.typography.labelSmall, color = c.accentLight)
                    Text("cybercrime\n.gov.in", style = MaterialTheme.typography.titleLarge, color = c.navy)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(s.emOpenPortalCta, style = MaterialTheme.typography.labelLarge, color = c.accentLight)
                        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // 5 Steps Card
        CheckFirstCard {
            Text(s.emStepsTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(10.dp))
            s.emActions.forEachIndexed { idx, (title, desc) ->
                if (idx > 0) HorizontalDivider(color = c.track)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(c.accentTint),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${idx + 1}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = c.accentLight,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = c.navy)
                        Spacer(Modifier.height(3.dp))
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
            }
            Text(
                text = s.emDisclaimer,
                style = MaterialTheme.typography.bodySmall,
                color = c.subtle,
            )
        }

        // Local Incident Summary Form
        CheckFirstCard {
            Text(s.emSummaryTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(4.dp))
            Text(
                text = s.emSummarySub,
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
            )
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dateTime,
                    onValueChange = { dateTime = it },
                    label = { Text(s.emFieldDateLabel) },
                    placeholder = { Text(s.emFieldDatePlaceholder) },
                    singleLine = true,
                    colors = checkFirstTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("emergency_date_input"),
                    shape = InputShape,
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(s.emFieldAmountLabel) },
                    placeholder = { Text(s.emFieldAmountPlaceholder) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = checkFirstTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("emergency_amount_input"),
                    shape = InputShape,
                )
                OutlinedTextField(
                    value = utr,
                    onValueChange = { utr = it },
                    label = { Text(s.emFieldUtrLabel) },
                    placeholder = { Text(s.emFieldUtrPlaceholder) },
                    singleLine = true,
                    colors = checkFirstTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("emergency_utr_input"),
                    shape = InputShape,
                )
                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text(s.emFieldBankLabel) },
                    placeholder = { Text(s.emFieldBankPlaceholder) },
                    singleLine = true,
                    colors = checkFirstTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("emergency_bank_input"),
                    shape = InputShape,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(s.emFieldDescLabel) },
                    placeholder = { Text(s.emFieldDescPlaceholder) },
                    minLines = 3,
                    maxLines = 6,
                    colors = checkFirstTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("emergency_desc_input"),
                    shape = InputShape,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(summary))
                            onShowNotice(s.emCopiedNotice)
                        },
                        modifier = Modifier.weight(1f).testTag("emergency_copy_summary_button"),
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text(s.emCopyBtn, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            dateTime = ""
                            amount = ""
                            utr = ""
                            bank = ""
                            description = ""
                        },
                        modifier = Modifier.testTag("emergency_clear_form_button"),
                        shape = PillShape,
                        border = BorderStroke(1.dp, c.pillSecondaryBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = c.pillSecondary,
                            contentColor = c.navy,
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                    ) {
                        Text(s.emClearFormBtn)
                    }
                }
                Surface(
                    color = c.surfaceRaised,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, c.line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(s.emPreviewLabel, style = MaterialTheme.typography.labelMedium, color = c.accentLight)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = c.ink,
                        )
                    }
                }
                Text(
                    text = s.emNeverEnterNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.subtle,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
