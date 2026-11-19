package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AnimationStyleTest {
    @Test
    fun exposesTenSelectableStylesWithUniqueIds() {
        val styles = AnimationStyle.entries

        assertEquals(10, styles.size)
        assertEquals(10, styles.map { it.id }.toSet().size)
    }

    @Test
    fun everyStyleHasAValidMotionProfile() {
        AnimationStyle.entries.forEach { style ->
            assertNotEquals(0f, style.rotationSpeed)
            assertNotEquals(0, style.pulseDurationMillis)
            assertNotEquals(0f, style.packetSpeed)
            assertNotEquals(0f, style.nodeScale)
            assertEquals(true, style.orbitRatios.isNotEmpty())
        }

        assertEquals(
            10,
            AnimationStyle.entries.map {
                listOf(it.rotationSpeed, it.pulseDurationMillis, it.packetSpeed, it.nodeScale, it.orbitRatios)
            }.toSet().size
        )
    }

    @Test
    fun unknownStyleFallsBackToHologram() {
        assertEquals(AnimationStyle.HOLOGRAM, AnimationStyle.fromId("unknown-style"))
    }

    @Test
    fun knownStyleIdResolvesCorrectly() {
        assertEquals(AnimationStyle.CLAYMATION, AnimationStyle.fromId("claymation"))
    }
}
