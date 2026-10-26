package com.tsport.links.matchday

interface MatchDaySource {
    suspend fun load(date: String): Result<List<TodayMatch>>
}
