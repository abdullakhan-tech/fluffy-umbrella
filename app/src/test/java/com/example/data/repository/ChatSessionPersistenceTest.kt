package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.PreferencesRepository
import com.example.data.remote.FirestoreRepository
import com.example.data.remote.FirestoreSessionAndBrainStore
import com.example.domain.AgentRole
import com.example.domain.ChatMessage
import com.example.domain.UiState
import com.example.ui.TtsSpeaker
import com.example.ui.ZaytharViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.shadows.ShadowLooper

class ChatSessionPersistenceTest : FirestoreEmulatorTestBase() {

    private val noOpSpeaker = object : TtsSpeaker {
        override fun speak(text: String, languageCode: String) {}
        override fun stop() {}
        override fun shutdown() {}
    }

    @Test
    fun saveChatSession_and_loadChatSessions_persistsAndRestoresMultipleThreadsWithMessages() = runBlocking {
        signInTestUser("session_alice@test.com")
        val writerStore = FirestoreSessionAndBrainStore(firestore)

        val firstThreadId = withTimeout(TIMEOUT_MS) {
            writerStore.saveChatSession(
                title = "Kotlin Architecture",
                agent = AgentRole.CODING.id,
                messages = listOf(
                    ChatMessage(id = "u_arch_1", text = "Design clean MVVM layers", isFromUser = true, agentId = AgentRole.CODING.id),
                    ChatMessage(
                        id = "a_arch_1",
                        text = "Keep domain models pure and isolate Firestore repositories.",
                        isFromUser = false,
                        agentId = AgentRole.CODING.id,
                        modelLabel = "gemini-3.8-flash"
                    )
                )
            ).getOrThrow()
        }

        val secondThreadId = withTimeout(TIMEOUT_MS) {
            writerStore.saveChatSession(
                title = "Orbital Research",
                agent = AgentRole.RESEARCH.id,
                messages = listOf(
                    ChatMessage(id = "u_orb_1", text = "Summarize Artemis telemetry", isFromUser = true, agentId = AgentRole.RESEARCH.id)
                )
            ).getOrThrow()
        }

        // Simulate app restart with a fresh store instance loading directly from Cloud Firestore
        val restartedStore = FirestoreSessionAndBrainStore(firestore)
        val loadedSessions = withTimeout(TIMEOUT_MS) {
            restartedStore.loadChatSessions().getOrThrow()
        }

        val firstLoaded = loadedSessions.find { it.id == firstThreadId }
        val secondLoaded = loadedSessions.find { it.id == secondThreadId }
        assertEquals("Kotlin Architecture", firstLoaded?.title)
        assertEquals(AgentRole.CODING.id, firstLoaded?.agent)
        assertEquals(2L, firstLoaded?.messageCount)
        assertEquals("Orbital Research", secondLoaded?.title)
        assertEquals(AgentRole.RESEARCH.id, secondLoaded?.agent)
        assertEquals(1L, secondLoaded?.messageCount)

        val firstMessages = withTimeout(TIMEOUT_MS) {
            restartedStore.loadSessionMessages(firstThreadId).getOrThrow()
        }
        assertEquals(2, firstMessages.size)
        assertEquals("Design clean MVVM layers", firstMessages.first().text)
        assertEquals("Keep domain models pure and isolate Firestore repositories.", firstMessages.last().text)
    }

