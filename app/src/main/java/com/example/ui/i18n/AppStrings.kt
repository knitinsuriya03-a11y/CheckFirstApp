package com.example.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.domain.LoanRiskAnswer
import com.example.domain.RiskLevel
import com.example.domain.TenureUnit

data class GuideStepCopy(
    val title: String,
    val body: String,
    val detail: String,
)

/**
 * Interface-backed localization contract (avoids JVM's 255-parameter constructor limit)
 * implemented by [EnglishStrings] and [TamilStrings] and provided via [LocalStrings].
 */
interface AppStrings {
    val isTamil: Boolean

    // Brand & Navigation
    val brandTagline: String
    val brandFooter: String
    val navDashboard: String
    val navScanner: String
    val navQrAnalyzer: String
    val navGuide: String
    val navEmergency: String
    val navLoanCalculator: String
    val navLoanRisk: String
    val navAiChat: String
    val navLiveVoice: String
    val navSettings: String
    val bottomHome: String
    val bottomScanner: String
    val bottomQrUpi: String
    val bottomAiChat: String
    val bottomVoice: String
    val groupBorrowClarity: String
    val groupAiCompanion: String
    val groupPreferences: String
    val drawerUrgentTitle: String
    val drawerUrgentBody: String
    val drawerUrgentCta: String
    val openNavMenuDesc: String
    val toggleLangDesc: String
    val accountMenuDesc: String
    val menuSignedIn: String
    val menuPreferences: String
    val menuPreferencesPrivacy: String
    val menuSignOut: String
    val menuSignInGoogle: String
    val noticeSwitchedToTamil: String
    val noticeSwitchedToEnglish: String
    val noticeSignedIn: String
    val noticeSignedOut: String
    val noticeHistoryCleared: String
    val noticeHistoryDisabledCleared: String
    val noticeCheckCompleteSuffix: String

    // Login Gate Screen
    val loginEyebrow: String
    val loginTitle: String
    val loginSubtitle: String
    val loginButtonGoogle: String
    val loginContinueAsPrefix: String
    val loginSwitchAccount: String
    val loginSignedInAs: String
    val loginWhyTitle: String
    val loginBenefit1: String
    val loginBenefit2: String
    val loginBenefit3: String
    val loginPrivacyNote: String
    val loginRequiredNote: String

    // Shared Components
    val badgeDanger: String
    val badgeCaution: String
    val badgeClear: String
    val badgeHighConcern: String
    val badgeReview: String
    val statusDisclaimer: String
    val aiInsightButton: String
    val aiInsightHelper: String
    val aiInsightLoading: String
    val aiInsightRetry: String
    val aiInsightHeader: String
    val aiInsightWatchOut: String
    val aiInsightNextSteps: String
    val aiInsightAutoSwitchedPrefix: String
    val aiInsightModelPrefix: String
    val checksUnit: String

    // Dashboard
    val dashEyebrow: String
    val dashWelcomePrefix: String
    val dashWelcomeFullTamil: String
    val dashHeroSubtitle: String
    val dashScanMessageCta: String
    val dashCheckLoanCta: String
    val dashPromiseTitle: String
    val dashPromiseSubtitle: String
    val dashAiChatTitle: String
    val dashAiChatSubtitle: String
    val dashVoiceTitle: String
    val dashVoiceSubtitle: String
    val dashToolsTitle: String
    val dashToolsNote: String
    val dashTool1Num: String
    val dashTool1Desc: String
    val dashTool1Cta: String
    val dashTool2Num: String
    val dashTool2Desc: String
    val dashTool2Cta: String
    val dashTool3Num: String
    val dashTool3Desc: String
    val dashTool3Cta: String
    val dashTool4Num: String
    val dashTool4Desc: String
    val dashTool4Cta: String
    val dashAwarenessTitle: String
    val dashAwarenessAlert: String
    val dashAwarenessSubhead: String
    val dashAwarenessBullets: List<String>
    val dashTipEyebrow: String
    val dashTipHeadline: String
    val dashTipBody: String
    val dashTipCta: String
    val dashAnalyticsTitleReal: String
    val dashAnalyticsTitleDemo: String
    val dashAnalyticsNoteSynced: String
    val dashAnalyticsNoteLocal: String
    val dashAnalyticsNoteDemo: String
    val dashStatTotalReal: String
    val dashStatTotalDemo: String
    val dashStatNoteOptIn: String
    val dashStatNoteDemo: String
    val dashStatDanger: String
    val dashStatDangerNoteReal: String
    val dashStatSampleChecks: String
    val dashStatCaution: String
    val dashStatCautionNoteReal: String
    val dashStatClear: String
    val dashStatClearNote: String
    val dashDemoDisclaimer: String
    val dashDonutTitle: String
    val dashDonutSubReal: String
    val dashDonutSubDemo: String
    val dashDonutFooter: String
    val dashSliceDanger: String
    val dashSliceCaution: String
    val dashSliceClear: String
    val dashWeeklyTitle: String
    val dashWeeklySub: String
    val dashWeeklyTag: String
    val dashWeeklyFooter: String
    val dashWeekDays: List<String>
    val dashWarningsTitle: String
    val dashWarningsSub: String
    val dashWarningsFooter: String
    val dashWarningLabels: List<String>
    val dashHistoryTitleSynced: String
    val dashHistoryTitleLocal: String
    val dashHistoryTitleDemo: String
    val dashHistorySubSynced: String
    val dashHistorySubLocal: String
    val dashHistorySubDemo: String
    val dashPrivacyLink: String
    val dashTurnOffClearHistory: String
    val dashSimpleModeBanner: String

