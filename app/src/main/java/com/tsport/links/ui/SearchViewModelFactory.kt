package com.tsport.links.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tsport.links.links.LinkExtractor
import com.tsport.links.links.PriorityAccount
import com.tsport.links.search.SearchViewModel

class SearchViewModelFactory(
    private val extractor: com.tsport.links.links.LinkExtractor,
    private val prioritiesProvider: () -> List<PriorityAccount> = { emptyList() }
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SearchViewModel(extractor, prioritiesProvider) as T
}
