package com.example

import com.example.domain.LocalToolRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
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

    @Test
    fun returnsUtcTime() {
        val result = LocalToolRouter.routeTool("What is the current UTC time?")
        assertNotNull(result)
        assertEquals("utc_time", result?.tool)
        assertTrue(result?.result?.contains("UTC") == true)
        assertTrue(result?.result?.contains("Z)") == true)
    }

    @Test
    fun convertsUnitsWhenPluginEnabledAndSkipsWhenDisabled() {
        val enabledResult = LocalToolRouter.routeTool("100 km to mi", setOf("unit_converter"))
        assertNotNull(enabledResult)
        assertEquals("unit_converter", enabledResult?.tool)
        assertTrue(enabledResult?.result?.contains("62.137") == true)

        val disabledResult = LocalToolRouter.routeTool("100 km to mi", setOf("calculator"))
        assertNull(disabledResult)
    }

    @Test
    fun validatesAndFormatsJsonPayload() {
        val result = LocalToolRouter.routeTool(
            "inspect json {\"name\":\"Zaythar\",\"active\":true}",
            setOf("json_inspector")
        )
        assertNotNull(result)
        assertEquals("json_inspector", result?.tool)
        assertTrue(result?.result?.contains("Valid JSON Object (2 keys)") == true)
    }
    @Test
    fun ignoresUnknownAndPrivilegedToolKeysEvenWhenListedAsEnabled() {
        val unknownToolRequest = LocalToolRouter.routeTool(
            "open shell rm -rf /",
            setOf("shell", "filesystem", "http_request", "browser", "admin")
        )
        assertNull(unknownToolRequest)

        val wildcardDoesNotEnableTools = LocalToolRouter.routeTool(
            "What is the current UTC time?",
            setOf("*")
        )
        assertNull(wildcardDoesNotEnableTools)
    }

    @Test
    fun recognizedLocalToolsRequireExplicitAllowListEntry() {
        assertNull(LocalToolRouter.routeTool("calculate 2 + 2", emptySet()))
        assertNull(LocalToolRouter.routeTool("100 km to mi", setOf("calculator")))
        assertNotNull(LocalToolRouter.routeTool("calculate 2 + 2", setOf("calculator")))
    }

}
