package com.example.data.remote

import com.example.domain.ProviderRoute
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CompanionBackendClientTest {

    @Test
    fun companionHealthDataModel_parsesCorrectly() {
        val health = CompanionHealth(
            status = "healthy",
            app = "Zaythar AI Companion Backend",
            version = "1.0.0",
            environment = "testing"
        )
        assertEquals("healthy", health.status)
        assertEquals("Zaythar AI Companion Backend", health.app)
        assertEquals("1.0.0", health.version)
        assertEquals("testing", health.environment)
    }

    @Test
    fun companionChatResponseDataModel_storesValues() {
        val resp = CompanionChatResponse(
            reply = "Hello from Python AI",
            modelUsed = "gemini-3.8-flash",
            provider = "gemini",
            executionTimeMs = 124.5,
            sessionId = "sess_001"
        )
        assertEquals("Hello from Python AI", resp.reply)
        assertEquals("gemini-3.8-flash", resp.modelUsed)
        assertEquals("gemini", resp.provider)
        assertEquals(124.5, resp.executionTimeMs, 0.001)
        assertEquals("sess_001", resp.sessionId)
    }

    @Test
    fun companionOrbStateDataModel_storesValues() {
        val orb = CompanionOrbState(
            state = "processing",
            pulseRateHz = 2.4,
            glowColorHex = "#9D00FF",
            activeConnections = 2
        )
        assertEquals("processing", orb.state)
        assertEquals(2.4, orb.pulseRateHz, 0.001)
        assertEquals("#9D00FF", orb.glowColorHex)
        assertEquals(2, orb.activeConnections)
    }

    @Test
    fun companionAgentTaskResponseDataModel_storesValues() {
        val taskResp = CompanionAgentTaskResponse(
            status = "success",
            toolUsed = "SafeMathEvaluator",
            summary = "Evaluated 42 * 2 = 84",
            executionTimeMs = 45.2
        )
        assertEquals("success", taskResp.status)
        assertEquals("SafeMathEvaluator", taskResp.toolUsed)
        assertEquals("Evaluated 42 * 2 = 84", taskResp.summary)
    }

    @Test
    fun jsonParsing_parsesCompanionChatJsonResponse() {
        val rawJson = """
            {
                "reply": "System operational.",
                "model_used": "companion-local",
                "provider": "auto",
                "execution_time_ms": 15.4,
                "session_id": "nexus-7"
            }
        """.trimIndent()

        val json = JSONObject(rawJson)
        val reply = json.optString("reply")
        val model = json.optString("model_used")
        val provider = json.optString("provider")
        val timeMs = json.optDouble("execution_time_ms")
        val session = json.optString("session_id")

        assertEquals("System operational.", reply)
        assertEquals("companion-local", model)
        assertEquals("auto", provider)
        assertEquals(15.4, timeMs, 0.001)
        assertEquals("nexus-7", session)
    }

    @Test
    fun companionBackendUrl_allowsHttpsAndDebugLoopbackOnly() {
        val client = CompanionBackendClient()
        assertEquals("https://api.example.com", client.normalizeUrl("https://api.example.com/"))
        assertEquals("http://127.0.0.1:8000", client.normalizeUrl("http://127.0.0.1:8000/"))
        assertThrows(IllegalArgumentException::class.java) {
            client.normalizeUrl("http://api.example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            client.normalizeUrl("http://user:password@127.0.0.1:8000")
        }
    }

    @Test
    fun pythonCompanionRoute_isConfiguredInProviderRouteEnum() {
        val route = ProviderRoute.fromId("python_companion")
        assertEquals(ProviderRoute.PYTHON_COMPANION, route)
        assertEquals("companion-server", route.modelName)
        assertTrue(route.title.contains("PYTHON"))
    }
}
