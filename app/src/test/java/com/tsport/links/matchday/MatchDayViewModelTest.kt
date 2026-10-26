package com.tsport.links.matchday

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchDayViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private var now = 1_000L
    private var date = "2026-10-26"

    private val match = TodayMatch(
        id = "1", homeTeam = "Galatasaray", awayTeam = "Fenerbahçe",
        time = "21:30", status = MatchStatus.SCHEDULED
    )

    private class FakeSource : MatchDaySource {
        var result: Result<List<TodayMatch>> = Result.success(emptyList())
        var calls = 0
        var lastDate: String? = null
        var delayMs = 0L

        override suspend fun load(date: String): Result<List<TodayMatch>> {
            calls++
            lastDate = date
            if (delayMs > 0) delay(delayMs)
            return result
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(source: MatchDaySource) = MatchDayViewModel(
        source = source,
        dateProvider = { date },
        nowProvider = { now },
        cacheTtlMs = 1_000L
    )

    @Test
    fun `loads matches and exposes success`() = runTest {
        val source = FakeSource().apply { result = Result.success(listOf(match)) }
        val vm = viewModel(source)
        assertTrue(vm.state.value is MatchDayState.Loading)
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value as MatchDayState.Success
        assertEquals("2026-10-26", state.date)
        assertEquals(listOf(match), state.matches)
        assertEquals("2026-10-26", source.lastDate)
    }

    @Test
    fun `empty list and failures map to matching states and retry works`() = runTest {
        val source = FakeSource().apply { result = Result.success(emptyList()) }
        val vm = viewModel(source)
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is MatchDayState.Empty)

        source.result = Result.failure(IllegalStateException("offline"))
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.state.value is MatchDayState.Failure)

        source.result = Result.success(listOf(match))
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.state.value is MatchDayState.Success)
        assertEquals(3, source.calls)
    }

    @Test
    fun `fresh cache avoids network until forced`() = runTest {
        val source = FakeSource().apply { result = Result.success(listOf(match)) }
        val vm = viewModel(source)
        vm.load()
        advanceUntilIdle()
        vm.load()
        advanceUntilIdle()
        assertEquals(1, source.calls)

        vm.load(force = true)
        advanceUntilIdle()
        assertEquals(2, source.calls)
    }

    @Test
    fun `changed istanbul date invalidates cache`() = runTest {
        val source = FakeSource().apply { result = Result.success(listOf(match)) }
        val vm = viewModel(source)
        vm.load()
        advanceUntilIdle()

        date = "2026-10-27"
        vm.load()
        advanceUntilIdle()
        assertEquals(2, source.calls)
        assertEquals("2026-10-27", (vm.state.value as MatchDayState.Success).date)
    }

    @Test
    fun `stale refresh keeps current matches visible`() = runTest {
        val source = FakeSource().apply {
            result = Result.success(listOf(match))
            delayMs = 1_000
        }
        val vm = viewModel(source)
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is MatchDayState.Success)

        now += 1_001
        source.delayMs = 10_000
        source.result = Result.success(listOf(match.copy(homeTeam = "Yeni")))
        vm.load()
        assertTrue(vm.state.value is MatchDayState.Success)

        advanceUntilIdle()
        assertEquals("Yeni", (vm.state.value as MatchDayState.Success).matches.single().homeTeam)
    }

    @Test
    fun `concurrent load for same date is not duplicated`() = runTest {
        val source = FakeSource().apply {
            result = Result.success(listOf(match))
            delayMs = 1_000
        }
        val vm = viewModel(source)
        vm.load()
        vm.load()
        assertEquals(1, source.calls)
        advanceUntilIdle()
        assertTrue(vm.state.value is MatchDayState.Success)
    }

    @Test
    fun `source exception becomes failure`() = runTest {
        val source = object : MatchDaySource {
            override suspend fun load(date: String): Result<List<TodayMatch>> =
                throw IllegalStateException("boom")
        }
        val vm = viewModel(source)
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is MatchDayState.Failure)
    }
}
