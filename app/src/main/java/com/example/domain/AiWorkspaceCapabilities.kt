package com.example.domain

import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreRepository
import com.example.data.remote.FirestoreSessionAndBrainStore
import com.example.data.remote.FirestoreSessionMessage
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val WORKSPACE_AI_TAG = "AiWorkspaceCapabilities"
private const val WORKSPACE_TIMEOUT_MS = 60_000L
private const val MAX_MULTI_TASK_CHARS = 12_000
private const val MAX_SPECIALIST_OUTPUT_CHARS = 7_000

internal class AiWorkspaceCapabilities(
    private val aiProvider: () -> FirebaseAI,
    private val firestoreRepo: FirestoreRepository,
    private val workspaceStore: FirestoreSessionAndBrainStore?,
    private val guestStore: GuestWorkspaceStore? = null,
    private val ragLoader: suspend (String, List<FirestoreSessionMessage>, Int) -> RagRetrievalBundle
) {
    private val ai: FirebaseAI get() = aiProvider()

    suspend fun runNeuralBrainAnalysis(
        rawPrompt: String,
        mode: NeuralBrainMode,
        recentConversationContext: String,
        prefs: ZaytharPreferences
    ): NeuralBrainAnalysis {
        val prompt = rawPrompt.trim()
        require(prompt.isNotEmpty() && prompt.length <= 4000) {
            "Input must contain 1 to 4,000 characters."
        }
        val language = SupportedLanguage.fromCode(prefs.languageCode).englishName
        val ragBundle = ragLoader(prompt, emptyList(), 8)
        val ragContextBlock = ragBundle.toContextBlock(maxChars = 5000)
        val modelName = ProviderRoute.fromId(prefs.selectedProviderRoute).modelName

        val tripleSchema = Schema.obj(
            properties = mapOf(
                "subject" to Schema.string(description = "Entity or topic subject"),
                "predicate" to Schema.string(description = "Concise relationship verb"),
                "objectValue" to Schema.string(description = "Related entity, attribute, or outcome")
            )
        )
        val brainSchema = Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(description = "Comprehensive executive analysis in $language (supports Markdown)"),
                "confidence" to Schema.integer(description = "Calibrated confidence score 0 to 100"),
                "reasoningSteps" to Schema.array(
                    items = Schema.string(description = "Step-by-step cognitive synthesis phase"),
                    description = "Ordered reasoning steps from first principles"
                ),
                "insights" to Schema.array(
                    items = Schema.string(description = "Actionable high-signal insight"),
                    description = "Key synthesized insights"
                ),
                "risksAndGaps" to Schema.array(
                    items = Schema.string(description = "Potential blind spot, failure mode, or security risk"),
                    description = "Risks, assumptions, and failure modes"
                ),
                "actionPlan" to Schema.array(
                    items = Schema.string(description = "Concrete prioritized next action item"),
                    description = "Prioritized execution roadmap steps"
                ),
                "suggestedTriples" to Schema.array(
                    items = tripleSchema,
                    description = "Up to 6 canonical knowledge graph triples"
                )
            )
        )

        val structuredModel = ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                temperature = 0.30f
                responseMimeType = "application/json"
                responseSchema = brainSchema
            },
            systemInstruction = content {
                text(
                    "You are Zaythar AI Brain Cognitive Core. Mode: ${mode.title}. ${mode.directive} " +
                        "Ground findings in the provided [RAG-#] knowledge when applicable without inventing facts. " +
                        "Return strict JSON matching the schema in $language."
                )
            }
        )

        val fullInput = buildString {
            append("MODE: ${mode.title}\n")
            if (ragContextBlock.isNotBlank()) {
                append("\n$ragContextBlock\n")
            }
            if (recentConversationContext.isNotBlank()) {
                append("\nRECENT SESSION CONTEXT:\n${recentConversationContext.take(2500)}\n")
            }
            append("\nTARGET INPUT:\n$prompt")
        }

        val response = withTimeout(WORKSPACE_TIMEOUT_MS) {
            structuredModel.generateContent(fullInput)
        }
        val rawJson = response.text.orEmpty().trim()
        if (rawJson.isBlank()) throw IllegalStateException("AI Brain returned an empty response.")

        val obj = JSONObject(rawJson)
        val summary = obj.optString("summary", "").trim().ifBlank { "Analysis complete." }
        val confidence = obj.optInt("confidence", 80).coerceIn(0, 100)
        val reasoningSteps = extractStringArray(obj.optJSONArray("reasoningSteps"), limit = 8, maxLen = 350)
        val insights = extractStringArray(obj.optJSONArray("insights"), limit = 10, maxLen = 400)
        val risksAndGaps = extractStringArray(obj.optJSONArray("risksAndGaps"), limit = 6, maxLen = 350)
        val actionPlan = extractStringArray(obj.optJSONArray("actionPlan"), limit = 6, maxLen = 350)

        val triplesArr = obj.optJSONArray("suggestedTriples")
        val triples = buildList {
            if (triplesArr != null) {
                for (i in 0 until minOf(triplesArr.length(), 8)) {
                    val item = triplesArr.optJSONObject(i) ?: continue
                    val s = item.optString("subject", "").trim().take(280)
                    val p = item.optString("predicate", "").trim().take(280)
                    val o = item.optString("objectValue", "").trim().take(280)
                    if (s.isNotBlank() && p.isNotBlank() && o.isNotBlank()) {
                        add(SuggestedTriple(s, p, o))
                    }
                }
            }
        }

        runCatching {
            if (guestStore != null) {
                guestStore.saveBrainRun(
                    mode = mode.id,
                    prompt = prompt,
                    summary = summary,
                    confidence = confidence,
                    insights = insights,
                    suggestedTriplesCount = triples.size,
                    modelUsed = modelName
                )
            } else {
                workspaceStore?.saveBrainRun(
                    mode = mode.id,
                    prompt = prompt,
                    summary = summary,
                    confidence = confidence,
                    insights = insights,
                    suggestedTriplesCount = triples.size,
                    modelUsed = modelName
                )
            }
        }

        val citations = ragBundle.chunks.mapIndexed { idx, c ->
            "[RAG-${idx + 1}] ${c.sourceType.tag} (${c.scorePercent}%): ${c.title}"
        }

        return NeuralBrainAnalysis(
            mode = mode,
            prompt = prompt,
            summary = summary,
            confidence = confidence,
            insights = insights,
            suggestedTriples = triples,
            modelUsed = modelName,
            reasoningSteps = reasoningSteps,
            risksAndGaps = risksAndGaps,
            actionPlan = actionPlan,
            ragCitations = citations
        )
    }

    suspend fun analyzeImageWithVision(
        bitmap: Bitmap,
        rawPrompt: String,
        mode: VisionMode,
        prefs: ZaytharPreferences
    ): CameraVisionResult {
        val userPrompt = rawPrompt.trim().ifBlank {
            when (mode) {
                VisionMode.SCENE -> "Describe what you see and highlight key details."
                VisionMode.OCR -> "Extract all text clearly from this image."
                VisionMode.CODE_UI -> "Inspect the code, terminal, or interface shown in this image."
                VisionMode.OBJECT_ID -> "Identify the objects or technical components shown."
            }
        }
        val language = SupportedLanguage.fromCode(prefs.languageCode).englishName
        val modelName = "gemini-3.8-flash"
        val visionModel = ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig { temperature = 0.4f },
            systemInstruction = content {
                text(
                    "You are Zaythar Camera Eyes multimodal vision core. ${mode.instruction} " +
                        "Never invent text or details not visible in the image. Reply clearly in $language."
                )
            }
        )

        val inputContent = content {
            image(bitmap)
            text(userPrompt)
        }

        val response = withTimeout(WORKSPACE_TIMEOUT_MS) {
            visionModel.generateContent(inputContent)
        }
        val resultText = response.text.orEmpty().trim()
        if (resultText.isBlank()) {
            throw IllegalStateException("Camera Eyes returned an empty vision analysis.")
        }
        return CameraVisionResult(
            mode = mode,
            prompt = userPrompt,
            analysis = resultText,
            modelUsed = modelName
        )
    }

    fun streamCodeCopilot(
        task: String,
        language: CodeLanguage,
        mode: CodeCopilotMode,
        existingCode: String,
        prefs: ZaytharPreferences,
        includeUnitTests: Boolean = true,
        strictSecurityAudit: Boolean = true
    ): Flow<ChatStreamEvent> = flow {
        val cleanedTask = task.trim()
        require(cleanedTask.isNotEmpty() || existingCode.isNotBlank()) {
            "Enter a coding task or paste source code."
        }
        val modelName = ProviderRoute.fromId(prefs.selectedProviderRoute).modelName
        val ragQuery = "$cleanedTask ${language.title} ${mode.title}".trim()
        val ragContext = ragLoader(ragQuery, emptyList(), 4).toContextBlock(maxChars = 2400)

        val systemPrompt = buildString {
            append("You are Zaythar Code Copilot, a principal software engineer specializing in ${language.title}. ")
            append("Operation mode: ${mode.title}. ${mode.directive} ")
            append(
                "Output well-formatted Markdown with clear headings: " +
                    "### Architecture & Design, ### Implementation (complete ${language.title} fenced code block with imports, input validation, and error handling), " +
                    "and ### Complexity & Edge Cases. "
            )
            if (strictSecurityAudit) {
                append("Enforce zero hardcoded secrets, safe bounds checking, injection/SSRF protection, and coroutine cancellation safety. ")
            }
            if (includeUnitTests && mode != CodeCopilotMode.TESTS) {
                append("Include a ### Verification Tests section with deterministic unit tests covering normal and edge cases. ")
            }
            append("Never fabricate unexecuted runtime test logs.")
            if (ragContext.isNotBlank()) {
                append("\n\n$ragContext")
            }
        }
        val input = buildString {
            if (cleanedTask.isNotBlank()) append("TASK:\n$cleanedTask\n\n")
            if (existingCode.isNotBlank()) append("SOURCE CODE (${language.title}):\n```${language.extension}\n${existingCode.take(10_000)}\n```")
        }

        val accumulated = StringBuilder()
        withTimeout(WORKSPACE_TIMEOUT_MS) {
            val model = ai.generativeModel(
                modelName = modelName,
                generationConfig = generationConfig { temperature = 0.22f },
                systemInstruction = content { text(systemPrompt) }
            )
            model.generateContentStream(input).collect { chunk ->
                val txt = chunk.text.orEmpty()
                if (txt.isNotEmpty()) {
                    accumulated.append(txt)
                    emit(ChatStreamEvent.Chunk(accumulated.toString(), modelName))
                }
            }
        }
        val final = accumulated.toString().trim()
        if (final.isBlank()) throw IllegalStateException("Code Copilot returned an empty response.")
        emit(ChatStreamEvent.Completed(final, modelName))
    }

    suspend fun generateStudioConcept(
        prompt: String,
        outputType: String,
        style: String,
        prefs: ZaytharPreferences
    ): String {
        val cleaned = prompt.trim()
        require(cleaned.isNotEmpty()) { "Enter a creative prompt first." }
        val language = SupportedLanguage.fromCode(prefs.languageCode).englishName
        val modelName = ProviderRoute.fromId(prefs.selectedProviderRoute).modelName
        val studioModel = ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig { temperature = 0.8f },
            systemInstruction = content {
                text(
                    "You are Zaythar Media Studio Creative Director. Produce a detailed production-ready $outputType in $style style in $language. Include visual composition, lighting, camera framing, color palette, and shot-by-shot cues."
                )
            }
        )
        val res = withTimeout(WORKSPACE_TIMEOUT_MS) {
            studioModel.generateContent("Concept: $cleaned\nOutput format: $outputType\nStyle: $style")
        }
        return res.text.orEmpty().trim().ifBlank { "No creative concept returned." }
    }

    suspend fun generateStudioImage(
        prompt: String,
        style: String,
        aspectRatio: StudioAspectRatio,
        referenceBitmap: Bitmap? = null
    ): GeneratedImageResult {
        val cleaned = prompt.trim()
        require(cleaned.isNotEmpty() && cleaned.length <= 2000) {
            "Image prompt must contain 1 to 2,000 characters."
        }
        val modelName = "gemini-nano-banana-2.1"
        val styledPrompt = "$cleaned. Visual style: $style. Crisp composition, high detail."

        val imgConfig = ImageConfig.builder().apply {
            when (aspectRatio) {
                StudioAspectRatio.SQUARE_1_1 -> setAspectRatio(AspectRatio.SQUARE_1x1)
                StudioAspectRatio.LANDSCAPE_16_9 -> setAspectRatio(AspectRatio.LANDSCAPE_16x9)
                StudioAspectRatio.PORTRAIT_9_16 -> setAspectRatio(AspectRatio.PORTRAIT_9x16)
            }
            setImageSize(ImageSize.SIZE_1K)
        }.build()

        val imageModel = ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = imgConfig
            }
        )

        val response = withTimeout(WORKSPACE_TIMEOUT_MS) {
            if (referenceBitmap != null) {
                imageModel.generateContent(
                    content {
                        image(referenceBitmap)
                        text(styledPrompt)
                    }
                )
            } else {
                imageModel.generateContent(styledPrompt)
            }
        }

        val bitmap = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
            ?.image
            ?: throw IllegalStateException("Image engine returned no bitmap. Verify Firebase Blaze plan & App Check.")

        return GeneratedImageResult(
            bitmap = bitmap,
            prompt = cleaned,
            modelUsed = modelName,
            aspectRatio = aspectRatio,
            isEdit = referenceBitmap != null
        )
    }

    suspend fun runMultiAgentTeam(
        rawTask: String,
        selectedAgents: List<AgentRole>,
        prefs: ZaytharPreferences
    ): MultiAgentRunResult {
        val task = rawTask.trim()
        require(task.isNotEmpty() && task.length <= MAX_MULTI_TASK_CHARS) {
            "Task must contain 1 to $MAX_MULTI_TASK_CHARS characters."
        }
        val uniqueAgents = selectedAgents.distinct()
        require(uniqueAgents.isNotEmpty() && uniqueAgents.size <= 4) {
            "Select 1 to 4 specialist roles."
        }

        val language = SupportedLanguage.fromCode(prefs.languageCode).englishName
        val modelName = ProviderRoute.fromId(prefs.selectedProviderRoute).modelName
        val ragContext = ragLoader(task, emptyList(), 6).toContextBlock(maxChars = 4000)

        val deliverables = coroutineScope {
            uniqueAgents.map { role ->
                async {
                    try {
                        val specialistSystem = buildString {
                            append("You are one specialist in Zaythar AI's multi-agent team. ")
                            append(role.specialistPrompt)
                            append(" Work only on the user's task. Do not claim actions you did not perform. ")
                            append("Write your deliverable in $language. ")
                            append("Return a concise, structured deliverable with findings, recommendations, and uncertainties.")
                        }
                        val contextBlock = if (ragContext.isNotBlank()) "$ragContext\n\n" else ""
                        val response = withTimeout(WORKSPACE_TIMEOUT_MS) {
                            ai.generativeModel(
                                modelName = modelName,
                                generationConfig = generationConfig { temperature = 0.60f },
                                systemInstruction = content { text(specialistSystem) }
                            ).generateContent(contextBlock + task)
                        }
                        val text = response.text.orEmpty().take(MAX_SPECIALIST_OUTPUT_CHARS)
                        if (text.isBlank()) {
                            SpecialistDeliverable(role, "failed", "Specialist returned an empty response.", modelName)
                        } else {
                            SpecialistDeliverable(role, "completed", text, modelName)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(WORKSPACE_AI_TAG, "Specialist ${role.id} failed", e)
                        SpecialistDeliverable(
                            agent = role,
                            status = "failed",
                            output = e.localizedMessage ?: e.javaClass.simpleName,
                            modelUsed = modelName
                        )
                    }
                }
            }.awaitAll()
        }

        val completed = deliverables.filter { it.status == "completed" }
        if (completed.isEmpty()) {
            val firstError = deliverables.firstOrNull()?.output ?: "All specialists failed."
            throw IllegalStateException("All selected specialists failed: $firstError")
        }

        val evidence = completed.joinToString("\n\n") { item ->
            "[${item.agent.title.uppercase()} SPECIALIST]\n${item.output}"
        }
        val coordinatorSystem = buildString {
            append("You are Zaythar AI's lead coordinator. Synthesize specialist deliverables into one useful answer. ")
            append("Resolve contradictions explicitly, preserve useful detail, identify uncertainty, and give prioritized next steps. ")
            append("Do not invent verification or claim that code was executed. Do not reveal private chain-of-thought. ")
            append("Write in $language.")
        }
        val coordinatorInput = buildString {
            append(ragContext.ifBlank { "RAG CONTEXT: (none)" })
            append("\n\nUSER TASK:\n")
            append(task)
            append("\n\nSPECIALIST DELIVERABLES:\n")
            append(evidence)
        }

        val synthesis = withTimeout(WORKSPACE_TIMEOUT_MS) {
            ai.generativeModel(
                modelName = modelName,
                generationConfig = generationConfig { temperature = 0.50f },
                systemInstruction = content { text(coordinatorSystem) }
            ).generateContent(coordinatorInput)
        }
        val finalReply = synthesis.text.orEmpty().ifBlank {
            "Coordinator returned no final response."
        }

        runCatching {
            val summary = ("Multi-agent task: ${task.take(700)}\nCoordinator result: ${finalReply.take(1100)}").take(2000)
            if (guestStore != null) {
                guestStore.addMemoryNote(summary)
            } else {
                firestoreRepo.addMemoryNote(summary)
            }
        }

        return MultiAgentRunResult(
            reply = finalReply,
            modelUsed = modelName,
            deliverables = deliverables,
            completedCount = completed.size,
            totalCount = uniqueAgents.size
        )
    }

    private fun extractStringArray(arr: JSONArray?, limit: Int, maxLen: Int): List<String> = buildList {
        if (arr != null) {
            for (i in 0 until minOf(arr.length(), limit)) {
                val item = arr.optString(i, "").trim()
                if (item.isNotBlank()) add(item.take(maxLen))
            }
        }
    }
}
