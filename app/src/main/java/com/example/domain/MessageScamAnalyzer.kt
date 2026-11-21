package com.example.domain

import java.util.Locale

/**
 * Extracted contextual features from a message (English, Tamil, Tanglish, or mixed).
 */
data class MessageContext(
    val claimedIdentities: Set<ClaimedIdentity>,
    val requestedActions: Set<RequestedAction>,
    val sensitiveDataMentions: Set<SensitiveDataType>,
    val hasAffirmativeCredentialRequest: Boolean,
    val hasProtectiveCredentialWarning: Boolean,
    val hasHardThreat: Boolean,
    val hasUrgencyPressure: Boolean,
    val hasSecrecyInstruction: Boolean,
    val hasDigitalArrestCue: Boolean,
    val hasUpfrontFeeRequest: Boolean,
    val hasLureCue: Boolean,
    val hasDeviceAccessRequest: Boolean,
    val extractedUrls: List<String>,
    val suspiciousUrls: List<String>,
    val officialUrls: List<String>,
    val extractedUpiIds: List<String>,
    val extractedPhones: List<String>,
    val hasMoneyAmount: Boolean,
    val contradictions: List<String>,
)

enum class ClaimedIdentity {
    BANK,
    GOVERNMENT_UTILITY,
    POLICE_COURT,
    COURIER,
    EMPLOYER_JOB,
    MERCHANT_SUPPORT,
    FRIEND_FAMILY,
}

enum class RequestedAction {
    SHARE_DISCLOSE,
    ENTER_CREDENTIAL,
    CLICK_LINK,
    PAY_OR_TRANSFER,
    SCAN_QR,
    INSTALL_APP,
    CALL_CONTACT,
    VERIFY_ACCOUNT,
    RECEIVE_MONEY_CLAIM,
}

enum class SensitiveDataType {
    OTP,
    UPI_PIN,
    PASSWORD_CVV_CARD,
}

/**
 * Contextual, intent-aware Scam Shield analyzer supporting English, Tamil script,
 * Tanglish, and code-mixed messages.
 *
 * Uses [SafetyDecisionEngine] to produce structured [SafetyAssessment] results with
 * documented rule IDs, trigger reasons, exceptions/limitations, and non-downgradable
 * critical rules.
 */
object MessageScamAnalyzer {

    const val FEATURE_NAME = "Scam Shield"

    // --- Identity patterns ---
    private val bankIdentityRegex = Regex(
        """\b(bank|sbi|hdfc|icici|axis|kotak|pnb|canara|bob|rbi|npci|paytm|phonepe|gpay|google\s*pay|bhim|kyc\s*dept|credit\s*card\s*dept|bank\s*(?:manager|officer|support|team)|bank\s*la\s*irundhu|bank\s*lendhu)\b|வங்கி(?:\s*அதிகாரி)?""",
        RegexOption.IGNORE_CASE,
    )
    private val policeIdentityRegex = Regex(
        """\b(police|cyber\s*cell|cyber\s*crime|cyber\s*police|cbi|enforcement\s*directorate|\bed\s*officer|\bncb\b|narcotics|supreme\s*court|high\s*court|magistrate|inspector|dsp|ips\s*officer|customs\s*officer|customs\s*dept|trai\s*officer)\b|காவல்துறை|போலீஸ்|சைபர்\s*கிரைம்|நீதிமன்றம்|சுங்க(?:த்துறை)?""",
        RegexOption.IGNORE_CASE,
    )
    private val govtUtilityIdentityRegex = Regex(
        """\b(government|govt|income\s*tax|it\s*dept|customs|trai|uidai|aadhaar\s*dept|epfo|electricity\s*board|tneb|bescom|msedcl|gas\s*connection|eb\s*office|eb\s*bill|electricity\s*bill|challan|rto)\b|அரசு|வருமான\s*வரி|மின்சார\s*வாரியம்|மின்\s*இணைப்பு""",
        RegexOption.IGNORE_CASE,
    )
    private val courierIdentityRegex = Regex(
        """\b(courier|fedex|dhl|bluedart|india\s*post|delhivery|parcel|shipment|consignment)\b|கூரியர்|பார்சல்""",
        RegexOption.IGNORE_CASE,
    )
    private val employerIdentityRegex = Regex(
        """\b(hr\s*team|recruiter|employer|job\s*offer|hiring\s*team|work\s*from\s*home|part[\s-]*time\s*(?:job|task)|telegram\s*task)\b|வேலை\s*வாய்ப்பு""",
        RegexOption.IGNORE_CASE,
    )
    private val merchantIdentityRegex = Regex(
        """\b(customer\s*care|support\s*team|helpdesk|refund\s*desk|merchant|amazon|flipkart|olx|meesho)\b|வாடிக்கையாளர்\s*சேவை""",
        RegexOption.IGNORE_CASE,
    )
    private val friendIdentityRegex = Regex(
        """\b(friend|amma|appa|anna|akka|machan|bro|sister|brother|relative)\b|நண்பர்|அம்மா|அப்பா|அண்ணா""",
        RegexOption.IGNORE_CASE,
    )

    // --- Sensitive credential patterns ---
    private val otpRegex = Regex(
        """\b(otp|one[\s-]*time\s*password|6[\s-]*digit\s*(?:code|otp)|4[\s-]*digit\s*otp|verification\s*code)\b|ஓடிபி|ஒருமுறை\s*கடவுச்சொல்""",
        RegexOption.IGNORE_CASE,
    )
    private val upiPinRegex = Regex(
        """\b(upi\s*pin|mpin|m-pin|atm\s*pin|card\s*pin|4[\s-]*digit\s*pin|6[\s-]*digit\s*pin|pin\s*number|(?:enter|type|share|put|give|podunga|podu)\s+(?:your\s+)?pin)\b|யுபிஐ\s*பின்|பின்\s*எண்|ரகசிய\s*எண்""",
        RegexOption.IGNORE_CASE,
    )
    private val passwordCvvRegex = Regex(
        """\b(cvv|cvc|netbanking\s*password|bank\s*password|login\s*password|password|debit\s*card\s*details|credit\s*card\s*details|card\s*number\s*(?:and|&)\s*(?:cvv|expiry))\b|கடவுச்சொல்|கார்டு\s*விவர""",
        RegexOption.IGNORE_CASE,
    )

