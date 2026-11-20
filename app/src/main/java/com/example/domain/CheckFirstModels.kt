package com.example.domain

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RiskLevel(val wireValue: String) {
    DANGER("danger"),
    CAUTION("caution"),
    CLEAR("clear");

    companion object {
        fun fromWire(raw: String?): RiskLevel =
            entries.firstOrNull { it.wireValue.equals(raw, ignoreCase = true) } ?: CAUTION
    }
}

data class Prefs(
    val tamil: Boolean = false,
    val simpleMode: Boolean = false,
    val largeText: Boolean = false,
    val safetyTips: Boolean = true,
    val completionAlerts: Boolean = false,
    val saveHistory: Boolean = false,
)

data class ActivityItem(
    val id: String,
    val kind: String,
    val label: String,
    val risk: RiskLevel,
    val date: String,
    val isDemo: Boolean = false,
)

@Keep
@IgnoreExtraProperties
data class UserPreferencesDoc(
    val userId: String = "",
    val tamil: Boolean = false,
    val simpleMode: Boolean = false,
    val largeText: Boolean = false,
    val safetyTips: Boolean = true,
    val completionAlerts: Boolean = false,
    val saveHistory: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toPrefs(): Prefs = Prefs(
        tamil = tamil,
        simpleMode = simpleMode,
        largeText = largeText,
        safetyTips = safetyTips,
        completionAlerts = completionAlerts,
        saveHistory = saveHistory,
    )
}

@Keep
@IgnoreExtraProperties
data class CheckHistoryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kind: String = "",
    val label: String = "",
    val risk: String = RiskLevel.CAUTION.wireValue,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

data class MatchedIndicator(
    val id: String,
    val label: String,
    val explanation: String,
    val severity: RiskLevel,
    val highlightTerms: List<String>,
)

data class MessageScanResult(
    val risk: RiskLevel,
    val verdict: String,
    val matches: List<MatchedIndicator>,
)

data class UpiPaymentDetails(
    val payee: String,
    val name: String,
    val amount: Double?,
    val note: String,
    val currency: String,
    val mode: String,
    val raw: String,
    val isRefundMislead: Boolean,
)

enum class TenureUnit(val label: String) {
    DAYS("Days"),
    MONTHS("Months"),
    YEARS("Years"),
}

data class LoanCalculationResult(
    val received: Double,
    val fees: Double,
    val repayment: Double,
    val cost: Double,
    val annualizedRatePercent: Double,
    val days: Double,
)

enum class LoanRiskAnswer(val label: String) {
    YES("Yes"),
    NO("No"),
    UNSURE("Not sure"),
}

data class AiScanInsight(
    val summary: String,
    val keyConcerns: List<String>,
    val recommendedActions: List<String>,
    val cautionNote: String,
    val modelUsed: String = "",
    val usedFallback: Boolean = false,
)

sealed interface AiInsightUiState {
    data object Idle : AiInsightUiState
    data object Loading : AiInsightUiState
    data class Success(val insight: AiScanInsight) : AiInsightUiState
    data class Error(val message: String) : AiInsightUiState
}

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
)

enum class LiveVoiceStatus {
    IDLE,
    CONNECTING,
    ACTIVE,
    ERROR,
}
