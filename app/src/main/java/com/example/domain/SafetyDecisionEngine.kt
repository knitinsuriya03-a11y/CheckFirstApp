package com.example.domain

/**
 * Shared four-outcome classification used across CheckFirst safety tools
 * (Scam Shield, Link Analyzer, QR Analyzer, and Loan Risk Checker).
 *
 * Deliberately avoids claiming "100% safe" or "guaranteed genuine".
 */
enum class SafetyClassification(val wireValue: String) {
    LIKELY_LEGITIMATE("likely_legitimate"),
    CAUTION("caution"),
    HIGH_RISK("high_risk"),
    UNABLE_TO_ASSESS("unable_to_assess");

    fun displayLabel(tamil: Boolean): String = when (this) {
        LIKELY_LEGITIMATE -> if (tamil) "பெரிய அபாய அறிகுறிகள் எதுவும் இல்லை" else "No major red flags detected"
        CAUTION -> if (tamil) "சந்தேகமான அறிகுறிகள் கண்டறியப்பட்டன" else "Suspicious indicators found"
        HIGH_RISK -> if (tamil) "அதிக அபாயம் — மோசடியாக இருக்கலாம்" else "High risk — possible scam"
        UNABLE_TO_ASSESS -> if (tamil) "போதுமான தகவல் இல்லை" else "Insufficient information"
    }

    /**
     * Maps the 4-state safety verdict to the 3-level metadata risk wire value
     * stored in opt-in check history without altering Firestore schema rules.
     */
    fun toRiskLevel(): RiskLevel = when (this) {
        HIGH_RISK -> RiskLevel.DANGER
        CAUTION, UNABLE_TO_ASSESS -> RiskLevel.CAUTION
        LIKELY_LEGITIMATE -> RiskLevel.CLEAR
    }
}

enum class EvidenceStrength(val rank: Int) {
    CRITICAL(5),
    STRONG(4),
    MEDIUM(3),
    WEAK(2),
    INFORMATIONAL(1);

    fun displayLabel(tamil: Boolean): String = when (this) {
        CRITICAL -> if (tamil) "மிகத் தீவிரம்" else "CRITICAL"
        STRONG -> if (tamil) "வலுவானது" else "STRONG"
        MEDIUM -> if (tamil) "நடுத்தரம்" else "MEDIUM"
        WEAK -> if (tamil) "பலவீனம்" else "WEAK"
        INFORMATIONAL -> if (tamil) "தகவல்" else "INFO"
    }

    fun toRiskLevel(): RiskLevel = when (this) {
        CRITICAL, STRONG -> RiskLevel.DANGER
        MEDIUM, WEAK -> RiskLevel.CAUTION
        INFORMATIONAL -> RiskLevel.CLEAR
    }
}

enum class EvidencePolarity {
    RISK,
    POSITIVE,
    NEUTRAL,
}

/**
 * Qualitative confidence category — never an invented numerical probability.
 */
enum class AssessmentConfidence {
    HIGH,
    MODERATE,
    LOW,
    INSUFFICIENT_DATA;

    fun displayLabel(tamil: Boolean): String = when (this) {
        HIGH -> if (tamil) "அதிக உறுதி" else "High"
        MODERATE -> if (tamil) "மிதமான உறுதி" else "Moderate"
        LOW -> if (tamil) "குறைந்த உறுதி" else "Low"
        INSUFFICIENT_DATA -> if (tamil) "தரவு போதாது" else "Insufficient data"
    }
}

/**
 * Structured evidence item emitted by any CheckFirst deterministic safety rule.
 */
data class SafetyEvidence(
    val ruleId: String,
    val featureName: String,
    val categoryId: String,
    val title: String,
    val description: String,
    val strength: EvidenceStrength,
    val triggerReason: String,
    val exceptionOrLimitation: String,
    val recommendedAction: String,
    val polarity: EvidencePolarity = EvidencePolarity.RISK,
    val matchedSpans: List<String> = emptyList(),
)

/**
 * Structured deterministic safety assessment returned by [SafetyDecisionEngine].
 */
