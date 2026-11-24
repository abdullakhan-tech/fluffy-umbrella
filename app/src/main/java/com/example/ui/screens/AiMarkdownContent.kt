package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import com.mikepenz.markdown.m3.Markdown

/**
 * Renders formatted Markdown for AI assistant message bubbles.
 *
 * Keeps the caller's layout and test semantics on a stable Compose
 * container rather than relying on the Markdown renderer to forward them.
 * External links are restricted to HTTP(S).
 */
@Composable
internal fun AiMarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    onToast: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    val safeUriHandler = remember(context, onToast) {
        object : UriHandler {
            override fun openUri(uri: String) {
                val trimmed = uri.trim()
                if (!trimmed.startsWith("https://", ignoreCase = true) &&
                    !trimmed.startsWith("http://", ignoreCase = true)
                ) {
                    onToast?.invoke("Blocked unsafe link scheme.")
                    return
                }
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, trimmed.toUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }.onFailure {
                    onToast?.invoke("No browser app available to open link.")
                }
            }
        }
    }

    CompositionLocalProvider(LocalUriHandler provides safeUriHandler) {
        Box(modifier = modifier.semantics { contentDescription = listOf(markdown.toAccessibleMarkdownText()) }) {
            Markdown(
                content = markdown,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


private fun String.toAccessibleMarkdownText(): String = lines().map { line ->
    line.trim()
        .replace(Regex("^#{1,6}\\s*"), "")
        .replace(Regex("^[-*+]\\s+"), "")
        .replace(Regex("^\\d+[.)]\\s+"), "")
        .replace(Regex("^```[a-zA-Z0-9_-]*$"), "")
        .replace(Regex("`([^`]+)`"), "$1")
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
}.filter { it.isNotBlank() }.joinToString(" ").replace(Regex("\\s+"), " ").trim()
