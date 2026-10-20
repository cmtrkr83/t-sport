package com.tsport.links.search

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

object TweetJsonLinkParser {

    private const val MAX_DEPTH = 60
    private const val MAX_URLS = 2_000

    fun extractUrls(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        val urls = LinkedHashSet<String>()
        return try {
            walk(JSONTokener(json).nextValue(), urls, 0)
            urls.toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun walk(value: Any?, urls: MutableSet<String>, depth: Int) {
        if (value == null || depth > MAX_DEPTH || urls.size >= MAX_URLS) return
        when (value) {
            is JSONObject -> {
                if (value.has("user_results")) return
                (value.opt("expanded_url") as? String)?.takeIf(::isWebUrl)?.let(urls::add)
                (value.opt("url") as? String)?.takeIf(::isTcoUrl)?.let(urls::add)
                if (value.optString("key") == "card_url") {
                    (value.optJSONObject("value")?.opt("string_value") as? String)
                        ?.takeIf(::isWebUrl)
                        ?.let(urls::add)
                }
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    walk(value.opt(key), urls, depth + 1)
                }
            }
            is JSONArray -> {
                for (index in 0 until value.length()) {
                    walk(value.opt(index), urls, depth + 1)
                }
            }
        }
    }

    private fun isWebUrl(value: String): Boolean =
        value.startsWith("http://") || value.startsWith("https://")

    private fun isTcoUrl(value: String): Boolean =
        value.startsWith("https://t.co/") || value.startsWith("http://t.co/")
}
