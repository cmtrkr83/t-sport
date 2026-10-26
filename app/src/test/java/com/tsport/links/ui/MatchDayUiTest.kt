package com.tsport.links.ui

import com.tsport.links.matchday.MatchDayState
import com.tsport.links.matchday.MatchStatus
import com.tsport.links.matchday.TodayMatch
import com.tsport.links.search.SearchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchDayUiTest {

    private val match = TodayMatch(
        "1", "Galatasaray", "Fenerbahçe", "21:30",
        "Rams Park", 9, null, null, MatchStatus.SCHEDULED
    )
    private val success = MatchDayState.Success("2026-10-26", listOf(match))

    @Test
    fun `matchday panel is shown only while search is idle`() {
        assertTrue(shouldShowMatchday(SearchState.Idle))
        assertFalse(shouldShowMatchday(SearchState.Searching))
        assertFalse(shouldShowMatchday(SearchState.Empty))
    }

    @Test
    fun `back link is hidden on idle and session expiry`() {
        val expired = "Oturum sona erdi, tekrar giriş yapın."
        assertFalse(shouldShowMatchdayBack(SearchState.Idle, expired))
        assertTrue(shouldShowMatchdayBack(SearchState.Searching, expired))
        assertTrue(shouldShowMatchdayBack(SearchState.Empty, expired))
        assertTrue(shouldShowMatchdayBack(SearchState.Failure("İnternet yok."), expired))
        assertFalse(shouldShowMatchdayBack(SearchState.Failure(expired), expired))
    }

    @Test
    fun `first match focus is tv only idle not typing and one shot`() {
        assertTrue(shouldFocusFirstMatch(true, SearchState.Idle, success, false, false))
        assertFalse(shouldFocusFirstMatch(false, SearchState.Idle, success, false, false))
        assertFalse(shouldFocusFirstMatch(true, SearchState.Searching, success, false, false))
        assertFalse(shouldFocusFirstMatch(true, SearchState.Idle, success, true, false))
        assertFalse(shouldFocusFirstMatch(true, SearchState.Idle, success, false, true))
        assertFalse(shouldFocusFirstMatch(true, SearchState.Idle, MatchDayState.Empty, false, false))
    }

    @Test
    fun `tv focus falls back to the search field when there is no match list`() {
        assertTarget(MatchDayFocusTarget.FIRST_MATCH, success)
        assertTarget(MatchDayFocusTarget.SEARCH_FIELD, MatchDayState.Loading)
        assertTarget(MatchDayFocusTarget.SEARCH_FIELD, MatchDayState.Empty)
        assertTarget(MatchDayFocusTarget.SEARCH_FIELD, MatchDayState.Failure)
        assertTarget(
            MatchDayFocusTarget.SEARCH_FIELD,
            MatchDayState.Success("2026-10-26", emptyList())
        )
    }

    @Test
    fun `tv focus never fires when hidden searching typing or already handled`() {
        assertTarget(MatchDayFocusTarget.NONE, success, panelVisible = false)
        assertTarget(MatchDayFocusTarget.NONE, success, searchState = SearchState.Searching)
        assertTarget(MatchDayFocusTarget.NONE, success, isTyping = true)
        assertTarget(MatchDayFocusTarget.NONE, success, isTelevision = false)
        assertTarget(MatchDayFocusTarget.NONE, success, focusHandled = true)
    }

    @Test
    fun `display time uses score for unknown status only when both scores exist`() {
        val scoredUnknown = match.copy(
            status = MatchStatus.UNKNOWN,
            homeScore = 2,
            awayScore = 1
        )
        assertEquals("2 - 1", matchDisplayTime(scoredUnknown))
        assertEquals("21:30", matchDisplayTime(match.copy(status = MatchStatus.UNKNOWN)))
        assertEquals(
            "2 - 1",
            matchDisplayTime(match.copy(status = MatchStatus.FINISHED, homeScore = 2, awayScore = 1))
        )
        assertEquals("21:30", matchDisplayTime(match))
    }

    private fun assertTarget(
        expected: MatchDayFocusTarget,
        matchState: MatchDayState,
        searchState: SearchState = SearchState.Idle,
        isTelevision: Boolean = true,
        panelVisible: Boolean = true,
        isTyping: Boolean = false,
        focusHandled: Boolean = false
    ) {
        assertEquals(
            expected,
            matchDayFocusTarget(
                isTelevision = isTelevision,
                searchState = searchState,
                matchState = matchState,
                isTyping = isTyping,
                panelVisible = panelVisible,
                focusHandled = focusHandled
            )
        )
    }

    @Test
    fun `formats turkish match date`() {
        assertEquals("26 Ekim 2026, Pazartesi", formatMatchDate("2026-10-26"))
        assertNull(formatMatchDate("not-a-date"))
    }
}
