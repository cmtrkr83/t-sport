package com.tsport.links.search

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

class ScrapeBridge {

    @Volatile
    var onDomResult: ((String) -> Unit)? = null

    @Volatile
    var onApiResult: ((String) -> Unit)? = null

    @Volatile
    var expectedToken: String? = null

    /** Last real SearchTimeline request seen in the X WebView. */
    @Volatile
    var lastSearchTimelineUrl: String? = null

    /** X-Client-Transaction-Id captured from that request. */
    @Volatile
    var lastTransactionId: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun domResult(token: String, json: String) {
        if (token != expectedToken) return
        val callback = onDomResult
        mainHandler.post { callback?.invoke(json) }
    }

    @JavascriptInterface
    fun apiResult(token: String, json: String) {
        if (token != expectedToken) return
        val callback = onApiResult
        mainHandler.post { callback?.invoke(json) }
    }
}
