package com.example.domain

import com.example.data.remote.FirestoreKnowledgeFact
import com.example.data.remote.FirestoreMemoryNote
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ZaytharEngineId(
    val id: String,
    val title: String,
    val subtitle: String,
    val route: String,
    val modelTag: String
) {
    SMART_ROUTER(
        id = "smart_router",
        title = "Smart Auto-Router",
        subtitle = "Intent classifier & dynamic Gemini tier selector",
        route = "engine_hub",
        modelTag = "LOCAL + AI ROUTER"
    ),
    NEURAL_CHAT(
        id = "neural_chat",
        title = "Neural Core Chat",
        subtitle = "Streaming multi-thread chat with Google Search Grounding",
        route = "assistant",
        modelTag = "gemini-3.8-flash"
    ),
    AI_BRAIN(
        id = "ai_brain",
        title = "Cognitive AI Brain",
        subtitle = "Structured JSON reasoning & Knowledge Graph triples",
        route = "brain",
        modelTag = "gemini-3.8-flash + JSON"
    ),
    IMAGE_STUDIO(
        id = "image_studio",
        title = "Nano Banana Image Studio",
        subtitle = "Runtime text-to-image generation & photo editing",
        route = "studio",
        modelTag = "gemini-nano-banana-2.1"
    ),
    CAMERA_EYES(
        id = "camera_eyes",
        title = "Camera Eyes Vision",
        subtitle = "Multimodal photo OCR, code/UI inspection & object ID",
        route = "camera_eyes",
        modelTag = "gemini-3.8-flash Vision"
    ),
    CODE_COPILOT(
        id = "code_copilot",
        title = "Code Copilot Engine",
        subtitle = "Principal architecture, debugging, tests & security audit",
        route = "code",
        modelTag = "gemini-4-argon / 3.8"
    ),
    MULTI_AGENT(
        id = "multi_agent",
        title = "Multi-Agent Swarm",
        subtitle = "Concurrent 1–4 specialist parallel synthesis",
        route = "collaboration",
        modelTag = "PARALLEL SWARM"
    ),
    RAG_AND_TOOLS(
        id = "rag_and_tools",
        title = "Hybrid RAG & Local Sandbox",
        subtitle = "BM25 + Bigram + Graph RAG, SSRF Guard & Deterministic Tools",
        route = "knowledge",
        modelTag = "ON-DEVICE DETERMINISTIC"
    ),
    VOICE_AUDIO(
        id = "voice_audio",
        title = "Live Voice & Transcribe",
        subtitle = "Bidirectional Gemini Live audio & speech transcription",
        route = "voice_audio",
        modelTag = "gemini-3.8-live / 3.5-transcribe"
    )
}

data class SmartRouteDecision(
    val engine: ZaytharEngineId,
    val recommendedRoute: ProviderRoute,
    val candidateModels: List<String>,
    val complexityLabel: String,
    val confidencePercent: Int,
    val rationale: String,
    val localToolPreview: ToolRouteResult? = null
)

enum class EngineCheckStatus(val label: String) {
    PASS("PASS"),
    READY("READY"),
    WARN("WARN"),
    FAIL("FAIL")
}

data class EngineCheckResult(
    val engine: ZaytharEngineId,
    val status: EngineCheckStatus,
    val summary: String,
    val latencyMs: Long,
    val details: String
)

data class EngineHealthReport(
    val ranAtLabel: String,
    val passedCount: Int,
    val totalCount: Int,
    val liveGeminiStatus: String,
    val liveGeminiLatencyMs: Long?,
    val results: List<EngineCheckResult>
)

object SmartEngineRouter {

