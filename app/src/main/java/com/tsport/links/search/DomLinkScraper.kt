package com.tsport.links.search

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.tsport.links.links.LinkCandidate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DomLinkScraper(
    private val webView: WebView,
    private val bridge: ScrapeBridge,
    private val script: String
) {

    @Volatile
    var lastTabSelected: Boolean = true
        private set

    suspend fun scrape(
        searchUrl: String,
        tab: SearchTab = SearchTab.LATEST,
        maxCandidates: Int = 150,
        timeoutMs: Long = 30_000
    ): List<LinkCandidate> = withTimeoutOrNull(timeoutMs) {
        withContext(Dispatchers.Main) {
            lastTabSelected = true
            loadAndWait(searchUrl)
            delay(PAGE_SETTLE_MS)
            val tabSelection = if (tab == SearchTab.LATEST) {
                LatestTabSelector(::evaluate).select()
            } else {
                TabSelection.SELECTED
            }
            lastTabSelected = tabSelection != TabSelection.FAILED
            if (tabSelection == TabSelection.CLICKED) delay(RESULT_SETTLE_MS)
            val deferred = CompletableDeferred<String>()
            val token = UUID.randomUUID().toString()
            bridge.expectedToken = token
            bridge.onDomResult = { json -> deferred.complete(json) }
            try {
                val js = script
                    .replace("__MAX__", maxCandidates.toString())
                    .replace("__BUDGET_MS__", SCRIPT_BUDGET_MS.toString())
                    .replace("__TOKEN__", token)
                webView.evaluateJavascript(js, null)
                parseCandidates(deferred.await())
            } finally {
                bridge.onDomResult = null
                bridge.expectedToken = null
            }
        }
    } ?: emptyList()

    private suspend fun evaluate(js: String): String? {
        val result = CompletableDeferred<String>()
        webView.evaluateJavascript(js) { value -> result.complete(value) }
        return withTimeoutOrNull(EVAL_TIMEOUT_MS) { result.await() }
    }

    private suspend fun loadAndWait(url: String) = suspendCancellableCoroutine<Unit> { continuation ->
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request?.isForMainFrame != true) return false
                val host = request.url.host?.lowercase() ?: return true
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

    private fun parseCandidates(json: String): List<LinkCandidate> = try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { index ->
            when (val value = array.opt(index)) {
                is JSONObject -> {
                    val url = value.optString("url").takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    val author = value.optString("author").takeIf { it.isNotBlank() }
                    LinkCandidate(url, author)
                }
                is String -> value.takeIf { it.isNotBlank() }?.let { LinkCandidate(it) }
                else -> null
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private companion object {
        private const val PAGE_SETTLE_MS = 2_000L
        private const val EVAL_TIMEOUT_MS = 500L
        private const val RESULT_SETTLE_MS = 3_000L
        private const val SCRIPT_BUDGET_MS = 20_000L
    }
}
