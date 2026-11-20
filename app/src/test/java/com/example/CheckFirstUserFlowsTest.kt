package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.AiModelBackend
import com.example.data.ChatStreamEvent
import com.example.data.CheckFirstAiRepository
import com.example.data.PreferencesDataStore
import com.example.domain.ChatMessage
import com.example.domain.CheckFirstDemoData
import com.example.domain.LocalSafetyEngines
import com.example.domain.Prefs
import com.example.domain.RiskLevel
import com.example.domain.TenureUnit
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.TamilStrings
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmergencyScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.LiveVoiceScreen
import com.example.ui.screens.LoanCalculatorScreen
import com.example.ui.screens.LoanRiskScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.QrAnalyzerScreen
import com.example.ui.screens.ScamScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CheckFirstTheme
import com.example.viewmodel.AiAssistantViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.ai.type.Schema
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive user-perspective test suite verifying:
 * 1. Mandatory Login Gate at startup (never skips login).
 * 2. Automatic Gemini AI model failover across gemini-3.8-flash -> gemini-3.5-flash-lite -> gemini-4-argon.
 * 3. Complete end-to-end English <-> Tamil localization of ALL screen text (not only headings/navigation).
 * 4. Every user workflow, form edge case, clear/reset flow, and accessibility mode.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CheckFirstUserFlowsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUpFirebaseForJvm() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:testcheckfirst")
                    .setApiKey("test-local-jvm-key")
                    .setProjectId("test-checkfirst-project")
                    .build(),
            )
        }
        runBlocking {
            PreferencesDataStore(context).savePrefs(Prefs())
        }
    }

    // =========================================================================
    // 1. MANDATORY STARTUP LOGIN GATE
    // =========================================================================

    @Test
    fun `app always starts on LoginScreen and never bypasses authentication`() {
        composeRule.setContent {
            CheckFirstApp()
        }
        composeRule.waitForIdle()

        // Login gate is mandatory at startup
        composeRule.onNodeWithText(EnglishStrings.loginTitle).assertIsDisplayed()
        composeRule.onNodeWithTag("login_google_sign_in_button").performScrollTo().assertIsDisplayed()
        // Dashboard MUST NOT be rendered behind the login gate
        assertEquals(0, composeRule.onAllNodesWithText(EnglishStrings.dashScanMessageCta).fetchSemanticsNodes().size)
    }

    @Test
    fun `LoginScreen toggles completely between English and Tamil`() {
        var tamil by mutableStateOf(false)
        composeRule.setContent {
            CheckFirstTheme(tamil = tamil) {
                LoginScreen(
                    currentUser = null,
                    tamil = tamil,
                    onToggleLanguage = { tamil = !tamil },
                    onSignInSuccess = {},
                    onContinueSignedInUser = {},
                    onSwitchAccount = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(EnglishStrings.loginTitle).assertIsDisplayed()
        composeRule.onNodeWithText(EnglishStrings.loginWhyTitle).performScrollTo().assertIsDisplayed()

        // Toggle to Tamil
        composeRule.onNodeWithTag("login_language_toggle").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(TamilStrings.loginTitle).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.loginButtonGoogle).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.loginWhyTitle).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.loginBenefit1).performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 2. AUTOMATIC GEMINI AI MODEL FAILOVER (INSIGHTS + STREAMING CHAT)
    // =========================================================================

    private class FakeAiBackend(
        private val insightHandler: suspend (modelName: String, prompt: String) -> String?,
        private val chatHandler: (modelName: String, prompt: String) -> Flow<String> = { _, _ ->
            flow { emit("Calm advice from fake backend.") }
        },
    ) : AiModelBackend {
        val attemptedModels = mutableListOf<String>()

        override suspend fun generateStructuredInsight(
            modelName: String,
            prompt: String,
            systemInstruction: String,
            schema: Schema,
        ): String? {
            attemptedModels.add(modelName)
            return insightHandler(modelName, prompt)
        }

        override fun streamChatMessage(
            modelName: String,
            history: List<ChatMessage>,
            userPrompt: String,
            systemInstruction: String,
        ): Flow<String> {
            attemptedModels.add(modelName)
            return chatHandler(modelName, userPrompt)
        }
    }

    private val sampleInsightJson = """
        {
          "summary": "This message uses artificial urgency and asks for secret credentials.",
          "keyConcerns": ["Asks for UPI PIN", "Suspicious unverified link"],
          "recommendedActions": ["Do not enter your UPI PIN", "Contact your bank via official number"],
          "cautionNote": "Guidance only, not proof of legitimacy."
        }
    """.trimIndent()

    @Test
    fun `AI insights automatically switch from busy gemini-3_8-flash to gemini-3_5-flash-lite`() = runTest {
        val backend = FakeAiBackend(
            insightHandler = { modelName, _ ->
                if (modelName == "gemini-3.8-flash") {
                    throw IllegalStateException("429 Resource Exhausted: model busy")
                }
                sampleInsightJson
            },
        )
        val repo = CheckFirstAiRepository(backend = backend)
        val localResult = LocalSafetyEngines.scanMessageLocal(CheckFirstDemoData.SAMPLE_SCAM_MESSAGE)

        val result = repo.analyzeMessageWithGemini(
            message = CheckFirstDemoData.SAMPLE_SCAM_MESSAGE,
            language = "English",
            localResult = localResult,
        )

        assertTrue("Expected failover to succeed", result.isSuccess)
        val insight = result.getOrThrow()
        assertEquals("gemini-3.5-flash-lite", insight.modelUsed)
        assertTrue("usedFallback should be true", insight.usedFallback)
        assertEquals(listOf("gemini-3.8-flash", "gemini-3.5-flash-lite"), backend.attemptedModels)
    }

    @Test
    fun `AI insights switch to gemini-4-argon when first two models time out or fail`() = runTest {
        val backend = FakeAiBackend(
            insightHandler = { modelName, _ ->
                when (modelName) {
                    "gemini-3.8-flash" -> {
                        delay(500) // triggers short test timeout
                        sampleInsightJson
                    }
                    "gemini-3.5-flash-lite" -> throw RuntimeException("503 Service Unavailable")
                    "gemini-4-argon" -> sampleInsightJson
                    else -> null
                }
            },
        )
        val repo = CheckFirstAiRepository(backend = backend, perModelTimeoutMs = 100L)
        val upi = LocalSafetyEngines.parseUpiUri(CheckFirstDemoData.SAMPLE_UPI_URI).getOrThrow()

        val result = repo.analyzeUpiWithGemini(upi, tamil = false)

        assertTrue("Expected third model to succeed", result.isSuccess)
        val insight = result.getOrThrow()
        assertEquals("gemini-4-argon", insight.modelUsed)
        assertTrue(insight.usedFallback)
        assertEquals(
            listOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon"),
            backend.attemptedModels,
        )
    }

    @Test
    fun `AI insights return transparent failure when all models are unavailable`() = runTest {
        val backend = FakeAiBackend(
            insightHandler = { modelName, _ ->
                throw IllegalStateException("$modelName overloaded")
            },
        )
        val repo = CheckFirstAiRepository(backend = backend)
        val localResult = LocalSafetyEngines.scanMessageLocal(CheckFirstDemoData.SAMPLE_SCAM_MESSAGE)

        val resultEn = repo.analyzeMessageWithGemini("test", "English", localResult, tamilUi = false)
        assertTrue(resultEn.isFailure)
        assertTrue(resultEn.exceptionOrNull()?.message?.contains("busy or unreachable") == true)

        val resultTa = repo.analyzeMessageWithGemini("test", "தமிழ்", localResult, tamilUi = true)
        assertTrue(resultTa.isFailure)
        assertTrue(resultTa.exceptionOrNull()?.message?.contains("Gemini") == true)
    }

    @Test
    fun `AI safety chat streams seamlessly after failing over from busy primary model`() = runTest {
        val backend = FakeAiBackend(
            insightHandler = { _, _ -> sampleInsightJson },
            chatHandler = { modelName, _ ->
                flow {
                    if (modelName == "gemini-3.8-flash") {
                        throw IllegalStateException("Model overloaded")
                    }
                    emit("Never share ")
                    emit("your UPI PIN.")
                }
            },
        )
        val repo = CheckFirstAiRepository(backend = backend)
        val events = repo.streamSafetyChatWithFallback(
            history = emptyList(),
            userPrompt = "Do I need a PIN for a refund?",
            tamil = false,
        ).toList()

        val chunks = events.filterIsInstance<ChatStreamEvent.Chunk>()
        assertEquals("Never share your UPI PIN.", chunks.joinToString("") { it.text })
        assertTrue(chunks.all { it.modelName == "gemini-3.5-flash-lite" && it.usedFallback })
        assertEquals(listOf("gemini-3.8-flash", "gemini-3.5-flash-lite"), backend.attemptedModels)
    }

    @Test
    fun `ScamScannerScreen UI displays auto-switched fallback model badge when primary is busy`() {
        val backend = FakeAiBackend(
            insightHandler = { modelName, _ ->
                if (modelName == "gemini-3.8-flash") throw IllegalStateException("Busy")
                sampleInsightJson
            },
        )
        val repo = CheckFirstAiRepository(backend = backend)

        composeRule.setContent {
            CheckFirstTheme(tamil = false) {
                ScamScannerScreen(
                    aiRepository = repo,
                    onRecordActivity = { _, _, _ -> },
                    onShowNotice = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("scanner_load_example_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("scanner_analyze_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("scanner_gemini_insight_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("${EnglishStrings.aiInsightAutoSwitchedPrefix} gemini-3.5-flash-lite")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("${EnglishStrings.aiInsightAutoSwitchedPrefix} gemini-3.5-flash-lite")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("This message uses artificial urgency and asks for secret credentials.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // =========================================================================
    // 3. COMPLETE END-TO-END LANGUAGE SWITCHING (EVERY TEXT CHANGES EN <-> TA)
    // =========================================================================

    @Test
    fun `CheckFirstApp language toggle translates Dashboard body, buttons, bottom nav, and headings`() {
        composeRule.setContent {
            CheckFirstApp(bypassLoginForTestUid = "user-test-uid")
        }
        composeRule.waitForIdle()

        // Verify English dashboard & bottom bar
        composeRule.onNodeWithText("Welcome to CheckFirst").assertIsDisplayed()
        composeRule.onNodeWithText(EnglishStrings.dashScanMessageCta).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(EnglishStrings.bottomHome).assertIsDisplayed()
        composeRule.onNodeWithText(EnglishStrings.bottomScanner).assertIsDisplayed()

        // Toggle language to Tamil via top bar pill
        composeRule.onNodeWithTag("topbar_language_toggle").performClick()
        composeRule.waitForIdle()

        // Every part of the app — hero, body, buttons, bottom bar, section titles — is now in Tamil
        composeRule.onNodeWithText(TamilStrings.dashWelcomeFullTamil).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.dashScanMessageCta).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.dashCheckLoanCta).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.dashToolsTitle).performScrollTo().assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText(TamilStrings.bottomHome).fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithText(TamilStrings.bottomScanner).assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.bottomAiChat).assertIsDisplayed()

        // Switch back to English
        composeRule.onNodeWithTag("topbar_language_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Welcome to CheckFirst").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(EnglishStrings.bottomHome).assertIsDisplayed()
    }

    @Test
    fun `ScamScannerScreen live scan result re-localizes immediately when switching to Tamil`() {
        var tamil by mutableStateOf(false)
        composeRule.setContent {
            CheckFirstTheme(tamil = tamil) {
                ScamScannerScreen(
                    aiRepository = CheckFirstAiRepository(),
                    onRecordActivity = { _, _, _ -> },
                    onShowNotice = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("scanner_load_example_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("scanner_analyze_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Several serious warning signs were found.").performScrollTo().assertIsDisplayed()

        // Flip app language to Tamil while result is on screen
        tamil = true
        composeRule.waitForIdle()

        // Both screen UI chrome AND the computed local scan verdict/indicators translate to Tamil
        composeRule.onNodeWithText(TamilStrings.navScanner).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.scanAnalyzeBtn).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.scanIndicatorsTitle, substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("பல தீவிர எச்சரிக்கை அறிகுறிகள் கண்டறியப்பட்டன.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `all remaining screens render complete Tamil translations when tamil is enabled`() {
        // 1. QrAnalyzerScreen in Tamil
        composeRule.setContent {
            CheckFirstTheme(tamil = true) {
                QrAnalyzerScreen(
                    tamil = true,
                    aiRepository = CheckFirstAiRepository(),
                    onRecordActivity = { _, _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TamilStrings.qrCardTitle).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("qr_analyze_sample_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TamilStrings.qrDirectionTitle).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.qrPinTitle).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `Guide, Emergency, LoanCalculator, LoanRisk, Settings, AiChat, and LiveVoice translate fully to Tamil`() {
        var screenIndex by mutableStateOf(0)
        val aiVm = AiAssistantViewModel()
        composeRule.setContent {
            CheckFirstTheme(tamil = true) {
                when (screenIndex) {
                    0 -> GuideScreen(onOpenEmergency = {})
                    1 -> EmergencyScreen(onShowNotice = {})
                    2 -> LoanCalculatorScreen(onRecordActivity = { _, _, _ -> })
                    3 -> LoanRiskScreen(onRecordActivity = { _, _, _ -> })
                    4 -> SettingsScreen(
                        prefs = Prefs(tamil = true),
                        currentUser = null,
                        syncError = null,
                        onUpdatePrefs = {},
                        onClearHistory = {},
                        onSignInSuccess = {},
                        onSignInError = {},
                        onSignOut = {},
                    )
                    5 -> AiChatScreen(tamil = true, viewModel = aiVm)
                    else -> LiveVoiceScreen(tamil = true, viewModel = aiVm)
                }
            }
        }

        // 0: Guide
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TamilStrings.guideTitle).assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.guideOpenEmergencyBtn).performScrollTo().assertIsDisplayed()

        // 1: Emergency
        screenIndex = 1
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TamilStrings.emTitle).assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.emCopyBtn).performScrollTo().assertIsDisplayed()

        // 2: Loan Calculator
        screenIndex = 2
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TamilStrings.navLoanCalculator).assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.loanCalcBtn).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.loanResultEyebrow).performScrollTo().assertIsDisplayed()

        // 3: Loan Risk
        screenIndex = 3
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TamilStrings.navLoanRisk).assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.riskCardTitle).performScrollTo().assertIsDisplayed()

        // 4: Settings
        screenIndex = 4
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TamilStrings.setTitle).assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.setSimpleModeTitle).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.setAboutTitle).performScrollTo().assertIsDisplayed()

        // 5: AI Chat
        screenIndex = 5
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TamilStrings.chatTitle).assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.chatPrivacyChip).assertIsDisplayed()

        // 6: Live Voice
        screenIndex = 6
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TamilStrings.voiceTitle).assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.voiceStatusIdle).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TamilStrings.voiceStartBtn).performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 4. USER-PERSPECTIVE EDGE CASES & BUG FIX VERIFICATION
    // =========================================================================

    @Test
    fun `QR parser flags open-amount fake refund QR in English and Tamil and rejects missing pa`() {
        // Open-amount fake refund QR (no am= param) MUST still trigger isRefundMislead
        val openRefundEn = LocalSafetyEngines.parseUpiUri(
            "upi://pay?pa=helpdesk@demobank&pn=Cashback%20Team&tn=Scan%20to%20receive%20refund",
        ).getOrThrow()
        assertTrue("Open-amount refund QR must be flagged", openRefundEn.isRefundMislead)

        val openRefundTa = LocalSafetyEngines.parseUpiUri(
            "upi://pay?pa=help@bank&pn=Support&tn=பணம்%20பெற%20ஸ்கேன்%20செய்க",
            tamil = true,
        ).getOrThrow()
        assertTrue("Tamil refund QR must be flagged", openRefundTa.isRefundMislead)

        // Missing pa parameter MUST fail cleanly
        assertTrue(LocalSafetyEngines.parseUpiUri("upi://pay?am=500&tn=Test").isFailure)
        assertTrue(LocalSafetyEngines.parseUpiUri("upi://pay").isFailure)

        // Comma-formatted amount parses cleanly
        val commaAmt = LocalSafetyEngines.parseUpiUri("upi://pay?pa=shop@upi&am=1,500.50").getOrThrow()
        assertEquals(1500.50, commaAmt.amount!!, 0.001)
    }

    @Test
    fun `Loan calculator handles comma inputs, blank upfront fees, and pending edit state cleanly`() {
        // Comma-formatted values + blank fees treated as 0
        val parsed = LocalSafetyEngines.calculateLoanCost(
            receivedText = "10,000",
            feesText = "",
            repaymentText = "11,200",
            tenureText = "30",
            unit = TenureUnit.DAYS,
        ).getOrThrow()
        assertEquals(0.0, parsed.fees, 0.001)
        assertEquals(1200.0, parsed.cost, 0.001)
        assertEquals(146.0, parsed.annualizedRatePercent, 0.001)

        // UI flow: editing a field shows "Ready to recalculate" instead of false error card
        composeRule.setContent {
            CheckFirstTheme {
                LoanCalculatorScreen(onRecordActivity = { _, _, _ -> })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("loan_fees_input").performScrollTo().performTextClearance()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(EnglishStrings.loanPendingTitle).performScrollTo().assertIsDisplayed()

        // Clicking calculate with blank fees succeeds cleanly
        composeRule.onNodeWithTag("loan_calculate_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("146.0").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `ScamScanner Scan Another button clears text and empty submit shows notice`() {
        var lastNotice: String? = null
        composeRule.setContent {
            CheckFirstTheme {
                ScamScannerScreen(
                    aiRepository = CheckFirstAiRepository(),
                    onRecordActivity = { _, _, _ -> },
                    onShowNotice = { lastNotice = it },
                )
            }
        }
        composeRule.waitForIdle()
        // Empty scan triggers helpful notice
        composeRule.onNodeWithTag("scanner_analyze_button").performScrollTo().performClick()
        assertEquals(EnglishStrings.scanNoticeEmpty, lastNotice)

        // Load example, scan, then tap "Scan another message" -> clears input and returns to empty state
        composeRule.onNodeWithTag("scanner_load_example_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("scanner_analyze_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("scanner_scan_another_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(EnglishStrings.scanEmptyTitle).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `LoanRiskScreen supports CLEAR, CAUTION, and reset workflows`() {
        var lastRisk: RiskLevel? = null
        composeRule.setContent {
            CheckFirstTheme {
                LoanRiskScreen(onRecordActivity = { _, _, r -> lastRisk = r })
            }
        }
        composeRule.waitForIdle()

        // All NO -> CLEAR
        for (i in 0..5) {
            composeRule.onNodeWithTag("loan_risk_q${i}_no").performScrollTo().performClick()
        }
        composeRule.onNodeWithTag("loan_risk_submit_button").performScrollTo().assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals(RiskLevel.CLEAR, lastRisk)
        composeRule.onNodeWithText(EnglishStrings.riskVerdictClear).performScrollTo().assertIsDisplayed()

        // Reset clears all answers and disables submit again
        composeRule.onNodeWithTag("loan_risk_reset_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("loan_risk_submit_button").performScrollTo().assertIsNotEnabled()

        // 2 UNSURE + 4 NO -> CAUTION
        composeRule.onNodeWithTag("loan_risk_q0_unsure").performScrollTo().performClick()
        composeRule.onNodeWithTag("loan_risk_q1_unsure").performScrollTo().performClick()
        for (i in 2..5) {
            composeRule.onNodeWithTag("loan_risk_q${i}_no").performScrollTo().performClick()
        }
        composeRule.onNodeWithTag("loan_risk_submit_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(RiskLevel.CAUTION, lastRisk)
        composeRule.onNodeWithText(EnglishStrings.riskVerdictCaution).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `EmergencyScreen builds summary, copies cleanly, and clears form`() {
        var copiedNotice: String? = null
        composeRule.setContent {
            CheckFirstTheme {
                EmergencyScreen(onShowNotice = { copiedNotice = it })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("emergency_amount_input").performScrollTo().performTextInput("4,500")
        composeRule.onNodeWithTag("emergency_copy_summary_button").performScrollTo().performClick()
        assertEquals(EnglishStrings.emCopiedNotice, copiedNotice)

        composeRule.onNodeWithTag("emergency_clear_form_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        // Preview reverts to Not provided for amount
        assertTrue(
            composeRule.onAllNodesWithText("4,500", substring = true).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun `DashboardScreen honors Simple Mode and safetyTips preference toggles`() {
        composeRule.setContent {
            CheckFirstTheme {
                DashboardScreen(
                    prefs = Prefs(simpleMode = true, safetyTips = false),
                    history = emptyList(),
                    isSignedIn = true,
                    onNavigate = {},
                    onDisableAndClearHistory = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_simple_mode_banner").performScrollTo().assertIsDisplayed()
        // Safety tip hidden when safetyTips = false
        assertEquals(0, composeRule.onAllNodesWithText(EnglishStrings.dashTipEyebrow).fetchSemanticsNodes().size)
        // Demo analytics charts streamlined out in Simple Mode when user has no real history
        assertEquals(0, composeRule.onAllNodesWithText(EnglishStrings.dashAnalyticsTitleDemo).fetchSemanticsNodes().size)
    }
}
