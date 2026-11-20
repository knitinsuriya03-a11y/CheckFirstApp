package com.example.domain

import android.graphics.Bitmap
import android.net.Uri
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.text.NumberFormat
import java.util.Locale

object CheckFirstDemoData {
    val starterActivity: List<ActivityItem> = listOf(
        ActivityItem("demo-1", "Message scan", "Bank refund message", RiskLevel.DANGER, "Sample · 10:42 AM", true),
        ActivityItem("demo-2", "Loan check", "Short-term loan offer", RiskLevel.CAUTION, "Sample · 10:35 AM", true),
        ActivityItem("demo-3", "Link analysis", "Payment link preview", RiskLevel.CAUTION, "Sample · Yesterday", true),
    )

    val weeklyChecks: List<Pair<String, Int>> = listOf(
        "Mon" to 12,
        "Tue" to 16,
        "Wed" to 14,
        "Thu" to 20,
        "Fri" to 18,
        "Sat" to 22,
        "Sun" to 18,
    )

    val commonWarnings: List<Pair<String, Int>> = listOf(
        "Urgency / pressure" to 42,
        "PIN or OTP request" to 30,
        "Upfront payment" to 24,
        "Unfamiliar link" to 18,
    )

    const val SAMPLE_SCAM_MESSAGE =
        "Congratulations! Your bank refund is pending. Scan this QR code and enter your UPI PIN within 10 minutes to receive your refund: https://example.invalid/refund"

    const val SAMPLE_SCAM_MESSAGE_TAMIL =
        "அவசரம்! உங்கள் வங்கி திரும்பப் பணம் நிலுவையில் உள்ளது. 10 நிமிடத்தில் பணத்தைப் பெற QR ஸ்கேன் செய்து உங்கள் யுபிஐ பின் உள்ளிடவும்: https://example.invalid/refund"

    const val SAMPLE_UPI_URI =
        "upi://pay?pa=refund-help@demobank&pn=Refund%20Support&am=2000&tn=Refund%20processing"
}

object LocalSafetyEngines {

    private data class ScanRule(
        val id: String,
        val labelEn: String,
        val labelTa: String,
        val explanationEn: String,
        val explanationTa: String,
        val regex: Regex,
        val severity: RiskLevel,
        val highlightTerms: List<String>,
    )

