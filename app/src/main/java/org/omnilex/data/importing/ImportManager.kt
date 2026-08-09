package org.omnilex.data.importing

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.omnilex.data.repository.ImportReport
import org.omnilex.data.repository.LexicalImporter

class ImportManager(
    private val importers: List<LexicalImporter>
) {
    private val _importStatus = MutableStateFlow<ImportState>(ImportState.Idle)
    val importStatus: StateFlow<ImportState> = _importStatus

    suspend fun runAllImports() {
        Log.d("ImportManager", "Starting all imports. Importers: ${importers.size}")
        _importStatus.value = ImportState.Running(0, importers.size)
        val reports = mutableListOf<ImportReport>()
        
        importers.forEachIndexed { index, importer ->
            try {
                Log.d("ImportManager", "Running importer $index")
                val report = importer.import()
                reports.add(report)
                _importStatus.value = ImportState.Running(index + 1, importers.size)
            } catch (e: Exception) {
                Log.e("ImportManager", "Error during import: ${e.message}", e)
                _importStatus.value = ImportState.Error(e.message ?: "Unknown error")
                return
            }
        }
        Log.d("ImportManager", "All imports finished successfully. Total reports: ${reports.size}")
        _importStatus.value = ImportState.Success(reports)
    }
}

sealed class ImportState {
    object Idle : ImportState()
    data class Running(val current: Int, val total: Int) : ImportState()
    data class Success(val reports: List<ImportReport>) : ImportState()
    data class Error(val message: String) : ImportState()
}
