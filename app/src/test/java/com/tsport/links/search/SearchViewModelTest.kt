package com.tsport.links.search

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
        viewModel.search("GsvBJK") {
            listOf("https://example.com/a", "https://example.com/b")
        }
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Success)
        assertEquals(2, (state as SearchState.Success).links.size)
    }

    @Test
    fun `empty candidate list yields empty state`() = runTest {
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search("GsvBJK") { emptyList() }
        advanceUntilIdle()
        assertTrue(viewModel.state.value is SearchState.Empty)
    }

    @Test
    fun `blank hashtag fails without calling source`() = runTest {
        var called = false
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search("   ") {
            called = true
            emptyList()
        }
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Failure)
        assertEquals("Geçerli bir hashtag girin.", (state as SearchState.Failure).message)
        assertTrue(!called)
    }

    @Test
    fun `source exception yields failure state`() = runTest {
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search("GsvBJK") { throw IllegalStateException("boom") }
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Failure)
        assertEquals("Arama başarısız oldu.", (state as SearchState.Failure).message)
    }

    @Test
    fun `page load error yields clear message`() = runTest {
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search("GsvBJK") { throw PageLoadException() }
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Failure)
        assertEquals("X sayfası yüklenemedi.", (state as SearchState.Failure).message)
    }

    @Test
    fun `source time counts toward total timeout`() = runTest(dispatcher) {
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search("GsvBJK") {
            delay(30_000)
            emptyList()
        }
        advanceTimeBy(26_000)
        runCurrent()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Failure)
        assertEquals("Arama zaman aşımına uğradı.", (state as SearchState.Failure).message)
    }

    @Test
    fun `link extraction time counts toward total timeout`() = runTest(dispatcher) {
        val slowResolver = UrlResolver {
            delay(30_000)
            null
        }
        val viewModel = SearchViewModel(LinkExtractor(slowResolver))
        viewModel.search("GsvBJK") { listOf("https://t.co/slow") }
        advanceTimeBy(26_000)
        runCurrent()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Failure)
        assertEquals("Arama zaman aşımına uğradı.", (state as SearchState.Failure).message)
    }

    @Test
    fun `reset returns to idle and cancel stops an in flight search`() = runTest(dispatcher) {
        val viewModel = SearchViewModel(LinkExtractor(resolver))
        viewModel.search("GsvBJK") {
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
        viewModel.search("GsvBJK") { throw SessionExpiredException() }
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is SearchState.Failure)
        assertEquals("Oturum sona erdi, tekrar giriş yapın.", (state as SearchState.Failure).message)
    }
}
