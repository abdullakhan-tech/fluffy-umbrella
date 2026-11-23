package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraVisionChatContextTest {
    @Test
    fun contextIdentifiesVisionModeAndModel() {
        val context = buildCameraVisionChatContext("OCR", "gemini-3.8-flash", "Invoice total: 250")
        assertTrue(context.startsWith("[CAMERA EYES · OCR]"))
        assertTrue(context.contains("gemini-3.8-flash"))
        assertTrue(context.endsWith("Invoice total: 250"))
    }

    @Test
    fun contextIsBoundedToChatMessageLimit() {
        val context = buildCameraVisionChatContext("SCENE", "vision", "x".repeat(20_000), maxChars = 11_900)
        assertEquals(11_900, context.length)
    }
}
