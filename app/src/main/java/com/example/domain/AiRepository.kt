package com.example.domain

import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestorePluginConfig
import com.example.data.remote.FirestoreRepository
import com.example.data.remote.FirestoreSessionAndBrainStore
import com.example.data.remote.FirestoreSessionMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import java.time.LocalDate

private const val TAG = "AiRepository"
private const val AI_TIMEOUT_MS = 60_000L

sealed interface ChatStreamEvent {
    data class Chunk(
        val accumulatedText: String,
        val modelLabel: String,
        val webSources: List<GroundedWebSource> = emptyList(),
        val searchQueries: List<String> = emptyList()
    ) : ChatStreamEvent

    data class Completed(
        val finalText: String,
        val modelLabel: String,
        val webSources: List<String> = emptyList(),
        val webSourceTitles: List<String> = emptyList(),
        val searchQueries: List<String> = emptyList(),
        val searchEntryPointHtml: String = ""
    ) : ChatStreamEvent
}

class AiRepository(
    private val firestoreRepo: FirestoreRepository,
    private val workspaceStore: FirestoreSessionAndBrainStore? = null,
    private val guestStore: GuestWorkspaceStore? = null
) {

    // Lazily access Firebase.ai only on first inference, never at construction time.
    private val ai by lazy { Firebase.ai }

    private val workspaceCapabilities by lazy {
        AiWorkspaceCapabilities(
            aiProvider = { ai },
            firestoreRepo = firestoreRepo,
            workspaceStore = workspaceStore,
            guestStore = guestStore,
            ragLoader = ::retrieveRagBundle
        )
    }

    private val voiceAudioRecorder by lazy { AudioNoteRecorderAndTranscriber { ai } }

    fun createLiveVoiceManager(): LiveVoiceSessionManager = LiveVoiceSessionManager { ai }

    fun startAudioNoteRecording(context: android.content.Context): Result<Unit> =
        voiceAudioRecorder.startRecording(context)

    suspend fun stopAudioNoteAndTranscribe(languageName: String): AudioTranscriptionResult {
        val bytes = voiceAudioRecorder.stopAndReadAudioBytes().getOrThrow()
        return voiceAudioRecorder.transcribeAudio(audioBytes = bytes, languageName = languageName)
    }

    fun releaseAudioRecorder() = voiceAudioRecorder.release()

    @OptIn(PublicPreviewAPI::class)
    private fun buildGroundingTools(
        enableGoogleSearch: Boolean,
        enableGoogleMaps: Boolean
    ): List<Tool>? = buildList {
        if (enableGoogleSearch) add(Tool.googleSearch())
        if (enableGoogleMaps) add(Tool.googleMaps())
    }.takeIf { it.isNotEmpty() }

    private fun buildModel(
        modelName: String,
        systemPrompt: String,
        temperature: Float,
        enableGoogleSearch: Boolean = false,
        enableGoogleMaps: Boolean = false
    ) = ai.generativeModel(
        modelName = modelName,
        generationConfig = generationConfig {
            this.temperature = temperature
        },
        tools = buildGroundingTools(enableGoogleSearch, enableGoogleMaps),
        systemInstruction = content { text(systemPrompt) }
    )

    private suspend fun retrieveRagBundle(
        query: String,
        recentHistory: List<FirestoreSessionMessage> = emptyList(),
        topK: Int = 6
    ): RagRetrievalBundle {
        val memories = guestStore?.memoryNotes?.value ?: firestoreRepo.getRecentMemoryNotes(limit = 60)
        val facts = guestStore?.knowledgeFacts?.value ?: firestoreRepo.getRecentKnowledgeFacts(limit = 100)
        return RagRetrievalEngine.retrieve(
            query = query,
            memories = memories,
            facts = facts,
            recentMessages = recentHistory,
            topK = topK
        )
    }

    fun streamChatReply(
        message: String,
        agent: AgentRole,
        prefs: ZaytharPreferences,
        recentHistory: List<FirestoreSessionMessage>,
        activePlugins: List<FirestorePluginConfig> = emptyList(),
        webConnectEnabled: Boolean = true,
        mapsGroundingEnabled: Boolean = prefs.mapsGroundingEnabled
    ): Flow<ChatStreamEvent> = flow {
        val enabledKeys = if (activePlugins.isEmpty()) {
            setOf(
                "calculator",
                "utc_clock",
                "unit_converter",
                "json_inspector",
                "web_grounding",
                "crypto_hash_uuid",
                "color_text_metrics"
            )
        } else {
            activePlugins.filter { it.enabled }.map { it.pluginKey }.toSet()
        }

        val toolResult = LocalToolRouter.routeTool(message, enabledKeys)
        if (toolResult != null) {
            emit(ChatStreamEvent.Completed(toolResult.result, "plugin:${toolResult.tool}"))
            return@flow
        }

        val webEnabled = isWebGroundingActive(
            webConnectEnabled = webConnectEnabled,
            enabledPluginKeys = enabledKeys
        )
        val explicitUrls = if (webEnabled) WebConnector.extractPublicHttpsUrls(message) else emptyList()
        val explicitSnippets = if (explicitUrls.isNotEmpty()) {
            explicitUrls.mapNotNull { WebConnector.fetchWebPageSnippet(it).getOrNull() }
        } else {
            emptyList()
        }
        val explicitContextBlock = if (explicitSnippets.isNotEmpty()) {
            WebGroundingResult(queryOrUrl = message.take(200), snippets = explicitSnippets).toContextBlock()
        } else {
            ""
        }

        val language = SupportedLanguage.fromCode(prefs.languageCode)
        val ragBundle = retrieveRagBundle(
            query = message,
            recentHistory = recentHistory.takeLast(6),
            topK = 6
        )
        val savedRagContext = ragBundle.toContextBlock(maxChars = 4800)
        val planHint = LocalToolRouter.reasoningHint(message)
        val pluginDirectives = activePlugins
            .filter { it.enabled && it.systemInstruction.isNotBlank() }
            .joinToString("\n") { "- [${it.name}]: ${it.systemInstruction}" }

        val systemPrompt = buildString {
            append("You are Zaythar AI Neural Core, a safe, helpful, multilingual personal intelligence engine. ")
            append(agent.chatInstruction)
            append(" Reply in ${language.englishName}, unless the user explicitly requests another language. ")
            append(
                "Persona style: ${prefs.personaStyle}. " +
                    "Empathy: ${prefs.empathyRatio.toInt()}/100; " +
                    "analytical depth: ${prefs.cognitiveCapacity.toInt()}/100; " +
                    "adaptability: ${prefs.adaptability.toInt()}/100; " +
                    "creativity: ${prefs.personaCreativity.toInt()}/100. "
            )
            append("Treat these as tone preferences, not permission to ignore safety or accuracy. Be clear, practical, and honest. ")
            append("Task approach: $planHint ")
            if (webEnabled) {
                append(
                    "\nCurrent date: ${LocalDate.now()}. Google Search grounding is ACTIVE. " +
                        "For current events, recent updates, prices, releases, live scores, weather, or any time-sensitive query, " +
                        "use Google Search so your response reflects verified, up-to-date facts."
                )
            }
            if (mapsGroundingEnabled) {
                append(
                    "\nGoogle Maps grounding is ACTIVE. When answering location, venue, dining, directions, " +
                        "or local place queries, ground recommendations using Google Maps data and place details."
                )
            }
            if (pluginDirectives.isNotBlank()) {
                append("\nActive Plugin Directives:\n$pluginDirectives")
            }
            if (savedRagContext.isNotBlank()) {
                append("\n$savedRagContext")
            }
            if (explicitContextBlock.isNotBlank()) {
                append("\n$explicitContextBlock")
            }
        }

        val historyContents: List<Content> = recentHistory
            .filter { it.text.isNotBlank() && it.text != "PROCESSING REQUEST…" }
            .takeLast(16)
            .map { item ->
                val role = if (item.role == "user") "user" else "model"
                content(role) { text(item.text.take(6000)) }
            }

        val route = ProviderRoute.fromId(prefs.selectedProviderRoute)
        val candidateModels = when (route) {
            ProviderRoute.AUTO -> {
                val smart = SmartEngineRouter.classifyPrompt(message, enabledKeys)
                smart.candidateModels
                    .filterNot { it.startsWith("plugin:") || it == "gemini-nano-banana-2.1" }
                    .ifEmpty { listOf("gemini-3.8-flash", "gemini-3.5-flash-lite") }
            }
            ProviderRoute.HYBRID -> listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")
            ProviderRoute.FLASH -> listOf("gemini-3.8-flash")
            ProviderRoute.ARGON -> listOf("gemini-4-argon", "gemini-3.8-flash")
            ProviderRoute.LITE -> listOf("gemini-3.5-flash-lite")
            ProviderRoute.HYDROGEN -> listOf("gemini-4-hydrogen", "gemini-3.8-flash")
            ProviderRoute.CARBON -> listOf("gemini-4-carbon", "gemini-4-argon", "gemini-3.8-flash")
            ProviderRoute.NEON -> listOf("gemini-4-neon", "gemini-3.8-flash")
            ProviderRoute.FLASH_LITE_31 -> listOf("gemini-3.1-flash-lite", "gemini-3.5-flash-lite")
        }

        val temperature = (0.35f + (prefs.creativityBias / 100f) * 0.65f).coerceIn(0.2f, 1.0f)
        var lastError: Exception? = null

        for (modelName in candidateModels) {
            val groundingAttempts = when {
                webEnabled && mapsGroundingEnabled -> listOf(true to true, true to false, false to false)
                webEnabled -> listOf(true to false, false to false)
                mapsGroundingEnabled -> listOf(false to true, false to false)
                else -> listOf(false to false)
            }
            for ((useGoogleSearch, useGoogleMaps) in groundingAttempts) {
                val accumulated = StringBuilder()
                val collectedSources = linkedMapOf<String, GroundedWebSource>()
                explicitSnippets.forEach { snippet ->
                    collectedSources[snippet.url] = GroundedWebSource(
                        title = snippet.title,
                        url = snippet.url
                    )
                }
                val collectedQueries = linkedSetOf<String>()
                var searchEntryPointHtml = ""
                val effectiveSystemPrompt = if (webEnabled && !useGoogleSearch && explicitContextBlock.isBlank()) {
                    val fallbackWeb = WebConnector.groundPromptWithInternet(message)
                    fallbackWeb.snippets.forEach { snippet ->
                        collectedSources[snippet.url] = GroundedWebSource(snippet.title, snippet.url)
                    }
                    val block = fallbackWeb.toContextBlock()
                    if (block.isNotBlank()) "$systemPrompt\n$block" else systemPrompt
                } else {
                    systemPrompt
                }
                try {
                    withTimeout(AI_TIMEOUT_MS) {
                        val model = buildModel(
                            modelName = modelName,
                            systemPrompt = effectiveSystemPrompt,
                            temperature = temperature,
                            enableGoogleSearch = useGoogleSearch,
                            enableGoogleMaps = useGoogleMaps
                        )
                        val chat = model.startChat(history = historyContents)
                        chat.sendMessageStream(message).collect { chunk ->
                            val metadata = chunk.candidates.firstOrNull()?.groundingMetadata
                            metadata?.webSearchQueries
                                ?.map { it.trim() }
                                ?.filter { it.isNotBlank() }
                                ?.forEach { if (collectedQueries.size < 8) collectedQueries.add(it) }
                            metadata?.groundingChunks?.forEach { gChunk ->
                                val web = gChunk.web
                                val uri = web?.uri?.trim().orEmpty()
                                if (uri.startsWith("https://") || uri.startsWith("http://")) {
                                    val title = web?.title?.trim().orEmpty()
                                    val existing = collectedSources[uri]
                                    if (collectedSources.size < 10 || existing != null) {
                                        collectedSources[uri] = GroundedWebSource(
                                            title = title.ifBlank { existing?.title.orEmpty() },
                                            url = uri
                                        )
                                    }
                                }
                                val maps = gChunk.maps
                                val mapPlaceId = maps?.placeId?.trim().orEmpty()
                                val rawMapUri = maps?.uri?.trim().orEmpty()
                                val mapUri = when {
                                    rawMapUri.startsWith("https://") || rawMapUri.startsWith("http://") -> rawMapUri
                                    mapPlaceId.isNotBlank() -> "https://www.google.com/maps/search/?api=1&query_place_id=$mapPlaceId"
                                    else -> ""
                                }
                                if (mapUri.isNotBlank() && (collectedSources.size < 10 || collectedSources.containsKey(mapUri))) {
                                    val mapTitle = maps?.title?.trim().orEmpty().ifBlank { "Google Maps Place" }
                                    collectedSources[mapUri] = GroundedWebSource(
                                        title = "Maps: $mapTitle",
                                        url = mapUri
                                    )
                                }
                            }
                            val rendered = metadata?.searchEntryPoint?.renderedContent?.trim().orEmpty()
                            if (rendered.isNotBlank()) {
                                searchEntryPointHtml = rendered.take(24_000)
                            }
                            val chunkText = chunk.text.orEmpty()
                            if (chunkText.isNotEmpty()) {
                                accumulated.append(chunkText)
                            }
                            if (chunkText.isNotEmpty() || collectedQueries.isNotEmpty() || collectedSources.isNotEmpty()) {
                                val liveLabel = when {
                                    useGoogleMaps && useGoogleSearch -> "$modelName+search+maps"
                                    useGoogleMaps -> "$modelName+maps"
                                    useGoogleSearch -> "$modelName+search"
                                    collectedSources.isNotEmpty() -> "$modelName+web"
                                    else -> modelName
                                }
                                emit(
                                    ChatStreamEvent.Chunk(
                                        accumulatedText = accumulated.toString(),
                                        modelLabel = liveLabel,
                                        webSources = collectedSources.values.toList().take(10),
                                        searchQueries = collectedQueries.toList()
                                    )
                                )
                            }
                        }
                    }
                    val finalText = accumulated.toString().trim()
                    if (finalText.isBlank()) {
                        throw IllegalStateException("Model returned an empty response.")
                    }
                    val finalSources = collectedSources.values.toList().take(10)
                    val finalLabel = when {
                        useGoogleMaps && useGoogleSearch -> "$modelName+search+maps"
                        useGoogleMaps -> "$modelName+maps"
                        useGoogleSearch && (collectedQueries.isNotEmpty() || finalSources.isNotEmpty() || searchEntryPointHtml.isNotBlank()) ->
                            "$modelName+search"
                        finalSources.isNotEmpty() -> "$modelName+web"
                        else -> modelName
                    }
                    emit(
                        ChatStreamEvent.Completed(
                            finalText = finalText,
                            modelLabel = finalLabel,
                            webSources = finalSources.map { it.url },
                            webSourceTitles = finalSources.map { it.title },
                            searchQueries = collectedQueries.toList(),
                            searchEntryPointHtml = searchEntryPointHtml
                        )
                    )
                    return@flow
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Chat attempt failed on $modelName (search=$useGoogleSearch)", e)
                    lastError = e
                    if (accumulated.isNotEmpty()) {
                        throw e
                    }
                }
            }
        }

        throw lastError ?: IllegalStateException("AI inference failed.")
    }

    suspend fun runNeuralBrainAnalysis(
        rawPrompt: String,
        mode: NeuralBrainMode,
        recentConversationContext: String,
        prefs: ZaytharPreferences
    ): NeuralBrainAnalysis = workspaceCapabilities.runNeuralBrainAnalysis(
        rawPrompt = rawPrompt,
        mode = mode,
        recentConversationContext = recentConversationContext,
        prefs = prefs
    )

    suspend fun analyzeImageWithVision(
        bitmap: Bitmap,
        rawPrompt: String,
        mode: VisionMode,
        prefs: ZaytharPreferences
    ): CameraVisionResult = workspaceCapabilities.analyzeImageWithVision(
        bitmap = bitmap,
        rawPrompt = rawPrompt,
        mode = mode,
        prefs = prefs
    )

    fun streamCodeCopilot(
        task: String,
        language: CodeLanguage,
        mode: CodeCopilotMode,
        existingCode: String,
        prefs: ZaytharPreferences,
        includeUnitTests: Boolean = true,
        strictSecurityAudit: Boolean = true
    ): Flow<ChatStreamEvent> = workspaceCapabilities.streamCodeCopilot(
        task = task,
        language = language,
        mode = mode,
        existingCode = existingCode,
        prefs = prefs,
        includeUnitTests = includeUnitTests,
        strictSecurityAudit = strictSecurityAudit
    )

    suspend fun generateStudioConcept(
        prompt: String,
        outputType: String,
        style: String,
        prefs: ZaytharPreferences
    ): String = workspaceCapabilities.generateStudioConcept(
        prompt = prompt,
        outputType = outputType,
        style = style,
        prefs = prefs
    )

    suspend fun generateStudioImage(
        prompt: String,
        style: String,
        aspectRatio: StudioAspectRatio,
        referenceBitmap: Bitmap? = null
    ): GeneratedImageResult = workspaceCapabilities.generateStudioImage(
        prompt = prompt,
        style = style,
        aspectRatio = aspectRatio,
        referenceBitmap = referenceBitmap
    )

    suspend fun probeGeminiEngine(): Pair<String, Long> {
        val modelName = "gemini-3.8-flash"
        val startNanos = System.nanoTime()
        val response = withTimeout(30_000L) {
            ai.generativeModel(
                modelName = modelName,
                generationConfig = generationConfig {
                    temperature = 0.0f
                    maxOutputTokens = 16
                }
            ).generateContent("Reply with exactly: ZAYTHAR_CORE_OK")
        }
        val text = response.text.orEmpty().trim()
        if (text.isBlank()) {
            throw IllegalStateException("Gemini probe returned an empty response.")
        }
        val elapsedMs = ((System.nanoTime() - startNanos) / 1_000_000L).coerceAtLeast(1L)
        return modelName to elapsedMs
    }

    suspend fun runMultiAgentTeam(
        rawTask: String,
        selectedAgents: List<AgentRole>,
        prefs: ZaytharPreferences
    ): MultiAgentRunResult = workspaceCapabilities.runMultiAgentTeam(
        rawTask = rawTask,
        selectedAgents = selectedAgents,
        prefs = prefs
    )

    companion object {
        internal fun isWebGroundingActive(
            webConnectEnabled: Boolean,
            enabledPluginKeys: Set<String>
        ): Boolean = webConnectEnabled && "web_grounding" in enabledPluginKeys
    }
}
