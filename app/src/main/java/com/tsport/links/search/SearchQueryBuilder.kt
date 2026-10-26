package com.tsport.links.search

import java.net.URLEncoder

enum class SearchTab { POPULAR, LATEST }

data class SearchOptions(
    val query: String,
    val includeBroadcasts: Boolean = false,
    val linksOnly: Boolean = false,
    val tab: SearchTab = SearchTab.LATEST
)

object SearchQueryBuilder {

    fun buildSearchUrl(options: SearchOptions): String? {
        val base = options.query.trim().replace(Regex("\\s+"), " ")
        if (base.isEmpty() || base.all { it == '#' || it.isWhitespace() }) return null

        val parts = mutableListOf(base)
        if (options.includeBroadcasts && !containsToken(base, "broadcasts")) {
            parts += "broadcasts"
        }
        if (options.linksOnly && !containsToken(base, "filter:links")) {
            parts += "filter:links"
        }

        val encoded = URLEncoder.encode(parts.joinToString(" "), "UTF-8")
            .replace("+", "%20")
        return "https://x.com/search?q=$encoded&src=typed_query"
    }

    private fun containsToken(query: String, token: String): Boolean =
        Regex(
            "(?i)(?<![\\p{L}\\p{N}_])${Regex.escape(token)}(?![\\p{L}\\p{N}_])"
        ).containsMatchIn(query)
}
