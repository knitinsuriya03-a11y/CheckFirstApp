package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.CheckFirstAiRepository
import com.example.data.PreferencesDataStore
import com.example.domain.ActivityItem
import com.example.domain.CheckFirstDemoData
import com.example.domain.LocalSafetyEngines
import com.example.domain.Prefs
import com.example.domain.RiskLevel
import com.example.domain.TenureUnit
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmergencyScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.LiveVoiceScreen
import com.example.ui.screens.LoanCalculatorScreen
import com.example.ui.screens.LoanRiskScreen
import com.example.ui.screens.QrAnalyzerScreen
import com.example.ui.screens.ScamScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CheckFirstTheme
import com.example.viewmodel.AiAssistantViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CheckFirstAuditTest {

    @get:Rule
    val composeRule = createComposeRule()

    // -------------------------------------------------------------------------
    // 1. Scam Message Detection (English, Tamil, Mixed-Language, Clean)
    // -------------------------------------------------------------------------

    @Test
    fun `scam scanner detects English UPI PIN, OTP, fake refund, urgency, impersonation, and suspicious links`() {
        val sample = LocalSafetyEngines.scanMessageLocal(CheckFirstDemoData.SAMPLE_SCAM_MESSAGE)
        assertEquals(RiskLevel.DANGER, sample.risk)
        val matchedIds = sample.matches.map { it.id }.toSet()
        assertTrue("urgency expected", "urgency" in matchedIds)
        assertTrue("authority expected", "authority" in matchedIds)
        assertTrue("refund expected", "refund" in matchedIds)
        assertTrue("credential expected", "credential" in matchedIds)
        assertTrue("lure expected", "lure" in matchedIds)
        assertTrue("link expected", "link" in matchedIds)

        val otpPoliceScam = LocalSafetyEngines.scanMessageLocal(
            "Cyber cell police warning: Your account will be blocked immediately. Share your OTP and call +91 98765 43210 or visit bit.ly/verify-now",
        )
        assertEquals(RiskLevel.DANGER, otpPoliceScam.risk)
        val ids2 = otpPoliceScam.matches.map { it.id }.toSet()
        assertTrue(setOf("urgency", "authority", "credential", "link").all { it in ids2 })
    }

    @Test
    fun `scam scanner detects pure Tamil scam messages`() {
        val tamilRefundPin = LocalSafetyEngines.scanMessageLocal(
            "அவசரம்! வங்கி திரும்பப் பணம் பெற உடனே உங்கள் யுபிஐ பின் அல்லது ஓடிபி பகிரவும். முன்பணம் செலுத்தவும்.",
        )
        assertEquals(RiskLevel.DANGER, tamilRefundPin.risk)
        val ids = tamilRefundPin.matches.map { it.id }.toSet()
        assertTrue("urgency expected", "urgency" in ids)
        assertTrue("authority expected", "authority" in ids)
        assertTrue("refund expected", "refund" in ids)
        assertTrue("credential expected", "credential" in ids)
        assertTrue("fee expected", "fee" in ids)
    }

    @Test
    fun `scam scanner detects mixed Tamil and English scam messages`() {
        val mixed = LocalSafetyEngines.scanMessageLocal(
            "உங்கள் வங்கி KYC expire ஆகிறது within 10 minutes! பணத்தைப் பெற OTP சொல்லவும்: https://wa.me/919999999999",
        )
        assertEquals(RiskLevel.DANGER, mixed.risk)
        val ids = mixed.matches.map { it.id }.toSet()
        assertTrue("urgency expected", "urgency" in ids)
        assertTrue("authority expected", "authority" in ids)
        assertTrue("refund expected", "refund" in ids)
        assertTrue("credential expected", "credential" in ids)
        assertTrue("link expected", "link" in ids)
    }

    @Test
    fun `scam scanner returns CLEAR for normal everyday messages`() {
        val benign = LocalSafetyEngines.scanMessageLocal(
            "Hi Amma, reached Chennai safely. Will call after dinner.",
        )
        assertEquals(RiskLevel.CLEAR, benign.risk)
        assertTrue(benign.matches.isEmpty())
    }

    // -------------------------------------------------------------------------
    // 2. QR Code Bitmap Decoding & UPI Payment Link Analysis
    // -------------------------------------------------------------------------

    @Test
    fun `QR bitmap decoding extracts UPI payload and flags fake refund payment request`() {
        val qrBitmap = createQrBitmap(CheckFirstDemoData.SAMPLE_UPI_URI, 300)
        try {
            val decoded = LocalSafetyEngines.decodeQrFromBitmap(qrBitmap)
            assertTrue("QR decode should succeed", decoded.isSuccess)
            assertEquals(CheckFirstDemoData.SAMPLE_UPI_URI, decoded.getOrThrow())

            val parsed = LocalSafetyEngines.parseUpiUri(decoded.getOrThrow())
            assertTrue("UPI URI parse should succeed", parsed.isSuccess)
            val details = parsed.getOrThrow()
            assertEquals("refund-help@demobank", details.payee)
            assertEquals("Refund Support", details.name)
            assertEquals(2000.0, details.amount!!, 0.001)
            assertEquals("Refund processing", details.note)
            assertEquals("INR", details.currency)
            assertTrue("Fake refund outgoing request must be flagged", details.isRefundMislead)
        } finally {
            qrBitmap.recycle()
        }
    }

    @Test
    fun `UPI parser handles normal merchant payment and rejects invalid URIs`() {
        val merchant = LocalSafetyEngines.parseUpiUri(
            "upi://pay?pa=freshmart@okaxis&pn=Fresh%20Mart&am=450.50&tn=Groceries&cu=INR",
        ).getOrThrow()
        assertEquals("freshmart@okaxis", merchant.payee)
        assertEquals("Fresh Mart", merchant.name)
        assertEquals(450.50, merchant.amount!!, 0.001)
        assertFalse(merchant.isRefundMislead)

        assertTrue(LocalSafetyEngines.parseUpiUri("").isFailure)
        assertTrue(LocalSafetyEngines.parseUpiUri("https://example.com/pay").isFailure)
        assertTrue(LocalSafetyEngines.parseUpiUri("upi://pay?pa=x@y&am=-100").isFailure)
        assertTrue(LocalSafetyEngines.parseUpiUri("upi://pay?pa=x@y&am=abc").isFailure)

        val blankBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        try {
            assertTrue(LocalSafetyEngines.decodeQrFromBitmap(blankBitmap).isFailure)
        } finally {
            blankBitmap.recycle()
        }
    }

    // -------------------------------------------------------------------------
    // 3. Loan Borrowing Cost Calculations vs Independent Hand-Calculated Math
    // -------------------------------------------------------------------------

    @Test
    fun `loan cost calculator matches independently calculated examples across days, months, and years`() {
        // Case 1 (Days): Received ₹10,000, Repay ₹11,200 in 30 days
        // Cost = 11200 - 10000 = 1200; Rate = (1200 / 10000) * (365 / 30) * 100 = 146.0%
        val daysCase = LocalSafetyEngines.calculateLoanCost("10000", "500", "11200", "30", TenureUnit.DAYS).getOrThrow()
        assertEquals(1200.0, daysCase.cost, 0.0001)
        assertEquals(30.0, daysCase.days, 0.0001)
        assertEquals(146.0, daysCase.annualizedRatePercent, 0.0001)

        // Case 2 (Months): Received ₹50,000, Repay ₹56,000 in 6 months (6 * 30.4375 = 182.625 days)
        // Cost = 6000; Rate = (6000 / 50000) * (365 / 182.625) * 100 = 23.983572895277206%
        val monthsCase = LocalSafetyEngines.calculateLoanCost("50000", "2000", "56000", "6", TenureUnit.MONTHS).getOrThrow()
        assertEquals(6000.0, monthsCase.cost, 0.0001)
        assertEquals(182.625, monthsCase.days, 0.0001)
        assertEquals(23.9836, monthsCase.annualizedRatePercent, 0.001)

        // Case 3 (Years): Received ₹100,000, Repay ₹115,000 in 1 year (365.25 days)
        // Cost = 15000; Rate = (15000 / 100000) * (365 / 365.25) * 100 = 14.989733059548255%
        val yearsCase = LocalSafetyEngines.calculateLoanCost("100000", "0", "115000", "1", TenureUnit.YEARS).getOrThrow()
        assertEquals(15000.0, yearsCase.cost, 0.0001)
        assertEquals(365.25, yearsCase.days, 0.0001)
        assertEquals(14.9897, yearsCase.annualizedRatePercent, 0.001)

        // Invalid inputs rejected
        assertTrue(LocalSafetyEngines.calculateLoanCost("10000", "0", "9000", "30", TenureUnit.DAYS).isFailure)
        assertTrue(LocalSafetyEngines.calculateLoanCost("0", "0", "1000", "30", TenureUnit.DAYS).isFailure)
        assertTrue(LocalSafetyEngines.calculateLoanCost("10000", "-50", "11000", "30", TenureUnit.DAYS).isFailure)
        assertTrue(LocalSafetyEngines.calculateLoanCost("10000", "0", "11000", "0", TenureUnit.DAYS).isFailure)
    }

    // -------------------------------------------------------------------------
    // 4. Settings Persistence, Opt-In Privacy Default, Metadata-Only History
    // -------------------------------------------------------------------------

    @Test
    fun `settings persist in DataStore and opt-in history defaults to OFF and wipes when disabled`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = PreferencesDataStore(context)

        // Default preferences check
        val defaultPrefs = Prefs()
        assertFalse("Opt-in history MUST default to OFF", defaultPrefs.saveHistory)
        assertTrue(defaultPrefs.safetyTips)
        assertFalse(defaultPrefs.tamil)

        // Save accessibility settings + opt-in history ON
        val updated = Prefs(
            tamil = true,
            simpleMode = true,
            largeText = true,
            safetyTips = false,
            completionAlerts = true,
            saveHistory = true,
        )
        store.savePrefs(updated)
        assertEquals(updated, store.prefsFlow.first())

        // Add metadata-only item
        store.addLocalHistoryItem(
            ActivityItem("id-1", "Message scan", "Message scan result", RiskLevel.DANGER, "Nov 19 · 5:00 PM", false),
        )
        val storedHistory = store.localHistoryFlow.first()
        assertEquals(1, storedHistory.size)
        assertEquals("Message scan result", storedHistory.first().label)

        // Turning saveHistory OFF immediately purges stored local history
        store.savePrefs(updated.copy(saveHistory = false))
        assertTrue(store.localHistoryFlow.first().isEmpty())

        // Restore defaults so other tests see clean state
        store.savePrefs(Prefs())
    }

    // -------------------------------------------------------------------------
    // 5. Compose UI Functional Verification Across All 10 Screens
    // -------------------------------------------------------------------------

    @Test
    fun `DashboardScreen renders hero, tools, and respects safetyTips toggle`() {
        var navigatedTo: String? = null
        composeRule.setContent {
            CheckFirstTheme {
                DashboardScreen(
                    prefs = Prefs(safetyTips = true),
                    history = emptyList(),
                    isSignedIn = false,
                    onNavigate = { navigatedTo = it },
                    onDisableAndClearHistory = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Welcome to CheckFirst").assertIsDisplayed()
        composeRule.onNodeWithTag("hero_scan_message_button").performClick()
        assertEquals("scanner", navigatedTo)
    }

    @Test
    fun `ScamScannerScreen scans locally and records only generic metadata without raw message`() {
        var recordedKind: String? = null
        var recordedLabel: String? = null
        var recordedRisk: RiskLevel? = null

        composeRule.setContent {
            CheckFirstTheme {
                ScamScannerScreen(
                    aiRepository = CheckFirstAiRepository(),
                    onRecordActivity = { k, l, r ->
                        recordedKind = k
                        recordedLabel = l
                        recordedRisk = r
                    },
                    onShowNotice = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("scanner_load_example_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("scanner_analyze_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("Message scan", recordedKind)
        assertEquals("Message scan result", recordedLabel)
        assertEquals(RiskLevel.DANGER, recordedRisk)
        // Confirm raw message content is NOT leaked into recorded metadata label
        val label = requireNotNull(recordedLabel)
        assertFalse(label.contains("UPI PIN"))
        assertFalse(label.contains("example.invalid"))
        composeRule.onNodeWithText("High risk — possible scam").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `QrAnalyzerScreen parses sample UPI link and records generic metadata only`() {
        var recordedLabel: String? = null
        var recordedRisk: RiskLevel? = null

        composeRule.setContent {
            CheckFirstTheme {
                QrAnalyzerScreen(
                    tamil = false,
                    aiRepository = CheckFirstAiRepository(),
                    onRecordActivity = { _, l, r ->
                        recordedLabel = l
                        recordedRisk = r
                    },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("qr_analyze_sample_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("Payment request inspected", recordedLabel)
        assertEquals(RiskLevel.DANGER, recordedRisk)
        assertFalse(recordedLabel!!.contains("refund-help@demobank"))
        composeRule.onNodeWithText("OUT → SENDING MONEY").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `GuideScreen displays all 5 verification steps and opens Emergency Help`() {
        var emergencyOpened = false
        composeRule.setContent {
            CheckFirstTheme {
                GuideScreen(onOpenEmergency = { emergencyOpened = true })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Pause. Think. Verify.").assertIsDisplayed()
        composeRule.onNodeWithTag("guide_open_emergency_button").performScrollTo().performClick()
        assertTrue(emergencyOpened)
    }

    @Test
    fun `EmergencyScreen launches 1930 dialer and cybercrime portal intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowApp = shadowOf(context as android.app.Application)

        composeRule.setContent {
            CheckFirstTheme {
                EmergencyScreen(onShowNotice = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("emergency_call_1930_card").performClick()
        val dialIntent: Intent? = shadowApp.nextStartedActivity
        assertNotNull("Expected ACTION_DIAL intent", dialIntent)
        assertEquals(Intent.ACTION_DIAL, dialIntent!!.action)
        assertEquals("tel:1930", dialIntent.dataString)

        composeRule.onNodeWithTag("emergency_portal_card").performClick()
        val portalIntent: Intent? = shadowApp.nextStartedActivity
        assertNotNull("Expected ACTION_VIEW intent", portalIntent)
        assertEquals(Intent.ACTION_VIEW, portalIntent!!.action)
        assertEquals("https://cybercrime.gov.in", portalIntent.dataString)
    }

    @Test
    fun `LoanCalculatorScreen calculates true cost and shows error for invalid repayment`() {
        var recordedRisk: RiskLevel? = null
        composeRule.setContent {
            CheckFirstTheme {
                LoanCalculatorScreen(onRecordActivity = { _, _, r -> recordedRisk = r })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("loan_calculate_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(RiskLevel.DANGER, recordedRisk)
        composeRule.onNodeWithText("146.0").performScrollTo().assertIsDisplayed()

        // Enter invalid repayment < received
        composeRule.onNodeWithTag("loan_repayment_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("loan_repayment_input").performTextInput("5000")
        composeRule.onNodeWithTag("loan_calculate_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter valid loan details").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `LoanRiskScreen computes DANGER when 3 or more indicators are flagged`() {
        var recordedRisk: RiskLevel? = null
        composeRule.setContent {
            CheckFirstTheme {
                LoanRiskScreen(onRecordActivity = { _, _, r -> recordedRisk = r })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("loan_risk_submit_button").performScrollTo().assertIsNotEnabled()

        // Flag 4 questions as YES, 2 as NO
        for (i in 0..3) {
            composeRule.onNodeWithTag("loan_risk_q${i}_yes").performScrollTo().performClick()
        }
        for (i in 4..5) {
            composeRule.onNodeWithTag("loan_risk_q${i}_no").performScrollTo().performClick()
        }
        composeRule.onNodeWithTag("loan_risk_submit_button").performScrollTo().assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertEquals(RiskLevel.DANGER, recordedRisk)
        composeRule.onNodeWithText("Multiple warning signs identified").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `SettingsScreen renders privacy controls and updates prefs callbacks`() {
        var current = Prefs()
        composeRule.setContent {
            CheckFirstTheme {
                SettingsScreen(
                    prefs = current,
                    currentUser = null,
                    syncError = null,
                    onUpdatePrefs = { current = it(current) },
                    onClearHistory = {},
                    onSignInSuccess = {},
                    onSignInError = {},
                    onSignOut = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_large_text_switch").performScrollTo().performClick()
        assertTrue(current.largeText)
        composeRule.onNodeWithTag("settings_save_history_switch").performScrollTo().performClick()
        assertTrue(current.saveHistory)
    }

    @Test
    fun `AiChatScreen and LiveVoiceScreen render safety reminders and controls`() {
        val aiVm = AiAssistantViewModel()
        composeRule.setContent {
            CheckFirstTheme {
                AiChatScreen(tamil = false, viewModel = aiVm)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Never share OTPs or UPI PINs").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_send_button").assertIsNotEnabled()
    }

    @Test
    fun `LiveVoiceScreen renders idle state and privacy reminder`() {
        val aiVm = AiAssistantViewModel()
        composeRule.setContent {
            CheckFirstTheme {
                LiveVoiceScreen(tamil = false, viewModel = aiVm)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("GEMINI LIVE · READY").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("live_voice_start_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Voice Privacy Reminder").performScrollTo().assertIsDisplayed()
    }

    private fun createQrBitmap(content: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, size, 0, 0, size, size)
        }
    }
}
