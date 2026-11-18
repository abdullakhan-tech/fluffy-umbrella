package com.example.ui.screens

import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.net.toUri
import com.example.ui.theme.LocalCyberColors
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

/**
 * Renders formatted Markdown for AI assistant message bubbles using
 * Mike Penz's Multiplatform Markdown Renderer styled to match Zaythar's
 * Light, Dark, and System Default Material 3 themes.
 */
@Composable
internal fun AiMarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    onToast: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current
    val colors = MaterialTheme.colorScheme
    val typo = MaterialTheme.typography

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

    val mdColors = markdownColor(
        text = colors.onSurface,
        codeText = colors.onSurface,
        inlineCodeText = if (cyber.isDark) cyber.accentSoft else colors.primary,
        linkText = cyber.cyberGreen,
        codeBackground = if (cyber.isDark) colors.background else colors.surfaceVariant,
        inlineCodeBackground = colors.surfaceVariant,
        dividerColor = colors.outline
    )

    val mdTypography = markdownTypography(
        h1 = typo.headlineSmall,
        h2 = typo.titleLarge,
        h3 = typo.titleMedium,
        h4 = typo.titleSmall,
        h5 = typo.labelLarge,
        h6 = typo.labelLarge,
        text = typo.bodyMedium,
        paragraph = typo.bodyMedium,
        code = typo.bodySmall.copy(fontFamily = FontFamily.Monospace),
        inlineCode = typo.bodySmall.copy(fontFamily = FontFamily.Monospace),
        quote = typo.bodyMedium.copy(fontStyle = FontStyle.Italic),
        ordered = typo.bodyMedium,
        bullet = typo.bodyMedium,
        list = typo.bodyMedium,
        link = typo.bodyMedium.copy(textDecoration = TextDecoration.Underline)
    )

    CompositionLocalProvider(LocalUriHandler provides safeUriHandler) {
        Markdown(
            content = markdown,
            colors = mdColors,
            typography = mdTypography,
            modifier = modifier
        )
    }
}
