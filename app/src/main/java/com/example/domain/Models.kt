package com.example.domain

enum class AgentRole(
    val id: String,
    val shortCode: String,
    val title: String,
    val subtitle: String,
    val chatInstruction: String,
    val specialistPrompt: String
) {
    GENERAL(
        id = "general",
        shortCode = "Z",
        title = "AI Core",
        subtitle = "General intelligence",
        chatInstruction = "Act as a capable general-purpose assistant.",
        specialistPrompt = "Generalist. Provide a clear, balanced overview."
    ),
    RESEARCH(
        id = "research",
        shortCode = "R",
        title = "Research",
        subtitle = "Evidence & synthesis",
        chatInstruction = "Act as a research specialist. Separate established facts from uncertainty.",
        specialistPrompt = "Research specialist. Produce verifiable findings, distinguish facts from assumptions, and flag missing sources."
    ),
    CODING(
        id = "coding",
        shortCode = "C",
        title = "Coding",
        subtitle = "Engineering & security",
        chatInstruction = "Act as a senior software engineer. Provide robust, secure code and consider edge cases.",
        specialistPrompt = "Senior software engineer. Produce implementation guidance/code, tests, security considerations, and edge cases."
    ),
    WRITING(
        id = "writing",
        shortCode = "W",
        title = "Writing",
        subtitle = "Clarity & editorial",
        chatInstruction = "Act as an expert writing editor. Match audience and tone while preserving intent.",
        specialistPrompt = "Writing specialist. Produce clear, audience-appropriate wording and preserve the task's intent."
    ),
    ANALYSIS(
        id = "analysis",
        shortCode = "A",
        title = "Analysis",
        subtitle = "Trade-offs & strategy",
        chatInstruction = "Act as an analytical strategist. Explain evidence, options, trade-offs, and recommendations.",
        specialistPrompt = "Analyst. Evaluate constraints, alternatives, risks, trade-offs, and a reasoned recommendation."
    ),
    CREATIVE(
        id = "creative",
        shortCode = "M",
        title = "Creative",
        subtitle = "Original concepts",
        chatInstruction = "Act as a creative director. Develop original concepts with concrete details.",
        specialistPrompt = "Creative specialist. Generate original concepts with concrete execution details and constraints."
    );

    companion object {
        val specialists: List<AgentRole> = listOf(RESEARCH, CODING, WRITING, ANALYSIS, CREATIVE)
        fun fromId(id: String): AgentRole = entries.find { it.id == id } ?: GENERAL
    }
}

enum class SupportedLanguage(val code: String, val displayName: String, val englishName: String) {
    ENGLISH("en-US", "English", "English"),
    URDU("ur-PK", "اردو (Urdu)", "Urdu"),
    ARABIC("ar-SA", "العربية (Arabic)", "Arabic"),
    HINDI("hi-IN", "हिन्दी (Hindi)", "Hindi"),
    FRENCH("fr-FR", "Français", "French"),
    SPANISH("es-ES", "Español", "Spanish"),
    GERMAN("de-DE", "Deutsch", "German"),
    CHINESE("zh-CN", "中文 (Chinese)", "Chinese"),
    JAPANESE("ja-JP", "日本語 (Japanese)", "Japanese");

    companion object {
        fun fromCode(code: String): SupportedLanguage = entries.find { it.code == code } ?: ENGLISH
    }
}

enum class ProviderRoute(
    val id: String,
    val title: String,
    val modelName: String,
    val description: String
) {
    FLASH(
        id = "flash",
        title = "GEMINI 3.8 FLASH",
        modelName = "gemini-3.8-flash",
        description = "High-capability Gemini 3.8 Flash via Firebase AI Logic."
    ),
    HYBRID(
        id = "hybrid",
        title = "HYBRID CORE",
        modelName = "gemini-3.8-flash",
        description = "Gemini 3.8 Flash primary routing with Gemini 3.5 Flash-Lite automatic fallback."
    ),
    LITE(
        id = "lite",
        title = "GEMINI FLASH-LITE",
        modelName = "gemini-3.5-flash-lite",
        description = "Ultra-fast Gemini 3.5 Flash-Lite routing for rapid responses."
    );

    companion object {
        fun fromId(id: String): ProviderRoute = entries.find { it.id == id } ?: HYBRID
    }
}

data class PromptTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val promptPrefix: String
)

val defaultPromptTemplates = listOf(
    PromptTemplate(
        id = "research_analyst",
        title = "RESEARCH ANALYST",
        subtitle = "Structured research and synthesis",
        promptPrefix = "Act as a research analyst. Break down this topic into key findings, evidence, and open questions: "
    ),
    PromptTemplate(
        id = "code_review",
        title = "CODE REVIEW",
        subtitle = "Quality and security review",
        promptPrefix = "Act as a senior software engineer. Review this code for bugs, security issues, and improvements: "
    ),
    PromptTemplate(
        id = "project_planner",
        title = "PROJECT PLANNER",
        subtitle = "Milestones and execution plan",
        promptPrefix = "Create a clear project plan with milestones, dependencies, risks, and next actions for: "
    ),
    PromptTemplate(
        id = "writing_studio",
        title = "WRITING STUDIO",
        subtitle = "Editing and communication",
        promptPrefix = "Rewrite the following text to be clear, concise, and compelling while preserving its meaning: "
    )
)

data class SpecialistDeliverable(
    val agent: AgentRole,
    val status: String, // "completed" or "failed"
    val output: String,
    val modelUsed: String = ""
)

data class MultiAgentRunResult(
    val reply: String,
    val modelUsed: String,
    val deliverables: List<SpecialistDeliverable>,
    val completedCount: Int,
    val totalCount: Int
)

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Streaming(val partialText: String) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