    @Test
    fun saveChatSession_existingThread_appendsNewMessagesIdempotently() = runBlocking {
        signInTestUser("session_bob@test.com")
        val store = FirestoreSessionAndBrainStore(firestore)

        val sessionId = withTimeout(TIMEOUT_MS) {
            store.saveChatSession(
                title = "Incremental Thread",
                agent = AgentRole.GENERAL.id,
                messages = listOf(
                    ChatMessage(id = "msg_fixed_1", text = "First prompt", isFromUser = true)
                )
            ).getOrThrow()
        }

        withTimeout(TIMEOUT_MS) {
            store.saveChatSession(
                sessionId = sessionId,
                title = "Incremental Thread Updated",
                agent = AgentRole.ANALYSIS.id,
                messages = listOf(
                    ChatMessage(id = "msg_fixed_1", text = "First prompt", isFromUser = true),
                    ChatMessage(id = "msg_fixed_2", text = "Strategic response", isFromUser = false)
                )
            ).getOrThrow()
        }

        val sessions = withTimeout(TIMEOUT_MS) { store.loadChatSessions().getOrThrow() }
        val updated = sessions.first { it.id == sessionId }
        assertEquals("Incremental Thread Updated", updated.title)
        assertEquals(AgentRole.ANALYSIS.id, updated.agent)
        assertEquals(2L, updated.messageCount)

        val loadedMessages = withTimeout(TIMEOUT_MS) { store.loadSessionMessages(sessionId).getOrThrow() }
        assertEquals(2, loadedMessages.size)
        assertEquals(listOf("First prompt", "Strategic response"), loadedMessages.map { it.text })
    }

    @Test
    fun viewModel_restartRecovery_restoresSwitchedThreadAndLoadsItsMessages() = runBlocking {
        val uid = signInTestUser("session_restart@test.com")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = PreferencesRepository(context)
        prefsRepo.setLastActiveSession("", "")

        val store = FirestoreSessionAndBrainStore(firestore)
        val alphaId = withTimeout(TIMEOUT_MS) {
            store.saveChatSession(
                title = "Alpha Research",
                agent = AgentRole.RESEARCH.id,
                messages = listOf(
                    ChatMessage(id = "alpha_1", text = "Alpha question", isFromUser = true, agentId = AgentRole.RESEARCH.id),
                    ChatMessage(id = "alpha_2", text = "Alpha verified answer", isFromUser = false, agentId = AgentRole.RESEARCH.id)
                )
            ).getOrThrow()
        }
        val betaId = withTimeout(TIMEOUT_MS) {
            store.saveChatSession(
                title = "Beta Code",
                agent = AgentRole.CODING.id,
                messages = listOf(
                    ChatMessage(id = "beta_1", text = "Beta code task", isFromUser = true, agentId = AgentRole.CODING.id)
                )
            ).getOrThrow()
        }

        // First app launch: switch explicitly to Alpha (even though Beta was created more recently)
        val vm1 = ZaytharViewModel(
            firestoreRepo = FirestoreRepository(firestore),
            workspaceStore = FirestoreSessionAndBrainStore(firestore),
            prefsRepo = prefsRepo,
            ttsSpeaker = noOpSpeaker,
            currentUserId = uid,
            firebaseInitialized = true
        )
        val vm1Collector = listOf(
            this@runBlocking.launch { vm1.chatSessionsState.collect {} },
            this@runBlocking.launch { vm1.sessionMessagesState.collect {} }
        )
        awaitUntil(TIMEOUT_MS) {
            vm1.activeSessionId.value.isNotBlank() && vm1.chatSessionsState.value is UiState.Success
        }

        vm1.selectChatSession(alphaId)
        awaitUntil(TIMEOUT_MS) {
            val p = prefsRepo.preferences.first()
            p.lastActiveSessionUid == uid && p.lastActiveSessionId == alphaId
        }
        assertEquals(alphaId, vm1.activeSessionId.value)
        vm1Collector.forEach { it.cancel() }

        // Simulate app restart: recreate ViewModel with same user ID, DataStore, and Firestore backend
        val vm2 = ZaytharViewModel(
            firestoreRepo = FirestoreRepository(firestore),
            workspaceStore = FirestoreSessionAndBrainStore(firestore),
            prefsRepo = prefsRepo,
            ttsSpeaker = noOpSpeaker,
            currentUserId = uid,
            firebaseInitialized = true
        )
        val vm2Collector = listOf(
            this@runBlocking.launch { vm2.chatSessionsState.collect {} },
            this@runBlocking.launch { vm2.sessionMessagesState.collect {} }
        )

        try {
            awaitUntil(TIMEOUT_MS) {
                vm2.activeSessionId.value == alphaId &&
                    (vm2.sessionMessagesState.value as? UiState.Success)?.data?.any { it.text == "Alpha verified answer" } == true
            }
            assertEquals(AgentRole.RESEARCH, vm2.activeAgent.value)

            // Now switch to Beta thread in the restarted ViewModel and confirm messages switch cleanly
            vm2.selectChatSession(betaId)
            awaitUntil(TIMEOUT_MS) {
                vm2.activeSessionId.value == betaId &&
                    (vm2.sessionMessagesState.value as? UiState.Success)?.data?.any { it.text == "Beta code task" } == true
            }
            assertTrue(
                (vm2.sessionMessagesState.value as UiState.Success).data.none { it.text == "Alpha verified answer" }
            )
        } finally {
            vm2Collector.forEach { it.cancel() }
        }
    }

