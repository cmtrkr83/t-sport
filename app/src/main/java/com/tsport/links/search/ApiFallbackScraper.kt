package com.tsport.links.search

import android.net.Uri
import android.webkit.WebView
import com.tsport.links.links.LinkCandidate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

class ApiFallbackScraper(
    private val webView: WebView,
    private val bridge: ScrapeBridge,
    private val script: String,
    private val bearer: String = TwitterApi.BEARER,
    private val transactionScript: String = ""
) {


    suspend fun scrape(
        csrfToken: String,
        query: String,
        tab: SearchTab,
        maxPages: Int = 6,
        timeoutMs: Long = 15_000
    ): List<LinkCandidate> {
        if (csrfToken.isBlank() || query.isBlank()) return emptyList()

        val capturedUrl = bridge.lastSearchTimelineUrl
        val transactionId = bridge.lastTransactionId.orEmpty()
        val queryId = queryIdOf(capturedUrl) ?: ScweetSearch.DEFAULT_QUERY_ID
        val features = featuresOf(capturedUrl) ?: ScweetSearch.DEFAULT_FEATURES
        val product = if (tab == SearchTab.LATEST) "Latest" else "Top"

        val raw = withContext(Dispatchers.Main) {
            ensureTransactionLibrary()
            val deferred = CompletableDeferred<String>()
            val token = UUID.randomUUID().toString()
            bridge.expectedToken = token
            bridge.onApiResult = { json -> deferred.complete(json) }
            val js = script
                .replace("__BEARER__", bearer)
                .replace("__CSRF__", csrfToken)
                .replace("__MAX_PAGES__", maxPages.toString())
                .replace("__TOKEN__", token)
                .replace("__QUERY__", JSONObject.quote(query))
                .replace("__PRODUCT__", JSONObject.quote(product))
                .replace("__QUERY_ID__", JSONObject.quote(queryId))
                .replace("__FALLBACK_QUERY_ID__", JSONObject.quote(ScweetSearch.DEFAULT_QUERY_ID))
                .replace("__FEATURES__", JSONObject.quote(features))
                .replace("__TX__", JSONObject.quote(transactionId))
                .replace("__CAPTURED_URL__", JSONObject.quote(capturedUrl.orEmpty()))
            webView.evaluateJavascript(js, null)
            try {
                withTimeoutOrNull(timeoutMs) { deferred.await() }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            } finally {
                bridge.onApiResult = null
                bridge.expectedToken = null
            }
        }
        if (raw == SESSION_EXPIRED) throw SessionExpiredException()
        return withContext(Dispatchers.Default) { TweetJsonLinkParser.extractCandidates(raw) }
    }

    private suspend fun ensureTransactionLibrary() {
        if (transactionScript.isBlank()) return
        val ready = CompletableDeferred<Unit>()
        webView.evaluateJavascript(transactionScript) { ready.complete(Unit) }
        withTimeoutOrNull(15_000) { ready.await() }
    }

    private fun queryIdOf(capturedUrl: String?): String? {
        val url = capturedUrl ?: return null
        return try {
            val segments = Uri.parse(url).pathSegments
            val index = segments.indexOf("graphql")
            segments.getOrNull(index + 1)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun featuresOf(capturedUrl: String?): String? {
        val url = capturedUrl ?: return null
        return try {
            Uri.parse(url).getQueryParameter("features")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val SESSION_EXPIRED = "SESSION_EXPIRED"
    }
}