    // Scam Scanner
    val scanEyebrow: String
    val scanSubtitle: String
    val scanPrivacyChip: String
    val scanCardTitle: String
    val scanCardSubtitle: String
    val scanMaxTag: String
    val scanInputLabel: String
    val scanInputPlaceholder: String
    val scanCredentialNote: String
    val scanCharCountSuffix: String
    val scanLoadExample: String
    val scanSamplesLabel: String
    val scanSampleRefundPin: String
    val scanSampleDigitalArrest: String
    val scanSampleGenuineOtp: String
    val scanSampleTanglish: String
    val scanLanguageLabel: String
    val scanLangEnglish: String
    val scanLangTamil: String
    val scanLangMixed: String
    val scanAnalyzeBtn: String
    val scanClearBtn: String
    val scanNoticeEmpty: String
    val scanNoticeTooLong: String
    val scanResultTitle: String
    val scanResultSubPrefix: String
    val scanLocalCheckTag: String
    val scanNoPatternReason: String
    val scanIndicatorsTitle: String
    val scanNoIndicators: String
    val scanOriginalMsgTitle: String
    val scanAnotherBtn: String
    val scanMoreDetails: String
    val scanHideDetails: String
    val scanRecommendedActionLabel: String
    val scanConfidenceLabel: String
    val scanOnlineCheckLabel: String
    val scanOnlineCheckNo: String
    val scanRuleSetLabel: String
    val scanMissingInfoTitle: String
    val scanReasonPrefix: String
    val scanLimitationPrefix: String
    val verdictHighRisk: String
    val verdictCaution: String
    val verdictLikelyLegitimate: String
    val verdictUnableToAssess: String
    val scanEmptyTitle: String
    val scanEmptySub: String
    val scanEmptyBadge: String
    val scanHowTitle: String
    val scanHowSteps: List<String>
    val scanHowFooter: String
    val scanLooksForTitle: String
    val scanLooksForPills: List<String>

    // QR & Payment Link Analyzer
    val qrEyebrow: String
    val qrSubtitle: String
    val qrCardTitle: String
    val qrCardSubtitle: String
    val qrImageSelected: String
    val qrPickPrompt: String
    val qrPickSub: String
    val qrPickCta: String
    val qrOrPaste: String
    val qrLinkLabel: String
    val qrLinkHelper: String
    val qrAnalyzeBtn: String
    val qrDetailsTitle: String
    val qrDetailsSub: String
    val qrDecodedTag: String
    val qrReviewBanner: String
    val qrDetailsBanner: String
    val qrAmountNotSpecified: String
    val qrRequestedAmount: String
    val qrFieldPayeeName: String
    val qrFieldUpiAddress: String
    val qrFieldNote: String
    val qrFieldCurrency: String
    val qrFieldMode: String
    val qrDirectionTitle: String
    val qrDirectionDescPrefix: String
    val qrDirectionDescSuffix: String
    val qrRefundWarning: String
    val qrPinTitle: String
    val qrPinBody: String
    val qrDisclaimer: String
    val qrEmptyTitle: String
    val qrEmptySub: String
    val qrTryExample: String
    val qrLoadSample: String
    val qrAnalyzeSample: String
    val qrKnowTitle: String
    val qrKnowItems: List<String>
    val qrKnowFooter: String
    val qrRememberTitle: String
    val qrRememberBody: String
    val qrErrUnsupportedFormat: String
    val qrErrReadFailed: String
    val qrErrTooLarge: String
    val qrErrDecodeFailed: String

