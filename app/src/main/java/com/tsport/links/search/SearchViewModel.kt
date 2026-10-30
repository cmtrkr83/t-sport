package com.tsport.links.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tsport.links.links.LinkCandidate
import com.tsport.links.links.LinkExtractor
import com.tsport.links.links.PriorityAccount
import com.tsport.links.links.PriorityAccounts
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class SearchViewModel(
    private val extractor: LinkExtractor,
    private val prioritiesProvider: () -> List<PriorityAccount> = { emptyList() }
) : ViewModel() {

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var job: Job? = null

    fun search(
        options: SearchOptions,
        candidateSource: suspend (searchUrl: String) -> List<LinkCandidate>
    ) {
        val searchUrl = SearchQueryBuilder.buildSearchUrl(options)
        if (searchUrl == null) {
            _state.value = SearchState.Failure("Geçerli bir arama metni girin.")
            return
        }
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = SearchState.Searching
            _state.value = try {
                val links = withTimeout(SEARCH_TIMEOUT_MS) {
                    val candidates = candidateSource(searchUrl)
                    extractor.extract(candidates)
                }
                val ordered = prioritize(links, prioritiesProvider())
                if (ordered.isEmpty()) SearchState.Empty else SearchState.Success(ordered)
            } catch (error: TimeoutCancellationException) {
                SearchState.Failure("Arama zaman aşımına uğradı.")
            } catch (error: SessionExpiredException) {
                SearchState.Failure("Oturum sona erdi, tekrar giriş yapın.")
            } catch (error: PageLoadException) {
                SearchState.Failure("X sayfası yüklenemedi.")
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                SearchState.Failure("İnternet bağlantısı yok.")
            } catch (error: Exception) {
                SearchState.Failure("Arama başarısız oldu.")
            }
        }
    }

    fun fail(message: String) {
        _state.value = SearchState.Failure(message)
    }

    fun cancel() {
        reset()
    }

    fun reset() {
        job?.cancel()
        job = null
        _state.value = SearchState.Idle
    }

    override fun onCleared() {
        job?.cancel()
        super.onCleared()
    }

    companion object {
        private const val SEARCH_TIMEOUT_MS = 120_000L
    }
}

/**
 * Stable sort: priority accounts first (in their configured order), then the rest
 * in the order the extractor produced them.
 */
private fun prioritize(
    links: List<com.tsport.links.links.LinkItem>,
    priorities: List<PriorityAccount>
): List<com.tsport.links.links.LinkItem> {
    if (links.isEmpty() || priorities.isEmpty()) return links
    val ranks = HashMap<String, Int>()
    priorities.forEachIndexed { index, account ->
        ranks.putIfAbsent(account.normalizedHandle, index)
        account.normalizedName?.let { ranks.putIfAbsent(it, index) }
    }
    if (ranks.isEmpty()) return links
    return links.sortedWith(compareBy { link ->
        val handle = link.handle?.trim()?.removePrefix("@")?.lowercase()
        val byHandle = handle?.let { ranks[it] }
        if (byHandle != null) return@compareBy byHandle
        val name = link.author?.trim()?.lowercase()
        name?.let { ranks[it] } ?: Int.MAX_VALUE
    })
}
