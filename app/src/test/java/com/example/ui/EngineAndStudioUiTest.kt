package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.local.ZaytharPreferences
import com.example.domain.GeneratedImageResult
import com.example.domain.ProviderRoute
import com.example.domain.SmartEngineRouter
import com.example.domain.StudioAspectRatio
import com.example.domain.UiState
import com.example.ui.screens.EngineHubScreen
import com.example.ui.screens.ModelMatrixScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.ZaytharTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1600dp")
class EngineAndStudioUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun engineHubScreen_classifiesPromptAndDisplaysDiagnosticsReport() {
        var ranDiagnostics = false
        var selectedRoute: ProviderRoute? = null
        var launchedPrompt = ""

        val sampleReport = SmartEngineRouter.runLocalAndHybridDiagnostics(
            memoriesCount = 2,
            factsCount = 3,
            activePluginsCount = 7,
            firebaseReady = true,
            isGuestMode = false,
            liveProbeResult = Result.success("gemini-3.8-flash" to 95L)
        )

        composeRule.setContent {
            ZaytharTheme {
                EngineHubScreen(
                    prefs = ZaytharPreferences(),
                    diagnosticsState = UiState.Success(sampleReport),
                    activePluginCount = 7,
                    onRunDiagnostics = { ranDiagnostics = true },
                    onSelectRouteAndChat = { route, prompt ->
                        selectedRoute = route
                        launchedPrompt = prompt
                    },
                    onNavigate = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("smart_router_prompt_input")
            .performScrollTo()
            .performTextInput("Generate image of orbital core")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("DETECTED: NANO BANANA IMAGE STUDIO (94%)")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("smart_chat_launch_button").performScrollTo().performClick()
        assertEquals(ProviderRoute.AUTO, selectedRoute)
        assertTrue(launchedPrompt.contains("Generate image"))

        composeRule.onNodeWithTag("run_all_engines_button").performScrollTo().performClick()
        assertTrue(ranDiagnostics)
        composeRule.onNodeWithTag("engine_report_summary").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun studioScreen_aiImageEngineMode_rendersBitmapAndGeneratesWithRatio() {
        var requestedPrompt = ""
        var requestedStyle = ""
        var requestedRatio: StudioAspectRatio? = null
        val sampleBmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)

        composeRule.setContent {
            ZaytharTheme {
                StudioScreen(
                    briefState = UiState.Idle,
                    onGenerateBrief = { _, _, _ -> },
                    imageState = UiState.Success(
                        GeneratedImageResult(
                            bitmap = sampleBmp,
                            prompt = "Cyber command bridge",
                            modelUsed = "gemini-nano-banana-2.1",
                            aspectRatio = StudioAspectRatio.LANDSCAPE_16_9
                        )
                    ),
                    onGenerateImage = { p, style, ratio, _ ->
                        requestedPrompt = p
                        requestedStyle = style
                        requestedRatio = ratio
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("studio_generated_image").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("save_generated_image_button").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("studio_ratio_16:9").performScrollTo().performClick()
        composeRule.onNodeWithTag("studio_image_prompt_input").performScrollTo().performTextInput("Neon orbital nexus")
        composeRule.onNodeWithTag("generate_image_button").performScrollTo().performClick()

        assertEquals("Neon orbital nexus", requestedPrompt)
        assertEquals("Cinematic sci-fi", requestedStyle)
        assertEquals(StudioAspectRatio.LANDSCAPE_16_9, requestedRatio)
    }

    @Test
    fun modelMatrix_supportsAutoAndArgonRoutes() {
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

        composeRule.onNodeWithTag("model_route_auto").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(ProviderRoute.AUTO, pickedRoute)

        composeRule.onNodeWithTag("model_route_argon").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(ProviderRoute.ARGON, pickedRoute)
    }

    @Test
    fun overviewScreen_navigatesToEngineHubFromHeroButton() {
        var navigatedTo = ""

        composeRule.setContent {
            ZaytharTheme {
                OverviewScreen(
                    prefs = ZaytharPreferences(motion3dEnabled = false),
                    firebaseReady = true,
                    uptimeSeconds = 45L,
                    activePluginCount = 7,
                    onNavigate = { navigatedTo = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("open_engine_hub_hero_button").performScrollTo().performClick()
        assertEquals("engine_hub", navigatedTo)
    }
}
