package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

interface TtsSpeaker {
    val isSpeaking: StateFlow<Boolean>
        get() = MutableStateFlow(false)

    fun speak(text: String, languageCode: String)
    fun stop()
    fun shutdown()
}

internal class AndroidTtsSpeaker(context: Context) : TtsSpeaker, TextToSpeech.OnInitListener {
    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val tts: TextToSpeech? = runCatching {
        TextToSpeech(context.applicationContext, this)
    }.getOrNull()
    private var ready = false

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Android API")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                _isSpeaking.value = false
            }
        })
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
    }

    override fun speak(text: String, languageCode: String) {
        val engine = tts ?: return
        if (!ready || text.isBlank()) return
        runCatching {
            engine.language = Locale.forLanguageTag(languageCode)
            val utteranceId = "zaythar_${System.currentTimeMillis()}"
            val result = engine.speak(text.take(3500), TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result == TextToSpeech.ERROR) _isSpeaking.value = false
        }.onFailure {
            _isSpeaking.value = false
        }
    }

    override fun stop() {
        runCatching { tts?.stop() }
        _isSpeaking.value = false
    }

    override fun shutdown() {
        ready = false
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        _isSpeaking.value = false
    }
}
