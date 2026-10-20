package com.tsport.links.links

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TcoResolverTest {

    private lateinit var server: MockWebServer
    private lateinit var resolver: TcoResolver

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        resolver = TcoResolver(OkHttpClient.Builder().followRedirects(false).build())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `follows redirect to final url`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/final"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        assertEquals(server.url("/final").toString(), resolver.resolve(server.url("/start").toString()))
    }

    @Test
    fun `returns null when there is no redirect`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        assertNull(resolver.resolve(server.url("/same").toString()))
    }

    @Test
    fun `returns null on http error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        assertNull(resolver.resolve(server.url("/missing").toString()))
    }

    @Test
    fun `follows at most five redirects`() = runBlocking {
        repeat(5) { index ->
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/step${index + 1}"))
        }
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        assertEquals(server.url("/step5").toString(), resolver.resolve(server.url("/start").toString()))
    }

    @Test
    fun `returns null when redirect limit is exceeded`() = runBlocking {
        repeat(6) { index ->
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/loop${index + 1}"))
        }
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        assertNull(resolver.resolve(server.url("/loop0").toString()))
    }

    @Test
    fun `falls back to get when head is not allowed`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(405))
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/final"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        assertEquals(server.url("/final").toString(), resolver.resolve(server.url("/start").toString()))
    }
}
