package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderRouteTest {
    private val firebaseAiLogicSupportedModels = setOf(
        "gemini-4-hydrogen",
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite"
    )

    @Test
    fun hybridUsesGemini4ThenBoundedFallbacks() {
        assertEquals(
            listOf("gemini-4-hydrogen", "gemini-3.8-flash", "gemini-3.5-flash-lite"),
            ProviderRoute.HYBRID.candidateModels
        )
    }

    @Test
    fun singleModelRoutesDoNotFanOut() {
        assertEquals(listOf("gemini-3.8-flash"), ProviderRoute.FLASH.candidateModels)
        assertEquals(listOf("gemini-3.5-flash-lite"), ProviderRoute.LITE.candidateModels)
    }

    @Test
    fun unknownPreferenceSafelyDefaultsToHybrid() {
        assertEquals(ProviderRoute.HYBRID, ProviderRoute.fromId("unknown-provider"))
    }

    @Test
    fun everyConfiguredCandidateIsInTheFirebaseSupportedModelAllowList() {
        assertTrue(
            ProviderRoute.entries
                .flatMap { it.candidateModels }
                .all { it in firebaseAiLogicSupportedModels }
        )
    }
}
