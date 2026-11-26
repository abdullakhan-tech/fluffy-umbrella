package com.example.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val AI_AGENTS_OFFICE_URL = "https://zaytharai.com/ai-agents/"

/** Hosts the uploaded 3D Zaythar AI Office inside the native app. */
@Composable
fun AiAgentsOfficeScreen(modifier: Modifier = Modifier) {
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        key(reloadToken) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isLoading = true
                                loadError = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    loadError = error?.description?.toString()
                                        ?: "The AI Agents Office page could not be loaded."
                                }
                            }
                        }
                        webChromeClient = WebChromeClient()
                        webView = this
                        loadUrl(AI_AGENTS_OFFICE_URL)
                    }
                }
            )
        }

        if (isLoading && loadError == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        loadError?.let { message ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "AI Agents Office could not be opened",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = {
                    loadError = null
                    isLoading = true
                    reloadToken += 1
                }) {
                    Text("Retry")
                }
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView?.apply {
                stopLoading()
                webChromeClient = null
                webViewClient = WebViewClient()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
    }
}
