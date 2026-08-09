package org.omnilex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.omnilex.data.model.EntryDetail
import org.omnilex.data.model.SearchResult
import org.omnilex.data.repository.LexicalRepository

class OmniLexViewModel(
    private val repository: LexicalRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val results = MutableStateFlow<List<SearchResult>>(emptyList())
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
        }
    }

    fun search(query: String) {
        searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            repository.search(query)
                .collect {
                    results.value = it
                }
        }
    }

    fun entry(id: String): Flow<EntryDetail?> = repository.entry(id)
}
