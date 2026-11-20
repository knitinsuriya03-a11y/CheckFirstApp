package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.CheckFirstAiRepository
import com.example.domain.AiInsightUiState
import com.example.domain.CheckFirstDemoData
import com.example.domain.LocalSafetyEngines
import com.example.domain.RiskLevel
import com.example.domain.UpiPaymentDetails
import com.example.ui.components.AiInsightSection
import com.example.ui.components.CheckFirstCard
import com.example.ui.components.EmptyResultCard
import com.example.ui.components.InlineErrorBanner
import com.example.ui.components.InputShape
import com.example.ui.components.PageTitle
import com.example.ui.components.PillShape
import com.example.ui.components.PinReminderCard
import com.example.ui.components.SmallTag
import com.example.ui.components.checkFirstTextFieldColors
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalCheckFirstColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QrAnalyzerScreen(
    tamil: Boolean,
    aiRepository: CheckFirstAiRepository,
    onRecordActivity: (kind: String, label: String, risk: RiskLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    val effectiveTamil = tamil || s.isTamil
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var link by rememberSaveable { mutableStateOf("") }
    var inspectedRawUri by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasSelectedImage by rememberSaveable { mutableStateOf(false) }
    var isDecoding by remember { mutableStateOf(false) }
    var aiInsightState by remember { mutableStateOf<AiInsightUiState>(AiInsightUiState.Idle) }

    // Re-parse reactively when language switches so fallback labels ("Not provided" / "வழங்கப்படவில்லை") update immediately
    val parsed: UpiPaymentDetails? = remember(inspectedRawUri, effectiveTamil) {
        inspectedRawUri?.let { LocalSafetyEngines.parseUpiUri(it, tamil = effectiveTamil).getOrNull() }
    }

    fun inspectUri(raw: String) {
        val res = LocalSafetyEngines.parseUpiUri(raw, tamil = effectiveTamil)
        res.fold(
            onSuccess = { details ->
                inspectedRawUri = raw
                errorMessage = null
                aiInsightState = AiInsightUiState.Idle
                onRecordActivity(
                    "UPI link analysis",
                    "Payment request inspected",
                    if (details.isRefundMislead) RiskLevel.DANGER else RiskLevel.CAUTION,
                )
            },
            onFailure = { err ->
                inspectedRawUri = null
                aiInsightState = AiInsightUiState.Idle
                errorMessage = err.message ?: if (effectiveTamil) {
                    "கட்டண இணைப்பை ஆய்வு செய்ய முடியவில்லை."
                } else {
                    "Could not inspect payment URI."
                }
            },
        )
    }

    // Zero-permission Android Photo Picker (Google Play policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        hasSelectedImage = true
        errorMessage = null
        inspectedRawUri = null
        aiInsightState = AiInsightUiState.Idle
        isDecoding = true

        scope.launch {
            val decodeResult = withContext(Dispatchers.IO) {
                try {
                    val mime = context.contentResolver.getType(uri)
                    if (mime != null && mime != "image/png" && mime != "image/jpeg" && mime != "image/webp") {
                        return@withContext Result.failure<String>(
                            IllegalArgumentException(s.qrErrUnsupportedFormat),
                        )
                    }
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext Result.failure<String>(IllegalStateException(s.qrErrReadFailed))
                    if (bytes.size > 5 * 1024 * 1024) {
                        return@withContext Result.failure<String>(IllegalArgumentException(s.qrErrTooLarge))
                    }
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: return@withContext Result.failure<String>(IllegalStateException(s.qrErrDecodeFailed))
                    try {
                        LocalSafetyEngines.decodeQrFromBitmap(bitmap, tamil = effectiveTamil)
                    } finally {
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            isDecoding = false
            decodeResult.fold(
                onSuccess = { decoded ->
                    link = decoded
                    inspectUri(decoded)
                },
                onFailure = { err ->
                    errorMessage = err.message ?: s.qrErrDecodeFailed
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
            eyebrow = s.qrEyebrow,
            title = s.navQrAnalyzer,
            subtitle = s.qrSubtitle,
        )

        // Upload & Paste Card
        CheckFirstCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.qrCardTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                    Text(s.qrCardSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(c.accentTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(14.dp))

            // Zero-permission Photo Picker Dropzone
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !isDecoding) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
                    .testTag("qr_pick_image_button"),
                color = c.surfaceGlow,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, c.accentBorder),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 22.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(c.accentTint),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDecoding) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = c.accentLight, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(22.dp))
                        }
                    }
                    Text(
                        text = if (hasSelectedImage) s.qrImageSelected else s.qrPickPrompt,
                        style = MaterialTheme.typography.titleSmall,
                        color = c.navy,
                        textAlign = TextAlign.Center,
                    )
                    Text(s.qrPickSub, style = MaterialTheme.typography.bodySmall, color = c.muted, textAlign = TextAlign.Center)
                    Text(s.qrPickCta, style = MaterialTheme.typography.labelLarge, color = c.accentLight)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = c.line)
                Text(s.qrOrPaste, style = MaterialTheme.typography.labelSmall, color = c.subtle)
                HorizontalDivider(modifier = Modifier.weight(1f), color = c.line)
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = link,
                onValueChange = {
                    link = it
                    inspectedRawUri = null
                    errorMessage = null
                    aiInsightState = AiInsightUiState.Idle
                },
                label = { Text(s.qrLinkLabel) },
                placeholder = { Text("upi://pay?pa=…") },
                singleLine = true,
                colors = checkFirstTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("qr_upi_link_input"),
                shape = InputShape,
            )
            Spacer(Modifier.height(4.dp))
            Text(s.qrLinkHelper, style = MaterialTheme.typography.bodySmall, color = c.subtle)
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { inspectUri(link) },
                    enabled = !isDecoding,
                    modifier = Modifier.weight(1f).testTag("qr_analyze_button"),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(s.qrAnalyzeBtn, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = {
                        link = ""
                        inspectedRawUri = null
                        errorMessage = null
                        hasSelectedImage = false
                        aiInsightState = AiInsightUiState.Idle
                    },
                    modifier = Modifier.testTag("qr_clear_button"),
                    shape = PillShape,
                    border = BorderStroke(1.dp, c.pillSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = c.pillSecondary,
                        contentColor = c.navy,
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 18.dp),
                ) {
                    Text(s.scanClearBtn)
                }
            }
            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                InlineErrorBanner(errorMessage!!)
            }
        }

        // Decoded Payment Breakdown OR Empty State with Sample
        val details = parsed
        if (details != null) {
            CheckFirstCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text(s.qrDetailsTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                        Text(s.qrDetailsSub, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    SmallTag(s.qrDecodedTag)
                }
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = if (details.isRefundMislead) c.dangerCardBg else c.surfaceGlow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (details.isRefundMislead) c.dangerBorder else c.accentBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (details.isRefundMislead) s.qrReviewBanner else s.qrDetailsBanner,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (details.isRefundMislead) c.danger else c.accentLight,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (details.amount != null) {
                                LocalSafetyEngines.formatInr(details.amount, minDecimals = 2, maxDecimals = 2)
                            } else {
                                s.qrAmountNotSpecified
                            },
                            style = MaterialTheme.typography.displaySmall,
                            color = c.navy,
                        )
                        Text(s.qrRequestedAmount, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                DetailRow(s.qrFieldPayeeName, details.name)
                DetailRow(s.qrFieldUpiAddress, details.payee)
                DetailRow(s.qrFieldNote, details.note)
                DetailRow(s.qrFieldCurrency, details.currency)
                DetailRow(s.qrFieldMode, details.mode)

                Spacer(Modifier.height(14.dp))
                Surface(
                    color = if (details.isRefundMislead) c.dangerBg else c.surfaceRaised,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (details.isRefundMislead) c.dangerBorder else c.line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (details.isRefundMislead) c.dangerCardBg else c.accentTint),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.ArrowOutward,
                                contentDescription = null,
                                tint = if (details.isRefundMislead) c.danger else c.accentLight,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.qrDirectionTitle, style = MaterialTheme.typography.labelLarge, color = c.navy)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "${s.qrDirectionDescPrefix}${details.name}${s.qrDirectionDescSuffix}",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.ink,
                            )
                        }
                    }
                }

                if (details.isRefundMislead) {
                    Spacer(Modifier.height(12.dp))
                    InlineErrorBanner(s.qrRefundWarning)
                }

                Spacer(Modifier.height(14.dp))
                AiInsightSection(
                    state = aiInsightState,
                    buttonLabel = s.aiInsightButton,
                    onRequestInsight = {
                        scope.launch {
                            aiInsightState = AiInsightUiState.Loading
                            val res = aiRepository.analyzeUpiWithGemini(details, effectiveTamil)
                            aiInsightState = res.fold(
                                onSuccess = { AiInsightUiState.Success(it) },
                                onFailure = {
                                    AiInsightUiState.Error(
                                        it.localizedMessage ?: if (effectiveTamil) {
                                            "Gemini ஆலோசனையைப் பெற முடியவில்லை."
                                        } else {
                                            "Unable to fetch Gemini insights."
                                        },
                                    )
                                },
                            )
                        }
                    },
                    testTag = "qr_gemini_insight_button",
                )

                Spacer(Modifier.height(12.dp))
                PinReminderCard(
                    title = s.qrPinTitle,
                    body = s.qrPinBody,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = s.qrDisclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.subtle,
                )
            }
        } else {
            EmptyResultCard(
                icon = Icons.Filled.QrCode2,
                title = s.qrEmptyTitle,
                subtitle = s.qrEmptySub,
            ) {
                Surface(
                    color = c.surfaceRaised,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, c.line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(s.qrTryExample, style = MaterialTheme.typography.labelMedium, color = c.accentLight)
                        Text(
                            text = CheckFirstDemoData.SAMPLE_UPI_URI,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = c.muted,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    link = CheckFirstDemoData.SAMPLE_UPI_URI
                                    errorMessage = null
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.testTag("qr_load_sample_button"),
                            ) {
                                Text(s.qrLoadSample, color = c.accentLight, style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(Modifier.width(4.dp))
                            OutlinedButton(
                                onClick = {
                                    link = CheckFirstDemoData.SAMPLE_UPI_URI
                                    inspectUri(CheckFirstDemoData.SAMPLE_UPI_URI)
                                },
                                modifier = Modifier.weight(1f).testTag("qr_analyze_sample_button"),
                                shape = PillShape,
                                border = BorderStroke(1.dp, c.accentBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = c.accentTint,
                                    contentColor = c.accentLight,
                                ),
                            ) {
                                Text(s.qrAnalyzeSample)
                            }
                        }
                    }
                }
            }
        }

        // Info Card
        CheckFirstCard {
            Text(s.qrKnowTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(10.dp))
            s.qrKnowItems.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = c.clear, modifier = Modifier.size(16.dp))
                    Text(item, style = MaterialTheme.typography.bodySmall, color = c.ink)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(s.qrKnowFooter, style = MaterialTheme.typography.bodySmall, color = c.subtle)
        }

        PinReminderCard(
            title = s.qrRememberTitle,
            body = s.qrRememberBody,
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val c = LocalCheckFirstColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = c.muted)
            Spacer(Modifier.width(12.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, color = c.navy, textAlign = TextAlign.End)
        }
        HorizontalDivider(color = c.track)
    }
}
