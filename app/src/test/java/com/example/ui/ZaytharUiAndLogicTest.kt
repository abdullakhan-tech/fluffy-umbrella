package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreKnowledgeFact
import com.example.data.remote.FirestoreMemoryNote
import com.example.data.remote.FirestorePluginConfig
import com.example.domain.AiRepository
import com.example.domain.CodeCopilotMode
import com.example.domain.CodeLanguage
import com.example.domain.NeuralBrainAnalysis
import com.example.domain.NeuralBrainMode
import com.example.domain.SuggestedTriple
import com.example.domain.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.CameraEyesScreen
import com.example.ui.screens.CodeCopilotScreen
import com.example.ui.screens.KnowledgeBaseScreen
import com.example.ui.screens.NeuralBrainScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.PersonaBuilderScreen
import com.example.ui.screens.PluginHubScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PromptNexusScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ZaytharTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1600dp")
class ZaytharUiAndLogicTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun webGroundingActive_requiresBothToggleAndPluginEnabled() {
        assertTrue(AiRepository.isWebGroundingActive(true, setOf("web_grounding", "calculator")))
        assertFalse(AiRepository.isWebGroundingActive(false, setOf("web_grounding", "calculator")))
        assertFalse(AiRepository.isWebGroundingActive(true, setOf("calculator")))
        assertFalse(AiRepository.isWebGroundingActive(false, emptySet()))
    }

    @Test
    fun overviewScreen_displaysTelemetryAndNavigatesOnModuleClick() {
        var navigatedRoute: String? = null
        composeRule.setContent {
            ZaytharTheme {
                OverviewScreen(
                    prefs = ZaytharPreferences(webConnectEnabled = true, motion3dEnabled = false),
                    firebaseReady = true,
                    uptimeSeconds = 75L,
                    activePluginCount = 5,
                    onNavigate = { navigatedRoute = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Firebase AI Logic").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("INTERNET LIVE").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("deck_brain").performScrollTo().performClick()
        assertEquals("brain", navigatedRoute)
    }

    @Test
    fun neuralBrainScreen_runsAnalysisAndCommitsTriples() {
        var capturedPrompt = ""
        var capturedMode: NeuralBrainMode? = null
        var committedTriples: List<SuggestedTriple> = emptyList()
        var savedSynthesis = ""

        val sampleAnalysis = NeuralBrainAnalysis(
            mode = NeuralBrainMode.CONSOLIDATOR,
            prompt = "Architect zero-trust mobile sync",
            summary = "Store user notes under authenticated UID paths.",
            confidence = 94,
            reasoningSteps = listOf("Checked UID scoping against Cloud Firestore rules"),
            insights = listOf("Gate reads/writes with request.auth.uid"),
            risksAndGaps = listOf("Unbounded client writes could exceed storage quotas"),
            actionPlan = listOf("Add payload length bounds in Firestore rules"),
            ragCitations = listOf("[RAG-1 · GRAPH · 91%] Zaythar enforces UID scoping"),
            suggestedTriples = listOf(
                SuggestedTriple("Zaythar", "enforces", "UID isolation")
            ),
            modelUsed = "gemini-3.8-flash"
        )

        composeRule.setContent {
            ZaytharTheme {
                NeuralBrainScreen(
                    brainAnalysisState = UiState.Success(sampleAnalysis),
                    brainRunsState = UiState.Success(emptyList()),
                    memoriesCount = 3,
                    factsCount = 2,
                    onRunBrain = { p, m ->
                        capturedPrompt = p
                        capturedMode = m
                    },
                    onCommitTriples = { committedTriples = it },
                    onDeleteBrainRun = {},
                    onSaveSynthesisToMemory = { savedSynthesis = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("brain_mode_consolidator").performClick()
        composeRule.onNodeWithTag("brain_prompt_input").performTextInput("Analyze security model")
        composeRule.onNodeWithTag("execute_brain_button").performClick()

        assertEquals("Analyze security model", capturedPrompt)
        assertEquals(NeuralBrainMode.CONSOLIDATOR, capturedMode)

        composeRule.onNodeWithText("Store user notes under authenticated UID paths.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("COGNITIVE REASONING TRACE").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("RISKS & BLIND SPOTS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("PRIORITIZED ACTION PLAN").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("commit_triples_button").performScrollTo().performClick()
        assertEquals(1, committedTriples.size)
        assertEquals("UID isolation", committedTriples.first().objectValue)

        composeRule.onNodeWithTag("save_brain_memory_button").performScrollTo().performClick()
        assertTrue(savedSynthesis.contains("Store user notes under authenticated UID paths."))
    }

    @Test
    fun pluginHubScreen_sandboxRespectsEnabledPluginState() {
        val enabledUnitPlugin = FirestorePluginConfig(
            id = "p1",
            pluginKey = "unit_converter",
            name = "Metric & Data Converter",
            description = "Converts units",
            category = "Compute",
            enabled = true,
            isBuiltIn = true
        )
        composeRule.setContent {
            ZaytharTheme {
                PluginHubScreen(
                    pluginsState = UiState.Success(listOf(enabledUnitPlugin)),
                    onTogglePlugin = { _, _ -> },
                    onAddCustomPlugin = { _, _, _, _ -> },
                    onDeletePlugin = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("plugin_sandbox_input").performTextInput("100 km to mi")
        composeRule.onNodeWithTag("run_plugin_sandbox_button").performClick()
        composeRule.onNodeWithText("100 km = 62.1371192 mi", substring = true).assertIsDisplayed()
    }

    @Test
    fun cameraEyesScreen_disablesAnalyzeWithoutImageAndSwitchesMode() {
        composeRule.setContent {
            ZaytharTheme {
                CameraEyesScreen(
                    visionState = UiState.Idle,
                    onAnalyzeImage = { _, _, _ -> },
                    onToast = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("analyze_vision_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("vision_mode_ocr").performClick()
        composeRule.onNodeWithText("OCR & Document Reader").assertIsDisplayed()
    }

    @Test
    fun codeCopilotScreen_updatesLanguageAndRendersOutput() {
        var ranLanguage: CodeLanguage? = null
        var ranMode: CodeCopilotMode? = null
        var ranTask = ""
        var ranTests = true
        var ranSecurity = true

        composeRule.setContent {
            ZaytharTheme {
                CodeCopilotScreen(
                    codeCopilotState = UiState.Success("### Implementation\n```python\ndef verify_token(token: str) -> bool:\n    return bool(token)\n```"),
                    onRunCopilot = { task, lang, mode, _, includeTests, strictSec ->
                        ranTask = task
                        ranLanguage = lang
                        ranMode = mode
                        ranTests = includeTests
                        ranSecurity = strictSec
                    },
                    onSaveToKnowledge = {},
                    onToast = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("code_lang_py").performClick()
        composeRule.onNodeWithTag("code_mode_architect").performClick()
        composeRule.onNodeWithTag("code_toggle_unit_tests").performClick()
        composeRule.onNodeWithTag("code_task_input").performTextInput("Harden parser")
        composeRule.onNodeWithTag("execute_copilot_button").performClick()

        assertEquals(CodeLanguage.PYTHON, ranLanguage)
        assertEquals(CodeCopilotMode.ARCHITECT, ranMode)
        assertEquals("Harden parser", ranTask)
        assertFalse(ranTests)
        assertTrue(ranSecurity)
        composeRule.onNodeWithText("● ● ●   Output.py").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("code_copilot_markdown_output").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun studioAndPromptNexusScreens_filterPromptsAndTriggerBrief() {
        var generatedPrompt = ""
        var generatedOutput = ""
        var generatedStyle = ""

        composeRule.setContent {
            ZaytharTheme {
                StudioScreen(
                    briefState = UiState.Success("Shot 1: Neon command deck wide angle"),
                    onGenerateBrief = { p, out, st ->
                        generatedPrompt = p
                        generatedOutput = out
                        generatedStyle = st
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("studio_prompt_input").performScrollTo().performTextInput("Orbital command bridge")
        composeRule.onNodeWithTag("make_brief_button").performScrollTo().performClick()

        assertEquals("Orbital command bridge", generatedPrompt)
        assertEquals("Image concept", generatedOutput)
        assertEquals("Cinematic sci-fi", generatedStyle)
        composeRule.onNodeWithText("Shot 1: Neon command deck wide angle").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun promptNexusScreen_filtersTemplatesAndSelectsPrefix() {
        var selectedPrefix = ""
        composeRule.setContent {
            ZaytharTheme {
                PromptNexusScreen(
                    onSelectPrompt = { selectedPrefix = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("prompt_search_input").performTextInput("security")
        composeRule.onNodeWithTag("prompt_card_code_review").assertIsDisplayed().performClick()
        assertTrue(selectedPrefix.contains("senior software engineer"))
    }

    @Test
    fun settingsScreen_themeSelectorSwitchesLightDarkAndSystemDefault() {
        var prefs by mutableStateOf(ZaytharPreferences(themeMode = ThemeMode.SYSTEM))

        composeRule.setContent {
            ZaytharTheme(themeMode = prefs.themeMode) {
                SettingsScreen(
                    prefs = prefs,
                    onUpdatePrefs = { transform -> prefs = transform(prefs) },
                    onSelectAccent = {},
                    onToast = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("theme_mode_light").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(ThemeMode.LIGHT, prefs.themeMode)

        composeRule.onNodeWithTag("theme_mode_dark").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(ThemeMode.DARK, prefs.themeMode)

        composeRule.onNodeWithTag("theme_mode_system").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
    }

    @Test
    fun knowledgeBaseScreen_runsHybridRagSearchAndShowsRankedMatches() {
        val sampleNotes = listOf(
            FirestoreMemoryNote(id = "n1", content = "Use Kotlin StateFlow and Jetpack Compose for Zaythar UI rendering."),
            FirestoreMemoryNote(id = "n2", content = "Grocery reminder: buy coffee beans.")
        )
        val sampleFacts = listOf(
            FirestoreKnowledgeFact(id = "f1", subject = "Zaythar Core", predicate = "uses", objectValue = "BM25 Hybrid RAG", source = "brain")
        )

        composeRule.setContent {
            ZaytharTheme {
                KnowledgeBaseScreen(
                    notesState = UiState.Success(sampleNotes),
                    onAddNote = {},
                    onDeleteNote = {},
                    factsState = UiState.Success(sampleFacts)
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("rag_search_input").performScrollTo().performTextInput("BM25 Hybrid RAG")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("GRAPH · Zaythar Core").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun authScreen_continueAsGuestButtonTriggersCallback() {
        var enteredGuest = false
        composeRule.setContent {
            ZaytharTheme {
                AuthScreen(
                    motionEnabled = false,
                    onAuthSuccess = {},
                    onContinueAsGuest = { enteredGuest = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("guest_sign_in_button").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(enteredGuest)
    }

    @Test
    fun profileScreen_guestModeShowsLocalStatusAndExitAction() {
        var exitedGuest = false
        composeRule.setContent {
            ZaytharTheme {
                ProfileScreen(
                    operatorName = "GUEST OPERATOR",
                    userEmail = null,
                    userDisplayName = "Guest User",
                    onSaveOperatorName = {},
                    onSignOut = { exitedGuest = true },
                    isGuest = true
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("● GUEST MODE · LOCAL DEVICE SESSION").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("sign_out_button").performScrollTo().performClick()
        assertTrue(exitedGuest)
    }
    @Test
    fun personaBuilderScreen_switchesActivePersonaAndHonorific() {
        var prefs by mutableStateOf(ZaytharPreferences(selectedPersona = "jarvis", honorific = "Sir"))

        composeRule.setContent {
            ZaytharTheme {
                PersonaBuilderScreen(
                    prefs = prefs,
                    onUpdatePrefs = { transform -> prefs = transform(prefs) },
                    onToast = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Switch to Friday
        composeRule.onNodeWithTag("persona_select_friday").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("friday", prefs.selectedPersona)
        assertEquals("Boss", prefs.honorific)

        // Switch honorific to Chief
        composeRule.onNodeWithTag("honorific_option_chief").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("Chief", prefs.honorific)

        // Switch to Zaythar Core
        composeRule.onNodeWithTag("persona_select_zaythar").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("zaythar", prefs.selectedPersona)
    }
}