package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(requestedUserId: String? = null): String =
        requireAuthenticatedOwner(requestedUserId, auth.currentUser?.uid)

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun requireText(value: String, field: String, maxLength: Int): String {
        val cleaned = value.trim()
        require(cleaned.isNotEmpty()) { "$field must not be blank." }
        require(cleaned.length <= maxLength) { "$field must be at most $maxLength characters." }
        return cleaned
    }

    private fun requireDocumentId(value: String, field: String): String {
        require(Regex("^[A-Za-z0-9_-]{1,128}$").matches(value)) {
            "$field must contain 1-128 letters, numbers, underscores, or hyphens."
        }
        return value
    }

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String): Flow<UserProfileDoc?> {
        val uid = requireUserId(userId)
        val docRef = userDoc(uid)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, docRef.path, userId)
                throw error
            }
    }

    suspend fun ensureUserProfile(defaultOperatorName: String): Result<UserProfileDoc> {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateOperatorName(operatorName: String): Result<Unit> {
        val uid = requireUserId()
        val cleaned = operatorName.trim().take(32).ifBlank { "OPERATOR 01" }
        val docRef = userDoc(uid)
        return try {
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
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- CHAT MESSAGES ---

    fun observeChatMessages(userId: String): Flow<List<FirestoreChatMessage>> {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("messages")
        return colRef
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(60)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path, userId)
                throw error
            }
    }

    suspend fun getRecentChatMessages(limit: Long = 24): List<FirestoreChatMessage> {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("messages")
        return try {
            colRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .toObjects(
                    FirestoreChatMessage::class.java,
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

    suspend fun addChatMessage(
        role: String,
        text: String,
        agent: String = "general",
        modelLabel: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("messages").document()
        val payload = mapOf(
            "userId" to uid,
            "role" to role,
            "text" to text.take(24_000),
            "agent" to agent.take(40).ifBlank { "general" },
            "modelLabel" to modelLabel.take(80),
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

    suspend fun updateChatMessage(
        messageId: String,
        text: String,
        modelLabel: String
    ): Result<Unit> {
        val uid = requireUserId()
        require(messageId.isNotBlank()) { "Message ID must not be blank" }
        val docRef = userDoc(uid).collection("messages").document(messageId)
        return try {
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
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearChatMessages(): Result<Unit> {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("messages")
        return try {
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
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            Result.failure(e)
        }
    }

    // --- MEMORY NOTES ---

    fun observeMemoryNotes(userId: String): Flow<List<FirestoreMemoryNote>> {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("memories")
        return colRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreMemoryNote::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path, userId)
                throw error
            }
    }

    suspend fun getRecentMemoryNotes(limit: Long = 20): List<FirestoreMemoryNote> {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("memories")
        return try {
            colRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .toObjects(
                    FirestoreMemoryNote::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            emptyList()
        }
    }

    suspend fun addMemoryNote(content: String): Result<String> {
        val uid = requireUserId()
        val cleanedContent = requireText(content, "Memory content", 2000)
        val docRef = userDoc(uid).collection("memories").document()
        val payload = mapOf(
            "userId" to uid,
            "content" to cleanedContent,
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

    suspend fun updateMemoryNote(memoryId: String, content: String): Result<Unit> {
        val uid = requireUserId()
        val safeId = requireDocumentId(memoryId, "Memory ID")
        val cleanedContent = requireText(content, "Memory content", 2000)
        val docRef = userDoc(uid).collection("memories").document(safeId)
        return try {
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
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMemoryNote(memoryId: String): Result<Unit> {
        val uid = requireUserId()
        val safeId = requireDocumentId(memoryId, "Memory ID")
        val docRef = userDoc(uid).collection("memories").document(safeId)
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

    // --- KNOWLEDGE FACTS ---

    fun observeKnowledgeFacts(userId: String): Flow<List<FirestoreKnowledgeFact>> {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("knowledge_facts")
        return colRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(200)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreKnowledgeFact::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path, userId)
                throw error
            }
    }

    suspend fun getRecentKnowledgeFacts(limit: Long = 20): List<FirestoreKnowledgeFact> {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("knowledge_facts")
        return try {
            colRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .toObjects(
                    FirestoreKnowledgeFact::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            emptyList()
        }
    }

    suspend fun countKnowledgeFacts(): Int {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("knowledge_facts")
        return try {
            colRef.limit(201).get().await().size()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            0
        }
    }

    suspend fun addKnowledgeFact(
        subject: String,
        predicate: String,
        objectValue: String,
        source: String = "operator"
    ): Result<String> {
        val uid = requireUserId()
        val cleanedSubject = requireText(subject, "Fact subject", 300)
        val cleanedPredicate = requireText(predicate, "Fact predicate", 300)
        val cleanedObject = requireText(objectValue, "Fact object", 300)
        val cleanedSource = source.trim().take(40).ifBlank { "operator" }
        val docRef = userDoc(uid).collection("knowledge_facts").document()
        val payload = mapOf(
            "userId" to uid,
            "subject" to cleanedSubject,
            "predicate" to cleanedPredicate,
            "objectValue" to cleanedObject,
            "source" to cleanedSource,
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

    suspend fun updateKnowledgeFact(
        factId: String,
        subject: String,
        predicate: String,
        objectValue: String,
        source: String = "operator"
    ): Result<Unit> {
        val uid = requireUserId()
        val safeId = requireDocumentId(factId, "Fact ID")
        val cleanedSubject = requireText(subject, "Fact subject", 300)
        val cleanedPredicate = requireText(predicate, "Fact predicate", 300)
        val cleanedObject = requireText(objectValue, "Fact object", 300)
        val cleanedSource = source.trim().take(40).ifBlank { "operator" }
        val docRef = userDoc(uid).collection("knowledge_facts").document(safeId)
        return try {
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
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteKnowledgeFact(factId: String): Result<Unit> {
        val uid = requireUserId()
        val safeId = requireDocumentId(factId, "Fact ID")
        val docRef = userDoc(uid).collection("knowledge_facts").document(safeId)
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
