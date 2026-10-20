package com.tsport.links.links

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkExtractorTest {

    private class FakeResolver(private val map: Map<String, String?> = emptyMap()) : UrlResolver {
        override suspend fun resolve(url: String): String? = map[url]
    }

    @Test
    fun `filters internal media relative and non http links`() = runBlocking {
        val extractor = LinkExtractor(FakeResolver())
        val result = extractor.extract(
            listOf(
                "https://x.com/user/status/1",
                "https://twitter.com/user/status/2",
                "https://pbs.twimg.com/media/a.jpg",
                "https://video.twimg.com/ext/tv/1.mp4",
                "https://abs.twimg.com/a.js",
                "/relative/path",
                "javascript:alert(1)",
                "ftp://example.com/file",
                "https://example.com/stream"
            )
        )
        assertEquals(listOf("https://example.com/stream"), result.map { it.url })
        assertEquals("example.com", result.single().domain)
    }

    @Test
    fun `resolves tco and deduplicates final urls`() = runBlocking {
        val resolver = FakeResolver(
            mapOf(
                "https://t.co/a" to "https://example.com/live",
                "https://t.co/b" to "https://example.com/live"
            )
        )
        val result = LinkExtractor(resolver).extract(
            listOf("https://t.co/a", "https://t.co/b", "https://example.com/live")
        )
        assertEquals(listOf("https://example.com/live"), result.map { it.url })
    }

    @Test
    fun `keeps tco when resolution fails`() = runBlocking {
        val result = LinkExtractor(FakeResolver()).extract(listOf("https://t.co/xyz"))
        assertEquals("https://t.co/xyz", result.single().url)
        assertEquals("t.co", result.single().domain)
    }

    @Test
    fun `strips trailing punctuation and fragments`() = runBlocking {
        val result = LinkExtractor(FakeResolver()).extract(
            listOf("https://example.com/live).", "https://example.com/page#section")
        )
        assertEquals(listOf("https://example.com/live", "https://example.com/page"), result.map { it.url })
    }

    @Test
    fun `caps results at max links`() = runBlocking {
        val result = LinkExtractor(FakeResolver(), maxLinks = 2).extract(
            listOf("https://a.com/1", "https://b.com/2", "https://c.com/3")
        )
        assertEquals(2, result.size)
    }

    @Test
    fun `resolves each unique tco url once`() = runBlocking {
        val calls = mutableListOf<String>()
        val resolver = UrlResolver { url ->
            calls.add(url)
            "https://example.com/live"
        }
        val result = LinkExtractor(resolver).extract(
            listOf("https://t.co/a", "https://t.co/a", "https://t.co/b")
        )
        assertEquals(listOf("https://example.com/live"), result.map { it.url })
        assertEquals(setOf("https://t.co/a", "https://t.co/b"), calls.toSet())
    }

    @Test
    fun `ignores blank and malformed entries`() = runBlocking {
        val result = LinkExtractor(FakeResolver()).extract(listOf("", "   ", "not a url", "https://ok.com/x"))
        assertEquals(listOf("https://ok.com/x"), result.map { it.url })
        assertTrue(result.single().domain == "ok.com")
    }
}
