package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

interface TtsSpeaker {
    fun speak(text: String, languageCode: String)
    fun stop()
    fun shutdown()
}

internal class AndroidTtsSpeaker(context: Context) : TtsSpeaker, TextToSpeech.OnInitListener {
    private val tts: TextToSpeech? = runCatching { TextToSpeech(context.applicationContext, this) }.getOrNull()
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
    }

    override fun speak(text: String, languageCode: String) {
        val engine = tts ?: return
        if (!ready) return
        runCatching {
            engine.language = Locale.forLanguageTag(languageCode)
            engine.speak(text.take(3500), TextToSpeech.QUEUE_FLUSH, null, "zaythar_${System.currentTimeMillis()}")
        }
    }

    override fun stop() {
        runCatching { tts?.stop() }
    }

    override fun shutdown() {
        ready = false
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
    }
}
