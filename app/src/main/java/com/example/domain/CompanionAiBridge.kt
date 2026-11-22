package com.example.domain

import android.util.Log
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.CompanionBackendClient
import com.example.data.remote.CompanionChatMessage
import com.example.data.remote.FirestoreSessionMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.FlowCollector

private const val TAG = "CompanionAiBridge"

object CompanionAiBridge {
    val client = CompanionBackendClient()

    suspend fun streamFromCompanion(
        prefs: ZaytharPreferences,
        message: String,
        recentHistory: List<FirestoreSessionMessage>,
        temperature: Float,
        collector: FlowCollector<ChatStreamEvent>
    ): Boolean {
        return try {
            val companionMessages = recentHistory
                .filter { it.text.isNotBlank() && it.text != "PROCESSING REQUEST…" }
                .takeLast(10)
                .map { CompanionChatMessage(role = if (it.role == "user") "user" else "assistant", content = it.text) }
                .plus(CompanionChatMessage(role = "user", content = message))

            var streamSuccess = false
            try {
                val stream = client.streamChatMessage(
                    baseUrl = prefs.companionBackendUrl,
                    messages = companionMessages,
                    temperature = temperature
                )
                val accumulated = StringBuilder()
                stream.collect { token ->
                    accumulated.append(token)
                    collector.emit(
                        ChatStreamEvent.Chunk(
                            accumulatedText = accumulated.toString(),
                            modelLabel = "python-companion-stream"
                        )
                    )
                }
                if (accumulated.isNotEmpty()) {
                    collector.emit(
                        ChatStreamEvent.Completed(
                            finalText = accumulated.toString(),
                            modelLabel = "python-companion-server"
                        )
                    )
                    streamSuccess = true
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Companion streaming encountered error, attempting direct chat fallback", e)
            }

            if (!streamSuccess) {
                val direct = client.sendChatMessage(
                    baseUrl = prefs.companionBackendUrl,
                    messages = companionMessages,
                    temperature = temperature
                )
                if (direct.isSuccess) {
                    val resp = direct.getOrThrow()
                    collector.emit(ChatStreamEvent.Chunk(accumulatedText = resp.reply, modelLabel = resp.modelUsed))
                    collector.emit(ChatStreamEvent.Completed(finalText = resp.reply, modelLabel = resp.modelUsed))
                    true
                } else {
                    false
                }
            } else {
                true
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Companion backend unavailable, falling back to core Gemini models", e)
            false
        }
    }
}
