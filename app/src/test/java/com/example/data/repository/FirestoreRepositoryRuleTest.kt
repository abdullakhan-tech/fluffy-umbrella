package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.remote.FirestoreRepository
import com.example.data.remote.FirestoreSessionAndBrainStore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun chatSessionsAndSessionMessages_authenticatedOwner_fullLifecycleSucceeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val store = FirestoreSessionAndBrainStore(firestore)

        val sessionId = withTimeout(DEFAULT_TIMEOUT_MS) {
            store.createChatSession(title = "Architecture Thread", agent = "coder").getOrThrow()
        }
        assertTrue(sessionId.isNotBlank())

        val userMsgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            store.addSessionMessage(
                sessionId = sessionId,
                role = "user",
                text = "Explain Compose state hoisting",
                agent = "coder",
                autoTitleFromFirstPrompt = "Compose state hoisting"
            ).getOrThrow()
        }
        assertTrue(userMsgId.isNotBlank())

        val replyId = withTimeout(DEFAULT_TIMEOUT_MS) {
            store.addSessionMessage(
                sessionId = sessionId,
                role = "assistant",
                text = "State hoisting moves state up to callers.",
                agent = "coder",
                modelLabel = "gemini-3.8-flash+web",
                webSources = listOf("https://developer.android.com/develop/ui/compose/state")
            ).getOrThrow()
        }
        assertTrue(replyId.isNotBlank())

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            store.observeSessionMessages(uid, sessionId).first { list ->
                list.any { it.id == replyId }
            }
        }
        val assistantMsg = messages.first { it.id == replyId }
        assertEquals(listOf("https://developer.android.com/develop/ui/compose/state"), assistantMsg.webSources)

        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { store.renameChatSession(sessionId, "Compose State Thread") }.isSuccess)
        val sessions = withTimeout(FLOW_TIMEOUT_MS) {
            store.observeChatSessions(uid).first { list -> list.any { it.id == sessionId && it.title == "Compose State Thread" } }
        }
        assertTrue(sessions.any { it.id == sessionId && it.messageCount >= 2L })

        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { store.clearSessionMessages(sessionId) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { store.deleteChatSession(sessionId) }.isSuccess)
    }

    @Test
    fun pluginsAndBrainRuns_authenticatedOwner_seedToggleCustomAndBrainCrudSucceeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val store = FirestoreSessionAndBrainStore(firestore)

        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { store.seedBuiltInPluginsIfMissing() }.isSuccess)
        val seeded = withTimeout(FLOW_TIMEOUT_MS) {
            store.observePlugins(uid).first { it.any { p -> p.pluginKey == "calculator" } }
        }
        val calcPlugin = seeded.first { it.pluginKey == "calculator" }
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { store.setPluginEnabled(calcPlugin.id, false) }.isSuccess)

        val updatedPlugins = withTimeout(FLOW_TIMEOUT_MS) {
            store.observePlugins(uid).first { list -> list.any { it.id == calcPlugin.id && !it.enabled } }
        }
        assertFalse(updatedPlugins.first { it.id == calcPlugin.id }.enabled)

        val customId = withTimeout(DEFAULT_TIMEOUT_MS) {
            store.addCustomPlugin(
                name = "Concise Architect",
                description = "Formats architecture decisions concisely",
                category = "Architecture",
                systemInstruction = "Always reply with Decision, Tradeoffs, and Risks."
            ).getOrThrow()
        }
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { store.deletePlugin(customId) }.isSuccess)

        val runId = withTimeout(DEFAULT_TIMEOUT_MS) {
            store.saveBrainRun(
                mode = "reasoner",
                prompt = "Analyze microservices vs modular monolith",
                summary = "Modular monolith minimizes ops overhead early.",
                confidence = 91,
                insights = listOf("Start with strict module boundaries", "Avoid premature RPC hops"),
                suggestedTriplesCount = 2,
                modelUsed = "gemini-3.8-flash"
            ).getOrThrow()
        }
        val runs = withTimeout(FLOW_TIMEOUT_MS) {
            store.observeBrainRuns(uid).first { list -> list.any { it.id == runId } }
        }
        assertTrue(runs.any { it.id == runId && it.confidence == 91L })
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { store.deleteBrainRun(runId) }.isSuccess)
    }

    @Test
    fun memoryNotesAndKnowledgeFacts_authenticatedOwner_crudLifecycleSucceeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreRepository(firestore)

        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMemoryNote("Persistent memory item").getOrThrow()
        }
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.updateMemoryNote(noteId, "Updated private memory item")
            }.isSuccess
        )
        val notes = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMemoryNotes(uid).first { list ->
                list.any { it.id == noteId && it.content == "Updated private memory item" }
            }
        }
        assertTrue(notes.any { it.id == noteId && it.content == "Updated private memory item" })

        val factId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addKnowledgeFact("Zaythar AI", "uses", "Firestore").getOrThrow()
        }
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.updateKnowledgeFact(factId, "Zaythar AI", "secures", "per-user memory")
            }.isSuccess
        )
        val facts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeKnowledgeFacts(uid).first { list ->
                list.any { it.id == factId && it.predicate == "secures" }
            }
        }
        assertTrue(facts.any { it.id == factId && it.objectValue == "per-user memory" })

        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.deleteMemoryNote(noteId) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.deleteKnowledgeFact(factId) }.isSuccess)
    }

    @Test
    fun observeSubcollections_crossUserAccess_isRejectedBeforeListenerCreation() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreRepository(firestore)
        val aliceStore = FirestoreSessionAndBrainStore(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addMemoryNote("Alice private note").getOrThrow()
            aliceStore.createChatSession("Alice private session", "general").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreRepository(firestore)
        val bobStore = FirestoreSessionAndBrainStore(firestore)

        // Client-side guards reject a mismatched requested UID before creating a listener.
        // Firestore Security Rules remain the authoritative server-side boundary.
        assertFailsWithType<SecurityException> { bobRepo.observeUserProfile(aliceUid) }
        assertFailsWithType<SecurityException> { bobRepo.observeChatMessages(aliceUid) }
        assertFailsWithType<SecurityException> { bobRepo.observeMemoryNotes(aliceUid) }
        assertFailsWithType<SecurityException> { bobRepo.observeKnowledgeFacts(aliceUid) }
        assertFailsWithType<SecurityException> { bobStore.observeChatSessions(aliceUid) }
        assertFailsWithType<SecurityException> { bobStore.observeSessionMessages(aliceUid, "alice-session") }
        assertFailsWithType<SecurityException> { bobStore.observePlugins(aliceUid) }
        assertFailsWithType<SecurityException> { bobStore.observeBrainRuns(aliceUid) }
    }

    @Test
    fun observeCollections_unauthenticatedUser_isRejectedBeforeListenerCreation() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        Firebase.auth.signOut()
        val repository = FirestoreRepository(firestore)

        val exception = assertFailsWithType<IllegalStateException> {
            repository.observeChatMessages(aliceUid)
        }
        assertTrue(exception.message.orEmpty().contains("signed in"))
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
