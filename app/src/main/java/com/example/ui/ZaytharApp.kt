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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.UiState
import com.example.ui.components.CyberGridBackground
import com.example.ui.components.CyberSidebar
import com.example.ui.components.CyberTopBar
import com.example.ui.screens.ToolsHubSheet
import com.example.ui.theme.LocalCyberColors
import kotlinx.coroutines.launch

@Composable
fun ZaytharApp(
    viewModel: ZaytharViewModel,
    workspaceViewModel: NexusWorkspaceViewModel,
    userEmail: String?,
    userDisplayName: String?,
    onSignOut: () -> Unit,
    isGuest: Boolean = false
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val uptimeSeconds by viewModel.uptimeSeconds.collectAsStateWithLifecycle()
    val clockText by viewModel.clockText.collectAsStateWithLifecycle()
    val firebaseReady by viewModel.firebaseReady.collectAsStateWithLifecycle()
    val chatSessionsState by viewModel.chatSessionsState.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()

    val recentSessions = (chatSessionsState as? UiState.Success)?.data.orEmpty()
    val operatorName = (userProfileState as? UiState.Success)
        ?.data?.operatorName
        ?.takeIf { it.isNotBlank() }
        ?: userDisplayName?.takeIf { it.isNotBlank() }
        ?: prefs.operatorName

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ZaytharRoutes.OVERVIEW
    val currentDest = allZaytharDestinations.find { it.route == currentRoute } ?: allZaytharDestinations.first()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showToolsSheet by remember { mutableStateOf(false) }

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

    fun startNewChatFromSidebar() {
        viewModel.createNewChatSession("New Nexus Thread")
        navigateTo(ZaytharRoutes.ASSISTANT)
    }

    fun openSessionFromSidebar(sessionId: String) {
        viewModel.selectChatSession(sessionId)
        navigateTo(ZaytharRoutes.ASSISTANT)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp

        if (isExpanded) {
            CyberGridBackground(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    CyberSidebar(
                        destinations = allZaytharDestinations,
                        currentRoute = currentRoute,
                        firebaseReady = firebaseReady,
                        onSelectRoute = ::navigateTo,
                        modifier = Modifier
                            .width(248.dp)
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        recentSessions = recentSessions,
                        activeSessionId = activeSessionId,
                        operatorName = operatorName,
                        isGuest = isGuest,
                        onNewChatClick = ::startNewChatFromSidebar,
                        onSelectSession = ::openSessionFromSidebar,
                        onOpenSettings = { navigateTo(ZaytharRoutes.SETTINGS) }
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
                                onToggleTheme = viewModel::toggleTheme,
                                themeMode = prefs.themeMode,
                                onOpenSearch = { navigateTo(ZaytharRoutes.SEARCH) },
                                onOpenTools = { showToolsSheet = true }
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
                                onSignOut = onSignOut,
                                isGuest = isGuest
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
                            destinations = allZaytharDestinations,
                            currentRoute = currentRoute,
                            firebaseReady = firebaseReady,
                            onSelectRoute = ::navigateTo,
                            modifier = Modifier
                                .width(270.dp)
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .navigationBarsPadding(),
                            recentSessions = recentSessions,
                            activeSessionId = activeSessionId,
                            operatorName = operatorName,
                            isGuest = isGuest,
                            onNewChatClick = ::startNewChatFromSidebar,
                            onSelectSession = ::openSessionFromSidebar,
                            onOpenSettings = { navigateTo(ZaytharRoutes.SETTINGS) }
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
                                onToggleTheme = viewModel::toggleTheme,
                                themeMode = prefs.themeMode,
                                onOpenSearch = { navigateTo(ZaytharRoutes.SEARCH) },
                                onOpenTools = { showToolsSheet = true }
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = cyber.panelGlass.copy(alpha = 0.96f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                modifier = Modifier.navigationBarsPadding()
                            ) {
                                allZaytharDestinations.filter { it.route in bottomBarZaytharRoutes }.forEach { item ->
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
                                onSignOut = onSignOut,
                                isGuest = isGuest
                            )
                        }
                    }
                }
            }
        }
    }

    if (showToolsSheet) {
        ToolsHubSheet(
            webConnectEnabled = prefs.webConnectEnabled,
            onToggleWebConnect = { viewModel.toggleWebConnect(!prefs.webConnectEnabled) },
            onNavigate = ::navigateTo,
            onDismiss = { showToolsSheet = false }
        )
    }
}
