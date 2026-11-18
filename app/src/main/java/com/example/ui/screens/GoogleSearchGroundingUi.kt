package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.example.domain.GroundedWebSource
import com.example.ui.theme.LocalCyberColors

internal fun openGroundedExternalUrl(
    context: Context,
    rawUrl: String,
    onToast: (String) -> Unit
) {
    val trimmed = rawUrl.trim()
    if (!trimmed.startsWith("https://", ignoreCase = true) &&
        !trimmed.startsWith("http://", ignoreCase = true)
    ) {
        onToast("Blocked unsupported link scheme.")
        return
    }
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, trimmed.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.onFailure {
        onToast("No browser app available to open link.")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GoogleSearchGroundingSection(
    searchQueries: List<String>,
    sources: List<GroundedWebSource>,
    searchEntryPointHtml: String,
    onOpenUrl: (String) -> Unit
) {
    if (searchQueries.isEmpty() && sources.isEmpty() && searchEntryPointHtml.isBlank()) return
    val cyber = LocalCyberColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TravelExplore,
                contentDescription = "Google Search Grounded",
                tint = cyber.cyberGreen,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (searchQueries.isNotEmpty()) {
                    "GOOGLE SEARCH GROUNDED"
                } else {
                    "GROUNDED SOURCES"
                },
                style = MaterialTheme.typography.labelSmall,
                color = cyber.cyberGreen
            )
        }

        if (searchQueries.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                searchQueries.forEachIndexed { idx, query ->
                    Surface(
                        modifier = Modifier.testTag("grounding_query_chip_$idx"),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = cyber.panelGlass,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = query,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (sources.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sources.forEachIndexed { idx, source ->
                    Surface(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable { onOpenUrl(source.url) }
                            .testTag("grounding_source_chip_$idx"),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = cyber.panelGlass,
                        border = BorderStroke(1.dp, cyber.cyberGreen.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open source ${source.displayLabel()}",
                                tint = cyber.cyberGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = source.displayLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = cyber.cyberGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (searchEntryPointHtml.isNotBlank()) {
            GoogleSearchSuggestionsWebView(
                html = searchEntryPointHtml,
                onOpenUrl = onOpenUrl
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun GoogleSearchSuggestionsWebView(
    html: String,
    onOpenUrl: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_search_entry_point"),
        shape = MaterialTheme.shapes.extraSmall,
        color = cyber.panelGlass,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 38.dp, max = 96.dp)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    isVerticalScrollBarEnabled = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val target = request?.url?.toString().orEmpty()
                            if (target.isNotBlank()) onOpenUrl(target)
                            return true
                        }
                    }
                }
            },
            update = { webView ->
                if (webView.tag != html) {
                    webView.tag = html
                    runCatching {
                        webView.loadDataWithBaseURL(
                            "https://www.google.com",
                            html,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StreamingGroundingIndicator(
    searchQueries: List<String>,
    sources: List<GroundedWebSource>
) {
    if (searchQueries.isEmpty() && sources.isEmpty()) return
    val cyber = LocalCyberColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .testTag("streaming_grounding_indicator"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Google Search active",
                tint = cyber.cyberGreen,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (searchQueries.isNotEmpty()) {
                    "Google Search: ${searchQueries.joinToString(" · ")}"
                } else {
                    "Grounding with ${sources.size} source(s)…"
                },
                style = MaterialTheme.typography.labelSmall,
                color = cyber.cyberGreen,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (sources.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sources.take(4).forEach { source ->
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = cyber.panelGlass,
                        border = BorderStroke(1.dp, cyber.cyberGreen.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = source.displayLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = cyber.cyberGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
