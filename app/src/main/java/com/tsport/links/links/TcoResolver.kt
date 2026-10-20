package com.tsport.links.links

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class TcoResolver(
    private val client: OkHttpClient = defaultClient()
) : UrlResolver {

    override suspend fun resolve(url: String): String? {
        var current = url
        var redirects = 0
        while (true) {
            var hop = fetch(current, useGet = false) ?: return null
            if (hop.code == 405 || hop.code == 403) {
                hop = fetch(current, useGet = true) ?: return null
            }
            if (hop.code in 300..399) {
                if (redirects >= MAX_REDIRECTS) return null
                val location = hop.location ?: return null
                current = current.toHttpUrlOrNull()?.resolve(location)?.toString() ?: return null
                redirects++
                continue
            }
            if (hop.code !in 200..299) return null
            return if (current != url) current else null
        }
    }

    private suspend fun fetch(url: String, useGet: Boolean): Hop? {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
        if (useGet) builder.get() else builder.head()
        val response = execute(builder.build()) ?: return null
        return response.use { Hop(it.code, it.header("Location")) }
    }

    private suspend fun execute(request: Request): Response? =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        continuation.resume(response)
                    } else {
                        response.close()
                    }
                }
            })
        }

    private data class Hop(val code: Int, val location: String?)

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        private const val MAX_REDIRECTS = 5

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }
}
