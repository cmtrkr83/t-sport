package com.tsport.links.search

import com.tsport.links.links.LinkCandidate
import com.tsport.links.links.LinkExtractor
import com.tsport.links.links.UrlResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val resolver = UrlResolver { null }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `success state contains extracted links`() = runTest {
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search(SearchOptions("GsvBJK")) {
            listOf(LinkCandidate("https://x.com/i/broadcasts/a"), LinkCandidate("https://x.com/i/broadcasts/b"))
        }
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Success)
        assertEquals(2, (state as SearchState.Success).links.size)
    }

    @Test
    fun `empty candidate list yields empty state`() = runTest {
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search(SearchOptions("GsvBJK")) { emptyList() }
        advanceUntilIdle()
        assertTrue(viewModel.state.value is SearchState.Empty)
    }

    @Test
    fun `blank query fails without calling source`() = runTest {
        var called = false
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search(SearchOptions("   ")) {
            called = true
            emptyList()
        }
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Failure)
        assertEquals("Geçerli bir arama metni girin.", (state as SearchState.Failure).message)
        assertTrue(!called)
    }

    @Test
    fun `source exception yields failure state`() = runTest {
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search(SearchOptions("GsvBJK")) { throw IllegalStateException("boom") }
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Failure)
        assertEquals("Arama başarısız oldu.", (state as SearchState.Failure).message)
    }

    @Test
    fun `page load error yields clear message`() = runTest {
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search(SearchOptions("GsvBJK")) { throw PageLoadException() }
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Failure)
        assertEquals("X sayfası yüklenemedi.", (state as SearchState.Failure).message)
    }

    @Test
    fun `source time counts toward total timeout`() = runTest(dispatcher) {
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search(SearchOptions("GsvBJK")) {
            delay(120_000)
            emptyList()
        }
        advanceTimeBy(119_000)
        runCurrent()
        assertTrue(viewModel.state.value is SearchState.Searching)
        advanceTimeBy(2_000)
        runCurrent()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Failure)
        assertEquals("Arama zaman aşımına uğradı.", (state as SearchState.Failure).message)
    }

    @Test
    fun `slow link extraction is bounded by resolve budget`() = runTest(dispatcher) {
        val slowResolver = UrlResolver {
            delay(30_000)
            null
        }
        val viewModel = SearchViewModel(LinkExtractor(slowResolver, resolveBudgetMs = 100))
        viewModel.search(SearchOptions("GsvBJK")) { listOf(LinkCandidate("https://t.co/slow")) }
        advanceTimeBy(200)
        runCurrent()
        assertTrue(viewModel.state.value is SearchState.Empty)
    }

    @Test
    fun `reset returns to idle and cancel stops an in flight search`() = runTest(dispatcher) {
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search(SearchOptions("GsvBJK")) {
            delay(10_000)
            emptyList()
        }
        assertTrue(viewModel.state.value is SearchState.Searching)
        viewModel.cancel()
        assertTrue(viewModel.state.value is SearchState.Idle)
        viewModel.fail("hata")
        assertTrue(viewModel.state.value is SearchState.Failure)
        viewModel.reset()
        assertTrue(viewModel.state.value is SearchState.Idle)
    }

    @Test
    fun `expired session yields login message`() = runTest {
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search(SearchOptions("GsvBJK")) { throw SessionExpiredException() }
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Failure)
        assertEquals("Oturum sona erdi, tekrar giriş yapın.", (state as SearchState.Failure).message)
    }

    @Test
    fun `source receives url without f parameter`() = runTest {
        var receivedUrl: String? = null
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search(SearchOptions("GsvBJK")) { searchUrl ->
            receivedUrl = searchUrl
            emptyList()
        }
        advanceUntilIdle()
        assertTrue(receivedUrl != null)
        assertFalse(receivedUrl!!.contains("f="))
    }
}