    private val scanRules: List<ScanRule> = listOf(
        ScanRule(
            id = "urgency",
            labelEn = "Urgency or pressure",
            labelTa = "அவசரம் அல்லது அழுத்தம்",
            explanationEn = "The message creates pressure to act quickly instead of giving you time to verify independently.",
            explanationTa = "தனியாக சரிபார்க்க நேரம் தராமல் உடனடியாக செயல்பட இந்த செய்தி அழுத்தம் கொடுக்கிறது.",
            regex = Regex(
                """\b(within\s*\d+\s*(minute|min|hour)|immediately|urgent|act now|last chance|expire soon|today only|account.*block|10 minutes)\b|உடனே|அவசரம்|நிமிடத்தில்""",
                RegexOption.IGNORE_CASE,
            ),
            severity = RiskLevel.CAUTION,
            highlightTerms = listOf("within 10 minutes", "immediately", "urgent", "act now", "last chance", "உடனே", "அவசரம்", "நிமிடத்தில்"),
        ),
        ScanRule(
            id = "authority",
            labelEn = "Possible impersonation",
            labelTa = "ஆள்மாறாட்டம் இருக்கலாம்",
            explanationEn = "It claims to represent a bank, government body, police, courier, or service provider. Verify using a known official channel.",
            explanationTa = "வங்கி, அரசு, காவல்துறை அல்லது சேவை நிறுவனம் எனக் கூறுகிறது. அதிகாரப்பூர்வ எண்ணில் தனியாக சரிபார்க்கவும்.",
            regex = Regex(
                """\b(bank|rbi|police|cyber cell|courier|electricity board|customs|income tax|kyc|customer care)\b|வங்கி|காவல்துறை""",
                RegexOption.IGNORE_CASE,
            ),
            severity = RiskLevel.CAUTION,
            highlightTerms = listOf("bank", "police", "RBI", "KYC", "cyber cell", "வங்கி", "காவல்துறை"),
        ),
        ScanRule(
            id = "refund",
            labelEn = "Refund or receive-money wording",
            labelTa = "பணம் திரும்பப் பெறும் ஏமாற்று வார்த்தைகள்",
            explanationEn = "It presents a QR scan, payment approval, or PIN entry as a way to receive a refund. A UPI PIN authorizes an outgoing payment.",
            explanationTa = "பணம் திரும்பப் பெற QR ஸ்கேன் அல்லது PIN உள்ளிடச் சொல்கிறது. UPI PIN என்பது பணம் அனுப்ப மட்டுமே பயன்படும்.",
            regex = Regex(
                """\b(refund|cashback|receive money|credit.*refund|scan.*qr|qr.*refund|claim.*refund)\b|பணத்தைப் பெற|திரும்பப் பணம்""",
                RegexOption.IGNORE_CASE,
            ),
            severity = RiskLevel.DANGER,
            highlightTerms = listOf("refund", "scan this QR", "scan QR", "cashback", "receive money", "பணத்தைப் பெற", "திரும்பப் பணம்"),
        ),
        ScanRule(
            id = "credential",
            labelEn = "Sensitive credential request",
            labelTa = "ரகசிய எண் / PIN / OTP கோரிக்கை",
            explanationEn = "A request for a UPI PIN, OTP, password, or banking credential is a serious warning sign. Never share these details.",
            explanationTa = "UPI PIN, OTP, கடவுச்சொல் அல்லது வங்கி ரகசியத் தகவல்களைக் கேட்பது தீவிர அபாய அறிகுறி. ஒருபோதும் பகிர வேண்டாம்.",
            regex = Regex(
                """\b(upi\s*pin|otp|one.?time password|bank password|card pin|cvv|password|share.*pin|enter.*pin)\b|ரகசிய எண்|கடவுச்சொல்|யுபிஐ\s*பின்|ஓடிபி""",
                RegexOption.IGNORE_CASE,
            ),
            severity = RiskLevel.DANGER,
            highlightTerms = listOf("UPI PIN", "OTP", "password", "CVV", "PIN", "ரகசிய எண்", "கடவுச்சொல்", "யுபிஐ பின்", "ஓடிபி"),
        ),
        ScanRule(
            id = "fee",
            labelEn = "Upfront payment request",
            labelTa = "முன்பணம் கோரிக்கை",
            explanationEn = "The message appears to ask for a fee before a reward, refund, job, or service is delivered.",
            explanationTa = "பரிசு, பணம் திரும்பப் பெறுதல், வேலை அல்லது சேவைக்கு முன்பே கட்டணம் கேட்பதாகத் தெரிகிறது.",
            regex = Regex(
                """\b(processing fee|registration fee|pay.*first|advance fee|unlock.*payment|verification fee)\b|முன்பணம்""",
                RegexOption.IGNORE_CASE,
            ),
            severity = RiskLevel.CAUTION,
            highlightTerms = listOf("processing fee", "registration fee", "advance fee", "verification fee", "முன்பணம்"),
        ),
        ScanRule(
            id = "lure",
            labelEn = "Prize or job lure",
            labelTa = "பரிசு அல்லது வேலை ஆசை",
            explanationEn = "Unexpected prizes or job offers can be used to start a conversation that later asks for money or personal information.",
            explanationTa = "எதிர்பாராத பரிசுகள் அல்லது வேலை சலுகைகள் பின்னர் பணமோ தனிப்பட்ட தகவலோ கேட்கப் பயன்படுத்தப்படலாம்.",
            regex = Regex(
                """\b(congratulations|winner|prize|lottery|selected for job|work from home|guaranteed income|reward)\b|பரிசு""",
                RegexOption.IGNORE_CASE,
            ),
            severity = RiskLevel.CAUTION,
            highlightTerms = listOf("congratulations", "winner", "prize", "lottery", "reward", "பரிசு"),
        ),
        ScanRule(
            id = "link",
            labelEn = "Link or contact needs verification",
            labelTa = "இணைப்பு அல்லது எண் சரிபார்க்கப்பட வேண்டும்",
            explanationEn = "The message includes a link or asks you to contact a number. Do not use contact details supplied in a suspicious message.",
            explanationTa = "இந்த செய்தியில் இணைய இணைப்பு அல்லது தொலைபேசி எண் உள்ளது. சந்தேகமான செய்தியில் உள்ள விவரங்களைப் பயன்படுத்த வேண்டாம்.",
            regex = Regex("""(https?://|www\.|bit\.ly|tinyurl|wa\.me|t\.me|\+91[\s-]?\d{5}[\s-]?\d{5}|\b\d{10}\b)""", RegexOption.IGNORE_CASE),
            severity = RiskLevel.CAUTION,
            highlightTerms = listOf("https://", "http://", "www.", "bit.ly", "wa.me", "t.me", "+91"),
        ),
    )

