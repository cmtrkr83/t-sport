package com.tsport.links.search

import android.webkit.WebView
import com.tsport.links.links.LinkCandidate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class ApiFallbackScraper(
    private val webView: WebView,
    private val bridge: ScrapeBridge,
    private val script: String,
    private val bearer: String = TwitterApi.BEARER
) {

    suspend fun scrape(
        csrfToken: String,
        maxPages: Int = 3,
        timeoutMs: Long = 20_000
    ): List<LinkCandidate> {
        if (csrfToken.isBlank()) return emptyList()
        val raw = withContext(Dispatchers.Main) {
            val deferred = CompletableDeferred<String>()
            val token = UUID.randomUUID().toString()
            bridge.expectedToken = token
            bridge.onApiResult = { json -> deferred.complete(json) }
            val js = script
                .replace("__BEARER__", bearer)
                .replace("__CSRF__", csrfToken)
                .replace("__MAX_PAGES__", maxPages.toString())
                .replace("__TOKEN__", token)
            webView.evaluateJavascript(js, null)
            try {
                withTimeout(timeoutMs) { deferred.await() }
            } catch (error: TimeoutCancellationException) {
                throw error
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

    companion object {
        private const val SESSION_EXPIRED = "SESSION_EXPIRED"
    }
}
