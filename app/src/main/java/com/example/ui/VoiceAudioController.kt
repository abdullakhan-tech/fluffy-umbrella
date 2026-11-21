package com.example.ui

import android.content.Context
import android.util.Log
import com.example.data.local.ZaytharPreferences
import com.example.domain.AiRepository
import com.example.domain.AudioTranscriptionResult
import com.example.domain.LiveTranscriptLine
import com.example.domain.LiveVoiceStatus
import com.example.domain.SupportedLanguage
import com.example.domain.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val VOICE_CTRL_TAG = "VoiceAudioCtrl"

internal class VoiceAudioController(
    private val scope: CoroutineScope,
    private val aiRepo: AiRepository
) {
    private val liveManager = aiRepo.createLiveVoiceManager()

    val liveVoiceStatus: StateFlow<LiveVoiceStatus> = liveManager.status
    val liveTranscripts: StateFlow<List<LiveTranscriptLine>> = liveManager.transcripts
    val liveVoiceError: StateFlow<String?> = liveManager.errorMessage

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0L)
    val recordingSeconds: StateFlow<Long> = _recordingSeconds.asStateFlow()

    private val _transcribeState = MutableStateFlow<UiState<AudioTranscriptionResult>>(UiState.Idle)
    val transcribeState: StateFlow<UiState<AudioTranscriptionResult>> = _transcribeState.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    fun startLiveVoice(voiceName: String, prefs: ZaytharPreferences, onToast: (String) -> Unit) {
        liveManager.startLiveConversation(scope, voiceName, prefs, onToast)
    }

    fun sendLiveTextTurn(text: String, onToast: (String) -> Unit) {
        liveManager.sendTextTurn(scope, text, onToast)
    }

    fun stopLiveVoice() {
        liveManager.stopLiveConversation(scope)
    }

    fun clearLiveTranscripts() {
        liveManager.clearTranscripts()
    }

    fun startAudioRecording(context: Context, onToast: (String) -> Unit) {
        if (_isRecordingAudio.value || _transcribeState.value is UiState.Loading) return
        val started = aiRepo.startAudioNoteRecording(context)
        if (started.isSuccess) {
            _isRecordingAudio.value = true
            _recordingSeconds.value = 0L
            timerJob?.cancel()
            timerJob = scope.launch {
                while (_isRecordingAudio.value) {
                    delay(1000L)
                    _recordingSeconds.value += 1L
                }
            }
        } else {
            val msg = started.exceptionOrNull()?.localizedMessage ?: "Unable to access device microphone."
            _transcribeState.value = UiState.Error(msg)
            onToast(msg)
        }
    }

    fun stopAndTranscribeAudio(prefs: ZaytharPreferences, onToast: (String) -> Unit) {
        if (!_isRecordingAudio.value) return
        _isRecordingAudio.value = false
        timerJob?.cancel()

        scope.launch {
            _transcribeState.value = UiState.Loading
            try {
                val lang = SupportedLanguage.fromCode(prefs.languageCode).englishName
                val result = aiRepo.stopAudioNoteAndTranscribe(lang)
                _transcribeState.value = UiState.Success(result)
                onToast("Audio transcribed via ${result.modelUsed}.")
            } catch (e: TimeoutCancellationException) {
                val msg = "Audio transcription timed out after 60 seconds."
                _transcribeState.value = UiState.Error(msg)
                onToast(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VOICE_CTRL_TAG, "Audio transcription failed", e)
                val msg = e.localizedMessage ?: "Audio transcription failed."
                _transcribeState.value = UiState.Error(msg)
                onToast(msg)
            }
        }
    }

    fun shutdown() {
        timerJob?.cancel()
        _isRecordingAudio.value = false
        liveManager.stopLiveConversation(scope)
        aiRepo.releaseAudioRecorder()
    }
}
