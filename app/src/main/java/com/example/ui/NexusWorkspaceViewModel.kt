package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreBrainRun
import com.example.data.remote.FirestoreKnowledgeFact
import com.example.data.remote.FirestoreMemoryNote
import com.example.data.remote.FirestorePluginConfig
import com.example.data.remote.FirestoreRepository
import com.example.data.remote.FirestoreSessionAndBrainStore
import com.example.domain.AgentRole
import com.example.domain.AiRepository
import com.example.domain.CameraVisionResult
import com.example.domain.ChatStreamEvent
import com.example.domain.CodeCopilotMode
import com.example.domain.CodeLanguage
import com.example.domain.EngineHealthReport
import com.example.domain.GeneratedImageResult
import com.example.domain.GuestWorkspaceStore
import com.example.domain.MultiAgentRunResult
import com.example.domain.NeuralBrainAnalysis
import com.example.domain.NeuralBrainMode
import com.example.domain.SmartEngineRouter
import com.example.domain.StudioAspectRatio
import com.example.domain.SuggestedTriple
import com.example.domain.UiState
import com.example.domain.VisionMode
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "NexusWorkspaceVM"

class NexusWorkspaceViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val workspaceStore: FirestoreSessionAndBrainStore,
    private val currentUserId: String,
    private val guestStore: GuestWorkspaceStore? = null
) : ViewModel() {

    private val aiRepo = AiRepository(firestoreRepo, workspaceStore, guestStore)
    private val crudDelegate = NexusWorkspaceCrudDelegate(viewModelScope, firestoreRepo, workspaceStore, guestStore)
    internal val voiceAudio = VoiceAudioController(viewModelScope, aiRepo)

    val memoryNotesState: StateFlow<UiState<List<FirestoreMemoryNote>>> = (
        guestStore?.memoryNotes ?: firestoreRepo.observeMemoryNotes(currentUserId)
    ).map<List<FirestoreMemoryNote>, UiState<List<FirestoreMemoryNote>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrRethrow(error)
            emit(UiState.Error(root.message ?: "Failed to load memory notes"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val knowledgeFactsState: StateFlow<UiState<List<FirestoreKnowledgeFact>>> = (
        guestStore?.knowledgeFacts ?: firestoreRepo.observeKnowledgeFacts(currentUserId)
    ).map<List<FirestoreKnowledgeFact>, UiState<List<FirestoreKnowledgeFact>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrRethrow(error)
            emit(UiState.Error(root.message ?: "Failed to load knowledge graph"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val pluginsState: StateFlow<UiState<List<FirestorePluginConfig>>> = (
        guestStore?.plugins ?: workspaceStore.observePlugins(currentUserId)
    ).map<List<FirestorePluginConfig>, UiState<List<FirestorePluginConfig>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrRethrow(error)
            emit(UiState.Error(root.message ?: "Failed to load plugins"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val brainRunsState: StateFlow<UiState<List<FirestoreBrainRun>>> = (
        guestStore?.brainRuns ?: workspaceStore.observeBrainRuns(currentUserId)
    ).map<List<FirestoreBrainRun>, UiState<List<FirestoreBrainRun>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrRethrow(error)
            emit(UiState.Error(root.message ?: "Failed to load AI Brain runs"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _agentRunState = MutableStateFlow<UiState<MultiAgentRunResult>>(UiState.Idle)
    val agentRunState: StateFlow<UiState<MultiAgentRunResult>> = _agentRunState.asStateFlow()

    private val _brainAnalysisState = MutableStateFlow<UiState<NeuralBrainAnalysis>>(UiState.Idle)
    val brainAnalysisState: StateFlow<UiState<NeuralBrainAnalysis>> = _brainAnalysisState.asStateFlow()

    private val _visionState = MutableStateFlow<UiState<CameraVisionResult>>(UiState.Idle)
    val visionState: StateFlow<UiState<CameraVisionResult>> = _visionState.asStateFlow()

    private val _codeCopilotState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val codeCopilotState: StateFlow<UiState<String>> = _codeCopilotState.asStateFlow()

    private val _studioBriefState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val studioBriefState: StateFlow<UiState<String>> = _studioBriefState.asStateFlow()

    private val _studioImageState = MutableStateFlow<UiState<GeneratedImageResult>>(UiState.Idle)
    val studioImageState: StateFlow<UiState<GeneratedImageResult>> = _studioImageState.asStateFlow()

    private val _engineDiagnosticsState = MutableStateFlow<UiState<EngineHealthReport>>(UiState.Idle)
    val engineDiagnosticsState: StateFlow<UiState<EngineHealthReport>> = _engineDiagnosticsState.asStateFlow()

    private var codeJob: Job? = null

    private fun unwrapFirestoreErrorOrRethrow(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    // --- MEMORY, KNOWLEDGE GRAPH & PLUGINS ---

    fun addMemoryNote(rawContent: String, onToast: (String) -> Unit) =
        crudDelegate.addMemoryNote(rawContent, onToast)

    fun deleteMemoryNote(id: String, onToast: (String) -> Unit) =
        crudDelegate.deleteMemoryNote(id, onToast)

    fun addKnowledgeFact(
        rawSubject: String,
        rawPredicate: String,
        rawObject: String,
        source: String = "operator",
        onToast: (String) -> Unit
    ) = crudDelegate.addKnowledgeFact(rawSubject, rawPredicate, rawObject, source, onToast)

    fun deleteKnowledgeFact(id: String, onToast: (String) -> Unit) =
        crudDelegate.deleteKnowledgeFact(id, onToast)

    fun commitSuggestedTriples(triples: List<SuggestedTriple>, onToast: (String) -> Unit) =
        crudDelegate.commitSuggestedTriples(triples, onToast)

    fun togglePlugin(pluginId: String, enabled: Boolean, onToast: (String) -> Unit) =
        crudDelegate.togglePlugin(pluginId, enabled, onToast)

    fun addCustomPlugin(name: String, description: String, category: String, systemInstruction: String, onToast: (String) -> Unit) =
        crudDelegate.addCustomPlugin(name, description, category, systemInstruction, onToast)

    fun deletePlugin(pluginId: String, onToast: (String) -> Unit) =
        crudDelegate.deletePlugin(pluginId, onToast)

    // --- AI BRAIN & NEURAL FUNCTIONS ---

    fun runNeuralBrain(
        prompt: String,
        mode: NeuralBrainMode,
        recentSessionMessagesText: String,
        prefs: ZaytharPreferences,
        onToast: (String) -> Unit
    ) {
        val cleaned = prompt.trim()
        if (cleaned.isEmpty()) {
            onToast("Enter a problem, note, or cognitive query for AI Brain.")
            return
        }
        if (_brainAnalysisState.value is UiState.Loading) return

        viewModelScope.launch {
            _brainAnalysisState.value = UiState.Loading
            try {
                val result = aiRepo.runNeuralBrainAnalysis(cleaned, mode, recentSessionMessagesText, prefs)
                _brainAnalysisState.value = UiState.Success(result)
                onToast("Neural Brain synthesis complete (${result.confidence}% confidence).")
            } catch (e: TimeoutCancellationException) {
                val msg = "AI Brain analysis timed out after 60 seconds."
                _brainAnalysisState.value = UiState.Error(msg)
                onToast(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Brain analysis failed", e)
                val msg = e.localizedMessage ?: "AI Brain synthesis failed."
                _brainAnalysisState.value = UiState.Error(msg)
                onToast(msg)
            }
        }
    }

    fun deleteBrainRun(runId: String, onToast: (String) -> Unit) {
        if (guestStore != null) {
            guestStore.deleteBrainRun(runId)
            return
        }
        viewModelScope.launch {
            if (workspaceStore.deleteBrainRun(runId).isFailure) onToast("Failed to delete brain snapshot.")
        }
    }

    // --- CAMERA EYES MULTIMODAL VISION ---

    fun analyzeWithCameraEyes(
        bitmap: Bitmap?,
        prompt: String,
        mode: VisionMode,
        prefs: ZaytharPreferences,
        activeSessionId: String,
        onToast: (String) -> Unit
    ) {
        if (bitmap == null) {
            onToast("Capture a photo or pick an image first.")
            return
        }
        if (_visionState.value is UiState.Loading) return

        viewModelScope.launch {
            _visionState.value = UiState.Loading
            try {
                val res = aiRepo.analyzeImageWithVision(bitmap, prompt, mode, prefs)
                _visionState.value = UiState.Success(res)
                if (activeSessionId.isNotBlank()) {
                    val msgText = "[CAMERA EYES · ${mode.title}]\nPrompt: ${res.prompt}\n\n${res.analysis}"
                    if (guestStore != null) {
                        guestStore.addSessionMessage(activeSessionId, "assistant", msgText, "research", "${res.modelUsed}+vision", hasImage = true)
                    } else {
                        workspaceStore.addSessionMessage(activeSessionId, "assistant", msgText, "research", "${res.modelUsed}+vision", hasImage = true)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                val msg = "Camera Eyes vision analysis timed out."
                _visionState.value = UiState.Error(msg)
                onToast(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Camera Eyes failed", e)
                val msg = e.localizedMessage ?: "Vision inference failed."
                _visionState.value = UiState.Error(msg)
                onToast(msg)
            }
        }
    }

    // --- CODE COPILOT & STUDIO ---

    fun runCodeCopilot(
        task: String,
        language: CodeLanguage,
        mode: CodeCopilotMode,
        existingCode: String,
        prefs: ZaytharPreferences,
        onToast: (String) -> Unit,
        includeUnitTests: Boolean = true,
        strictSecurityAudit: Boolean = true
    ) {
        if (task.isBlank() && existingCode.isBlank()) {
            onToast("Enter a task or paste source code first.")
            return
        }
        codeJob?.cancel()
        codeJob = viewModelScope.launch {
            _codeCopilotState.value = UiState.Loading
            try {
                var finalOutput = ""
                aiRepo.streamCodeCopilot(
                    task = task,
                    language = language,
                    mode = mode,
                    existingCode = existingCode,
                    prefs = prefs,
                    includeUnitTests = includeUnitTests,
                    strictSecurityAudit = strictSecurityAudit
                ).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Chunk -> _codeCopilotState.value = UiState.Streaming(event.accumulatedText)
                        is ChatStreamEvent.Completed -> finalOutput = event.finalText
                    }
                }
                if (finalOutput.isNotBlank()) {
                    _codeCopilotState.value = UiState.Success(finalOutput)
                }
            } catch (e: TimeoutCancellationException) {
                val msg = "Code Copilot timed out after 60 seconds."
                _codeCopilotState.value = UiState.Error(msg)
                onToast(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Code Copilot failed", e)
                val msg = e.localizedMessage ?: "Code Copilot generation failed."
                _codeCopilotState.value = UiState.Error(msg)
                onToast(msg)
            }
        }
    }

    fun generateCreativeBrief(
        prompt: String,
        outputType: String,
        style: String,
        prefs: ZaytharPreferences,
        onToast: (String) -> Unit
    ) {
        val cleaned = prompt.trim()
        if (cleaned.isEmpty()) {
            onToast("Enter a creative prompt first.")
            return
        }
        if (_studioBriefState.value is UiState.Loading) return
        viewModelScope.launch {
            _studioBriefState.value = UiState.Loading
            try {
                val result = aiRepo.generateStudioConcept(cleaned, outputType, style, prefs)
                _studioBriefState.value = UiState.Success(result)
            } catch (e: TimeoutCancellationException) {
                val msg = "Creative synthesis timed out."
                _studioBriefState.value = UiState.Error(msg)
                onToast(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Studio generation failed."
                _studioBriefState.value = UiState.Error(msg)
                onToast(msg)
            }
        }
    }

    fun generateStudioImage(
        prompt: String,
        style: String,
        aspectRatio: StudioAspectRatio,
        referenceBitmap: Bitmap?,
        onToast: (String) -> Unit
    ) {
        val cleaned = prompt.trim()
        if (cleaned.isEmpty()) { onToast("Enter an image prompt first."); return }
        if (_studioImageState.value is UiState.Loading) return
        viewModelScope.launch {
            _studioImageState.value = UiState.Loading
            try {
                val res = aiRepo.generateStudioImage(cleaned, style, aspectRatio, referenceBitmap)
                _studioImageState.value = UiState.Success(res)
                onToast("Image synthesized via ${res.modelUsed}.")
            } catch (e: TimeoutCancellationException) {
                val msg = "Image generation timed out after 60 seconds."
                _studioImageState.value = UiState.Error(msg)
                onToast(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image generation failed", e)
                val msg = e.localizedMessage ?: "Image generation failed."
                _studioImageState.value = UiState.Error(msg)
                onToast(msg)
            }
        }
    }

    fun runAllEnginesDiagnostic(
        includeLiveGeminiProbe: Boolean,
        firebaseReady: Boolean,
        activePluginsCount: Int,
        onToast: (String) -> Unit
    ) {
        if (_engineDiagnosticsState.value is UiState.Loading) return
        viewModelScope.launch {
            _engineDiagnosticsState.value = UiState.Loading
            try {
                val memoriesCount = (memoryNotesState.value as? UiState.Success)?.data?.size ?: 0
                val factsCount = (knowledgeFactsState.value as? UiState.Success)?.data?.size ?: 0
                val liveProbe = if (includeLiveGeminiProbe) {
                    runCatching { aiRepo.probeGeminiEngine() }
                        .onFailure { if (it is CancellationException) throw it }
                } else null
                val report = SmartEngineRouter.runLocalAndHybridDiagnostics(
                    memoriesCount, factsCount, activePluginsCount, firebaseReady, guestStore != null, liveProbe
                )
                _engineDiagnosticsState.value = UiState.Success(report)
                onToast("Zaythar Engine Suite: ${report.passedCount}/${report.totalCount} engines online.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Engine diagnostic run failed."
                _engineDiagnosticsState.value = UiState.Error(msg)
                onToast(msg)
            }
        }
    }

    // --- MULTI-AGENT COLLABORATION ---

    fun runMultiAgent(
        task: String,
        selectedAgents: List<AgentRole>,
        prefs: ZaytharPreferences,
        activeSessionId: String,
        onToast: (String) -> Unit
    ) {
        val trimmed = task.trim()
        if (trimmed.isEmpty()) {
            onToast("Describe a task for your specialist team first.")
            return
        }
        if (selectedAgents.isEmpty() || selectedAgents.size > 4) {
            onToast("Select between 1 and 4 specialists.")
            return
        }
        if (_agentRunState.value is UiState.Loading) return

        viewModelScope.launch {
            _agentRunState.value = UiState.Loading
            try {
                val result = aiRepo.runMultiAgentTeam(trimmed, selectedAgents, prefs)
                _agentRunState.value = UiState.Success(result)
                if (activeSessionId.isNotBlank()) {
                    val text = "[MULTI-AGENT RESULT]\n${result.reply}"
                    if (guestStore != null) {
                        guestStore.addSessionMessage(activeSessionId, "assistant", text, "coordinator", result.modelUsed)
                    } else {
                        workspaceStore.addSessionMessage(activeSessionId, "assistant", text, "coordinator", result.modelUsed)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                val msg = "Agent orchestration timed out after 60 seconds."
                _agentRunState.value = UiState.Error(msg)
                onToast(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Multi-agent orchestration failed", e)
                val msg = e.localizedMessage ?: "Agent team could not complete the task."
                _agentRunState.value = UiState.Error(msg)
                onToast(msg)
            }
        }
    }

    override fun onCleared() {
        voiceAudio.shutdown()
        super.onCleared()
    }

    companion object {
        fun provideFactory(
            application: Application,
            currentUserId: String,
            guestStore: GuestWorkspaceStore? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val databaseId = application.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                NexusWorkspaceViewModel(
                    firestoreRepo = FirestoreRepository(db),
                    workspaceStore = FirestoreSessionAndBrainStore(db),
                    currentUserId = currentUserId,
                    guestStore = guestStore
                )
            }
        }
    }
}