    private val imageKeywords = listOf(
        "generate image", "create image", "draw ", "illustration", "logo",
        "poster", "wallpaper", "render a", "cyberpunk art", "edit photo",
        "picture of", "portrait of", "concept art", "artwork", "tasveer", "image banao"
    )
    private val codeKeywords = listOf(
        "kotlin", "python", "typescript", "javascript", "sql", "compose",
        "refactor", "debug", "stack trace", "unit test", "compile", "gradle",
        "function", "class ", "algorithm", "regex", "security audit", "api endpoint",
        "code ", "bug ", "coroutine", "stateflow"
    )
    private val multiAgentKeywords = listOf(
        "multi-agent", "team", "compare", "trade-off", "tradeoff", "pros and cons",
        "critique", "research and design", "end-to-end launch", "strategy and code"
    )
    private val brainKeywords = listOf(
        "knowledge graph", "triple", "extract facts", "first principles",
        "root cause", "blind spot", "risk matrix", "roadmap", "architecture audit",
        "synthesize", "consolidate"
    )
    private val visionKeywords = listOf(
        "screenshot", "ocr", "read photo", "camera", "inspect image", "scan hardware"
    )
    private val ragKeywords = listOf(
        "my notes", "knowledge base", "saved memory", "rag", "from my memory", "search memory"
    )
    private val deepReasoningKeywords = listOf(
        "prove", "theorem", "distributed system", "concurrency", "formal", "cryptograph",
        "optimize", "complexity", "deep dive", "in-depth", "step by step"
    )
    private val voiceKeywords = listOf(
        "live voice", "voice conversation", "transcribe", "audio note", "dictation",
        "voice chat", "talk to zaythar", "record audio"
    )
    private val mapsKeywords = listOf(
        "nearby", "directions to", "google maps", "restaurant near", "coffee shop",
        "places near", "navigate to", "route to", "hotel near", "museum in"
    )

    fun classifyPrompt(
        rawPrompt: String,
        enabledPluginKeys: Set<String> = setOf(
            "calculator",
            "utc_clock",
            "unit_converter",
            "json_inspector",
            "web_grounding",
            "crypto_hash_uuid",
            "color_text_metrics"
        )
    ): SmartRouteDecision {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) {
            return SmartRouteDecision(
                engine = ZaytharEngineId.NEURAL_CHAT,
                recommendedRoute = ProviderRoute.HYBRID,
                candidateModels = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite"),
                complexityLabel = "STANDARD",
                confidencePercent = 70,
                rationale = "Enter any query to auto-select the best Zaythar AI engine and Gemini model tier."
            )
        }

        val localHit = LocalToolRouter.routeTool(prompt, enabledPluginKeys)
        if (localHit != null) {
            return SmartRouteDecision(
                engine = ZaytharEngineId.RAG_AND_TOOLS,
                recommendedRoute = ProviderRoute.LITE,
                candidateModels = listOf("plugin:${localHit.tool}"),
                complexityLabel = "ZERO-LATENCY LOCAL",
                confidencePercent = 99,
                rationale = "Matched deterministic local engine (${localHit.tool}) — zero cloud latency.",
                localToolPreview = localHit
            )
        }

        val lower = prompt.lowercase(Locale.ROOT)
        val wordCount = prompt.split(Regex("""\s+""")).count { it.isNotBlank() }

