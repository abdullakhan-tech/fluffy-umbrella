package com.example.domain.tools

enum class ToolRiskLevel {
    READ_ONLY,
    REVERSIBLE,
    HIGH_IMPACT
}

enum class ToolValueType {
    TEXT,
    NUMBER,
    BOOLEAN
}

sealed interface ToolValue {
    data class Text(val value: String) : ToolValue
    data class Number(val value: Double) : ToolValue
    data class BooleanValue(val value: Boolean) : ToolValue
}

typealias ToolArguments = Map<String, ToolValue>

data class ToolParameter(
    val name: String,
    val type: ToolValueType,
    val required: Boolean = true,
    val maxLength: Int = 2_000,
    val minNumber: Double? = null,
    val maxNumber: Double? = null
) {
    init {
        require(name.matches(Regex("^[a-z][a-zA-Z0-9_]{0,63}$"))) {
            "Tool parameter names must be stable identifiers."
        }
        require(maxLength in 1..12_000)
        require(minNumber == null || minNumber.isFinite())
        require(maxNumber == null || maxNumber.isFinite())
        require(minNumber == null || maxNumber == null || minNumber <= maxNumber)
    }
}

data class ToolDescriptor(
    val id: String,
    val displayName: String,
    val description: String,
    val riskLevel: ToolRiskLevel,
    val parameters: List<ToolParameter> = emptyList(),
    val requiredPermissions: Set<String> = emptySet(),
    val timeoutMillis: Long = 10_000,
    val maxTotalArgumentChars: Int = 8_000
) {
    init {
        require(id.matches(Regex("^[a-z][a-z0-9_.-]{0,63}$"))) {
            "Tool IDs must be stable lowercase identifiers."
        }
        require(displayName.isNotBlank() && displayName.length <= 80)
        require(description.isNotBlank() && description.length <= 500)
        require(timeoutMillis in 1..30_000)
        require(maxTotalArgumentChars in 1..16_384)
        require(parameters.map { it.name }.distinct().size == parameters.size) {
            "Tool parameter names must be unique."
        }
        require(requiredPermissions.all { it.isNotBlank() && it.length <= 160 })
    }
}

data class ToolCall(
    val requestId: String,
    val toolId: String,
    val arguments: ToolArguments = emptyMap()
)

/**
 * Immutable snapshot of the exact action shown to the user for approval.
 * Create it through ToolRegistry.approvalFor(call, context), not from model output.
 */
data class ToolApproval internal constructor(
    val authenticatedUid: String,
    val sessionId: String,
    val requestId: String,
    val toolId: String,
    val arguments: ToolArguments
)

/**
 * Construct this context from the signed-in app session and Android permission state.
 * Never populate identity or approvals from model-generated arguments.
 */
data class ToolExecutionContext(
    val authenticatedUid: String,
    val sessionId: String,
    val grantedPermissions: Set<String> = emptySet(),
    val approvedTools: Set<ToolApproval> = emptySet(),
    /** Enabled declarative plugin keys supplied by the authenticated app, never by the model. */
    val enabledPluginKeys: Set<String> = emptySet()
)

data class ToolPayload(
    val summary: String,
    val content: String = ""
)

interface ZaytharTool {
    val descriptor: ToolDescriptor

    suspend fun execute(
        context: ToolExecutionContext,
        arguments: ToolArguments
    ): ToolPayload
}

enum class ToolRejectionCode {
    UNAUTHENTICATED,
    INVALID_REQUEST_ID,
    INVALID_CONTEXT,
    UNKNOWN_TOOL,
    INVALID_ARGUMENTS,
    PERMISSION_REQUIRED
}

enum class ToolFailureCode {
    TIMED_OUT,
    EXECUTION_FAILED,
    INVALID_OUTPUT
}

enum class ToolAuditOutcome {
    COMPLETED,
    APPROVAL_REQUIRED,
    REJECTED,
    FAILED
}

/**
 * Deliberately excludes raw arguments, prompts, and tool output to avoid leaking user data.
 */
data class ToolAuditEvent(
    val requestId: String,
    val toolId: String,
    val outcome: ToolAuditOutcome,
    val reasonCode: String? = null
)

sealed interface ToolExecutionResult {
    data class Completed(val payload: ToolPayload) : ToolExecutionResult

    data class ApprovalRequired(
        val requestId: String,
        val toolId: String,
        val explanation: String
    ) : ToolExecutionResult

    data class Rejected(
        val code: ToolRejectionCode,
        val message: String
    ) : ToolExecutionResult

    data class Failed(
        val code: ToolFailureCode,
        val message: String
    ) : ToolExecutionResult
}
