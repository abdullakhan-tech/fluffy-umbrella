package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.local.PreferencesRepository
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreChatSession
import com.example.data.remote.FirestorePluginConfig
import com.example.data.remote.FirestoreRepository
import com.example.data.remote.FirestoreSessionAndBrainStore
import com.example.data.remote.FirestoreSessionMessage
import com.example.data.remote.UserProfileDoc
import com.example.domain.AgentRole
import com.example.domain.AiRepository
import com.example.domain.ChatMessage
import com.example.domain.ChatStreamEvent
import com.example.domain.GuestWorkspaceStore
import com.example.domain.ProviderRoute
import com.example.domain.SupportedLanguage
import com.example.domain.UiState
import com.example.domain.toChatMessage
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val TAG = "ZaytharViewModel"

@OptIn(ExperimentalCoroutinesApi::class)
class ZaytharViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val workspaceStore: FirestoreSessionAndBrainStore,
    private val prefsRepo: PreferencesRepository,
    private val ttsSpeaker: TtsSpeaker,
    private val currentUserId: String,
    firebaseInitialized: Boolean,
    private val guestStore: GuestWorkspaceStore? = null
) : ViewModel() {

    private val aiRepo = AiRepository(firestoreRepo, workspaceStore, guestStore)
    private val _isRestoringSession = MutableStateFlow(true)
    private val _activeSessionId = MutableStateFlow("")
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    private val _activeAgent = MutableStateFlow(AgentRole.GENERAL)
    val activeAgent: StateFlow<AgentRole> = _activeAgent.asStateFlow()

    private val _chatUiState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val chatUiState: StateFlow<UiState<String>> = _chatUiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 12)
    val toastEvents = _toastEvents.asSharedFlow()

    private var chatJob: Job? = null

    private val sessionController = ChatSessionController(
        workspaceStore = workspaceStore,
        prefsRepo = prefsRepo,
        currentUserId = currentUserId,
        activeSessionIdFlow = _activeSessionId,
        activeAgentFlow = _activeAgent,
        isRestoringFlow = _isRestoringSession,
        chatUiStateFlow = _chatUiState,
        onCancelActiveChat = { chatJob?.cancel() },
        onToast = ::emitToast,
        guestStore = guestStore
    )

    val preferences: StateFlow<ZaytharPreferences> = prefsRepo.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ZaytharPreferences())

    val userProfileState: StateFlow<UiState<UserProfileDoc?>> = (
        guestStore?.userProfile ?: firestoreRepo.observeUserProfile(currentUserId)
    ).map<UserProfileDoc?, UiState<UserProfileDoc?>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapFirestoreErrorOrRethrow(error)
            Log.w(TAG, "Error observing user profile (${rootError.javaClass.simpleName}).")
            emit(UiState.Error("User profile is temporarily unavailable."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val chatSessionsState: StateFlow<UiState<List<FirestoreChatSession>>> = (
        guestStore?.chatSessions ?: workspaceStore.observeChatSessions(currentUserId)
    ).map<List<FirestoreChatSession>, UiState<List<FirestoreChatSession>>> { sessions ->
        if (!_isRestoringSession.value && sessions.isNotEmpty() && _activeSessionId.value.isBlank()) {
            _activeSessionId.value = sessions.first().id
        }
        UiState.Success(sessions)
    }.catch { error ->
        val rootError = unwrapFirestoreErrorOrRethrow(error)
        Log.w(TAG, "Error observing chat sessions (${rootError.javaClass.simpleName}).")
        emit(UiState.Error("Chat threads are temporarily unavailable."))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val sessionMessagesState: StateFlow<UiState<List<FirestoreSessionMessage>>> = _activeSessionId
        .flatMapLatest { sessionId ->
            if (sessionId.isBlank()) {
                flowOf<UiState<List<FirestoreSessionMessage>>>(UiState.Loading)
            } else {
                val flow = guestStore?.observeSessionMessages(sessionId)
                    ?: workspaceStore.observeSessionMessages(currentUserId, sessionId)
                flow.map<List<FirestoreSessionMessage>, UiState<List<FirestoreSessionMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val rootError = unwrapFirestoreErrorOrRethrow(error)
                        Log.w(TAG, "Error observing session messages (${rootError.javaClass.simpleName}).")
                        emit(UiState.Error("Session history is temporarily unavailable."))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activePluginsFlow: StateFlow<List<FirestorePluginConfig>> = (
        guestStore?.plugins ?: workspaceStore.observePlugins(currentUserId)
    ).catch { error ->
        unwrapFirestoreErrorOrRethrow(error)
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _composerInput = MutableStateFlow("")
    val composerInput: StateFlow<String> = _composerInput.asStateFlow()

    private val _uptimeSeconds = MutableStateFlow(0L)
    val uptimeSeconds: StateFlow<Long> = _uptimeSeconds.asStateFlow()

    private val _clockText = MutableStateFlow(formatCurrentTime())
    val clockText: StateFlow<String> = _clockText.asStateFlow()

    private val _firebaseReady = MutableStateFlow(firebaseInitialized)
    val firebaseReady: StateFlow<Boolean> = _firebaseReady.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val prefsSnapshot = runCatching { prefsRepo.preferences.first() }.getOrNull()
                if (guestStore == null) {
                    val defaultName = prefsSnapshot?.operatorName ?: "OPERATOR 01"
                    val profileRes = firestoreRepo.ensureUserProfile(defaultName)
                    profileRes.getOrNull()?.let { doc ->
                        if (doc.operatorName.isNotBlank() && doc.operatorName != defaultName) {
                            prefsRepo.update { it.copy(operatorName = doc.operatorName) }
                        }
                    }
                    sessionController.loadSavedSessions(prefsSnapshot)
                    workspaceStore.seedBuiltInPluginsIfMissing()
                } else {
                    sessionController.loadSavedSessions(prefsSnapshot)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Deferred startup sync failed (${e.javaClass.simpleName}).")
                _isRestoringSession.value = false
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _uptimeSeconds.value += 1L
                _clockText.value = formatCurrentTime()
            }
        }
    }

    private fun unwrapFirestoreErrorOrRethrow(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private fun formatCurrentTime(): String =
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    fun emitToast(message: String) {
        _toastEvents.tryEmit(message)
    }

    fun updateComposerInput(value: String) {
        _composerInput.value = value
    }

    fun selectAgent(role: AgentRole) {
        _activeAgent.value = role
    }

    fun applyPromptTemplate(prefix: String) {
        _composerInput.value = prefix
    }

    fun setThemeMode(mode: com.example.ui.theme.ThemeMode) {
        viewModelScope.launch {
            prefsRepo.update { it.copy(themeMode = mode) }
            emitToast("Chat theme set to ${mode.label}.")
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val next = preferences.value.themeMode.next()
            prefsRepo.update { it.copy(themeMode = next) }
            emitToast("Chat theme set to ${next.label}.")
        }
    }

    fun toggleMotion3d() {
        viewModelScope.launch {
            val next = !preferences.value.motion3dEnabled
            prefsRepo.update { it.copy(motion3dEnabled = next) }
            emitToast(if (next) "Holographic effects enabled." else "Motion effects paused.")
        }
    }

    fun updateAccent(hex: Long) {
        viewModelScope.launch {
            prefsRepo.update { it.copy(accentHex = hex) }
        }
    }

    fun updatePreferences(transform: (ZaytharPreferences) -> ZaytharPreferences) {
        viewModelScope.launch {
            prefsRepo.update(transform)
        }
    }

    fun selectProviderRoute(route: ProviderRoute) {
        viewModelScope.launch {
            prefsRepo.update { it.copy(selectedProviderRoute = route.id) }
            emitToast("${route.title} route selected for future chat requests.")
        }
    }

    fun selectLanguage(lang: SupportedLanguage) {
        viewModelScope.launch {
            prefsRepo.update { it.copy(languageCode = lang.code) }
        }
    }

    fun toggleSpeakReplies(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.update { it.copy(speakReplies = enabled) }
            if (!enabled) ttsSpeaker.stop()
        }
    }

    fun toggleWebConnect(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.update { it.copy(webConnectEnabled = enabled) }
            emitToast(if (enabled) "Google Search Grounding CONNECTED." else "Offline Gemini mode active.")
        }
    }

    fun saveOperatorName(rawName: String) {
        val cleaned = rawName.trim().take(32).ifBlank { if (guestStore != null) "GUEST OPERATOR" else "OPERATOR 01" }
        viewModelScope.launch {
            prefsRepo.update { it.copy(operatorName = cleaned) }
            if (guestStore != null) {
                guestStore.updateOperatorName(cleaned)
                emitToast("Guest operator name updated locally.")
            } else {
                val result = firestoreRepo.updateOperatorName(cleaned)
                emitToast(
                    if (result.isSuccess) "Operator profile synced to Cloud Firestore."
                    else "Could not sync profile to cloud. Please try again."
                )
            }
        }
    }

    // --- MULTI-SESSION CHAT HISTORY ---

    fun loadSavedChatSessions() {
        viewModelScope.launch { sessionController.loadSavedSessions() }
    }

    fun saveCurrentChatSession(titleOverride: String? = null) {
        viewModelScope.launch {
            val sessionId = _activeSessionId.value.ifBlank { null }
            val sessions = (chatSessionsState.value as? UiState.Success)?.data.orEmpty()
            val title = titleOverride?.trim()?.takeIf { it.isNotEmpty() }
                ?: sessions.find { it.id == sessionId }?.title
                ?: "Primary Nexus"
            val messages = ((sessionMessagesState.value as? UiState.Success)?.data ?: emptyList())
                .map { it.toChatMessage() }
            sessionController.saveSession(sessionId, title, _activeAgent.value, messages)
        }
    }

    fun saveChatSession(
        title: String,
        agent: AgentRole = _activeAgent.value,
        messages: List<ChatMessage> = emptyList(),
        sessionId: String? = null
    ) {
        viewModelScope.launch { sessionController.saveSession(sessionId, title, agent, messages) }
    }

    fun selectChatSession(sessionId: String) {
        val sessions = (chatSessionsState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch { sessionController.selectSession(sessionId, sessions) }
    }

    fun createNewChatSession(title: String = "New Nexus Thread") {
        viewModelScope.launch { sessionController.createSession(title) }
    }

    fun renameChatSession(sessionId: String, newTitle: String) {
        viewModelScope.launch { sessionController.renameSession(sessionId, newTitle) }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch { sessionController.deleteSession(sessionId) }
    }

    fun sendChatMessage(rawText: String = _composerInput.value) {
        val text = rawText.trim()
        if (text.isEmpty() || _chatUiState.value is UiState.Loading || _chatUiState.value is UiState.Streaming) {
            return
        }
        if (text.length > 12_000) {
            emitToast("Message exceeds 12,000 character limit.")
            return
        }

        _composerInput.value = ""
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            val currentPrefs = preferences.value
            val agent = _activeAgent.value
            val sessionId = _activeSessionId.value.ifBlank {
                if (guestStore != null) {
                    guestStore.ensureDefaultSession().also { _activeSessionId.value = it }
                } else {
                    workspaceStore.ensureDefaultSession().getOrNull().orEmpty().also {
                        _activeSessionId.value = it
                        prefsRepo.setLastActiveSession(currentUserId, it)
                    }
                }
            }
            if (sessionId.isBlank()) {
                val msg = "Unable to initialize chat session."
                _chatUiState.value = UiState.Error(msg)
                emitToast(msg)
                return@launch
            }

            val historySnapshot = guestStore?.getRecentSessionMessages(sessionId)
                ?: workspaceStore.getRecentSessionMessages(sessionId)
            val sessions = (chatSessionsState.value as? UiState.Success)?.data.orEmpty()
            val activeSession = sessions.find { it.id == sessionId }
            val autoTitle = if (activeSession != null && activeSession.messageCount == 0L &&
                (activeSession.title == "Primary Nexus" || activeSession.title == "New Nexus Thread")
            ) text else null

            val userMsgRes = guestStore?.addSessionMessage(
                sessionId = sessionId, role = "user", text = text, agent = agent.id, autoTitleFromFirstPrompt = autoTitle
            ) ?: workspaceStore.addSessionMessage(
                sessionId = sessionId, role = "user", text = text, agent = agent.id, autoTitleFromFirstPrompt = autoTitle
            )
            if (userMsgRes.isFailure) {
                val msg = "Failed to save message. Please try again."
                _chatUiState.value = UiState.Error(msg)
                emitToast(msg)
                return@launch
            }

            _chatUiState.value = UiState.Loading
            try {
                var finalReply = ""
                var modelUsed = ""
                var webSources: List<String> = emptyList()
                var webSourceTitles: List<String> = emptyList()
                var searchQueries: List<String> = emptyList()
                var searchEntryPointHtml = ""
                aiRepo.streamChatReply(
                    message = text,
                    agent = agent,
                    prefs = currentPrefs,
                    recentHistory = historySnapshot,
                    activePlugins = activePluginsFlow.value,
                    webConnectEnabled = currentPrefs.webConnectEnabled
                ).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Chunk -> _chatUiState.value = UiState.Streaming(
                            event.accumulatedText, event.searchQueries, event.webSources
                        )
                        is ChatStreamEvent.Completed -> {
                            finalReply = event.finalText
                            modelUsed = event.modelLabel
                            webSources = event.webSources
                            webSourceTitles = event.webSourceTitles
                            searchQueries = event.searchQueries
                            searchEntryPointHtml = event.searchEntryPointHtml
                        }
                    }
                }
                if (finalReply.isNotBlank()) {
                    val saveReply = guestStore?.addSessionMessage(
                        sessionId, "assistant", finalReply, agent.id, modelUsed,
                        false, webSources, webSourceTitles, searchQueries, searchEntryPointHtml
                    ) ?: workspaceStore.addSessionMessage(
                        sessionId, "assistant", finalReply, agent.id, modelUsed,
                        false, webSources, webSourceTitles, searchQueries, searchEntryPointHtml
                    )
                    if (saveReply.isFailure) emitToast("Reply generated, but failed to sync to Firestore.")
                    _chatUiState.value = UiState.Success(finalReply)
                    if (currentPrefs.speakReplies) ttsSpeaker.speak(finalReply, currentPrefs.languageCode)
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Chat timed out.")
                val msg = "Generation timed out after 60 seconds. Check network connectivity and try again."
                _chatUiState.value = UiState.Error(msg)
                emitToast(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat generation failed (${e.javaClass.simpleName}).")
                val msg = "AI service failed to respond. Check Firebase AI Logic & App Check."
                _chatUiState.value = UiState.Error(msg)
                emitToast(msg)
            }
        }
    }

    fun clearChat() {
        chatJob?.cancel()
        ttsSpeaker.stop()
        val sessionId = _activeSessionId.value
        if (sessionId.isBlank()) return
        viewModelScope.launch {
            val res = guestStore?.clearSessionMessages(sessionId) ?: workspaceStore.clearSessionMessages(sessionId)
            _chatUiState.value = UiState.Idle
            emitToast(if (res.isSuccess) "Thread messages cleared." else "Failed to clear conversation.")
        }
    }

    fun clearChatAndStartNewSession() {
        chatJob?.cancel()
        ttsSpeaker.stop()
        _composerInput.value = ""
        viewModelScope.launch { sessionController.clearCurrentAndStartNewSession("New Nexus Thread") }
    }

    override fun onCleared() {
        super.onCleared()
        ttsSpeaker.shutdown()
    }

    companion object {
        fun provideFactory(
            application: Application,
            prefsRepo: PreferencesRepository,
            currentUserId: String,
            guestStore: GuestWorkspaceStore? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val databaseId = application.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val firebaseReady = runCatching { FirebaseApp.getApps(application).isNotEmpty() }.getOrDefault(false)
                ZaytharViewModel(
                    firestoreRepo = FirestoreRepository(db),
                    workspaceStore = FirestoreSessionAndBrainStore(db),
                    prefsRepo = prefsRepo,
                    ttsSpeaker = AndroidTtsSpeaker(application),
                    currentUserId = currentUserId,
                    firebaseInitialized = firebaseReady,
                    guestStore = guestStore
                )
            }
        }
    }
}
