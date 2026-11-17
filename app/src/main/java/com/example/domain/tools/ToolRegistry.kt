package com.example.domain.tools

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * In-memory allowlist for first-party tool adapters.
 *
 * The registry never loads code dynamically and never evaluates model-generated scripts.
 * The host app must provide a trusted identity, current Android permissions, and approvals.
 */
class ToolRegistry(
    private val auditSink: (ToolAuditEvent) -> Unit = {}
) {
    private val lock = Any()
    private val tools = linkedMapOf<String, ZaytharTool>()

    fun register(tool: ZaytharTool) {
        synchronized(lock) {
            val id = tool.descriptor.id
            require(id !in tools) { "A tool with ID '$id' is already registered." }
            tools[id] = tool
        }
    }

    fun descriptors(): List<ToolDescriptor> = synchronized(lock) {
        tools.values.map { it.descriptor }.sortedBy { it.id }
    }

    suspend fun execute(
        call: ToolCall,
        context: ToolExecutionContext
    ): ToolExecutionResult {
        if (context.authenticatedUid.isBlank()) {
            return reject(call, ToolRejectionCode.UNAUTHENTICATED, "Sign in before using tools.")
        }
        if (context.sessionId.isBlank() || context.sessionId.length > 128) {
            return reject(call, ToolRejectionCode.INVALID_CONTEXT, "A valid active session is required.")
        }
        if (!call.requestId.matches(Regex("^[A-Za-z0-9_-]{1,128}$"))) {
            return reject(call, ToolRejectionCode.INVALID_REQUEST_ID, "The tool request ID is invalid.")
        }

        val tool = synchronized(lock) { tools[call.toolId] }
            ?: return reject(call, ToolRejectionCode.UNKNOWN_TOOL, "This tool is not registered.")

        val descriptor = tool.descriptor
        validateArguments(descriptor, call.arguments)?.let { message ->
            return reject(call, ToolRejectionCode.INVALID_ARGUMENTS, message)
        }

        val missingPermissions = descriptor.requiredPermissions - context.grantedPermissions
        if (missingPermissions.isNotEmpty()) {
            return reject(
                call,
                ToolRejectionCode.PERMISSION_REQUIRED,
                "Required Android permissions have not been granted."
            )
        }

        if (descriptor.riskLevel != ToolRiskLevel.READ_ONLY &&
            call.requestId !in context.approvedRequestIds
        ) {
            return ToolExecutionResult.ApprovalRequired(
                requestId = call.requestId,
                toolId = descriptor.id,
                explanation = "This action needs explicit user approval before it can run."
            )
        }

        val payload = try {
            withTimeout(descriptor.timeoutMillis) {
                tool.execute(context, call.arguments)
            }
        } catch (_: TimeoutCancellationException) {
            return fail(call, ToolFailureCode.TIMED_OUT, "The tool exceeded its time limit.")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return fail(call, ToolFailureCode.EXECUTION_FAILED, "The tool could not complete safely.")
        }

        if (payload.summary.isBlank() ||
            payload.summary.length > MAX_SUMMARY_CHARS ||
            payload.content.length > MAX_OUTPUT_CHARS
        ) {
            return fail(call, ToolFailureCode.INVALID_OUTPUT, "The tool returned an invalid or oversized result.")
        }

        record(
            ToolAuditEvent(
                requestId = call.requestId,
                toolId = descriptor.id,
                outcome = ToolAuditOutcome.COMPLETED
            )
        )
        return ToolExecutionResult.Completed(payload)
    }

    private fun validateArguments(
        descriptor: ToolDescriptor,
        arguments: ToolArguments
    ): String? {
        val parameters = descriptor.parameters.associateBy { it.name }
        val unknownNames = arguments.keys - parameters.keys
        if (unknownNames.isNotEmpty()) return "The request contains an unsupported argument."

        val missingNames = descriptor.parameters
            .filter { it.required && it.name !in arguments }
        if (missingNames.isNotEmpty()) return "The request is missing a required argument."

        val totalChars = arguments.entries.sumOf { (name, value) ->
            name.length + when (value) {
                is ToolValue.Text -> value.value.length
                is ToolValue.Number -> value.value.toString().length
                is ToolValue.BooleanValue -> value.value.toString().length
            }
        }
        if (totalChars > descriptor.maxTotalArgumentChars) {
            return "The request arguments are too large."
        }

        for ((name, value) in arguments) {
            val parameter = parameters.getValue(name)
            when (parameter.type) {
                ToolValueType.TEXT -> {
                    val text = (value as? ToolValue.Text)?.value
                        ?: return "An argument has the wrong type."
                    if (text.length > parameter.maxLength) {
                        return "A text argument exceeds its size limit."
                    }
                }
                ToolValueType.NUMBER -> {
                    val number = (value as? ToolValue.Number)?.value
                        ?: return "An argument has the wrong type."
                    if (!number.isFinite()) return "A numeric argument must be finite."
                    if (parameter.minNumber != null && number < parameter.minNumber) {
                        return "A numeric argument is below its allowed range."
                    }
                    if (parameter.maxNumber != null && number > parameter.maxNumber) {
                        return "A numeric argument is above its allowed range."
                    }
                }
                ToolValueType.BOOLEAN -> {
                    if (value !is ToolValue.BooleanValue) {
                        return "An argument has the wrong type."
                    }
                }
            }
        }
        return null
    }

    private fun reject(
        call: ToolCall,
        code: ToolRejectionCode,
        message: String
    ): ToolExecutionResult.Rejected {
        record(
            ToolAuditEvent(
                requestId = call.requestId.take(128),
                toolId = call.toolId.take(64),
                outcome = ToolAuditOutcome.REJECTED,
                reasonCode = code.name
            )
        )
        return ToolExecutionResult.Rejected(code, message)
    }

    private fun fail(
        call: ToolCall,
        code: ToolFailureCode,
        message: String
    ): ToolExecutionResult.Failed {
        record(
            ToolAuditEvent(
                requestId = call.requestId.take(128),
                toolId = call.toolId.take(64),
                outcome = ToolAuditOutcome.FAILED,
                reasonCode = code.name
            )
        )
        return ToolExecutionResult.Failed(code, message)
    }

    private fun record(event: ToolAuditEvent) {
        // Audit persistence must never cause a tool to execute twice or crash the UI.
        runCatching { auditSink(event) }
    }

    private companion object {
        const val MAX_SUMMARY_CHARS = 300
        const val MAX_OUTPUT_CHARS = 12_000
    }
}
