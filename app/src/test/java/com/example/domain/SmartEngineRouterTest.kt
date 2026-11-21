package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SmartEngineRouterTest {

    @Test
    fun classifyPrompt_routesAcrossAllCoreEnginesAndTiers() {
        val calc = SmartEngineRouter.classifyPrompt("calculate 25 * 4")
        assertEquals(ZaytharEngineId.RAG_AND_TOOLS, calc.engine)
        assertNotNull(calc.localToolPreview)
        assertEquals("100", calc.localToolPreview?.result)

        val image = SmartEngineRouter.classifyPrompt("Generate image of a holographic cyber nexus")
        assertEquals(ZaytharEngineId.IMAGE_STUDIO, image.engine)
        assertEquals(listOf("gemini-nano-banana-2.1"), image.candidateModels)

        val code = SmartEngineRouter.classifyPrompt("Refactor Kotlin StateFlow repository with unit tests")
        assertEquals(ZaytharEngineId.CODE_COPILOT, code.engine)
        assertEquals(ProviderRoute.ARGON, code.recommendedRoute)
        assertEquals(listOf("gemini-4-argon", "gemini-3.8-flash"), code.candidateModels)

        val brain = SmartEngineRouter.classifyPrompt("Synthesize knowledge graph triples and blind spots")
        assertEquals(ZaytharEngineId.AI_BRAIN, brain.engine)

        val swarm = SmartEngineRouter.classifyPrompt("Compare pros and cons across security and architecture")
        assertEquals(ZaytharEngineId.MULTI_AGENT, swarm.engine)

        val lite = SmartEngineRouter.classifyPrompt("hello zaythar")
        assertEquals(ZaytharEngineId.NEURAL_CHAT, lite.engine)
        assertEquals(ProviderRoute.LITE, lite.recommendedRoute)

        val voice = SmartEngineRouter.classifyPrompt("Start live voice conversation with Zaythar")
        assertEquals(ZaytharEngineId.VOICE_AUDIO, voice.engine)
        assertEquals(listOf("gemini-3.8-live", "gemini-3.5-transcribe"), voice.candidateModels)

        val maps = SmartEngineRouter.classifyPrompt("Find a quiet coffee shop nearby")
        assertEquals(ZaytharEngineId.NEURAL_CHAT, maps.engine)
        assertEquals(ProviderRoute.HYDROGEN, maps.recommendedRoute)
    }

    @Test
    fun runLocalAndHybridDiagnostics_passesAllDeterministicEngines() {
        val report = SmartEngineRouter.runLocalAndHybridDiagnostics(
            memoriesCount = 4,
            factsCount = 7,
            activePluginsCount = 7,
            firebaseReady = true,
            isGuestMode = false,
            liveProbeResult = Result.success("gemini-3.8-flash" to 142L)
        )

        assertEquals(9, report.totalCount)
        assertEquals(9, report.passedCount)
        assertEquals(142L, report.liveGeminiLatencyMs)
        assertTrue(report.liveGeminiStatus.contains("ONLINE · gemini-3.8-flash"))
        assertTrue(report.results.any { it.engine == ZaytharEngineId.RAG_AND_TOOLS && it.status == EngineCheckStatus.PASS })
        assertTrue(report.results.any { it.engine == ZaytharEngineId.SMART_ROUTER && it.status == EngineCheckStatus.PASS })
        assertTrue(report.results.any { it.engine == ZaytharEngineId.VOICE_AUDIO && it.status == EngineCheckStatus.READY })
    }
}
