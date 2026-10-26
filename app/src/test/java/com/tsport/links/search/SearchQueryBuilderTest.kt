package com.tsport.links.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryBuilderTest {

    private fun url(
        query: String,
        broadcasts: Boolean = false,
        linksOnly: Boolean = false
    ): String? = SearchQueryBuilder.buildSearchUrl(
        SearchOptions(query, includeBroadcasts = broadcasts, linksOnly = linksOnly)
    )

    @Test
    fun `keeps free text without adding hash`() {
        assertEquals("https://x.com/search?q=SAMvAMD&src=typed_query", url("SAMvAMD"))
    }

    @Test
    fun `adds unquoted broadcasts when selected`() {
        assertEquals(
            "https://x.com/search?q=SAMvAMD%20broadcasts&src=typed_query",
            url("SAMvAMD", broadcasts = true)
        )
    }

    @Test
    fun `keeps spaces in multi word queries`() {
        assertEquals(
            "https://x.com/search?q=SAMvAMD%20canl%C4%B1%20yay%C4%B1n%20broadcasts&src=typed_query",
            url("  SAMvAMD   canlı  yayın  ", broadcasts = true)
        )
    }

    @Test
    fun `adds filter links when selected`() {
        assertEquals(
            "https://x.com/search?q=%23GSvFB%20filter%3Alinks&src=typed_query",
            url("#GSvFB", linksOnly = true)
        )
    }

    @Test
    fun `adds both options`() {
        assertEquals(
            "https://x.com/search?q=SAMvAMD%20broadcasts%20filter%3Alinks&src=typed_query",
            url("SAMvAMD", broadcasts = true, linksOnly = true)
        )
    }

    @Test
    fun `does not duplicate broadcasts already in query`() {
        assertEquals(
            "https://x.com/search?q=SAMvAMD%20broadcasts&src=typed_query",
            url("SAMvAMD broadcasts", broadcasts = true)
        )
        assertEquals(
            "https://x.com/search?q=SAMvAMD%20BROADCASTS&src=typed_query",
            url("SAMvAMD BROADCASTS", broadcasts = true)
        )
    }

    @Test
    fun `does not duplicate filter links already in query`() {
        assertEquals(
            "https://x.com/search?q=SAMvAMD%20filter%3Alinks&src=typed_query",
            url("SAMvAMD filter:links", linksOnly = true)
        )
    }

    @Test
    fun `does not put tab in url`() {
        val popular = SearchQueryBuilder.buildSearchUrl(
            SearchOptions("SAMvAMD", tab = SearchTab.POPULAR)
        )
        val latest = SearchQueryBuilder.buildSearchUrl(
            SearchOptions("SAMvAMD", tab = SearchTab.LATEST)
        )
        assertEquals(popular, latest)
        assertFalse(popular!!.contains("f="))
    }

    @Test
    fun `returns null for blank or hash only query`() {
        assertNull(url(""))
        assertNull(url("   "))
        assertNull(url("#"))
        assertNull(url(" # "))
        assertNull(url("##"))
        assertNull(url("# #"))
    }

    @Test
    fun `encodes turkish and special characters`() {
        assertEquals(
            "https://x.com/search?q=Fenerbah%C3%A7e&src=typed_query",
            url("Fenerbahçe")
        )
        assertEquals(
            "https://x.com/search?q=A%26B%3DC%25&src=typed_query",
            url("A&B=C%")
        )
    }
}
