package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
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

class FirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun cleanRequiredInput(value: String, maxLength: Int, label: String): String {
        val cleaned = value.trim()
        require(cleaned.isNotEmpty()) { "$label must not be blank." }
        require(cleaned.length <= maxLength) { "$label exceeds the $maxLength character limit." }
        return cleaned
    }

    private fun requireValidDocumentId(value: String, label: String) {
        require(value.matches(Regex("^[a-zA-Z0-9_-]{1,128}$"))) {
            "$label is invalid."
        }
    }

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String): Flow<UserProfileDoc?> {
        val ownerId = requireAuthenticatedOwner(userId, auth.currentUser?.uid)
        val docRef = userDoc(ownerId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObjectSafe<UserProfileDoc>() else null
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, docRef.path, ownerId)
                throw error
            }
    }

    suspend fun ensureUserProfile(defaultOperatorName: String): Result<UserProfileDoc> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val docRef = userDoc(uid)
            path = docRef.path
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObjectSafe<UserProfileDoc>()
                    ?: UserProfileDoc(id = uid, userId = uid, operatorName = defaultOperatorName.take(32).ifBlank { "OPERATOR 01" })
                Result.success(existing)
            } else {
                val cleanedName = defaultOperatorName.trim().take(32).ifBlank { "OPERATOR 01" }
                val payload = mapOf(
                    "userId" to uid,
                    "operatorName" to cleanedName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(UserProfileDoc(id = uid, userId = uid, operatorName = cleanedName))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOperatorName(operatorName: String): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val cleaned = operatorName.trim().take(32).ifBlank { "OPERATOR 01" }
            val docRef = userDoc(uid)
            path = docRef.path
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "operatorName" to cleaned,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "operatorName" to cleaned,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- CHAT MESSAGES ---

    fun observeChatMessages(userId: String): Flow<List<FirestoreChatMessage>> {
        val ownerId = requireAuthenticatedOwner(userId, auth.currentUser?.uid)
        val colRef = userDoc(ownerId).collection("messages")
        return colRef
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(60)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjectsSafe<FirestoreChatMessage>()
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path, ownerId)
                throw error
            }
    }

    suspend fun getRecentChatMessages(limit: Long = 24): List<FirestoreChatMessage> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val colRef = userDoc(uid).collection("messages")
            path = colRef.path
            colRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .toObjectsSafe<FirestoreChatMessage>()
                .reversed()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun addChatMessage(
        role: String,
        text: String,
        agent: String = "general",
        modelLabel: String = ""
    ): Result<String> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("messages").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "role" to role,
                "text" to text.take(24_000),
                "agent" to agent.take(40).ifBlank { "general" },
                "modelLabel" to modelLabel.take(80),
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

    suspend fun updateChatMessage(
        messageId: String,
        text: String,
        modelLabel: String
    ): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            require(messageId.isNotBlank()) { "Message ID must not be blank" }
            val docRef = userDoc(uid).collection("messages").document(messageId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "text" to text.take(24_000),
                    "modelLabel" to modelLabel.take(80),
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

    suspend fun clearChatMessages(): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val colRef = userDoc(uid).collection("messages")
            path = colRef.path
            val snapshot = colRef.get().await()
            val chunks = snapshot.documents.chunked(400)
            for (chunk in chunks) {
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
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

    // --- MEMORY NOTES ---

    fun observeMemoryNotes(userId: String): Flow<List<FirestoreMemoryNote>> {
        val ownerId = requireAuthenticatedOwner(userId, auth.currentUser?.uid)
        val colRef = userDoc(ownerId).collection("memories")
        return colRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjectsSafe<FirestoreMemoryNote>()
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path, ownerId)
                throw error
            }
    }

    suspend fun getRecentMemoryNotes(limit: Long = 20): List<FirestoreMemoryNote> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val colRef = userDoc(uid).collection("memories")
            path = colRef.path
            colRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.coerceIn(1L, 100L))
                .get()
                .await()
                .toObjectsSafe()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun addMemoryNote(content: String): Result<String> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val cleanedContent = cleanRequiredInput(content, 2000, "Memory content")
            val docRef = userDoc(uid).collection("memories").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "content" to cleanedContent,
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

    suspend fun updateMemoryNote(memoryId: String, content: String): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            requireValidDocumentId(memoryId, "Memory ID")
            val cleanedContent = cleanRequiredInput(content, 2000, "Memory content")
            val docRef = userDoc(uid).collection("memories").document(memoryId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "content" to cleanedContent,
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

    suspend fun deleteMemoryNote(memoryId: String): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            requireValidDocumentId(memoryId, "Memory ID")
            val docRef = userDoc(uid).collection("memories").document(memoryId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- KNOWLEDGE FACTS ---

    fun observeKnowledgeFacts(userId: String): Flow<List<FirestoreKnowledgeFact>> {
        val ownerId = requireAuthenticatedOwner(userId, auth.currentUser?.uid)
        val colRef = userDoc(ownerId).collection("knowledge_facts")
        return colRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(200)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjectsSafe<FirestoreKnowledgeFact>()
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path, ownerId)
                throw error
            }
    }

    suspend fun getRecentKnowledgeFacts(limit: Long = 20): List<FirestoreKnowledgeFact> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val colRef = userDoc(uid).collection("knowledge_facts")
            path = colRef.path
            colRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.coerceIn(1L, 200L))
                .get()
                .await()
                .toObjectsSafe()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun countKnowledgeFacts(): Int {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val colRef = userDoc(uid).collection("knowledge_facts")
            path = colRef.path
            colRef.limit(201).get().await().size()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            0
        }
    }

    suspend fun addKnowledgeFact(
        subject: String,
        predicate: String,
        objectValue: String,
        source: String = "operator"
    ): Result<String> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val cleanedSubject = cleanRequiredInput(subject, 300, "Knowledge subject")
            val cleanedPredicate = cleanRequiredInput(predicate, 300, "Knowledge predicate")
            val cleanedObject = cleanRequiredInput(objectValue, 300, "Knowledge object")
            val cleanedSource = source.trim().ifBlank { "operator" }
                .also { require(it.length <= 40) { "Knowledge source exceeds the 40 character limit." } }
            val docRef = userDoc(uid).collection("knowledge_facts").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "subject" to cleanedSubject,
                "predicate" to cleanedPredicate,
                "objectValue" to cleanedObject,
                "source" to cleanedSource,
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

    suspend fun updateKnowledgeFact(
        factId: String,
        subject: String,
        predicate: String,
        objectValue: String,
        source: String = "operator"
    ): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            requireValidDocumentId(factId, "Knowledge fact ID")
            val cleanedSubject = cleanRequiredInput(subject, 300, "Knowledge subject")
            val cleanedPredicate = cleanRequiredInput(predicate, 300, "Knowledge predicate")
            val cleanedObject = cleanRequiredInput(objectValue, 300, "Knowledge object")
            val cleanedSource = source.trim().ifBlank { "operator" }
                .also { require(it.length <= 40) { "Knowledge source exceeds the 40 character limit." } }
            val docRef = userDoc(uid).collection("knowledge_facts").document(factId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "subject" to cleanedSubject,
                    "predicate" to cleanedPredicate,
                    "objectValue" to cleanedObject,
                    "source" to cleanedSource,
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

    suspend fun deleteKnowledgeFact(factId: String): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            requireValidDocumentId(factId, "Knowledge fact ID")
            val docRef = userDoc(uid).collection("knowledge_facts").document(factId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
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
        if (exceptionToLog != null && runCatching { auth.currentUser?.uid }.getOrNull() == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }
}
