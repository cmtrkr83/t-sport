package com.tsport.links.ui

import com.tsport.links.matchday.MatchDayState
import com.tsport.links.matchday.MatchStatus
import com.tsport.links.matchday.TodayMatch
import com.tsport.links.search.SearchState
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

enum class MatchDayFocusTarget {
    FIRST_MATCH,
    SEARCH_FIELD,
    NONE
}

fun shouldShowMatchday(searchState: SearchState): Boolean =
    searchState is SearchState.Idle

fun shouldShowMatchdayBack(
    searchState: SearchState,
    sessionExpiredMessage: String
): Boolean = when (searchState) {
    SearchState.Idle -> false
    is SearchState.Failure -> searchState.message != sessionExpiredMessage
    else -> true
}

fun matchDayFocusTarget(
    isTelevision: Boolean,
    searchState: SearchState,
    matchState: MatchDayState,
    isTyping: Boolean,
    panelVisible: Boolean,
    focusHandled: Boolean
): MatchDayFocusTarget {
    if (!isTelevision || !panelVisible || isTyping || focusHandled) {
        return MatchDayFocusTarget.NONE
    }
    if (searchState !is SearchState.Idle) return MatchDayFocusTarget.NONE
    return if (matchState is MatchDayState.Success && matchState.matches.isNotEmpty()) {
        MatchDayFocusTarget.FIRST_MATCH
    } else {
        MatchDayFocusTarget.SEARCH_FIELD
    }
}

fun shouldFocusFirstMatch(
    isTelevision: Boolean,
    searchState: SearchState,
    matchState: MatchDayState,
    isTyping: Boolean,
    focusConsumed: Boolean
): Boolean = matchDayFocusTarget(
    isTelevision = isTelevision,
    searchState = searchState,
    matchState = matchState,
    isTyping = isTyping,
    panelVisible = true,
    focusHandled = focusConsumed
) == MatchDayFocusTarget.FIRST_MATCH

fun matchDisplayTime(match: TodayMatch): String {
    val score = match.scoreText
    val showsScore = score != null && when (match.status) {
        MatchStatus.FINISHED,
        MatchStatus.LIVE,
        MatchStatus.UNKNOWN -> true
        else -> false
    }
    return if (showsScore && score != null) score else match.time
}

fun formatMatchDate(isoDate: String): String? = runCatching {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Europe/Istanbul")
        isLenient = false
    }
    val date = parser.parse(isoDate) ?: return null
    SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr", "TR")).apply {
        timeZone = TimeZone.getTimeZone("Europe/Istanbul")
    }.format(date)
}.getOrNull()
