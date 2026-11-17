package com.example.domain.tools

import com.example.domain.LocalToolRouter
import java.util.UUID

/**
 * First-party, read-only adapter for Zaythar's existing local command router.
 * It cannot execute shell commands, load code, or call arbitrary URLs.
 */
class LocalCommandToolAdapter : ZaytharTool {
    override val descriptor = ToolDescriptor(
        id = "local_command_router",
        displayName = "Local Command Router",
        description = "Runs allowlisted calculator, UTC clock, unit conversion, and JSON inspection commands.",
        riskLevel = ToolRiskLevel.READ_ONLY,
        parameters = listOf(
            ToolParameter(
                name = "message",
                type = ToolValueType.TEXT,
                maxLength = 12_000
            )
        ),
        timeoutMillis = 2_000,
        maxTotalArgumentChars = 12_100
    )

    override suspend fun execute(
        context: ToolExecutionContext,
        arguments: ToolArguments
    ): ToolPayload {
        val message = (arguments["message"] as? ToolValue.Text)?.value
            ?: return ToolPayload(NO_MATCH)
        val route = LocalToolRouter.routeTool(message, context.enabledPluginKeys)
            ?: return ToolPayload(NO_MATCH)
        return ToolPayload(summary = route.tool, content = route.result)
    }

    companion object {
        const val NO_MATCH = "no_match"
    }
}

/**
 * App-owned runtime that exposes only explicitly registered first-party adapters.
 * Plugin keys come from the authenticated user's Firestore configuration.
 */
class BuiltInToolRuntime(
    auditSink: (ToolAuditEvent) -> Unit = {}
) {
    private val registry = ToolRegistry(auditSink).apply {
        register(LocalCommandToolAdapter())
    }

    fun descriptors(): List<ToolDescriptor> = registry.descriptors()

    suspend fun executeLocalCommand(
        message: String,
        authenticatedUid: String,
        sessionId: String,
        enabledPluginKeys: Set<String>
    ): ToolExecutionResult {
        val call = ToolCall(
            requestId = UUID.randomUUID().toString(),
            toolId = "local_command_router",
            arguments = mapOf("message" to ToolValue.Text(message))
        )
        return registry.execute(
            call = call,
            context = ToolExecutionContext(
                authenticatedUid = authenticatedUid,
                sessionId = sessionId,
                enabledPluginKeys = enabledPluginKeys
            )
        )
    }
}