    // --- Action verbs ---
    private val shareActionRegex = Regex(
        """\b(share|send|tell|give|provide|forward|reply\s*with|read\s*out|confirm\s*(?:your|the|with)|submit|disclose|pass\s*me|sollunga|sollu|anuppunga|anuppu|kodunga|kodu|share\s*pann?unga|send\s*pann?unga|tharunga)\b|பகிரவும்|பகிருங்கள்|சொல்லவும்|சொல்லுங்கள்|அனுப்பவும்|அனுப்புங்கள்|கொடுங்கள்|தரவும்""",
        RegexOption.IGNORE_CASE,
    )
    private val enterCredentialActionRegex = Regex(
        """\b(enter|type|fill|input|put|key\s*in|podunga|podu|type\s*pann?unga|enter\s*pann?unga)\b|உள்ளிடவும்|உள்ளிடுங்கள்|பதிவிடவும்|போடுங்கள்""",
        RegexOption.IGNORE_CASE,
    )
    private val protectiveNegationRegex = Regex(
        """\b(do\s*not\s*(?:share|disclose|tell|give|forward|enter)|don['’]?t\s*(?:share|disclose|tell|give|forward|enter)|never\s*(?:share|disclose|tell|give|ask\s*for)|not\s*to\s*share|keep\s*(?:it\s*)?confidential|nobody\s+from\s+\w+\s+will\s+ask|will\s+never\s+ask\s+for|yaarukkum\s*(?:soll?adheenga|soll?athinga|share\s*pann?adheenga|share\s*pann?athinga|kodukk?adheenga)|share\s*pann?a\s*vendam|share\s*pann?athinga|solla\s*vendam|pin\s*podadheenga)\b|பகிர\s*வேண்டாம்|பகிராதீர்கள்|பகிர\s*கூடாது|யாருக்கும்\s*சொல்ல\s*வேண்டாம்|யாருடனும்\s*பகிர\s*வேண்டாம்|யாருக்கும்\s*கொடுக்க\s*வேண்டாம்""",
        RegexOption.IGNORE_CASE,
    )
    private val passiveOtpNotificationRegex = Regex(
        """\b(your\s+(?:[\w\s]{0,18})?otp\s*(?:is|for|:)|otp\s*(?:is|:)\s*\d{4,8}|one[\s-]*time\s*password\s*(?:is|:)|ungal\s+otp)\b|உங்கள்\s*(?:வங்கி\s*)?(?:otp|ஓடிபி)""",
        RegexOption.IGNORE_CASE,
    )
    private val clickLinkRegex = Regex(
        """\b(click|tap|open|visit|follow\s*(?:this\s*)?link|login\s*at|verify\s*here|update\s*here|click\s*pann?unga|open\s*pann?unga)\b|கிளிக்|திறக்கவும்|தொடுங்கள்""",
        RegexOption.IGNORE_CASE,
    )
    private val payTransferRegex = Regex(
        """\b(pay\s*(?:now|rs|inr|₹|\d|immediately|first|the\s*fee|to)|transfer\s*(?:rs|inr|₹|\d|money|funds|immediately|amount|to)|send\s*(?:rs|inr|₹|\d|money)|deposit\s*(?:rs|inr|₹|\d|money|fee)|approve\s*(?:the\s*)?(?:payment|collect|request|mandate)|panam\s*(?:anuppunga|podunga|kattunga)|pay\s*pann?unga|fee\s*kattunga|katta\s*vendum)\b|பணம்\s*அனுப்ப|பணம்\s*செலுத்த|செலுத்தவும்|கட்டவும்""",
        RegexOption.IGNORE_CASE,
    )
    private val scanQrRegex = Regex(
        """\b(scan\s*(?:this\s*|the\s*)?qr(?:\s*code)?|qr\s*(?:code\s*)?scan(?:\s*pann?unga)?|scan\s*(?:this\s*)?code)\b|(?:qr|கியூஆர்)\s*ஸ்கேன்|ஸ்கேன்\s*செய்து""",
        RegexOption.IGNORE_CASE,
    )
    private val receiveClaimRegex = Regex(
        """\b(receive\s*(?:your\s*)?(?:money|refund|cashback|amount|payment)|get\s*(?:your\s*)?(?:refund|cashback|prize|reward)|claim\s*(?:your\s*)?(?:refund|cashback|prize|reward)|refund\s*(?:is\s*)?(?:pending|ready|approved)|credit\s*(?:your\s*)?refund|panam\s*pera|refund\s*(?:vara|kidaikka|ready)|cashback\s*pera)\b|பணத்தைப்\s*பெற|திரும்பப்\s*பணம்|பணம்\s*பெற|பணம்\s*வர""",
        RegexOption.IGNORE_CASE,
    )
    private val passiveRefundConfirmationRegex = Regex(
        """\b(refund\s+of\s+(?:rs\.?|inr|₹)\s*[\d,.]+\s+(?:for\s+[\w# -]+\s+)?has\s+been\s+(?:initiated|processed|credited)|credited\s+to\s+your\s+original\s+payment|will\s+reflect\s+in\s+your\s+(?:original\s+payment|bank\s+account))\b""",
        RegexOption.IGNORE_CASE,
    )
    private val verifyAccountRegex = Regex(
        """\b(verify\s*(?:your\s*)?(?:account|kyc|pan|card|details)|update\s*(?:your\s*)?(?:kyc|pan|account)|re-?kyc|reactivate|unblock\s*(?:your\s*)?account|kyc\s*(?:expire|expiring|expired|pending|suspended|update))\b|கேஒய்சி|சரிபார்க்கவும்|புதுப்பிக்கவும்""",
        RegexOption.IGNORE_CASE,
    )
    private val callActionRegex = Regex(
        """\b(call\s*(?:now|immediately|us|back|this|on|\+?91|\d)|dial|whatsapp\s*(?:us|on|now)|contact\s*(?:on|immediately)|call\s*pann?unga|phone\s*pann?unga)\b|அழைக்கவும்|தொடர்பு\s*கொள்ளவும்""",
        RegexOption.IGNORE_CASE,
    )

    // --- Device access & remote control ---
    private val remoteAccessRegex = Regex(
        """\b(anydesk|teamviewer|quicksupport|quick\s*support|rustdesk|ultraviewer|ammyy|supremo|airdroid|zoho\s*assist|splashtop|screen\s*share|share\s*(?:your\s*)?screen|remote\s*(?:access|support|control)\s*(?:app)?|accessibility\s*permission|allow\s*unknown\s*sources|screen\s*share\s*pann?unga)\b|திரை\s*பகிர்வு|ஸ்கிரீன்\s*ஷேர்""",
        RegexOption.IGNORE_CASE,
    )
    private val apkInstallRegex = Regex(
        """(?:[\w-]+\.apk\b|\b(?:install|download|sideload)\s+(?:this\s+|the\s+)?(?:apk|app\s+file|application\s+file)|\bapk\s*(?:file|link|install|download|pann?unga)|\b(?:kyc|challan|rto|bank|support)\s*\.?apk\b)|ஏபிகே""",
        RegexOption.IGNORE_CASE,
    )
    private val officialStoreUpdateRegex = Regex(
        """\b(google\s*play\s*store|play\s*store|apple\s*app\s*store|official\s*(?:bank\s*)?app)\b""",
        RegexOption.IGNORE_CASE,
    )

    // --- Threats, digital arrest, secrecy, urgency ---
    private val digitalArrestRegex = Regex(
        """\b(digital\s*arrest|arrest\s*warrant|non-?bailable\s*warrant|fir\s*(?:filed|registered|against)|money\s*laundering|illegal\s*(?:parcel|drugs|passport)|narcotics\s*parcel|rbi\s*verification\s*(?:account|transfer)|safe\s*custody\s*account|stay\s*on\s*(?:skype|whatsapp)?\s*video\s*call|video\s*call\s*(?:cut\s*pann?athinga|disconnect\s*pann?athinga)|arrest\s*pann?uvanga|police\s*case\s*poduvanga)\b|டிஜிட்டல்\s*அரெஸ்ட்|கைது\s*வாரண்ட்|கைது\s*செய்யப்படுவீர்கள்|வழக்கு\s*பதிவு""",
        RegexOption.IGNORE_CASE,
    )
    private val secrecyRegex = Regex(
        """\b(do\s*not\s*(?:tell|inform|disconnect)|don['’]?t\s*(?:tell|inform|disconnect)|keep\s*(?:this\s*)?secret|stay\s*on\s*(?:the\s*)?(?:call|video\s*call)|yaarukkum\s*sollama|cut\s*pann?athinga)\b|யாரிடமும்\s*சொல்ல\s*வேண்டாம்|வீடியோ\s*காலில்\s*இருங்கள்""",
        RegexOption.IGNORE_CASE,
    )
    private val hardThreatRegex = Regex(
        """\b(account\s*(?:will\s*be\s*|is\s*|has\s*been\s*)?(?:blocked|suspended|frozen|closed|deactivated)|sim\s*(?:will\s*be\s*)?blocked|number\s*(?:will\s*be\s*)?disconnected|electricity\s*(?:will\s*be\s*)?disconnected|power\s*(?:cut|disconnected)\s*tonight|legal\s*action\s*(?:will\s*be\s*taken|immediately)|account\s*block\s*(?:aagum|agidum|aidum)|account\s*freeze\s*aagum|current\s*cut\s*aagum|sim\s*block\s*aagum)\b|கணக்கு\s*முடக்கப்படும்|கணக்கு\s*நிறுத்தப்படும்|மின்சாரம்\s*துண்டிக்கப்படும்""",
        RegexOption.IGNORE_CASE,
    )
    private val urgencyRegex = Regex(
        """\b(within\s*\d+\s*(?:minute|min|hour|hr)s?|immediately|urgent(?:ly)?|act\s*now|last\s*chance|expire(?:s|d)?\s*soon|today\s*only|right\s*now|final\s*(?:warning|notice)|10\s*minutes|udane|udanadiya|avasaram|nimishathula|seekiram|ippo\s*ve|inraikke)\b|உடனே|உடனடியாக|அவசரம்|நிமிடத்தில்|இன்றே\s*கடைசி""",
        RegexOption.IGNORE_CASE,
    )
    private val politeReminderRegex = Regex(
        """\b(reminder:|bill\s+(?:of\s+[\w₹., ]+\s+)?is\s+due\s+today|pay\s+(?:via|using)\s+your\s+official|log\s*in\s+to\s+your\s+official|update\s+directly\s+from\s+the\s+(?:google\s+)?play\s*store)\b""",
        RegexOption.IGNORE_CASE,
    )

