package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatStreamEvent
import com.example.data.CheckFirstAiRepository
import com.example.domain.ChatMessage
import com.example.domain.LiveVoiceStatus
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "AiAssistantVM"

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val activeModelName: String = CheckFirstAiRepository.DEFAULT_MODEL_FALLBACK_ORDER.first(),
    val usedFallbackModel: Boolean = false,
)

enum class LiveVoiceMessageKey {
    IDLE,
    CONNECTING,
    ACTIVE,
    ENDED,
    TIMEOUT,
    MIC_DENIED,
    UNAVAILABLE,
}

data class LiveVoiceUiState(
    val status: LiveVoiceStatus = LiveVoiceStatus.IDLE,
    val statusKey: LiveVoiceMessageKey = LiveVoiceMessageKey.IDLE,
    val errorMessage: String? = null,
    val isMuted: Boolean = false,
)

@OptIn(PublicPreviewAPI::class)
class AiAssistantViewModel(
    private val aiRepository: CheckFirstAiRepository = CheckFirstAiRepository(),
) : ViewModel() {

    private val _chatState = MutableStateFlow(AiChatUiState())
    val chatState: StateFlow<AiChatUiState> = _chatState.asStateFlow()

    private val _voiceState = MutableStateFlow(LiveVoiceUiState())
    val voiceState: StateFlow<LiveVoiceUiState> = _voiceState.asStateFlow()

    private var liveSession: LiveSession? = null
    private var voiceJob: Job? = null

    fun sendChatMessage(rawPrompt: String, tamil: Boolean) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _chatState.value.isSending) return

        val priorHistory = _chatState.value.messages.filterNot { it.isStreaming }
        val userMsg = ChatMessage(id = UUID.randomUUID().toString(), isUser = true, text = prompt)
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderAssistant = ChatMessage(id = assistantMsgId, isUser = false, text = "", isStreaming = true)

        _chatState.update {
            it.copy(
                messages = it.messages + userMsg + placeholderAssistant,
                isSending = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            val accumulated = StringBuilder()
            var respondingModel = _chatState.value.activeModelName
            var usedFallback = false
            try {
                aiRepository.streamSafetyChatWithFallback(
                    history = priorHistory,
                    userPrompt = prompt,
                    tamil = tamil,
                ).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Chunk -> {
                            respondingModel = event.modelName
                            usedFallback = event.usedFallback
                            accumulated.append(event.text)
                            val snapshot = accumulated.toString()
                            _chatState.update { state ->
                                state.copy(
                                    activeModelName = respondingModel,
                                    usedFallbackModel = usedFallback,
                                    messages = state.messages.map { m ->
                                        if (m.id == assistantMsgId) m.copy(text = snapshot, isStreaming = true) else m
                                    },
                                )
                            }
                        }

                        is ChatStreamEvent.Completed -> {
                            respondingModel = event.modelName
                            usedFallback = event.usedFallback
                        }
                    }
                }
                val finalText = accumulated.toString().trim()
                if (finalText.isBlank()) {
                    _chatState.update { state ->
                        state.copy(
                            messages = state.messages.filterNot { it.id == assistantMsgId },
                            isSending = false,
                            errorMessage = if (tamil) {
                                "Gemini பதில் வெறுமையாக உள்ளது. மீண்டும் முயற்சிக்கவும்."
                            } else {
                                "Gemini returned an empty response. Please try again."
                            },
                        )
                    }
                } else {
                    _chatState.update { state ->
                        state.copy(
                            activeModelName = respondingModel,
                            usedFallbackModel = usedFallback,
                            messages = state.messages.map { m ->
                                if (m.id == assistantMsgId) m.copy(text = finalText, isStreaming = false) else m
                            },
                            isSending = false,
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Safety chat timed out", e)
                _chatState.update { state ->
                    state.copy(
                        messages = state.messages.filterNot { it.id == assistantMsgId && it.text.isBlank() }
                            .map { if (it.id == assistantMsgId) it.copy(isStreaming = false) else it },
                        isSending = false,
                        errorMessage = if (tamil) {
                            "பதில் நேரம் முடிந்தது. மீண்டும் முயற்சிக்கவும்."
                        } else {
                            "Response timed out. Please try again."
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Safety chat failed across models", e)
                _chatState.update { state ->
                    state.copy(
                        messages = state.messages.filterNot { it.id == assistantMsgId && it.text.isBlank() }
                            .map { if (it.id == assistantMsgId) it.copy(isStreaming = false) else it },
                        isSending = false,
                        errorMessage = e.localizedMessage ?: if (tamil) {
                            "தற்போது CheckFirst AI-ஐ அணுக முடியவில்லை."
                        } else {
                            "Unable to reach CheckFirst AI right now."
                        },
                    )
                }
            }
        }
    }

    fun resetChat() {
        _chatState.value = AiChatUiState()
    }

    fun startLiveVoiceSession(tamil: Boolean) {
        if (_voiceState.value.status == LiveVoiceStatus.CONNECTING ||
            _voiceState.value.status == LiveVoiceStatus.ACTIVE
        ) return

        voiceJob?.cancel()
        voiceJob = viewModelScope.launch {
            _voiceState.value = LiveVoiceUiState(
                status = LiveVoiceStatus.CONNECTING,
                statusKey = LiveVoiceMessageKey.CONNECTING,
            )
            try {
                val session = aiRepository.connectLiveVoiceSession(tamil)
                liveSession = session
                session.startAudioConversation()
                _voiceState.value = LiveVoiceUiState(
                    status = LiveVoiceStatus.ACTIVE,
                    statusKey = LiveVoiceMessageKey.ACTIVE,
                    isMuted = false,
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Live voice connection timed out", e)
                stopLiveVoiceInternal()
                _voiceState.value = LiveVoiceUiState(
                    status = LiveVoiceStatus.ERROR,
                    statusKey = LiveVoiceMessageKey.TIMEOUT,
                    errorMessage = if (tamil) {
                        "30 வினாடிகளுக்குள் Gemini Live-உடன் இணைக்க முடியவில்லை. இணைய இணைப்பைச் சரிபார்த்து மீண்டும் முயற்சிக்கவும்."
                    } else {
                        "Could not connect to Gemini Live within 30 seconds. Check your internet connection and try again."
                    },
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "Microphone permission missing or revoked", e)
                stopLiveVoiceInternal()
                _voiceState.value = LiveVoiceUiState(
                    status = LiveVoiceStatus.ERROR,
                    statusKey = LiveVoiceMessageKey.MIC_DENIED,
                    errorMessage = if (tamil) {
                        "மைக்ரோஃபோன் அனுமதி மறுக்கப்பட்டது. RECORD_AUDIO அனுமதியை இயக்கி மீண்டும் முயற்சிக்கவும்."
                    } else {
                        "Microphone access was denied or revoked. Enable RECORD_AUDIO permission and try again."
                    },
                )
            } catch (e: CancellationException) {
                stopLiveVoiceSession()
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Live voice session failed", e)
                stopLiveVoiceInternal()
                _voiceState.value = LiveVoiceUiState(
                    status = LiveVoiceStatus.ERROR,
                    statusKey = LiveVoiceMessageKey.UNAVAILABLE,
                    errorMessage = e.localizedMessage ?: if (tamil) {
                        "Gemini நேரடி குரல் சேவையுடன் இணைக்க முடியவில்லை."
                    } else {
                        "Could not connect to Gemini Live Voice."
                    },
                )
            }
        }
    }

    fun stopLiveVoiceSession() {
        voiceJob?.cancel()
        voiceJob = null
        viewModelScope.launch {
            stopLiveVoiceInternal()
            _voiceState.value = LiveVoiceUiState(
                status = LiveVoiceStatus.IDLE,
                statusKey = LiveVoiceMessageKey.ENDED,
            )
        }
    }

    private suspend fun stopLiveVoiceInternal() {
        val session = liveSession
        liveSession = null
        if (session != null) {
            try {
                session.stopAudioConversation()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping audio conversation: ${e.message}")
            }
            try {
                session.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing live session: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceJob?.cancel()
        val session = liveSession
        liveSession = null
        if (session != null) {
            try {
                session.stopAudioConversation()
            } catch (_: Exception) {
            }
        }
    }
}
