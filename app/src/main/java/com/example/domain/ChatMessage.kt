package com.example.domain

import androidx.core.net.toUri
import com.example.data.remote.FirestoreSessionMessage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

enum class MessageAuthor {
    USER,
    GEMINI
}

data class GroundedWebSource(
    val title: String,
    val url: String
) {
    fun displayLabel(): String {
        val cleanTitle = title.trim()
        if (cleanTitle.isNotBlank()) return cleanTitle
        val host = runCatching { url.toUri().host }.getOrNull()?.trim().orEmpty()
        return when {
            host.isNotBlank() && !host.equals("vertexaisearch.cloud.google.com", ignoreCase = true) -> host
            host.equals("vertexaisearch.cloud.google.com", ignoreCase = true) -> "Google Search Source"
            else -> url.take(28)
        }
    }
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestampMillis: Long = System.currentTimeMillis(),
    val agentId: String = AgentRole.GENERAL.id,
    val modelLabel: String = "",
    val hasImage: Boolean = false,
    val webSources: List<String> = emptyList(),
    val webSourceTitles: List<String> = emptyList(),
    val searchQueries: List<String> = emptyList(),
    val searchEntryPointHtml: String = "",
    val thoughtSummary: String = "",
    val isPending: Boolean = false
) {
    val author: MessageAuthor
        get() = if (isFromUser) MessageAuthor.USER else MessageAuthor.GEMINI

    fun resolvedSources(): List<GroundedWebSource> =
        webSources.mapIndexed { index, url ->
            GroundedWebSource(
                title = webSourceTitles.getOrNull(index).orEmpty(),
                url = url
            )
        }

    fun formattedTime(zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (timestampMillis <= 0L) return ""
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(zoneId)
            .format(TIME_FORMATTER)
    }

    private companion object {
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    }
}

fun FirestoreSessionMessage.toChatMessage(): ChatMessage {
    val millis = updatedAt?.toDate()?.time ?: createdAt?.toDate()?.time ?: 0L
    return ChatMessage(
        id = id.ifBlank { "${role}_${text.hashCode()}" },
        text = text,
        isFromUser = role == "user",
        timestampMillis = millis,
        agentId = agent,
        modelLabel = modelLabel,
        hasImage = hasImage,
        webSources = webSources,
        webSourceTitles = webSourceTitles,
        searchQueries = searchQueries,
        searchEntryPointHtml = searchEntryPointHtml,
        isPending = false
    )
}
