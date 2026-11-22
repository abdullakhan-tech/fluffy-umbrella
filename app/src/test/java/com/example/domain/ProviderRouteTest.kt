package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderRouteTest {
    private val firebaseAiLogicSupportedModels = setOf(
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite"
    )

    @Test
    fun hybridUsesFlashAsPrimaryModel() {
        assertEquals("gemini-3.8-flash", ProviderRoute.HYBRID.modelName)
    }

    @Test
    fun singleModelRoutesUseTheirConfiguredModels() {
        assertEquals("gemini-3.8-flash", ProviderRoute.FLASH.modelName)
        assertEquals("gemini-3.5-flash-lite", ProviderRoute.LITE.modelName)
    }

    @Test
    fun unknownPreferenceSafelyDefaultsToHybrid() {
        assertEquals(ProviderRoute.HYBRID, ProviderRoute.fromId("unknown-provider"))
    }

    @Test
    fun supportedFirebaseRoutesUseAllowListedModels() {
        assertTrue(
            listOf(ProviderRoute.HYBRID, ProviderRoute.FLASH, ProviderRoute.LITE)
                .all { it.modelName in firebaseAiLogicSupportedModels }
        )
    }

    @Test
    fun pythonCompanionRouteHasExpectedAttributes() {
        assertEquals("companion-server", ProviderRoute.PYTHON_COMPANION.modelName)
        assertEquals(ProviderRoute.PYTHON_COMPANION, ProviderRoute.fromId("python_companion"))
    }

    @Test
    fun everyRouteHasAnIdAndModelName() {
        assertTrue(ProviderRoute.entries.all { it.id.isNotBlank() && it.modelName.isNotBlank() })
        assertFalse(ProviderRoute.entries.map { it.id }.toSet().size < ProviderRoute.entries.size)
    }
}
