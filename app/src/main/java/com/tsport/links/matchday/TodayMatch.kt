package com.tsport.links.matchday

enum class MatchStatus { SCHEDULED, FINISHED, LIVE, POSTPONED, CANCELLED, UNKNOWN }

data class TodayMatch(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val time: String = "--:--",
    val venue: String? = null,
    val round: Int? = null,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val status: MatchStatus = MatchStatus.UNKNOWN
) {
    val query: String get() = "$homeTeam $awayTeam"
    val scoreText: String?
        get() = if (homeScore != null && awayScore != null) "$homeScore - $awayScore" else null
}