data class SafetyAssessment(
    val featureName: String,
    val classification: SafetyClassification,
    val verdictLabel: String,
    val findings: List<SafetyEvidence>,
    val confidence: AssessmentConfidence,
    val missingInformation: List<String>,
    val recommendedNextAction: String,
    val onlineVerificationPerformed: Boolean = false,
    val ruleSetVersion: String = SafetyDecisionEngine.RULE_SET_VERSION,
) {
    val riskFindings: List<SafetyEvidence>
        get() = findings.filter { it.polarity == EvidencePolarity.RISK }

    val primaryFindings: List<SafetyEvidence>
        get() = riskFindings.take(3).ifEmpty { findings.take(2) }

    val secondaryFindings: List<SafetyEvidence>
        get() {
            val primaryIds = primaryFindings.map { it.ruleId }.toSet()
            return findings.filterNot { it.ruleId in primaryIds }
        }
}

/**
 * Shared deterministic safety decision engine.
 *
 * Core decision principles:
 * 1. Critical safety rules ALWAYS produce [SafetyClassification.HIGH_RISK] and override
 *    any weak or strong positive signals — a positive cue never cancels a critical warning.
 * 2. Weak risk indicators alone NEVER produce [SafetyClassification.HIGH_RISK].
 * 3. Multiple independent supporting indicators (distinct rules/categories) can escalate
 *    a finding from [SafetyClassification.CAUTION] to [SafetyClassification.HIGH_RISK].
 * 4. Missing or fragmentary context without clear signals yields [SafetyClassification.UNABLE_TO_ASSESS].
 * 5. "No major red flags detected" is never presented as a guarantee of legitimacy.
 */
object SafetyDecisionEngine {

    const val RULE_SET_VERSION = "checkfirst-safety-rules/1.0"

    fun evaluate(
        featureName: String,
        findings: List<SafetyEvidence>,
        missingInformation: List<String> = emptyList(),
        insufficientContext: Boolean = false,
        hasContradictorySignals: Boolean = false,
        onlineVerificationPerformed: Boolean = false,
        tamil: Boolean = false,
    ): SafetyAssessment {
        val sortedFindings = findings.sortedWith(
            compareBy<SafetyEvidence> {
                when (it.polarity) {
                    EvidencePolarity.RISK -> 0
                    EvidencePolarity.NEUTRAL -> 1
                    EvidencePolarity.POSITIVE -> 2
                }
            }.thenByDescending { it.strength.rank },
        )

        val riskFindings = sortedFindings.filter { it.polarity == EvidencePolarity.RISK }
        val positiveFindings = sortedFindings.filter { it.polarity == EvidencePolarity.POSITIVE }

        val criticalRisks = riskFindings.filter { it.strength == EvidenceStrength.CRITICAL }
        val strongRisks = riskFindings.filter { it.strength == EvidenceStrength.STRONG }
        val mediumRisks = riskFindings.filter { it.strength == EvidenceStrength.MEDIUM }
        val weakRisks = riskFindings.filter { it.strength == EvidenceStrength.WEAK }

        val distinctStrongCategories = strongRisks.map { it.categoryId }.distinct().size
        val distinctMediumCategories = mediumRisks.map { it.categoryId }.distinct().size
        val distinctWeakCategories = weakRisks.map { it.categoryId }.distinct().size
        val distinctAllRiskCategories = riskFindings.map { it.categoryId }.distinct().size

        val (classification, confidence) = when {
            // Rule 1: Critical safety findings ALWAYS override positive signals and yield HIGH_RISK.
            criticalRisks.isNotEmpty() -> {
                SafetyClassification.HIGH_RISK to AssessmentConfidence.HIGH
            }

            // Rule 2: Multiple independent STRONG indicators escalate to HIGH_RISK.
            distinctStrongCategories >= 2 || strongRisks.size >= 2 -> {
                SafetyClassification.HIGH_RISK to AssessmentConfidence.HIGH
            }

            // Rule 3: One STRONG indicator supported by an independent MEDIUM indicator
            // or two independent WEAK indicators escalates to HIGH_RISK.
            strongRisks.isNotEmpty() && (
                (mediumRisks.any { m -> strongRisks.none { s -> s.categoryId == m.categoryId } }) ||
                    distinctWeakCategories >= 2
                ) -> {
                SafetyClassification.HIGH_RISK to AssessmentConfidence.MODERATE
            }

            // Rule 4: Three or more independent MEDIUM indicators escalate to HIGH_RISK.
            distinctMediumCategories >= 3 -> {
                SafetyClassification.HIGH_RISK to AssessmentConfidence.MODERATE
            }

            // Rule 5: Single STRONG or 1–2 MEDIUM indicators yield CAUTION.
            strongRisks.isNotEmpty() || mediumRisks.isNotEmpty() -> {
                SafetyClassification.CAUTION to AssessmentConfidence.MODERATE
            }

            // Rule 6: Insufficient context with no strong/medium/critical risks -> UNABLE_TO_ASSESS.
            insufficientContext && riskFindings.isEmpty() -> {
                SafetyClassification.UNABLE_TO_ASSESS to AssessmentConfidence.INSUFFICIENT_DATA
            }

            // Rule 7: Weak indicators alone NEVER produce HIGH_RISK; at most CAUTION (LOW confidence).
            weakRisks.isNotEmpty() -> {
                val conf = if (distinctAllRiskCategories >= 2 && !hasContradictorySignals) {
                    AssessmentConfidence.MODERATE
                } else {
                    AssessmentConfidence.LOW
                }
                SafetyClassification.CAUTION to conf
            }

            // Rule 8: Contradictory signals without clear risk -> CAUTION (LOW confidence).
            hasContradictorySignals -> {
                SafetyClassification.CAUTION to AssessmentConfidence.LOW
            }

            // Rule 9: Insufficient context with unresolved missing info -> UNABLE_TO_ASSESS.
            insufficientContext -> {
                SafetyClassification.UNABLE_TO_ASSESS to AssessmentConfidence.INSUFFICIENT_DATA
            }

            // Rule 10: No risk indicators detected -> LIKELY_LEGITIMATE ("No major red flags detected").
            else -> {
                val conf = if (positiveFindings.isNotEmpty()) {
                    AssessmentConfidence.MODERATE
                } else {
                    AssessmentConfidence.LOW
                }
                SafetyClassification.LIKELY_LEGITIMATE to conf
            }
        }

        val recommendedNextAction = selectRecommendedAction(
            classification = classification,
            riskFindings = riskFindings,
            tamil = tamil,
        )

        return SafetyAssessment(
            featureName = featureName,
            classification = classification,
            verdictLabel = classification.displayLabel(tamil),
            findings = sortedFindings,
            confidence = confidence,
            missingInformation = missingInformation.distinct(),
            recommendedNextAction = recommendedNextAction,
            onlineVerificationPerformed = onlineVerificationPerformed,
            ruleSetVersion = RULE_SET_VERSION,
        )
    }

