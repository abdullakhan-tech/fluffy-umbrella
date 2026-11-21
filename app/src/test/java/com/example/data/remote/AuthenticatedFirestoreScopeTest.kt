package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticatedFirestoreScopeTest {
    @Test fun authenticatedOwnerIsDerivedFromFirebaseUid() {
        assertEquals("alice_uid", requireAuthenticatedOwner("alice_uid", "alice_uid"))
    }
    @Test fun omittedRequestedOwnerUsesAuthenticatedUid() {
        assertEquals("alice_uid", requireAuthenticatedOwner(null, "alice_uid"))
    }
    @Test(expected = IllegalStateException::class) fun missingAuthenticationIsRejected() {
        requireAuthenticatedOwner("alice_uid", null)
    }
    @Test(expected = SecurityException::class) fun crossUserRequestedOwnerIsRejected() {
        requireAuthenticatedOwner("bob_uid", "alice_uid")
    }
}
