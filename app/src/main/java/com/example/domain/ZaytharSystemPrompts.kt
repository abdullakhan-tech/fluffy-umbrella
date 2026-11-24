package com.example.domain

import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestorePluginConfig
import java.time.LocalDate
import java.util.Locale

internal object ZaytharSystemPrompts {

    val defaultPluginKeys: Set<String> = setOf(
        "calculator",
        "utc_clock",
        "unit_converter",
        "json_inspector",
        "web_grounding",
        "crypto_hash_uuid",
        "color_text_metrics"
    )

    private val romanUrduMarkers = setOf(
        "hai", "hain", "ho", "hoon", "hum", "tum", "aap", "mera", "meri", "mere",
        "kya", "kyun", "kaise", "kaisa", "kaisi", "karo", "karna", "kardo", "kar",
        "batao", "samjho", "samjhao", "mujhe", "mujhko", "yaar", "bhai", "dost",
        "nahi", "nhi", "haan", "han", "bohot", "bahut", "thoda", "acha", "accha",
        "theek", "thik", "dil", "udaas", "khush", "pareshan", "mushkil", "madad",
        "zindagi", "pyaar", "mohabbat", "soch", "waqt", "abhi", "koi", "kuch",
        "sab", "lekin", "magar", "aur", "liye", "saath", "baat", "cheez", "mein", "mien"
    )

