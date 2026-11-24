package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreSessionMessage
import com.example.domain.AgentRole
import com.example.domain.ProviderRoute
import com.example.domain.SmartEngineRouter
import com.example.domain.UiState
import com.example.domain.ZaytharEngineId
import com.example.domain.ZaytharSystemPrompts
import com.example.ui.screens.AssistantChatCard
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
class RealAiCapabilitiesTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun systemPrompts_detectsRomanUrduAndBuildsEmpatheticPrompt() {
        assertTrue(ZaytharSystemPrompts.looksLikeRomanUrduOrHinglish("Mera mood off hai, thoda guide karo yaar"))
        assertFalse(ZaytharSystemPrompts.looksLikeRomanUrduOrHinglish("Explain Kotlin coroutines cleanly"))

        val prompt = ZaytharSystemPrompts.buildChatSystemPrompt(
            userMessage = "Mera mood off hai, guide karo",
            agent = AgentRole.GENERAL,
            prefs = ZaytharPreferences(autoMirrorLanguage = true, empathyRatio = 95f),
            planHint = "Guide empathetically",
            webEnabled = true,
            mapsGroundingEnabled = false,
            activePlugins = emptyList(),
            savedRagContext = "",
            explicitContextBlock = "",
            hasAttachedImage = true
        )
        assertTrue(prompt.contains("Roman Urdu"))
        assertTrue(prompt.contains("EMOTIONAL INTELLIGENCE"))
        assertTrue(prompt.contains("MULTIMODAL VISION"))
    }

    @Test
    fun stripMarkdownForSpeech_removesMarkdownAndLinks() {
        val raw = "### Step 1\nUse **Kotlin** with [Firebase](https://firebase.google.com) and ```val x = 1```."
        val clean = ZaytharSystemPrompts.stripMarkdownForSpeech(raw)
        assertFalse(clean.contains("###"))
        assertFalse(clean.contains("**"))
        assertFalse(clean.contains("https://"))
        assertTrue(clean.contains("Step 1"))
        assertTrue(clean.contains("Kotlin"))
        assertTrue(clean.contains("Firebase"))
    }

    @Test
    fun smartRouter_routesEmotionalGuidanceAndProvidesResilientFallbacks() {
        val decision = SmartEngineRouter.classifyPrompt("Mera mood off hai, guide karo")
        assertEquals(ZaytharEngineId.NEURAL_CHAT, decision.engine)
        assertEquals(ProviderRoute.FLASH, decision.recommendedRoute)

        val flashChain = ZaytharSystemPrompts.candidateModelsForChat(ProviderRoute.FLASH, "hi", emptySet())
        assertTrue(flashChain.contains("gemini-3.8-flash"))
        assertTrue(flashChain.contains("gemini-3.5-flash-lite"))
    }

    @Test
    fun assistantChatCard_togglesThinkingDisplaysTraceSpeaksAndClearsPhoto() {
        var thinkingToggled: Boolean? = null
        var spokenText = ""
        var clearedImage = false
        val previewBitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val msg = FirestoreSessionMessage(
            id = "m_think_1",
            role = "assistant",
            text = "I hear you. Let's take this step by step.",
            agent = "general",
            modelLabel = "gemini-3.8-flash"
        )

        composeRule.setContent {
            ZaytharTheme {
                AssistantChatCard(
                    sessionTitle = "Primary Nexus",
                    activePluginCount = 7,
                    messagesState = UiState.Success(listOf(msg)),
                    chatUiState = UiState.Streaming(
                        partialText = "Checking options…",
                        partialThoughts = "Analyzing user feeling and building step-by-step advice."
                    ),
                    listState = rememberLazyListState(),
                    composerText = "",
                    activeAgent = AgentRole.GENERAL,
                    prefs = ZaytharPreferences(thinkingEnabled = true),
                    isBusy = true,
                    isListening = false,
                    langMenuExpanded = false,
                    agentMenuExpanded = false,
                    onLangMenuToggle = {},
                    onAgentMenuToggle = {},
                    onComposerChange = {},
                    onSend = {},
                    onClearChat = {},
                    onOpenHistory = {},
                    onNewSession = {},
                    onOpenPlugins = {},
                    onOpenCameraEyes = {},
                    onToggleWebConnect = {},
                    onSelectAgent = {},
                    onSelectLanguage = {},
                    onToggleSpeakReplies = {},
                    onMicClick = {},
                    thoughtTraces = mapOf("m_think_1" to "Step 1: Acknowledge emotion. Step 2: Provide clarity."),
                    attachedImage = previewBitmap,
                    onAttachImage = { if (it == null) clearedImage = true },
                    onToggleThinking = { thinkingToggled = it },
                    onSpeakMessage = { spokenText = it },
                    onToast = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("thinking_toggle_chip").assertIsDisplayed().performClick()
        assertEquals(false, thinkingToggled)

        composeRule.onNodeWithTag("thinking_trace_toggle_m_think_1").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Step 1: Acknowledge emotion. Step 2: Provide clarity.").assertIsDisplayed()

        composeRule.onNodeWithTag("speak_message_m_think_1").assertIsDisplayed().performClick()
        assertEquals("I hear you. Let's take this step by step.", spokenText)

        composeRule.onNodeWithTag("attached_chat_image_preview").assertIsDisplayed()
        composeRule.onNodeWithTag("clear_attached_image_button").assertIsDisplayed().performClick()
        assertTrue(clearedImage)

        composeRule.onNodeWithTag("thinking_trace_body_streaming").assertIsDisplayed()
    }
}
