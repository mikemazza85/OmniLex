package org.omnilex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.omnilex.data.importing.ImportManager
import org.omnilex.data.importing.ImportState
import org.omnilex.data.importing.WordNetImporter
import org.omnilex.data.model.EntryDetail
import org.omnilex.data.model.LexicalEntry
import org.omnilex.data.model.SearchResult
import org.omnilex.data.repository.LexicalGraph
import org.omnilex.data.repository.LexicalRepository
import org.omnilex.data.repository.ResolutionResult

sealed class GraphState {
    object Idle : GraphState()
    object Loading : GraphState()
    data class Success(val graph: LexicalGraph) : GraphState()
    data class Error(val message: String) : GraphState()
}

sealed class NavigationTarget {
    data class Entry(val id: String) : NavigationTarget()
    data class Graph(val id: String) : NavigationTarget()
    data class Disambiguation(val candidates: List<LexicalEntry>) : NavigationTarget()
    data class Search(val query: String) : NavigationTarget()
}

class OmniLexViewModel(
    private val repository: LexicalRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val results = MutableStateFlow<List<SearchResult>>(emptyList())
    private var searchJob: Job? = null
    
    private val _graphState = MutableStateFlow<GraphState>(GraphState.Idle)
    val graphState = _graphState.asStateFlow()

    private val _preferGraphView = MutableStateFlow(false)
    val preferGraphView = _preferGraphView.asStateFlow()

    private val _navigationIntent = MutableStateFlow<NavigationTarget?>(null)
    val navigationIntent = _navigationIntent.asStateFlow()

    // In a real app, ImportManager would be injected via DI
    private val importManager = ImportManager(listOf(WordNetImporter(repository.dao)))
    val importStatus: StateFlow<ImportState> = importManager.importStatus

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
        }
    }

    fun toggleViewPreference() {
        _preferGraphView.value = !_preferGraphView.value
    }

    fun consumeNavigation() {
        _navigationIntent.value = null
    }

    fun triggerImport() {
        android.util.Log.d("OmniLexViewModel", "triggerImport called")
        viewModelScope.launch {
            importManager.runAllImports()
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

    fun loadGraph(entryId: String) {
        viewModelScope.launch {
            _graphState.value = GraphState.Loading
            try {
                val graph = repository.neighborhood(entryId, depth = 1)
                _graphState.value = GraphState.Success(graph)
            } catch (e: Exception) {
                _graphState.value = GraphState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun navigateToEntry(id: String) {
        android.util.Log.d("OmniLex", "navigateToEntry: $id, preferGraph: ${_preferGraphView.value}")
        _navigationIntent.value = if (_preferGraphView.value) NavigationTarget.Graph(id) else NavigationTarget.Entry(id)
    }

    fun resolveAndNavigate(word: String, contextEntryId: String?) {
        android.util.Log.d("OmniLex", "resolveAndNavigate: $word, context: $contextEntryId")
        viewModelScope.launch {
            when (val result = repository.resolveContextualEntry(word, contextEntryId)) {
                is ResolutionResult.Resolved -> navigateToEntry(result.entryId)
                is ResolutionResult.Ambiguous -> _navigationIntent.value = NavigationTarget.Disambiguation(result.candidates)
                ResolutionResult.NotFound -> _navigationIntent.value = NavigationTarget.Search(word)
            }
        }
    }
}
