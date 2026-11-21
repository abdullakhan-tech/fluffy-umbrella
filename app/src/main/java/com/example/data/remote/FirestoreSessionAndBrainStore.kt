package com.example.data.remote

import com.example.domain.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")

class FirestoreSessionAndBrainStore(private val db: FirebaseFirestore) {

    private val auth get() = Firebase.auth
    private val pluginAndBrainStore = FirestorePluginAndBrainStore(db)

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- CHAT SESSIONS & SESSION MESSAGES ---

    fun observeChatSessions(userId: String): Flow<List<FirestoreChatSession>> {
        val ownerId = requireAuthenticatedOwner(userId, auth.currentUser?.uid)
        val colRef = userDoc(ownerId).collection("chat_sessions")
        return colRef
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjectsSafe<FirestoreChatSession>()
                    .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                pluginAndBrainStore.logFlowError(error, OperationType.LIST, colRef.path, ownerId)
                throw error
            }
    }

    suspend fun loadChatSessions(limit: Long = 40): Result<List<FirestoreChatSession>> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val colRef = userDoc(uid).collection("chat_sessions")
            path = colRef.path
            val sessions = colRef
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(limit.coerceIn(1L, 100L))
                .get()
                .await()
                .toObjectsSafe<FirestoreChatSession>()
                .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            Result.success(sessions)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun loadSessionMessages(
        sessionId: String,
        limit: Long = 80
    ): Result<List<FirestoreSessionMessage>> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            if (!VALID_ID_REGEX.matches(sessionId)) {
                return Result.failure(IllegalArgumentException("Invalid session ID"))
            }
            val colRef = userDoc(uid).collection("chat_sessions").document(sessionId).collection("messages")
            path = colRef.path
            val messages = colRef
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(limit.coerceIn(1L, 500L))
                .get()
                .await()
                .toObjectsSafe<FirestoreSessionMessage>()
                .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            Result.success(messages)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun saveChatSession(
        sessionId: String? = null,
        title: String = "New Nexus Thread",
        agent: String = "general",
        messages: List<ChatMessage> = emptyList()
    ): Result<String> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val sessionsCol = userDoc(uid).collection("chat_sessions")
            val targetId = sessionId?.trim().orEmpty()
            if (targetId.isNotEmpty() && !VALID_ID_REGEX.matches(targetId)) {
                return Result.failure(IllegalArgumentException("Invalid session ID format"))
            }
            val sessionRef = if (targetId.isNotEmpty()) sessionsCol.document(targetId) else sessionsCol.document()
            path = sessionRef.path
            val resolvedSessionId = sessionRef.id
            val cleanTitle = title.trim().take(120).ifBlank { "New Nexus Thread" }
            val cleanAgent = agent.trim().take(40).ifBlank { "general" }
            val validMessages = messages.filter { it.text.trim().isNotEmpty() }

            val existingSessionSnap = sessionRef.get().await()
            val messagesCol = sessionRef.collection("messages")
            val existingMessageIds = if (existingSessionSnap.exists()) {
                messagesCol.get().await().documents.map { it.id }.toSet()
            } else {
                emptySet()
            }

            val newMessagesToInsert = validMessages.filter { msg ->
                val candidateId = msg.id.trim()
                candidateId.isEmpty() || !VALID_ID_REGEX.matches(candidateId) || candidateId !in existingMessageIds
            }
            val totalCount = (existingMessageIds.size + newMessagesToInsert.size).coerceIn(0, 5000).toLong()
            val preview = validMessages.lastOrNull()?.text?.trim()?.replace('\n', ' ')?.take(280)
                ?: existingSessionSnap.getString("lastMessagePreview").orEmpty().take(280)

            if (!existingSessionSnap.exists()) {
                sessionRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to cleanTitle,
                        "agent" to cleanAgent,
                        "lastMessagePreview" to preview,
                        "messageCount" to totalCount,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                sessionRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "agent" to cleanAgent,
                        "lastMessagePreview" to preview,
                        "messageCount" to totalCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            newMessagesToInsert.forEachIndexed { index, msg ->
                if (index > 0) delay(5L)
                val candidateId = msg.id.trim()
                val msgRef = if (VALID_ID_REGEX.matches(candidateId)) {
                    messagesCol.document(candidateId)
                } else {
                    messagesCol.document()
                }
                val role = if (msg.isFromUser) "user" else "assistant"
                val msgAgent = msg.agentId.trim().take(40).ifBlank { cleanAgent }
                msgRef.set(
                    mapOf(
                        "userId" to uid,
                        "sessionId" to resolvedSessionId,
                        "role" to role,
                        "text" to msg.text.trim().take(24_000),
                        "agent" to msgAgent,
                        "modelLabel" to msg.modelLabel.take(80),
                        "hasImage" to msg.hasImage,
                        "webSources" to msg.webSources.map { it.take(1000) }.filter { it.isNotBlank() }.take(10),
                        "webSourceTitles" to msg.webSourceTitles.map { it.take(160) }.take(10),
                        "searchQueries" to msg.searchQueries.map { it.take(200) }.filter { it.isNotBlank() }.take(8),
                        "searchEntryPointHtml" to msg.searchEntryPointHtml.take(24_000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(resolvedSessionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createChatSession(
        title: String = "New Nexus Thread",
        agent: String = "general"
    ): Result<String> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("chat_sessions").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to title.trim().take(120).ifBlank { "New Nexus Thread" },
                "agent" to agent.trim().take(40).ifBlank { "general" },
                "lastMessagePreview" to "",
                "messageCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun ensureDefaultSession(): Result<String> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val colRef = userDoc(uid).collection("chat_sessions")
            path = colRef.path
            val existing = colRef.orderBy("updatedAt", Query.Direction.DESCENDING).limit(1).get().await()
            if (!existing.isEmpty) {
                Result.success(existing.documents.first().id)
            } else {
                createChatSession(title = "Primary Nexus", agent = "general")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun renameChatSession(sessionId: String, newTitle: String): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            require(VALID_ID_REGEX.matches(sessionId)) { "Invalid session ID" }
            val docRef = userDoc(uid).collection("chat_sessions").document(sessionId)
            path = docRef.path
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
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private suspend fun deleteSubcollectionDocs(colRef: CollectionReference) {
        while (true) {
            val batchSnap = colRef.limit(400).get().await()
            if (batchSnap.isEmpty) break
            val batch = db.batch()
            batchSnap.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (batchSnap.size() < 400) break
        }
    }

    suspend fun deleteChatSession(sessionId: String): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            if (!VALID_ID_REGEX.matches(sessionId)) {
                return Result.failure(IllegalArgumentException("Invalid session ID"))
            }
            val sessionRef = userDoc(uid).collection("chat_sessions").document(sessionId)
            path = sessionRef.path
            deleteSubcollectionDocs(sessionRef.collection("messages"))
            sessionRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearAndStartNewChatSession(
        sessionId: String,
        newTitle: String = "New Nexus Thread",
        agent: String = "general"
    ): Result<String> {
        return try {
            requireUserId()
            val cleanId = sessionId.trim()
            if (cleanId.isNotEmpty()) {
                val deleteResult = deleteChatSession(cleanId)
                if (deleteResult.isFailure) {
                    return Result.failure(
                        deleteResult.exceptionOrNull() ?: IllegalStateException("Failed to delete current chat session")
                    )
                }
            }
            createChatSession(title = newTitle, agent = agent)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, null)
            Result.failure(e)
        }
    }

    fun observeSessionMessages(userId: String, sessionId: String): Flow<List<FirestoreSessionMessage>> {
        val ownerId = requireAuthenticatedOwner(userId, auth.currentUser?.uid)
        require(VALID_ID_REGEX.matches(sessionId)) { "Invalid session ID" }
        val colRef = userDoc(ownerId).collection("chat_sessions").document(sessionId).collection("messages")
        return colRef
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(80)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjectsSafe<FirestoreSessionMessage>()
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                pluginAndBrainStore.logFlowError(error, OperationType.LIST, colRef.path, ownerId)
                throw error
            }
    }

    suspend fun getRecentSessionMessages(sessionId: String, limit: Long = 20): List<FirestoreSessionMessage> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            if (!VALID_ID_REGEX.matches(sessionId)) return emptyList()
            val colRef = userDoc(uid).collection("chat_sessions").document(sessionId).collection("messages")
            path = colRef.path
            colRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .toObjectsSafe<FirestoreSessionMessage>()
                .reversed()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
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
        webSourceTitles: List<String> = emptyList(),
        searchQueries: List<String> = emptyList(),
        searchEntryPointHtml: String = "",
        autoTitleFromFirstPrompt: String? = null
    ): Result<String> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            require(VALID_ID_REGEX.matches(sessionId)) { "Invalid session ID" }
            val sessionRef = userDoc(uid).collection("chat_sessions").document(sessionId)
            val msgRef = sessionRef.collection("messages").document()
            path = msgRef.path
            val cleanedText = text.take(24_000)
            val msgPayload = mapOf(
                "userId" to uid,
                "sessionId" to sessionId,
                "role" to role,
                "text" to cleanedText,
                "agent" to agent.take(40).ifBlank { "general" },
                "modelLabel" to modelLabel.take(80),
                "hasImage" to hasImage,
                "webSources" to webSources.map { it.take(1000) }.filter { it.isNotBlank() }.take(10),
                "webSourceTitles" to webSourceTitles.map { it.take(160) }.take(10),
                "searchQueries" to searchQueries.map { it.take(200) }.filter { it.isNotBlank() }.take(8),
                "searchEntryPointHtml" to searchEntryPointHtml.take(24_000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
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
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun clearSessionMessages(sessionId: String): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            if (!VALID_ID_REGEX.matches(sessionId)) {
                return Result.failure(IllegalArgumentException("Invalid session ID"))
            }
            val sessionRef = userDoc(uid).collection("chat_sessions").document(sessionId)
            val colRef = sessionRef.collection("messages")
            path = colRef.path
            deleteSubcollectionDocs(colRef)
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
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- PLUGINS & AI BRAIN RUNS ---

    fun observePlugins(userId: String): Flow<List<FirestorePluginConfig>> =
        pluginAndBrainStore.observePlugins(userId)

    suspend fun seedBuiltInPluginsIfMissing(): Result<Unit> =
        pluginAndBrainStore.seedBuiltInPluginsIfMissing()

    suspend fun setPluginEnabled(pluginId: String, enabled: Boolean): Result<Unit> =
        pluginAndBrainStore.setPluginEnabled(pluginId, enabled)

    suspend fun addCustomPlugin(
        name: String,
        description: String,
        category: String,
        systemInstruction: String
    ): Result<String> = pluginAndBrainStore.addCustomPlugin(name, description, category, systemInstruction)

    suspend fun deletePlugin(pluginId: String): Result<Unit> =
        pluginAndBrainStore.deletePlugin(pluginId)

    fun observeBrainRuns(userId: String): Flow<List<FirestoreBrainRun>> =
        pluginAndBrainStore.observeBrainRuns(userId)

    suspend fun saveBrainRun(
        mode: String,
        prompt: String,
        summary: String,
        confidence: Int,
        insights: List<String>,
        suggestedTriplesCount: Int,
        modelUsed: String
    ): Result<String> = pluginAndBrainStore.saveBrainRun(
        mode = mode,
        prompt = prompt,
        summary = summary,
        confidence = confidence,
        insights = insights,
        suggestedTriplesCount = suggestedTriplesCount,
        modelUsed = modelUsed
    )

    suspend fun deleteBrainRun(runId: String): Result<Unit> =
        pluginAndBrainStore.deleteBrainRun(runId)
}
