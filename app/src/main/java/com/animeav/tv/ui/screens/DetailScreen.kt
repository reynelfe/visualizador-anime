package com.animeav.tv.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.animeav.tv.AnimeAvApp
import com.animeav.tv.data.MediaDto
import com.animeav.tv.data.MediaRefDto
import com.animeav.tv.data.backdropUrl
import com.animeav.tv.data.coverUrl
import com.animeav.tv.data.statusLabel
import com.animeav.tv.ui.components.EpisodeNumberCard
import com.animeav.tv.ui.components.ErrorState
import com.animeav.tv.ui.components.FocusIconButton
import com.animeav.tv.ui.components.FitRow
import com.animeav.tv.ui.components.HorizontalGradientScrim
import com.animeav.tv.ui.components.LoadingState
import com.animeav.tv.ui.components.PosterCard
import com.animeav.tv.ui.components.PrimaryButton
import com.animeav.tv.ui.components.SecondaryButton
import com.animeav.tv.ui.components.SectionHeader
import com.animeav.tv.ui.components.StaticChip
import com.animeav.tv.ui.components.VerticalGradientScrim
import com.animeav.tv.ui.components.rememberImageRequest
import com.animeav.tv.ui.theme.TextSecondary
import com.animeav.tv.ui.vm.DetailUiState
import com.animeav.tv.ui.vm.DetailViewModel
import com.animeav.tv.ui.vm.appViewModelFactory

@Composable
fun DetailScreen(nav: NavController, slug: String) {
    val app = LocalContext.current.applicationContext as AnimeAvApp
    val vm: DetailViewModel = viewModel(
        factory = appViewModelFactory { DetailViewModel(it.repository, it.library) },
    )
    LaunchedEffect(slug) { vm.load(slug) }
    val state by vm.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.loading -> LoadingState("Cargando ficha…")
            state.error != null -> ErrorState(state.error ?: "Error") { vm.retry() }
            state.media != null -> DetailContent(state, vm, nav)
        }
    }
}

@Composable
private fun DetailContent(
    state: DetailUiState,
    vm: DetailViewModel,
    nav: NavController,
) {
    val media = state.media ?: return
    val context = LocalContext.current
    val progress = state.progress
    val startEpisode = progress?.episodeNumber ?: 1
    val playLabel = if (progress != null) "Continuar EP $startEpisode" else "Ver EP 1"
    val gridState = rememberLazyGridState()
    val playFocus = remember { FocusRequester() }

    LaunchedEffect(media.slug) {
        runCatching { playFocus.requestFocus() }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(8),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 40.dp, end = 40.dp, bottom = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            DetailHero(
                media = media,
                inMyList = state.inMyList,
                playLabel = playLabel,
                playFocus = playFocus,
                onBack = { nav.popBackStack() },
                onPlay = { nav.navigate("player/${media.slug}/$startEpisode") },
                onToggleList = { vm.toggleMyList() },
                onTrailer = media.trailer?.takeIf { it.isNotBlank() }?.let { trailer ->
                    {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$trailer"))
                        runCatching { context.startActivity(intent) }
                    }
                },
            )
        }

        val relations = media.relations
            .mapNotNull { it.destination }
            .distinctBy { it.slug }
            .take(24)
        if (relations.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("Relacionados")
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                FitRow(minItemWidth = 150.dp) { width ->
                    items(relations, key = { it.slug }, contentType = { "related" }) { related ->
                        RelatedCard(related, width) {
                            nav.navigate("detail/${related.slug}")
                        }
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader("Episodios · ${media.episodes.size}")
        }

        if (media.episodes.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Este título todavía no tiene episodios publicados.",
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp),
                )
            }
        } else {
            gridItems(media.episodes, key = { it.number }, contentType = { "episode" }) { episode ->
                EpisodeNumberCard(
                    number = episode.number,
                    title = episode.title,
                    current = progress?.episodeNumber == episode.number,
                    onClick = { nav.navigate("player/${media.slug}/${episode.number}") },
                )
            }
        }
    }
}

@Composable
private fun DetailHero(
    media: MediaDto,
    inMyList: Boolean,
    playLabel: String,
    playFocus: FocusRequester,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onToggleList: () -> Unit,
    onTrailer: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(470.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            val backdropRequest = rememberImageRequest(media.backdropUrl, 1920, 1080)
            AsyncImage(
                model = backdropRequest,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            HorizontalGradientScrim(Modifier.fillMaxWidth(0.8f).fillMaxHeight())
            VerticalGradientScrim(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.BottomCenter),
            )
        }

        FocusIconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 48.dp, vertical = 28.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            AsyncImage(
                model = rememberImageRequest(media.coverUrl, 400, 600),
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(176.dp)
                    .height(264.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF20202B)),
            )
            Spacer(Modifier.width(28.dp))
            Column(modifier = Modifier.widthIn(max = 1000.dp)) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    media.score?.let {
                        Text(
                            text = "★ %.1f".format(it),
                            color = Color(0xFFFFC107),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(14.dp))
                    }
                    Text(
                        text = media.statusLabel(),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    media.startDate?.take(4)?.let { year ->
                        Text(" · $year", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = " · ${media.episodes.size} episodios",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (media.mature) {
                        Spacer(Modifier.width(10.dp))
                        StaticChip("+18", highlighted = true)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    media.category?.let { StaticChip(it.name, highlighted = true) }
                    media.genres.take(5).forEach { StaticChip(it.name) }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = media.synopsis.orEmpty(),
                    color = Color(0xFFD7D7DF),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(
                        text = playLabel,
                        onClick = onPlay,
                        modifier = Modifier.focusRequester(playFocus),
                    )
                    SecondaryButton(
                        text = if (inMyList) "Quitar de mi lista" else "Mi lista",
                        onClick = onToggleList,
                    )
                    if (onTrailer != null) {
                        SecondaryButton(text = "Tráiler", onClick = onTrailer)
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedCard(media: MediaRefDto, width: Dp = 150.dp, onClick: () -> Unit) {
    PosterCard(
        title = media.title,
        imageUrl = media.coverUrl,
        subtitle = media.startDate?.take(4),
        onClick = onClick,
        width = width,
    )
}
