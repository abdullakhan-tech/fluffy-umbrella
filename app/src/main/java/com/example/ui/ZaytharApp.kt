package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.BrandMarkBox
import com.example.ui.components.CyberGridBackground
import com.example.ui.components.CyberTopBar
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.CodeCopilotScreen
import com.example.ui.screens.CollaborationScreen
import com.example.ui.screens.KnowledgeBaseScreen
import com.example.ui.screens.ModelMatrixScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.PersonaBuilderScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PromptNexusScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.launch

object ZaytharRoutes {
    const val OVERVIEW = "overview"
    const val ASSISTANT = "assistant"
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

private data class NavDestination(
    val route: String,
    val label: String,
    val section: String,
    val icon: ImageVector,
    val titlePrefix: String,
    val titleAccent: String
)

private val allDestinations = listOf(
    NavDestination(ZaytharRoutes.OVERVIEW, "Overview", "COMMAND", Icons.Default.Dashboard, "SYSTEM", "OVERVIEW"),
    NavDestination(ZaytharRoutes.ASSISTANT, "AI Assistant", "COMMAND", Icons.Default.ChatBubble, "AI", "ASSISTANT"),
    NavDestination(ZaytharRoutes.PERSONA, "Persona Builder", "COMMAND", Icons.Default.Psychology, "PERSONA", "BUILDER"),
    NavDestination(ZaytharRoutes.SETTINGS, "AI Settings", "COMMAND", Icons.Default.Settings, "AI CORE", "SETTINGS"),
    NavDestination(ZaytharRoutes.COLLABORATION, "Collaboration", "WORKSPACE", Icons.Default.Hub, "COLLABORATION", "NEXUS"),
    NavDestination(ZaytharRoutes.MODELS, "Model Matrix", "WORKSPACE", Icons.Default.Layers, "AI MODEL", "MATRIX"),
    NavDestination(ZaytharRoutes.STUDIO, "Image & Video Studio", "WORKSPACE", Icons.Default.AutoAwesome, "MEDIA", "STUDIO"),
    NavDestination(ZaytharRoutes.CODE, "Code Copilot", "WORKSPACE", Icons.Default.Code, "CODE", "COPILOT"),
    NavDestination(ZaytharRoutes.PROMPTS, "Prompt Nexus", "WORKSPACE", Icons.AutoMirrored.Filled.MenuBook, "PROMPT", "NEXUS"),
    NavDestination(ZaytharRoutes.KNOWLEDGE, "Knowledge Base", "WORKSPACE", Icons.AutoMirrored.Filled.LibraryBooks, "KNOWLEDGE", "BASE"),
    NavDestination(ZaytharRoutes.PROFILE, "User Profile", "ACCOUNT", Icons.Default.Person, "USER", "PROFILE"),
    NavDestination(ZaytharRoutes.SECURITY, "API & Security", "ACCOUNT", Icons.Default.Security, "API &", "SECURITY")
)

private val bottomBarRoutes = setOf(
    ZaytharRoutes.OVERVIEW,
    ZaytharRoutes.ASSISTANT,
    ZaytharRoutes.COLLABORATION,
    ZaytharRoutes.KNOWLEDGE,
    ZaytharRoutes.SETTINGS
)

@Composable
fun ZaytharApp(
    viewModel: ZaytharViewModel,
    userEmail: String?,
    userDisplayName: String?,
    onSignOut: () -> Unit
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val chatMessagesState by viewModel.chatMessagesState.collectAsStateWithLifecycle()
    val memoryNotesState by viewModel.memoryNotesState.collectAsStateWithLifecycle()
    val knowledgeFactsState by viewModel.knowledgeFactsState.collectAsStateWithLifecycle()
    val activeAgent by viewModel.activeAgent.collectAsStateWithLifecycle()
    val composerInput by viewModel.composerInput.collectAsStateWithLifecycle()
    val chatUiState by viewModel.chatUiState.collectAsStateWithLifecycle()
    val agentRunState by viewModel.agentRunState.collectAsStateWithLifecycle()
    val studioBrief by viewModel.studioBrief.collectAsStateWithLifecycle()
    val uptimeSeconds by viewModel.uptimeSeconds.collectAsStateWithLifecycle()
    val clockText by viewModel.clockText.collectAsStateWithLifecycle()
    val firebaseReady by viewModel.firebaseReady.collectAsStateWithLifecycle()

    val syncedOperatorName = (userProfileState as? com.example.domain.UiState.Success)
        ?.data?.operatorName
        ?.takeIf { it.isNotBlank() }
        ?: prefs.operatorName

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
                                prefs = prefs,
                                syncedOperatorName = syncedOperatorName,
                                userEmail = userEmail,
                                userDisplayName = userDisplayName,
                                chatMessagesState = chatMessagesState,
                                memoryNotesState = memoryNotesState,
                                knowledgeFactsState = knowledgeFactsState,
                                activeAgent = activeAgent,
                                composerInput = composerInput,
                                chatUiState = chatUiState,
                                agentRunState = agentRunState,
                                studioBrief = studioBrief,
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
                                                    ZaytharRoutes.COLLABORATION -> "Agents"
                                                    ZaytharRoutes.KNOWLEDGE -> "Memory"
                                                    else -> "Settings"
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
                                prefs = prefs,
                                syncedOperatorName = syncedOperatorName,
                                userEmail = userEmail,
                                userDisplayName = userDisplayName,
                                chatMessagesState = chatMessagesState,
                                memoryNotesState = memoryNotesState,
                                knowledgeFactsState = knowledgeFactsState,
                                activeAgent = activeAgent,
                                composerInput = composerInput,
                                chatUiState = chatUiState,
                                agentRunState = agentRunState,
                                studioBrief = studioBrief,
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
    navController: androidx.navigation.NavHostController,
    viewModel: ZaytharViewModel,
    prefs: com.example.data.local.ZaytharPreferences,
    syncedOperatorName: String,
    userEmail: String?,
    userDisplayName: String?,
    chatMessagesState: com.example.domain.UiState<List<com.example.data.remote.FirestoreChatMessage>>,
    memoryNotesState: com.example.domain.UiState<List<com.example.data.remote.FirestoreMemoryNote>>,
    knowledgeFactsState: com.example.domain.UiState<List<com.example.data.remote.FirestoreKnowledgeFact>>,
    activeAgent: com.example.domain.AgentRole,
    composerInput: String,
    chatUiState: com.example.domain.UiState<String>,
    agentRunState: com.example.domain.UiState<com.example.domain.MultiAgentRunResult>,
    studioBrief: String,
    uptimeSeconds: Long,
    firebaseReady: Boolean,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = ZaytharRoutes.OVERVIEW
    ) {
        composable(ZaytharRoutes.OVERVIEW) {
            OverviewScreen(
                prefs = prefs,
                firebaseReady = firebaseReady,
                uptimeSeconds = uptimeSeconds,
                onNavigate = onNavigate
            )
        }
        composable(ZaytharRoutes.ASSISTANT) {
            AssistantScreen(
                prefs = prefs.copy(operatorName = syncedOperatorName),
                activeAgent = activeAgent,
                composerText = composerInput,
                messagesState = chatMessagesState,
                chatUiState = chatUiState,
                onComposerChange = viewModel::updateComposerInput,
                onSend = viewModel::sendChatMessage,
                onClearChat = viewModel::clearChat,
                onSelectAgent = viewModel::selectAgent,
                onSelectLanguage = viewModel::selectLanguage,
                onToggleSpeakReplies = viewModel::toggleSpeakReplies,
                onToast = viewModel::emitToast
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
                onRunTeam = viewModel::runMultiAgent,
                onAddFact = viewModel::addKnowledgeFact,
                onDeleteFact = viewModel::deleteKnowledgeFact
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
                briefText = studioBrief,
                onGenerateBrief = viewModel::generateCreativeBrief
            )
        }
        composable(ZaytharRoutes.CODE) {
            CodeCopilotScreen(onToast = viewModel::emitToast)
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
                onAddNote = viewModel::addMemoryNote,
                onDeleteNote = viewModel::deleteMemoryNote
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
        composable(ZaytharRoutes.SECURITY) {
            SecurityScreen(
                firebaseReady = firebaseReady,
                userEmail = userEmail
            )
        }
    }
}

@Composable
private fun CyberSidebar(
    currentRoute: String,
    firebaseReady: Boolean,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cyber = LocalCyberColors.current
    Column(
        modifier = modifier
            .background(cyber.panelGlass)
            .padding(14.dp)
    ) {
        // Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            BrandMarkBox(sizeDp = 40)
            Column {
                Text(
                    text = "ZAYTHAR",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 2.5.sp
                )
                Text(
                    text = "NEURAL COMMAND NEXUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Scrollable nav sections
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf("COMMAND", "WORKSPACE", "ACCOUNT").forEach { section ->
                Text(
                    text = section,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                allDestinations.filter { it.section == section }.forEach { dest ->
                    val active = currentRoute == dest.route
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { onSelectRoute(dest.route) }
                            .testTag("drawer_nav_${dest.route}"),
                        shape = MaterialTheme.shapes.small,
                        color = if (active) cyber.accent.copy(alpha = 0.18f) else Color.Transparent,
                        border = if (active) BorderStroke(1.dp, cyber.accent.copy(alpha = 0.55f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = null,
                                tint = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (firebaseReady) cyber.cyberGreen else cyber.accent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (firebaseReady) "CORE SYSTEM ACTIVE" else "CORE STANDBY",
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            text = "HYBRID INTELLIGENCE · v2.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 15.dp, top = 2.dp)
        )
    }
}