    private fun selectRecommendedAction(
        classification: SafetyClassification,
        riskFindings: List<SafetyEvidence>,
        tamil: Boolean,
    ): String {
        val topRisk = riskFindings.firstOrNull()
        if (topRisk != null && classification == SafetyClassification.HIGH_RISK) {
            return topRisk.recommendedAction
        }
        return when (classification) {
            SafetyClassification.HIGH_RISK -> if (tamil) {
                "உடனே நிறுத்துங்கள். பணம் அனுப்பவோ, OTP/UPI PIN பகிரவோ, இணைப்பைத் திறக்கவோ வேண்டாம். அதிகாரப்பூர்வ எண்ணில் சரிபார்க்கவும்."
            } else {
                "Stop immediately. Do not send money, share OTP/UPI PIN, install apps, or open links. Verify via an official channel."
            }

            SafetyClassification.CAUTION -> topRisk?.recommendedAction ?: if (tamil) {
                "பணம் செலுத்துவதற்கு அல்லது பதில் அளிப்பதற்கு முன், இந்த செய்தியின் விவரங்களை அதிகாரப்பூர்வ செயலியில் தனியாக சரிபார்க்கவும்."
            } else {
                "Pause and verify the sender and request independently using an official app or known phone number."
            }

            SafetyClassification.UNABLE_TO_ASSESS -> if (tamil) {
                "முழு செய்தி, இணைப்பு அல்லது அனுப்பியவர் விவரத்தை ஒட்டவும்; அப்போதுதான் துல்லியமாக மதிப்பிட முடியும்."
            } else {
                "Paste the complete message, link, or request details so CheckFirst can evaluate the full context."
            }

            SafetyClassification.LIKELY_LEGITIMATE -> if (tamil) {
                "பெரிய அபாய அறிகுறிகள் இல்லை; எனினும் சந்தேகம் இருந்தால் அதிகாரப்பூர்வ செயலியில் சரிபார்க்கவும். ஒருபோதும் OTP/PIN பகிர வேண்டாம்."
            } else {
                "No major red flags were found, but never treat this as proof of safety. Still verify unexpected requests directly in the official app."
            }
        }
    }
}