    // Pause & Verify Guide
    val guideEyebrow: String
    val guideTitle: String
    val guideSubtitle: String
    val guideSteps: List<GuideStepCopy>
    val guideVerifyTitle: String
    val guideVerifyBody: String
    val guideScamsTitle: String
    val guideScamsNote: String
    val guideScams: List<Pair<String, String>>
    val guideAlreadySentTitle: String
    val guideAlreadySentBody: String
    val guideOpenEmergencyBtn: String

    // Emergency Help
    val emEyebrow: String
    val emTitle: String
    val emSubtitle: String
    val emBannerEyebrow: String
    val emBannerTitle: String
    val emBannerSub: String
    val emHelplineLabel: String
    val emCallHelplineCta: String
    val emDialFallbackNotice: String
    val emPortalLabel: String
    val emOpenPortalCta: String
    val emPortalFallbackNotice: String
    val emStepsTitle: String
    val emActions: List<Pair<String, String>>
    val emDisclaimer: String
    val emSummaryTitle: String
    val emSummarySub: String
    val emFieldDateLabel: String
    val emFieldDatePlaceholder: String
    val emFieldAmountLabel: String
    val emFieldAmountPlaceholder: String
    val emFieldUtrLabel: String
    val emFieldUtrPlaceholder: String
    val emFieldBankLabel: String
    val emFieldBankPlaceholder: String
    val emFieldDescLabel: String
    val emFieldDescPlaceholder: String
    val emCopyBtn: String
    val emClearFormBtn: String
    val emCopiedNotice: String
    val emPreviewLabel: String
    val emNeverEnterNote: String
    val emNotProvided: String
    val emSummaryHeader: String
    val emSummaryFooter: String

    // Loan Cost Calculator
    val loanEyebrow: String
    val loanSubtitle: String
    val loanFormTitle: String
    val loanFormSub: String
    val loanDemoTag: String
    val loanReceivedLabel: String
    val loanReceivedSupport: String
    val loanFeesLabel: String
    val loanFeesSupport: String
    val loanRepaymentLabel: String
    val loanRepaymentSupport: String
    val loanTenureLabel: String
    val loanUnitLabel: String
    val loanCalcBtn: String
    val loanResultEyebrow: String
    val loanNotAprTag: String
    val loanMetricReceived: String
    val loanMetricRepayment: String
    val loanMetricCost: String
    val loanPer100Format: String
    val loanComparisonTitle: String
    val loanComparisonSub: String
    val loanComparisonNote: String
    val loanFormulaTitle: String
    val loanFormulaBody: String
    val loanFormulaDisclaimer: String
    val loanInvalidTitle: String
    val loanInvalidSub: String
    val loanPendingTitle: String
    val loanPendingSub: String
    val loanHowTitle: String
    val loanHowBody: String

    // Loan Risk Checker
    val riskEyebrow: String
    val riskSubtitle: String
    val riskCardTitle: String
    val riskCardSub: String
    val riskTag: String
    val riskSubmitPrefix: String
    val riskSubmitSuffix: String
    val riskLenderNote: String
    val riskVerdictDanger: String
    val riskVerdictCaution: String
    val riskVerdictClear: String
    val riskReasonYes: String
    val riskReasonUnsure: String
    val riskReasonNone: String
    val riskActionSuffix: String
    val riskBreakdownTitle: String
    val riskBadgeFlagged: String
    val riskBadgeCheck: String
    val riskBadgeClear: String
    val riskChecklistDisclaimer: String
    val riskResetBtn: String
    val riskEmptyTitle: String
    val riskEmptySub: String
    val riskProgressSuffix: String
    val riskPauseTitle: String
    val riskPauseBody: String
    val riskImportantTitle: String
    val riskImportantBody: String

    // Settings
    val setEyebrow: String
    val setTitle: String
    val setSubtitle: String
    val setLangAccessTitle: String
    val setLangPrefTitle: String
    val setLangPrefSub: String
    val setSimpleModeTitle: String
    val setSimpleModeSub: String
    val setLargeTextTitle: String
    val setLargeTextSub: String
    val setNotificationsTitle: String
    val setSafetyTipsTitle: String
    val setSafetyTipsSub: String
    val setAlertsTitle: String
    val setAlertsSub: String
    val setAccountTitle: String
    val setAccountSubSignedIn: String
    val setAccountSubSignedOut: String
    val setGoogleAccountFallback: String
    val setPrivacyTitle: String
    val setHistoryTitleSignedIn: String
    val setHistoryTitleLocal: String
    val setHistorySub: String
    val setHistoryEnabledSynced: String
    val setHistoryEnabledLocal: String
    val setPromiseTitle: String
    val setPromiseBody: String
    val setProcessedTitle: String
    val setProcessedBody1: String
    val setProcessedBody2: String
    val setProcessedBullets: List<String>
    val setAboutTitle: String
    val setAboutBody: String
    val setAboutTag: String

