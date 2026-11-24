package com.example.domain

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.example.data.local.ZaytharPreferences
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.UUID

private const val VOICE_TAG = "AiVoiceAndAudio"
private const val LIVE_CONNECT_TIMEOUT_MS = 25_000L
private const val TRANSCRIBE_TIMEOUT_MS = 60_000L
private const val MAX_AUDIO_BYTES = 18 * 1024 * 1024

enum class LiveVoiceStatus(val label: String) {
    IDLE("STANDBY"),
    CONNECTING("CONNECTING LIVE CORE…"),
    ACTIVE("LIVE BIDIRECTIONAL AUDIO"),
    STOPPING("CLOSING SESSION…"),
    ERROR("CONNECTION ERROR")
}

data class LiveTranscriptLine(
    val id: String = UUID.randomUUID().toString(),
    val speaker: String,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class AudioTranscriptionResult(
    val transcript: String,
    val modelUsed: String,
    val byteCount: Int
)

@OptIn(PublicPreviewAPI::class)
class LiveVoiceSessionManager(
    private val aiProvider: () -> FirebaseAI
) {
    private val _status = MutableStateFlow(LiveVoiceStatus.IDLE)
    val status: StateFlow<LiveVoiceStatus> = _status.asStateFlow()

    private val _transcripts = MutableStateFlow<List<LiveTranscriptLine>>(emptyList())
    val transcripts: StateFlow<List<LiveTranscriptLine>> = _transcripts.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var activeSession: LiveSession? = null
    private var sessionJob: Job? = null

    fun clearTranscripts() {
        _transcripts.value = emptyList()
    }

    @SuppressLint("MissingPermission")
    fun startLiveConversation(
        scope: CoroutineScope,
        voiceName: String,
        prefs: ZaytharPreferences,
        onToast: (String) -> Unit
    ) {
        if (_status.value == LiveVoiceStatus.CONNECTING || _status.value == LiveVoiceStatus.ACTIVE) {
            return
        }
        _errorMessage.value = null
        _status.value = LiveVoiceStatus.CONNECTING

        sessionJob?.cancel()
        sessionJob = scope.launch {
            try {
                val systemPrompt = ZaytharSystemPrompts.buildLiveVoiceSystemPrompt(prefs)
                val config = liveGenerationConfig {
                    responseModality = ResponseModality.AUDIO
                    inputAudioTranscription = AudioTranscriptionConfig()
                    outputAudioTranscription = AudioTranscriptionConfig()
                    speechConfig = SpeechConfig(voice = Voice(voiceName))
                }
                val liveModel = aiProvider().liveModel(
                    modelName = "gemini-3.8-live",
                    generationConfig = config,
                    systemInstruction = content { text(systemPrompt) }
                )
                val session = withTimeout(LIVE_CONNECT_TIMEOUT_MS) {
                    liveModel.connect()
                }
                activeSession = session
                _status.value = LiveVoiceStatus.ACTIVE
                appendLine("SYSTEM", "Connected to gemini-3.8-live ($voiceName voice). Speak naturally.")

                session.startAudioConversation(
                    functionCallHandler = null,
                    transcriptHandler = { inputTranscription, outputTranscription ->
                        val userSaid = inputTranscription?.text?.trim().orEmpty()
                        val modelSaid = outputTranscription?.text?.trim().orEmpty()
                        if (userSaid.isNotBlank()) appendLine("YOU", userSaid)
                        if (modelSaid.isNotBlank()) appendLine("ZAYTHAR", modelSaid)
                    },
                    goAwayHandler = { goAway ->
                        appendLine("SYSTEM", "Live session closing soon (${goAway.timeLeft}).")
                    },
                    enableInterruptions = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VOICE_TAG, "Gemini Live voice failed", e)
                val msg = e.localizedMessage ?: "Live voice session failed. Check App Check & network."
                _errorMessage.value = msg
                _status.value = LiveVoiceStatus.ERROR
                onToast(msg)
                closeSessionQuietly()
            }
        }
    }

    fun sendTextTurn(
        scope: CoroutineScope,
        rawText: String,
        onToast: (String) -> Unit
    ) {
        val text = rawText.trim()
        if (text.isEmpty() || text.length > 2000) return
        val session = activeSession
        if (session == null || session.isClosed()) {
            onToast("Start a Live Voice session first.")
            return
        }
        scope.launch {
            try {
                appendLine("YOU", text)
                session.send(text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(VOICE_TAG, "Failed sending text turn to live session", e)
                onToast(e.localizedMessage ?: "Could not send text message to Live session.")
            }
        }
    }

    fun stopLiveConversation(scope: CoroutineScope) {
        if (_status.value == LiveVoiceStatus.IDLE || _status.value == LiveVoiceStatus.STOPPING) {
            return
        }
        _status.value = LiveVoiceStatus.STOPPING
        val runningJob = sessionJob
        scope.launch {
            withContext(NonCancellable) {
                closeSessionQuietly()
                runningJob?.cancel()
                _status.value = LiveVoiceStatus.IDLE
            }
        }
    }

    private suspend fun closeSessionQuietly() {
        val session = activeSession
        activeSession = null
        if (session != null) {
            runCatching { session.stopAudioConversation() }
            runCatching { session.close() }
        }
    }

    private fun appendLine(speaker: String, text: String) {
        val current = _transcripts.value
        val last = current.lastOrNull()
        val updated = if (last != null && last.speaker == speaker && speaker != "SYSTEM" &&
            System.currentTimeMillis() - last.timestampMs < 2500L
        ) {
            current.dropLast(1) + last.copy(text = "${last.text} $text".trim())
        } else {
            (current + LiveTranscriptLine(speaker = speaker, text = text)).takeLast(60)
        }
        _transcripts.value = updated
    }
}

internal class AudioNoteRecorderAndTranscriber(
    private val aiProvider: () -> FirebaseAI
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(context: Context): Result<Unit> = runCatching {
        stopRecorderQuietly()
        val tempFile = File.createTempFile("zaythar_audio_", ".m4a", context.cacheDir)
        outputFile = tempFile
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context.applicationContext)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        recorder = mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(tempFile.absolutePath)
            prepare()
            start()
        }
    }

    suspend fun stopAndReadAudioBytes(): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val active = recorder ?: throw IllegalStateException("No active audio recording.")
            try {
                active.stop()
            } finally {
                runCatching { active.release() }
                recorder = null
            }
            val file = outputFile ?: throw IllegalStateException("Audio file missing.")
            outputFile = null
            try {
                if (!file.exists() || file.length() <= 0L) {
                    throw IllegalStateException("Recorded audio clip is empty.")
                }
                if (file.length() > MAX_AUDIO_BYTES) {
                    throw IllegalArgumentException("Recording exceeds 18 MB inline audio limit.")
                }
                file.readBytes()
            } finally {
                runCatching { file.delete() }
            }
        }
    }

    fun release() {
        stopRecorderQuietly()
        outputFile?.let { runCatching { it.delete() } }
        outputFile = null
    }

    private fun stopRecorderQuietly() {
        recorder?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        recorder = null
    }

    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4",
        languageName: String = "English"
    ): AudioTranscriptionResult {
        require(audioBytes.isNotEmpty() && audioBytes.size <= MAX_AUDIO_BYTES) {
            "Audio payload must be between 1 byte and 18 MB."
        }
        val candidates = listOf("gemini-3.5-transcribe", "gemini-3.8-flash")
        var lastError: Exception? = null

        for (modelName in candidates) {
            try {
                val model = aiProvider().generativeModel(modelName = modelName)
                val prompt = content {
                    inlineData(audioBytes, mimeType)
                    text(
                        "Transcribe the spoken audio accurately in $languageName (or the language spoken). " +
                            "Use clean punctuation and paragraph breaks, and add a brief 2-bullet summary at the end under '### Key Takeaways'."
                    )
                }
                val response = withTimeout(TRANSCRIBE_TIMEOUT_MS) {
                    model.generateContent(prompt)
                }
                val text = response.text.orEmpty().trim()
                if (text.isBlank()) throw IllegalStateException("Transcription model returned empty text.")
                return AudioTranscriptionResult(
                    transcript = text,
                    modelUsed = modelName,
                    byteCount = audioBytes.size
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(VOICE_TAG, "Transcription attempt failed on $modelName", e)
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Audio transcription failed.")
    }
}
