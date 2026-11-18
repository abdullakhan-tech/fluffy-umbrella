package com.example.domain

import com.example.data.remote.FirestoreBrainRun
import com.example.data.remote.FirestoreChatSession
import com.example.data.remote.FirestoreKnowledgeFact
import com.example.data.remote.FirestoreMemoryNote
import com.example.data.remote.FirestorePluginConfig
import com.example.data.remote.FirestoreSessionMessage
import com.example.data.remote.UserProfileDoc
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

const val GUEST_USER_ID = "__zaythar_guest__"

class GuestWorkspaceStore {

    private fun newId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(12)}"

    private val initialSession = FirestoreChatSession(
        id = "guest_session_primary",
        userId = GUEST_USER_ID,
        title = "Primary Nexus",
        agent = AgentRole.GENERAL.id,
        lastMessagePreview = "",
        messageCount = 0L,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now()
    )

    private val _userProfile = MutableStateFlow<UserProfileDoc?>(
        UserProfileDoc(
            id = GUEST_USER_ID,
            userId = GUEST_USER_ID,
            operatorName = "GUEST OPERATOR",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
    )
    val userProfile: StateFlow<UserProfileDoc?> = _userProfile.asStateFlow()

    private val _chatSessions = MutableStateFlow(listOf(initialSession))
    val chatSessions: StateFlow<List<FirestoreChatSession>> = _chatSessions.asStateFlow()

    private val _messagesBySession = MutableStateFlow<Map<String, List<FirestoreSessionMessage>>>(emptyMap())

    private val _memoryNotes = MutableStateFlow<List<FirestoreMemoryNote>>(emptyList())
    val memoryNotes: StateFlow<List<FirestoreMemoryNote>> = _memoryNotes.asStateFlow()

    private val _knowledgeFacts = MutableStateFlow<List<FirestoreKnowledgeFact>>(emptyList())
    val knowledgeFacts: StateFlow<List<FirestoreKnowledgeFact>> = _knowledgeFacts.asStateFlow()

    private val _brainRuns = MutableStateFlow<List<FirestoreBrainRun>>(emptyList())
    val brainRuns: StateFlow<List<FirestoreBrainRun>> = _brainRuns.asStateFlow()

    private val _plugins = MutableStateFlow(
        defaultBuiltInPlugins.map { spec ->
            FirestorePluginConfig(
                id = "builtin_${spec.pluginKey}",
                userId = GUEST_USER_ID,
                pluginKey = spec.pluginKey,
                name = spec.name,
                description = spec.description,
                category = spec.category,
                enabled = spec.defaultEnabled,
                systemInstruction = spec.systemInstruction,
                isBuiltIn = true,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        }
    )
    val plugins: StateFlow<List<FirestorePluginConfig>> = _plugins.asStateFlow()

    fun observeSessionMessages(sessionId: String): Flow<List<FirestoreSessionMessage>> =
        _messagesBySession.map { it[sessionId].orEmpty() }

    fun getRecentSessionMessages(sessionId: String, limit: Int = 20): List<FirestoreSessionMessage> =
        _messagesBySession.value[sessionId].orEmpty().takeLast(limit)

    fun updateOperatorName(rawName: String): Result<Unit> {
        val cleaned = rawName.trim().take(32).ifBlank { "GUEST OPERATOR" }
        _userProfile.update { current ->
            (current ?: UserProfileDoc(id = GUEST_USER_ID, userId = GUEST_USER_ID)).copy(
                operatorName = cleaned,
                updatedAt = Timestamp.now()
            )
        }
        return Result.success(Unit)
    }

    fun ensureDefaultSession(): String {
        val current = _chatSessions.value
        if (current.isNotEmpty()) return current.first().id
        val created = FirestoreChatSession(
            id = newId("gsess"),
            userId = GUEST_USER_ID,
            title = "Primary Nexus",
            agent = AgentRole.GENERAL.id,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _chatSessions.value = listOf(created)
        return created.id
    }

    fun createChatSession(title: String = "New Nexus Thread", agent: String = "general"): Result<String> {
        val cleaned = title.trim().take(80).ifBlank { "New Nexus Thread" }
        val session = FirestoreChatSession(
            id = newId("gsess"),
            userId = GUEST_USER_ID,
            title = cleaned,
            agent = agent,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _chatSessions.update { listOf(session) + it }
        return Result.success(session.id)
    }

    fun saveChatSession(
        sessionId: String?,
        title: String,
        agent: String,
        messages: List<ChatMessage>
    ): Result<String> {
        val targetId = sessionId?.takeIf { it.isNotBlank() } ?: createChatSession(title, agent).getOrThrow()
        val now = Timestamp.now()
        val mapped = messages.takeLast(120).map { msg ->
            FirestoreSessionMessage(
                id = msg.id.ifBlank { newId("gmsg") },
                userId = GUEST_USER_ID,
                sessionId = targetId,
                role = if (msg.isFromUser) "user" else "assistant",
                text = msg.text.take(12_000),
                agent = msg.agentId,
                modelLabel = msg.modelLabel,
                hasImage = msg.hasImage,
                webSources = msg.webSources,
                webSourceTitles = msg.webSourceTitles,
                searchQueries = msg.searchQueries,
                searchEntryPointHtml = msg.searchEntryPointHtml,
                createdAt = now,
                updatedAt = now
            )
        }
        _messagesBySession.update { it + (targetId to mapped) }
        _chatSessions.update { list ->
            val preview = mapped.lastOrNull()?.text?.take(120).orEmpty()
            val existing = list.any { it.id == targetId }
            val updatedList = if (existing) {
                list.map {
                    if (it.id == targetId) {
                        it.copy(
                            title = title.trim().take(80).ifBlank { it.title },
                            agent = agent,
                            lastMessagePreview = preview,
                            messageCount = mapped.size.toLong(),
                            updatedAt = now
                        )
                    } else it
                }
            } else {
                listOf(
                    FirestoreChatSession(
                        id = targetId,
                        userId = GUEST_USER_ID,
                        title = title.trim().take(80).ifBlank { "New Nexus Thread" },
                        agent = agent,
                        lastMessagePreview = preview,
                        messageCount = mapped.size.toLong(),
                        createdAt = now,
                        updatedAt = now
                    )
                ) + list
            }
            updatedList.sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
        }
        return Result.success(targetId)
    }

    fun renameChatSession(sessionId: String, newTitle: String): Result<Unit> {
        val cleaned = newTitle.trim().take(80)
        if (cleaned.isEmpty()) return Result.failure(IllegalArgumentException("Session title cannot be empty"))
        _chatSessions.update { list ->
            list.map { if (it.id == sessionId) it.copy(title = cleaned, updatedAt = Timestamp.now()) else it }
        }
        return Result.success(Unit)
    }

    fun deleteChatSession(sessionId: String): Result<Unit> {
        _messagesBySession.update { it - sessionId }
        _chatSessions.update { list -> list.filterNot { it.id == sessionId } }
        return Result.success(Unit)
    }

    fun clearSessionMessages(sessionId: String): Result<Unit> {
        _messagesBySession.update { it + (sessionId to emptyList()) }
        _chatSessions.update { list ->
            list.map {
                if (it.id == sessionId) it.copy(lastMessagePreview = "", messageCount = 0L, updatedAt = Timestamp.now())
                else it
            }
        }
        return Result.success(Unit)
    }

    fun clearAndStartNewChatSession(
        sessionId: String,
        newTitle: String = "New Nexus Thread",
        agent: String = "general"
    ): Result<String> {
        if (sessionId.isNotBlank()) deleteChatSession(sessionId)
        return createChatSession(newTitle, agent)
    }

    fun addSessionMessage(
        sessionId: String,
        role: String,
        text: String,
        agent: String,
        modelLabel: String = "",
        hasImage: Boolean = false,
        webSources: List<String> = emptyList(),
        webSourceTitles: List<String> = emptyList(),
        searchQueries: List<String> = emptyList(),
        searchEntryPointHtml: String = "",
        autoTitleFromFirstPrompt: String? = null
    ): Result<String> {
        val now = Timestamp.now()
        val msgId = newId("gmsg")
        val item = FirestoreSessionMessage(
            id = msgId,
            userId = GUEST_USER_ID,
            sessionId = sessionId,
            role = role,
            text = text.take(12_000),
            agent = agent,
            modelLabel = modelLabel,
            hasImage = hasImage,
            webSources = webSources,
            webSourceTitles = webSourceTitles,
            searchQueries = searchQueries,
            searchEntryPointHtml = searchEntryPointHtml,
            createdAt = now,
            updatedAt = now
        )
        _messagesBySession.update { map ->
            val current = map[sessionId].orEmpty()
            map + (sessionId to (current + item))
        }
        _chatSessions.update { sessions ->
            sessions.map { s ->
                if (s.id == sessionId) {
                    val updatedTitle = autoTitleFromFirstPrompt?.trim()?.take(80)?.takeIf { it.isNotEmpty() } ?: s.title
                    s.copy(
                        title = updatedTitle,
                        agent = agent,
                        lastMessagePreview = text.trim().take(120),
                        messageCount = (_messagesBySession.value[sessionId]?.size ?: 0).toLong(),
                        updatedAt = now
                    )
                } else s
            }.sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
        }
        return Result.success(msgId)
    }

    fun addMemoryNote(content: String): Result<String> {
        val cleaned = content.trim().take(2000)
        if (cleaned.isEmpty()) return Result.failure(IllegalArgumentException("Note cannot be empty"))
        val id = newId("gmem")
        val note = FirestoreMemoryNote(
            id = id,
            userId = GUEST_USER_ID,
            content = cleaned,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _memoryNotes.update { listOf(note) + it }
        return Result.success(id)
    }

    fun deleteMemoryNote(id: String): Result<Unit> {
        _memoryNotes.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }

    fun addKnowledgeFact(
        subject: String,
        predicate: String,
        objectValue: String,
        source: String = "operator"
    ): Result<String> {
        if (_knowledgeFacts.value.size >= 200) {
            return Result.failure(IllegalStateException("Knowledge graph limit reached (200 facts)."))
        }
        val id = newId("gfact")
        val fact = FirestoreKnowledgeFact(
            id = id,
            userId = GUEST_USER_ID,
            subject = subject.trim().take(300),
            predicate = predicate.trim().take(300),
            objectValue = objectValue.trim().take(300),
            source = source.trim().take(80),
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _knowledgeFacts.update { listOf(fact) + it }
        return Result.success(id)
    }

    fun deleteKnowledgeFact(id: String): Result<Unit> {
        _knowledgeFacts.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }

    fun setPluginEnabled(pluginId: String, enabled: Boolean): Result<Unit> {
        _plugins.update { list ->
            list.map { if (it.id == pluginId) it.copy(enabled = enabled, updatedAt = Timestamp.now()) else it }
        }
        return Result.success(Unit)
    }

    fun addCustomPlugin(
        name: String,
        description: String,
        category: String,
        systemInstruction: String
    ): Result<String> {
        val id = newId("gplug")
        val key = "custom_${name.lowercase().replace(Regex("[^a-z0-9]"), "_").take(24).ifBlank { "tool" }}"
        val plugin = FirestorePluginConfig(
            id = id,
            userId = GUEST_USER_ID,
            pluginKey = key,
            name = name.trim().take(80),
            description = description.trim().take(400),
            category = category.trim().take(40).ifBlank { "Custom" },
            enabled = true,
            systemInstruction = systemInstruction.trim().take(2000),
            isBuiltIn = false,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _plugins.update { it + plugin }
        return Result.success(id)
    }

    fun deletePlugin(pluginId: String): Result<Unit> {
        _plugins.update { list -> list.filterNot { it.id == pluginId && !it.isBuiltIn } }
        return Result.success(Unit)
    }

    fun saveBrainRun(
        mode: String,
        prompt: String,
        summary: String,
        confidence: Int,
        insights: List<String>,
        suggestedTriplesCount: Int,
        modelUsed: String
    ): Result<String> {
        val id = newId("gbrain")
        val run = FirestoreBrainRun(
            id = id,
            userId = GUEST_USER_ID,
            mode = mode,
            prompt = prompt.take(2000),
            summary = summary.take(4000),
            confidence = confidence.toLong(),
            insights = insights.take(10),
            suggestedTriplesCount = suggestedTriplesCount.toLong(),
            modelUsed = modelUsed,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _brainRuns.update { (listOf(run) + it).take(30) }
        return Result.success(id)
    }

    fun deleteBrainRun(runId: String): Result<Unit> {
        _brainRuns.update { list -> list.filterNot { it.id == runId } }
        return Result.success(Unit)
    }
}