    // AI Chat
    val chatEyebrow: String
    val chatTitle: String
    val chatResetDesc: String
    val chatHeroTitle: String
    val chatHeroModelPrefix: String
    val chatHeroBody: String
    val chatPrivacyChip: String
    val chatTryAsking: String
    val chatStarterPrompts: List<String>
    val chatPlaceholder: String
    val chatSendDesc: String
    val chatThinking: String
    val chatStreaming: String

    // Live Voice
    val voiceEyebrow: String
    val voiceTitle: String
    val voiceSubtitle: String
    val voiceTagReady: String
    val voiceTagConnecting: String
    val voiceTagActive: String
    val voiceTagStopped: String
    val voiceStatusIdle: String
    val voiceStatusConnecting: String
    val voiceStatusActive: String
    val voiceStatusEnded: String
    val voiceStatusTimeout: String
    val voiceStatusMicDenied: String
    val voiceStatusUnavailable: String
    val voiceDesc: String
    val voiceEndBtn: String
    val voiceStartBtn: String
    val voiceMicPermissionError: String
    val voiceSituationsTitle: String
    val voiceSituations: List<String>
    val voicePrivacyTitle: String
    val voicePrivacyBody: String

    fun riskBadgeLabel(risk: RiskLevel): String = when (risk) {
        RiskLevel.DANGER -> badgeDanger
        RiskLevel.CAUTION -> badgeCaution
        RiskLevel.CLEAR -> badgeClear
    }

    fun tenureUnitLabel(unit: TenureUnit): String = when (unit) {
        TenureUnit.DAYS -> if (isTamil) "நாட்கள்" else "Days"
        TenureUnit.MONTHS -> if (isTamil) "மாதங்கள்" else "Months"
        TenureUnit.YEARS -> if (isTamil) "ஆண்டுகள்" else "Years"
    }

    fun loanRiskAnswerLabel(answer: LoanRiskAnswer): String = when (answer) {
        LoanRiskAnswer.YES -> if (isTamil) "ஆம்" else "Yes"
        LoanRiskAnswer.NO -> if (isTamil) "இல்லை" else "No"
        LoanRiskAnswer.UNSURE -> if (isTamil) "உறுதியில்லை" else "Not sure"
    }

    fun navLabelForRoute(route: String): String = when (route) {
        "dashboard" -> navDashboard
        "scanner" -> navScanner
        "qr-analyzer" -> navQrAnalyzer
        "guide" -> navGuide
        "emergency" -> navEmergency
        "loan-calculator" -> navLoanCalculator
        "loan-risk" -> navLoanRisk
        "ai-chat" -> navAiChat
        "live-voice" -> navLiveVoice
        "settings" -> navSettings
        else -> navDashboard
    }

    fun activityKindLabel(kind: String): String {
        if (!isTamil) return kind
        return when (kind) {
            "Message scan" -> "செய்தி சோதனை"
            "UPI link analysis" -> "UPI இணைப்பு பகுப்பாய்வு"
            "Link analysis" -> "இணைப்பு பகுப்பாய்வு"
            "Loan cost calculation" -> "கடன் செலவு கணக்கீடு"
            "Loan risk check" -> "கடன் அபாய சோதனை"
            "Loan check" -> "கடன் சோதனை"
            else -> kind
        }
    }

    fun activityTitleLabel(label: String): String {
        if (!isTamil) return label
        return when (label) {
            "Bank refund message" -> "வங்கி பணம் திரும்பப் பெறும் செய்தி"
            "Short-term loan offer" -> "குறுகிய கால கடன் சலுகை"
            "Payment link preview" -> "கட்டண இணைப்பு முன்னோட்டம்"
            "Message scan result" -> "செய்தி சோதனை முடிவு"
            "Payment request inspected" -> "கட்டண கோரிக்கை ஆய்வு"
            "Loan cost estimate" -> "கடன் செலவு மதிப்பீடு"
            "Loan offer assessment" -> "கடன் சலுகை மதிப்பீடு"
            else -> label
        }
    }

    fun activityDateLabel(date: String): String {
        if (!isTamil) return date
        return date
            .replace("Sample · ", "மாதிரி · ")
            .replace("Yesterday", "நேற்று")
    }
}

val LocalStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }
