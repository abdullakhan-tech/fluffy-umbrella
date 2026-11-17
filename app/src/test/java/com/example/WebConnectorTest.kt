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
            "https://192.0.2.10/documentation",
            "https://198.18.0.1/benchmark",
            "https://203.0.113.10/documentation",
            "https://240.0.0.1/reserved",
            "https://[::1]/loopback",
            "https://[fc00::1]/private",
            "https://[2001:db8::1]/documentation",
            "https://service.internal/secret",
            "https://user:password@example.com/private",
            "https://example.com:8443/nonstandard-port"
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
