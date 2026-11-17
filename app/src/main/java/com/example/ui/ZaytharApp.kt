package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PsychologyAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.UiState
import com.example.ui.components.CyberGridBackground
import com.example.ui.components.CyberSidebar
import com.example.ui.components.CyberTopBar
import com.example.ui.components.ZaytharNavDestination
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.CameraEyesScreen
import com.example.ui.screens.CodeCopilotScreen
import com.example.ui.screens.CollaborationScreen
import com.example.ui.screens.KnowledgeBaseScreen
import com.example.ui.screens.ModelMatrixScreen
import com.example.ui.screens.NeuralBrainScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.NetworkStatusScreen
import com.example.ui.screens.PersonaBuilderScreen
import com.example.ui.screens.PluginHubScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PromptNexusScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.LocalCyberColors
import kotlinx.coroutines.launch

object ZaytharRoutes {
    const val OVERVIEW = "overview"
    const val NETWORK = "network"
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
}

private val allDestinations = listOf(
    ZaytharNavDestination(ZaytharRoutes.OVERVIEW, "Overview", "COMMAND", Icons.Default.Dashboard, "SYSTEM", "OVERVIEW"),
    ZaytharNavDestination(ZaytharRoutes.NETWORK, "Network Intelligence", "COMMAND", Icons.Default.SignalCellularAlt, "LIVE", "NETWORK"),
    ZaytharNavDestination(ZaytharRoutes.ASSISTANT, "AI Assistant", "COMMAND", Icons.Default.ChatBubble, "AI", "ASSISTANT"),
    ZaytharNavDestination(ZaytharRoutes.BRAIN, "AI Brain", "COMMAND", Icons.Default.PsychologyAlt, "NEURAL", "BRAIN"),
    ZaytharNavDestination(ZaytharRoutes.PERSONA, "Persona Builder", "COMMAND", Icons.Default.Psychology, "PERSONA", "BUILDER"),
    ZaytharNavDestination(ZaytharRoutes.SETTINGS, "AI Settings", "COMMAND", Icons.Default.Settings, "AI CORE", "SETTINGS"),
    ZaytharNavDestination(ZaytharRoutes.CAMERA_EYES, "Camera Eyes", "WORKSPACE", Icons.Default.CameraAlt, "CAMERA", "EYES"),
    ZaytharNavDestination(ZaytharRoutes.PLUGINS, "Plugin Plug", "WORKSPACE", Icons.Default.Extension, "PLUGIN", "PLUG HUB"),
    ZaytharNavDestination(ZaytharRoutes.COLLABORATION, "Collaboration", "WORKSPACE", Icons.Default.Hub, "COLLABORATION", "NEXUS"),
    ZaytharNavDestination(ZaytharRoutes.MODELS, "Model Matrix", "WORKSPACE", Icons.Default.Layers, "AI MODEL", "MATRIX"),
    ZaytharNavDestination(ZaytharRoutes.STUDIO, "Image & Video Studio", "WORKSPACE", Icons.Default.AutoAwesome, "MEDIA", "STUDIO"),
    ZaytharNavDestination(ZaytharRoutes.CODE, "Code Copilot", "WORKSPACE", Icons.Default.Code, "CODE", "COPILOT"),
    ZaytharNavDestination(ZaytharRoutes.PROMPTS, "Prompt Nexus", "WORKSPACE", Icons.AutoMirrored.Filled.MenuBook, "PROMPT", "NEXUS"),
    ZaytharNavDestination(ZaytharRoutes.KNOWLEDGE, "Knowledge Base", "WORKSPACE", Icons.AutoMirrored.Filled.LibraryBooks, "KNOWLEDGE", "BASE"),
    ZaytharNavDestination(ZaytharRoutes.PROFILE, "User Profile", "ACCOUNT", Icons.Default.Person, "USER", "PROFILE"),
    ZaytharNavDestination(ZaytharRoutes.SECURITY, "API & Security", "ACCOUNT", Icons.Default.Security, "API &", "SECURITY")
)

private val bottomBarRoutes = setOf(
    ZaytharRoutes.OVERVIEW,
    ZaytharRoutes.ASSISTANT,
    ZaytharRoutes.BRAIN,
    ZaytharRoutes.CAMERA_EYES,
    ZaytharRoutes.PLUGINS
)