    // --- Upfront fees, lures, payments ---
    private val upfrontFeeRegex = Regex(
        """\b(processing\s*fee|registration\s*fee|advance\s*(?:fee|payment)|verification\s*fee|clearance\s*fee|customs\s*(?:duty|fee)|security\s*deposit|refundable\s*deposit|unlock\s*payment|pay\s*first|pay\s*to\s*release|munpanam|advance\s*panam|processing\s*fee\s*kattunga|fee\s*kattunga)\b|முன்பணம்|கட்டணம்\s*செலுத்த""",
        RegexOption.IGNORE_CASE,
    )
    private val lureRegex = Regex(
        """\b(congratulations|lucky\s*winner|winner|lottery|lucky\s*draw|won\s*(?:a\s*)?(?:prize|lakhs?|crore|car|iphone|voucher)|selected\s*for\s*(?:a\s*)?job|work\s*from\s*home|guaranteed\s*income|daily\s*earning|earn\s*(?:rs\.?|₹)\s*\d+\s*daily|part[\s-]*time\s*task|reward)\b|பரிசு|லொட்டரி|parisu""",
        RegexOption.IGNORE_CASE,
    )
    private val moneyAmountRegex = Regex(
        """(?:₹\s?\d[\d,]*(?:\.\d+)?|\b(?:rs\.?|inr)\s?\d[\d,]*(?:\.\d+)?|\b\d[\d,]*(?:\.\d+)?\s*(?:rupees|lakhs?|crores?|ரூபாய்|லட்சம்|ஆயிரம்))""",
        RegexOption.IGNORE_CASE,
    )
    private val urlRegex = Regex(
        """\b((?:https?://|www\.)[^\s<>()]+|(?:bit\.ly|tinyurl\.com|t\.co|rb\.gy|cutt\.ly|is\.gd|shorturl\.at|wa\.me|t\.me)/[^\s<>()]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val upiIdRegex = Regex("""\b[a-zA-Z0-9._-]{2,}@(?:ok(?:axis|hdfcbank|icici|sbi)|ybl|ibl|axl|paytm|upi|apl|barodampay|sbi|icici|hdfcbank|demobank)\b""", RegexOption.IGNORE_CASE)
    private val phoneRegex = Regex("""(?:\+91[\s-]?\d{5}[\s-]?\d{5}|\b[6-9]\d{9}\b)""")

    // URL structural risk indicators
    private val shortenerHostRegex = Regex("""(?:^|\.)(bit\.ly|tinyurl\.com|t\.co|rb\.gy|cutt\.ly|is\.gd|shorturl\.at|ow\.ly|goo\.gl|tiny\.cc)$""", RegexOption.IGNORE_CASE)
    private val messengerRedirectRegex = Regex("""(?:^|\.)(wa\.me|t\.me|telegram\.me)$""", RegexOption.IGNORE_CASE)
    private val suspiciousTldRegex = Regex("""\.(xyz|top|click|tk|ml|ga|cf|gq|vip|buzz|icu|work|loan|win|invalid|cc|site|online|shop|live)$""", RegexOption.IGNORE_CASE)
    private val ipHostRegex = Regex("""^\d{1,3}(?:\.\d{1,3}){3}$""")
    private val officialIndianDomainRegex = Regex(
        """(?:^|\.)(gov\.in|nic\.in|rbi\.org\.in|npci\.org\.in|sbi\.co\.in|onlinesbi\.sbi|hdfcbank\.com|icicibank\.com|axisbank\.com|kotak\.com|pnbindia\.in|canarabank\.com|cybercrime\.gov\.in|incometax\.gov\.in|uidai\.gov\.in|epfindia\.gov\.in|tnebnet\.org)$""",
        RegexOption.IGNORE_CASE,
    )
    private val brandImpersonationInHostRegex = Regex(
        """(sbi|hdfc|icici|axis|rbi|npci|paytm|phonepe|gpay|kyc|uidai|aadhaar|incometax|customs|cybercrime|tneb|fedex|epfo)""",
        RegexOption.IGNORE_CASE,
    )

    fun analyze(rawMessage: String, tamil: Boolean = false): SafetyAssessment {
        val norm = TextNormalizer.normalize(rawMessage)
        if (norm.normalized.isEmpty()) {
            return SafetyDecisionEngine.evaluate(
                featureName = FEATURE_NAME,
                findings = emptyList(),
                missingInformation = listOf(
                    if (tamil) "பகுப்பாய்வு செய்ய எந்த செய்தியும் உள்ளிடப்படவில்லை." else "No message text was provided.",
                ),
                insufficientContext = true,
                tamil = tamil,
            )
        }

        val text = norm.deobfuscated
        val context = extractContext(norm)
        val findings = mutableListOf<SafetyEvidence>()
        val missingInfo = mutableListOf<String>()

        // ---------------------------------------------------------------------
        // RULE 1: MSG-CREDENTIAL-01 (CRITICAL) — Affirmative credential request
        // ---------------------------------------------------------------------
        if (context.hasAffirmativeCredentialRequest) {
            val spans = buildList {
                addAll(TextNormalizer.findSpans(norm.original, otpRegex))
                addAll(TextNormalizer.findSpans(norm.original, upiPinRegex))
                addAll(TextNormalizer.findSpans(norm.original, passwordCvvRegex))
                addAll(TextNormalizer.findSpans(norm.original, shareActionRegex))
                addAll(TextNormalizer.findSpans(norm.original, enterCredentialActionRegex))
            }.distinct()
            findings += SafetyEvidence(
                ruleId = "MSG-CREDENTIAL-01",
                featureName = FEATURE_NAME,
                categoryId = "credential",
                title = if (tamil) "ரகசிய எண் (OTP / UPI PIN / கடவுச்சொல்) கோரிக்கை" else "Request for secret OTP, UPI PIN, or password",
                description = if (tamil) {
                    "இந்த செய்தி உங்கள் OTP, UPI PIN, கடவுச்சொல் அல்லது கார்டு ரகசியத் தகவலைப் பகிரவோ உள்ளிடவோ கேட்கிறது."
                } else {
                    "This message asks you to share, send, or enter a secret OTP, UPI PIN, password, or card security detail."
                },
                strength = EvidenceStrength.CRITICAL,
                triggerReason = if (tamil) {
                    "ரகசிய தகவல் சொல்லும் பகிர்வு/உள்ளீட்டுக் கோரிக்கையும் ஒரே வாக்கியத்தில் பாதுகாப்பு மறுப்பு இன்றி கண்டறியப்பட்டது."
                } else {
                    "Detected a sensitive credential paired with a request to share or enter it without protective negation."
                },
                exceptionOrLimitation = if (tamil) {
                    "உண்மையான வங்கி அல்லது UPI சேவைகள் ஒருபோதும் OTP, UPI PIN அல்லது கடவுச்சொல்லைக் கேட்காது."
                } else {
                    "Legitimate banks, UPI apps, and government agencies never ask you to share an OTP, UPI PIN, CVV, or password."
                },
                recommendedAction = if (tamil) {
                    "OTP, UPI PIN, கடவுச்சொல் அல்லது CVV-ஐ ஒருபோதும் பகிர வேண்டாம். இந்த செய்தியைப் புறக்கணித்து அதிகாரப்பூர்வ செயலியில் சரிபார்க்கவும்."
                } else {
                    "Never share your OTP, UPI PIN, password, or CVV. Ignore this message and check directly in your official bank or UPI app."
                },
                matchedSpans = spans,
            )
        }

        // ---------------------------------------------------------------------
        // RULE 2: MSG-RECEIVE-PAYMENT-01 (CRITICAL) — UPI PIN / QR to receive money
        // ---------------------------------------------------------------------
        val asksPinOrQrToReceive =
            RequestedAction.RECEIVE_MONEY_CLAIM in context.requestedActions &&
                (
                    SensitiveDataType.UPI_PIN in context.sensitiveDataMentions ||
                        RequestedAction.SCAN_QR in context.requestedActions ||
                        (RequestedAction.ENTER_CREDENTIAL in context.requestedActions && !context.hasProtectiveCredentialWarning)
                    ) &&
                !passiveRefundConfirmationRegex.containsMatchIn(text)

        if (asksPinOrQrToReceive) {
            val spans = (
                TextNormalizer.findSpans(norm.original, receiveClaimRegex) +
                    TextNormalizer.findSpans(norm.original, scanQrRegex) +
                    TextNormalizer.findSpans(norm.original, upiPinRegex)
                ).distinct()
            findings += SafetyEvidence(
                ruleId = "MSG-RECEIVE-PAYMENT-01",
                featureName = FEATURE_NAME,
                categoryId = "refund",
                title = if (tamil) "பணம் பெற UPI PIN / QR ஸ்கேன் ஏமாற்று" else "Instructs UPI PIN or QR scan to receive money",
                description = if (tamil) {
                    "பணம் அல்லது திரும்பப் பணம் (refund) பெற QR ஸ்கேன் செய்யவோ UPI PIN உள்ளிடவோ சொல்கிறது. UPI PIN பணம் அனுப்ப மட்டுமே பயன்படும்."
                } else {
                    "Claims you must scan a QR code or enter a UPI PIN to receive money or a refund. A UPI PIN only authorizes outgoing payments."
                },
                strength = EvidenceStrength.CRITICAL,
                triggerReason = if (tamil) {
                    "பணம் பெறுவதாகக் கூறும் நோக்கமும் QR ஸ்கேன் / UPI PIN உள்ளீட்டு கோரிக்கையும் முரண்படுகின்றன."
                } else {
                    "Contradiction detected between claimed purpose (receiving money/refund) and requested outgoing-payment action (QR scan or UPI PIN)."
                },
                exceptionOrLimitation = if (tamil) {
                    "வங்கி கணக்கில் பணம் பெற எந்த சூழ்நிலையிலும் UPI PIN உள்ளிடத் தேவையில்லை."
                } else {
                    "There is no valid UPI flow that requires entering your UPI PIN to receive money."
                },
                recommendedAction = if (tamil) {
                    "QR குறியீட்டை ஸ்கேன் செய்யவோ UPI PIN உள்ளிடவோ வேண்டாம். பணம் பெற PIN தேவையில்லை."
                } else {
                    "Do not scan the QR code or enter your UPI PIN. You never need a UPI PIN to receive money."
                },
                matchedSpans = spans,
            )
        }

        // ---------------------------------------------------------------------
        // RULE 3: MSG-REMOTE-ACCESS-01 (CRITICAL) — Unknown APK / remote-access app
        // ---------------------------------------------------------------------
        if (context.hasDeviceAccessRequest) {
            val spans = (
                TextNormalizer.findSpans(norm.original, remoteAccessRegex) +
                    TextNormalizer.findSpans(norm.original, apkInstallRegex)
                ).distinct()
            findings += SafetyEvidence(
                ruleId = "MSG-REMOTE-ACCESS-01",
                featureName = FEATURE_NAME,
                categoryId = "remote_access",
                title = if (tamil) "APK கோப்பு அல்லது தொலைநிலைக் கட்டுப்பாட்டு செயலி கோரிக்கை" else "Request to install APK or remote-access tool",
                description = if (tamil) {
                    "அறியாத .apk கோப்பை நிறுவவோ, AnyDesk/TeamViewer போன்ற திரை பகிர்வு செயலியை இயக்கவோ கேட்கிறது. இது உங்கள் சாதனத்தையும் வங்கி செயலிகளையும் முழுமையாகக் கட்டுப்படுத்த அனுமதிக்கும்."
                } else {
                    "Asks you to install an unknown APK file or remote-access/screen-sharing app (e.g., AnyDesk, TeamViewer), which can let a scammer control your phone and read OTPs."
                },
                strength = EvidenceStrength.CRITICAL,
                triggerReason = if (tamil) {
                    "APK பதிவிறக்கம் அல்லது திரை பகிர்வு / தொலைநிலை செயலி கோரிக்கை கண்டறியப்பட்டது."
                } else {
                    "Detected instruction to sideload an APK, share screen, or install remote-control software."
                },
                exceptionOrLimitation = if (tamil) {
                    "நிறுவன IT உதவி மட்டுமே பணியிட சாதனங்களில் தொலைநிலை செயலியைப் பயன்படுத்தும்; வங்கிகள் ஒருபோதும் இவற்றை நிறுவச் சொல்லாது."
                } else {
                    "Corporate IT teams may use remote tools on work devices, but banks, UPI support, and government agencies never ask citizens to install APKs or remote-access apps."
                },
                recommendedAction = if (tamil) {
                    "எந்த APK கோப்பையோ திரை பகிர்வு செயலியையோ நிறுவ வேண்டாம். ஏற்கனவே நிறுவியிருந்தால் உடனே நீக்கிவிட்டு இணையத்தைத் துண்டிக்கவும்."
                } else {
                    "Do not install any APK file or remote-access app. Only install or update banking apps directly from the official Google Play Store."
                },
                matchedSpans = spans,
            )
        }

        // ---------------------------------------------------------------------
        // RULE 4: MSG-AUTHORITY-THREAT-01 (CRITICAL) — Digital arrest / police threat
        // ---------------------------------------------------------------------
        val isAuthorityThreat =
            context.hasDigitalArrestCue ||
                (
                    ClaimedIdentity.POLICE_COURT in context.claimedIdentities &&
                        !context.hasProtectiveCredentialWarning &&
                        (
                            context.hasHardThreat ||
                                context.hasSecrecyInstruction ||
                                RequestedAction.PAY_OR_TRANSFER in context.requestedActions ||
                                context.hasAffirmativeCredentialRequest
                            )
                    )

        if (isAuthorityThreat) {
            val spans = (
                TextNormalizer.findSpans(norm.original, policeIdentityRegex) +
                    TextNormalizer.findSpans(norm.original, digitalArrestRegex) +
                    TextNormalizer.findSpans(norm.original, secrecyRegex)
                ).distinct()
            findings += SafetyEvidence(
                ruleId = "MSG-AUTHORITY-THREAT-01",
                featureName = FEATURE_NAME,
                categoryId = "authority",
                title = if (tamil) "டிஜிட்டல் அரெஸ்ட் / காவல்துறை மிரட்டல் அறிகுறி" else "Digital-arrest or police impersonation threat",
                description = if (tamil) {
                    "காவல்துறை, CBI, சுங்கம் அல்லது நீதிமன்றம் எனக் கூறி கைது மிரட்டல், ரகசியம் காக்கச் சொல்லுதல் அல்லது பணம் அனுப்ப அழுத்தம் கொடுக்கிறது."
                } else {
                    "Impersonates police, CBI, customs, or courts with threats of arrest, secrecy, or money demands."
                },
                strength = EvidenceStrength.CRITICAL,
                triggerReason = if (tamil) {
                    "காவல்துறை/அதிகாரி ஆள்மாறாட்டத்துடன் கைது மிரட்டல், வீடியோ கால் நிர்ப்பந்தம் அல்லது பணக் கோரிக்கை இணைந்துள்ளது."
                } else {
                    "Combined law-enforcement/court claim with arrest/legal threat, secrecy pressure, or payment/credential demand."
                },
                exceptionOrLimitation = if (tamil) {
                    "இந்திய காவல்துறை அல்லது நீதிமன்றம் ஒருபோதும் தொலைபேசி/வீடியோ காலில் 'டிஜிட்டல் அரெஸ்ட்' செய்யாது அல்லது சரிபார்ப்புக்கு பணம் கேட்காது."
                } else {
                    "Indian police, CBI, RBI, and courts never place citizens under 'digital arrest' or ask for money transfers to verify accounts."
                },
                recommendedAction = if (tamil) {
                    "அழைப்பை உடனே துண்டித்து பணம் அனுப்பாமல் இருங்கள். குடும்பத்தினரிடம் பேசி 1930 அல்லது அருகிலுள்ள காவல் நிலையத்தில் சரிபார்க்கவும்."
                } else {
                    "Disconnect immediately and do not transfer money. Talk to a trusted family member and report on 1930 or cybercrime.gov.in."
                },
                matchedSpans = spans,
            )
        }

        // ---------------------------------------------------------------------
        // RULE 5: MSG-REFUND-DECEPTION-01 (CRITICAL/STRONG) — Refund/prize asks payment or link
        // ---------------------------------------------------------------------
        val hasRefundDeception =
            RequestedAction.RECEIVE_MONEY_CLAIM in context.requestedActions &&
                !passiveRefundConfirmationRegex.containsMatchIn(text) &&
                !asksPinOrQrToReceive &&
                (
                    RequestedAction.PAY_OR_TRANSFER in context.requestedActions ||
                        context.hasUpfrontFeeRequest ||
                        context.suspiciousUrls.isNotEmpty() ||
                        context.hasAffirmativeCredentialRequest
                    )

        if (hasRefundDeception) {
            val spans = (
                TextNormalizer.findSpans(norm.original, receiveClaimRegex) +
                    TextNormalizer.findSpans(norm.original, upfrontFeeRegex) +
                    TextNormalizer.findSpans(norm.original, payTransferRegex)
                ).distinct()
            val isCriticalDeception =
                RequestedAction.PAY_OR_TRANSFER in context.requestedActions ||
                    context.hasUpfrontFeeRequest ||
                    context.hasAffirmativeCredentialRequest
            findings += SafetyEvidence(
                ruleId = "MSG-REFUND-DECEPTION-01",
                featureName = FEATURE_NAME,
                categoryId = "refund",
                title = if (tamil) "பணம் திரும்பப் பெறும் / பரிசு ஏமாற்று முரண்பாடு" else "Refund or reward message asks you to pay or verify via link",
                description = if (tamil) {
                    "பணம் திரும்பப் பெறவோ பரிசு பெறவோ முன்பணம் செலுத்த, கட்டணத்தை ஏற்க, அல்லது சந்தேகமான இணைப்பைத் திறக்கச் சொல்கிறது."
                } else {
                    "Promises a refund, cashback, or reward while asking you to send money, pay a fee, or act through an unverified link."
                },
                strength = if (isCriticalDeception) EvidenceStrength.CRITICAL else EvidenceStrength.STRONG,
                triggerReason = if (tamil) {
                    "பணம் பெறுவதாகக் கூறி அதற்கு மாறாக பணம்/கட்டணம் அல்லது சந்தேகமான இணைப்பு நடவடிக்கை கேட்கப்படுகிறது."
                } else {
                    "Detected receive-money/refund promise paired with a conflicting outgoing payment, fee, or suspicious link."
                },
                exceptionOrLimitation = if (tamil) {
                    "உண்மையான திரும்பப் பணம் தானாகவே உங்கள் அசல் கட்டண கணக்கிற்கு வரவு வைக்கப்படும்."
                } else {
                    "Genuine merchant refunds credit automatically to your original payment method without extra payments or external links."
                },
                recommendedAction = if (tamil) {
                    "திரும்பப் பணம் பெற எந்த கட்டணமும் செலுத்த வேண்டாம். அசல் வணிகர் செயலியில் நேரடியாக நிலையைச் சரிபார்க்கவும்."
                } else {
                    "Do not pay any fee or tap links to claim a refund. Check the status directly inside the original merchant app."
                },
                matchedSpans = spans,
            )
        }

        // ---------------------------------------------------------------------
        // RULE 6: MSG-IMPERSONATION-ACTION-01 (CRITICAL/STRONG) — Authority + risky action
        // ---------------------------------------------------------------------
        val nonFriendAuthority = context.claimedIdentities.any {
            it == ClaimedIdentity.BANK ||
                it == ClaimedIdentity.GOVERNMENT_UTILITY ||
                it == ClaimedIdentity.COURIER ||
                it == ClaimedIdentity.MERCHANT_SUPPORT
        }
        val hasHighRiskImpersonationAction =
            nonFriendAuthority &&
                !context.hasProtectiveCredentialWarning &&
                !politeReminderRegex.containsMatchIn(text) &&
                (
                    context.hasAffirmativeCredentialRequest ||
                        context.hasDeviceAccessRequest ||
                        context.hasUpfrontFeeRequest ||
                        (context.hasHardThreat && (context.extractedUrls.isNotEmpty() || context.extractedPhones.isNotEmpty() || RequestedAction.VERIFY_ACCOUNT in context.requestedActions)) ||
                        (RequestedAction.VERIFY_ACCOUNT in context.requestedActions && context.suspiciousUrls.isNotEmpty())
                    )

        if (hasHighRiskImpersonationAction) {
            val spans = (
                TextNormalizer.findSpans(norm.original, bankIdentityRegex) +
                    TextNormalizer.findSpans(norm.original, govtUtilityIdentityRegex) +
                    TextNormalizer.findSpans(norm.original, courierIdentityRegex)
                ).distinct()
            val strength = if (context.hasAffirmativeCredentialRequest || context.hasDeviceAccessRequest) {
                EvidenceStrength.STRONG
            } else if (context.hasHardThreat || context.hasUpfrontFeeRequest) {
                EvidenceStrength.STRONG
            } else {
                EvidenceStrength.MEDIUM
            }
            findings += SafetyEvidence(
                ruleId = "MSG-IMPERSONATION-ACTION-01",
                featureName = FEATURE_NAME,
                categoryId = "authority",
                title = if (tamil) "நிறுவன ஆள்மாறாட்டத்துடன் அபாய கோரிக்கை" else "Organization impersonation paired with risky action",
                description = if (tamil) {
                    "வங்கி, அரசு, கூரியர் அல்லது சேவை நிறுவனம் எனக் கூறி அவசர கணக்கு சரிபார்ப்பு, கட்டணம், இணைப்பு அல்லது ரகசியத் தகவல் கேட்கிறது."
                } else {
                    "Claims to represent a bank, utility, government body, or courier while pushing an urgent account action, fee, or unverified link."
                },
                strength = strength,
                triggerReason = if (tamil) {
                    "அதிகாரப்பூர்வ நிறுவனப் பெயருடன் மிரட்டல், சரிபார்ப்பு இணைப்பு அல்லது கட்டண கோரிக்கை சேர்ந்துள்ளது."
                } else {
                    "Claimed institutional identity combined with threats, suspicious links, upfront fees, or credential/app demands."
                },
                exceptionOrLimitation = if (tamil) {
                    "உண்மையான நிறுவனங்களும் அறிவிப்புகள் அனுப்பலாம்; ஆனால் எப்போதும் அதிகாரப்பூர்வ செயலி அல்லது தெரிந்த எண்ணில் சரிபார்க்க வேண்டும்."
                } else {
                    "Real organizations send alerts too, so verify directly through the official app or website you open yourself—never via links or numbers inside the message."
                },
                recommendedAction = if (tamil) {
                    "செய்தியில் உள்ள இணைப்பு அல்லது எண்ணைப் பயன்படுத்தாமல், நிறுவனத்தின் அதிகாரப்பூர்வ செயலி அல்லது இணையதளத்தை நீங்களே திறந்து சரிபார்க்கவும்."
                } else {
                    "Do not use links or phone numbers from this message. Open the organization's official app or call the number on the back of your card."
                },
                matchedSpans = spans,
            )
        } else if (
            (nonFriendAuthority || ClaimedIdentity.POLICE_COURT in context.claimedIdentities) &&
            !isAuthorityThreat &&
            !context.hasProtectiveCredentialWarning &&
            !politeReminderRegex.containsMatchIn(text) &&
            !passiveRefundConfirmationRegex.containsMatchIn(text) &&
            (
                context.suspiciousUrls.isNotEmpty() ||
                    context.hasUrgencyPressure ||
                    context.hasHardThreat ||
                    context.hasUpfrontFeeRequest ||
                    context.extractedPhones.isNotEmpty()
                )
        ) {
            // Supporting authority cue ONLY when paired with another active cue (not bare "I went to the bank")
            val spans = (
                TextNormalizer.findSpans(norm.original, bankIdentityRegex) +
                    TextNormalizer.findSpans(norm.original, policeIdentityRegex) +
                    TextNormalizer.findSpans(norm.original, govtUtilityIdentityRegex)
                ).distinct()
            findings += SafetyEvidence(
                ruleId = "MSG-AUTHORITY-MENTION-01",
                featureName = FEATURE_NAME,
                categoryId = "authority",
                title = if (tamil) "நிறுவனப் பெயர் சரிபார்ப்பு தேவை" else "Claimed organization needs verification",
                description = if (tamil) {
                    "வங்கி, அரசு அல்லது சேவை நிறுவனம் சார்பாக வந்ததாகக் கூறுகிறது. செய்தியில் உள்ள எண்ணையோ இணைப்பையோ நம்ப வேண்டாம்."
                } else {
                    "References a bank, authority, or service alongside an urgent request or contact detail."
                },
                strength = EvidenceStrength.WEAK,
                triggerReason = if (tamil) {
                    "அதிகாரப்பூர்வ நிறுவனப் பெயருடன் அவசர அல்லது தொடர்பு கோரிக்கை உள்ளது."
                } else {
                    "Institutional reference appeared alongside pressure or unverified contact details."
                },
                exceptionOrLimitation = if (tamil) {
                    "நிறுவனப் பெயரைக் குறிப்பிடுவது மட்டும் மோசடி அல்ல."
                } else {
                    "Mentioning a bank or agency by itself does not prove fraud."
                },
                recommendedAction = if (tamil) {
                    "நிறுவனத்தின் அதிகாரப்பூர்வ செயலியில் தனியாக சரிபார்க்கவும்."
                } else {
                    "Verify independently using the official app or known customer-care number."
                },
                matchedSpans = spans,
            )
        }

        // ---------------------------------------------------------------------
        // RULE 7: MSG-SUSPICIOUS-LINK-01 (STRONG/MEDIUM/WEAK) — Suspicious URLs or contacts
        // ---------------------------------------------------------------------
        val hasFinancialContext =
            context.hasMoneyAmount ||
                context.claimedIdentities.any { it != ClaimedIdentity.FRIEND_FAMILY } ||
                RequestedAction.PAY_OR_TRANSFER in context.requestedActions ||
                RequestedAction.VERIFY_ACCOUNT in context.requestedActions ||
                RequestedAction.RECEIVE_MONEY_CLAIM in context.requestedActions ||
                context.hasUpfrontFeeRequest ||
                context.hasLureCue

        val unverifiedUrls = context.extractedUrls.filterNot { it in context.officialUrls }
        if (context.suspiciousUrls.isNotEmpty()) {
            val strength = if (hasFinancialContext || context.hasUrgencyPressure || context.hasHardThreat) {
                EvidenceStrength.STRONG
            } else {
                EvidenceStrength.MEDIUM
            }
            findings += SafetyEvidence(
                ruleId = "MSG-SUSPICIOUS-LINK-01",
                featureName = FEATURE_NAME,
                categoryId = "link",
                title = if (tamil) "சந்தேகமான அல்லது சுருக்கப்பட்ட இணைப்பு" else "Suspicious or unverified link",
                description = if (tamil) {
                    "இந்த செய்தியில் சுருக்கப்பட்ட இணைப்பு, சந்தேகமான டொமைன், அல்லது WhatsApp/Telegram திசைதிருப்பல் உள்ளது."
                } else {
                    "Contains a shortened URL, suspicious domain/TLD, insecure HTTP link, lookalike host, or messaging redirect."
                },
                strength = strength,
                triggerReason = if (tamil) {
                    "அடையாளம் மறைக்கப்பட்ட அல்லது அதிகாரப்பூர்வமற்ற இணைய முகவரி கண்டறியப்பட்டது: ${context.suspiciousUrls.first()}."
                } else {
                    "Detected high-risk URL pattern (${context.suspiciousUrls.first()}) rather than a verified official domain."
                },
                exceptionOrLimitation = if (tamil) {
                    "சில சட்டபூர்வ நிறுவனங்களும் சுருக்கப்பட்ட இணைப்புகளைப் பயன்படுத்தலாம்; ஆனால் நிதி அல்லது KYC கோரிக்கைகளுக்கு அவற்றைத் திறக்க வேண்டாம்."
                } else {
                    "Some legitimate businesses use URL shorteners, but you should never enter banking or KYC details via shortened or unfamiliar links."
                },
                recommendedAction = if (tamil) {
                    "இந்த இணைப்பைத் தட்ட வேண்டாம். அதிகாரப்பூர்வ இணையதளத்தை உலாவியில் நீங்களே தட்டச்சு செய்யவும்."
                } else {
                    "Do not tap this link. Type the official website address yourself or open the official app."
                },
                matchedSpans = (context.suspiciousUrls + context.extractedPhones).distinct(),
            )
        } else if (unverifiedUrls.isNotEmpty() || (context.extractedPhones.isNotEmpty() && (hasFinancialContext || context.hasHardThreat || context.hasUrgencyPressure))) {
            val strength = if (hasFinancialContext && (context.hasUrgencyPressure || context.hasHardThreat)) {
                EvidenceStrength.MEDIUM
            } else {
                EvidenceStrength.WEAK
            }
            findings += SafetyEvidence(
                ruleId = "MSG-SUSPICIOUS-LINK-01",
                featureName = FEATURE_NAME,
                categoryId = "link",
                title = if (tamil) "சரிபார்க்கப்படாத இணைப்பு அல்லது தொலைபேசி எண்" else "Unverified link or phone number",
                description = if (tamil) {
                    "செய்தியில் உள்ள இணைப்பு அல்லது தொலைபேசி எண்ணை நேரடியாகப் பயன்படுத்துவதற்கு முன் அதிகாரப்பூர்வமாக சரிபார்க்கவும்."
                } else {
                    "Includes an external link or phone number supplied inside the message that has not been verified online."
                },
                strength = strength,
                triggerReason = if (tamil) {
                    "வெளிப்புற இணைப்பு அல்லது தனிப்பட்ட தொலைபேசி எண் கண்டறியப்பட்டது."
                } else {
                    "Detected an unverified URL or direct callback number inside the message."
                },
                exceptionOrLimitation = if (tamil) {
                    "உள்ளூர் சோதனையால் இணைப்பு பாதுகாப்பானதா என்பதை இணையத்தில் சரிபார்க்க முடியாது."
                } else {
                    "Offline analysis cannot inspect the destination webpage or verify who owns the phone number."
                },
                recommendedAction = if (tamil) {
                    "செய்தியில் கொடுக்கப்பட்ட எண்ணையோ இணைப்பையோ பயன்படுத்தாமல் அதிகாரப்பூர்வ எண்ணில் தொடர்பு கொள்ளவும்."
                } else {
                    "Avoid calling numbers or opening links from unexpected messages; look up the official contact independently."
                },
                matchedSpans = (unverifiedUrls + context.extractedPhones).distinct(),
            )
        }

        // ---------------------------------------------------------------------
        // RULE 8: MSG-UPFRONT-FEE-01 (STRONG/MEDIUM) — Advance/processing fee demand
        // ---------------------------------------------------------------------
        if (context.hasUpfrontFeeRequest) {
            val strength = if (
                context.hasLureCue ||
                RequestedAction.RECEIVE_MONEY_CLAIM in context.requestedActions ||
                ClaimedIdentity.EMPLOYER_JOB in context.claimedIdentities ||
                ClaimedIdentity.COURIER in context.claimedIdentities
            ) {
                EvidenceStrength.STRONG
            } else {
                EvidenceStrength.MEDIUM
            }
            findings += SafetyEvidence(
                ruleId = "MSG-UPFRONT-FEE-01",
                featureName = FEATURE_NAME,
                categoryId = "fee",
                title = if (tamil) "முன்பணம் / செயலாக்கக் கட்டண கோரிக்கை" else "Upfront or processing fee requested",
                description = if (tamil) {
                    "பரிசு, வேலை, கடன், பார்சல் அல்லது திரும்பப் பணத்திற்கு முன்பே கட்டணம் செலுத்தக் கேட்கிறது."
                } else {
                    "Asks for an advance, processing, registration, or clearance fee before releasing a benefit, job, parcel, or refund."
                },
                strength = strength,
                triggerReason = if (tamil) {
                    "முன்பணம் அல்லது செயலாக்கக் கட்டண வார்த்தைகள் கண்டறியப்பட்டன."
                } else {
                    "Detected advance-fee or processing-fee wording linked to a pending release or reward."
                },
                exceptionOrLimitation = if (tamil) {
                    "சில சேவைகளுக்கு அதிகாரப்பூர்வ கட்டணம் இருக்கலாம்; ஆனால் தனிப்பட்ட UPI அல்லது உடனடி பரிமாற்றம் மூலம் முன்பணம் கேட்பது பொதுவான மோசடி."
                } else {
                    "Official services may charge documented fees through official portals, never via urgent personal transfers to unlock refunds, jobs, or prizes."
                },
                recommendedAction = if (tamil) {
                    "பரிசு, வேலை அல்லது திரும்பப் பணத்தைத் திறக்க எந்த முன்பணமும் செலுத்த வேண்டாம்."
                } else {
                    "Do not pay upfront fees to unlock a refund, job offer, parcel, or prize."
                },
                matchedSpans = TextNormalizer.findSpans(norm.original, upfrontFeeRegex),
            )
        }

        // ---------------------------------------------------------------------
        // RULE 9: MSG-PRESSURE-01 (MEDIUM/WEAK) — Threats & artificial urgency
        // ---------------------------------------------------------------------
        val isBenignPoliteDeadline = politeReminderRegex.containsMatchIn(text) &&
            !context.hasHardThreat &&
            !context.hasAffirmativeCredentialRequest &&
            context.suspiciousUrls.isEmpty() &&
            !context.hasDeviceAccessRequest

        if (!isBenignPoliteDeadline && (context.hasHardThreat || context.hasUrgencyPressure)) {
            val strength = when {
                context.hasHardThreat && (context.hasUrgencyPressure || hasFinancialContext || context.extractedUrls.isNotEmpty()) ->
                    EvidenceStrength.MEDIUM
                context.hasHardThreat -> EvidenceStrength.MEDIUM
                context.hasUrgencyPressure && (
                    context.hasAffirmativeCredentialRequest ||
                        context.suspiciousUrls.isNotEmpty() ||
                        RequestedAction.PAY_OR_TRANSFER in context.requestedActions ||
                        RequestedAction.SCAN_QR in context.requestedActions ||
                        RequestedAction.VERIFY_ACCOUNT in context.requestedActions
                    ) -> EvidenceStrength.MEDIUM
                else -> EvidenceStrength.WEAK
            }
            val spans = (
                TextNormalizer.findSpans(norm.original, hardThreatRegex) +
                    TextNormalizer.findSpans(norm.original, urgencyRegex)
                ).distinct()
            findings += SafetyEvidence(
                ruleId = "MSG-PRESSURE-01",
                featureName = FEATURE_NAME,
                categoryId = "urgency",
                title = if (tamil) "அவசரம் அல்லது கணக்கு முடக்க மிரட்டல்" else "Urgency pressure or account-block threat",
                description = if (tamil) {
                    "தனியாக சரிபார்க்க நேரம் தராமல் உடனடியாக செயல்பட அழுத்தம் அல்லது கணக்கு முடக்கப்படும் என்ற மிரட்டல் உள்ளது."
                } else {
                    "Creates time pressure or threatens account/service suspension to rush you before you can verify."
                },
                strength = strength,
                triggerReason = if (tamil) {
                    "உடனடி காலக்கெடு அல்லது கணக்கு/சேவை நிறுத்த மிரட்டல் கண்டறியப்பட்டது."
                } else {
                    "Detected coercive deadline language or threat of account/utility suspension."
                },
                exceptionOrLimitation = if (tamil) {
                    "உண்மையான கட்டண நினைவூட்டல்களிலும் கடைசி தேதி இருக்கலாம்; ஆனால் அவை OTP/PIN அல்லது சந்தேகமான இணைப்புகளைக் கேட்காது."
                } else {
                    "Legitimate bill reminders can mention due dates, so urgency alone does not prove a scam."
                },
                recommendedAction = if (tamil) {
                    "அவசரப்பட்டு எந்த முடிவும் எடுக்க வேண்டாம். நிதானமாக அதிகாரப்பூர்வ செயலியில் சரிபார்க்கவும்."
                } else {
                    "Pause and take your time. Check your account status directly inside the official app."
                },
                matchedSpans = spans,
            )
        }

        // ---------------------------------------------------------------------
        // RULE 10: MSG-LURE-01 (MEDIUM/WEAK) — Prize, lottery, or easy-money lure
        // ---------------------------------------------------------------------
        if (context.hasLureCue) {
            val strength = if (
                context.hasUpfrontFeeRequest ||
                context.suspiciousUrls.isNotEmpty() ||
                RequestedAction.PAY_OR_TRANSFER in context.requestedActions ||
                RequestedAction.SCAN_QR in context.requestedActions
            ) {
                EvidenceStrength.MEDIUM
            } else {
                EvidenceStrength.WEAK
            }
            findings += SafetyEvidence(
                ruleId = "MSG-LURE-01",
                featureName = FEATURE_NAME,
                categoryId = "lure",
                title = if (tamil) "பரிசு, லொட்டரி அல்லது எளிய வருமான ஆசை" else "Unexpected prize, lottery, or easy-income lure",
                description = if (tamil) {
                    "எதிர்பாராத பரிசு, லொட்டரி அல்லது தினசரி உத்தரவாத வருமான சலுகை காட்டப்படுகிறது."
                } else {
                    "Uses an unexpected prize, reward, or guaranteed work-from-home income offer to draw you into acting."
                },
                strength = strength,
                triggerReason = if (tamil) {
                    "பரிசு/வெற்றி/உத்தரவாத வருமான வார்த்தைகள் கண்டறியப்பட்டன."
                } else {
                    "Detected prize, lottery, or guaranteed-earning lure phrasing."
                },
                exceptionOrLimitation = if (tamil) {
                    "நீங்கள் பங்கேற்ற அதிகாரப்பூர்வ போட்டியாகவும் இருக்கலாம்; ஆனால் பணம் அல்லது PIN கேட்டால் அது மோசடி."
                } else {
                    "Promotional messages from known retailers can use congratulatory words; evaluate whether money, links, or credentials are demanded."
                },
                recommendedAction = if (tamil) {
                    "நீங்கள் பங்கேற்காத பரிசு அல்லது அதிக வருமான சலுகைகளை நம்பி பணமோ தகவலோ தர வேண்டாம்."
                } else {
                    "Verify any reward directly on the brand's official app or store without paying fees or tapping unknown links."
                },
                matchedSpans = TextNormalizer.findSpans(norm.original, lureRegex),
            )
        }

        // ---------------------------------------------------------------------
        // POSITIVE / INFORMATIONAL SIGNAL: Genuine protective OTP notification
        // ---------------------------------------------------------------------
        val isGenuineProtectiveOtp =
            SensitiveDataType.OTP in context.sensitiveDataMentions &&
                !context.hasAffirmativeCredentialRequest &&
                (context.hasProtectiveCredentialWarning || passiveOtpNotificationRegex.containsMatchIn(text)) &&
                context.suspiciousUrls.isEmpty() &&
                !context.hasDeviceAccessRequest &&
                !isAuthorityThreat

        if (isGenuineProtectiveOtp) {
            findings += SafetyEvidence(
                ruleId = "MSG-INFO-PROTECTIVE-OTP",
                featureName = FEATURE_NAME,
                categoryId = "context",
                title = if (tamil) "பாதுகாப்பு எச்சரிக்கையுடன் கூடிய சாதாரண OTP அறிவிப்பு" else "Standard OTP notice with do-not-share warning",
                description = if (tamil) {
                    "இந்த செய்தி OTP-ஐப் பகிரக் கேட்காமல், அதைப் பகிர வேண்டாம் என எச்சரிக்கும் வழக்கமான அறிவிப்பாகத் தெரிகிறது."
                } else {
                    "Reads like a standard one-time password notification warning you not to share the code, rather than requesting it."
                },
                strength = EvidenceStrength.INFORMATIONAL,
                polarity = EvidencePolarity.POSITIVE,
                triggerReason = if (tamil) {
                    "OTP குறிப்புடன் 'பகிர வேண்டாம்' என்ற பாதுகாப்பு எச்சரிக்கை உள்ளது; ரகசிய எண் கோரிக்கை இல்லை."
                } else {
                    "OTP reference is accompanied by protective non-disclosure phrasing and no request to share it."
                },
                exceptionOrLimitation = if (tamil) {
                    "நீங்கள் கோராத OTP வந்தால் உங்கள் கணக்கின் கடவுச்சொல்லை உடனே மாற்றி சரிபார்க்கவும்."
                } else {
                    "If you did not initiate this login or transaction, do not share the OTP and immediately check your account security."
                },
                recommendedAction = if (tamil) {
                    "இந்த OTP-ஐ நீங்களே கோரியிருந்தால் மட்டும் பயன்படுத்தவும்; யாருடனும் பகிர வேண்டாம்."
                } else {
                    "Use this OTP only if you initiated the request yourself, and never read or forward it to anyone."
                },
            )
        }

        // ---------------------------------------------------------------------
        // INSUFFICIENT INFORMATION CHECK
        // ---------------------------------------------------------------------
        val riskFindings = findings.filter { it.polarity == EvidencePolarity.RISK }
        val isFragmentaryInput = riskFindings.isEmpty() &&
            !isGenuineProtectiveOtp &&
            (norm.wordCount < 4 || norm.meaningfulCharCount < 15)

        if (isFragmentaryInput) {
            missingInfo += if (tamil) {
                "செய்தி மிகவும் சுருக்கமாக உள்ளது. முழு செய்தி, அனுப்பியவர் மற்றும் கேட்கப்படும் நடவடிக்கையை ஒட்டவும்."
            } else {
                "The text is too short to determine sender intent. Paste the full message including any request, link, or contact details."
            }
        }
        if (context.extractedUrls.isNotEmpty()) {
            missingInfo += if (tamil) {
                "இணைப்பு சேரும் இணையதளத்தை ஆன்லைனில் சரிபார்க்கவில்லை (உள்ளூர் சோதனை மட்டும்)."
            } else {
                "Destination webpage reputation was not checked online (offline pattern analysis only)."
            }
        }

        val hasContradictorySignals =
            context.hasProtectiveCredentialWarning && context.hasAffirmativeCredentialRequest

        return SafetyDecisionEngine.evaluate(
            featureName = FEATURE_NAME,
            findings = findings,
            missingInformation = missingInfo,
            insufficientContext = isFragmentaryInput,
            hasContradictorySignals = hasContradictorySignals,
            onlineVerificationPerformed = false,
            tamil = tamil,
        )
    }

    fun extractContext(norm: NormalizedMessage): MessageContext {
        val text = norm.deobfuscated
        val identities = buildSet {
            if (bankIdentityRegex.containsMatchIn(text)) add(ClaimedIdentity.BANK)
            if (policeIdentityRegex.containsMatchIn(text)) add(ClaimedIdentity.POLICE_COURT)
            if (govtUtilityIdentityRegex.containsMatchIn(text)) add(ClaimedIdentity.GOVERNMENT_UTILITY)
            if (courierIdentityRegex.containsMatchIn(text)) add(ClaimedIdentity.COURIER)
            if (employerIdentityRegex.containsMatchIn(text)) add(ClaimedIdentity.EMPLOYER_JOB)
            if (merchantIdentityRegex.containsMatchIn(text)) add(ClaimedIdentity.MERCHANT_SUPPORT)
            if (friendIdentityRegex.containsMatchIn(text)) add(ClaimedIdentity.FRIEND_FAMILY)
        }

        val actions = buildSet {
            if (shareActionRegex.containsMatchIn(text)) add(RequestedAction.SHARE_DISCLOSE)
            if (enterCredentialActionRegex.containsMatchIn(text)) add(RequestedAction.ENTER_CREDENTIAL)
            if (clickLinkRegex.containsMatchIn(text)) add(RequestedAction.CLICK_LINK)
            if (payTransferRegex.containsMatchIn(text)) add(RequestedAction.PAY_OR_TRANSFER)
            if (scanQrRegex.containsMatchIn(text)) add(RequestedAction.SCAN_QR)
            if (apkInstallRegex.containsMatchIn(text) || remoteAccessRegex.containsMatchIn(text)) {
                add(RequestedAction.INSTALL_APP)
            }
            if (callActionRegex.containsMatchIn(text)) add(RequestedAction.CALL_CONTACT)
            if (verifyAccountRegex.containsMatchIn(text)) add(RequestedAction.VERIFY_ACCOUNT)
            if (receiveClaimRegex.containsMatchIn(text)) add(RequestedAction.RECEIVE_MONEY_CLAIM)
        }

        val sensitive = buildSet {
            if (otpRegex.containsMatchIn(text)) add(SensitiveDataType.OTP)
            if (upiPinRegex.containsMatchIn(text)) add(SensitiveDataType.UPI_PIN)
            if (passwordCvvRegex.containsMatchIn(text)) add(SensitiveDataType.PASSWORD_CVV_CARD)
        }

        // Clause-level negation & intent evaluation:
        // Distinguish "Your OTP is 482913. Do not share this OTP with anyone."
        // from "Kindly share that OTP now to complete KYC."
        var hasAffirmativeCredRequest = false
        var hasProtectiveWarning = false
        for (clause in norm.clauses) {
            val clauseHasCred = otpRegex.containsMatchIn(clause) ||
                upiPinRegex.containsMatchIn(clause) ||
                passwordCvvRegex.containsMatchIn(clause)
            val clauseHasNegation = protectiveNegationRegex.containsMatchIn(clause)
            val clauseHasAskVerb = shareActionRegex.containsMatchIn(clause) ||
                enterCredentialActionRegex.containsMatchIn(clause)

            if (clauseHasNegation) {
                hasProtectiveWarning = true
            }
            if (clauseHasCred && clauseHasAskVerb && !clauseHasNegation) {
                hasAffirmativeCredRequest = true
            }
        }
        // Also catch cross-clause requests like "Send it now" right after mentioning OTP when there's NO protective negation in the message
        if (!hasAffirmativeCredRequest && !hasProtectiveWarning && sensitive.isNotEmpty()) {
            val hasDirectRequest = (RequestedAction.SHARE_DISCLOSE in actions || RequestedAction.ENTER_CREDENTIAL in actions) &&
                !passiveOtpNotificationRegex.containsMatchIn(text)
            if (hasDirectRequest) {
                hasAffirmativeCredRequest = true
            }
        }

        val urls = urlRegex.findAll(norm.original).map { it.value.trimEnd('.', ',', ';', ')', ']') }.distinct().toList()
        val officialUrls = urls.filter { isOfficialIndianUrl(it) }
        val suspiciousUrls = urls.filter { isSuspiciousUrl(it) }
        val upiIds = upiIdRegex.findAll(norm.original).map { it.value }.distinct().toList()
        val phones = phoneRegex.findAll(norm.original).map { it.value }.distinct().toList()

        val hasDeviceAccess =
            (remoteAccessRegex.containsMatchIn(text) || apkInstallRegex.containsMatchIn(text)) &&
                !(officialStoreUpdateRegex.containsMatchIn(text) && !apkInstallRegex.containsMatchIn(text) && !remoteAccessRegex.containsMatchIn(text))

        val contradictions = buildList {
            if (RequestedAction.RECEIVE_MONEY_CLAIM in actions &&
                (SensitiveDataType.UPI_PIN in sensitive || RequestedAction.SCAN_QR in actions || RequestedAction.PAY_OR_TRANSFER in actions)
            ) {
                add("Claims to send/credit money while requesting outgoing payment authorization")
            }
            if (ClaimedIdentity.POLICE_COURT in identities && RequestedAction.PAY_OR_TRANSFER in actions) {
                add("Law enforcement impersonation combined with payment transfer demand")
            }
            if (ClaimedIdentity.BANK in identities && (hasAffirmativeCredRequest || hasDeviceAccess)) {
                add("Bank impersonation combined with secret credential or remote-access demand")
            }
        }

        return MessageContext(
            claimedIdentities = identities,
            requestedActions = actions,
            sensitiveDataMentions = sensitive,
            hasAffirmativeCredentialRequest = hasAffirmativeCredRequest,
            hasProtectiveCredentialWarning = hasProtectiveWarning,
            hasHardThreat = hardThreatRegex.containsMatchIn(text),
            hasUrgencyPressure = urgencyRegex.containsMatchIn(text),
            hasSecrecyInstruction = secrecyRegex.containsMatchIn(text),
            hasDigitalArrestCue = digitalArrestRegex.containsMatchIn(text),
            hasUpfrontFeeRequest = upfrontFeeRegex.containsMatchIn(text),
            hasLureCue = lureRegex.containsMatchIn(text),
            hasDeviceAccessRequest = hasDeviceAccess,
            extractedUrls = urls,
            suspiciousUrls = suspiciousUrls,
            officialUrls = officialUrls,
            extractedUpiIds = upiIds,
            extractedPhones = phones,
            hasMoneyAmount = moneyAmountRegex.containsMatchIn(text),
            contradictions = contradictions,
        )
    }

    private fun extractHost(rawUrl: String): String {
        val cleaned = rawUrl.trim().lowercase(Locale.ROOT)
            .removePrefix("https://")
            .removePrefix("http://")
        val authority = cleaned.substringBefore('/').substringBefore('?').substringBefore('#')
        // Handle @ userinfo spoofing: host is after @
        val hostPort = authority.substringAfterLast('@')
        return hostPort.substringBefore(':')
    }

    private fun isOfficialIndianUrl(rawUrl: String): Boolean {
        val lower = rawUrl.lowercase(Locale.ROOT)
        if (lower.startsWith("http://")) return false
        if (lower.contains("@") || lower.contains(".apk")) return false
        val host = extractHost(lower)
        return officialIndianDomainRegex.containsMatchIn(host)
    }

    private fun isSuspiciousUrl(rawUrl: String): Boolean {
        val lower = rawUrl.lowercase(Locale.ROOT)
        val host = extractHost(lower)
        if (host.isBlank()) return true
        if (isOfficialIndianUrl(lower)) return false
        return shortenerHostRegex.containsMatchIn(host) ||
            messengerRedirectRegex.containsMatchIn(host) ||
            suspiciousTldRegex.containsMatchIn(host) ||
            ipHostRegex.matches(host) ||
            lower.startsWith("http://") ||
            lower.contains("@") ||
            lower.contains(".apk") ||
            host.contains("xn--") ||
            brandImpersonationInHostRegex.containsMatchIn(host)
    }
}
