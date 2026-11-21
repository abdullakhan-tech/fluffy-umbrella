package com.example.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ZaytharPreferences
import com.example.domain.AgentRole
import com.example.domain.AudioTranscriptionResult
import com.example.domain.LiveTranscriptLine
import com.example.domain.LiveVoiceStatus
import com.example.domain.ProviderRoute
import com.example.domain.UiState
import com.example.ui.screens.AssistantChatCard
import com.example.ui.screens.ModelMatrixScreen
import com.example.ui.screens.VoiceAndAudioScreen
import com.example.ui.theme.ZaytharTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1600dp")
class VoiceAndMapsAiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun modelMatrix_supportsGemini4AndFlashLite31Routes() {
        var pickedRoute: ProviderRoute? = null

        composeRule.setContent {
            ZaytharTheme {
                ModelMatrixScreen(
                    prefs = ZaytharPreferences(selectedProviderRoute = ProviderRoute.HYBRID.id),
                    onSelectRoute = { pickedRoute = it },
                    onOpenAssistant = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("model_route_hydrogen").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(ProviderRoute.HYDROGEN, pickedRoute)

        composeRule.onNodeWithTag("model_route_carbon").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(ProviderRoute.CARBON, pickedRoute)

        composeRule.onNodeWithTag("model_route_neon").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(ProviderRoute.NEON, pickedRoute)

        composeRule.onNodeWithTag("model_route_flash_lite_31").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(ProviderRoute.FLASH_LITE_31, pickedRoute)
    }

    @Test
    fun assistantChatCard_togglesMapsGroundingAndOpensLiveVoice() {
        var mapsToggled: Boolean? = null
        var openedVoice = false
        var stoppedTts = false

        composeRule.setContent {
            ZaytharTheme {
                AssistantChatCard(
                    sessionTitle = "Primary Nexus",
                    activePluginCount = 7,
                    messagesState = UiState.Success(emptyList()),
                    chatUiState = UiState.Idle,
                    listState = rememberLazyListState(),
                    composerText = "",
                    activeAgent = AgentRole.GENERAL,
                    prefs = ZaytharPreferences(mapsGroundingEnabled = false),
                    isBusy = false,
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
                    onOpenVoiceAudio = { openedVoice = true },
                    onToggleWebConnect = {},
                    onToggleMapsGrounding = { mapsToggled = it },
                    isSpeaking = true,
                    onStopSpeaking = { stoppedTts = true },
                    onSelectAgent = {},
                    onSelectLanguage = {},
                    onToggleSpeakReplies = {},
                    onMicClick = {},
                    onToast = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("maps_grounding_toggle").assertIsDisplayed().performClick()
        assertEquals(true, mapsToggled)

        composeRule.onNodeWithTag("assistant_voice_live_chip").assertIsDisplayed().performClick()
        assertTrue(openedVoice)

        composeRule.onNodeWithTag("stop_tts_chip").assertIsDisplayed().performClick()
        assertTrue(stoppedTts)
    }

    @Test
    fun voiceAndAudioScreen_rendersLiveVoiceAndTranscriptionActions() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)

        var startedWithVoice = ""
        var savedRagNote = ""
        var sentToChat = ""

        composeRule.setContent {
            ZaytharTheme {
                VoiceAndAudioScreen(
                    liveStatus = LiveVoiceStatus.IDLE,
                    liveTranscripts = listOf(
                        LiveTranscriptLine(speaker = "YOU", text = "Summarize system telemetry"),
                        LiveTranscriptLine(speaker = "ZAYTHAR", text = "All 9 engines are standing by.")
                    ),
                    liveError = null,
                    isRecordingAudio = false,
                    recordingSeconds = 0L,
                    transcribeState = UiState.Success(
                        AudioTranscriptionResult(
                            transcript = "Meeting notes: Deploy Firebase AI Logic.",
                            modelUsed = "gemini-3.5-transcribe",
                            byteCount = 4096
                        )
                    ),
                    onStartLiveVoice = { startedWithVoice = it },
                    onStopLiveVoice = {},
                    onSendLiveText = {},
                    onClearLiveTranscripts = {},
                    onStartRecording = {},
                    onStopRecordingAndTranscribe = {},
                    onSendTranscriptToChat = { sentToChat = it },
                    onSaveTranscriptToRag = { savedRagNote = it },
                    onToast = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("live_voice_chip_kore").performScrollTo().performClick()
        composeRule.onNodeWithTag("live_voice_toggle_button").performScrollTo().performClick()
        assertEquals("Kore", startedWithVoice)

        composeRule.onNodeWithText("ZAYTHAR: All 9 engines are standing by.").assertIsDisplayed()
        composeRule.onNodeWithTag("save_live_transcript_button").performScrollTo().performClick()
        assertTrue(savedRagNote.contains("[LIVE VOICE SESSION]"))

        composeRule.onNodeWithTag("transcribe_result_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("send_transcript_chat_button").performScrollTo().performClick()
        assertEquals("Meeting notes: Deploy Firebase AI Logic.", sentToChat)
    }
}
