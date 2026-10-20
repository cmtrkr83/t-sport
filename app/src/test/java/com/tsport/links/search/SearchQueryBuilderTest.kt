package com.tsport.links.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryBuilderTest {

    @Test
    fun `builds live search url with broadcasts and links filter`() {
        assertEquals(
            "https://x.com/search?q=%23GsvBJK%20%22broadcasts%22%20filter%3Alinks&f=live",
            SearchQueryBuilder.buildSearchUrl("GsvBJK")
        )
    }

    @Test
    fun `trims leading hash and whitespace`() {
        assertEquals(
            SearchQueryBuilder.buildSearchUrl("GsvBJK"),
            SearchQueryBuilder.buildSearchUrl("  #GsvBJK  ")
        )
    }

    @Test
    fun `returns null for blank hashtag`() {
        assertNull(SearchQueryBuilder.buildSearchUrl("   "))
        assertNull(SearchQueryBuilder.buildSearchUrl(" # "))
    }

    @Test
    fun `encodes turkish characters`() {
        assertEquals(
            "https://x.com/search?q=%23Fenerbah%C3%A7e%20%22broadcasts%22%20filter%3Alinks&f=live",
            SearchQueryBuilder.buildSearchUrl("Fenerbahçe")
        )
    }

    @Test
    fun `removes internal whitespace`() {
        assertEquals(
            SearchQueryBuilder.buildSearchUrl("GsvBJK"),
            SearchQueryBuilder.buildSearchUrl("Gsv BJK")
        )
    }
}
