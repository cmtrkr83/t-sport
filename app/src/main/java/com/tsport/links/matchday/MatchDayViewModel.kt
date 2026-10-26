package com.tsport.links.matchday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MatchDayViewModel(
    private val source: MatchDaySource,
    private val dateProvider: () -> String = { IstanbulDate.today() },
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val cacheTtlMs: Long = 15 * 60 * 1_000L
) : ViewModel() {

    private val _state = MutableStateFlow<MatchDayState>(MatchDayState.Loading)
    val state: StateFlow<MatchDayState> = _state.asStateFlow()

    private data class CacheEntry(
        val date: String,
        val loadedAt: Long,
        val matches: List<TodayMatch>
    )

    private var cache: CacheEntry? = null
    private var job: Job? = null
    private var inFlightDate: String? = null

    fun load(force: Boolean = false) {
        val date = dateProvider()
        val cached = cache

        if (!force && cached != null && cached.date == date &&
            nowProvider() - cached.loadedAt < cacheTtlMs
        ) {
            _state.value = cached.toState()
            return
        }
        if (!force && job?.isActive == true && inFlightDate == date) return

        val visibleCached = cached?.date == date && _state.value is MatchDayState.Success
        if (!visibleCached) _state.value = MatchDayState.Loading

        job?.cancel()
        inFlightDate = date
        job = viewModelScope.launch {
            val result = try {
                source.load(date)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }

            result.fold(
                onSuccess = { matches ->
                    cache = CacheEntry(date, nowProvider(), matches)
                    _state.value = if (matches.isEmpty()) {
                        MatchDayState.Empty
                    } else {
                        MatchDayState.Success(date, matches)
                    }
                },
                onFailure = {
                    if (!visibleCached) _state.value = MatchDayState.Failure
                }
            )
        }
    }

    fun retry() = load(force = true)

    private fun CacheEntry.toState(): MatchDayState =
        if (matches.isEmpty()) MatchDayState.Empty else MatchDayState.Success(date, matches)
}
