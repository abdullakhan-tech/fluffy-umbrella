package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreChatSession
import com.example.data.remote.FirestoreKnowledgeFact
import com.example.data.remote.FirestoreMemoryNote
import com.example.domain.AgentRole
import com.example.domain.ProviderRoute
import com.example.domain.UiState
import com.example.ui.components.CyberSidebar
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ModelSelectorSheet
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.SearchHubScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsHubSheet
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ZaytharTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1800dp")
class UnifiedUiArtifactsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchHubScreen_filtersChatsAndMemoriesAndOpensSession() {
        var openedSessionId = ""
        val sessions = listOf(
            FirestoreChatSession(id = "s_alpha", title = "Kotlin Coroutines Help", lastMessagePreview = "Flow collectors"),
            FirestoreChatSession(id = "s_beta", title = "Recipe Ideas", lastMessagePreview = "Biryani")
        )
        val notes = listOf(
            FirestoreMemoryNote(id = "n_1", content = "App Check protects Firebase AI Logic")
        )
        val facts = listOf(
            FirestoreKnowledgeFact(id = "f_1", subject = "Zaythar", predicate = "uses", objectValue = "Gemini 3.8 Flash")
        )

        composeRule.setContent {
            ZaytharTheme {
                SearchHubScreen(
                    sessionsState = UiState.Success(sessions),
                    notesState = UiState.Success(notes),
                    factsState = UiState.Success(facts),
                    onOpenSession = { openedSessionId = it },
                    onSelectAgent = {},
                    onSelectPrompt = {},
                    onNavigate = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("search_hub_input").performTextInput("Kotlin")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_result_session_s_alpha").assertIsDisplayed().performClick()
        assertEquals("s_alpha", openedSessionId)

        composeRule.onNodeWithTag("search_filter_memories").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SAVED MEMORIES & GRAPH (0)").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_switchesTabsCreatesThreadAndSelectsAgent() {
        var createdTitle = ""
        var selectedAgent: AgentRole? = null
        val sessions = listOf(FirestoreChatSession(id = "lib_1", title = "Zaythar Dev", messageCount = 5))
        val notes = listOf(FirestoreMemoryNote(id = "mem_1", content = "Always validate URLs with WebConnector"))

        composeRule.setContent {
            ZaytharTheme {
                LibraryScreen(
                    prefs = ZaytharPreferences(),
                    sessionsState = UiState.Success(sessions),
                    activeSessionId = "lib_1",
                    notesState = UiState.Success(notes),
                    factsState = UiState.Success(emptyList()),
                    onOpenSession = {},
                    onCreateSession = { createdTitle = it },
                    onDeleteSession = {},
                    onDeleteNote = {},
                    onSelectAgent = { selectedAgent = it },
                    onNavigate = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("library_session_lib_1").assertIsDisplayed()
        composeRule.onNodeWithTag("library_new_thread_button").performClick()
        assertEquals("New Project Thread", createdTitle)

        composeRule.onNodeWithTag("library_tab_memory").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("library_open_knowledge_button").assertIsDisplayed()

        composeRule.onNodeWithTag("library_tab_instructions").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("library_agent_coding").assertIsDisplayed().performClick()
        assertEquals(AgentRole.CODING, selectedAgent)
    }

    @Test
    fun cyberSidebar_triggersNewChatRecentThreadAndSettings() {
        var newChatClicked = false
        var selectedSession = ""
        var settingsClicked = false

        composeRule.setContent {
            ZaytharTheme {
                CyberSidebar(
                    destinations = allZaytharDestinations,
                    currentRoute = ZaytharRoutes.OVERVIEW,
                    firebaseReady = true,
                    onSelectRoute = {},
                    recentSessions = listOf(FirestoreChatSession(id = "rec_1", title = "Smart Work Thread")),
                    activeSessionId = "rec_1",
                    operatorName = "Zaythar Operator",
                    onNewChatClick = { newChatClicked = true },
                    onSelectSession = { selectedSession = it },
                    onOpenSettings = { settingsClicked = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("sidebar_new_chat_button").assertIsDisplayed().performClick()
        assertTrue(newChatClicked)

        composeRule.onNodeWithTag("sidebar_recent_rec_1").performScrollTo().assertIsDisplayed().performClick()
        assertEquals("rec_1", selectedSession)

        composeRule.onNodeWithTag("sidebar_settings_button").assertIsDisplayed().performClick()
        assertTrue(settingsClicked)
    }

    @Test
    fun overviewHeroAndVoiceBottomPill_navigateToExpectedRoutes() {
        val navigatedRoutes = mutableListOf<String>()
        composeRule.setContent {
            ZaytharTheme {
                OverviewScreen(
                    prefs = ZaytharPreferences(motion3dEnabled = false),
                    firebaseReady = true,
                    uptimeSeconds = 45L,
                    activePluginCount = 5,
                    onNavigate = { navigatedRoutes += it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("hero_quick_studio").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("voice_chat_bottom_pill").assertIsDisplayed().performClick()
        assertEquals(listOf("studio", "voice_audio"), navigatedRoutes)
    }

    @Test
    fun settingsScreen_switchesCategoryAndUpdatesThemeAndEmeraldSwatch() {
        var currentPrefs = ZaytharPreferences()
        var selectedAccent = 0L

        composeRule.setContent {
            ZaytharTheme {
                SettingsScreen(
                    prefs = currentPrefs,
                    onUpdatePrefs = { updater -> currentPrefs = updater(currentPrefs) },
                    onSelectAccent = { selectedAccent = it },
                    onToast = {},
                    onNavigate = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_tab_appearance").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("theme_mode_light").assertIsDisplayed().performClick()
        assertEquals(ThemeMode.LIGHT, currentPrefs.themeMode)

        composeRule.onNodeWithTag("accent_swatch_emerald").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(0xFF2ED573L, selectedAccent)
    }

    @Test
    fun quickModals_selectGeminiRouteAndToggleWebGrounding() {
        var chosenRoute: ProviderRoute? = null
        var webToggled = false
        var navigatedTool = ""

        composeRule.setContent {
            ZaytharTheme {
                ModelSelectorSheet(
                    currentRoute = ProviderRoute.AUTO,
                    onSelectRoute = { chosenRoute = it },
                    onOpenModelMatrix = {},
                    onDismiss = {}
                )
                ToolsHubSheet(
                    webConnectEnabled = false,
                    onToggleWebConnect = { webToggled = true },
                    onNavigate = { navigatedTool = it },
                    onDismiss = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("sheet_model_${ProviderRoute.FLASH.id}").assertIsDisplayed().performClick()
        assertEquals(ProviderRoute.FLASH, chosenRoute)

        composeRule.onNodeWithTag("tool_sheet_web_toggle").assertIsDisplayed().performClick()
        assertTrue(webToggled)

        composeRule.onNodeWithTag("tool_sheet_code").assertIsDisplayed().performClick()
        assertEquals("code", navigatedTool)
    }

    @Test
    fun toolsHubSheet_triggersAllSixToolsAndWebToggle() {
        val navigatedTools = mutableListOf<String>()
        var webToggled = false
        var dismissed = false

        composeRule.setContent {
            ZaytharTheme {
                ToolsHubSheet(
                    webConnectEnabled = true,
                    onToggleWebConnect = { webToggled = true },
                    onNavigate = { navigatedTools += it },
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeRule.waitForIdle()

        // 1. Web grounding toggle
        composeRule.onNodeWithTag("tool_sheet_web_toggle").assertIsDisplayed().performClick()
        assertTrue(webToggled)

        // 2. Studio tool
        composeRule.onNodeWithTag("tool_sheet_studio").assertIsDisplayed().performClick()
        assertTrue("studio" in navigatedTools)

        // 3. Code tool
        composeRule.onNodeWithTag("tool_sheet_code").assertIsDisplayed().performClick()
        assertTrue("code" in navigatedTools)

        // 4. Vision tool
        composeRule.onNodeWithTag("tool_sheet_vision").assertIsDisplayed().performClick()
        assertTrue("camera_eyes" in navigatedTools)

        // 5. Brain tool
        composeRule.onNodeWithTag("tool_sheet_brain").assertIsDisplayed().performClick()
        assertTrue("brain" in navigatedTools)

        // 6. Plugins tool
        composeRule.onNodeWithTag("tool_sheet_plugins").assertIsDisplayed().performClick()
        assertTrue("plugins" in navigatedTools)

        // 7. Prompts tool
        composeRule.onNodeWithTag("tool_sheet_prompts").assertIsDisplayed().performClick()
        assertTrue("prompts" in navigatedTools)
    }

    @Test
    fun modelSelectorSheet_selectsAllProviderRoutesAndOpensModelMatrix() {
        val chosenRoutes = mutableListOf<ProviderRoute>()
        var modelMatrixOpened = false

        composeRule.setContent {
            ZaytharTheme {
                ModelSelectorSheet(
                    currentRoute = ProviderRoute.AUTO,
                    onSelectRoute = { chosenRoutes += it },
                    onOpenModelMatrix = { modelMatrixOpened = true },
                    onDismiss = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Test each provider route in ModelSelectorSheet
        val expectedRoutes = ProviderRoute.entries

        for (route in expectedRoutes) {
            composeRule.onNodeWithTag("sheet_model_${route.id}").performScrollTo().assertIsDisplayed().performClick()
            composeRule.waitForIdle()
        }
        assertEquals(expectedRoutes, chosenRoutes)

        // Compare models button
        composeRule.onNodeWithTag("compare_models_sheet_button").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(modelMatrixOpened)
    }

    @Test
    fun cyberTopBar_triggersAllActionsAndToggles() {
        var menuClicked = false
        var motionToggled = false
        var themeToggled = false
        var searchOpened = false
        var toolsOpened = false

        composeRule.setContent {
            ZaytharTheme {
                com.example.ui.components.CyberTopBar(
                    titlePrefix = "TEST",
                    titleAccent = "COMMAND",
                    clockText = "12:00:00 UTC",
                    motionEnabled = true,
                    firebaseReady = true,
                    showMenuButton = true,
                    onMenuClick = { menuClicked = true },
                    onToggleMotion = { motionToggled = true },
                    onToggleTheme = { themeToggled = true },
                    themeMode = ThemeMode.DARK,
                    onOpenSearch = { searchOpened = true },
                    onOpenTools = { toolsOpened = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_menu_button").assertIsDisplayed().performClick()
        assertTrue(menuClicked)

        composeRule.onNodeWithTag("topbar_search_button").assertIsDisplayed().performClick()
        assertTrue(searchOpened)

        composeRule.onNodeWithTag("topbar_tools_button").assertIsDisplayed().performClick()
        assertTrue(toolsOpened)

        composeRule.onNodeWithTag("motion_toggle_button").assertIsDisplayed().performClick()
        assertTrue(motionToggled)

        composeRule.onNodeWithTag("theme_toggle_button").assertIsDisplayed().performClick()
        assertTrue(themeToggled)
    }
}
