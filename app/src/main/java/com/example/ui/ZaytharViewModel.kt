package com.example.ui

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import com.example.domain.ChatStreamEvent
import com.example.domain.ProviderRoute
import com.example.domain.SupportedLanguage
import com.example.domain.tools.LocalCommandToolAdapter
import com.example.domain.tools.ToolExecutionResult
import com.example.domain.UiState
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
import java.util.Locale

private const val TAG = "ZaytharViewModel"

interface TtsSpeaker {
    fun speak(
        text: String,
        languageCode: String,
        onSpeakingChanged: (Boolean) -> Unit = {}
    )
    fun stop()
    fun shutdown()
}

private class AndroidTtsSpeaker(context: Context) : TtsSpeaker, TextToSpeech.OnInitListener {
    private val tts: TextToSpeech? = runCatching { TextToSpeech(context.applicationContext, this) }.getOrNull()
    private var ready = false
    private var onSpeakingChanged: (Boolean) -> Unit = {}

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeakingChanged(true)
            }

            override fun onDone(utteranceId: String?) {
                onSpeakingChanged(false)
            }

            @Deprecated("Deprecated in Android API")
            override fun onError(utteranceId: String?) {
                onSpeakingChanged(false)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                onSpeakingChanged(false)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                onSpeakingChanged(false)
            }
        })
    }

    override fun speak(
        text: String,
        languageCode: String,
        onSpeakingChanged: (Boolean) -> Unit
    ) {
        val engine = tts
        if (engine == null || !ready || text.isBlank()) {
            onSpeakingChanged(false)
            return
        }
        this.onSpeakingChanged = onSpeakingChanged
        val selectedLocale = Locale.forLanguageTag(languageCode)
        val languageResult = runCatching { engine.setLanguage(selectedLocale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            runCatching { engine.setLanguage(Locale.US) }
        }
        val result = engine.speak(
            text.take(3500),
            TextToSpeech.QUEUE_FLUSH,
            null,
            "zaythar_${System.currentTimeMillis()}"
        )
        if (result == TextToSpeech.ERROR) onSpeakingChanged(false)
    }

    override fun stop() {
        tts?.stop()
        onSpeakingChanged(false)
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ZaytharViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val workspaceStore: FirestoreSessionAndBrainStore,
    private val prefsRepo: PreferencesRepository,
    private val ttsSpeaker: TtsSpeaker,
    private val currentUserId: String,
    firebaseInitialized: Boolean
) : ViewModel() {

    private val aiRepo = AiRepository(firestoreRepo, workspaceStore)
    val toolDescriptors = aiRepo.toolDescriptors()

    suspend fun runPluginSandbox(command: String): String {
        val sessionId = _activeSessionId.value.ifBlank { "plugin-sandbox" }
        return when (
            val result = aiRepo.executeLocalTool(
                message = command,
                authenticatedUid = currentUserId,
                sessionId = sessionId,
                activePlugins = activePluginsFlow.value
            )
        ) {
            is ToolExecutionResult.Completed -> {
                val payload = result.payload
                if (payload.summary == LocalCommandToolAdapter.NO_MATCH) {
                    "No local tool matched (or the required plugin is disabled)."
                } else {
                    "[${payload.summary.uppercase()}]\n${payload.content}"
                }
            }
            is ToolExecutionResult.Rejected -> "Tool runtime blocked this request: ${result.message}"
            is ToolExecutionResult.ApprovalRequired -> "This action needs explicit approval before it can run."
            is ToolExecutionResult.Failed -> "Tool runtime failed safely: ${result.message}"
        }
    }

    val preferences: StateFlow<ZaytharPreferences> = prefsRepo.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ZaytharPreferences())

    val userProfileState: StateFlow<UiState<UserProfileDoc?>> = firestoreRepo.observeUserProfile(currentUserId)
        .map<UserProfileDoc?, UiState<UserProfileDoc?>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapFirestoreErrorOrRethrow(error)
            Log.w(TAG, "Error observing user profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load user profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val chatSessionsState: StateFlow<UiState<List<FirestoreChatSession>>> =
        workspaceStore.observeChatSessions(currentUserId)
            .map<List<FirestoreChatSession>, UiState<List<FirestoreChatSession>>> { sessions ->
                if (sessions.isNotEmpty() && _activeSessionId.value.isBlank()) {
                    _activeSessionId.value = sessions.first().id
                }
                UiState.Success(sessions)
            }
            .catch { error ->
                val rootError = unwrapFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Error observing chat sessions", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load chat threads"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _activeSessionId = MutableStateFlow("")
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    val sessionMessagesState: StateFlow<UiState<List<FirestoreSessionMessage>>> = _activeSessionId
        .flatMapLatest { sessionId ->
            if (sessionId.isBlank()) {
                flowOf<UiState<List<FirestoreSessionMessage>>>(UiState.Loading)
            } else {
                workspaceStore.observeSessionMessages(currentUserId, sessionId)
                    .map<List<FirestoreSessionMessage>, UiState<List<FirestoreSessionMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val rootError = unwrapFirestoreErrorOrRethrow(error)
                        Log.w(TAG, "Error observing session messages", rootError)
                        emit(UiState.Error(rootError.message ?: "Failed to load session history"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activePluginsFlow: StateFlow<List<FirestorePluginConfig>> =
        workspaceStore.observePlugins(currentUserId)
            .catch { error ->
                unwrapFirestoreErrorOrRethrow(error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _activeAgent = MutableStateFlow(AgentRole.GENERAL)
    val activeAgent: StateFlow<AgentRole> = _activeAgent.asStateFlow()

    private val _composerInput = MutableStateFlow("")
    val composerInput: StateFlow<String> = _composerInput.asStateFlow()

    private val _chatUiState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val chatUiState: StateFlow<UiState<String>> = _chatUiState.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _uptimeSeconds = MutableStateFlow(0L)
    val uptimeSeconds: StateFlow<Long> = _uptimeSeconds.asStateFlow()

    private val _clockText = MutableStateFlow(formatCurrentTime())
    val clockText: StateFlow<String> = _clockText.asStateFlow()

    private val _firebaseReady = MutableStateFlow(firebaseInitialized)
    val firebaseReady: StateFlow<Boolean> = _firebaseReady.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 12)
    val toastEvents = _toastEvents.asSharedFlow()

    private var chatJob: Job? = null

    init {
        viewModelScope.launch {
            val defaultName = runCatching { prefsRepo.preferences.first().operatorName }
                .getOrDefault("OPERATOR 01")
            val profileRes = firestoreRepo.ensureUserProfile(defaultName)
            profileRes.getOrNull()?.let { doc ->
                if (doc.operatorName.isNotBlank() && doc.operatorName != defaultName) {
                    prefsRepo.update { it.copy(operatorName = doc.operatorName) }
                }
            }
            workspaceStore.ensureDefaultSession().getOrNull()?.let { sid ->
                if (_activeSessionId.value.isBlank()) {
                    _activeSessionId.value = sid
                }
            }
            workspaceStore.seedBuiltInPluginsIfMissing()
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

    fun toggleTheme() {
        viewModelScope.launch {
            prefsRepo.update { it.copy(isDarkTheme = !it.isDarkTheme) }
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
            emitToast(if (enabled) "Internet Web Grounding CONNECTED." else "Offline Gemini mode active.")
        }
    }

    fun saveOperatorName(rawName: String) {
        val cleaned = rawName.trim().take(32).ifBlank { "OPERATOR 01" }
        viewModelScope.launch {
            prefsRepo.update { it.copy(operatorName = cleaned) }
            val result = firestoreRepo.updateOperatorName(cleaned)
            emitToast(
                if (result.isSuccess) "Operator profile synced to Cloud Firestore."
                else "Could not sync profile to cloud: ${result.exceptionOrNull()?.localizedMessage ?: "error"}"
            )
        }
    }

    // --- MULTI-SESSION CHAT HISTORY ---

    fun selectChatSession(sessionId: String) {
        if (sessionId.isNotBlank() && sessionId != _activeSessionId.value) {
            chatJob?.cancel()
            _chatUiState.value = UiState.Idle
            _activeSessionId.value = sessionId
        }
    }

    fun createNewChatSession(title: String = "New Nexus Thread") {
        viewModelScope.launch {
            val res = workspaceStore.createChatSession(title = title, agent = _activeAgent.value.id)
            res.onSuccess { newId ->
                _chatUiState.value = UiState.Idle
                _activeSessionId.value = newId
                emitToast("New conversation thread created.")
            }.onFailure { err ->
                emitToast("Could not create thread: ${err.localizedMessage ?: "error"}")
            }
        }
    }

    fun renameChatSession(sessionId: String, newTitle: String) {
        val cleaned = newTitle.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            val res = workspaceStore.renameChatSession(sessionId, cleaned)
            if (res.isSuccess) emitToast("Thread renamed.")
            else emitToast("Rename failed: ${res.exceptionOrNull()?.localizedMessage ?: "error"}")
        }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            val res = workspaceStore.deleteChatSession(sessionId)
            if (res.isSuccess) {
                if (_activeSessionId.value == sessionId) {
                    val fallback = workspaceStore.ensureDefaultSession().getOrNull().orEmpty()
                    _activeSessionId.value = fallback
                }
                emitToast("Thread deleted from Cloud Firestore.")
            } else {
                emitToast("Delete failed: ${res.exceptionOrNull()?.localizedMessage ?: "error"}")
            }
        }
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
                workspaceStore.ensureDefaultSession().getOrNull().orEmpty().also {
                    _activeSessionId.value = it
                }
            }
            if (sessionId.isBlank()) {
                val msg = "Unable to initialize cloud chat session."
                _chatUiState.value = UiState.Error(msg)
                emitToast(msg)
                return@launch
            }

            val historySnapshot = workspaceStore.getRecentSessionMessages(sessionId)
            val sessions = (chatSessionsState.value as? UiState.Success)?.data.orEmpty()
            val activeSession = sessions.find { it.id == sessionId }
            val autoTitle = if (activeSession != null && activeSession.messageCount == 0L &&
                (activeSession.title == "Primary Nexus" || activeSession.title == "New Nexus Thread")
            ) {
                text
            } else {
                null
            }

            val userMsgRes = workspaceStore.addSessionMessage(
                sessionId = sessionId,
                role = "user",
                text = text,
                agent = agent.id,
                autoTitleFromFirstPrompt = autoTitle
            )
            if (userMsgRes.isFailure) {
                val msg = userMsgRes.exceptionOrNull()?.localizedMessage ?: "Failed to save message to Firestore."
                _chatUiState.value = UiState.Error(msg)
                emitToast(msg)
                return@launch
            }

            _chatUiState.value = UiState.Loading

            try {
                var finalReply = ""
                var modelUsed = ""
                var webSources: List<String> = emptyList()
                aiRepo.streamChatReply(
                    message = text,
                    agent = agent,
                    prefs = currentPrefs,
                    recentHistory = historySnapshot,
                    activePlugins = activePluginsFlow.value,
                    webConnectEnabled = currentPrefs.webConnectEnabled,
                    authenticatedUid = currentUserId,
                    sessionId = sessionId
                ).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Chunk -> {
                            _chatUiState.value = UiState.Streaming(event.accumulatedText)
                        }
                        is ChatStreamEvent.Completed -> {
                            finalReply = event.finalText
                            modelUsed = event.modelLabel
                            webSources = event.webSources
                        }
                    }
                }
                if (finalReply.isNotBlank()) {
                    val saveReply = workspaceStore.addSessionMessage(
                        sessionId = sessionId,
                        role = "assistant",
                        text = finalReply,
                        agent = agent.id,
                        modelLabel = modelUsed,
                        webSources = webSources
                    )
                    if (saveReply.isFailure) {
                        emitToast("Reply generated, but failed to sync to Firestore.")
                    }
                    _chatUiState.value = UiState.Success(finalReply)
                    if (currentPrefs.speakReplies) {
                        ttsSpeaker.speak(finalReply, currentPrefs.languageCode) { speaking ->
                            _isSpeaking.value = speaking
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat timed out", e)
                val msg = "Generation timed out after 60 seconds. Please check network connectivity and try again."
                _chatUiState.value = UiState.Error(msg)
                emitToast(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat generation failed", e)
                val msg = e.localizedMessage ?: "AI service failed to respond. Check Firebase AI Logic & App Check status."
                _chatUiState.value = UiState.Error(msg)
                emitToast(msg)
            }
        }
    }

    fun clearChat() {
        chatJob?.cancel()
        ttsSpeaker.stop()
        _isSpeaking.value = false
        val sessionId = _activeSessionId.value
        if (sessionId.isBlank()) return
        viewModelScope.launch {
            val res = workspaceStore.clearSessionMessages(sessionId)
            _chatUiState.value = UiState.Idle
            if (res.isSuccess) {
                emitToast("Thread messages cleared.")
            } else {
                emitToast("Failed to clear conversation: ${res.exceptionOrNull()?.localizedMessage ?: "error"}")
            }
        }
    }

    override fun onCleared() {
        ttsSpeaker.stop()
        _isSpeaking.value = false
        ttsSpeaker.shutdown()
        super.onCleared()
    }

    companion object {
        fun provideFactory(
            application: Application,
            prefsRepo: PreferencesRepository,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val databaseId = application.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val firestoreRepo = FirestoreRepository(db)
                val workspaceStore = FirestoreSessionAndBrainStore(db)
                val firebaseReady = runCatching {
                    FirebaseApp.getApps(application).isNotEmpty()
                }.getOrDefault(false)
                ZaytharViewModel(
                    firestoreRepo = firestoreRepo,
                    workspaceStore = workspaceStore,
                    prefsRepo = prefsRepo,
                    ttsSpeaker = AndroidTtsSpeaker(application),
                    currentUserId = currentUserId,
                    firebaseInitialized = firebaseReady
                )
            }
        }
    }
}
