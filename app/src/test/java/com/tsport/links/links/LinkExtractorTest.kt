package com.tsport.links.links

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkExtractorTest {

    private class FakeResolver(private val map: Map<String, String?> = emptyMap()) : UrlResolver {
        override suspend fun resolve(url: String): String? = map[url]
    }

    private fun candidates(vararg urls: String): List<LinkCandidate> =
        urls.map { LinkCandidate(it) }

    @Test
    fun `keeps only x broadcast links`() = runBlocking {
        val extractor = LinkExtractor(FakeResolver())
        val result = extractor.extract(
            candidates(
                "https://x.com/user/status/1",
                "https://twitter.com/user/status/2",
                "https://pbs.twimg.com/media/a.jpg",
                "https://video.twimg.com/ext/tv/1.mp4",
                "https://abs.twimg.com/a.js",
                "https://example.com/stream",
                "/relative/path",
                "javascript:alert(1)",
                "ftp://example.com/file",
                "https://x.com/i/broadcasts/1dKrPrjObjkJX"
            )
        )
        assertEquals(listOf("https://x.com/i/broadcasts/1dKrPrjObjkJX"), result.map { it.url })
        assertEquals("x.com", result.single().domain)
    }

    @Test
    fun `resolves tco and deduplicates broadcasts`() = runBlocking {
        val resolver = FakeResolver(
            mapOf(
                "https://t.co/a" to "https://x.com/i/broadcasts/abc?utm=1",
                "https://t.co/b" to "https://twitter.com/i/broadcasts/abc"
            )
        )
        val result = LinkExtractor(resolver).extract(
            candidates("https://t.co/a", "https://t.co/b", "https://x.com/i/broadcasts/abc")
        )
        assertEquals(listOf("https://x.com/i/broadcasts/abc"), result.map { it.url })
    }

    @Test
    fun `dropping unresolved tco links`() = runBlocking {
        val result = LinkExtractor(FakeResolver()).extract(candidates("https://t.co/xyz"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `canonicalizes query fragments trailing punctuation and twitter host`() = runBlocking {
        val result = LinkExtractor(FakeResolver()).extract(
            candidates(
                "https://x.com/i/broadcasts/abc).",
                "https://mobile.twitter.com/i/broadcasts/def?s=20#section",
                "https://x.com/i/broadcasts/abc/"
            )
        )
        assertEquals(
            listOf("https://x.com/i/broadcasts/abc", "https://x.com/i/broadcasts/def"),
            result.map { it.url }
        )
    }

    @Test
    fun `caps results at max links`() = runBlocking {
        val result = LinkExtractor(FakeResolver(), maxLinks = 2).extract(
            candidates(
                "https://x.com/i/broadcasts/a",
                "https://x.com/i/broadcasts/b",
                "https://x.com/i/broadcasts/c"
            )
        )
        assertEquals(2, result.size)
    }

    @Test
    fun `resolves each unique tco url once`() = runBlocking {
        val calls = mutableListOf<String>()
        val resolver = UrlResolver { url ->
            calls.add(url)
            "https://x.com/i/broadcasts/abc"
        }
        val result = LinkExtractor(resolver).extract(
            candidates("https://t.co/a", "https://t.co/a", "https://t.co/b")
        )
        assertEquals(listOf("https://x.com/i/broadcasts/abc"), result.map { it.url })
        assertEquals(setOf("https://t.co/a", "https://t.co/b"), calls.toSet())
    }

    @Test
    fun `keeps author through tco resolution`() = runBlocking {
        val resolver = FakeResolver(mapOf("https://t.co/a" to "https://x.com/i/broadcasts/abc"))
        val result = LinkExtractor(resolver).extract(
            listOf(LinkCandidate("https://t.co/a", "Ali Veli"))
        )
        assertEquals("Ali Veli", result.single().author)
    }

    @Test
    fun `uses first non blank author for duplicate broadcasts`() = runBlocking {
        val result = LinkExtractor(FakeResolver()).extract(
            listOf(
                LinkCandidate("https://x.com/i/broadcasts/abc"),
                LinkCandidate("https://twitter.com/i/broadcasts/abc", "Ayşe Yılmaz")
            )
        )
        assertEquals("Ayşe Yılmaz", result.single().author)
    }

    @Test
    fun `leaves author null when no tweet author is available`() = runBlocking {
        val result = LinkExtractor(FakeResolver()).extract(
            candidates("https://x.com/i/broadcasts/abc")
        )
        assertNull(result.single().author)
    }

    @Test
    fun `ignores blank malformed and non broadcast x paths`() = runBlocking {
        val result = LinkExtractor(FakeResolver()).extract(
            candidates(
                "",
                "   ",
                "not a url",
                "https://x.com/i/broadcasts/",
                "https://x.com/i/broadcasts/not/valid",
                "https://x.com/i/flow/login",
                "https://x.com/i/broadcasts/ok_123"
            )
        )
        assertEquals(listOf("https://x.com/i/broadcasts/ok_123"), result.map { it.url })
    }

    @Test
    fun `default cap keeps fifty broadcast links`() = runBlocking {
        val urls = (1..55).map { "https://x.com/i/broadcasts/$it" }
        val result = LinkExtractor(FakeResolver()).extract(candidates(*urls.toTypedArray()))
        assertEquals(50, result.size)
        assertEquals("https://x.com/i/broadcasts/1", result.first().url)
    }

    @Test
    fun `keeps resolved links when resolve budget expires`() = runBlocking {
        val resolver = object : UrlResolver {
            override suspend fun resolve(url: String): String? {
                if (url.endsWith("/slow")) delay(5_000)
                return when (url) {
                    "https://t.co/fast" -> "https://x.com/i/broadcasts/fast"
                    "https://t.co/slow" -> "https://x.com/i/broadcasts/slow"
                    else -> null
                }
            }
        }
        val result = LinkExtractor(resolver, resolveBudgetMs = 100).extract(
            candidates("https://t.co/fast", "https://t.co/slow")
        )
        assertEquals(listOf("https://x.com/i/broadcasts/fast"), result.map { it.url })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `default resolve budget is ten seconds`() = runTest {
        val start = currentTime
        val resolver = UrlResolver {
            delay(20_000)
            "https://x.com/i/broadcasts/slow"
        }
        val result = LinkExtractor(resolver).extract(candidates("https://t.co/slow"))
        assertTrue(result.isEmpty())
        assertEquals(10_000, currentTime - start)
    }
}