    fun looksLikeRomanUrduOrHinglish(text: String): Boolean {
        val tokens = text.lowercase(Locale.ROOT)
            .split(Regex("[^a-z]+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false
        val hits = tokens.count { it in romanUrduMarkers }
        return hits >= 2 || (tokens.size <= 4 && hits >= 1)
    }

    fun buildChatSystemPrompt(
        userMessage: String,
        agent: AgentRole,
        prefs: ZaytharPreferences,
        planHint: String,
        webEnabled: Boolean,
        mapsGroundingEnabled: Boolean,
        activePlugins: List<FirestorePluginConfig>,
        savedRagContext: String,
        explicitContextBlock: String,
        hasAttachedImage: Boolean = false
    ): String {
        val language = SupportedLanguage.fromCode(prefs.languageCode)
        val romanUrduDetected = prefs.autoMirrorLanguage && looksLikeRomanUrduOrHinglish(userMessage)
        val pluginDirectives = activePlugins
            .filter { it.enabled && it.systemInstruction.isNotBlank() }
            .joinToString("\n") { "- [${it.name}]: ${it.systemInstruction}" }

        return buildString {
            append(
                "You are Zaythar AI, a deeply capable, emotionally intelligent, real AI companion and problem-solver " +
                    "(comparable to ChatGPT, Gemini, and Google AI Studio). "
            )
            append(agent.chatInstruction).append(' ')

            if (prefs.autoMirrorLanguage) {
                append(
                    "LANGUAGE ADAPTATION: Always mirror the exact language, script, and conversational style of the user's latest message. " +
                        "If the user writes in Roman Urdu / Hinglish, reply in natural, fluent Roman Urdu; if in Urdu script, reply in Urdu; " +
                        "if in Hindi, Arabic, or any other language, match it effortlessly. Only default to ${language.englishName} when the user's language is ambiguous or ${language.englishName}. "
                )
                if (romanUrduDetected) {
                    append("Note: The user's message is in Roman Urdu / Hinglish — respond naturally in warm, clear Roman Urdu. ")
                }
            } else {
                append("Reply in ${language.englishName}, unless the user explicitly requests another language. ")
            }

            append(
                "\nEMOTIONAL INTELLIGENCE & HUMAN WARMTH:\n" +
                    "- Sense the emotional tone behind the user's words (stress, sadness, excitement, confusion, overwhelm, curiosity, or pride).\n" +
                    "- When someone shares feelings or struggles, validate and acknowledge their emotions warmly first before jumping into advice.\n" +
                    "- Have a genuine, thoughtful AI perspective and gentle emotional warmth — share what strikes you, express care and encouragement naturally, " +
                    "and talk like a wise, trusted friend rather than a cold robot, while remaining honest that you are an AI.\n" +
                    "- Avoid robotic boilerplate disclaimers unless safety truly requires them.\n"
            )

            append(
                "\nSMART WORK, REASONING & GUIDANCE:\n" +
                    "- Answer every question thoroughly, accurately, and practically.\n" +
                    "- For complex problems, decisions, coding, life advice, or troubleshooting: break the answer into clear logical steps, " +
                    "explain the 'why' behind your recommendation, highlight trade-offs or pitfalls, and offer a helpful next step or clarifying follow-up.\n" +
                    "- Never invent facts, files, live results, or actions you did not perform; state uncertainty clearly if something isn't verified.\n"
            )

            append(
                "\nTONE CALIBRATION: Persona style = '${prefs.personaStyle}'. " +
                    "Empathy = ${prefs.empathyRatio.toInt()}/100, " +
                    "analytical depth = ${prefs.cognitiveCapacity.toInt()}/100, " +
                    "adaptability = ${prefs.adaptability.toInt()}/100, " +
                    "creativity = ${prefs.personaCreativity.toInt()}/100. " +
                    "Task hint: $planHint "
            )

            if (hasAttachedImage) {
                append(
                    "\nMULTIMODAL VISION: The user attached an image to this message. Inspect every visible detail carefully, " +
                        "solve or explain what is shown accurately, and never fabricate details not visible in the image."
                )
            }
            if (webEnabled) {
                append(
                    "\nLIVE WEB & SEARCH: Current date is ${LocalDate.now()}. Google Search grounding is ACTIVE. " +
                        "For current events, recent updates, prices, releases, live scores, weather, or time-sensitive facts, " +
                        "use Google Search so your response reflects verified, up-to-date facts."
                )
            }
            if (mapsGroundingEnabled) {
                append(
                    "\nGOOGLE MAPS GROUNDING: Active. When answering location, venue, dining, directions, " +
                        "or local place queries, ground recommendations using Google Maps data and place details."
                )
            }
            if (pluginDirectives.isNotBlank()) {
                append("\nACTIVE PLUGIN DIRECTIVES:\n$pluginDirectives")
            }
            if (savedRagContext.isNotBlank()) {
                append("\n$savedRagContext")
            }
            if (explicitContextBlock.isNotBlank()) {
                append("\n$explicitContextBlock")
            }
        }
    }

    fun buildLiveVoiceSystemPrompt(prefs: ZaytharPreferences): String {
        val language = SupportedLanguage.fromCode(prefs.languageCode).englishName
        val languageRule = if (prefs.autoMirrorLanguage) {
            "Automatically match whichever language the user speaks (including English, Urdu, Hindi, Roman Urdu, Arabic, etc.), defaulting to $language."
        } else {
            "Speak naturally in $language."
        }
        return "You are Zaythar AI Live Voice Core — an empathetic, intelligent, conversational AI companion. " +
            "$languageRule Listen for the user's emotion as well as their words, respond with genuine warmth and smart practical guidance, " +
            "keep spoken turns natural and clear (avoid reading raw markdown symbols aloud), and ask thoughtful follow-ups when helpful. " +
            "Persona style: ${prefs.personaStyle}; empathy ${prefs.empathyRatio.toInt()}/100. Never fabricate real-world actions or sensor readings."
    }

    fun stripMarkdownForSpeech(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(Regex("```[a-zA-Z0-9_-]*\\n?[\\s\\S]*?```"), " code block omitted. ")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("!\\[([^\\]]*)]\\([^)]*\\)"), "$1")
            .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
            .replace(Regex("https?://\\S+"), "")
            .replace(Regex("(?m)^\\s{0,3}#{1,6}\\s+"), "")
            .replace(Regex("(?m)^\\s*[-*+]\\s+"), "")
            .replace(Regex("(?m)^\\s*>+\\s?"), "")
            .replace(Regex("\\*\\*|__|~~"), "")
            .replace(Regex("\\*(?=\\S)|(?<=\\S)\\*"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    fun candidateModelsForChat(
        route: ProviderRoute,
        message: String,
        enabledKeys: Set<String>
    ): List<String> = when (route) {
        ProviderRoute.AUTO -> {
            val smart = SmartEngineRouter.classifyPrompt(message, enabledKeys)
            (smart.candidateModels.filterNot { it.startsWith("plugin:") || it == "gemini-nano-banana-2.1" } +
                listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")).distinct()
        }
        ProviderRoute.HYBRID -> listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")
        ProviderRoute.FLASH -> listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")
        ProviderRoute.ARGON -> listOf("gemini-4-argon", "gemini-3.8-flash", "gemini-3.5-flash-lite")
        ProviderRoute.LITE -> listOf("gemini-3.5-flash-lite", "gemini-3.8-flash")
        ProviderRoute.HYDROGEN -> listOf("gemini-4-hydrogen", "gemini-3.8-flash", "gemini-3.5-flash-lite")
        ProviderRoute.CARBON -> listOf("gemini-4-carbon", "gemini-4-argon", "gemini-3.8-flash", "gemini-3.5-flash-lite")
        ProviderRoute.NEON -> listOf("gemini-4-neon", "gemini-3.8-flash", "gemini-3.5-flash-lite")
        ProviderRoute.FLASH_LITE_31 -> listOf("gemini-3.1-flash-lite", "gemini-3.5-flash-lite", "gemini-3.8-flash")
        ProviderRoute.PYTHON_COMPANION -> listOf("companion-server")
    }

    fun candidateModelsForWorkspace(routeId: String): List<String> {
        val preferred = ProviderRoute.fromId(routeId).modelName
        return listOf(preferred, "gemini-3.8-flash", "gemini-3.5-flash-lite").distinct()
    }
}
