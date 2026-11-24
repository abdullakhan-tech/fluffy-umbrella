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
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "AiRepository"
private const val AI_TIMEOUT_MS = 60_000L

sealed interface ChatStreamEvent {
    data class Chunk(
        val accumulatedText: String,
        val modelLabel: String,
        val webSources: List<GroundedWebSource> = emptyList(),
        val searchQueries: List<String> = emptyList(),
        val accumulatedThoughts: String = ""
    ) : ChatStreamEvent

    data class Completed(
        val finalText: String,
        val modelLabel: String,
        val webSources: List<String> = emptyList(),
        val webSourceTitles: List<String> = emptyList(),
        val searchQueries: List<String> = emptyList(),
        val searchEntryPointHtml: String = "",
        val thoughtSummary: String = ""
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
        enableGoogleMaps: Boolean = false,
        thinkingLevel: ThinkingLevel? = null
    ) = ai.generativeModel(
        modelName = modelName,
        generationConfig = generationConfig {
            this.temperature = temperature
            if (thinkingLevel != null) {
                this.thinkingConfig = thinkingConfig {
                    this.includeThoughts = true
                    this.thinkingLevel = thinkingLevel
                }
            }
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
        mapsGroundingEnabled: Boolean = prefs.mapsGroundingEnabled,
        attachedImage: Bitmap? = null
    ): Flow<ChatStreamEvent> = flow {
        val enabledKeys = if (activePlugins.isEmpty()) {
            ZaytharSystemPrompts.defaultPluginKeys
        } else {
            activePlugins.filter { it.enabled }.map { it.pluginKey }.toSet()
        }

        if (attachedImage == null) {
            val toolResult = LocalToolRouter.routeTool(message, enabledKeys)
            if (toolResult != null) {
                emit(ChatStreamEvent.Completed(toolResult.result, "plugin:${toolResult.tool}"))
                return@flow
            }
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

        val ragBundle = retrieveRagBundle(
            query = message,
            recentHistory = recentHistory.takeLast(6),
            topK = 6
        )
        val savedRagContext = ragBundle.toContextBlock(maxChars = 4800)
        val planHint = LocalToolRouter.reasoningHint(message)
        val systemPrompt = ZaytharSystemPrompts.buildChatSystemPrompt(
            userMessage = message,
            agent = agent,
            prefs = prefs,
            planHint = planHint,
            webEnabled = webEnabled,
            mapsGroundingEnabled = mapsGroundingEnabled,
            activePlugins = activePlugins,
            savedRagContext = savedRagContext,
            explicitContextBlock = explicitContextBlock,
            hasAttachedImage = attachedImage != null
        )

        val historyContents: List<Content> = recentHistory
            .filter { it.text.isNotBlank() && it.text != "PROCESSING REQUEST…" }
            .takeLast(16)
            .map { item ->
                val role = if (item.role == "user") "user" else "model"
                content(role) { text(item.text.take(6000)) }
            }

        val route = ProviderRoute.fromId(prefs.selectedProviderRoute)
        val candidateModels = ZaytharSystemPrompts.candidateModelsForChat(route, message, enabledKeys)
        val preferredThinkingLevel: ThinkingLevel? = if (prefs.thinkingEnabled) {
            if (route in setOf(ProviderRoute.ARGON, ProviderRoute.CARBON, ProviderRoute.NEON) ||
                prefs.cognitiveCapacity >= 80f
            ) {
                ThinkingLevel.HIGH
            } else {
                ThinkingLevel.MEDIUM
            }
        } else {
            null
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
            val supportsThinkingHint = "lite" !in modelName.lowercase()
            val thinkingAttempts: List<ThinkingLevel?> = if (preferredThinkingLevel != null && supportsThinkingHint) {
                listOf(preferredThinkingLevel, null)
            } else {
                listOf(null)
            }

            for ((useGoogleSearch, useGoogleMaps) in groundingAttempts) {
                val effectiveSystemPrompt = if (webEnabled && !useGoogleSearch && explicitContextBlock.isBlank()) {
                    val fallbackWeb = WebConnector.groundPromptWithInternet(message)
                    val block = fallbackWeb.toContextBlock()
                    if (block.isNotBlank()) "$systemPrompt\n$block" else systemPrompt
                } else {
                    systemPrompt
                }
                val fallbackWebSnippets = if (webEnabled && !useGoogleSearch && explicitContextBlock.isBlank()) {
                    WebConnector.groundPromptWithInternet(message).snippets
                } else {
                    emptyList()
                }

                for (thinkingLevel in thinkingAttempts) {
                    val accumulated = StringBuilder()
                    val accumulatedThoughts = StringBuilder()
                    val collectedSources = linkedMapOf<String, GroundedWebSource>()
                    (explicitSnippets + fallbackWebSnippets).forEach { snippet ->
                        collectedSources[snippet.url] = GroundedWebSource(snippet.title, snippet.url)
                    }
                    val collectedQueries = linkedSetOf<String>()
                    var searchEntryPointHtml = ""

                    try {
                        withTimeout(AI_TIMEOUT_MS) {
                            val model = buildModel(
                                modelName = modelName,
                                systemPrompt = effectiveSystemPrompt,
                                temperature = temperature,
                                enableGoogleSearch = useGoogleSearch,
                                enableGoogleMaps = useGoogleMaps,
                                thinkingLevel = thinkingLevel
                            )
                            val chat = model.startChat(history = historyContents)
                            val requestStream = if (attachedImage != null) {
                                chat.sendMessageStream(
                                    content {
                                        image(attachedImage)
                                        text(message)
                                    }
                                )
                            } else {
                                chat.sendMessageStream(message)
                            }

                            requestStream.collect { chunk ->
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

                                val thoughtDelta = chunk.thoughtSummary?.takeIf { it.isNotBlank() }
                                    ?: chunk.candidates.firstOrNull()?.content?.parts
                                        ?.filterIsInstance<TextPart>()
                                        ?.filter { it.isThought }
                                        ?.joinToString("") { it.text }
                                        .orEmpty()
                                if (thoughtDelta.isNotEmpty() && !accumulatedThoughts.endsWith(thoughtDelta)) {
                                    accumulatedThoughts.append(thoughtDelta)
                                }

                                val chunkText = chunk.text.orEmpty()
                                if (chunkText.isNotEmpty()) {
                                    accumulated.append(chunkText)
                                }

                                if (chunkText.isNotEmpty() || thoughtDelta.isNotEmpty() ||
                                    collectedQueries.isNotEmpty() || collectedSources.isNotEmpty()
                                ) {
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
                                            searchQueries = collectedQueries.toList(),
                                            accumulatedThoughts = accumulatedThoughts.toString().trim()
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
                                searchEntryPointHtml = searchEntryPointHtml,
                                thoughtSummary = accumulatedThoughts.toString().trim().take(4000)
                            )
                        )
                        return@flow
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Chat attempt failed on $modelName (search=$useGoogleSearch, thinking=$thinkingLevel)", e)
                        lastError = e
                        if (accumulated.isNotEmpty()) {
                            throw e
                        }
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
