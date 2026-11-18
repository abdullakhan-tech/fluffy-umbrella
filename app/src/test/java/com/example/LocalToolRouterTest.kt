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
    fun hashesSha256GeneratesUuidAndEncodesBase64() {
        val sha = LocalToolRouter.routeTool("sha256 zaythar", setOf("crypto_hash_uuid"))
        assertNotNull(sha)
        assertEquals("crypto_sha256", sha?.tool)
        assertTrue(sha?.result?.startsWith("SHA-256: ") == true)
        assertEquals(73, sha?.result?.length)

        val uuid = LocalToolRouter.routeTool("generate uuid", setOf("crypto_hash_uuid"))
        assertNotNull(uuid)
        assertEquals("crypto_uuid", uuid?.tool)
        assertTrue(uuid?.result?.contains("-") == true)

        val encoded = LocalToolRouter.routeTool("base64 encode ZaytharAI", setOf("crypto_hash_uuid"))
        assertEquals("Base64 Encoded: WmF5dGhhckFJ", encoded?.result)

        val decoded = LocalToolRouter.routeTool("base64 decode WmF5dGhhckFJ", setOf("crypto_hash_uuid"))
        assertEquals("Base64 Decoded: ZaytharAI", decoded?.result)
    }

    @Test
    fun convertsHexRgbColorsCountsTextAndExpandedUnits() {
        val toRgb = LocalToolRouter.routeTool("#FF3155 to rgb", setOf("color_text_metrics"))
        assertEquals("#FF3155 = rgb(255, 49, 85)", toRgb?.result)

        val toHex = LocalToolRouter.routeTool("rgb(255, 49, 85) to hex", setOf("color_text_metrics"))
        assertEquals("rgb(255, 49, 85) = #FF3155", toHex?.result)

        val stats = LocalToolRouter.routeTool("count words Zaythar Smart AI Engine", setOf("color_text_metrics"))
        assertTrue(stats?.result?.contains("Words: 4") == true)

        val storage = LocalToolRouter.routeTool("1024 mb to gb", setOf("unit_converter"))
        assertEquals("1024 mb = 1 gb", storage?.result)

        val time = LocalToolRouter.routeTool("120 min to hr", setOf("unit_converter"))
        assertEquals("120 min = 2 hr", time?.result)
    }
}
