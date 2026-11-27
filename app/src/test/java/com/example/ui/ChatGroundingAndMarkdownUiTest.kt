package com.example.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreSessionMessage
import com.example.domain.AgentRole
import com.example.domain.ChatMessage
import com.example.domain.GroundedWebSource
import com.example.domain.UiState
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.GeminiChatView
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ZaytharTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class ChatGroundingAndMarkdownUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun geminiChatView_rendersAiMarkdownCodeBlocksBoldTextAndLists() {
        val markdownPayload = """
            ### Architecture Overview

            Use **Zero-Trust Sync** with `users/{uid}` scoping.

            - Validate UID ownership
            - Enforce rules in tests

            1. Initialize App Check
            2. Query Firestore

            ```kotlin
            fun greet() = "Zaythar"
            ```
        """.trimIndent()

        val history = listOf(
            ChatMessage(id = "u_md", text = "Show **raw** markdown", isFromUser = true, timestampMillis = 0L),
            ChatMessage(id = "a_md", text = markdownPayload, isFromUser = false, timestampMillis = 0L)
        )
        var themeMode by mutableStateOf(ThemeMode.DARK)

        composeRule.setContent {
            ZaytharTheme(themeMode = themeMode) {
                Column(modifier = Modifier.fillMaxSize()) {
                    GeminiChatView(
                        messages = history,
                        inputText = "",
                        onInputChange = {},
                        onSendMessage = {},
                        listState = rememberLazyListState()
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Assert the actual Markdown host node and its complete accessible text summary.
        composeRule.onNodeWithTag("chat_message_a_md").performScrollTo().assertIsDisplayed()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Architecture Overview", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Zero-Trust Sync", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Validate UID ownership", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Initialize App Check", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("fun greet() = \"Zaythar\"", substring = true).assertExists()
        composeRule.onNodeWithText("Show **raw** markdown").assertExists()

        themeMode = ThemeMode.LIGHT
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Zero-Trust Sync", substring = true).assertExists()

        composeRule.onNodeWithTag("copy_message_a_md").performScrollTo().performClick()
        composeRule.waitForIdle()
        val app = ApplicationProvider.getApplicationContext<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals(markdownPayload, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun geminiChatView_displaysGoogleSearchGroundingQueriesSourcesAndLiveStreaming() {
        val groundedUrl = "https://vertexaisearch.cloud.google.com/grounding-api-redirect/demo-token"
        val history = listOf(
            ChatMessage(
                id = "a_grounded",
                text = "Android 16 introduces updated adaptive layout APIs.",
                isFromUser = false,
                timestampMillis = 0L,
                modelLabel = "gemini-3.8-flash+search",
                webSources = listOf(groundedUrl),
                webSourceTitles = listOf("developer.android.com"),
                searchQueries = listOf("android 16 adaptive layout features"),
                searchEntryPointHtml = "<div><a href=\"https://www.google.com/search?q=android+16\">Android 16</a></div>"
            )
        )

        composeRule.setContent {
            ZaytharTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    GeminiChatView(
                        messages = history,
                        inputText = "",
                        onInputChange = {},
                        onSendMessage = {},
                        isGenerating = true,
                        streamingReply = "Fetching latest release notes…",
                        streamingSearchQueries = listOf("gemini 3.8 flash release notes"),
                        streamingWebSources = listOf(
                            GroundedWebSource("firebase.google.com", "https://firebase.google.com/docs/ai-logic")
                        )
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("GOOGLE SEARCH GROUNDED").assertIsDisplayed()
        composeRule.onNodeWithTag("grounding_query_chip_0").assertIsDisplayed()
        composeRule.onNodeWithText("android 16 adaptive layout features").assertIsDisplayed()
        composeRule.onNodeWithTag("grounding_source_chip_0").assertIsDisplayed()
        composeRule.onNodeWithText("developer.android.com").assertIsDisplayed()
        composeRule.onNodeWithTag("google_search_entry_point").assertIsDisplayed()
        composeRule.onNodeWithTag("streaming_grounding_indicator").assertIsDisplayed()

        composeRule.onNodeWithTag("grounding_source_chip_0").performClick()
        composeRule.waitForIdle()
        val app = ApplicationProvider.getApplicationContext<Application>()
        val started = shadowOf(app).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(groundedUrl, started.dataString)
    }

    @Test
    fun assistantScreen_searchGroundingToggle_updatesSearchState() {
        var searchEnabled by mutableStateOf(true)

        composeRule.setContent {
            ZaytharTheme {
                AssistantScreen(
                    prefs = ZaytharPreferences(webConnectEnabled = searchEnabled, motion3dEnabled = false),
                    activeAgent = AgentRole.GENERAL,
                    composerText = "",
                    sessionsState = UiState.Success(emptyList()),
                    activeSessionId = "",
                    activePluginCount = 1,
                    messagesState = UiState.Success(emptyList()),
                    chatUiState = UiState.Idle,
                    onComposerChange = {},
                    onSend = {},
                    onClearChat = {},
                    onSelectSession = {},
                    onCreateSession = {},
                    onRenameSession = { _, _ -> },
                    onDeleteSession = {},
                    onOpenPlugins = {},
                    onOpenCameraEyes = {},
                    onToggleWebConnect = { searchEnabled = it },
                    onSelectAgent = {},
                    onSelectLanguage = {},
                    onToggleSpeakReplies = {},
                    onToast = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("SEARCH: ON").assertIsDisplayed()
        composeRule.onNodeWithTag("web_connect_toggle").performClick()
        composeRule.waitForIdle()

        assertFalse(searchEnabled)
        composeRule.onNodeWithText("SEARCH: OFF").assertIsDisplayed()
    }

    @Test
    fun assistantScreen_clearAndNewButton_confirmsAndStartsNewSession() {
        var clearedCount = 0

        composeRule.setContent {
            ZaytharTheme {
                AssistantScreen(
                    prefs = ZaytharPreferences(motion3dEnabled = false),
                    activeAgent = AgentRole.GENERAL,
                    composerText = "",
                    sessionsState = UiState.Success(emptyList()),
                    activeSessionId = "s_active",
                    activePluginCount = 0,
                    messagesState = UiState.Success(
                        listOf(
                            FirestoreSessionMessage(
                                id = "m1",
                                sessionId = "s_active",
                                role = "user",
                                text = "Current chat message"
                            )
                        )
                    ),
                    chatUiState = UiState.Idle,
                    onComposerChange = {},
                    onSend = {},
                    onClearChat = {},
                    onClearAndNewSession = { clearedCount++ },
                    onSelectSession = {},
                    onCreateSession = {},
                    onRenameSession = { _, _ -> },
                    onDeleteSession = {},
                    onOpenPlugins = {},
                    onOpenCameraEyes = {},
                    onToggleWebConnect = {},
                    onSelectAgent = {},
                    onSelectLanguage = {},
                    onToggleSpeakReplies = {},
                    onToast = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Tapping Clear & New opens confirmation dialog; Cancel does not clear
        composeRule.onNodeWithTag("clear_new_session_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("clear_session_confirm_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("cancel_clear_new_session_button").performClick()
        composeRule.waitForIdle()
        assertEquals(0, clearedCount)

        // Confirming triggers clearing Firestore history and starting a new session
        composeRule.onNodeWithTag("clear_new_session_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_clear_new_session_button").performClick()
        composeRule.waitForIdle()
        assertEquals(1, clearedCount)
    }

        @Test
    fun geminiChatView_markdownNodesVisibilityAndComposeScrolling_regressionTest() {
        val longMarkdownContent = """
            # Header Level 1
            
            This is a paragraph with **bold text**, *italic text*, and `inline code`.
            
            ## Architecture Details
            
            Here are key requirements:
            - Client-side input validation
            - Realtime stream handling
            - Zero-Trust security rules
            
            ### Code Snippet
            
            ```kotlin
            fun calculateChecksum(input: String): Long {
                return input.fold(0L) { acc, char -> acc * 31 + char.code }
            }
            ```
            
            > Caution: Always verify authentication tokens before processing sensitive payload.
            
            1. First step in initialization
            2. Second step in verification
        """.trimIndent()

        val messages = listOf(
            ChatMessage(
                id = "msg_user_prompt",
                text = "Explain the system architecture and implementation details with code examples.",
                isFromUser = true,
                timestampMillis = 1000L
            ),
            ChatMessage(
                id = "msg_ai_markdown",
                text = longMarkdownContent,
                isFromUser = false,
                timestampMillis = 2000L,
                modelLabel = "gemini-3.8-flash"
            ),
            ChatMessage(
                id = "msg_user_followup",
                text = "Can you summarize the security guarantees?",
                isFromUser = true,
                timestampMillis = 3000L
            ),
            ChatMessage(
                id = "msg_ai_summary",
                text = "Security is guaranteed via cryptographic App Check and Firestore security rules.",
                isFromUser = false,
                timestampMillis = 4000L,
                modelLabel = "gemini-3.8-flash"
            )
        )

        var themeMode by mutableStateOf(ThemeMode.DARK)

        composeRule.setContent {
            ZaytharTheme(themeMode = themeMode) {
                Column(modifier = Modifier.fillMaxSize()) {
                    GeminiChatView(
                        messages = messages,
                        inputText = "",
                        onInputChange = {},
                        onSendMessage = {},
                        listState = rememberLazyListState()
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // 1. Verify user prompt is in semantics tree and scrolled properly
        composeRule.onNodeWithTag("chat_message_msg_user_prompt").performScrollTo().assertExists()
        composeRule.onNodeWithText("Explain the system architecture", substring = true).assertExists()

        // 2. Scroll to markdown message host and verify all headings, lists, and code blocks exist
        composeRule.onNodeWithTag("chat_message_msg_ai_markdown").performScrollTo().assertExists()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Header Level 1", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Architecture Details", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Client-side input validation", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Realtime stream handling", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("fun calculateChecksum", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Always verify authentication tokens", substring = true).assertExists()

        // 3. Scroll to copy button, verify it exists and performs copy
        composeRule.onNodeWithTag("copy_message_msg_ai_markdown").performScrollTo().performClick()
        composeRule.waitForIdle()

        val app = ApplicationProvider.getApplicationContext<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals(longMarkdownContent, clipboard.primaryClip?.getItemAt(0)?.text?.toString())

        // 4. Scroll down to follow-up messages
        composeRule.onNodeWithTag("chat_message_msg_user_followup").performScrollTo().assertExists()
        composeRule.onNodeWithTag("chat_message_msg_ai_summary").performScrollTo().assertExists()
        composeRule.onNodeWithText("Security is guaranteed via cryptographic App Check", substring = true).assertExists()

        // 5. Test theme change during scrolled position without dropping markdown nodes
        themeMode = ThemeMode.LIGHT
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_message_msg_ai_markdown").performScrollTo().assertExists()
        composeRule.onNodeWithContentDescription("Architecture Details", substring = true).assertExists()
    }
}
