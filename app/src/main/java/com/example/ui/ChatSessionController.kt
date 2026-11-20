package com.example.ui

import com.example.data.local.PreferencesRepository
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreChatSession
import com.example.data.remote.FirestoreSessionAndBrainStore
import com.example.domain.AgentRole
import com.example.domain.ChatMessage
import com.example.domain.GuestWorkspaceStore
import com.example.domain.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

internal class ChatSessionController(
    private val workspaceStore: FirestoreSessionAndBrainStore,
    private val prefsRepo: PreferencesRepository,
    private val currentUserId: String,
    private val activeSessionIdFlow: MutableStateFlow<String>,
    private val activeAgentFlow: MutableStateFlow<AgentRole>,
    private val isRestoringFlow: MutableStateFlow<Boolean>,
    private val chatUiStateFlow: MutableStateFlow<UiState<String>>,
    private val onCancelActiveChat: () -> Unit,
    private val onToast: (String) -> Unit,
    private val guestStore: GuestWorkspaceStore? = null
) {

    suspend fun loadSavedSessions(prefsSnapshot: ZaytharPreferences? = null) {
        if (guestStore != null) {
            isRestoringFlow.value = true
            try {
                val id = guestStore.ensureDefaultSession()
                activeSessionIdFlow.value = id
                guestStore.chatSessions.value.find { it.id == id }?.let {
                    activeAgentFlow.value = AgentRole.fromId(it.agent)
                }
            } finally {
                isRestoringFlow.value = false
            }
            return
        }
        isRestoringFlow.value = true
        try {
            val prefs = prefsSnapshot ?: runCatching { prefsRepo.preferences.first() }.getOrNull()
            val savedId = if (prefs?.lastActiveSessionUid == currentUserId) {
                prefs.lastActiveSessionId
            } else {
                ""
            }
            val loadedSessions = workspaceStore.loadChatSessions().getOrNull().orEmpty()
            val targetSession = loadedSessions.find { it.id == savedId } ?: loadedSessions.firstOrNull()

            if (targetSession != null) {
                activeSessionIdFlow.value = targetSession.id
                activeAgentFlow.value = AgentRole.fromId(targetSession.agent)
                if (savedId != targetSession.id) {
                    prefsRepo.setLastActiveSession(currentUserId, targetSession.id)
                }
            } else {
                workspaceStore.ensureDefaultSession().getOrNull()?.let { createdId ->
                    activeSessionIdFlow.value = createdId
                    prefsRepo.setLastActiveSession(currentUserId, createdId)
                }
            }
        } finally {
            isRestoringFlow.value = false
        }
    }

    suspend fun saveSession(
        sessionId: String?,
        title: String,
        agent: AgentRole,
        messages: List<ChatMessage>
    ) {
        val res = if (guestStore != null) {
            guestStore.saveChatSession(sessionId, title, agent.id, messages)
        } else {
            workspaceStore.saveChatSession(
                sessionId = sessionId,
                title = title,
                agent = agent.id,
                messages = messages
            )
        }
        res.onSuccess { savedId ->
            chatUiStateFlow.value = UiState.Idle
            activeSessionIdFlow.value = savedId
            activeAgentFlow.value = agent
            if (guestStore == null) {
                prefsRepo.setLastActiveSession(currentUserId, savedId)
                onToast("Chat session saved to Cloud Firestore.")
            } else {
                onToast("Guest session saved locally.")
            }
        }.onFailure { err ->
            onToast("Could not save session. Please try again.")
        }
    }

    suspend fun selectSession(sessionId: String, sessions: List<FirestoreChatSession>) {
        if (sessionId.isBlank() || sessionId == activeSessionIdFlow.value) return
        onCancelActiveChat()
        chatUiStateFlow.value = UiState.Idle
        activeSessionIdFlow.value = sessionId
        sessions.find { it.id == sessionId }?.let { session ->
            activeAgentFlow.value = AgentRole.fromId(session.agent)
        }
        if (guestStore == null) {
            prefsRepo.setLastActiveSession(currentUserId, sessionId)
        }
    }

    suspend fun createSession(title: String) {
        val res = if (guestStore != null) {
            guestStore.createChatSession(title = title, agent = activeAgentFlow.value.id)
        } else {
            workspaceStore.createChatSession(title = title, agent = activeAgentFlow.value.id)
        }
        res.onSuccess { newId ->
            chatUiStateFlow.value = UiState.Idle
            activeSessionIdFlow.value = newId
            if (guestStore == null) prefsRepo.setLastActiveSession(currentUserId, newId)
            onToast("New conversation thread created.")
        }.onFailure { err ->
            onToast("Could not create thread. Please try again.")
        }
    }

    suspend fun renameSession(sessionId: String, newTitle: String) {
        val cleaned = newTitle.trim()
        if (cleaned.isEmpty()) return
        val res = if (guestStore != null) {
            guestStore.renameChatSession(sessionId, cleaned)
        } else {
            workspaceStore.renameChatSession(sessionId, cleaned)
        }
        if (res.isSuccess) onToast("Thread renamed.")
        else onToast("Rename failed. Please try again.")
    }

    suspend fun deleteSession(sessionId: String) {
        val res = if (guestStore != null) {
            guestStore.deleteChatSession(sessionId)
        } else {
            workspaceStore.deleteChatSession(sessionId)
        }
        if (res.isSuccess) {
            if (activeSessionIdFlow.value == sessionId) {
                val fallback = if (guestStore != null) {
                    guestStore.ensureDefaultSession()
                } else {
                    workspaceStore.ensureDefaultSession().getOrNull().orEmpty()
                }
                activeSessionIdFlow.value = fallback
                if (guestStore == null) prefsRepo.setLastActiveSession(currentUserId, fallback)
            }
            onToast(if (guestStore != null) "Guest thread deleted." else "Thread deleted from Cloud Firestore.")
        } else {
            onToast("Delete failed. Please try again.")
        }
    }

    suspend fun clearCurrentAndStartNewSession(title: String = "New Nexus Thread"): Result<String> {
        onCancelActiveChat()
        val oldSessionId = activeSessionIdFlow.value
        val res = if (guestStore != null) {
            guestStore.clearAndStartNewChatSession(
                sessionId = oldSessionId,
                newTitle = title,
                agent = activeAgentFlow.value.id
            )
        } else {
            workspaceStore.clearAndStartNewChatSession(
                sessionId = oldSessionId,
                newTitle = title,
                agent = activeAgentFlow.value.id
            )
        }
        res.onSuccess { newId ->
            chatUiStateFlow.value = UiState.Idle
            activeSessionIdFlow.value = newId
            if (guestStore == null) {
                prefsRepo.setLastActiveSession(currentUserId, newId)
                onToast("Chat history deleted from Cloud Firestore. New session started.")
            } else {
                onToast("Guest history cleared. New session started.")
            }
        }.onFailure { err ->
            onToast("Failed to clear chat session. Please try again.")
        }
        return res
    }
}
