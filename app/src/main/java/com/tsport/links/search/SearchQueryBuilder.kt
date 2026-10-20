package com.tsport.links.search

import java.net.URLEncoder

object SearchQueryBuilder {

    fun buildSearchUrl(rawHashtag: String): String? {
        val tag = rawHashtag.trim().removePrefix("#").replace(Regex("\\s+"), "")
        if (tag.isEmpty()) return null
        val query = URLEncoder.encode("#$tag \"broadcasts\" filter:links", "UTF-8")
            .replace("+", "%20")
        return "https://x.com/search?q=$query&f=live"
    }
}
