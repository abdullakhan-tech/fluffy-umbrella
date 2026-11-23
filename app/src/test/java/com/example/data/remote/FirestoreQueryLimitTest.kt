package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreQueryLimitTest {
    @Test
    fun keepsValidRequestedLimit() {
        assertEquals(24L, boundedFirestoreQueryLimit(requested = 24L, maximum = 60L))
    }

    @Test
    fun clampsZeroAndNegativeLimitsToOne() {
        assertEquals(1L, boundedFirestoreQueryLimit(requested = 0L, maximum = 60L))
        assertEquals(1L, boundedFirestoreQueryLimit(requested = -10L, maximum = 60L))
    }

    @Test
    fun clampsExtremeLimitsToConfiguredMaximum() {
        assertEquals(60L, boundedFirestoreQueryLimit(requested = Long.MAX_VALUE, maximum = 60L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveMaximum() {
        boundedFirestoreQueryLimit(requested = 10L, maximum = 0L)
    }
}
