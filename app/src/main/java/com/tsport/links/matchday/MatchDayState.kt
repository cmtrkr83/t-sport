package com.tsport.links.matchday

sealed interface MatchDayState {
    data object Loading : MatchDayState
    data class Success(val date: String, val matches: List<TodayMatch>) : MatchDayState
    data object Empty : MatchDayState
    data object Failure : MatchDayState
}
