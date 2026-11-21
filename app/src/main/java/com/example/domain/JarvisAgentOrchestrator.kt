package com.example.domain

/**
 * Product-level orchestration contract for Zaythar's Jarvis-style agent team.
 *
 * Planning is deterministic and safe by default. Actual side effects must be performed by
 * registered app-owned tools, which independently enforce identity, permissions and approval.
 * A model response can suggest a plan; it can never grant itself execution authority.
 */
enum class AgentAutonomy {
    ASSISTED,
    GUARDED_AUTONOMOUS
}

enum class AgentCapability {
    RESEARCH,
    REASON,
    PLAN,
    WRITE_DRAFT,
    REVIEW_CODE,
    READ_LOCAL_DATA,
    RUN_APPROVED_LOCAL_TOOL,
    EXTERNAL_WRITE
}

enum class AgentActionRisk {
    READ_ONLY,
    REVERSIBLE,
    HIGH_IMPACT
}

enum class AgentTaskKind {
    RESEARCH,
    ENGINEERING,
    STRATEGY,
    COMMUNICATION,
    DEVICE_ACTION,
    DESTRUCTIVE_ACTION,
    GENERAL
}

data class JarvisAgentPersona(
    val id: String,
    val name: String,
    val title: String,
    val personality: String,
    val operatingPrinciples: List<String>,
    val capabilities: Set<AgentCapability>
)

data class AgentWorkStep(
    val id: String,
    val title: String,
    val ownerAgentId: String,
    val risk: AgentActionRisk,
    val requiresUserApproval: Boolean,
    val completionCheck: String
)

data class AgentExecutionPlan(
    val request: String,
    val taskKind: AgentTaskKind,
    val autonomy: AgentAutonomy,
    val assignedAgents: List<JarvisAgentPersona>,
    val steps: List<AgentWorkStep>,
    val approvalRequired: Boolean,
    val approvalReason: String?
)

object JarvisAgentOrchestrator {
    val team: List<JarvisAgentPersona> = listOf(
        JarvisAgentPersona(
            id = "athena",
            name = "Athena",
            title = "Research Intelligence",
            personality = "Curious, precise and evidence-first; openly labels uncertainty.",
            operatingPrinciples = listOf("Separate facts from assumptions", "Prefer verifiable sources", "Show confidence and unknowns"),
            capabilities = setOf(AgentCapability.RESEARCH, AgentCapability.REASON, AgentCapability.PLAN)
        ),
        JarvisAgentPersona(
            id = "forge",
            name = "Forge",
            title = "Principal Engineer",
            personality = "Pragmatic, meticulous and test-driven; favors maintainable solutions.",
            operatingPrinciples = listOf("Inspect before editing", "Minimize risky changes", "Run tests after implementation"),
            capabilities = setOf(AgentCapability.REASON, AgentCapability.PLAN, AgentCapability.REVIEW_CODE, AgentCapability.RUN_APPROVED_LOCAL_TOOL)
        ),
        JarvisAgentPersona(
            id = "sentinel",
            name = "Sentinel",
            title = "Security Guardian",
            personality = "Cautious and adversarial in reviews; protects user data and device control.",
            operatingPrinciples = listOf("Least privilege", "Never expose secrets", "Require approval for consequential actions"),
            capabilities = setOf(AgentCapability.RESEARCH, AgentCapability.REASON, AgentCapability.REVIEW_CODE)
        ),
        JarvisAgentPersona(
            id = "oracle",
            name = "Oracle",
            title = "Strategy & Planning",
            personality = "Calm, systems-minded and outcome-focused; turns goals into measurable milestones.",
            operatingPrinciples = listOf("Define success criteria", "Surface dependencies", "Track blockers and trade-offs"),
            capabilities = setOf(AgentCapability.REASON, AgentCapability.PLAN, AgentCapability.READ_LOCAL_DATA)
        ),
        JarvisAgentPersona(
            id = "echo",
            name = "Echo",
            title = "Communication & Memory",
            personality = "Warm, concise and context-aware; preserves the user's intent and preferred tone.",
            operatingPrinciples = listOf("Preserve user intent", "Draft before sending", "Keep durable notes transparent"),
            capabilities = setOf(AgentCapability.WRITE_DRAFT, AgentCapability.READ_LOCAL_DATA, AgentCapability.PLAN)
        ),
        JarvisAgentPersona(
            id = "atlas",
            name = "Atlas",
            title = "Workflow Operator",
            personality = "Methodical and dependable; reports each action and verifies the result.",
            operatingPrinciples = listOf("Use only registered tools", "Never bypass permission prompts", "Verify every completed step"),
            capabilities = setOf(AgentCapability.PLAN, AgentCapability.RUN_APPROVED_LOCAL_TOOL)
        )
    )

