package com.example.data.remote

import com.example.domain.DefaultPluginSpec
import com.example.domain.defaultBuiltInPlugins
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSessionAndBrainStore(private val db: FirebaseFirestore) {

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- CHAT SESSIONS & SESSION MESSAGES ---

    fun observeChatSessions(userId: String): Flow<List<FirestoreChatSession>> {
        val colRef = userDoc(userId).collection("chat_sessions")
        return colRef
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreChatSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path, userId)
                throw error
            }
    }

    suspend fun createChatSession(
        title: String = "New Nexus Thread",
        agent: String = "general"
    ): Result<String> {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("chat_sessions").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().take(120).ifBlank { "New Nexus Thread" },
            "agent" to agent.trim().take(40).ifBlank { "general" },
            "lastMessagePreview" to "",
            "messageCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun ensureDefaultSession(): Result<String> {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("chat_sessions")
        return try {
            val existing = colRef.orderBy("updatedAt", Query.Direction.DESCENDING).limit(1).get().await()
            if (!existing.isEmpty) {
                Result.success(existing.documents.first().id)
            } else {
                createChatSession(title = "Primary Nexus", agent = "general")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun renameChatSession(sessionId: String, newTitle: String): Result<Unit> {
        val uid = requireUserId()
        require(sessionId.isNotBlank()) { "Session ID must not be blank" }
        val docRef = userDoc(uid).collection("chat_sessions").document(sessionId)
        return try {
            docRef.update(
                mapOf(
                    "title" to newTitle.trim().take(120).ifBlank { "Nexus Thread" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteChatSession(sessionId: String): Result<Unit> {
        val uid = requireUserId()
        require(sessionId.isNotBlank()) { "Session ID must not be blank" }
        val sessionRef = userDoc(uid).collection("chat_sessions").document(sessionId)
        val msgCol = sessionRef.collection("messages")
        return try {
            val msgs = msgCol.get().await()
            for (chunk in msgs.documents.chunked(400)) {
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            sessionRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, sessionRef.path)
            Result.failure(e)
        }
    }

    fun observeSessionMessages(userId: String, sessionId: String): Flow<List<FirestoreSessionMessage>> {
        val colRef = userDoc(userId).collection("chat_sessions").document(sessionId).collection("messages")
        return colRef
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(80)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreSessionMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path, userId)
                throw error
            }
    }

    suspend fun getRecentSessionMessages(sessionId: String, limit: Long = 20): List<FirestoreSessionMessage> {
        val uid = requireUserId()
        if (sessionId.isBlank()) return emptyList()
        val colRef = userDoc(uid).collection("chat_sessions").document(sessionId).collection("messages")
        return try {
            colRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .toObjects(
                    FirestoreSessionMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                .reversed()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            emptyList()
        }
    }

    suspend fun addSessionMessage(
        sessionId: String,
        role: String,
        text: String,
        agent: String = "general",
        modelLabel: String = "",
        hasImage: Boolean = false,
        webSources: List<String> = emptyList(),
        autoTitleFromFirstPrompt: String? = null
    ): Result<String> {
        val uid = requireUserId()
        require(sessionId.isNotBlank()) { "Session ID must not be blank" }
        val sessionRef = userDoc(uid).collection("chat_sessions").document(sessionId)
        val msgRef = sessionRef.collection("messages").document()
        val boundedSources = webSources.map { it.take(300) }.filter { it.isNotBlank() }.take(10)
        val cleanedText = text.take(24_000)

        val msgPayload = mapOf(
            "userId" to uid,
            "sessionId" to sessionId,
            "role" to role,
            "text" to cleanedText,
            "agent" to agent.take(40).ifBlank { "general" },
            "modelLabel" to modelLabel.take(80),
            "hasImage" to hasImage,
            "webSources" to boundedSources,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            msgRef.set(msgPayload).await()
            val sessionUpdates = mutableMapOf<String, Any>(
                "lastMessagePreview" to cleanedText.replace('\n', ' ').take(280),
                "messageCount" to FieldValue.increment(1L),
                "agent" to agent.take(40).ifBlank { "general" },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!autoTitleFromFirstPrompt.isNullOrBlank()) {
                sessionUpdates["title"] = autoTitleFromFirstPrompt.trim().replace('\n', ' ').take(48)
            }
            sessionRef.update(sessionUpdates).await()
            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearSessionMessages(sessionId: String): Result<Unit> {
        val uid = requireUserId()
        require(sessionId.isNotBlank()) { "Session ID must not be blank" }
        val sessionRef = userDoc(uid).collection("chat_sessions").document(sessionId)
        val colRef = sessionRef.collection("messages")
        return try {
            val snapshot = colRef.get().await()
            for (chunk in snapshot.documents.chunked(400)) {
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            sessionRef.update(
                mapOf(
                    "lastMessagePreview" to "",
                    "messageCount" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            Result.failure(e)
        }
    }

    // --- PLUGINS ---

    fun observePlugins(userId: String): Flow<List<FirestorePluginConfig>> {
        val colRef = userDoc(userId).collection("plugins")
        return colRef
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(60)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestorePluginConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path, userId)
                throw error
            }
    }

    suspend fun seedBuiltInPluginsIfMissing(): Result<Unit> {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("plugins")
        return try {
            val existingKeys = colRef.get().await().documents
                .mapNotNull { it.getString("pluginKey") }
                .toSet()
            val missing = defaultBuiltInPlugins.filter { it.pluginKey !in existingKeys }
            if (missing.isNotEmpty()) {
                val batch = db.batch()
                missing.forEach { spec: DefaultPluginSpec ->
                    val ref = colRef.document("builtin_${spec.pluginKey}")
                    batch.set(
                        ref,
                        mapOf(
                            "userId" to uid,
                            "pluginKey" to spec.pluginKey,
                            "name" to spec.name.take(80),
                            "description" to spec.description.take(400),
                            "category" to spec.category.take(40),
                            "enabled" to spec.defaultEnabled,
                            "systemInstruction" to spec.systemInstruction.take(2000),
                            "isBuiltIn" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun setPluginEnabled(pluginId: String, enabled: Boolean): Result<Unit> {
        val uid = requireUserId()
        require(pluginId.isNotBlank()) { "Plugin ID must not be blank" }
        val docRef = userDoc(uid).collection("plugins").document(pluginId)
        return try {
            docRef.update(
                mapOf(
                    "enabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addCustomPlugin(
        name: String,
        description: String,
        category: String,
        systemInstruction: String
    ): Result<String> {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("plugins")
        val docRef = colRef.document()
        val sanitizedKey = ("custom_" + name.lowercase().replace(Regex("[^a-z0-9]+"), "_"))
            .trim('_')
            .take(48)
            .ifBlank { "custom_plugin" } + "_" + docRef.id.take(6)

        val payload = mapOf(
            "userId" to uid,
            "pluginKey" to sanitizedKey.take(64),
            "name" to name.trim().take(80),
            "description" to description.trim().take(400),
            "category" to category.trim().take(40).ifBlank { "Custom" },
            "enabled" to true,
            "systemInstruction" to systemInstruction.trim().take(2000),
            "isBuiltIn" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePlugin(pluginId: String): Result<Unit> {
        val uid = requireUserId()
        require(pluginId.isNotBlank()) { "Plugin ID must not be blank" }
        val docRef = userDoc(uid).collection("plugins").document(pluginId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- AI BRAIN RUNS ---

    fun observeBrainRuns(userId: String): Flow<List<FirestoreBrainRun>> {
        val colRef = userDoc(userId).collection("brain_runs")
        return colRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreBrainRun::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path, userId)
                throw error
            }
    }

    suspend fun saveBrainRun(
        mode: String,
        prompt: String,
        summary: String,
        confidence: Int,
        insights: List<String>,
        suggestedTriplesCount: Int,
        modelUsed: String
    ): Result<String> {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("brain_runs").document()
        val payload = mapOf(
            "userId" to uid,
            "mode" to mode.trim().take(40).ifBlank { "reasoner" },
            "prompt" to prompt.trim().take(4000),
            "summary" to summary.trim().take(12000),
            "confidence" to confidence.coerceIn(0, 100).toLong(),
            "insights" to insights.map { it.trim().take(400) }.filter { it.isNotBlank() }.take(12),
            "suggestedTriplesCount" to suggestedTriplesCount.coerceIn(0, 20).toLong(),
            "modelUsed" to modelUsed.trim().take(80).ifBlank { "gemini-3.8-flash" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBrainRun(runId: String): Result<Unit> {
        val uid = requireUserId()
        require(runId.isNotBlank()) { "Run ID must not be blank" }
        val docRef = userDoc(uid).collection("brain_runs").document(runId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun logFlowError(
        error: Throwable,
        op: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }
}
