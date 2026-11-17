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
        title = "GEMINI 4 HYDROGEN · HYBRID CORE",
        modelName = "gemini-4-hydrogen",
        description = "Gemini 4 Hydrogen primary routing with Gemini 3.8 Flash and Gemini 3.5 Flash-Lite automatic fallbacks."
    ),
    LITE(
        id = "lite",
        title = "GEMINI FLASH-LITE",
        modelName = "gemini-3.5-flash-lite",
        description = "Ultra-fast Gemini 3.5 Flash-Lite routing for rapid responses."
    );

    val candidateModels: List<String>
        get() = when (this) {
            HYBRID -> listOf(modelName, FLASH.modelName, LITE.modelName)
            FLASH, LITE -> listOf(modelName)
        }

    companion object {
        fun fromId(id: String): ProviderRoute = entries.find { it.id == id } ?: HYBRID
    }
}

enum class NeuralBrainMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val directive: String
) {
    REASONER(
        id = "reasoner",
        title = "Neural Reasoner",
        subtitle = "Step-by-step decomposition & action plan",
        directive = "Decompose the problem into first principles, evaluate hidden constraints, quantify certainty, and recommend concrete next actions."
    ),
    CONSOLIDATOR(
        id = "consolidator",
        title = "Memory Consolidator",
        subtitle = "Extract knowledge graph triples",
        directive = "Synthesize the input and recent memory/conversation context into canonical knowledge triples (subject, predicate, objectValue) plus durable insights."
    ),
    STRATEGIST(
        id = "strategist",
        title = "Strategic Planner",
        subtitle = "Architecture, milestones & risk matrix",
        directive = "Produce an architectural & operational plan covering critical path, trade-offs, mitigation steps, and measurable success metrics."
    ),
    DIAGNOSTICS(
        id = "diagnostics",
        title = "Cognitive Diagnostics",
        subtitle = "Audit gaps, assumptions & blind spots",
        directive = "Critically stress-test the input for unstated assumptions, failure modes, ambiguity, and security/reliability gaps."
    );

    companion object {
        fun fromId(id: String): NeuralBrainMode = entries.find { it.id == id } ?: REASONER
    }
}

data class SuggestedTriple(
    val subject: String,
    val predicate: String,
    val objectValue: String
)

data class NeuralBrainAnalysis(
    val mode: NeuralBrainMode,
    val prompt: String,
    val summary: String,
    val confidence: Int,
    val insights: List<String>,
    val suggestedTriples: List<SuggestedTriple>,
    val modelUsed: String
)

enum class VisionMode(
    val id: String,
    val title: String,
    val instruction: String
) {
    SCENE(
        id = "scene",
        title = "Scene & Object Vision",
        instruction = "Analyze the visual scene thoroughly: identify key objects, layout, context, and actionable observations."
    ),
    OCR(
        id = "ocr",
        title = "OCR & Document Reader",
        instruction = "Transcribe all visible text accurately while preserving structure, headings, numbers, and key takeaways."
    ),
    CODE_UI(
        id = "code_ui",
        title = "Code & UI Inspector",
        instruction = "Inspect the code, terminal output, diagram, or UI screenshot; extract readable code/bugs and propose concrete fixes."
    ),
    OBJECT_ID(
        id = "object_id",
        title = "Hardware & Spec Scanner",
        instruction = "Identify components, labels, symbols, or technical equipment shown and explain what each part does safely."
    );
}

data class CameraVisionResult(
    val mode: VisionMode,
    val prompt: String,
    val analysis: String,
    val modelUsed: String
)

enum class CodeLanguage(val title: String, val extension: String) {
    KOTLIN("Kotlin", "kt"),
    PYTHON("Python", "py"),
    TYPESCRIPT("TypeScript", "ts"),
    SQL("SQL", "sql"),
    BASH("Bash", "sh")
}

enum class CodeCopilotMode(val title: String, val directive: String) {
    GENERATE("Generate", "Write clean, production-ready, idiomatic code with complete imports, error handling, and concise comments."),
    DEBUG("Debug & Fix", "Diagnose bugs, race conditions, null-safety hazards, and logic errors, then output the corrected implementation."),
    REFACTOR("Refactor", "Refactor for clarity, modularity, performance, and immutability while preserving behavior."),
    TESTS("Unit Tests", "Write comprehensive, isolated unit tests covering happy paths, edge cases, and failure modes."),
    AUDIT("Security Audit", "Audit for injection, SSRF, secret leakage, unsafe deserialization, and concurrency risks, then provide hardened code.")
}

data class DefaultPluginSpec(
    val pluginKey: String,
    val name: String,
    val description: String,
    val category: String,
    val defaultEnabled: Boolean,
    val systemInstruction: String
)

val defaultBuiltInPlugins = listOf(
    DefaultPluginSpec(
        pluginKey = "calculator",
        name = "Precision Calculator",
        description = "Local deterministic math engine supporting parentheses, powers, and floating-point expressions.",
        category = "Compute",
        defaultEnabled = true,
        systemInstruction = "Prefer exact numeric verification when arithmetic expressions are evaluated."
    ),
    DefaultPluginSpec(
        pluginKey = "utc_clock",
        name = "Chronos UTC & Timezone",
        description = "Instant local and UTC time resolver with zero network latency.",
        category = "System",
        defaultEnabled = true,
        systemInstruction = "Use precise ISO-8601 UTC timestamps when discussing schedules or logs."
    ),
    DefaultPluginSpec(
        pluginKey = "unit_converter",
        name = "Metric & Data Converter",
        description = "Local converter for km/mi, C/F, kg/lb, and MB/GB storage units.",
        category = "Compute",
        defaultEnabled = true,
        systemInstruction = "Provide exact dual-unit equivalents when engineering measurements appear."
    ),
    DefaultPluginSpec(
        pluginKey = "json_inspector",
        name = "JSON & Regex Validator",
        description = "Validates and pretty-prints JSON payloads locally inside the command core.",
        category = "Developer",
        defaultEnabled = true,
        systemInstruction = "Format structured payloads strictly as valid JSON when requested."
    ),
    DefaultPluginSpec(
        pluginKey = "web_grounding",
        name = "Live Internet Grounding",
        description = "Enables HTTPS public web page extraction, Wikipedia/DuckDuckGo lookup, and Gemini Search Grounding.",
        category = "Internet",
        defaultEnabled = true,
        systemInstruction = "Cite retrieved public web URLs clearly and separate live web facts from uncertainty."
    ),
    DefaultPluginSpec(
        pluginKey = "kotlin_architect",
        name = "Kotlin & Compose Architect",
        description = "Injects Android Material 3, StateFlow, and coroutine safety standards into AI replies.",
        category = "Engineering",
        defaultEnabled = false,
        systemInstruction = "When writing Android code, use Kotlin, Jetpack Compose Material 3, StateFlow, and explicit coroutine CancellationException propagation."
    ),
    DefaultPluginSpec(
        pluginKey = "zero_trust_auditor",
        name = "Zero-Trust Security Guard",
        description = "Automatically checks answers for SSRF, hardcoded secrets, and permission compliance.",
        category = "Security",
        defaultEnabled = false,
        systemInstruction = "Highlight security boundaries, input validation, least-privilege permissions, and secret isolation in technical recommendations."
    )
)

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
    ),
    PromptTemplate(
        id = "web_briefing",
        title = "LIVE WEB BRIEFING",
        subtitle = "Retrieve and summarize an HTTPS resource or topic",
        promptPrefix = "Research the latest public information and summarize key takeaways with sources for: "
    )
)

data class SpecialistDeliverable(
    val agent: AgentRole,
    val status: String,
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
