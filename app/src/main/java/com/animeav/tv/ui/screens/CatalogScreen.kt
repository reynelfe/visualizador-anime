package com.animeav.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animeav.tv.AnimeAvApp
import com.animeav.tv.data.coverUrl
import com.animeav.tv.ui.components.AppTopBar
import com.animeav.tv.ui.components.Chip
import com.animeav.tv.ui.components.ErrorState
import com.animeav.tv.ui.components.FadeRow
import com.animeav.tv.ui.components.LoadingState
import com.animeav.tv.ui.components.PosterCard
import com.animeav.tv.ui.components.SectionHeader
import com.animeav.tv.ui.theme.AnimeRed
import com.animeav.tv.ui.theme.TextSecondary
import com.animeav.tv.ui.vm.CatalogUiState
import com.animeav.tv.ui.vm.CatalogViewModel
import com.animeav.tv.ui.vm.appViewModelFactory

@Composable
fun CatalogScreen(nav: NavController) {
    val app = LocalContext.current.applicationContext as AnimeAvApp
    val vm: CatalogViewModel = viewModel(
        factory = appViewModelFactory { CatalogViewModel(it.repository) },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        AppTopBar(currentRoute = "catalog") { nav.navigateCatalog(it) }
        when {
            state.loading -> LoadingState("Cargando catálogo…")
            state.error != null && state.items.isEmpty() ->
                ErrorState(state.error ?: "Error") { vm.retry() }
            else -> CatalogGrid(state = state, vm = vm, nav = nav)
        }
    }
}

private fun NavController.navigateCatalog(route: String) {
    navigate(route) {
        launchSingleTop = true
        popUpTo("home") { saveState = true }
        restoreState = true
    }
}

@Composable
private fun CatalogGrid(
    state: CatalogUiState,
    vm: CatalogViewModel,
    nav: NavController,
) {
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.items.size - 8 && state.page < state.totalPages
        }
    }

    LaunchedEffect(shouldLoadMore, state.items.size, state.page) {
        if (shouldLoadMore) vm.loadMore()
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 40.dp, end = 40.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            CatalogFilters(state, vm)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader("Catálogo · ${state.items.size} títulos")
        }
        items(state.items, key = { it.id }, contentType = { "media" }) { media ->
            PosterCard(
                title = media.title,
                imageUrl = media.coverUrl,
                subtitle = media.category?.name,
                onClick = { nav.navigate("detail/${media.slug}") },
            )
        }
        if (state.loadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AnimeRed)
                }
            }
        }
        if (state.error != null && state.items.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = state.error ?: "",
                    color = TextSecondary,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CatalogFilters(state: CatalogUiState, vm: CatalogViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        FilterRow(
            label = "Orden",
            options = listOf(
                FilterOption("Recientes", state.filters.order == "recent") { vm.setOrder("recent") },
                FilterOption("Populares", state.filters.order == "popular") { vm.setOrder("popular") },
                FilterOption("Mejor valorados", state.filters.order == "score") { vm.setOrder("score") },
            ),
        )
        FilterRow(
            label = "Categoría",
            options = buildList {
                add(FilterOption("Todas", state.filters.category == null) { vm.setCategory(null) })
                state.categories.forEach { category ->
                    add(
                        FilterOption(category.name, state.filters.category == category.slug) {
                            vm.setCategory(category.slug)
                        },
                    )
                }
            },
        )
        FilterRow(
            label = "Género",
            options = buildList {
                add(FilterOption("Todos", state.filters.genre == null) { vm.setGenre(null) })
                state.genres.take(18).forEach { genre ->
                    add(
                        FilterOption(genre.name, state.filters.genre == genre.slug) {
                            vm.setGenre(genre.slug)
                        },
                    )
                }
            },
        )
        FilterRow(
            label = "Año",
            options = buildList {
                add(FilterOption("Todos", state.filters.year == null) { vm.setYear(null) })
                val years = state.years
                val range = if (years.size >= 2) {
                    (years.first()..years.last()).reversed().take(20)
                } else {
                    years.reversed()
                }
                range.forEach { year ->
                    add(
                        FilterOption(year.toString(), state.filters.year == year) {
                            vm.setYear(year)
                        },
                    )
                }
            },
        )
    }
}

private data class FilterOption(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun FilterRow(label: String, options: List<FilterOption>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(92.dp),
        )
        val rowState = rememberLazyListState()
        FadeRow(state = rowState) {
            LazyRow(
                state = rowState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 40.dp),
            ) {
                items(options.size, contentType = { "filter" }) { index ->
                    val option = options[index]
                    Chip(option.label, option.selected, option.onClick)
                }
            }
        }
    }
}