@Composable
fun ZaytharApp(
    viewModel: ZaytharViewModel,
    workspaceViewModel: NexusWorkspaceViewModel,
    userEmail: String?,
    userDisplayName: String?,
    onSignOut: () -> Unit
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val uptimeSeconds by viewModel.uptimeSeconds.collectAsStateWithLifecycle()
    val clockText by viewModel.clockText.collectAsStateWithLifecycle()
    val firebaseReady by viewModel.firebaseReady.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ZaytharRoutes.OVERVIEW
    val currentDest = allDestinations.find { it.route == currentRoute } ?: allDestinations.first()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        scope.launch { drawerState.close() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp

        if (isExpanded) {
            CyberGridBackground(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    CyberSidebar(
                        destinations = allDestinations,
                        currentRoute = currentRoute,
                        firebaseReady = firebaseReady,
                        onSelectRoute = ::navigateTo,
                        modifier = Modifier
                            .width(248.dp)
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    )
                    Scaffold(
                        modifier = Modifier.weight(1f),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            CyberTopBar(
                                titlePrefix = currentDest.titlePrefix,
                                titleAccent = currentDest.titleAccent,
                                clockText = clockText,
                                motionEnabled = prefs.motion3dEnabled,
                                firebaseReady = firebaseReady,
                                showMenuButton = false,
                                onMenuClick = {},
                                onToggleMotion = viewModel::toggleMotion3d,
                                onToggleTheme = viewModel::toggleTheme
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            ZaytharNavHost(
                                navController = navController,
                                viewModel = viewModel,
                                workspaceViewModel = workspaceViewModel,
                                userEmail = userEmail,
                                userDisplayName = userDisplayName,
                                uptimeSeconds = uptimeSeconds,
                                firebaseReady = firebaseReady,
                                onNavigate = ::navigateTo,
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        } else {
            val cyber = LocalCyberColors.current
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = cyber.panelGlass,
                        drawerContentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        CyberSidebar(
                            destinations = allDestinations,
                            currentRoute = currentRoute,
                            firebaseReady = firebaseReady,
                            onSelectRoute = ::navigateTo,
                            modifier = Modifier
                                .width(270.dp)
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        )
                    }
                }
            ) {
                CyberGridBackground(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            CyberTopBar(
                                titlePrefix = currentDest.titlePrefix,
                                titleAccent = currentDest.titleAccent,
                                clockText = clockText,
                                motionEnabled = prefs.motion3dEnabled,
                                firebaseReady = firebaseReady,
                                showMenuButton = true,
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onToggleMotion = viewModel::toggleMotion3d,
                                onToggleTheme = viewModel::toggleTheme
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = cyber.panelGlass.copy(alpha = 0.96f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                modifier = Modifier.navigationBarsPadding()
                            ) {
                                allDestinations.filter { it.route in bottomBarRoutes }.forEach { item ->
                                    val selected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTo(item.route) },
                                        modifier = Modifier.testTag("bottom_nav_${item.route}"),
                                        icon = {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = when (item.route) {
                                                    ZaytharRoutes.OVERVIEW -> "Overview"
                                                    ZaytharRoutes.ASSISTANT -> "Chat"
                                                    ZaytharRoutes.BRAIN -> "Brain"
                                                    ZaytharRoutes.CAMERA_EYES -> "Eyes"
                                                    else -> "Plugins"
                                                },
                                                maxLines = 1
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = cyber.accentSoft,
                                            selectedTextColor = cyber.accentSoft,
                                            indicatorColor = cyber.accent.copy(alpha = 0.22f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            ZaytharNavHost(
                                navController = navController,
                                viewModel = viewModel,
                                workspaceViewModel = workspaceViewModel,
                                userEmail = userEmail,
                                userDisplayName = userDisplayName,
                                uptimeSeconds = uptimeSeconds,
                                firebaseReady = firebaseReady,
                                onNavigate = ::navigateTo,
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZaytharNavHost(
    navController: NavHostController,
    viewModel: ZaytharViewModel,
    workspaceViewModel: NexusWorkspaceViewModel,
    userEmail: String?,
    userDisplayName: String?,
    uptimeSeconds: Long,
    firebaseReady: Boolean,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit
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

    val memoryNotesState by workspaceViewModel.memoryNotesState.collectAsStateWithLifecycle()
    val knowledgeFactsState by workspaceViewModel.knowledgeFactsState.collectAsStateWithLifecycle()
    val pluginsState by workspaceViewModel.pluginsState.collectAsStateWithLifecycle()
    val brainRunsState by workspaceViewModel.brainRunsState.collectAsStateWithLifecycle()
    val brainAnalysisState by workspaceViewModel.brainAnalysisState.collectAsStateWithLifecycle()
    val visionState by workspaceViewModel.visionState.collectAsStateWithLifecycle()
    val codeCopilotState by workspaceViewModel.codeCopilotState.collectAsStateWithLifecycle()
    val studioBriefState by workspaceViewModel.studioBriefState.collectAsStateWithLifecycle()
    val agentRunState by workspaceViewModel.agentRunState.collectAsStateWithLifecycle()

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
            OverviewScreen(
                prefs = prefs,
                firebaseReady = firebaseReady,
                uptimeSeconds = uptimeSeconds,
                activePluginCount = enabledPluginsCount,
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
                onComposerChange = viewModel::updateComposerInput,
                onSend = viewModel::sendChatMessage,
                onClearChat = viewModel::clearChat,
                onSelectSession = viewModel::selectChatSession,
                onCreateSession = viewModel::createNewChatSession,
                onRenameSession = viewModel::renameChatSession,
                onDeleteSession = viewModel::deleteChatSession,
                onOpenPlugins = { onNavigate(ZaytharRoutes.PLUGINS) },
                onOpenCameraEyes = { onNavigate(ZaytharRoutes.CAMERA_EYES) },
                onToggleWebConnect = viewModel::toggleWebConnect,
                onSelectAgent = viewModel::selectAgent,
                onSelectLanguage = viewModel::selectLanguage,
                onToggleSpeakReplies = viewModel::toggleSpeakReplies,
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
                onToast = viewModel::emitToast
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
                }
            )
        }
        composable(ZaytharRoutes.CODE) {
            CodeCopilotScreen(
                codeCopilotState = codeCopilotState,
                onRunCopilot = { task, lang, mode, code ->
                    workspaceViewModel.runCodeCopilot(task, lang, mode, code, prefs, viewModel::emitToast)
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
                }
            )
        }
        composable(ZaytharRoutes.PROFILE) {
            ProfileScreen(
                operatorName = syncedOperatorName,
                userEmail = userEmail,
                userDisplayName = userDisplayName,
                onSaveOperatorName = viewModel::saveOperatorName,
                onSignOut = onSignOut
            )
        }
        composable(ZaytharRoutes.NETWORK) {
            NetworkStatusScreen()
        }
        composable(ZaytharRoutes.SECURITY) {
            SecurityScreen(
                firebaseReady = firebaseReady,
                userEmail = userEmail
            )
        }
    }
}
