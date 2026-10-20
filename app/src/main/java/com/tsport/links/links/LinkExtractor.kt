package com.tsport.links.links

import java.net.URI
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class LinkExtractor(
    private val resolver: UrlResolver,
    private val maxLinks: Int = 20
) {

    suspend fun extract(candidates: List<String>): List<LinkItem> {
        val normalized = candidates.mapNotNull(::normalize).distinct()
        val tcoUrls = normalized.filter(::isTco)
        val resolvedTco = coroutineScope {
            val permits = Semaphore(MAX_CONCURRENT_RESOLVES)
            tcoUrls.map { url ->
                async { permits.withPermit { url to resolver.resolve(url) } }
            }.awaitAll().toMap()
        }

        val results = LinkedHashMap<String, LinkItem>()
        for (candidate in normalized) {
            if (results.size >= maxLinks) break
            val resolved = resolvedTco[candidate]
            val finalUrl = normalize(resolved ?: candidate) ?: candidate
            if (isExcludedHost(finalUrl)) continue
            if (results.containsKey(finalUrl)) continue
            val domain = hostOf(finalUrl) ?: continue
            results[finalUrl] = LinkItem(url = finalUrl, domain = domain)
        }
        return results.values.toList()
    }

    private fun normalize(raw: String): String? {
        var value = raw.trim()
        if (value.isEmpty()) return null
        value = value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}', '\'', '"')
        if (!value.startsWith("http://") && !value.startsWith("https://")) return null
        val withoutFragment = value.substringBefore('#')
        return try {
            val uri = URI(withoutFragment)
            if (uri.host.isNullOrBlank()) return null
            withoutFragment
        } catch (_: Exception) {
            null
        }
    }

    private fun isTco(url: String): Boolean = hostOf(url) == "t.co"

    private fun isExcludedHost(url: String): Boolean {
        val host = hostOf(url) ?: return true
        return host == "x.com" ||
            host.endsWith(".x.com") ||
            host == "twitter.com" ||
            host.endsWith(".twitter.com") ||
            host == "twimg.com" ||
            host.endsWith(".twimg.com")
    }

    private fun hostOf(url: String): String? = try {
        URI(url).host?.lowercase()?.removePrefix("www.")
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val MAX_CONCURRENT_RESOLVES = 6
    }
}