    fun scanMessageLocal(message: String, tamil: Boolean = false): MessageScanResult {
        val matches = scanRules
            .filter { it.regex.containsMatchIn(message) }
            .map { rule ->
                MatchedIndicator(
                    id = rule.id,
                    label = if (tamil) rule.labelTa else rule.labelEn,
                    explanation = if (tamil) rule.explanationTa else rule.explanationEn,
                    severity = rule.severity,
                    highlightTerms = rule.highlightTerms,
                )
            }

        val risk = when {
            matches.any { it.severity == RiskLevel.DANGER } -> RiskLevel.DANGER
            matches.isNotEmpty() -> RiskLevel.CAUTION
            else -> RiskLevel.CLEAR
        }

        val verdict = when (risk) {
            RiskLevel.DANGER -> if (tamil) {
                "பல தீவிர எச்சரிக்கை அறிகுறிகள் கண்டறியப்பட்டன."
            } else {
                "Several serious warning signs were found."
            }

            RiskLevel.CAUTION -> if (tamil) {
                "சில விவரங்களைத் தனியாக சரிபார்க்க வேண்டும்."
            } else {
                "Some details deserve independent verification."
            }

            RiskLevel.CLEAR -> if (tamil) {
                "இந்த உரையில் அமைக்கப்பட்ட மோசடி அறிகுறிகள் எதுவும் கிடைக்கவில்லை."
            } else {
                "No configured warning patterns were found in this text."
            }
        }

        return MessageScanResult(risk = risk, verdict = verdict, matches = matches)
    }

