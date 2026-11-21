package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.CheckFirstAiRepository
import com.example.domain.AssessmentConfidence
import com.example.domain.EvidencePolarity
import com.example.domain.EvidenceStrength
import com.example.domain.LocalSafetyEngines
import com.example.domain.MessageScamAnalyzer
import com.example.domain.SafetyClassification
import com.example.domain.SafetyDecisionEngine
import com.example.domain.SafetyEvidence
import com.example.domain.TextNormalizer
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.TamilStrings
import com.example.ui.screens.ScamScannerScreen
import com.example.ui.theme.CheckFirstTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 1 test suite covering:
 * - Shared deterministic SafetyDecisionEngine invariants (Parts A & B)
 * - Contextual Scam Shield rules & paired malicious vs. legitimate/borderline fixtures (Parts C, D, E)
 * - Multilingual English, Tamil script, Tanglish, mixed-language, misspellings & Unicode obfuscation (Part E)
 * - Compact Scam Shield UI verdict, sample chips, expandable "More details", and zero pre-scan empty cards (Part F)
 *
 * All message examples below are synthetic test fixtures constructed for automated verification.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SafetyDecisionAndScamShieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    // =========================================================================
    // 1. SHARED SAFETY DECISION ENGINE INVARIANTS (PARTS A & B)
    // =========================================================================

    @Test
    fun `critical rule cannot be downgraded by multiple positive signals`() {
        val criticalEvidence = SafetyEvidence(
            ruleId = "MSG-CREDENTIAL-01",
            featureName = "Scam Shield",
            categoryId = "credential",
            title = "Credential request",
            description = "Requests secret OTP",
            strength = EvidenceStrength.CRITICAL,
            triggerReason = "Synthetic test critical finding",
            exceptionOrLimitation = "Never share OTP",
            recommendedAction = "Stop immediately",
            polarity = EvidencePolarity.RISK,
        )
        val positiveSignals = (1..3).map { idx ->
            SafetyEvidence(
                ruleId = "POS-0$idx",
                featureName = "Scam Shield",
                categoryId = "context",
                title = "Positive cue $idx",
                description = "Benign framing",
                strength = EvidenceStrength.STRONG,
                triggerReason = "Synthetic positive signal",
                exceptionOrLimitation = "Does not guarantee safety",
                recommendedAction = "Verify independently",
                polarity = EvidencePolarity.POSITIVE,
            )
        }

        val result = SafetyDecisionEngine.evaluate(
            featureName = "Scam Shield",
            findings = positiveSignals + criticalEvidence,
            hasContradictorySignals = true,
        )

        assertEquals(SafetyClassification.HIGH_RISK, result.classification)
        assertEquals("High risk — possible scam", result.verdictLabel)
        assertEquals(AssessmentConfidence.HIGH, result.confidence)
        assertFalse(result.onlineVerificationPerformed)
        assertTrue(result.ruleSetVersion.isNotBlank())
    }

    @Test
    fun `weak indicators alone never produce HIGH_RISK`() {
        val weakFindings = (1..5).map { idx ->
            SafetyEvidence(
                ruleId = "WEAK-0$idx",
                featureName = "Scam Shield",
                categoryId = "cat_$idx",
                title = "Weak indicator $idx",
                description = "Mild signal",
                strength = EvidenceStrength.WEAK,
                triggerReason = "Synthetic weak indicator",
                exceptionOrLimitation = "Common in benign messages",
                recommendedAction = "Verify independently",
                polarity = EvidencePolarity.RISK,
            )
        }

        val result = SafetyDecisionEngine.evaluate(
            featureName = "Scam Shield",
            findings = weakFindings,
        )

        assertNotEquals(SafetyClassification.HIGH_RISK, result.classification)
        assertEquals(SafetyClassification.CAUTION, result.classification)
        assertEquals("Suspicious indicators found", result.verdictLabel)
    }

    @Test
    fun `four user-facing verdict labels never claim 100 percent safe or guaranteed genuine`() {
        for (classification in SafetyClassification.entries) {
            val en = classification.displayLabel(tamil = false)
            val ta = classification.displayLabel(tamil = true)
            assertFalse(en.contains("100%", ignoreCase = true))
            assertFalse(en.contains("guaranteed", ignoreCase = true))
            assertFalse(ta.contains("100%", ignoreCase = true))
        }
        assertEquals("No major red flags detected", SafetyClassification.LIKELY_LEGITIMATE.displayLabel(false))
        assertEquals("Suspicious indicators found", SafetyClassification.CAUTION.displayLabel(false))
        assertEquals("High risk — possible scam", SafetyClassification.HIGH_RISK.displayLabel(false))
        assertEquals("Insufficient information", SafetyClassification.UNABLE_TO_ASSESS.displayLabel(false))
    }

    // =========================================================================
    // 2. CREDENTIAL REQUESTS vs. GENUINE OTP NOTIFICATIONS
    // =========================================================================

    @Test
    fun `genuine OTP notifications in English, Tamil, and Tanglish are NOT flagged as HIGH_RISK or CAUTION`() {
        // Synthetic genuine OTP fixtures
        val genuineEn = MessageScamAnalyzer.analyze(
            "Your OTP is 482913. Do not share this OTP with anyone.",
        )
        assertEquals(SafetyClassification.LIKELY_LEGITIMATE, genuineEn.classification)
        assertTrue("Expected no risk findings on genuine OTP", genuineEn.riskFindings.isEmpty())
        assertTrue(genuineEn.findings.any { it.ruleId == "MSG-INFO-PROTECTIVE-OTP" })

        val genuineTa = MessageScamAnalyzer.analyze(
            "உங்கள் வங்கி OTP 482913. இதை யாருடனும் பகிர வேண்டாம்.",
            tamil = true,
        )
        assertEquals(SafetyClassification.LIKELY_LEGITIMATE, genuineTa.classification)
        assertTrue(genuineTa.riskFindings.isEmpty())

        val genuineTanglish = MessageScamAnalyzer.analyze(
            "Ungal bank OTP 482913. Yaarukkum share pannathinga.",
        )
        assertEquals(SafetyClassification.LIKELY_LEGITIMATE, genuineTanglish.classification)
        assertTrue(genuineTanglish.riskFindings.isEmpty())
    }

    @Test
    fun `credential requests using polite professional language still trigger HIGH_RISK`() {
        // Synthetic polite credential-phishing fixture
        val politePhish = MessageScamAnalyzer.analyze(
            "Dear valued customer, thank you for banking with us. To complete your profile review, kindly reply with the 6-digit OTP sent to your registered mobile number.",
        )
        assertEquals(SafetyClassification.HIGH_RISK, politePhish.classification)
        assertTrue(politePhish.findings.any { it.ruleId == "MSG-CREDENTIAL-01" })

        // Legitimate polite counterpart
        val politeLegit = MessageScamAnalyzer.analyze(
            "Dear valued customer, your monthly bank account statement for October is ready. Please log in to your official bank app to view it.",
        )
        assertEquals(SafetyClassification.LIKELY_LEGITIMATE, politeLegit.classification)
    }

    // =========================================================================
    // 3. DIGITAL ARREST SCRIPTS vs. LEGITIMATE POLICE/AWARENESS ALERTS
    // =========================================================================

    @Test
    fun `digital arrest scripts in English and Tanglish trigger MSG-AUTHORITY-THREAT-01 while awareness alerts stay safe`() {
        // Synthetic digital arrest script (English)
        val digitalArrestEn = MessageScamAnalyzer.analyze(
            "This is Inspector Sharma from Mumbai Cyber Police CBI. An illegal parcel with drugs was found in your name. You are under digital arrest. Stay on video call, do not tell anyone, and transfer Rs 50,000 for RBI verification.",
        )
        assertEquals(SafetyClassification.HIGH_RISK, digitalArrestEn.classification)
        assertTrue(digitalArrestEn.findings.any { it.ruleId == "MSG-AUTHORITY-THREAT-01" })

        // Synthetic digital arrest script (Tanglish)
        val digitalArrestTanglish = MessageScamAnalyzer.analyze(
            "Naan CBI cyber police pesuren. Ungal perla illegal parcel case irukku, arrest warrant vandhurukku. Video call cut pannathinga, udane 25000 panam anuppunga.",
        )
        assertEquals(SafetyClassification.HIGH_RISK, digitalArrestTanglish.classification)
        assertTrue(digitalArrestTanglish.findings.any { it.ruleId == "MSG-AUTHORITY-THREAT-01" })

        // Legitimate public awareness counterpart mentioning Cyber Police, OTP, UPI PIN, cybercrime.gov.in
        val legitAwareness = MessageScamAnalyzer.analyze(
            "Public awareness from Cyber Police: Never share your OTP or UPI PIN with anyone. Report cyber fraud on 1930 or https://cybercrime.gov.in.",
        )
        assertEquals(SafetyClassification.LIKELY_LEGITIMATE, legitAwareness.classification)
    }

    // =========================================================================
    // 4. REFUND & UPI PIN DECEPTION vs. LEGITIMATE REFUND CONFIRMATION
    // =========================================================================

    @Test
    fun `refund plus UPI PIN or QR scan triggers CRITICAL deception while passive refund confirmation stays LIKELY_LEGITIMATE`() {
        // Synthetic refund + UPI PIN deception (English)
        val refundTrapEn = MessageScamAnalyzer.analyze(
            "Your Rs 2,500 order refund is ready. Scan the QR code and enter your UPI PIN to receive the money.",
        )
        assertEquals(SafetyClassification.HIGH_RISK, refundTrapEn.classification)
        assertTrue(refundTrapEn.findings.any { it.ruleId == "MSG-RECEIVE-PAYMENT-01" })

        // Synthetic refund + UPI PIN deception (Tanglish)
        val refundTrapTanglish = MessageScamAnalyzer.analyze(
            "Unga refund 1500 ready ah irukku. Panam pera QR scan pannunga, apram UPI PIN podunga.",
        )
        assertEquals(SafetyClassification.HIGH_RISK, refundTrapTanglish.classification)
        assertTrue(refundTrapTanglish.findings.any { it.ruleId == "MSG-RECEIVE-PAYMENT-01" })

        // Synthetic refund + upfront fee deception (without PIN)
        val refundFeeTrap = MessageScamAnalyzer.analyze(
            "Your cashback refund of Rs 3,000 is approved. Pay a Rs 199 processing fee first to release your refund.",
        )
        assertEquals(SafetyClassification.HIGH_RISK, refundFeeTrap.classification)
        assertTrue(refundFeeTrap.findings.any { it.ruleId == "MSG-REFUND-DECEPTION-01" })

        // Legitimate passive refund confirmation counterpart
        val legitRefund = MessageScamAnalyzer.analyze(
            "Your refund of Rs. 899 for Order 4421 has been initiated and will reflect in your original payment account within 3-5 business days.",
        )
        assertEquals(SafetyClassification.LIKELY_LEGITIMATE, legitRefund.classification)
    }

    // =========================================================================
    // 5. REMOTE-ACCESS / APK REQUESTS vs. LEGITIMATE APP UPDATE NOTICE
    // =========================================================================

    @Test
    fun `APK sideloading and AnyDesk requests trigger MSG-REMOTE-ACCESS-01 while Play Store notice stays LIKELY_LEGITIMATE`() {
        // Synthetic remote-access & APK scam
        val apkAndAnyDesk = MessageScamAnalyzer.analyze(
            "Your KYC is pending. Please download SBI_KYC_Update.apk or install AnyDesk and share the 9-digit code with our support agent.",
        )
        assertEquals(SafetyClassification.HIGH_RISK, apkAndAnyDesk.classification)
        assertTrue(apkAndAnyDesk.findings.any { it.ruleId == "MSG-REMOTE-ACCESS-01" })

        // Legitimate Play Store update counterpart
        val legitUpdate = MessageScamAnalyzer.analyze(
            "A new version of your banking app is available on the Google Play Store. Update directly from the Play Store when convenient.",
        )
        assertEquals(SafetyClassification.LIKELY_LEGITIMATE, legitUpdate.classification)
    }

    // =========================================================================
    // 6. URGENT BUT LEGITIMATE NOTIFICATIONS & MIXED LEGITIMATE/SUSPICIOUS CONTENT
    // =========================================================================

    @Test
    fun `urgent but legitimate bill reminder does not trigger HIGH_RISK`() {
        val legitUrgentBill = MessageScamAnalyzer.analyze(
            "Reminder: Your electricity bill of Rs. 1,420 is due today. Please pay using your official utility app or bank portal.",
        )
        assertNotEquals(SafetyClassification.HIGH_RISK, legitUrgentBill.classification)
        assertEquals(SafetyClassification.LIKELY_LEGITIMATE, legitUrgentBill.classification)
    }

    @Test
    fun `mixed legitimate OTP text and appended scam request still triggers HIGH_RISK`() {
        // Synthetic mixed legitimate + malicious fixture
        val mixedScam = MessageScamAnalyzer.analyze(
            "Your OTP is 482913. Do not share this OTP with anyone. Hi sir I am calling from bank KYC desk, kindly share that OTP right now or your account will be blocked.",
        )
        assertEquals(SafetyClassification.HIGH_RISK, mixedScam.classification)
        assertTrue(mixedScam.findings.any { it.ruleId == "MSG-CREDENTIAL-01" })
    }

    // =========================================================================
    // 7. TAMIL, TANGLISH, CODE-MIXING, MISSPELLINGS & UNICODE VARIATIONS
    // =========================================================================

    @Test
    fun `informal Tanglish code-mixing without scam intent is NOT treated as risk`() {
        // Synthetic benign informal Tanglish fixture mentioning "bank"
        val benignTanglish = MessageScamAnalyzer.analyze(
            "Dei machan, naalaikku morning bank poi passbook update pannitu varuvaen, lunch ku meet pannalam.",
        )
        assertEquals(SafetyClassification.LIKELY_LEGITIMATE, benignTanglish.classification)
        assertTrue(benignTanglish.riskFindings.isEmpty())
    }

    @Test
    fun `Unicode homoglyphs, zero-width chars, leetspeak, and spaced letters are normalized while preserving original text`() {
        // Uses zero-width space \u200B, Cyrillic 'а' (\u0430), leetspeak '0.T.P' / 'P 1 N', and suspicious .xyz link
        val obfuscated = "Urgent! Sh@re your 0.T.P or U\u200B P I  P 1 N now to unbl0ck your b\u0430nk acc0unt at http://sbi-kyc.xyz"
        val norm = TextNormalizer.normalize(obfuscated)
        assertEquals("Original text must be preserved verbatim", obfuscated, norm.original)
        assertTrue(norm.deobfuscated.contains("otp", ignoreCase = true))
        assertTrue(norm.deobfuscated.contains("pin", ignoreCase = true))

        val assessment = MessageScamAnalyzer.analyze(obfuscated)
        assertEquals(SafetyClassification.HIGH_RISK, assessment.classification)
        assertTrue(assessment.findings.any { it.ruleId == "MSG-CREDENTIAL-01" })
        assertTrue(assessment.findings.any { it.ruleId == "MSG-SUSPICIOUS-LINK-01" })
    }

    // =========================================================================
    // 8. INSUFFICIENT INFORMATION CASES
    // =========================================================================

    @Test
    fun `short or fragmentary messages return UNABLE_TO_ASSESS with missingInformation`() {
        val fragments = listOf("", "   ", "hello", "call me", "OTP", "refund", "வணக்கம்")
        for (frag in fragments) {
            val res = MessageScamAnalyzer.analyze(frag)
            assertEquals("Expected UNABLE_TO_ASSESS for '$frag'", SafetyClassification.UNABLE_TO_ASSESS, res.classification)
            assertEquals("Insufficient information", res.verdictLabel)
            assertTrue("Expected missingInformation for '$frag'", res.missingInformation.isNotEmpty())
        }
    }

    // =========================================================================
    // 9. COMPACT SCAM SHIELD UI VERIFICATION (PART F)
    // =========================================================================

    @Test
    fun `ScamScannerScreen has no pre-scan empty card, supports sample chips, and toggles expandable More details`() {
        composeRule.setContent {
            CheckFirstTheme(tamil = false) {
                ScamScannerScreen(
                    aiRepository = CheckFirstAiRepository(),
                    onRecordActivity = { _, _, _ -> },
                    onShowNotice = {},
                )
            }
        }
        composeRule.waitForIdle()

        // No pre-scan empty card or filler explanation before the user scans
        assertEquals(0, composeRule.onAllNodesWithText(EnglishStrings.scanEmptyTitle).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText(EnglishStrings.scanHowTitle).fetchSemanticsNodes().size)

        // Tap clearly labelled "Sample: Genuine OTP" chip and scan -> No major red flags detected
        composeRule.onNodeWithTag("scanner_sample_genuine_otp").performScrollTo().performClick()
        composeRule.onNodeWithTag("scanner_analyze_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(EnglishStrings.verdictLikelyLegitimate).performScrollTo().assertIsDisplayed()

        // Tap "Sample: Digital Arrest" chip and scan -> High risk — possible scam
        composeRule.onNodeWithTag("scanner_sample_digital_arrest").performScrollTo().performClick()
        composeRule.onNodeWithTag("scanner_analyze_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(EnglishStrings.verdictHighRisk).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(EnglishStrings.scanRecommendedActionLabel).performScrollTo().assertIsDisplayed()

        // Expand "More details" -> rule ID and metadata become visible
        composeRule.onNodeWithTag("scanner_more_details_toggle").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("MSG-AUTHORITY-THREAT-01").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(EnglishStrings.scanHideDetails).performScrollTo().assertIsDisplayed()

        // Clear and test fragmentary input -> Insufficient information
        composeRule.onNodeWithTag("scanner_clear_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("scanner_message_input").performScrollTo().performTextInput("hello")
        composeRule.onNodeWithTag("scanner_analyze_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(EnglishStrings.verdictUnableToAssess).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `ScamScannerScreen displays Tamil 4-outcome verdict cleanly`() {
        val tamilResult = LocalSafetyEngines.scanMessageLocal(
            "அவசரம்! உங்கள் வங்கி OTP-ஐ உடனே சொல்லுங்கள்.",
            tamil = true,
        )
        assertEquals(SafetyClassification.HIGH_RISK, tamilResult.assessment.classification)
        assertEquals(TamilStrings.verdictHighRisk, tamilResult.verdict)
    }
}
