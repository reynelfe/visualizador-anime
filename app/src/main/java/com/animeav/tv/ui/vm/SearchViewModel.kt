package com.animeav.tv.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animeav.tv.data.AnimeRepository
import com.animeav.tv.data.SearchResultDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResultDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false,
)

class SearchViewModel(
    private val repo: AnimeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update {
                it.copy(results = emptyList(), loading = false, error = null, hasSearched = false)
            }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            search(query)
        }
    }

    fun searchNow() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { search(_state.value.query) }
    }

    private suspend fun search(query: String) {
        if (query.isBlank()) return
        _state.update { it.copy(loading = true, error = null, hasSearched = true) }
        runCatching { repo.search(query.trim()) }
            .onSuccess { results ->
                _state.update { it.copy(results = results, loading = false) }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "Error al buscar",
                        results = emptyList(),
                    )
                }
            }
    }
}
