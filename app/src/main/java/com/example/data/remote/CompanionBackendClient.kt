package com.example.data.remote

import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "CompanionBackendClient"
private const val CONNECT_TIMEOUT_MS = 6000
private const val READ_TIMEOUT_MS = 30000

data class CompanionHealth(
    val status: String,
    val app: String,
    val version: String,
    val environment: String
)

data class CompanionChatMessage(
    val role: String,
    val content: String
)

data class CompanionChatResponse(
    val reply: String,
    val modelUsed: String,
    val provider: String,
    val executionTimeMs: Double,
    val sessionId: String?
)

data class CompanionOrbState(
    val state: String,
    val pulseRateHz: Double,
    val glowColorHex: String,
    val activeConnections: Int
)

data class CompanionAgentTaskResponse(
    val status: String,
    val toolUsed: String,
    val summary: String,
    val executionTimeMs: Double
)

class CompanionBackendClient(
    private val defaultBaseUrl: String = "http://10.0.2.2:8000"
) {

    private fun normalizeUrl(baseUrl: String): String {
        val normalized = baseUrl.trim().trimEnd('/')
        val uri = Uri.parse(normalized)
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        val localDevelopmentHosts = setOf("localhost", "127.0.0.1", "::1", "10.0.2.2")
        val secure = scheme == "https"
        val permittedDebugLoopback = BuildConfig.DEBUG && scheme == "http" && host in localDevelopmentHosts
        require(secure || permittedDebugLoopback) {
            "The companion backend must use HTTPS. HTTP is allowed only for debug loopback development."
        }
        require(!uri.userInfo.let { !it.isNullOrBlank() }) {
            "Backend URLs must not embed usernames or passwords."
        }
        return normalized
    }

    private suspend fun applyFirebaseAuthHeaders(connection: HttpURLConnection) {
        val user = FirebaseAuth.getInstance().currentUser
            ?: error("Sign in to Zaythar AI before using the companion backend.")
        val idToken = user.getIdToken(false).await().token
            ?: error("Firebase authentication token is unavailable.")
        val appCheckToken = FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token
            ?: error("Firebase App Check attestation is unavailable.")
        connection.setRequestProperty("Authorization", "Bearer $idToken")
        connection.setRequestProperty("X-Firebase-AppCheck", appCheckToken)
    }

    suspend fun checkHealth(baseUrl: String = defaultBaseUrl): Result<CompanionHealth> =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetUrl = "${normalizeUrl(baseUrl)}/health"
                val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = CONNECT_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val code = conn.responseCode
                    if (code != HttpURLConnection.HTTP_OK) {
                        error("Health check failed with HTTP $code")
                    }
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    CompanionHealth(
                        status = json.optString("status", "unknown"),
                        app = json.optString("app", "Zaythar AI Companion Backend"),
                        version = json.optString("version", "1.0.0"),
                        environment = json.optString("environment", "production")
                    )
                } finally {
                    conn.disconnect()
                }
            }
        }

    suspend fun getOrbVisualState(baseUrl: String = defaultBaseUrl): Result<CompanionOrbState> =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetUrl = "${normalizeUrl(baseUrl)}/api/v1/orb/animation-state"
                val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = CONNECT_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val code = conn.responseCode
                    if (code != HttpURLConnection.HTTP_OK) {
                        error("Orb state request failed with HTTP $code")
                    }
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    CompanionOrbState(
                        state = json.optString("state", "idle"),
                        pulseRateHz = json.optDouble("pulse_rate_hz", 1.2),
                        glowColorHex = json.optString("glow_color_hex", "#00D2FF"),
                        activeConnections = json.optInt("active_connections", 1)
                    )
                } finally {
                    conn.disconnect()
                }
            }
        }

    suspend fun sendChatMessage(
        baseUrl: String = defaultBaseUrl,
        messages: List<CompanionChatMessage>,
        provider: String = "auto",
        model: String? = null,
        temperature: Float = 0.7f,
        sessionId: String? = null
    ): Result<CompanionChatResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUrl = "${normalizeUrl(baseUrl)}/api/v1/chat"
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            applyFirebaseAuthHeaders(conn)

            val payload = JSONObject().apply {
                val msgArray = JSONArray()
                messages.forEach { m ->
                    msgArray.put(JSONObject().apply {
                        put("role", m.role)
                        put("content", m.content)
                    })
                }
                put("messages", msgArray)
                put("provider", provider)
                if (!model.isNullOrBlank()) put("model", model)
                put("temperature", temperature.toDouble())
                if (!sessionId.isNullOrBlank()) put("session_id", sessionId)
            }

            try {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
                val code = conn.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                    error("Chat failed: $err")
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                CompanionChatResponse(
                    reply = json.optString("reply", ""),
                    modelUsed = json.optString("model_used", "companion-local"),
                    provider = json.optString("provider", provider),
                    executionTimeMs = json.optDouble("execution_time_ms", 0.0),
                    sessionId = json.optString("session_id").takeIf { it.isNotBlank() }
                )
            } finally {
                conn.disconnect()
            }
        }
    }

    fun streamChatMessage(
        baseUrl: String = defaultBaseUrl,
        messages: List<CompanionChatMessage>,
        provider: String = "auto",
        model: String? = null,
        temperature: Float = 0.7f
    ): Flow<String> = flow {
        val targetUrl = "${normalizeUrl(baseUrl)}/api/v1/chat/stream"
        val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "text/event-stream")
        }

        applyFirebaseAuthHeaders(conn)

        val payload = JSONObject().apply {
            val msgArray = JSONArray()
            messages.forEach { m ->
                msgArray.put(JSONObject().apply {
                    put("role", m.role)
                    put("content", m.content)
                })
            }
            put("messages", msgArray)
            put("provider", provider)
            if (!model.isNullOrBlank()) put("model", model)
            put("temperature", temperature.toDouble())
        }

        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                throw IllegalStateException("Stream request failed with HTTP $code: $err")
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: break
                if (currentLine.startsWith("data:")) {
                    val rawData = currentLine.removePrefix("data:").trim()
                    if (rawData.isNotEmpty()) {
                        val token = runCatching {
                            val json = JSONObject(rawData)
                            json.optString("token", "")
                        }.getOrDefault(rawData)
                        if (token.isNotEmpty()) {
                            emit(token)
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun executeAgentTask(
        baseUrl: String = defaultBaseUrl,
        task: String,
        context: String? = null,
        toolHint: String? = null
    ): Result<CompanionAgentTaskResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUrl = "${normalizeUrl(baseUrl)}/api/v1/agent/task"
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            applyFirebaseAuthHeaders(conn)

            val payload = JSONObject().apply {
                put("task", task)
                if (!context.isNullOrBlank()) put("context", context)
                if (!toolHint.isNullOrBlank()) put("tool_hint", toolHint)
            }

            try {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
                val code = conn.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                    error("Agent task failed: $err")
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                CompanionAgentTaskResponse(
                    status = json.optString("status", "unknown"),
                    toolUsed = json.optString("tool_used", "none"),
                    summary = json.optString("summary", ""),
                    executionTimeMs = json.optDouble("execution_time_ms", 0.0)
                )
            } finally {
                conn.disconnect()
            }
        }
    }
}
