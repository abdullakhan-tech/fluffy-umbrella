package com.example.data.remote

import com.example.domain.DefaultPluginSpec
import com.example.domain.defaultBuiltInPlugins
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

internal class FirestorePluginAndBrainStore(private val db: FirebaseFirestore) {

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observePlugins(userId: String): Flow<List<FirestorePluginConfig>> {
        val colRef = userDoc(userId).collection("plugins")
        return colRef
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(60)
            .snapshots()
            .map { snapshot -> snapshot.toObjectsSafe<FirestorePluginConfig>() }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path, userId)
                throw error
            }
    }

    suspend fun seedBuiltInPluginsIfMissing(): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val colRef = userDoc(uid).collection("plugins")
            path = colRef.path
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
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun setPluginEnabled(pluginId: String, enabled: Boolean): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            require(pluginId.isNotBlank()) { "Plugin ID must not be blank" }
            val docRef = userDoc(uid).collection("plugins").document(pluginId)
            path = docRef.path
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
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addCustomPlugin(
        name: String,
        description: String,
        category: String,
        systemInstruction: String
    ): Result<String> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            val colRef = userDoc(uid).collection("plugins")
            val docRef = colRef.document()
            path = docRef.path
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
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePlugin(pluginId: String): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            require(pluginId.isNotBlank()) { "Plugin ID must not be blank" }
            val docRef = userDoc(uid).collection("plugins").document(pluginId)
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

    fun observeBrainRuns(userId: String): Flow<List<FirestoreBrainRun>> {
        val colRef = userDoc(userId).collection("brain_runs")
        return colRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjectsSafe<FirestoreBrainRun>()
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
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
        var path: String? = null
        return try {
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("brain_runs").document()
            path = docRef.path
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
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBrainRun(runId: String): Result<Unit> {
        var path: String? = null
        return try {
            val uid = requireUserId()
            require(runId.isNotBlank()) { "Run ID must not be blank" }
            val docRef = userDoc(uid).collection("brain_runs").document(runId)
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

    internal fun logFlowError(
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
