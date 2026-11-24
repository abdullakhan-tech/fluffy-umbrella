package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.PreferencesRepository
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreChatSession
import com.example.data.remote.FirestorePluginConfig
import com.example.data.remote.FirestoreSessionAndBrainStore
import com.example.domain.AgentRole
import com.example.domain.AiRepository
import com.example.domain.ChatStreamEvent
import com.example.domain.GuestWorkspaceStore
import com.example.domain.UiState
import com.example.domain.ZaytharSystemPrompts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val RUNNER_TAG = "ChatReplyRunner"

internal class ChatReplyRunner(
    private val aiRepo: AiRepository,
    private val workspaceStore: FirestoreSessionAndBrainStore,
    private val prefsRepo: PreferencesRepository,
    private val ttsSpeaker: TtsSpeaker,
    private val currentUserId: String,
    private val guestStore: GuestWorkspaceStore?,
    private val activeSessionIdFlow: MutableStateFlow<String>,
    private val chatUiStateFlow: MutableStateFlow<UiState<String>>,
    private val thoughtTracesFlow: MutableStateFlow<Map<String, String>>,
    private val onToast: (String) -> Unit
) {

    suspend fun execute(
        text: String,
        attachedImage: Bitmap?,
        agent: AgentRole,
        currentPrefs: ZaytharPreferences,
        sessions: List<FirestoreChatSession>,
        activePlugins: List<FirestorePluginConfig>
    ) {
        val sessionId = activeSessionIdFlow.value.ifBlank {
            if (guestStore != null) {
                guestStore.ensureDefaultSession().also { activeSessionIdFlow.value = it }
            } else {
                workspaceStore.ensureDefaultSession().getOrNull().orEmpty().also {
                    activeSessionIdFlow.value = it
                    prefsRepo.setLastActiveSession(currentUserId, it)
                }
            }
        }
        if (sessionId.isBlank()) {
            val msg = "Unable to initialize chat session."
            chatUiStateFlow.value = UiState.Error(msg)
            onToast(msg)
            return
        }

        val historySnapshot = guestStore?.getRecentSessionMessages(sessionId)
            ?: workspaceStore.getRecentSessionMessages(sessionId)
        val activeSession = sessions.find { it.id == sessionId }
        val autoTitle = if (activeSession != null && activeSession.messageCount == 0L &&
            (activeSession.title == "Primary Nexus" || activeSession.title == "New Nexus Thread")
        ) text else null

        val userMsgRes = guestStore?.addSessionMessage(
            sessionId = sessionId,
            role = "user",
            text = text,
            agent = agent.id,
            hasImage = attachedImage != null,
            autoTitleFromFirstPrompt = autoTitle
        ) ?: workspaceStore.addSessionMessage(
            sessionId = sessionId,
            role = "user",
            text = text,
            agent = agent.id,
            hasImage = attachedImage != null,
            autoTitleFromFirstPrompt = autoTitle
        )
        if (userMsgRes.isFailure) {
            val msg = userMsgRes.exceptionOrNull()?.localizedMessage ?: "Failed to save message."
            chatUiStateFlow.value = UiState.Error(msg)
            onToast(msg)
            return
        }

        chatUiStateFlow.value = UiState.Loading
        try {
            var finalReply = ""
            var modelUsed = ""
            var webSources: List<String> = emptyList()
            var webSourceTitles: List<String> = emptyList()
            var searchQueries: List<String> = emptyList()
            var searchEntryPointHtml = ""
            var thoughtSummary = ""

            aiRepo.streamChatReply(
                message = text,
                agent = agent,
                prefs = currentPrefs,
                recentHistory = historySnapshot,
                activePlugins = activePlugins,
                webConnectEnabled = currentPrefs.webConnectEnabled,
                mapsGroundingEnabled = currentPrefs.mapsGroundingEnabled,
                attachedImage = attachedImage
            ).collect { event ->
                when (event) {
                    is ChatStreamEvent.Chunk -> chatUiStateFlow.value = UiState.Streaming(
                        partialText = event.accumulatedText,
                        searchQueries = event.searchQueries,
                        webSources = event.webSources,
                        partialThoughts = event.accumulatedThoughts
                    )
                    is ChatStreamEvent.Completed -> {
                        finalReply = event.finalText
                        modelUsed = event.modelLabel
                        webSources = event.webSources
                        webSourceTitles = event.webSourceTitles
                        searchQueries = event.searchQueries
                        searchEntryPointHtml = event.searchEntryPointHtml
                        thoughtSummary = event.thoughtSummary
                    }
                }
            }

            if (finalReply.isNotBlank()) {
                val saveReply = guestStore?.addSessionMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    text = finalReply,
                    agent = agent.id,
                    modelLabel = modelUsed,
                    hasImage = attachedImage != null,
                    webSources = webSources,
                    webSourceTitles = webSourceTitles,
                    searchQueries = searchQueries,
                    searchEntryPointHtml = searchEntryPointHtml
                ) ?: workspaceStore.addSessionMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    text = finalReply,
                    agent = agent.id,
                    modelLabel = modelUsed,
                    hasImage = attachedImage != null,
                    webSources = webSources,
                    webSourceTitles = webSourceTitles,
                    searchQueries = searchQueries,
                    searchEntryPointHtml = searchEntryPointHtml
                )
                val savedMsgId = saveReply.getOrNull().orEmpty()
                if (savedMsgId.isNotBlank() && thoughtSummary.isNotBlank()) {
                    thoughtTracesFlow.update { current ->
                        (current + (savedMsgId to thoughtSummary)).entries.toList().takeLast(40).associate { it.key to it.value }
                    }
                }
                if (saveReply.isFailure) onToast("Reply generated, but failed to sync to Firestore.")
                chatUiStateFlow.value = UiState.Success(finalReply)
                if (currentPrefs.speakReplies) {
                    ttsSpeaker.speak(
                        ZaytharSystemPrompts.stripMarkdownForSpeech(finalReply),
                        currentPrefs.languageCode
                    )
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(RUNNER_TAG, "Chat timed out", e)
            val msg = "Generation timed out after 60 seconds. Check network connectivity and try again."
            chatUiStateFlow.value = UiState.Error(msg)
            onToast(msg)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(RUNNER_TAG, "Chat generation failed", e)
            val msg = e.localizedMessage ?: "AI service failed to respond. Check Firebase AI Logic & App Check."
            chatUiStateFlow.value = UiState.Error(msg)
            onToast(msg)
        }
    }
}