    fun parseUpiUri(value: String, tamil: Boolean = false): Result<UpiPaymentDetails> {
        val raw = value.trim()
        if (raw.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    if (tamil) "UPI கட்டண இணைப்பை ஒட்டவும் அல்லது QR படத்தைப் பதிவேற்றவும்."
                    else "Paste a UPI payment link or upload a QR image.",
                ),
            )
        }
        val uri = try {
            Uri.parse(raw)
        } catch (_: Exception) {
            return Result.failure(
                IllegalArgumentException(
                    if (tamil) "இது சரியான கட்டண இணைப்பாகத் தெரியவில்லை."
                    else "This does not look like a valid payment URI.",
                ),
            )
        }
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "upi") {
            return Result.failure(
                IllegalArgumentException(
                    if (tamil) "UPI கட்டண இணைப்புகள் (upi://...) மட்டுமே ஆதரிக்கப்படுகின்றன."
                    else "Only UPI payment URIs (upi://...) are supported.",
                ),
            )
        }

        val rawPayee = uri.getQueryParameter("pa")?.trim().orEmpty()
        if (rawPayee.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    if (tamil) "இந்த UPI இணைப்பில் பெறுநர் முகவரி (pa) இல்லை."
                    else "This UPI URI is missing a payee address (pa).",
                ),
            )
        }

        val amountText = uri.getQueryParameter("am")?.trim()
        val amount = if (!amountText.isNullOrBlank()) {
            val parsed = amountText.replace(",", "").toDoubleOrNull()
            if (parsed == null || !parsed.isFinite() || parsed < 0.0) {
                return Result.failure(
                    IllegalArgumentException(
                        if (tamil) "இந்த UPI இணைப்பில் உள்ள தொகை தவறானது."
                        else "The amount in this UPI URI is invalid.",
                    ),
                )
            }
            parsed
        } else {
            null
        }

        val notProvided = if (tamil) "வழங்கப்படவில்லை" else "Not provided"
        val notSpecified = if (tamil) "குறிப்பிடப்படவில்லை" else "Not specified"
        val name = uri.getQueryParameter("pn").orEmpty().trim().ifBlank { notProvided }
        val note = uri.getQueryParameter("tn")?.trim()?.takeIf { it.isNotBlank() }
            ?: uri.getQueryParameter("tr")?.trim()?.takeIf { it.isNotBlank() }
            ?: notProvided
        val currency = uri.getQueryParameter("cu").orEmpty().trim().ifBlank { "INR" }
        val mode = uri.getQueryParameter("mode").orEmpty().trim().ifBlank { notSpecified }

        // Detect both fixed-amount AND open-amount (am omitted) fake refund UPI requests in English & Tamil
        val refundRegex = Regex("""refund|cashback|receive|credit|திரும்ப|பணம்\s*பெற|ரீபண்ட்""", RegexOption.IGNORE_CASE)
        val isRefundMislead = (amount == null || amount > 0.0) && refundRegex.containsMatchIn("$note $name $rawPayee")

        return Result.success(
            UpiPaymentDetails(
                payee = rawPayee,
                name = name,
                amount = amount,
                note = note,
                currency = currency,
                mode = mode,
                raw = raw,
                isRefundMislead = isRefundMislead,
            ),
        )
    }

    fun decodeQrFromBitmap(bitmap: Bitmap, tamil: Boolean = false): Result<String> {
        val failureMsg = if (tamil) {
            "படிக்கக்கூடிய QR குறியீடு எதுவும் கிடைக்கவில்லை. தெளிவான படத்தை முயற்சிக்கவும்."
        } else {
            "No readable QR code found. Try a clearer image."
        }
        return try {
            val scaled = if (bitmap.width > 1200 || bitmap.height > 1200) {
                val ratio = minOf(1200f / bitmap.width, 1200f / bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                bitmap
            }
            val width = scaled.width
            val height = scaled.height
            val pixels = IntArray(width * height)
            scaled.getPixels(pixels, 0, width, 0, 0, width, height)
            if (scaled !== bitmap) scaled.recycle()

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            val text = result.text?.trim().orEmpty()
            if (text.isEmpty()) {
                Result.failure(IllegalStateException(failureMsg))
            } else {
                Result.success(text)
            }
        } catch (_: Exception) {
            Result.failure(IllegalStateException(failureMsg))
        }
    }

    private fun parseSanitizedNumber(raw: String): Double? =
        raw.trim().replace(",", "").replace("₹", "").replace(" ", "").toDoubleOrNull()

    fun calculateLoanCost(
        receivedText: String,
        feesText: String,
        repaymentText: String,
        tenureText: String,
        unit: TenureUnit,
        tamil: Boolean = false,
    ): Result<LoanCalculationResult> {
        val r = parseSanitizedNumber(receivedText)
        val f = if (feesText.trim().isEmpty()) 0.0 else parseSanitizedNumber(feesText)
        val p = parseSanitizedNumber(repaymentText)
        val t = parseSanitizedNumber(tenureText)

        if (r == null || f == null || p == null || t == null ||
            !r.isFinite() || !f.isFinite() || !p.isFinite() || !t.isFinite() ||
            r <= 0.0 || p <= 0.0 || f < 0.0 || t <= 0.0
        ) {
            return Result.failure(
                IllegalArgumentException(
                    if (tamil) "சரியான நேர்மறை தொகைகள் மற்றும் காலத்தை உள்ளிடவும்."
                    else "Enter valid positive amounts and tenure.",
                ),
            )
        }

        val cost = p - r
        if (cost < 0.0) {
            return Result.failure(
                IllegalArgumentException(
                    if (tamil) "சரியான தொகைகளை உள்ளிடவும். மொத்த திருப்பிச் செலுத்தல் கையில் கிடைத்த தொகையை விட குறைவாக இருக்கக் கூடாது."
                    else "Enter valid amounts. Total repayment should not be lower than the amount received.",
                ),
            )
        }

        val days = when (unit) {
            TenureUnit.DAYS -> t
            TenureUnit.MONTHS -> t * 30.4375
            TenureUnit.YEARS -> t * 365.25
        }
        val rate = (cost / r) * (365.0 / days) * 100.0
        return Result.success(
            LoanCalculationResult(
                received = r,
                fees = f,
                repayment = p,
                cost = cost,
                annualizedRatePercent = rate,
                days = days,
            ),
        )
    }

    val loanRiskQuestions: List<String> = listOf(
        "Is a large fee deducted before you receive the loan?",
        "Is the repayment period unusually short?",
        "Is the total repayment much higher than the amount received?",
        "Is the lender asking for unnecessary phone permissions?",
        "Is the lender pressuring you to accept immediately?",
        "Are the interest rate, fees, and repayment terms unclear?",
    )

    private val loanRiskQuestionsTamil: List<String> = listOf(
        "கடன் கிடைக்கும் முன்பே பெரிய கட்டணம் கழிக்கப்படுகிறதா?",
        "திருப்பிச் செலுத்தும் காலம் வழக்கத்திற்கு மாறாக மிகவும் குறுகியதா?",
        "கையில் கிடைத்த தொகையை விட திருப்பிச் செலுத்தும் மொத்த தொகை மிக அதிகமா?",
        "கடன் வழங்குநர் தேவையற்ற தொலைபேசி அனுமதிகளை (Contacts/Photos) கேட்கிறாரா?",
        "உடனடியாக ஒப்புக்கொள்ளுமாறு கடன் வழங்குநர் அழுத்தம் கொடுக்கிறாரா?",
        "வட்டி விகிதம், கட்டணங்கள் மற்றும் திருப்பிச் செலுத்தும் நிபந்தனைகள் தெளிவாக இல்லையா?",
    )

    fun loanRiskQuestions(tamil: Boolean): List<String> =
        if (tamil) loanRiskQuestionsTamil else loanRiskQuestions

    val loanRiskShortLabels: List<String> = listOf(
        "Upfront fee deduction",
        "Short repayment period",
        "High total repayment",
        "Unnecessary permissions",
        "Pressure to accept",
        "Unclear loan terms",
    )

    private val loanRiskShortLabelsTamil: List<String> = listOf(
        "முன்பே கட்டண பிடித்தம்",
        "குறுகிய திருப்பிச் செலுத்தும் காலம்",
        "அதிக மொத்த திருப்பிச் செலுத்தல்",
        "தேவையற்ற தொலைபேசி அனுமதிகள்",
        "உடனடி ஏற்பு அழுத்தம்",
        "தெளிவற்ற கடன் நிபந்தனைகள்",
    )

    fun loanRiskShortLabels(tamil: Boolean): List<String> =
        if (tamil) loanRiskShortLabelsTamil else loanRiskShortLabels

    fun formatInr(value: Double, minDecimals: Int = 0, maxDecimals: Int = 2): String {
        val locale = Locale.Builder().setLanguage("en").setRegion("IN").build()
        val fmt = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = minDecimals
            maximumFractionDigits = maxDecimals
        }
        return "₹${fmt.format(value)}"
    }

    fun localizedLabel(englishLabel: String, tamil: Boolean): String {
        if (!tamil) return englishLabel
        return when (englishLabel) {
            "Dashboard" -> "முகப்பு"
            "Scam Message Scanner" -> "மோசடி செய்தி சோதனை"
            "QR & Payment Link Analyzer" -> "QR / கட்டண இணைப்பு"
            "Pause & Verify Guide" -> "நிறுத்தி சரிபார்க்கவும்"
            "Emergency Help" -> "அவசர உதவி"
            "Loan Cost Calculator" -> "கடன் செலவு கணிப்பான்"
            "Loan Risk Checker" -> "கடன் அபாய சோதனை"
            "Settings" -> "அமைப்புகள்"
            "AI Safety Chat" -> "AI பாதுகாப்பு அரட்டை"
            "Live Voice Advisor" -> "நேரடி குரல் ஆலோசகர்"
            else -> englishLabel
        }
    }
}
