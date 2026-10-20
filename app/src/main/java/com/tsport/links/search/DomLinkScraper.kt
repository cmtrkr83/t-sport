package com.tsport.links.search

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DomLinkScraper(
    private val webView: WebView,
    private val bridge: ScrapeBridge,
    private val script: String
) {

    suspend fun scrape(
        searchUrl: String,
        maxCandidates: Int = 60,
        timeoutMs: Long = 25_000
    ): List<String> = withTimeout(timeoutMs) {
        withContext(Dispatchers.Main) {
            loadAndWait(searchUrl)
            delay(2_000)
            val deferred = CompletableDeferred<String>()
            val token = UUID.randomUUID().toString()
            bridge.expectedToken = token
            bridge.onDomResult = { json -> deferred.complete(json) }
            try {
                val js = script
                    .replace("__MAX__", maxCandidates.toString())
                    .replace("__TOKEN__", token)
                webView.evaluateJavascript(js, null)
                parseCandidates(deferred.await())
            } finally {
                bridge.onDomResult = null
                bridge.expectedToken = null
            }
        }
    }

    private suspend fun loadAndWait(url: String) = suspendCancellableCoroutine<Unit> { continuation ->
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val host = request?.url?.host?.lowercase() ?: return true
                return !isXHost(host)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) = Unit

            override fun onPageFinished(view: WebView?, url: String?) {
                if (!continuation.isActive) return
                if (isLoginUrl(url)) {
                    continuation.resumeWithException(SessionExpiredException())
                } else {
                    continuation.resume(Unit)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true && continuation.isActive) {
                    continuation.resumeWithException(PageLoadException())
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                if (request?.isForMainFrame != true || !continuation.isActive) return
                val status = errorResponse?.statusCode ?: 0
                if (status == 401 || status == 403) {
                    continuation.resumeWithException(SessionExpiredException())
                } else if (status >= 400) {
                    continuation.resumeWithException(PageLoadException())
                }
            }
        }
        webView.loadUrl(url)
    }

    private fun isXHost(host: String): Boolean =
        host == "x.com" ||
            host.endsWith(".x.com") ||
            host == "twitter.com" ||
            host.endsWith(".twitter.com")

    private fun parseCandidates(json: String): List<String> = try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { index ->
            array.optString(index).takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
