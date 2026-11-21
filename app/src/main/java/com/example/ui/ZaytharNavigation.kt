package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.PsychologyAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.domain.ProviderRoute
import com.example.domain.UiState
import com.example.ui.components.ZaytharNavDestination
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.CameraEyesScreen
import com.example.ui.screens.CodeCopilotScreen
import com.example.ui.screens.CollaborationScreen
import com.example.ui.screens.EngineHubScreen
import com.example.ui.screens.KnowledgeBaseScreen
import com.example.ui.screens.ModelMatrixScreen
import com.example.ui.screens.NeuralBrainScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.PersonaBuilderScreen
import com.example.ui.screens.PluginHubScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PromptNexusScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.VoiceAndAudioScreen

object ZaytharRoutes {
    const val OVERVIEW = "overview"
    const val ENGINES = "engine_hub"
    const val ASSISTANT = "assistant"
    const val BRAIN = "brain"
    const val CAMERA_EYES = "camera_eyes"
    const val PLUGINS = "plugins"
    const val PERSONA = "persona"
    const val SETTINGS = "settingsPanel"
    const val COLLABORATION = "collaboration"
    const val MODELS = "models"
    const val STUDIO = "studio"
    const val CODE = "code"
    const val PROMPTS = "prompts"
    const val KNOWLEDGE = "knowledge"
    const val PROFILE = "profile"
    const val SECURITY = "security"
    const val VOICE_AUDIO = "voice_audio"
}

internal val allZaytharDestinations = listOf(
    ZaytharNavDestination(ZaytharRoutes.OVERVIEW, "Overview", "COMMAND", Icons.Rounded.Dashboard, "SYSTEM", "OVERVIEW"),
    ZaytharNavDestination(ZaytharRoutes.ENGINES, "AI Engine Hub", "COMMAND", Icons.Rounded.PrecisionManufacturing, "AI ENGINE", "NEXUS"),
    ZaytharNavDestination(ZaytharRoutes.ASSISTANT, "AI Assistant", "COMMAND", Icons.Rounded.ChatBubble, "AI", "ASSISTANT"),
    ZaytharNavDestination(ZaytharRoutes.VOICE_AUDIO, "Voice & Audio", "COMMAND", Icons.Rounded.GraphicEq, "VOICE &", "AUDIO"),
    ZaytharNavDestination(ZaytharRoutes.BRAIN, "AI Brain", "COMMAND", Icons.Rounded.PsychologyAlt, "NEURAL", "BRAIN"),
    ZaytharNavDestination(ZaytharRoutes.PERSONA, "Persona Builder", "COMMAND", Icons.Rounded.Psychology, "PERSONA", "BUILDER"),
    ZaytharNavDestination(ZaytharRoutes.SETTINGS, "AI Settings", "COMMAND", Icons.Rounded.Settings, "AI CORE", "SETTINGS"),
    ZaytharNavDestination(ZaytharRoutes.CAMERA_EYES, "Camera Eyes", "WORKSPACE", Icons.Rounded.CameraAlt, "CAMERA", "EYES"),
    ZaytharNavDestination(ZaytharRoutes.PLUGINS, "Plugin Plug", "WORKSPACE", Icons.Rounded.Extension, "PLUGIN", "PLUG HUB"),
    ZaytharNavDestination(ZaytharRoutes.COLLABORATION, "Collaboration", "WORKSPACE", Icons.Rounded.Hub, "COLLABORATION", "NEXUS"),
    ZaytharNavDestination(ZaytharRoutes.MODELS, "Model Matrix", "WORKSPACE", Icons.Rounded.Layers, "AI MODEL", "MATRIX"),
    ZaytharNavDestination(ZaytharRoutes.STUDIO, "Image & Video Studio", "WORKSPACE", Icons.Rounded.AutoAwesome, "MEDIA", "STUDIO"),
    ZaytharNavDestination(ZaytharRoutes.CODE, "Code Copilot", "WORKSPACE", Icons.Rounded.Code, "CODE", "COPILOT"),
    ZaytharNavDestination(ZaytharRoutes.PROMPTS, "Prompt Nexus", "WORKSPACE", Icons.AutoMirrored.Rounded.MenuBook, "PROMPT", "NEXUS"),
    ZaytharNavDestination(ZaytharRoutes.KNOWLEDGE, "Knowledge Base", "WORKSPACE", Icons.AutoMirrored.Rounded.LibraryBooks, "KNOWLEDGE", "BASE"),
    ZaytharNavDestination(ZaytharRoutes.PROFILE, "User Profile", "ACCOUNT", Icons.Rounded.Person, "USER", "PROFILE"),
    ZaytharNavDestination(ZaytharRoutes.SECURITY, "API & Security", "ACCOUNT", Icons.Rounded.VerifiedUser, "API &", "SECURITY")
)

