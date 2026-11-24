package com.example.domain

import android.graphics.Bitmap
import com.example.data.local.ZaytharPreferences
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout

private const val MEDIA_TIMEOUT_MS = 60_000L

internal class AiVisionAndMediaEngine(
    private val aiProvider: () -> FirebaseAI
) {
    private val ai: FirebaseAI get() = aiProvider()

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
        val candidates = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")
        val langHint = if (prefs.autoMirrorLanguage) {
            "Reply naturally in the user's language (defaulting to $language if unclear)."
        } else {
            "Reply clearly in $language."
        }
        var lastError: Exception? = null
        for (modelName in candidates) {
            try {
                val visionModel = ai.generativeModel(
                    modelName = modelName,
                    generationConfig = generationConfig { temperature = 0.4f },
                    systemInstruction = content {
                        text(
                            "You are Zaythar Camera Eyes multimodal vision core. ${mode.instruction} " +
                                "Never invent text or details not visible in the image. $langHint"
                        )
                    }
                )
                val inputContent = content {
                    image(bitmap)
                    text(userPrompt)
                }
                val response = withTimeout(MEDIA_TIMEOUT_MS) {
                    visionModel.generateContent(inputContent)
                }
                val resultText = response.text.orEmpty().trim()
                if (resultText.isNotBlank()) {
                    return CameraVisionResult(
                        mode = mode,
                        prompt = userPrompt,
                        analysis = resultText,
                        modelUsed = modelName
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Camera Eyes returned an empty vision analysis.")
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
        val candidates = ZaytharSystemPrompts.candidateModelsForWorkspace(prefs.selectedProviderRoute)
        var lastError: Exception? = null
        for (modelName in candidates) {
            try {
                val studioModel = ai.generativeModel(
                    modelName = modelName,
                    generationConfig = generationConfig { temperature = 0.8f },
                    systemInstruction = content {
                        text(
                            "You are Zaythar Media Studio Creative Director. Produce a detailed production-ready $outputType " +
                                "in $style style in $language. Include visual composition, lighting, camera framing, color palette, and shot-by-shot cues."
                        )
                    }
                )
                val res = withTimeout(MEDIA_TIMEOUT_MS) {
                    studioModel.generateContent("Concept: $cleaned\nOutput format: $outputType\nStyle: $style")
                }
                val text = res.text.orEmpty().trim()
                if (text.isNotBlank()) return text
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("No creative concept returned.")
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

        val response = withTimeout(MEDIA_TIMEOUT_MS) {
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
}
