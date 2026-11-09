package com.example.sample.settings

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sample.security.ui.VaultSecondary

private const val LEGAL_HOST = "krishnapensalwar.github.io"
private const val LEGAL_ROOT = "https://krishnapensalwar.github.io/personal-vault-legal/"

@Composable
fun TermsScreen() {
    LegalWebView(LEGAL_ROOT + "terms.html")
}

@Composable
fun PrivacyScreen() {
    LegalWebView(LEGAL_ROOT + "privacy-policy.html")
}

@Composable
private fun LegalWebView(url: String) {
    var failed by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().padding(WindowInsets.statusBars.asPaddingValues())) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val host = request.url.host
                            return host != LEGAL_HOST
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            failed = false
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError,
                        ) {
                            if (request.isForMainFrame) failed = true
                        }
                    }
                    loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.url != url) webView.loadUrl(url)
            },
        )
        if (failed) {
            Text(
                "Could not load this page. Check the connection and try again.",
                color = VaultSecondary,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }
    }
}
