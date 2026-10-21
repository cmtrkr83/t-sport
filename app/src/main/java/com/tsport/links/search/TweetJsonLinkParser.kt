package com.tsport.links.search

import com.tsport.links.links.LinkCandidate
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

object TweetJsonLinkParser {

    private const val MAX_DEPTH = 60
    private const val MAX_CANDIDATES = 2_000

    fun extractCandidates(json: String?): List<LinkCandidate> {
        if (json.isNullOrBlank()) return emptyList()
        val candidates = LinkedHashMap<String, LinkCandidate>()
        return try {
            walk(JSONTokener(json).nextValue(), candidates, 0)
            candidates.values.toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun walk(
        value: Any?,
        out: MutableMap<String, LinkCandidate>,
        depth: Int
    ) {
        if (value == null || depth > MAX_DEPTH || out.size >= MAX_CANDIDATES) return
        when (value) {
            is JSONObject -> {
                if (looksLikeTweet(value)) collectTweet(value, out)
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key == "user_results") continue
                    walk(value.opt(key), out, depth + 1)
                }
            }
            is JSONArray -> {
                for (index in 0 until value.length()) {
                    walk(value.opt(index), out, depth + 1)
                }
            }
        }
    }

    private fun looksLikeTweet(value: JSONObject): Boolean {
        val legacy = value.optJSONObject("legacy") ?: return false
        return legacy.has("full_text") || value.has("core") || value.has("note_tweet")
    }

    private fun collectTweet(tweet: JSONObject, out: MutableMap<String, LinkCandidate>) {
        val author = authorOf(tweet)
        val legacy = tweet.optJSONObject("legacy")
        addUrlArray(
            legacy?.optJSONObject("entities")?.optJSONArray("urls"),
            author,
            out
        )
        addUrlArray(
            tweet.optJSONObject("note_tweet")
                ?.optJSONObject("note_tweet_results")
                ?.optJSONObject("result")
                ?.optJSONObject("entity_set")
                ?.optJSONArray("urls"),
            author,
            out
        )
        addCardUrl(legacy?.optJSONObject("card"), author, out)
    }

    private fun authorOf(tweet: JSONObject): String? {
        val user = tweet.optJSONObject("core")
            ?.optJSONObject("user_results")
            ?.optJSONObject("result")
            ?.optJSONObject("legacy")
        val name = user?.optString("name")?.trim()?.takeIf { it.isNotEmpty() }
        if (name != null) return name
        val screenName = user?.optString("screen_name")?.trim()?.takeIf { it.isNotEmpty() }
        return screenName?.let { "@$it" }
    }

    private fun addUrlArray(
        array: JSONArray?,
        author: String?,
        out: MutableMap<String, LinkCandidate>
    ) {
        if (array == null) return
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            addUrl(item.opt("expanded_url") as? String, author, out)
            addUrl(item.opt("url") as? String, author, out)
        }
    }

    private fun addCardUrl(
        card: JSONObject?,
        author: String?,
        out: MutableMap<String, LinkCandidate>
    ) {
        val values = card?.optJSONObject("legacy")?.optJSONArray("binding_values") ?: return
        for (index in 0 until values.length()) {
            val item = values.optJSONObject(index) ?: continue
            if (item.optString("key") != "card_url") continue
            addUrl(
                item.optJSONObject("value")?.opt("string_value") as? String,
                author,
                out
            )
        }
    }

    private fun addUrl(
        raw: String?,
        author: String?,
        out: MutableMap<String, LinkCandidate>
    ) {
        if (raw == null) return
        val value = raw.trim()
        if (!value.startsWith("http://") && !value.startsWith("https://")) return
        val existing = out[value]
        if (existing == null) {
            out[value] = LinkCandidate(value, author)
        } else if (existing.author.isNullOrBlank() && !author.isNullOrBlank()) {
            out[value] = existing.copy(author = author)
        }
    }
}
