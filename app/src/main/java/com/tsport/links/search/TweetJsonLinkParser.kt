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
        val user = userOf(tweet)
        val legacy = tweet.optJSONObject("legacy")
        addUrlArray(
            legacy?.optJSONObject("entities")?.optJSONArray("urls"),
            user?.first,
            user?.second,
            out
        )
        addUrlArray(
            tweet.optJSONObject("note_tweet")
                ?.optJSONObject("note_tweet_results")
                ?.optJSONObject("result")
                ?.optJSONObject("entity_set")
                ?.optJSONArray("urls"),
            user?.first,
            user?.second,
            out
        )
        addCardUrl(legacy?.optJSONObject("card"), user?.first, user?.second, out)
    }

    /** Returns the tweet author as (display name or @handle fallback, screen name). */
    private fun userOf(tweet: JSONObject): Pair<String?, String?>? {
        val user = tweet.optJSONObject("core")
            ?.optJSONObject("user_results")
            ?.optJSONObject("result")
            ?.optJSONObject("legacy") ?: return null
        val name = user.optString("name").trim().takeIf { it.isNotEmpty() }
        val screenName = user.optString("screen_name").trim().takeIf { it.isNotEmpty() }
        if (name == null && screenName == null) return null
        val author = name ?: screenName?.let { "@$it" }
        return author to screenName
    }

    private fun addUrlArray(
        array: JSONArray?,
        author: String?,
        handle: String?,
        out: MutableMap<String, LinkCandidate>
    ) {
        if (array == null) return
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            addUrl(item.opt("expanded_url") as? String, author, handle, out)
            addUrl(item.opt("url") as? String, author, handle, out)
        }
    }

    private fun addCardUrl(
        card: JSONObject?,
        author: String?,
        handle: String?,
        out: MutableMap<String, LinkCandidate>
    ) {
        val values = card?.optJSONObject("legacy")?.optJSONArray("binding_values") ?: return
        for (index in 0 until values.length()) {
            val item = values.optJSONObject(index) ?: continue
            if (item.optString("key") != "card_url") continue
            addUrl(
                item.optJSONObject("value")?.opt("string_value") as? String,
                author,
                handle,
                out
            )
        }
    }

    private fun addUrl(
        raw: String?,
        author: String?,
        handle: String?,
        out: MutableMap<String, LinkCandidate>
    ) {
        if (raw == null) return
        val value = raw.trim()
        if (!value.startsWith("http://") && !value.startsWith("https://")) return
        val existing = out[value]
        if (existing == null) {
            out[value] = LinkCandidate(value, author, handle)
        } else if (
            (existing.author.isNullOrBlank() && !author.isNullOrBlank()) ||
            (existing.handle.isNullOrBlank() && !handle.isNullOrBlank())
        ) {
            out[value] = existing.copy(
                author = existing.author ?: author,
                handle = existing.handle ?: handle
            )
        }
    }
}
