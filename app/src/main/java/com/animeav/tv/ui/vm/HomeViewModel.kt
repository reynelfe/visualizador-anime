package com.animeav.tv.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animeav.tv.data.AnimeRepository
import com.animeav.tv.data.HomeDto
import com.animeav.tv.data.LibraryStore
import com.animeav.tv.data.MediaDto
import com.animeav.tv.data.WatchEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class HomeContent(
    val home: HomeDto,
    val popular: List<MediaDto>,
)

class HomeViewModel(
    private val repo: AnimeRepository,
    private val library: LibraryStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeContent>>(UiState.Loading)
    val state: StateFlow<UiState<HomeContent>> = _state.asStateFlow()

    val history: StateFlow<List<WatchEntry>> = library.history

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                coroutineScope {
                    val homeDeferred = async { repo.home() }
                    val popularDeferred = async {
                        runCatching {
                            repo.catalog(page = 1, order = "popular").results
                        }.getOrDefault(emptyList())
                    }
                    HomeContent(home = homeDeferred.await(), popular = popularDeferred.await())
                }
            }.onSuccess {
                _state.value = UiState.Success(it)
            }.onFailure {
                _state.value = UiState.Error(it.message ?: "No se pudo conectar con AnimeAV1")
            }
        }
    }
}
