package com.dyu.ereader.data.repository.search

import android.content.Context
import com.dyu.ereader.data.model.search.SearchResult
import com.dyu.ereader.data.model.search.SearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SearchRepository(
    private val context: Context
) {
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    suspend fun search(query: SearchQuery): List<SearchResult> = withContext(Dispatchers.Default) {
        _isSearching.value = true
        // Results will be populated by JavaScript from epub.js
        emptyList()
    }

    fun startSearch(query: SearchQuery) {
        _isSearching.value = true
    }

    fun onSearchResultsReceived(results: List<SearchResult>) {
        _searchResults.value = results
        _isSearching.value = false
    }

    fun clearResults() {
        _searchResults.value = emptyList()
        _isSearching.value = false
    }
}
