package com.tsport.links.matchday

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchDayParserTest {

    private val superLig = """
        {
          "events": [
            {
              "idEvent": "3",
              "idLeague": "4339",
              "strEvent": "Rizespor vs Trabzonspor",
              "strHomeTeam": "Rizespor",
              "strAwayTeam": "Trabzonspor",
              "strTimeLocal": "19:00:00",
              "dateEventLocal": "2026-10-27",
              "strVenue": "Çaykur Didi Stadyumu",
              "intRound": "9",
              "intHomeScore": null,
              "intAwayScore": null,
              "strStatus": "NS"
            },
            {
              "idEvent": "1",
              "idLeague": "4339",
              "strEvent": "Beşiktaş vs İstanbul Başakşehir",
              "strHomeTeam": "Beşiktaş",
              "strAwayTeam": "İstanbul Başakşehir",
              "strTimeLocal": "19:00:00",
              "strVenue": "Tüpraş Stadyumu",
              "intRound": "9",
              "intHomeScore": "2",
              "intAwayScore": "1",
              "strStatus": "FT"
            },
            {
              "idEvent": "2",
              "idLeague": "4339",
              "strEvent": "Galatasaray vs Fenerbahçe",
              "strHomeTeam": "Galatasaray",
              "strAwayTeam": "Fenerbahçe",
              "strTimeLocal": "21:30:00",
              "strVenue": "Rams Park",
              "intRound": 9,
              "intHomeScore": null,
              "intAwayScore": null,
              "strStatus": "NS"
            },
            {
              "idEvent": "99",
              "idLeague": "9999",
              "strEvent": "Other vs Match",
              "strHomeTeam": "Other",
              "strAwayTeam": "Match",
              "strTimeLocal": "20:00:00"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `keeps only super lig events and sorts by kickoff`() {
        val matches = MatchDayParser.parse(superLig)
        assertEquals(listOf("1", "3", "2"), matches.map { it.id })
        assertEquals("Beşiktaş", matches[0].homeTeam)
        assertEquals("19:00", matches[0].time)
        assertEquals("Rams Park", matches[2].venue)
        assertEquals(9, matches[2].round)
    }

    @Test
    fun `parses scores and status labels`() {
        val matches = MatchDayParser.parse(superLig)
        assertEquals("2 - 1", matches[0].scoreText)
        assertEquals(MatchStatus.FINISHED, matches[0].status)
        assertEquals(MatchStatus.SCHEDULED, matches[1].status)
        assertNull(matches[1].scoreText)
    }

    @Test
    fun `falls back to event text when team fields are missing`() {
        val json = """{"events":[{"idEvent":"7","idLeague":"4339",
            "strEvent":"A vs B","strTimeLocal":"20:00:00","strStatus":"NS"}]}"""
        val match = MatchDayParser.parse(json).single()
        assertEquals("A", match.homeTeam)
        assertEquals("B", match.awayTeam)
        assertEquals("A B", match.query)
    }

    @Test
    fun `skips event without id or resolvable teams`() {
        val json = """{"events":[
            {"idLeague":"4339","strHomeTeam":"A","strAwayTeam":"B"},
            {"idEvent":"2","idLeague":"4339","strEvent":"not a fixture"}
        ]}"""
        assertTrue(MatchDayParser.parse(json).isEmpty())
    }

    @Test
    fun `maps postponed cancelled live and unknown statuses`() {
        val json = """{"events":[
            {"idEvent":"1","idLeague":"4339","strHomeTeam":"A","strAwayTeam":"B","strTimeLocal":"18:00:00","strStatus":"POST"},
            {"idEvent":"2","idLeague":"4339","strHomeTeam":"C","strAwayTeam":"D","strTimeLocal":"18:00:00","strStatus":"CANC"},
            {"idEvent":"3","idLeague":"4339","strHomeTeam":"E","strAwayTeam":"F","strTimeLocal":"18:00:00","strStatus":"2H","intHomeScore":"1","intAwayScore":"0"},
            {"idEvent":"4","idLeague":"4339","strHomeTeam":"G","strAwayTeam":"H","strTimeLocal":"18:00:00","strStatus":"???"}
        ]}"""
        val statuses = MatchDayParser.parse(json).associate { it.id to it.status }
        assertEquals(MatchStatus.POSTPONED, statuses["1"])
        assertEquals(MatchStatus.CANCELLED, statuses["2"])
        assertEquals(MatchStatus.LIVE, statuses["3"])
        assertEquals(MatchStatus.UNKNOWN, statuses["4"])
    }

    @Test
    fun `missing optional time is shown as placeholder and sorted last`() {
        val json = """{"events":[
            {"idEvent":"1","idLeague":"4339","strHomeTeam":"A","strAwayTeam":"B","strStatus":"NS"},
            {"idEvent":"2","idLeague":"4339","strHomeTeam":"C","strAwayTeam":"D","strTimeLocal":"10:00:00","strStatus":"NS"}
        ]}"""
        val matches = MatchDayParser.parse(json)
        assertEquals("--:--", matches.last().time)
        assertEquals("2", matches.first().id)
    }

    @Test
    fun `reads numeric and compact string kickoff times`() {
        val json = """{"events":[
            {"idEvent":"1","idLeague":"4339","strHomeTeam":"A","strAwayTeam":"B","strTimeLocal":1930,"strStatus":"NS"},
            {"idEvent":"2","idLeague":"4339","strHomeTeam":"C","strAwayTeam":"D","strTime":"21:30:00","strStatus":"NS"},
            {"idEvent":"3","idLeague":"4339","strHomeTeam":"E","strAwayTeam":"F","strTimeLocal":"0830","strStatus":"NS"},
            {"idEvent":"4","idLeague":"4339","strHomeTeam":"G","strAwayTeam":"H","strTimeLocal":19,"strStatus":"NS"}
        ]}"""
        val times = MatchDayParser.parse(json).associate { it.id to it.time }
        assertEquals("19:30", times["1"])
        assertEquals("21:30", times["2"])
        assertEquals("08:30", times["3"])
        assertEquals("19:00", times["4"])
    }

    @Test
    fun `empty null and malformed responses`() {
        assertTrue(MatchDayParser.parse("""{"events":null}""").isEmpty())
        assertTrue(MatchDayParser.parse("""{"events":[]}""").isEmpty())
        assertThrows(JSONException::class.java) { MatchDayParser.parse("not json") }
    }
}
