package com.animeav.tv.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animeav.tv.data.AnimeRepository
import com.animeav.tv.data.LibraryStore
import com.animeav.tv.data.MediaDto
import com.animeav.tv.data.WatchEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val media: MediaDto? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val inMyList: Boolean = false,
    val progress: WatchEntry? = null,
)

class DetailViewModel(
    private val repo: AnimeRepository,
    private val library: LibraryStore,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    @Volatile
    private var currentSlug: String? = null

    init {
        viewModelScope.launch {
            library.history.collect { entries ->
                val slug = currentSlug ?: return@collect
                _state.update { current ->
                    current.copy(progress = entries.firstOrNull { it.slug == slug })
                }
            }
        }
    }

    fun load(slug: String) {
        currentSlug = slug
        viewModelScope.launch {
            _state.value = DetailUiState(
                loading = true,
                inMyList = library.isInMyList(slug),
            )
            runCatching { repo.media(slug) }
                .onSuccess { media ->
                    _state.update {
                        it.copy(
                            media = media,
                            loading = false,
                            error = null,
                            inMyList = library.isInMyList(slug),
                            progress = library.progressFor(slug),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = error.message ?: "No se pudo cargar el anime",
                        )
                    }
                }
        }
    }

    fun toggleMyList() {
        val media = _state.value.media ?: return
        library.toggleMyList(media.slug, media.title, media.id)
        _state.update { it.copy(inMyList = library.isInMyList(media.slug)) }
    }

    fun retry() {
        currentSlug?.let { load(it) }
    }
}
