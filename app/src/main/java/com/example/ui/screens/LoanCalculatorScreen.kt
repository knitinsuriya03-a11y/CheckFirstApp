package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.LocalSafetyEngines
import com.example.domain.RiskLevel
import com.example.domain.TenureUnit
import com.example.ui.components.CheckFirstCard
import com.example.ui.components.CosmicHorizonCard
import com.example.ui.components.EmptyResultCard
import com.example.ui.components.HorizontalMetricBar
import com.example.ui.components.InlineErrorBanner
import com.example.ui.components.InputShape
import com.example.ui.components.PageTitle
import com.example.ui.components.PillShape
import com.example.ui.components.SmallTag
import com.example.ui.components.checkFirstFilterChipColors
import com.example.ui.components.checkFirstTextFieldColors
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalCheckFirstColors
import java.util.Locale

@Composable
fun LoanCalculatorScreen(
    onRecordActivity: (kind: String, label: String, risk: RiskLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current

    var received by rememberSaveable { mutableStateOf("10000") }
    var fees by rememberSaveable { mutableStateOf("500") }
    var repayment by rememberSaveable { mutableStateOf("11200") }
    var tenure by rememberSaveable { mutableStateOf("30") }
    var unit by rememberSaveable { mutableStateOf(TenureUnit.DAYS) }
    var calculated by rememberSaveable { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val calcResult = remember(received, fees, repayment, tenure, unit, s.isTamil) {
        LocalSafetyEngines.calculateLoanCost(received, fees, repayment, tenure, unit, tamil = s.isTamil).getOrNull()
    }

    fun submitCalculation() {
        val eval = LocalSafetyEngines.calculateLoanCost(received, fees, repayment, tenure, unit, tamil = s.isTamil)
        eval.fold(
            onSuccess = { res ->
                errorMessage = null
                calculated = true
                val risk = when {
                    res.annualizedRatePercent > 100.0 -> RiskLevel.DANGER
                    res.annualizedRatePercent > 36.0 -> RiskLevel.CAUTION
                    else -> RiskLevel.CLEAR
                }
                onRecordActivity("Loan cost calculation", "Loan cost estimate", risk)
            },
            onFailure = { err ->
                calculated = false
                errorMessage = err.message ?: s.loanInvalidTitle
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageTitle(
            eyebrow = s.loanEyebrow,
            title = s.navLoanCalculator,
            subtitle = s.loanSubtitle,
        )

        // Form Card
        CheckFirstCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.loanFormTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                    Text(s.loanFormSub, style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                SmallTag(s.loanDemoTag)
            }
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = received,
                    onValueChange = { received = it; calculated = false; errorMessage = null },
                    label = { Text(s.loanReceivedLabel) },
                    supportingText = { Text(s.loanReceivedSupport) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = checkFirstTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("loan_received_input"),
                    shape = InputShape,
                )
                OutlinedTextField(
                    value = fees,
                    onValueChange = { fees = it; calculated = false; errorMessage = null },
                    label = { Text(s.loanFeesLabel) },
                    supportingText = { Text(s.loanFeesSupport) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = checkFirstTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("loan_fees_input"),
                    shape = InputShape,
                )
                OutlinedTextField(
                    value = repayment,
                    onValueChange = { repayment = it; calculated = false; errorMessage = null },
                    label = { Text(s.loanRepaymentLabel) },
                    supportingText = { Text(s.loanRepaymentSupport) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = checkFirstTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("loan_repayment_input"),
                    shape = InputShape,
                )
                OutlinedTextField(
                    value = tenure,
                    onValueChange = { tenure = it; calculated = false; errorMessage = null },
                    label = { Text(s.loanTenureLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = checkFirstTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("loan_tenure_input"),
                    shape = InputShape,
                )
                Text(s.loanUnitLabel, style = MaterialTheme.typography.labelLarge, color = c.navy)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TenureUnit.entries.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u; calculated = false; errorMessage = null },
                            label = { Text(s.tenureUnitLabel(u)) },
                            shape = PillShape,
                            colors = checkFirstFilterChipColors(),
                            modifier = Modifier.testTag("loan_unit_${u.name.lowercase()}"),
                        )
                    }
                }
                if (!errorMessage.isNullOrBlank()) {
                    InlineErrorBanner(errorMessage!!)
                }
                Button(
                    onClick = { submitCalculation() },
                    modifier = Modifier.fillMaxWidth().testTag("loan_calculate_button"),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(s.loanCalcBtn, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Result Cards
        if (calculated && calcResult != null && calcResult.cost >= 0.0) {
            CosmicHorizonCard {
                Text(s.loanResultEyebrow, style = MaterialTheme.typography.labelMedium, color = c.accentLight)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.1f", calcResult.annualizedRatePercent),
                        style = MaterialTheme.typography.displayLarge,
                        color = c.navy,
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = c.accentLight,
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                SmallTag(s.loanNotAprTag)
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = c.line)
                Spacer(Modifier.height(12.dp))
                LoanMetricRow(s.loanMetricReceived, LocalSafetyEngines.formatInr(calcResult.received), c.clear)
                Spacer(Modifier.height(8.dp))
                LoanMetricRow(s.loanMetricRepayment, LocalSafetyEngines.formatInr(calcResult.repayment), c.navy)
                Spacer(Modifier.height(8.dp))
                LoanMetricRow(s.loanMetricCost, LocalSafetyEngines.formatInr(calcResult.cost), c.danger)
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = c.accentTint,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, c.accentBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val per100 = (calcResult.repayment / calcResult.received) * 100.0
                    Text(
                        text = String.format(Locale.US, s.loanPer100Format, per100),
                        style = MaterialTheme.typography.labelLarge,
                        color = c.accentLight,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            CheckFirstCard {
                Text(s.loanComparisonTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                Text(s.loanComparisonSub, style = MaterialTheme.typography.bodySmall, color = c.muted)
                Spacer(Modifier.height(14.dp))
                val denom = calcResult.repayment.coerceAtLeast(1.0)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    HorizontalMetricBar(
                        label = s.loanMetricReceived,
                        valueText = LocalSafetyEngines.formatInr(calcResult.received),
                        fraction = (calcResult.received / denom).toFloat(),
                        barColor = c.clear,
                    )
                    HorizontalMetricBar(
                        label = s.loanMetricRepayment,
                        valueText = LocalSafetyEngines.formatInr(calcResult.repayment),
                        fraction = (calcResult.repayment / denom).toFloat(),
                        barColor = c.accentGlow,
                    )
                    HorizontalMetricBar(
                        label = s.loanMetricCost,
                        valueText = LocalSafetyEngines.formatInr(calcResult.cost),
                        fraction = (calcResult.cost / denom).toFloat(),
                        barColor = c.danger,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = s.loanComparisonNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.subtle,
                )
            }

            CheckFirstCard {
                Text(s.loanFormulaTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = c.surfaceRaised,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, c.line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = s.loanFormulaBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.ink,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = s.loanFormulaDisclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.subtle,
                )
            }
        } else if (!errorMessage.isNullOrBlank()) {
            EmptyResultCard(
                icon = Icons.Filled.ErrorOutline,
                title = s.loanInvalidTitle,
                subtitle = s.loanInvalidSub,
            )
        } else {
            EmptyResultCard(
                icon = Icons.Filled.Calculate,
                title = s.loanPendingTitle,
                subtitle = s.loanPendingSub,
            )
        }

        CheckFirstCard {
            Text(s.loanHowTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(6.dp))
            Text(
                text = s.loanHowBody,
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LoanMetricRow(label: String, value: String, valueColor: Color) {
    val c = LocalCheckFirstColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.muted)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = valueColor)
    }
}
