package com.example.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException

/**
 * Resolves a requested Firestore owner scope against the currently authenticated Firebase UID.
 *
 * Firestore Security Rules remain the authoritative server-side boundary. This check prevents
 * accidental or malicious callers inside the client process from constructing listeners for
 * another account before a network request is issued.
 */
internal fun requireAuthenticatedOwner(
    requestedUserId: String?,
    authenticatedUserId: String?
): String {
    val currentUid = authenticatedUserId
        ?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("User must be signed in before accessing private Firestore data.")

    if (requestedUserId != null && requestedUserId != currentUid) {
        throw SecurityException("Access to another user's private data is not allowed.")
    }

    return currentUid
}

internal fun cleanRequiredFirestoreInput(value: String, maxLength: Int, label: String): String {
    val cleaned = value.trim()
    require(cleaned.isNotEmpty()) { "$label must not be blank." }
    require(cleaned.length <= maxLength) { "$label exceeds the $maxLength character limit." }
    return cleaned
}

internal fun requireValidFirestoreDocumentId(value: String, label: String) {
    require(value.matches(Regex("^[a-zA-Z0-9_-]{1,128}$"))) {
        "$label is invalid."
    }
}

internal fun logFirestoreFlowError(
    error: Throwable,
    op: OperationType,
    path: String,
    expectedUid: String,
    auth: FirebaseAuth
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
