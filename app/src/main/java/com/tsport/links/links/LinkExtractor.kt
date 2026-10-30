package com.tsport.links.links

import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

class LinkExtractor(
    private val resolver: UrlResolver,
    private val maxLinks: Int = 50,
    private val resolveBudgetMs: Long = 10_000
) {

    suspend fun extract(candidates: List<LinkCandidate>): List<LinkItem> {
        val normalized = LinkedHashMap<String, LinkCandidate>()
        for (candidate in candidates) {
            val url = normalize(candidate.url) ?: continue
            val author = candidate.author?.trim()?.takeIf { it.isNotEmpty() }
            val handle = candidate.handle?.trim()?.takeIf { it.isNotEmpty() }
            val existing = normalized[url]
            if (existing == null) {
                normalized[url] = LinkCandidate(url, author, handle)
            } else {
                val merged = existing.copy(
                    author = existing.author ?: author,
                    handle = existing.handle ?: handle
                )
                if (merged != existing) normalized[url] = merged
            }
        }

        val tcoUrls = normalized.keys.filter(::isTco)
        val resolvedTco = ConcurrentHashMap<String, String>()
        coroutineScope {
            val permits = Semaphore(MAX_CONCURRENT_RESOLVES)
            val jobs = tcoUrls.map { url ->
                launch {
                    permits.withPermit {
                        resolver.resolve(url)?.let { resolvedTco[url] = it }
                    }
                }
            }
            withTimeoutOrNull(resolveBudgetMs) { jobs.joinAll() }
            jobs.forEach { it.cancel() }
        }

        val results = LinkedHashMap<String, LinkItem>()
        for ((candidate, meta) in normalized) {
            if (results.size >= maxLinks) break
            val finalUrl = canonicalBroadcast(resolvedTco[candidate] ?: candidate) ?: continue
            val existing = results[finalUrl]
            if (existing == null) {
                results[finalUrl] = LinkItem(
                    url = finalUrl,
                    domain = "x.com",
                    author = meta.author,
                    handle = meta.handle
                )
            } else {
                val merged = existing.copy(
                    author = existing.author ?: meta.author,
                    handle = existing.handle ?: meta.handle
                )
                if (merged != existing) results[finalUrl] = merged
            }
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

    private fun canonicalBroadcast(raw: String): String? {
        val uri = try {
            URI(raw)
        } catch (_: Exception) {
            return null
        }
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        if (!isXHost(host)) return null
        val path = uri.path ?: return null
        if (!path.startsWith(BROADCAST_PATH)) return null
        val id = path.removePrefix(BROADCAST_PATH).trim('/')
        if (!BROADCAST_ID.matches(id)) return null
        return "https://x.com/i/broadcasts/$id"
    }

    private fun isXHost(host: String): Boolean =
        host == "x.com" ||
            host.endsWith(".x.com") ||
            host == "twitter.com" ||
            host.endsWith(".twitter.com")

    private fun isTco(url: String): Boolean = hostOf(url) == "t.co"

    private fun hostOf(url: String): String? = try {
        URI(url).host?.lowercase()?.removePrefix("www.")
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val MAX_CONCURRENT_RESOLVES = 6
        private const val BROADCAST_PATH = "/i/broadcasts/"
        private val BROADCAST_ID = Regex("^[A-Za-z0-9_-]+$")
    }
}
