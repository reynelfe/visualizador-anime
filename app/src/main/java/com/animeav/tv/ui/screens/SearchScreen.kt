package com.animeav.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animeav.tv.AnimeAvApp
import com.animeav.tv.ui.components.AppTopBar
import com.animeav.tv.ui.components.EmptyState
import com.animeav.tv.ui.components.ErrorState
import com.animeav.tv.ui.components.LoadingState
import com.animeav.tv.ui.components.PosterCard
import com.animeav.tv.ui.components.PrimaryButton
import com.animeav.tv.ui.theme.AnimeRed
import com.animeav.tv.ui.theme.SurfaceDark
import com.animeav.tv.ui.theme.TextSecondary
import com.animeav.tv.ui.vm.SearchUiState
import com.animeav.tv.ui.vm.SearchViewModel
import com.animeav.tv.ui.vm.appViewModelFactory
import androidx.compose.ui.platform.LocalContext

@Composable
fun SearchScreen(nav: NavController) {
    val app = LocalContext.current.applicationContext as AnimeAvApp
    val vm: SearchViewModel = viewModel(
        factory = appViewModelFactory { SearchViewModel(it.repository) },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        AppTopBar(currentRoute = "search") { route ->
            nav.navigate(route) {
                launchSingleTop = true
                popUpTo("home") { saveState = true }
                restoreState = true
            }
        }
        SearchBox(state, vm)
        when {
            state.loading -> LoadingState("Buscando…")
            state.error != null -> ErrorState(state.error ?: "Error") { vm.searchNow() }
            state.hasSearched && state.results.isEmpty() ->
                EmptyState("Sin resultados para \"${state.query}\"")
            !state.hasSearched ->
                EmptyState("Escribe el nombre de un anime y pulsa Buscar")
            else -> SearchResults(state, nav)
        }
    }
}

@Composable
private fun SearchBox(state: SearchUiState, vm: SearchViewModel) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = state.query,
            onValueChange = vm::onQueryChanged,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            ),
            cursorBrush = SolidColor(AnimeRed),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.searchNow() }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) AnimeRed else Color(0xFF333342),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 22.dp, vertical = 18.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (state.query.isEmpty()) {
                        Text(
                            text = "Buscar anime…",
                            color = TextSecondary,
                            fontSize = 22.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
        Spacer(Modifier.width(14.dp))
        PrimaryButton(text = "Buscar", onClick = { vm.searchNow() })
    }
}

@Composable
private fun SearchResults(state: SearchUiState, nav: NavController) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxSize().focusGroup(),
        contentPadding = PaddingValues(start = 40.dp, end = 40.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text(
                text = "${state.results.size} resultados",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
        items(state.results, key = { it.id }, contentType = { "media" }) { result ->
            PosterCard(
                title = result.title,
                imageUrl = "https://cdn.animeav1.com/covers/${result.id}.jpg",
                onClick = { nav.navigate("detail/${result.slug}") },
            )
        }
    }
}
