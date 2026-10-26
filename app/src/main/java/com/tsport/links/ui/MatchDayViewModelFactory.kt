package com.tsport.links.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tsport.links.matchday.MatchDaySource
import com.tsport.links.matchday.MatchDayViewModel

class MatchDayViewModelFactory(
    private val source: MatchDaySource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MatchDayViewModel(source) as T
}
