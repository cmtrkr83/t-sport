package com.tsport.links.matchday

import com.tsport.links.search.SearchOptions
import com.tsport.links.search.SearchQueryBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class MatchDayQueryTest {

    @Test
    fun `match selection builds broadcasts search without vs`() {
        val match = TodayMatch(
            id = "2527798", homeTeam = "Galatasaray", awayTeam = "Fenerbahçe",
            time = "21:30", venue = "Rams Park", round = 9,
            homeScore = null, awayScore = null, status = MatchStatus.SCHEDULED
        )
        val url = SearchQueryBuilder.buildSearchUrl(
            SearchOptions(match.query, includeBroadcasts = true)
        )
        assertEquals(
            "https://x.com/search?q=Galatasaray%20Fenerbah%C3%A7e%20broadcasts&src=typed_query",
            url
        )
    }
}