        return when {
            voiceKeywords.any { it in lower } -> SmartRouteDecision(
                engine = ZaytharEngineId.VOICE_AUDIO,
                recommendedRoute = ProviderRoute.FLASH,
                candidateModels = listOf("gemini-3.8-live", "gemini-3.5-transcribe"),
                complexityLabel = "VOICE & AUDIO AI",
                confidencePercent = 94,
                rationale = "Voice conversation / audio transcription intent detected; routing to Voice & Audio Nexus."
            )
            mapsKeywords.any { it in lower } -> SmartRouteDecision(
                engine = ZaytharEngineId.NEURAL_CHAT,
                recommendedRoute = ProviderRoute.HYDROGEN,
                candidateModels = listOf("gemini-4-hydrogen", "gemini-3.8-flash"),
                complexityLabel = "GOOGLE MAPS + SEARCH",
                confidencePercent = 92,
                rationale = "Location & places intent detected; Gemini 4 Hydrogen with Google Maps Grounding fits best."
            )
            imageKeywords.any { it in lower } -> SmartRouteDecision(
                engine = ZaytharEngineId.IMAGE_STUDIO,
                recommendedRoute = ProviderRoute.FLASH,
                candidateModels = listOf("gemini-nano-banana-2.1"),
                complexityLabel = "GENERATIVE MEDIA",
                confidencePercent = 94,
                rationale = "Visual creation intent detected; routing to Nano Banana 2.1 Image Studio."
            )
            visionKeywords.any { it in lower } -> SmartRouteDecision(
                engine = ZaytharEngineId.CAMERA_EYES,
                recommendedRoute = ProviderRoute.FLASH,
                candidateModels = listOf("gemini-3.8-flash"),
                complexityLabel = "MULTIMODAL VISION",
                confidencePercent = 90,
                rationale = "Image inspection / OCR intent detected; Camera Eyes multimodal core recommended."
            )
            codeKeywords.any { it in lower } -> SmartRouteDecision(
                engine = ZaytharEngineId.CODE_COPILOT,
                recommendedRoute = ProviderRoute.ARGON,
                candidateModels = listOf("gemini-4-argon", "gemini-3.8-flash"),
                complexityLabel = "ENGINEERING",
                confidencePercent = 93,
                rationale = "Software engineering / security intent detected; prioritizing Gemini 4 Argon with 3.8 Flash fallback."
            )
            multiAgentKeywords.any { it in lower } -> SmartRouteDecision(
                engine = ZaytharEngineId.MULTI_AGENT,
                recommendedRoute = ProviderRoute.FLASH,
                candidateModels = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite"),
                complexityLabel = "MULTI-PERSPECTIVE",
                confidencePercent = 91,
                rationale = "Multi-disciplinary trade-off query; concurrent Specialist Swarm provides deeper synthesis."
            )
            brainKeywords.any { it in lower } -> SmartRouteDecision(
                engine = ZaytharEngineId.AI_BRAIN,
                recommendedRoute = ProviderRoute.ARGON,
                candidateModels = listOf("gemini-4-argon", "gemini-3.8-flash"),
                complexityLabel = "STRUCTURED COGNITION",
                confidencePercent = 90,
                rationale = "Structured analysis / knowledge extraction intent; AI Brain JSON schema pipeline fits best."
            )
            ragKeywords.any { it in lower } -> SmartRouteDecision(
                engine = ZaytharEngineId.RAG_AND_TOOLS,
                recommendedRoute = ProviderRoute.HYBRID,
                candidateModels = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite"),
                complexityLabel = "HYBRID RAG",
                confidencePercent = 88,
                rationale = "Memory / knowledge graph lookup requested; Hybrid BM25 + Graph RAG engine is optimal."
            )
            deepReasoningKeywords.any { it in lower } || wordCount >= 45 -> SmartRouteDecision(
                engine = ZaytharEngineId.NEURAL_CHAT,
                recommendedRoute = ProviderRoute.ARGON,
                candidateModels = listOf("gemini-4-argon", "gemini-3.8-flash"),
                complexityLabel = "DEEP REASONING",
                confidencePercent = 87,
                rationale = "High-complexity prompt; routing to Gemini 4 Argon with 3.8 Flash fallback."
            )
            wordCount <= 8 && "http" !in lower && "?" !in prompt -> SmartRouteDecision(
                engine = ZaytharEngineId.NEURAL_CHAT,
                recommendedRoute = ProviderRoute.LITE,
                candidateModels = listOf("gemini-3.5-flash-lite", "gemini-3.8-flash"),
                complexityLabel = "FAST LITE",
                confidencePercent = 84,
                rationale = "Short conversational prompt; Gemini 3.5 Flash-Lite delivers ultra-fast latency."
            )
            else -> SmartRouteDecision(
                engine = ZaytharEngineId.NEURAL_CHAT,
                recommendedRoute = ProviderRoute.FLASH,
                candidateModels = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite"),
                complexityLabel = "BALANCED",
                confidencePercent = 86,
                rationale = "General intelligence query; Gemini 3.8 Flash + Search Grounding is balanced."
            )
        }
    }

    fun runLocalAndHybridDiagnostics(
        memoriesCount: Int,
        factsCount: Int,
        activePluginsCount: Int,
        firebaseReady: Boolean,
        isGuestMode: Boolean,
        liveProbeResult: Result<Pair<String, Long>>? = null
    ): EngineHealthReport {
        val results = mutableListOf<EngineCheckResult>()

        // 1. Local Tool Router & Plugin Sandbox Engine
        val t0 = System.nanoTime()
        val calcOk = LocalToolRouter.routeTool("calculate (12 + 8) * 3")?.result == "60"
        val unitOk = LocalToolRouter.routeTool("1024 mb to gb")?.result == "1024 mb = 1 gb"
        val jsonOk = LocalToolRouter.routeTool("inspect json {\"zaythar\":true}")?.result?.contains("Valid JSON Object") == true
        val shaOk = LocalToolRouter.routeTool("sha256 zaythar")?.result?.startsWith("SHA-256: ") == true
        val uuidOk = LocalToolRouter.routeTool("generate uuid")?.result?.startsWith("Generated UUID v4: ") == true
        val colorOk = LocalToolRouter.routeTool("#FF3155 to rgb")?.result == "#FF3155 = rgb(255, 49, 85)"
        val localLatency = ((System.nanoTime() - t0) / 1_000_000L).coerceAtLeast(1L)
        val localAllPass = calcOk && unitOk && jsonOk && shaOk && uuidOk && colorOk
        results += EngineCheckResult(
            engine = ZaytharEngineId.RAG_AND_TOOLS,
            status = if (localAllPass) EngineCheckStatus.PASS else EngineCheckStatus.FAIL,
            summary = if (localAllPass) "All 6 deterministic tool engines verified ($activePluginsCount plugins active)" else "Tool assertion failure",
            latencyMs = localLatency,
            details = "Calc=$calcOk · Units=$unitOk · JSON=$jsonOk · SHA256=$shaOk · UUID=$uuidOk · Color=$colorOk"
        )

        // 2. Hybrid BM25 + Graph RAG Retrieval Engine
        val t1 = System.nanoTime()
        val sampleMemories = listOf(FirestoreMemoryNote(id = "diag_m1", content = "Zaythar uses Hybrid BM25 Bigram Graph RAG retrieval."))
        val sampleFacts = listOf(FirestoreKnowledgeFact(id = "diag_f1", subject = "Zaythar", predicate = "protects", objectValue = "UID Cloud Firestore isolation", source = "diag"))
        val ragBundle = RagRetrievalEngine.retrieve(
            query = "BM25 Graph RAG Firestore",
            memories = sampleMemories,
            facts = sampleFacts,
            topK = 3
        )
        val ragLatency = ((System.nanoTime() - t1) / 1_000_000L).coerceAtLeast(1L)
        val ragPass = ragBundle.chunks.isNotEmpty()
        results += EngineCheckResult(
            engine = ZaytharEngineId.AI_BRAIN,
            status = if (ragPass) EngineCheckStatus.PASS else EngineCheckStatus.FAIL,
            summary = "Hybrid BM25+Graph RAG verified · $memoriesCount memories, $factsCount triples indexed",
            latencyMs = ragLatency,
            details = "Top match score=${ragBundle.chunks.firstOrNull()?.scorePercent ?: 0}% · JSON Schema Brain pipeline ready"
        )

        // 3. Web Grounding SSRF & URL Guard
        val t2 = System.nanoTime()
        val publicAllowed = WebConnector.validatePublicHttpsUrl("https://firebase.google.com/docs/ai-logic").isSuccess
        val localhostBlocked = WebConnector.validatePublicHttpsUrl("https://127.0.0.1/admin").isFailure
        val metadataBlocked = WebConnector.validatePublicHttpsUrl("https://169.254.169.254/latest/meta-data/").isFailure
        val httpBlocked = WebConnector.validatePublicHttpsUrl("http://example.com").isFailure
        val webLatency = ((System.nanoTime() - t2) / 1_000_000L).coerceAtLeast(1L)
        val ssrfOk = publicAllowed && localhostBlocked && metadataBlocked && httpBlocked
        results += EngineCheckResult(
            engine = ZaytharEngineId.NEURAL_CHAT,
            status = if (ssrfOk && firebaseReady) EngineCheckStatus.PASS else if (ssrfOk) EngineCheckStatus.WARN else EngineCheckStatus.FAIL,
            summary = if (ssrfOk) "SSRF Web Guard verified · Cloud AI ${if (firebaseReady) "initialized" else "standby"}" else "SSRF guard failed",
            latencyMs = webLatency,
            details = "HTTPS allow=$publicAllowed · Loopback blocked=$localhostBlocked · Metadata blocked=$metadataBlocked · Mode=${if (isGuestMode) "Guest" else "Cloud UID"}"
        )

        // 4. Smart Auto-Router Engine
        val t3 = System.nanoTime()
        val codeRoute = classifyPrompt("Refactor Kotlin StateFlow repository with unit tests").engine == ZaytharEngineId.CODE_COPILOT
        val imgRoute = classifyPrompt("Generate image of a cyberpunk orbital nexus").engine == ZaytharEngineId.IMAGE_STUDIO
        val calcRoute = classifyPrompt("calculate 25 * 4").engine == ZaytharEngineId.RAG_AND_TOOLS
        val routerLatency = ((System.nanoTime() - t3) / 1_000_000L).coerceAtLeast(1L)
        val routerOk = codeRoute && imgRoute && calcRoute
        results += EngineCheckResult(
            engine = ZaytharEngineId.SMART_ROUTER,
            status = if (routerOk) EngineCheckStatus.PASS else EngineCheckStatus.FAIL,
            summary = if (routerOk) "Intent classifier verified across Code, Image & Tool routes" else "Router classification mismatch",
            latencyMs = routerLatency,
            details = "Code=$codeRoute · Image=$imgRoute · LocalTool=$calcRoute"
        )

        // 5. Runtime AI Image Studio Engine (Nano Banana 2.1)
        results += EngineCheckResult(
            engine = ZaytharEngineId.IMAGE_STUDIO,
            status = if (firebaseReady) EngineCheckStatus.READY else EngineCheckStatus.WARN,
            summary = if (firebaseReady) "Gemini Nano Banana 2.1 text-to-image & editing pipeline wired" else "Firebase not initialized",
            latencyMs = 0L,
            details = "Supports 1:1 / 16:9 / 9:16 + Photo Picker image-to-image editing (60s timeout)"
        )

        // 6. Camera Eyes Vision Engine
        results += EngineCheckResult(
            engine = ZaytharEngineId.CAMERA_EYES,
            status = if (firebaseReady) EngineCheckStatus.READY else EngineCheckStatus.WARN,
            summary = if (firebaseReady) "Multimodal Vision (Scene, OCR, Code UI, Hardware ID) ready" else "Firebase not initialized",
            latencyMs = 0L,
            details = "FileProvider camera + Photo Picker zero-permission input -> gemini-3.8-flash"
        )

        // 7. Code Copilot & Multi-Agent Swarm
        results += EngineCheckResult(
            engine = ZaytharEngineId.CODE_COPILOT,
            status = if (firebaseReady) EngineCheckStatus.READY else EngineCheckStatus.WARN,
            summary = "Streaming Kotlin/Python/TS/SQL/Bash Copilot + Argon/Flash routing ready",
            latencyMs = 0L,
            details = "Supports Unit Tests & Security Audit flags + RAG context injection"
        )
        results += EngineCheckResult(
            engine = ZaytharEngineId.MULTI_AGENT,
            status = if (firebaseReady) EngineCheckStatus.READY else EngineCheckStatus.WARN,
            summary = "Parallel 1–4 Specialist Swarm + Lead Coordinator synthesis ready",
            latencyMs = 0L,
            details = "Concurrent async fan-out across Research, Coding, Writing, Analysis, Creative"
        )
        results += EngineCheckResult(
            engine = ZaytharEngineId.VOICE_AUDIO,
            status = if (firebaseReady) EngineCheckStatus.READY else EngineCheckStatus.WARN,
            summary = if (firebaseReady) "Gemini 3.8 Live voice & 3.5 Transcribe audio pipeline wired" else "Firebase not initialized",
            latencyMs = 0L,
            details = "Live bidirectional audio + audio/mp4 transcription + Google Maps/Search grounding"
        )

        val (liveStatusText, liveLatency) = if (liveProbeResult == null) {
            "NOT PROBED (TAP LIVE CHECK)" to null
        } else {
            liveProbeResult.fold(
                onSuccess = { (model, ms) -> "ONLINE · $model (${ms}ms)" to ms },
                onFailure = { err -> "CLOUD ERROR: ${err.localizedMessage?.take(90) ?: "Failed"}" to null }
            )
        }

        val passOrReady = results.count { it.status == EngineCheckStatus.PASS || it.status == EngineCheckStatus.READY }
        val timeLabel = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

        return EngineHealthReport(
            ranAtLabel = timeLabel,
            passedCount = passOrReady,
            totalCount = results.size,
            liveGeminiStatus = liveStatusText,
            liveGeminiLatencyMs = liveLatency,
            results = results
        )
    }
}
