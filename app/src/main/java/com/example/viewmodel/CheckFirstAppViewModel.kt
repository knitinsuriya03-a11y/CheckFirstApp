package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreUserRepository
import com.example.data.PreferencesDataStore
import com.example.domain.ActivityItem
import com.example.domain.Prefs
import com.example.domain.RiskLevel
import com.example.firebase.authStateFlow
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "CheckFirstAppVM"

data class AppShellUiState(
    val prefs: Prefs = Prefs(),
    val currentUser: FirebaseUser? = null,
    val history: List<ActivityItem> = emptyList(),
    val notice: String? = null,
    val syncError: String? = null,
    val isSyncingCloud: Boolean = false,
)

class CheckFirstAppViewModel(
    private val localStore: PreferencesDataStore,
    private val firestoreRepo: FirestoreUserRepository,
    private val auth: FirebaseAuth = Firebase.auth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppShellUiState(currentUser = auth.currentUser))
    val uiState: StateFlow<AppShellUiState> = _uiState.asStateFlow()

    private var cloudPrefsJob: Job? = null
    private var cloudHistoryJob: Job? = null

    private fun formatTimestamp(date: Date): String =
        SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(date)

    init {
        viewModelScope.launch {
            localStore.prefsFlow.collectLatest { localPrefs ->
                if (_uiState.value.currentUser == null) {
                    _uiState.update { it.copy(prefs = localPrefs) }
                }
            }
        }
        viewModelScope.launch {
            localStore.localHistoryFlow.collectLatest { localHistory ->
                if (_uiState.value.currentUser == null) {
                    _uiState.update { it.copy(history = localHistory) }
                }
            }
        }
        viewModelScope.launch {
            auth.authStateFlow().collectLatest { user ->
                handleAuthUserChanged(user)
            }
        }
    }

    private fun handleAuthUserChanged(user: FirebaseUser?) {
        stopCloudListeners()
        val uid = user?.uid
        if (uid.isNullOrBlank()) {
            // Immediately drop any signed-in cloud history from memory, then restore local snapshot
            _uiState.update { it.copy(currentUser = null, history = emptyList(), syncError = null) }
            viewModelScope.launch {
                val localPrefs = localStore.prefsFlow.first()
                val localHistory = if (localPrefs.saveHistory) localStore.localHistoryFlow.first() else emptyList()
                if (_uiState.value.currentUser == null) {
                    _uiState.update { it.copy(prefs = localPrefs, history = localHistory) }
                }
            }
            return
        }
        _uiState.update { it.copy(currentUser = user, syncError = null) }

        // Auth-gated Firestore listeners start ONLY with a verified non-blank UID
        cloudPrefsJob = viewModelScope.launch {
            firestoreRepo.observePreferences(uid)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing cloud preferences", rootError)
                    _uiState.update { it.copy(syncError = rootError.message ?: "Failed to sync preferences.") }
                }
                .collectLatest { cloudPrefs ->
                    if (cloudPrefs != null) {
                        _uiState.update { it.copy(prefs = cloudPrefs, syncError = null) }
                        localStore.savePrefs(cloudPrefs)
                        updateHistoryListener(uid, cloudPrefs.saveHistory)
                    } else {
                        // First sign-in: keep local prefs & wait for user changes or save current prefs
                        updateHistoryListener(uid, _uiState.value.prefs.saveHistory)
                    }
                }
        }
    }

    private fun updateHistoryListener(uid: String, saveHistoryEnabled: Boolean) {
        if (!saveHistoryEnabled) {
            cloudHistoryJob?.cancel()
            cloudHistoryJob = null
            _uiState.update { it.copy(history = emptyList()) }
            return
        }
        if (cloudHistoryJob?.isActive == true) return
        cloudHistoryJob = viewModelScope.launch {
            firestoreRepo.observeCheckHistory(uid)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing check history", rootError)
                    _uiState.update { it.copy(syncError = rootError.message ?: "Failed to observe history.") }
                }
                .collectLatest { docs ->
                    val mapped = docs
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { doc ->
                            val ts = doc.createdAt ?: Timestamp.now()
                            ActivityItem(
                                id = doc.id.ifBlank { UUID.randomUUID().toString() },
                                kind = doc.kind,
                                label = doc.label,
                                risk = RiskLevel.fromWire(doc.risk),
                                date = formatTimestamp(ts.toDate()),
                                isDemo = false,
                            )
                        }
                    _uiState.update { it.copy(history = mapped, syncError = null) }
                }
        }
    }

    fun stopCloudListeners() {
        cloudPrefsJob?.cancel()
        cloudPrefsJob = null
        cloudHistoryJob?.cancel()
        cloudHistoryJob = null
    }

    fun updatePrefs(transform: (Prefs) -> Prefs) {
        val next = transform(_uiState.value.prefs)
        val prevSaveHistory = _uiState.value.prefs.saveHistory
        _uiState.update {
            it.copy(
                prefs = next,
                history = if (!next.saveHistory) emptyList() else it.history,
            )
        }

        viewModelScope.launch {
            localStore.savePrefs(next)
            val uid = auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                _uiState.update { it.copy(isSyncingCloud = true) }
                val res = firestoreRepo.savePreferences(uid, next)
                if (res.isFailure) {
                    _uiState.update {
                        it.copy(
                            isSyncingCloud = false,
                            syncError = res.exceptionOrNull()?.localizedMessage ?: "Cloud preference sync failed.",
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSyncingCloud = false, syncError = null) }
                }
                if (prevSaveHistory && !next.saveHistory) {
                    firestoreRepo.clearCheckHistory(uid)
                    updateHistoryListener(uid, false)
                } else if (!prevSaveHistory && next.saveHistory) {
                    updateHistoryListener(uid, true)
                }
            }
        }
    }

    fun recordCheckActivity(kind: String, label: String, risk: RiskLevel) {
        val currentPrefs = _uiState.value.prefs
        if (currentPrefs.completionAlerts) {
            val strings = if (currentPrefs.tamil) {
                com.example.ui.i18n.TamilStrings
            } else {
                com.example.ui.i18n.EnglishStrings
            }
            val localizedKind = strings.activityKindLabel(kind)
            showNotice("$localizedKind ${strings.noticeCheckCompleteSuffix}")
        }
        if (!currentPrefs.saveHistory) return

        val uid = auth.currentUser?.uid
        viewModelScope.launch {
            if (!uid.isNullOrBlank()) {
                val result = firestoreRepo.addCheckHistoryEntry(uid, kind, label, risk)
                if (result.isFailure) {
                    _uiState.update {
                        it.copy(syncError = result.exceptionOrNull()?.localizedMessage ?: "Could not save check metadata.")
                    }
                }
            } else {
                val item = ActivityItem(
                    id = UUID.randomUUID().toString(),
                    kind = kind,
                    label = label,
                    risk = risk,
                    date = formatTimestamp(Date()),
                    isDemo = false,
                )
                localStore.addLocalHistoryItem(item)
            }
        }
    }

    fun clearHistory(disableToggle: Boolean = false) {
        val isTamil = _uiState.value.prefs.tamil
        val strings = if (isTamil) com.example.ui.i18n.TamilStrings else com.example.ui.i18n.EnglishStrings
        viewModelScope.launch {
            localStore.clearLocalHistory()
            val uid = auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                val res = firestoreRepo.clearCheckHistory(uid)
                if (res.isFailure) {
                    showNotice(res.exceptionOrNull()?.localizedMessage ?: "Could not clear cloud history.")
                    return@launch
                }
            }
            if (disableToggle) {
                updatePrefs { it.copy(saveHistory = false) }
                showNotice(strings.noticeHistoryDisabledCleared)
            } else {
                _uiState.update { it.copy(history = emptyList()) }
                showNotice(strings.noticeHistoryCleared)
            }
        }
    }

    fun showNotice(message: String) {
        _uiState.update { it.copy(notice = message) }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopCloudListeners()
    }
}
