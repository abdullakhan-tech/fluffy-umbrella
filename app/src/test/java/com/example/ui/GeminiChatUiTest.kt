package com.example.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreChatSession
import com.example.data.remote.FirestoreSessionMessage
import com.example.domain.AgentRole
import com.example.domain.ChatMessage
import com.example.domain.MessageAuthor
import com.example.domain.UiState
import com.example.domain.toChatMessage
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.GeminiChatView
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ZaytharTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.ZoneOffset
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class GeminiChatUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chatMessageModel_mapsFirestoreMessageAndFormatsUtcTime() {
        val epochMillis = 1_700_000_000_000L // 2023-11-14T22:13:20Z
        val userDoc = FirestoreSessionMessage(
            id = "m1",
            role = "user",
            text = "Hello Gemini",
            createdAt = Timestamp(Date(epochMillis))
        )
        val modelDoc = FirestoreSessionMessage(
            id = "m2",
            role = "assistant",
            text = "Hi Operator!",
            modelLabel = "gemini-3.8-flash"
        )

        val mappedUser = userDoc.toChatMessage()
        val mappedModel = modelDoc.toChatMessage()

        assertTrue(mappedUser.isFromUser)
        assertEquals(MessageAuthor.USER, mappedUser.author)
        assertEquals("22:13", mappedUser.formattedTime(ZoneOffset.UTC))

        assertFalse(mappedModel.isFromUser)
        assertEquals(MessageAuthor.GEMINI, mappedModel.author)
        assertEquals("gemini-3.8-flash", mappedModel.modelLabel)
        assertEquals("", mappedModel.formattedTime(ZoneOffset.UTC))
    }

    @Test
    fun geminiChatView_showsEmptyStateAndDisablesSendForBlankInput() {
        composeRule.setContent {
            ZaytharTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    GeminiChatView(
                        messages = emptyList(),
                        inputText = "",
                        onInputChange = {},
                        onSendMessage = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("gemini_chat_empty_state").assertIsDisplayed()
        composeRule.onNodeWithTag("send_chat_button").assertIsNotEnabled()
    }

    @Test
    fun geminiChatView_displaysHistoryInLazyColumnAndSendsNewMessage() {
        val history = listOf(
            ChatMessage(id = "u1", text = "Summarize Kotlin coroutines", isFromUser = true, timestampMillis = 0L),
            ChatMessage(
                id = "a1",
                text = "Coroutines provide structured concurrency on JVM and Android.",
                isFromUser = false,
                timestampMillis = 0L,
                modelLabel = "gemini-3.8-flash"
            )
        )
        var draft by mutableStateOf("")
        var sentMessage: String? = null

        composeRule.setContent {
            ZaytharTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    GeminiChatView(
                        messages = history,
                        inputText = draft,
                        onInputChange = { draft = it },
                        onSendMessage = { sentMessage = it }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("gemini_chat_lazy_column").assertIsDisplayed()
        composeRule.onNodeWithText("Summarize Kotlin coroutines").assertIsDisplayed()
        composeRule.onNodeWithText("Coroutines provide structured concurrency on JVM and Android.").assertIsDisplayed()

        composeRule.onNodeWithTag("chat_prompt_input").performTextInput("Show an example with Flow")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("send_chat_button").assertIsEnabled().performClick()
        assertEquals("Show an example with Flow", sentMessage)
    }

    @Test
    fun geminiChatView_showsStreamingReplyAndDisablesSendWhileGenerating() {
        val history = listOf(
            ChatMessage(id = "u1", text = "Explain App Check", isFromUser = true, timestampMillis = 0L)
        )

        composeRule.setContent {
            ZaytharTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    GeminiChatView(
                        messages = history,
                        inputText = "Follow up",
                        onInputChange = {},
                        onSendMessage = {},
                        isGenerating = true,
                        streamingReply = "App Check attests requests..."
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("streaming_chat_bubble").assertIsDisplayed()
        composeRule.onNodeWithText("App Check attests requests...").assertIsDisplayed()
        composeRule.onNodeWithTag("send_chat_button").assertIsNotEnabled()
    }

    @Test
    fun assistantScreen_opensThreadSheetAndSwitchesBetweenChatThreads() {
        val threadOne = FirestoreChatSession(
            id = "thread_1",
            title = "Kernel Architecture",
            agent = "coder",
            lastMessagePreview = "Refactor repository boundaries",
            messageCount = 1L
        )
        val threadTwo = FirestoreChatSession(
            id = "thread_2",
            title = "Mission Telemetry",
            agent = "researcher",
            lastMessagePreview = "Check orbital window",
            messageCount = 1L
        )
        var activeId by mutableStateOf("thread_1")

        composeRule.setContent {
            ZaytharTheme {
                val activeMessages = if (activeId == "thread_1") {
                    listOf(FirestoreSessionMessage(id = "m1", sessionId = "thread_1", role = "user", text = "Refactor repository boundaries"))
                } else {
                    listOf(FirestoreSessionMessage(id = "m2", sessionId = "thread_2", role = "assistant", text = "Orbital telemetry nominal"))
                }
                AssistantScreen(
                    prefs = ZaytharPreferences(motion3dEnabled = false),
                    activeAgent = AgentRole.GENERAL,
                    composerText = "",
                    sessionsState = UiState.Success(listOf(threadOne, threadTwo)),
                    activeSessionId = activeId,
                    activePluginCount = 3,
                    messagesState = UiState.Success(activeMessages),
                    chatUiState = UiState.Idle,
                    onComposerChange = {},
                    onSend = {},
                    onClearChat = {},
                    onSelectSession = { activeId = it },
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

        composeRule.onNodeWithText("Refactor repository boundaries").assertIsDisplayed()
        composeRule.onNodeWithTag("switch_thread_chip").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("chat_history_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("session_item_thread_2").performClick()
        composeRule.waitForIdle()

        assertEquals("thread_2", activeId)
        composeRule.onNodeWithText("Orbital telemetry nominal").assertIsDisplayed()
    }

    @Test
    fun themeMode_resolvesSystemLightDarkAndCyclesCleanly() {
        assertTrue(ThemeMode.SYSTEM.resolveIsDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.resolveIsDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.resolveIsDark(systemDark = true))
        assertFalse(ThemeMode.LIGHT.resolveIsDark(systemDark = false))
        assertTrue(ThemeMode.DARK.resolveIsDark(systemDark = true))
        assertTrue(ThemeMode.DARK.resolveIsDark(systemDark = false))

        assertEquals(ThemeMode.LIGHT, ThemeMode.SYSTEM.next())
        assertEquals(ThemeMode.DARK, ThemeMode.LIGHT.next())
        assertEquals(ThemeMode.SYSTEM, ThemeMode.DARK.next())
    }

    @Test
    fun assistantScreen_chatThemeChip_switchesLightDarkAndSystemDefault() {
        var currentMode by mutableStateOf(ThemeMode.SYSTEM)
        var resolvedIsDark = false

        composeRule.setContent {
            ZaytharTheme(themeMode = currentMode) {
                resolvedIsDark = LocalCyberColors.current.isDark
                AssistantScreen(
                    prefs = ZaytharPreferences(themeMode = currentMode, motion3dEnabled = false),
                    activeAgent = AgentRole.GENERAL,
                    composerText = "",
                    sessionsState = UiState.Success(emptyList()),
                    activeSessionId = "",
                    activePluginCount = 0,
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
                    onToggleWebConnect = {},
                    onSelectAgent = {},
                    onSelectLanguage = {},
                    onToggleSpeakReplies = {},
                    onSelectThemeMode = { currentMode = it },
                    onToast = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("chat_theme_chip").performClick()
        composeRule.onNodeWithTag("chat_theme_option_light").performClick()
        composeRule.waitForIdle()
        assertEquals(ThemeMode.LIGHT, currentMode)
        assertFalse(resolvedIsDark)

        composeRule.onNodeWithTag("chat_theme_chip").performClick()
        composeRule.onNodeWithTag("chat_theme_option_dark").performClick()
        composeRule.waitForIdle()
        assertEquals(ThemeMode.DARK, currentMode)
        assertTrue(resolvedIsDark)

        composeRule.onNodeWithTag("chat_theme_chip").performClick()
        composeRule.onNodeWithTag("chat_theme_option_system").performClick()
        composeRule.waitForIdle()
        assertEquals(ThemeMode.SYSTEM, currentMode)
    }

    @Test
    fun geminiChatView_copyAndShareButtons_copySpecificMessageAndLaunchShareIntent() {
        val history = listOf(
            ChatMessage(id = "msg_u1", text = "First user question", isFromUser = true, timestampMillis = 0L),
            ChatMessage(id = "msg_a2", text = "Specific Gemini insight to share", isFromUser = false, timestampMillis = 0L)
        )
        var lastToast = ""

        composeRule.setContent {
            ZaytharTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    GeminiChatView(
                        messages = history,
                        inputText = "",
                        onInputChange = {},
                        onSendMessage = {},
                        onToast = { lastToast = it }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Copy the specific assistant message to system clipboard
        composeRule.onNodeWithTag("copy_message_msg_a2").performClick()
        composeRule.waitForIdle()

        val app = ApplicationProvider.getApplicationContext<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("Specific Gemini insight to share", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        assertEquals("Message copied to clipboard.", lastToast)

        // Share the specific assistant message via Android system chooser intent
        composeRule.onNodeWithTag("share_message_msg_a2").performClick()
        composeRule.waitForIdle()

        val startedIntent = shadowOf(app).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)
        @Suppress("DEPRECATION")
        val targetIntent = startedIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(targetIntent)
        assertEquals(Intent.ACTION_SEND, targetIntent?.action)
        assertEquals("text/plain", targetIntent?.type)
        assertEquals("Specific Gemini insight to share", targetIntent?.getStringExtra(Intent.EXTRA_TEXT))
    }
}
