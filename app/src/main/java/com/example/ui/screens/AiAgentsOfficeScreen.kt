package com.example.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val AI_AGENTS_OFFICE_URL = "https://zaytharai.com/ai-agents/"

/** Hosts the uploaded 3D Zaythar AI Office inside the native app. */
@Composable
fun AiAgentsOfficeScreen(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadUrl(AI_AGENTS_OFFICE_URL)
            }
        }
    )
}
