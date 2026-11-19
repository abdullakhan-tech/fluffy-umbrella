package com.example.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class FirestoreErrorHandlerTest {

    @Test
    fun handlerDoesNotExposeExceptionTextOrFirestorePath() {
        val privatePrompt = "private prompt: alice@example.test and memory contents"
        val privatePath = "users/alice_uid/memories/private_note"

        val payload = handleFirestoreError(
            exception = IllegalStateException(privatePrompt),
            operationType = OperationType.WRITE,
            path = privatePath
        )

        assertTrue(payload.contains("\"operationType\":\"write\""))
        assertTrue(payload.contains("\"code\":\"UNKNOWN\""))
        assertFalse(payload.contains("alice@example.test"))
        assertFalse(payload.contains("alice_uid"))
        assertFalse(payload.contains("private_note"))
        assertFalse(payload.contains("private prompt"))
        assertFalse(payload.contains("memory contents"))
        assertFalse(payload.contains("authInfo"))
        assertFalse(payload.contains("providerInfo"))
    }

    @Test
    fun permissionErrorsUseSafeActionableMessage() {
        val payload = safeFirestoreErrorPayload(
            OperationType.DELETE,
            "PERMISSION_DENIED"
        )

        assertTrue(payload.contains("permission"))
        assertTrue(payload.contains("\"operationType\":\"delete\""))
        assertFalse(payload.contains("uid"))
        assertFalse(payload.contains("email"))
    }

    @Test
    fun untrustedErrorCodeIsNormalizedBeforeSerialization() {
        val payload = safeFirestoreErrorPayload(
            OperationType.UPDATE,
            "secret-token-alice@example.test"
        )

        assertTrue(payload.contains("\"code\":\"UNKNOWN\""))
        assertFalse(payload.contains("secret-token"))
        assertFalse(payload.contains("alice@example.test"))
    }
}
