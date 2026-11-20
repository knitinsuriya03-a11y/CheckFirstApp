package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.CheckHistoryDoc
import com.example.domain.Prefs
import com.example.domain.RiskLevel
import com.example.domain.UserPreferencesDoc
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id),
        ),
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) {
            "User must be signed in with Google before accessing Firestore."
        }
        return uid
    }

    fun observePreferences(userId: String): Flow<Prefs?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val docRef = db.collection("users").document(uid)
            .collection("preferences").document("default")
        val path = docRef.path

        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) {
                        null
                    } else {
                        snapshot.toObject(
                            UserPreferencesDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                        )?.toPrefs()
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                },
        )
    }

    suspend fun savePreferences(userId: String, prefs: Prefs): Result<Unit> {
        var docPath = "users/$userId/preferences/default"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val docRef = db.collection("users").document(uid)
                .collection("preferences").document("default")
            docPath = docRef.path

            val existing = docRef.get().await()
            if (existing.exists()) {
                val updatePayload = mapOf(
                    "tamil" to prefs.tamil,
                    "simpleMode" to prefs.simpleMode,
                    "largeText" to prefs.largeText,
                    "safetyTips" to prefs.safetyTips,
                    "completionAlerts" to prefs.completionAlerts,
                    "saveHistory" to prefs.saveHistory,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "tamil" to prefs.tamil,
                    "simpleMode" to prefs.simpleMode,
                    "largeText" to prefs.largeText,
                    "safetyTips" to prefs.safetyTips,
                    "completionAlerts" to prefs.completionAlerts,
                    "saveHistory" to prefs.saveHistory,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeCheckHistory(userId: String): Flow<List<CheckHistoryDoc>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/checkHistory"
        emitAll(
            db.collection("users").document(uid).collection("checkHistory")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CheckHistoryDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                },
        )
    }

    suspend fun addCheckHistoryEntry(
        userId: String,
        kind: String,
        label: String,
        risk: RiskLevel,
    ): Result<Unit> {
        var docPath = "users/$userId/checkHistory"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val safeKind = kind.trim().take(60).ifBlank { "Safety check" }
            val safeLabel = label.trim().take(120).ifBlank { "Check result" }

            val docRef = db.collection("users").document(uid)
                .collection("checkHistory").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "kind" to safeKind,
                "label" to safeLabel,
                "risk" to risk.wireValue,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearCheckHistory(userId: String): Result<Unit> {
        val path = "users/$userId/checkHistory"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
            val snapshot = db.collection("users").document(uid)
                .collection("checkHistory")
                .limit(100)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val batch = db.batch()
                snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
