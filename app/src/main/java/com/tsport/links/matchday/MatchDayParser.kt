package com.tsport.links.matchday

import java.util.Locale
import org.json.JSONObject

object MatchDayParser {

    private const val SUPER_LIG_ID = "4339"
    private const val MISSING_TIME = "--:--"
    private val kickoffPattern = Regex("^\\d{2}:\\d{2}$")
    private val eventSeparator = Regex("(?i)\\s+(?:vs\\.?|@)\\s+")
    private val whitespace = Regex("\\s+")

    fun parse(json: String): List<TodayMatch> {
        val events = JSONObject(json).optJSONArray("events") ?: return emptyList()
        return buildList {
            for (index in 0 until events.length()) {
                val event = events.optJSONObject(index) ?: continue
                if (event.optString("idLeague") != SUPER_LIG_ID) continue

                val id = text(event, "idEvent") ?: continue
                val eventName = text(event, "strEvent")
                val eventTeams = eventName?.split(eventSeparator, limit = 2)
                    ?.takeIf { it.size == 2 }
                val homeTeam = text(event, "strHomeTeam")
                    ?: eventTeams?.getOrNull(0)?.let(::normalize)
                val awayTeam = text(event, "strAwayTeam")
                    ?: eventTeams?.getOrNull(1)?.let(::normalize)
                if (homeTeam == null || awayTeam == null) continue

                val postponed = text(event, "strPostponed")?.equals("yes", ignoreCase = true) == true
                val homeScore = intOrNull(event, "intHomeScore")
                val awayScore = intOrNull(event, "intAwayScore")
                add(
                    TodayMatch(
                        id = id,
                        homeTeam = homeTeam,
                        awayTeam = awayTeam,
                        time = clockValue(event, "strTimeLocal")
                            ?: clockValue(event, "strTime")
                            ?: MISSING_TIME,
                        venue = text(event, "strVenue"),
                        round = intOrNull(event, "intRound"),
                        homeScore = homeScore,
                        awayScore = awayScore,
                        status = statusOf(text(event, "strStatus"), postponed)
                    )
                )
            }
        }
            .distinctBy { it.id }
            .sortedWith(
                compareBy<TodayMatch>(
                    { if (it.time == MISSING_TIME) "99:99" else it.time },
                    { it.homeTeam }
                )
            )
    }

    private fun text(json: JSONObject, key: String): String? =
        normalize(json.optString(key, ""))

    private fun normalize(value: String?): String? =
        value
            ?.trim()
            ?.replace(whitespace, " ")
            ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

    private fun clockValue(json: JSONObject, key: String): String? {
        if (!json.has(key) || json.isNull(key)) return null
        return when (val value = json.opt(key)) {
            is Number -> clock(value.toInt().toString())
            is String -> clock(value)
            else -> null
        }
    }

    private fun clock(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val normalized = when {
            kickoffPattern.matches(raw.take(5)) -> raw.take(5)
            raw.length == 4 && raw.all { it.isDigit() } ->
                "${raw.take(2)}:${raw.drop(2)}"
            raw.length <= 2 && raw.all { it.isDigit() } ->
                "${raw.padStart(2, '0')}:00"
            else -> return null
        }
        val hour = normalized.substring(0, 2).toIntOrNull() ?: return null
        val minute = normalized.substring(3, 5).toIntOrNull() ?: return null
        return normalized.takeIf { hour in 0..23 && minute in 0..59 }
    }

    private fun intOrNull(json: JSONObject, key: String): Int? {
        if (!json.has(key) || json.isNull(key)) return null
        return when (val value = json.opt(key)) {
            is Number -> value.toInt()
            is String -> value.trim().toIntOrNull()
            else -> null
        }
    }

    private fun statusOf(raw: String?, postponed: Boolean): MatchStatus {
        val status = raw?.uppercase(Locale.ROOT).orEmpty()
        return when {
            postponed || status == "POST" || status == "PST" -> MatchStatus.POSTPONED
            status == "CANC" || status == "ABD" -> MatchStatus.CANCELLED
            status == "FT" || status == "AET" || status == "PEN" -> MatchStatus.FINISHED
            status in LIVE_STATUSES -> MatchStatus.LIVE
            status.isEmpty() || status == "NS" -> MatchStatus.SCHEDULED
            else -> MatchStatus.UNKNOWN
        }
    }

    private val LIVE_STATUSES = setOf("1H", "HT", "2H", "ET", "P", "BT")
}
