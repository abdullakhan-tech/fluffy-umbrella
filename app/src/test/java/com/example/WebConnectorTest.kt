package com.example

import com.example.domain.WebConnector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebConnectorTest {

    @Test
    fun validatePublicHttpsUrl_allowsPublicHttps() {
        val result = WebConnector.validatePublicHttpsUrl("https://developer.android.com/jetpack")
        assertTrue(result.isSuccess)
    }

    @Test
    fun validatePublicHttpsUrl_blocksInsecureHttpAndPrivateHosts() {
        val blockedUrls = listOf(
            "http://developer.android.com",
            "https://localhost/admin",
            "https://127.0.0.1:8080/health",
            "https://10.0.0.5/internal",
            "https://169.254.169.254/latest/meta-data/",
            "https://192.168.1.1/router",
            "https://172.16.0.10/db",
            "https://[::1]/loopback",
            "https://[fc00::1]/private",
            "https://service.internal/secret"
        )
        blockedUrls.forEach { url ->
            assertTrue("Expected SSRF check to block $url", WebConnector.validatePublicHttpsUrl(url).isFailure)
        }
    }

    @Test
    fun extractPublicHttpsUrls_extractsOnlySafeUrlsAndBoundsToTwo() {
        val text = "Compare https://kotlinlang.org/docs/home.html, https://localhost/secret, " +
            "https://firebase.google.com/docs/ai-logic, and https://example.org/extra."
        val extracted = WebConnector.extractPublicHttpsUrls(text)
        assertEquals(
            listOf(
                "https://kotlinlang.org/docs/home.html",
                "https://firebase.google.com/docs/ai-logic"
            ),
            extracted
        )
    }
}
