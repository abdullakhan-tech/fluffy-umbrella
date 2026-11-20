package com.example.domain.tools

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInToolRuntimeTest {
    @Test
    fun calculatorRunsThroughTheAllowlistedRegistry() = runBlocking {
        val runtime = BuiltInToolRuntime()

        val result = runtime.executeLocalCommand(
            message = "calculate (12 + 8) * 3 - 4 / 2",
            authenticatedUid = "firebase-user",
            sessionId = "session-1",
            enabledPluginKeys = setOf("calculator")
        )

        val payload = (result as ToolExecutionResult.Completed).payload
        assertEquals("calculator", payload.summary)
        assertEquals("58", payload.content)
    }

    @Test
    fun disabledPluginDoesNotExecute() = runBlocking {
        val runtime = BuiltInToolRuntime()

        val result = runtime.executeLocalCommand(
            message = "calculate 2 + 2",
            authenticatedUid = "firebase-user",
            sessionId = "session-1",
            enabledPluginKeys = setOf("unit_converter")
        )

        val payload = (result as ToolExecutionResult.Completed).payload
        assertEquals(LocalCommandToolAdapter.NO_MATCH, payload.summary)
        assertTrue(payload.content.isEmpty())
    }

    @Test
    fun untrustedPluginKeysCannotEnableShellOrWildcardTools() = runBlocking {
        val runtime = BuiltInToolRuntime()

        val result = runtime.executeLocalCommand(
            message = "shell whoami",
            authenticatedUid = "firebase-user",
            sessionId = "session-1",
            enabledPluginKeys = setOf("shell", "filesystem", "http_request", "dynamic_code", "*")
        )

        val payload = (result as ToolExecutionResult.Completed).payload
        assertEquals(LocalCommandToolAdapter.NO_MATCH, payload.summary)
        assertTrue(payload.content.isEmpty())
    }

    @Test
    fun unrecognizedCommandsFallThroughWithoutRunningAnyTool() = runBlocking {
        val runtime = BuiltInToolRuntime()

        val result = runtime.executeLocalCommand(
            message = "explain quantum computing",
            authenticatedUid = "firebase-user",
            sessionId = "session-1",
            enabledPluginKeys = setOf("calculator", "utc_clock")
        )

        assertEquals(
            LocalCommandToolAdapter.NO_MATCH,
            (result as ToolExecutionResult.Completed).payload.summary
        )
    }

    @Test
    fun runtimeRejectsMissingIdentityOrSession() = runBlocking {
        val runtime = BuiltInToolRuntime()

        val missingUser = runtime.executeLocalCommand(
            message = "calculate 2 + 2",
            authenticatedUid = "",
            sessionId = "session-1",
            enabledPluginKeys = setOf("calculator")
        )
        val missingSession = runtime.executeLocalCommand(
            message = "calculate 2 + 2",
            authenticatedUid = "firebase-user",
            sessionId = "",
            enabledPluginKeys = setOf("calculator")
        )

        assertEquals(
            ToolRejectionCode.UNAUTHENTICATED,
            (missingUser as ToolExecutionResult.Rejected).code
        )
        assertEquals(
            ToolRejectionCode.INVALID_CONTEXT,
            (missingSession as ToolExecutionResult.Rejected).code
        )
    }

    @Test
    fun registryExposesOnlyTheBuiltInAllowlistedAdapter() {
        val descriptors = BuiltInToolRuntime().descriptors()

        assertEquals(listOf("local_command_router"), descriptors.map { it.id })
        assertEquals(ToolRiskLevel.READ_ONLY, descriptors.single().riskLevel)
    }
}
