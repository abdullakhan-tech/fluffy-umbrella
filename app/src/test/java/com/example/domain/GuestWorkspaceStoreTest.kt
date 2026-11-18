package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestWorkspaceStoreTest {

    @Test
    fun guestSessions_createMessageClearAndStartNewSessionWorksLocally() {
        val store = GuestWorkspaceStore()
        val initialId = store.ensureDefaultSession()
        assertEquals("guest_session_primary", initialId)

        store.addSessionMessage(
            sessionId = initialId,
            role = "user",
            text = "Hello guest Zaythar",
            agent = "general",
            autoTitleFromFirstPrompt = "Hello guest Zaythar"
        )
        assertEquals(1, store.getRecentSessionMessages(initialId).size)
        assertEquals("Hello guest Zaythar", store.chatSessions.value.first().title)

        val newId = store.clearAndStartNewChatSession(initialId, "Fresh Guest Thread", "coder").getOrThrow()
        assertFalse(newId == initialId)
        assertTrue(store.getRecentSessionMessages(initialId).isEmpty())
        assertEquals(1, store.chatSessions.value.size)
        assertEquals("Fresh Guest Thread", store.chatSessions.value.first().title)
    }

    @Test
    fun guestRagMemoryFactsPluginsAndBrainRunsWorkWithoutFirestore() {
        val store = GuestWorkspaceStore()
        val noteId = store.addMemoryNote("Kotlin Coroutines structured concurrency").getOrThrow()
        val factId = store.addKnowledgeFact("Zaythar", "runs_in", "Guest Mode", "operator").getOrThrow()
        assertEquals(1, store.memoryNotes.value.size)
        assertEquals(1, store.knowledgeFacts.value.size)

        val rag = RagRetrievalEngine.retrieve(
            query = "Guest Mode structured concurrency",
            memories = store.memoryNotes.value,
            facts = store.knowledgeFacts.value
        )
        assertTrue(rag.hasMatches)

        val firstPlugin = store.plugins.value.first()
        store.setPluginEnabled(firstPlugin.id, !firstPlugin.enabled)
        assertEquals(!firstPlugin.enabled, store.plugins.value.first().enabled)

        store.saveBrainRun("reasoner", "Analyze guest flow", "Local only", 90, listOf("Fast"), 1, "gemini-3.8-flash")
        assertEquals(1, store.brainRuns.value.size)

        store.deleteMemoryNote(noteId)
        store.deleteKnowledgeFact(factId)
        assertTrue(store.memoryNotes.value.isEmpty())
        assertTrue(store.knowledgeFacts.value.isEmpty())
    }
}
