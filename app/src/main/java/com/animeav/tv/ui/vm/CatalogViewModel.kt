package com.animeav.tv.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animeav.tv.data.AnimeRepository
import com.animeav.tv.data.CategoryDto
import com.animeav.tv.data.GenreDto
import com.animeav.tv.data.MediaDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogFilters(
    val genre: String? = null,
    val category: String? = null,
    val year: Int? = null,
    val order: String = "recent",
)

data class CatalogUiState(
    val items: List<MediaDto> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val totalPages: Int = 1,
    val genres: List<GenreDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val years: List<Int> = emptyList(),
    val filters: CatalogFilters = CatalogFilters(),
)

class CatalogViewModel(
    private val repo: AnimeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    private var loadingJob: kotlinx.coroutines.Job? = null

    init {
        load(reset = true)
    }

    fun setGenre(slug: String?) {
        _state.update { it.copy(filters = it.filters.copy(genre = slug)) }
        load(reset = true)
    }

    fun setCategory(slug: String?) {
        _state.update { it.copy(filters = it.filters.copy(category = slug)) }
        load(reset = true)
    }

    fun setYear(year: Int?) {
        _state.update { it.copy(filters = it.filters.copy(year = year)) }
        load(reset = true)
    }

    fun setOrder(order: String) {
        _state.update { it.copy(filters = it.filters.copy(order = order)) }
        load(reset = true)
    }

    fun loadMore() {
        val snapshot = _state.value
        if (snapshot.loading || snapshot.loadingMore || snapshot.page >= snapshot.totalPages) return
        load(reset = false)
    }

    fun retry() {
        load(reset = true)
    }

    private fun load(reset: Boolean) {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            val snapshot = _state.value
            val nextPage = if (reset) 1 else snapshot.page + 1
            _state.update {
                it.copy(
                    loading = reset,
                    loadingMore = !reset,
                    error = null,
                    items = if (reset) emptyList() else it.items,
                )
            }
            runCatching {
                repo.catalog(
                    page = nextPage,
                    genre = snapshot.filters.genre,
                    category = snapshot.filters.category,
                    year = snapshot.filters.year,
                    order = snapshot.filters.order,
                )
            }.onSuccess { catalog ->
                val merged = if (reset) catalog.results else (snapshot.items + catalog.results)
                    .distinctBy { it.slug }
                _state.update {
                    it.copy(
                        items = merged,
                        loading = false,
                        loadingMore = false,
                        page = catalog.pagination?.currentPage ?: nextPage,
                        totalPages = catalog.pagination?.totalPages ?: 1,
                        genres = if (catalog.genresIdsMap.isNotEmpty()) {
                            catalog.genresIdsMap.values.sortedBy { g -> g.id }
                        } else it.genres,
                        categories = if (catalog.categoriesIdsMap.isNotEmpty()) {
                            catalog.categoriesIdsMap.values.sortedBy { c -> c.id }
                        } else it.categories,
                        years = catalog.years.ifEmpty { it.years },
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        loadingMore = false,
                        error = error.message ?: "Error al cargar el catálogo",
                    )
                }
            }
        }
    }
}