internal val bottomBarZaytharRoutes = setOf(
    ZaytharRoutes.OVERVIEW,
    ZaytharRoutes.ASSISTANT,
    ZaytharRoutes.BRAIN,
    ZaytharRoutes.CAMERA_EYES,
    ZaytharRoutes.PLUGINS
)

@Composable
internal fun ZaytharNavHost(
    navController: NavHostController,
    viewModel: ZaytharViewModel,
    workspaceViewModel: NexusWorkspaceViewModel,
    userEmail: String?,
    userDisplayName: String?,
    uptimeSeconds: Long,
    firebaseReady: Boolean,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    isGuest: Boolean = false
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val chatSessionsState by viewModel.chatSessionsState.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val sessionMessagesState by viewModel.sessionMessagesState.collectAsStateWithLifecycle()
    val activePlugins by viewModel.activePluginsFlow.collectAsStateWithLifecycle()
    val activeAgent by viewModel.activeAgent.collectAsStateWithLifecycle()
    val composerInput by viewModel.composerInput.collectAsStateWithLifecycle()
    val chatUiState by viewModel.chatUiState.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()

    val memoryNotesState by workspaceViewModel.memoryNotesState.collectAsStateWithLifecycle()
    val knowledgeFactsState by workspaceViewModel.knowledgeFactsState.collectAsStateWithLifecycle()
    val pluginsState by workspaceViewModel.pluginsState.collectAsStateWithLifecycle()
    val brainRunsState by workspaceViewModel.brainRunsState.collectAsStateWithLifecycle()
    val brainAnalysisState by workspaceViewModel.brainAnalysisState.collectAsStateWithLifecycle()
    val visionState by workspaceViewModel.visionState.collectAsStateWithLifecycle()
    val codeCopilotState by workspaceViewModel.codeCopilotState.collectAsStateWithLifecycle()
    val studioBriefState by workspaceViewModel.studioBriefState.collectAsStateWithLifecycle()
    val studioImageState by workspaceViewModel.studioImageState.collectAsStateWithLifecycle()
    val engineDiagnosticsState by workspaceViewModel.engineDiagnosticsState.collectAsStateWithLifecycle()
    val agentRunState by workspaceViewModel.agentRunState.collectAsStateWithLifecycle()
    val liveVoiceStatus by workspaceViewModel.voiceAudio.liveVoiceStatus.collectAsStateWithLifecycle()
    val liveTranscripts by workspaceViewModel.voiceAudio.liveTranscripts.collectAsStateWithLifecycle()
    val liveVoiceError by workspaceViewModel.voiceAudio.liveVoiceError.collectAsStateWithLifecycle()
    val isRecordingAudio by workspaceViewModel.voiceAudio.isRecordingAudio.collectAsStateWithLifecycle()
    val recordingSeconds by workspaceViewModel.voiceAudio.recordingSeconds.collectAsStateWithLifecycle()
    val transcribeState by workspaceViewModel.voiceAudio.transcribeState.collectAsStateWithLifecycle()

    val syncedOperatorName = (userProfileState as? UiState.Success)
        ?.data?.operatorName
        ?.takeIf { it.isNotBlank() }
        ?: prefs.operatorName
    val enabledPluginsCount = activePlugins.count { it.enabled }

    NavHost(
        navController = navController,
        startDestination = ZaytharRoutes.OVERVIEW
    ) {
        composable(ZaytharRoutes.OVERVIEW) {
            val ragCorpusCount = ((memoryNotesState as? UiState.Success)?.data?.size ?: 0) +
                ((knowledgeFactsState as? UiState.Success)?.data?.size ?: 0)
            OverviewScreen(
                prefs = prefs,
                firebaseReady = firebaseReady,
                uptimeSeconds = uptimeSeconds,
                activePluginCount = enabledPluginsCount,
                onNavigate = onNavigate,
                ragCorpusCount = ragCorpusCount
            )
        }
        composable(ZaytharRoutes.ENGINES) {
            EngineHubScreen(
                prefs = prefs,
                diagnosticsState = engineDiagnosticsState,
                activePluginCount = enabledPluginsCount,
                onRunDiagnostics = { includeLiveProbe ->
                    workspaceViewModel.runAllEnginesDiagnostic(
                        includeLiveGeminiProbe = includeLiveProbe,
                        firebaseReady = firebaseReady,
                        activePluginsCount = enabledPluginsCount,
                        onToast = viewModel::emitToast
                    )
                },
                onSelectRouteAndChat = { route, prompt ->
                    viewModel.selectProviderRoute(route)
                    if (prompt.isNotBlank()) {
                        viewModel.applyPromptTemplate(prompt)
                    }
                    onNavigate(ZaytharRoutes.ASSISTANT)
                },
                onNavigate = onNavigate
            )
        }
        composable(ZaytharRoutes.ASSISTANT) {
            AssistantScreen(
                prefs = prefs.copy(operatorName = syncedOperatorName),
                activeAgent = activeAgent,
                composerText = composerInput,
                sessionsState = chatSessionsState,
                activeSessionId = activeSessionId,
                activePluginCount = enabledPluginsCount,
                messagesState = sessionMessagesState,
                chatUiState = chatUiState,
                isSpeaking = isSpeaking,
                onStopSpeaking = viewModel::stopSpeaking,
                onComposerChange = viewModel::updateComposerInput,
                onSend = viewModel::sendChatMessage,
                onClearChat = viewModel::clearChatAndStartNewSession,
                onClearAndNewSession = viewModel::clearChatAndStartNewSession,
                onSelectSession = viewModel::selectChatSession,
                onCreateSession = viewModel::createNewChatSession,
                onRenameSession = viewModel::renameChatSession,
                onDeleteSession = viewModel::deleteChatSession,
                onOpenPlugins = { onNavigate(ZaytharRoutes.PLUGINS) },
                onOpenCameraEyes = { onNavigate(ZaytharRoutes.CAMERA_EYES) },
                onOpenVoiceAudio = { onNavigate(ZaytharRoutes.VOICE_AUDIO) },
                onToggleWebConnect = viewModel::toggleWebConnect,
                onToggleMapsGrounding = viewModel::toggleMapsGrounding,
                onSelectAgent = viewModel::selectAgent,
                onSelectLanguage = viewModel::selectLanguage,
                onToggleSpeakReplies = viewModel::toggleSpeakReplies,
                onSelectThemeMode = viewModel::setThemeMode,
                onToast = viewModel::emitToast
            )
        }
        composable(ZaytharRoutes.VOICE_AUDIO) {
            VoiceAndAudioScreen(
                liveStatus = liveVoiceStatus,
                liveTranscripts = liveTranscripts,
                liveError = liveVoiceError,
                isRecordingAudio = isRecordingAudio,
                recordingSeconds = recordingSeconds,
                transcribeState = transcribeState,
                onStartLiveVoice = { voice -> workspaceViewModel.voiceAudio.startLiveVoice(voice, prefs, viewModel::emitToast) },
                onStopLiveVoice = workspaceViewModel.voiceAudio::stopLiveVoice,
                onSendLiveText = { txt -> workspaceViewModel.voiceAudio.sendLiveTextTurn(txt, viewModel::emitToast) },
                onClearLiveTranscripts = workspaceViewModel.voiceAudio::clearLiveTranscripts,
                onStartRecording = { ctx -> workspaceViewModel.voiceAudio.startAudioRecording(ctx, viewModel::emitToast) },
                onStopRecordingAndTranscribe = { workspaceViewModel.voiceAudio.stopAndTranscribeAudio(prefs, viewModel::emitToast) },
                onSendTranscriptToChat = { text ->
                    viewModel.applyPromptTemplate(text)
                    onNavigate(ZaytharRoutes.ASSISTANT)
                },
                onSaveTranscriptToRag = { note -> workspaceViewModel.addMemoryNote(note, viewModel::emitToast) },
                onToast = viewModel::emitToast
            )
        }
        composable(ZaytharRoutes.BRAIN) {
            val memoriesCount = (memoryNotesState as? UiState.Success)?.data?.size ?: 0
            val factsCount = (knowledgeFactsState as? UiState.Success)?.data?.size ?: 0
            val recentChatTranscript = ((sessionMessagesState as? UiState.Success)?.data ?: emptyList())
                .takeLast(6)
                .joinToString("\n") { "${it.role.uppercase()}: ${it.text.take(240)}" }

            NeuralBrainScreen(
                brainAnalysisState = brainAnalysisState,
                brainRunsState = brainRunsState,
                memoriesCount = memoriesCount,
                factsCount = factsCount,
                onRunBrain = { query, mode ->
                    workspaceViewModel.runNeuralBrain(
                        prompt = query,
                        mode = mode,
                        recentSessionMessagesText = recentChatTranscript,
                        prefs = prefs,
                        onToast = viewModel::emitToast
                    )
                },
                onCommitTriples = { triples ->
                    workspaceViewModel.commitSuggestedTriples(triples, viewModel::emitToast)
                },
                onDeleteBrainRun = { runId ->
                    workspaceViewModel.deleteBrainRun(runId, viewModel::emitToast)
                },
                onSaveSynthesisToMemory = { synthesis ->
                    workspaceViewModel.addMemoryNote(synthesis, viewModel::emitToast)
                }
            )
        }
        composable(ZaytharRoutes.CAMERA_EYES) {
            CameraEyesScreen(
                visionState = visionState,
                onAnalyzeImage = { bitmap, prompt, mode ->
                    workspaceViewModel.analyzeWithCameraEyes(
                        bitmap = bitmap,
                        prompt = prompt,
                        mode = mode,
                        prefs = prefs,
                        activeSessionId = activeSessionId,
                        onToast = viewModel::emitToast
                    )
                },
                onToast = viewModel::emitToast,
                onSaveToRag = { note ->
                    workspaceViewModel.addMemoryNote(note, viewModel::emitToast)
                }
            )
        }
        composable(ZaytharRoutes.PLUGINS) {
            PluginHubScreen(
                pluginsState = pluginsState,
                onTogglePlugin = { id, enabled ->
                    workspaceViewModel.togglePlugin(id, enabled, viewModel::emitToast)
                },
                onAddCustomPlugin = { name, desc, cat, instr ->
                    workspaceViewModel.addCustomPlugin(name, desc, cat, instr, viewModel::emitToast)
                },
                onDeletePlugin = { id ->
                    workspaceViewModel.deletePlugin(id, viewModel::emitToast)
                }
            )
        }
        composable(ZaytharRoutes.PERSONA) {
            PersonaBuilderScreen(
                prefs = prefs,
                onUpdatePrefs = viewModel::updatePreferences,
                onToast = viewModel::emitToast
            )
        }
        composable(ZaytharRoutes.SETTINGS) {
            SettingsScreen(
                prefs = prefs,
                onUpdatePrefs = viewModel::updatePreferences,
                onSelectAccent = viewModel::updateAccent,
                onToast = viewModel::emitToast
            )
        }
        composable(ZaytharRoutes.COLLABORATION) {
            CollaborationScreen(
                agentRunState = agentRunState,
                knowledgeFactsState = knowledgeFactsState,
                onAgentNodeClick = { role ->
                    viewModel.selectAgent(role)
                    viewModel.emitToast("${role.title} specialist selected.")
                    onNavigate(ZaytharRoutes.ASSISTANT)
                },
                onRunTeam = { task, roles ->
                    workspaceViewModel.runMultiAgent(
                        task = task,
                        selectedAgents = roles,
                        prefs = prefs,
                        activeSessionId = activeSessionId,
                        onToast = viewModel::emitToast
                    )
                },
                onAddFact = { s, p, o ->
                    workspaceViewModel.addKnowledgeFact(s, p, o, onToast = viewModel::emitToast)
                },
                onDeleteFact = { id ->
                    workspaceViewModel.deleteKnowledgeFact(id, viewModel::emitToast)
                }
            )
        }
        composable(ZaytharRoutes.MODELS) {
            ModelMatrixScreen(
                prefs = prefs,
                onSelectRoute = viewModel::selectProviderRoute,
                onOpenAssistant = { onNavigate(ZaytharRoutes.ASSISTANT) }
            )
        }
        composable(ZaytharRoutes.STUDIO) {
            StudioScreen(
                briefState = studioBriefState,
                onGenerateBrief = { prompt, outputType, style ->
                    workspaceViewModel.generateCreativeBrief(prompt, outputType, style, prefs, viewModel::emitToast)
                },
                imageState = studioImageState,
                onGenerateImage = { prompt, style, ratio, refBitmap ->
                    workspaceViewModel.generateStudioImage(prompt, style, ratio, refBitmap, viewModel::emitToast)
                },
                onToast = viewModel::emitToast
            )
        }
        composable(ZaytharRoutes.CODE) {
            CodeCopilotScreen(
                codeCopilotState = codeCopilotState,
                onRunCopilot = { task, lang, mode, code, includeTests, strictSecurity ->
                    workspaceViewModel.runCodeCopilot(
                        task = task,
                        language = lang,
                        mode = mode,
                        existingCode = code,
                        prefs = prefs,
                        onToast = viewModel::emitToast,
                        includeUnitTests = includeTests,
                        strictSecurityAudit = strictSecurity
                    )
                },
                onSaveToKnowledge = { content ->
                    workspaceViewModel.addMemoryNote(content, viewModel::emitToast)
                },
                onToast = viewModel::emitToast
            )
        }
        composable(ZaytharRoutes.PROMPTS) {
            PromptNexusScreen(
                onSelectPrompt = { prefix ->
                    viewModel.applyPromptTemplate(prefix)
                    onNavigate(ZaytharRoutes.ASSISTANT)
                }
            )
        }
        composable(ZaytharRoutes.KNOWLEDGE) {
            KnowledgeBaseScreen(
                notesState = memoryNotesState,
                onAddNote = { content ->
                    workspaceViewModel.addMemoryNote(content, viewModel::emitToast)
                },
                onDeleteNote = { id ->
                    workspaceViewModel.deleteMemoryNote(id, viewModel::emitToast)
                },
                factsState = knowledgeFactsState
            )
        }
        composable(ZaytharRoutes.PROFILE) {
            ProfileScreen(
                operatorName = syncedOperatorName,
                userEmail = userEmail,
                userDisplayName = userDisplayName,
                onSaveOperatorName = viewModel::saveOperatorName,
                onSignOut = onSignOut,
                isGuest = isGuest
            )
        }
        composable(ZaytharRoutes.SECURITY) {
            SecurityScreen(
                firebaseReady = firebaseReady,
                userEmail = userEmail,
                onToast = viewModel::emitToast,
                isGuest = isGuest
            )
        }
    }
}
