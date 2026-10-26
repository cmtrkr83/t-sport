package com.tsport.links.matchday

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TheSportsDbSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var source: TheSportsDbSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        source = TheSportsDbSource(
            client = OkHttpClient(),
            baseUrl = server.url("/api/v1/json/123/eventsday.php").toString()
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `requests super lig for requested date and parses response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"events":[{"idEvent":"1","idLeague":"4339",
                 "strHomeTeam":"Galatasaray","strAwayTeam":"Fenerbahçe",
                 "strTimeLocal":"21:30:00","strStatus":"NS"}]}"""
        ))
        val result = source.load("2026-10-26")
        assertTrue(result.isSuccess)
        assertEquals("Galatasaray", result.getOrThrow().single().homeTeam)
        val request = server.takeRequest()
        assertEquals("2026-10-26", request.requestUrl?.queryParameter("d"))
        assertEquals("4339", request.requestUrl?.queryParameter("l"))
        assertEquals("GET", request.method)
    }

    @Test
    fun `http error and malformed json become failure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        assertTrue(source.load("2026-10-26").isFailure)

        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))
        assertTrue(source.load("2026-10-26").isFailure)
    }

    @Test
    fun `empty events is a successful empty list`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"events":null}"""))
        val result = source.load("2026-10-26")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }
}
