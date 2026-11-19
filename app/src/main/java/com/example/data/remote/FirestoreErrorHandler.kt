package com.example.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

internal inline fun <reified T> com.google.firebase.firestore.QuerySnapshot.toObjectsSafe(): List<T> =
    documents.mapNotNull { doc ->
        runCatching {
            doc.toObject(
                T::class.java,
                com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        }.getOrNull()
    }

internal inline fun <reified T> com.google.firebase.firestore.DocumentSnapshot.toObjectSafe(): T? =
    runCatching {
        toObject(T::class.java, com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }.getOrNull()

private val SAFE_ERROR_CODE = Regex("^[A-Z][A-Z0-9_]{0,39}$")

/**
 * Builds a user-safe diagnostic payload. It intentionally accepts only an allow-listed
 * operation and a normalized Firebase error code; it never serializes exception text,
 * document paths, UIDs, email addresses, provider metadata, or memory content.
 */
internal fun safeFirestoreErrorPayload(
    operationType: OperationType,
    errorCode: String?
): String {
    val safeCode = errorCode
        ?.takeIf(SAFE_ERROR_CODE::matches)
        ?: "UNKNOWN"
    val safeMessage = when (safeCode) {
        "UNAUTHENTICATED" -> "Please sign in to continue."
        "PERMISSION_DENIED" -> "You do not have permission to complete this operation."
        "NOT_FOUND" -> "The requested item could not be found."
        "INVALID_ARGUMENT" -> "The request is invalid."
        "FAILED_PRECONDITION" -> "The operation cannot be completed in the current state."
        "UNAVAILABLE", "DEADLINE_EXCEEDED", "ABORTED", "RESOURCE_EXHAUSTED" ->
            "The service is temporarily unavailable. Please try again later."
        else -> "The operation could not be completed. Please try again."
    }

    return JSONObject()
        .put("error", safeMessage)
        .put("operationType", operationType.value)
        .put("code", safeCode)
        .toString()
}

/**
 * Logs only a fixed operation label and Firebase's bounded status code.
 *
 * [path] remains in the signature for source compatibility with existing callers, but is
 * deliberately ignored because Firestore paths contain account UIDs and resource identifiers.
 */
@Suppress("UNUSED_PARAMETER")
fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val firestoreError = generateSequence(exception) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    val errorCode = firestoreError?.code?.name
    val safePayload = safeFirestoreErrorPayload(operationType, errorCode)

    val isCancellation = generateSequence(exception) { it.cause }
        .any { it is CancellationException }
    if (!isCancellation) {
        Log.e(
            "FirestoreError",
            "Firestore operation failed (operation=${operationType.value}, code=${errorCode ?: "UNKNOWN"})."
        )
    }
    return safePayload
}
