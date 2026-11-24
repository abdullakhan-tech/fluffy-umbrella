package com.example.ui.screens

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.core.net.toUri
import com.mikepenz.markdown.m3.Markdown

/**
 * Renders formatted Markdown for AI assistant message bubbles.
 *
 * Uses the renderer's version-compatible Material 3 defaults while
 * enforcing safe external-link handling for assistant-provided content.
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
        Markdown(
            content = markdown,
            modifier = modifier
        )
    }
}
