package com.example

import com.example.domain.LocalToolRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalToolRouterTest {

    @Test
    fun calculatesBasicArithmetic() {
        val result = LocalToolRouter.routeTool("calculate (12 + 8) * 3 - 4 / 2")
        assertNotNull(result)
        assertEquals("calculator", result?.tool)
        assertEquals("58", result?.result)
    }

    @Test
    fun respectsOperatorPrecedence() {
        assertEquals("14", LocalToolRouter.calculate("2 + 3 * 4"))
    }

    @Test
    fun supportsNestedParenthesesAndUnaryMinus() {
        assertEquals("-14", LocalToolRouter.calculate("-(2 + (3 * 4)) + 0"))
    }

    @Test
    fun supportsDecimalArithmetic() {
        assertEquals("3.75", LocalToolRouter.calculate("1.5 * 2.5"))
    }

    @Test
    fun rejectsArbitraryNonMathText() {
        assertNull(LocalToolRouter.routeTool("explain quantum computing"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDivisionByZero() {
        LocalToolRouter.calculate("10 / 0")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsExcessiveExponent() {
        LocalToolRouter.calculate("2 ** 12")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTrailingGarbage() {
        LocalToolRouter.calculate("2 + 2; delete all files")
    }

    @Test
    fun returnsUtcTime() {
        val result = LocalToolRouter.routeTool("What is the current UTC time?")
        assertNotNull(result)
        assertEquals("utc_time", result?.tool)
        assertTrue(result?.result?.endsWith("Z") == true)
    }
}
