package com.dyu.ereader.data.repository

import android.content.Context
import com.dyu.ereader.data.model.SearchResult
import com.dyu.ereader.data.model.SearchQuery
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
        try {
            // This will be populated by JavaScript from epub.js
            // The JS will call Android with search results
            emptyList()
        } finally {
            _isSearching.value = false
        }
    }

    fun onSearchResultsReceived(results: List<SearchResult>) {
        _searchResults.value = results
    }

    fun clearResults() {
        _searchResults.value = emptyList()
    }
}
