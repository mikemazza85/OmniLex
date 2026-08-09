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
import org.omnilex.data.model.SearchResult
import org.omnilex.data.repository.LexicalRepository

class OmniLexViewModel(
    private val repository: LexicalRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val results = MutableStateFlow<List<SearchResult>>(emptyList())
    private var searchJob: Job? = null
    
    // In a real app, ImportManager would be injected via DI
    private val importManager = ImportManager(listOf(WordNetImporter(repository.dao)))
    val importStatus: StateFlow<ImportState> = importManager.importStatus

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
        }
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
}
