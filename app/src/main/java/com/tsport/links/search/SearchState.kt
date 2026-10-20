package com.tsport.links.search

import com.tsport.links.links.LinkItem

sealed interface SearchState {
    data object Idle : SearchState
    data object Searching : SearchState
    data class Success(val links: List<LinkItem>) : SearchState
    data object Empty : SearchState
    data class Failure(val message: String) : SearchState
}