    fun createPlan(
        request: String,
        autonomy: AgentAutonomy = AgentAutonomy.GUARDED_AUTONOMOUS
    ): AgentExecutionPlan {
        val normalized = request.trim().lowercase()
        require(normalized.isNotBlank()) { "A task request is required." }
        require(normalized.length <= 8_000) { "Task request is too long." }

        val destructive = listOf("delete", "remove permanently", "wipe", "erase", "format").any(normalized::contains)
        val externalWrite = listOf("send", "publish", "post", "purchase", "pay", "transfer", "install", "deploy", "commit", "upload").any(normalized::contains)
        val deviceAction = listOf("open app", "change setting", "turn on", "turn off", "control device", "call ", "message ").any(normalized::contains)
        val engineering = listOf("code", "build", "fix", "debug", "repo", "repository", "gradle", "apk", "app").any(normalized::contains)
        val research = listOf("research", "find", "compare", "look up", "investigate", "explain").any(normalized::contains)
        val communication = listOf("email", "reply", "write message", "send message", "draft").any(normalized::contains)

        val kind = when {
            destructive -> AgentTaskKind.DESTRUCTIVE_ACTION
            deviceAction -> AgentTaskKind.DEVICE_ACTION
            engineering -> AgentTaskKind.ENGINEERING
            research -> AgentTaskKind.RESEARCH
            communication -> AgentTaskKind.COMMUNICATION
            else -> AgentTaskKind.STRATEGY
        }
        val assignedIds = when (kind) {
            AgentTaskKind.DESTRUCTIVE_ACTION, AgentTaskKind.DEVICE_ACTION -> listOf("atlas", "sentinel", "oracle")
            AgentTaskKind.ENGINEERING -> listOf("forge", "sentinel", "oracle", "atlas")
            AgentTaskKind.RESEARCH -> listOf("athena", "sentinel", "oracle")
            AgentTaskKind.COMMUNICATION -> listOf("echo", "sentinel")
            AgentTaskKind.STRATEGY -> listOf("oracle", "athena", "sentinel")
            AgentTaskKind.GENERAL -> listOf("oracle", "echo")
        }
        val assigned = team.filter { it.id in assignedIds }
        val approval = destructive || externalWrite || deviceAction
        val risk = when {
            destructive -> AgentActionRisk.HIGH_IMPACT
            approval -> AgentActionRisk.REVERSIBLE
            else -> AgentActionRisk.READ_ONLY
        }
        val steps = listOf(
            AgentWorkStep("understand", "Understand request and constraints", "oracle", AgentActionRisk.READ_ONLY, false, "Goal and success criteria are explicit."),
            AgentWorkStep("prepare", "Research, plan and prepare the proposed work", assigned.first().id, AgentActionRisk.READ_ONLY, false, "Plan includes dependencies and expected outputs."),
            AgentWorkStep("review", "Run safety and quality review", "sentinel", AgentActionRisk.READ_ONLY, false, "No secrets, permission bypasses or unsafe actions."),
            AgentWorkStep("execute", "Execute permitted work using registered tools", "atlas", risk, approval, "Tool result is checked against the requested outcome."),
            AgentWorkStep("verify", "Verify outcome and report incomplete items", "forge", AgentActionRisk.READ_ONLY, false, "Evidence or test results support completion.")
        )
        val reason = when {
            destructive -> "Destructive changes require explicit approval of the exact action."
            deviceAction -> "Device or account actions require user permission and confirmation."
            externalWrite -> "External writes, publishing, payments, installs and deployments require explicit approval."
            else -> null
        }
        return AgentExecutionPlan(
            request = request.trim(),
            taskKind = kind,
            autonomy = autonomy,
            assignedAgents = assigned,
            steps = steps,
            approvalRequired = approval,
            approvalReason = reason
        )
    }
}
