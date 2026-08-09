package org.omnilex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.omnilex.data.repository.LexicalRepository

class OmniLexViewModelFactory(
    private val repository: LexicalRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(OmniLexViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OmniLexViewModel(repository) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}