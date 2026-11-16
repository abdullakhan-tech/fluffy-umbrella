package com.example.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

/**
 * Logs only non-sensitive diagnostic metadata.
 * Firestore paths, exception messages and auth details may contain private data.
 */
fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    @Suppress("UNUSED_PARAMETER") path: String?
): String {
    val firestoreError = generateSequence(exception) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    val errorCode = firestoreError?.code?.name ?: "UNKNOWN"
    val safeSummary = JSONObject()
        .put("code", errorCode)
        .put("operationType", operationType.value)
        .toString()

    Log.e("FirestoreError", "Firestore operation failed: $safeSummary")
    return safeSummary
}
