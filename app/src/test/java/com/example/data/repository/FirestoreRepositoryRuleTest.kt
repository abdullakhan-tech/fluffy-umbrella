package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.remote.FirestoreRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_authenticatedOwner_createsAndUpdatesOperatorName() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreRepository(firestore)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile("OPERATOR AL")
        }
        assertTrue(profileResult.isSuccess)
        assertEquals(uid, profileResult.getOrThrow().userId)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateOperatorName("COMMANDER 09")
        }
        assertTrue(updateResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it?.operatorName == "COMMANDER 09" }
        }
        assertNotNull(observed)
        assertEquals("COMMANDER 09", observed?.operatorName)
    }

    @Test
    fun chatMessages_authenticatedOwner_createsUpdatesAndObservesRealtime() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreRepository(firestore)

        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addChatMessage(
                role = "assistant",
                text = "Initial payload",
                agent = "general",
                modelLabel = "gemini-3.8-flash"
            )
        }
        assertTrue(addResult.isSuccess)
        val messageId = addResult.getOrThrow()

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateChatMessage(messageId, "Updated payload", "gemini-3.8-flash")
        }
        assertTrue(updateResult.isSuccess)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeChatMessages(uid).first { list ->
                list.any { it.id == messageId && it.text == "Updated payload" }
            }
        }
        assertTrue(emitted.any { it.id == messageId && it.userId == uid })

        val clearResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.clearChatMessages()
        }
        assertTrue(clearResult.isSuccess)
    }

    @Test
    fun memoryNotesAndKnowledgeFacts_authenticatedOwner_crudLifecycleSucceeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreRepository(firestore)

        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMemoryNote("Persistent memory item").getOrThrow()
        }
        val notes = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMemoryNotes(uid).first { list -> list.any { it.id == noteId } }
        }
        assertTrue(notes.any { it.id == noteId && it.content == "Persistent memory item" })

        val factId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addKnowledgeFact("Zaythar AI", "uses", "Firestore").getOrThrow()
        }
        val facts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeKnowledgeFacts(uid).first { list -> list.any { it.id == factId } }
        }
        assertTrue(facts.any { it.id == factId && it.subject == "Zaythar AI" })

        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.deleteMemoryNote(noteId) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.deleteKnowledgeFact(factId) }.isSuccess)
    }

    @Test
    fun observeSubcollections_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addMemoryNote("Alice private note").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreRepository(firestore)
        val exception = assertFailsWithType<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeMemoryNotes(aliceUid).first()
            }
        }
        val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $exception"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun observeCollections_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        Firebase.auth.signOut()
        val repository = FirestoreRepository(firestore)

        val exception = assertFailsWithType<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeChatMessages(aliceUid).first()
            }
        }
        val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $exception"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
