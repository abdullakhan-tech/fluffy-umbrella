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
import com.example.domain.MultiAgentRunResult
import com.example.domain.NeuralBrainAnalysis
import com.example.domain.NeuralBrainMode
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
    private val currentUserId: String
) : ViewModel() {

    private val aiRepo = AiRepository(firestoreRepo, workspaceStore)

    val memoryNotesState: StateFlow<UiState<List<FirestoreMemoryNote>>> = firestoreRepo.observeMemoryNotes(currentUserId)
        .map<List<FirestoreMemoryNote>, UiState<List<FirestoreMemoryNote>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrRethrow(error)
            emit(UiState.Error(root.message ?: "Failed to load memory notes"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val knowledgeFactsState: StateFlow<UiState<List<FirestoreKnowledgeFact>>> = firestoreRepo.observeKnowledgeFacts(currentUserId)
        .map<List<FirestoreKnowledgeFact>, UiState<List<FirestoreKnowledgeFact>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrRethrow(error)
            emit(UiState.Error(root.message ?: "Failed to load knowledge graph"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val pluginsState: StateFlow<UiState<List<FirestorePluginConfig>>> = workspaceStore.observePlugins(currentUserId)
        .map<List<FirestorePluginConfig>, UiState<List<FirestorePluginConfig>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrRethrow(error)
            emit(UiState.Error(root.message ?: "Failed to load plugins"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val brainRunsState: StateFlow<UiState<List<FirestoreBrainRun>>> = workspaceStore.observeBrainRuns(currentUserId)
        .map<List<FirestoreBrainRun>, UiState<List<FirestoreBrainRun>>> { UiState.Success(it) }
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

    private var codeJob: Job? = null

    private fun unwrapFirestoreErrorOrRethrow(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    // --- MEMORY & KNOWLEDGE GRAPH ---

    fun addMemoryNote(rawContent: String, onToast: (String) -> Unit) {
        val cleaned = rawContent.trim()
        if (cleaned.isEmpty()) return
        if (cleaned.length > 2000) {
            onToast("Note must be 2000 characters or fewer.")
            return
        }
        viewModelScope.launch {
            val res = firestoreRepo.addMemoryNote(cleaned)
            onToast(
                if (res.isSuccess) "Note synced to Zaythar cloud memory."
                else "Could not save note: ${res.exceptionOrNull()?.localizedMessage ?: "error"}"
            )
        }
    }

    fun deleteMemoryNote(id: String, onToast: (String) -> Unit) {
        viewModelScope.launch {
            if (firestoreRepo.deleteMemoryNote(id).isFailure) onToast("Failed to delete note.")
        }
    }

    fun addKnowledgeFact(
        rawSubject: String,
        rawPredicate: String,
        rawObject: String,
        source: String = "operator",
        onToast: (String) -> Unit
    ) {
        val subject = rawSubject.trim()
        val predicate = rawPredicate.trim()
        val obj = rawObject.trim()
        if (subject.isEmpty() || predicate.isEmpty() || obj.isEmpty()) {
            onToast("Fill in subject, relation, and object.")
            return
        }
        if (subject.length > 300 || predicate.length > 300 || obj.length > 300) {
            onToast("Each field must be 300 characters or fewer.")
            return
        }
        viewModelScope.launch {
            if (firestoreRepo.countKnowledgeFacts() >= 200) {
                onToast("Knowledge graph limit reached (200 facts).")
                return@launch
            }
            val res = firestoreRepo.addKnowledgeFact(subject, predicate, obj, source)
            onToast(
                if (res.isSuccess) "Knowledge fact synced to Cloud Firestore."
                else "Failed to save fact: ${res.exceptionOrNull()?.localizedMessage ?: "error"}"
            )
        }
    }

    fun deleteKnowledgeFact(id: String, onToast: (String) -> Unit) {
        viewModelScope.launch {
            if (firestoreRepo.deleteKnowledgeFact(id).isFailure) onToast("Failed to delete knowledge fact.")
        }
    }

    fun commitSuggestedTriples(triples: List<SuggestedTriple>, onToast: (String) -> Unit) {
        if (triples.isEmpty()) return
        viewModelScope.launch {
            val currentCount = firestoreRepo.countKnowledgeFacts()
            if (currentCount >= 200) {
                onToast("Knowledge graph limit reached (200 facts).")
                return@launch
            }
            var saved = 0
            for (t in triples.take(200 - currentCount)) {
                val res = firestoreRepo.addKnowledgeFact(t.subject, t.predicate, t.objectValue, source = "neural_brain")
                if (res.isSuccess) saved++
            }
            onToast("Committed $saved knowledge triple(s) to Cloud Firestore.")
        }
    }

    // --- PLUGINS ---

    fun togglePlugin(pluginId: String, enabled: Boolean, onToast: (String) -> Unit) {
        viewModelScope.launch {
            val res = workspaceStore.setPluginEnabled(pluginId, enabled)
            if (res.isFailure) onToast("Failed to update plugin state.")
        }
    }

    fun addCustomPlugin(
        name: String,
        description: String,
        category: String,
        systemInstruction: String,
        onToast: (String) -> Unit
    ) {
        if (name.isBlank() || description.isBlank()) {
            onToast("Plugin name and description are required.")
            return
        }
        viewModelScope.launch {
            val res = workspaceStore.addCustomPlugin(name, description, category, systemInstruction)
            onToast(
                if (res.isSuccess) "Custom plugin plugged into Zaythar Core."
                else "Could not add plugin: ${res.exceptionOrNull()?.localizedMessage ?: "error"}"
            )
        }
    }

    fun deletePlugin(pluginId: String, onToast: (String) -> Unit) {
        viewModelScope.launch {
            val res = workspaceStore.deletePlugin(pluginId)
            onToast(if (res.isSuccess) "Plugin unplugged and removed." else "Failed to remove plugin.")
        }
    }

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
                    workspaceStore.addSessionMessage(
                        sessionId = activeSessionId,
                        role = "assistant",
                        text = "[CAMERA EYES · ${mode.title}]\nPrompt: ${res.prompt}\n\n${res.analysis}",
                        agent = "research",
                        modelLabel = "${res.modelUsed}+vision",
                        hasImage = true
                    )
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
        onToast: (String) -> Unit
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
                aiRepo.streamCodeCopilot(task, language, mode, existingCode, prefs).collect { event ->
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
                    workspaceStore.addSessionMessage(
                        sessionId = activeSessionId,
                        role = "assistant",
                        text = "[MULTI-AGENT RESULT]\n${result.reply}",
                        agent = "coordinator",
                        modelLabel = result.modelUsed
                    )
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

    companion object {
        fun provideFactory(
            application: Application,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val databaseId = application.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                NexusWorkspaceViewModel(
                    firestoreRepo = FirestoreRepository(db),
                    workspaceStore = FirestoreSessionAndBrainStore(db),
                    currentUserId = currentUserId
                )
            }
        }
    }
}
