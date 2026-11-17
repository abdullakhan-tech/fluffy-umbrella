package com.example.domain.tools

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryTest {
    @Test
    fun readOnlyToolRunsWithoutApproval() = runBlocking {
        val registry = ToolRegistry()
        registry.register(
            FakeTool(
                descriptor = descriptor(risk = ToolRiskLevel.READ_ONLY),
                handler = { _, _ -> ToolPayload("Device status ready") }
            )
        )

        val result = registry.execute(
            ToolCall("request_1", "sample_tool"),
            context()
        )

        assertTrue(result is ToolExecutionResult.Completed)
    }

    @Test
    fun mutatingToolCannotRunUntilTheExactRequestIsApproved() = runBlocking {
        var executions = 0
        val registry = ToolRegistry()
        registry.register(
            FakeTool(
                descriptor = descriptor(risk = ToolRiskLevel.REVERSIBLE),
                handler = { _, _ ->
                    executions += 1
                    ToolPayload("Preference updated")
                }
            )
        )
        val call = ToolCall("request_2", "sample_tool")

        val pending = registry.execute(call, context())
        assertTrue(pending is ToolExecutionResult.ApprovalRequired)
        assertEquals(0, executions)

        val approved = registry.execute(
            call,
            context(approved = setOf("request_2"))
        )
        assertTrue(approved is ToolExecutionResult.Completed)
        assertEquals(1, executions)
    }

    @Test
    fun missingAndroidPermissionBlocksExecution() = runBlocking {
        var executions = 0
        val registry = ToolRegistry()
        registry.register(
            FakeTool(
                descriptor = descriptor(
                    risk = ToolRiskLevel.READ_ONLY,
                    permissions = setOf("android.permission.CAMERA")
                ),
                handler = { _, _ ->
                    executions += 1
                    ToolPayload("Camera ready")
                }
            )
        )

        val result = registry.execute(ToolCall("request_3", "sample_tool"), context())

        assertTrue(result is ToolExecutionResult.Rejected)
        assertEquals(
            ToolRejectionCode.PERMISSION_REQUIRED,
            (result as ToolExecutionResult.Rejected).code
        )
        assertEquals(0, executions)
    }

    @Test
    fun rejectsUnknownAndOversizedArguments() = runBlocking {
        val registry = ToolRegistry()
        registry.register(
            FakeTool(
                descriptor = descriptor(
                    parameters = listOf(
                        ToolParameter(
                            name = "query",
                            type = ToolValueType.TEXT,
                            maxLength = 8
                        )
                    )
                ),
                handler = { _, _ -> ToolPayload("Search complete") }
            )
        )

        val unknown = registry.execute(
            ToolCall("request_4", "sample_tool", mapOf("shell" to ToolValue.Text("whoami"))),
            context()
        )
        val oversized = registry.execute(
            ToolCall("request_5", "sample_tool", mapOf("query" to ToolValue.Text("123456789"))),
            context()
        )

        assertTrue(unknown is ToolExecutionResult.Rejected)
        assertTrue(oversized is ToolExecutionResult.Rejected)
    }

    @Test
    fun rejectsUnauthenticatedContext() = runBlocking {
        val registry = ToolRegistry()
        registry.register(FakeTool(descriptor(), handler = { _, _ -> ToolPayload("OK") }))

        val result = registry.execute(
            ToolCall("request_6", "sample_tool"),
            ToolExecutionContext(authenticatedUid = "", sessionId = "session")
        )

        assertEquals(
            ToolRejectionCode.UNAUTHENTICATED,
            (result as ToolExecutionResult.Rejected).code
        )
    }

    @Test
    fun timesOutSlowToolAndDoesNotExposeExceptionText() = runBlocking {
        val registry = ToolRegistry()
        registry.register(
            FakeTool(
                descriptor(timeoutMillis = 20),
                handler = { _, _ ->
                    delay(250)
                    ToolPayload("Too late")
                }
            )
        )

        val result = registry.execute(ToolCall("request_7", "sample_tool"), context())

        assertTrue(result is ToolExecutionResult.Failed)
        assertEquals(
            ToolFailureCode.TIMED_OUT,
            (result as ToolExecutionResult.Failed).code
        )
        assertTrue(result.message.contains("time limit"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateToolIdsCannotReplaceAnExistingAdapter() {
        val registry = ToolRegistry()
        registry.register(FakeTool(descriptor(), handler = { _, _ -> ToolPayload("First") }))
        registry.register(FakeTool(descriptor(), handler = { _, _ -> ToolPayload("Second") }))
    }

    private fun descriptor(
        risk: ToolRiskLevel = ToolRiskLevel.READ_ONLY,
        permissions: Set<String> = emptySet(),
        parameters: List<ToolParameter> = emptyList(),
        timeoutMillis: Long = 1_000
    ) = ToolDescriptor(
        id = "sample_tool",
        displayName = "Sample tool",
        description = "A test-only adapter.",
        riskLevel = risk,
        requiredPermissions = permissions,
        parameters = parameters,
        timeoutMillis = timeoutMillis
    )

    private fun context(
        approved: Set<String> = emptySet(),
        permissions: Set<String> = emptySet()
    ) = ToolExecutionContext(
        authenticatedUid = "firebase-user",
        sessionId = "chat-session",
        grantedPermissions = permissions,
        approvedRequestIds = approved
    )

    private class FakeTool(
        override val descriptor: ToolDescriptor,
        private val handler: suspend (ToolExecutionContext, ToolArguments) -> ToolPayload
    ) : ZaytharTool {
        override suspend fun execute(
            context: ToolExecutionContext,
            arguments: ToolArguments
        ): ToolPayload = handler(context, arguments)
    }
}