    @Test
    fun viewModel_clearChatAndStartNewSession_deletesFirestoreDocsAndStartsFreshThread() = runBlocking {
        val uid = signInTestUser("session_clear@test.com")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = PreferencesRepository(context)
        prefsRepo.setLastActiveSession("", "")

        val store = FirestoreSessionAndBrainStore(firestore)
        val oldSessionId = withTimeout(TIMEOUT_MS) {
            store.saveChatSession(
                title = "Temporary Session",
                agent = AgentRole.GENERAL.id,
                messages = listOf(
                    ChatMessage(id = "clear_u1", text = "Old user question", isFromUser = true),
                    ChatMessage(id = "clear_a1", text = "Old assistant reply", isFromUser = false)
                )
            ).getOrThrow()
        }

        val vm = ZaytharViewModel(
            firestoreRepo = FirestoreRepository(firestore),
            workspaceStore = store,
            prefsRepo = prefsRepo,
            ttsSpeaker = noOpSpeaker,
            currentUserId = uid,
            firebaseInitialized = true
        )
        val collectors = listOf(
            this@runBlocking.launch { vm.chatSessionsState.collect {} },
            this@runBlocking.launch { vm.sessionMessagesState.collect {} }
        )

        try {
            awaitUntil(TIMEOUT_MS) {
                vm.activeSessionId.value == oldSessionId &&
                    (vm.sessionMessagesState.value as? UiState.Success)?.data?.size == 2
            }

            vm.updateComposerInput("unsent draft")
            vm.clearChatAndStartNewSession()

            awaitUntil(TIMEOUT_MS) {
                vm.activeSessionId.value.isNotBlank() &&
                    vm.activeSessionId.value != oldSessionId &&
                    (vm.sessionMessagesState.value as? UiState.Success)?.data?.isEmpty() == true
            }

            val newSessionId = vm.activeSessionId.value
            assertNotEquals(oldSessionId, newSessionId)
            assertEquals("", vm.composerInput.value)

            val oldSessionRef = firestore.collection("users").document(uid)
                .collection("chat_sessions").document(oldSessionId)
            val oldSessionDoc = withTimeout(TIMEOUT_MS) { oldSessionRef.get().await() }
            val oldMessageDocs = withTimeout(TIMEOUT_MS) { oldSessionRef.collection("messages").get().await() }
            assertFalse(oldSessionDoc.exists())
            assertTrue(oldMessageDocs.isEmpty)

            val newSessionDoc = withTimeout(TIMEOUT_MS) {
                firestore.collection("users").document(uid)
                    .collection("chat_sessions").document(newSessionId).get().await()
            }
            assertTrue(newSessionDoc.exists())
            assertEquals(0L, newSessionDoc.getLong("messageCount"))
        } finally {
            collectors.forEach { it.cancel() }
        }
    }

    private suspend fun awaitUntil(timeoutMs: Long, predicate: suspend () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!predicate()) {
                ShadowLooper.idleMainLooper()
                delay(40)
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 6000L
    }
}
