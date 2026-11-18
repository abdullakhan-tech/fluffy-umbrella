package com.example.data.remote

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
