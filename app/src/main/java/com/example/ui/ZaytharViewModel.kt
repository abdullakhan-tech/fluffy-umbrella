package com.example.ui

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.local.PreferencesRepository
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreChatMessage
import com.example.data.remote.FirestoreKnowledgeFact
import com.example.data.remote.FirestoreMemoryNote
import com.example.data.remote.FirestoreRepository
import com.example.data.remote.UserProfileDoc
import com.example.domain.AgentRole
import com.example.domain.AiRepository
import com.example.domain.ChatStreamEvent
import com.example.domain.MultiAgentRunResult
import com.example.domain.ProviderRoute
import com.example.domain.SupportedLanguage
import com.example.domain.UiState
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TAG = "ZaytharViewModel"

interface TtsSpeaker {
    fun speak(text: String, languageCode: String)
    fun stop()
    fun shutdown()
}

private class AndroidTtsSpeaker(context: Context) : TtsSpeaker, TextToSpeech.OnInitListener {
    private val tts: TextToSpeech? = runCatching { TextToSpeech(context.applicationContext, this) }.getOrNull()
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
    }

    override fun speak(text: String, languageCode: String) {
        val engine = tts ?: return
        if (!ready) return
        engine.language = Locale.forLanguageTag(languageCode)
        engine.speak(text.take(3500), TextToSpeech.QUEUE_FLUSH, null, "zaythar_${System.currentTimeMillis()}")
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

class ZaytharViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val prefsRepo: PreferencesRepository,
    private val ttsSpeaker: TtsSpeaker,
    private val currentUserId: String,
    firebaseInitialized: Boolean
) : ViewModel() {

    private val aiRepo = AiRepository(firestoreRepo)

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

    val chatMessagesState: StateFlow<UiState<List<FirestoreChatMessage>>> = firestoreRepo.observeChatMessages(currentUserId)
        .map<List<FirestoreChatMessage>, UiState<List<FirestoreChatMessage>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapFirestoreErrorOrRethrow(error)
            Log.w(TAG, "Error observing chat messages", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load conversation history"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val memoryNotesState: StateFlow<UiState<List<FirestoreMemoryNote>>> = firestoreRepo.observeMemoryNotes(currentUserId)
        .map<List<FirestoreMemoryNote>, UiState<List<FirestoreMemoryNote>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapFirestoreErrorOrRethrow(error)
            Log.w(TAG, "Error observing memory notes", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load memory notes"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val knowledgeFactsState: StateFlow<UiState<List<FirestoreKnowledgeFact>>> = firestoreRepo.observeKnowledgeFacts(currentUserId)
        .map<List<FirestoreKnowledgeFact>, UiState<List<FirestoreKnowledgeFact>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapFirestoreErrorOrRethrow(error)
            Log.w(TAG, "Error observing knowledge facts", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load knowledge graph"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _activeAgent = MutableStateFlow(AgentRole.GENERAL)
    val activeAgent: StateFlow<AgentRole> = _activeAgent.asStateFlow()

    private val _composerInput = MutableStateFlow("")
    val composerInput: StateFlow<String> = _composerInput.asStateFlow()

    private val _chatUiState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val chatUiState: StateFlow<UiState<String>> = _chatUiState.asStateFlow()

    private val _agentRunState = MutableStateFlow<UiState<MultiAgentRunResult>>(UiState.Idle)
    val agentRunState: StateFlow<UiState<MultiAgentRunResult>> = _agentRunState.asStateFlow()

    private val _studioBrief = MutableStateFlow("")
    val studioBrief: StateFlow<String> = _studioBrief.asStateFlow()

    private val _uptimeSeconds = MutableStateFlow(0L)
    val uptimeSeconds: StateFlow<Long> = _uptimeSeconds.asStateFlow()

    private val _clockText = MutableStateFlow(formatCurrentTime())
    val clockText: StateFlow<String> = _clockText.asStateFlow()

    private val _firebaseReady = MutableStateFlow(firebaseInitialized)
    val firebaseReady: StateFlow<Boolean> = _firebaseReady.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
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
            if (!enabled) {
                ttsSpeaker.stop()
            }
        }
    }

    fun saveOperatorName(rawName: String) {
        val cleaned = rawName.trim().take(32).ifBlank { "OPERATOR 01" }
        viewModelScope.launch {
            prefsRepo.update { it.copy(operatorName = cleaned) }
            val result = firestoreRepo.updateOperatorName(cleaned)
            if (result.isSuccess) {
                emitToast("Operator profile synced to Cloud Firestore.")
            } else {
                emitToast("Could not sync profile to cloud: ${result.exceptionOrNull()?.localizedMessage ?: "error"}")
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
            val historySnapshot = firestoreRepo.getRecentChatMessages()

            val userMsgRes = firestoreRepo.addChatMessage(
                role = "user",
                text = text,
                agent = agent.id
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
                aiRepo.streamChatReply(text, agent, currentPrefs, historySnapshot).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Chunk -> {
                            _chatUiState.value = UiState.Streaming(event.accumulatedText)
                        }
                        is ChatStreamEvent.Completed -> {
                            finalReply = event.finalText
                            modelUsed = event.modelLabel
                        }
                    }
                }
                if (finalReply.isNotBlank()) {
                    val saveReply = firestoreRepo.addChatMessage(
                        role = "assistant",
                        text = finalReply,
                        agent = agent.id,
                        modelLabel = modelUsed
                    )
                    if (saveReply.isFailure) {
                        emitToast("Reply generated, but failed to sync to Firestore.")
                    }
                    _chatUiState.value = UiState.Success(finalReply)
                    if (currentPrefs.speakReplies) {
                        ttsSpeaker.speak(finalReply, currentPrefs.languageCode)
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
        viewModelScope.launch {
            val res = firestoreRepo.clearChatMessages()
            _chatUiState.value = UiState.Idle
            if (res.isSuccess) {
                emitToast("Cloud conversation cleared.")
            } else {
                emitToast("Failed to clear conversation: ${res.exceptionOrNull()?.localizedMessage ?: "error"}")
            }
        }
    }

    fun addMemoryNote(rawContent: String) {
        val cleaned = rawContent.trim()
        if (cleaned.isEmpty()) return
        if (cleaned.length > 2000) {
            emitToast("Note must be 2000 characters or fewer.")
            return
        }
        viewModelScope.launch {
            val res = firestoreRepo.addMemoryNote(cleaned)
            if (res.isSuccess) {
                emitToast("Note synced to Zaythar cloud memory.")
            } else {
                emitToast("Could not save note: ${res.exceptionOrNull()?.localizedMessage ?: "error"}")
            }
        }
    }

    fun deleteMemoryNote(id: String) {
        viewModelScope.launch {
            val res = firestoreRepo.deleteMemoryNote(id)
            if (res.isFailure) {
                emitToast("Failed to delete note.")
            }
        }
    }

    fun addKnowledgeFact(rawSubject: String, rawPredicate: String, rawObject: String) {
        val subject = rawSubject.trim()
        val predicate = rawPredicate.trim()
        val obj = rawObject.trim()
        if (subject.isEmpty() || predicate.isEmpty() || obj.isEmpty()) {
            emitToast("Fill in subject, relation, and object.")
            return
        }
        if (subject.length > 300 || predicate.length > 300 || obj.length > 300) {
            emitToast("Each field must be 300 characters or fewer.")
            return
        }
        viewModelScope.launch {
            if (firestoreRepo.countKnowledgeFacts() >= 200) {
                emitToast("Knowledge graph limit reached (200 facts).")
                return@launch
            }
            val res = firestoreRepo.addKnowledgeFact(
                subject = subject,
                predicate = predicate,
                objectValue = obj,
                source = "operator"
            )
            if (res.isSuccess) {
                emitToast("Knowledge fact synced to Cloud Firestore.")
            } else {
                emitToast("Failed to save fact: ${res.exceptionOrNull()?.localizedMessage ?: "error"}")
            }
        }
    }

    fun deleteKnowledgeFact(id: String) {
        viewModelScope.launch {
            val res = firestoreRepo.deleteKnowledgeFact(id)
            if (res.isFailure) {
                emitToast("Failed to delete knowledge fact.")
            }
        }
    }

    fun runMultiAgent(task: String, selectedAgents: List<AgentRole>) {
        val trimmed = task.trim()
        if (trimmed.isEmpty()) {
            emitToast("Describe a task for your specialist team first.")
            return
        }
        if (selectedAgents.isEmpty() || selectedAgents.size > 4) {
            emitToast("Select between 1 and 4 specialists.")
            return
        }
        if (_agentRunState.value is UiState.Loading) return

        viewModelScope.launch {
            _agentRunState.value = UiState.Loading
            try {
                val result = aiRepo.runMultiAgentTeam(trimmed, selectedAgents, preferences.value)
                _agentRunState.value = UiState.Success(result)
                firestoreRepo.addChatMessage(
                    role = "assistant",
                    text = "[MULTI-AGENT RESULT]\n${result.reply}",
                    agent = "coordinator",
                    modelLabel = result.modelUsed
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Multi-agent run timed out", e)
                val msg = "Agent orchestration timed out after 60 seconds."
                _agentRunState.value = UiState.Error(msg)
                emitToast(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Multi-agent orchestration failed", e)
                val msg = e.localizedMessage ?: "Agent team could not complete the task."
                _agentRunState.value = UiState.Error(msg)
                emitToast(msg)
            }
        }
    }

    fun generateCreativeBrief(prompt: String, outputType: String, style: String) {
        val cleaned = prompt.trim()
        _studioBrief.value = if (cleaned.isEmpty()) {
            "Enter a creative prompt first."
        } else {
            "Creative brief prepared ($outputType): $cleaned — Style: $style. Connect a media generation provider to render final assets."
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsSpeaker.shutdown()
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
                val firebaseReady = runCatching {
                    FirebaseApp.getApps(application).isNotEmpty()
                }.getOrDefault(false)
                ZaytharViewModel(
                    firestoreRepo = firestoreRepo,
                    prefsRepo = prefsRepo,
                    ttsSpeaker = AndroidTtsSpeaker(application),
                    currentUserId = currentUserId,
                    firebaseInitialized = firebaseReady
                )
            }
        }
    }
}
