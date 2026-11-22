package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.NeuralCoreVisualizer
import com.example.ui.components.OrbState
import com.example.ui.theme.ZaytharTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1600dp")
class NeuralOrbVisualizerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun orbStateEnum_containsExpectedStateDefinitions() {
        val states = OrbState.entries
        assertEquals(5, states.size)
        assertNotNull(OrbState.valueOf("IDLE"))
        assertNotNull(OrbState.valueOf("LISTENING"))
        assertNotNull(OrbState.valueOf("PROCESSING"))
        assertNotNull(OrbState.valueOf("SPEAKING"))
        assertNotNull(OrbState.valueOf("ERROR"))
    }

    @Test
    fun neuralCoreVisualizer_rendersIdleStateWithReducedMotion() {
        composeRule.setContent {
            ZaytharTheme {
                NeuralCoreVisualizer(
                    size = 180.dp,
                    motionEnabled = false,
                    coreLabel = "AI",
                    orbState = OrbState.IDLE
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("neural_core_visualizer").assertIsDisplayed()
    }

    @Test
    fun neuralCoreVisualizer_rendersProcessingAndCustomGlow() {
        composeRule.setContent {
            ZaytharTheme {
                NeuralCoreVisualizer(
                    size = 200.dp,
                    motionEnabled = true,
                    coreLabel = "Z",
                    orbState = OrbState.PROCESSING,
                    customGlowColor = Color(0xFF9D00FF)
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("neural_core_visualizer").assertIsDisplayed()
    }

    @Test
    fun neuralCoreVisualizer_rendersListeningAndSpeakingWithAudioLevel() {
        composeRule.setContent {
            ZaytharTheme {
                NeuralCoreVisualizer(
                    size = 150.dp,
                    motionEnabled = true,
                    coreLabel = "VOX",
                    orbState = OrbState.LISTENING,
                    audioLevel = 0.85f
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("neural_core_visualizer").assertIsDisplayed()
    }

    @Test
    fun neuralCoreVisualizer_rendersErrorState() {
        composeRule.setContent {
            ZaytharTheme {
                NeuralCoreVisualizer(
                    size = 120.dp,
                    motionEnabled = false,
                    coreLabel = "ERR",
                    orbState = OrbState.ERROR
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("neural_core_visualizer").assertIsDisplayed()
    }
}
