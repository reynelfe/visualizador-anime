package com.animeav.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.animeav.tv.AnimeAvApp
import com.animeav.tv.data.LatestEpisodeDto
import com.animeav.tv.data.MediaDto
import com.animeav.tv.data.WatchEntry
import com.animeav.tv.data.backdropUrl
import com.animeav.tv.data.coverUrl
import com.animeav.tv.data.statusLabel
import com.animeav.tv.data.thumbnailUrl
import com.animeav.tv.ui.components.AppTopBar
import com.animeav.tv.ui.components.EpisodeWideCard
import com.animeav.tv.ui.components.ErrorState
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
import com.animeav.tv.ui.vm.HomeContent
import com.animeav.tv.ui.vm.HomeViewModel
import com.animeav.tv.ui.vm.UiState
import com.animeav.tv.ui.vm.appViewModelFactory

@Composable
fun HomeScreen(nav: NavController) {
    val app = LocalContext.current.applicationContext as AnimeAvApp
    val vm: HomeViewModel = viewModel(
        factory = appViewModelFactory { HomeViewModel(it.repository, it.library) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        AppTopBar(currentRoute = "home") { route -> nav.navigateMain(route) }
        when (val snapshot = state) {
            is UiState.Loading -> LoadingState("Cargando anime…")
            is UiState.Error -> ErrorState(snapshot.message) { vm.load() }
            is UiState.Success -> HomeContentList(
                content = snapshot.data,
                history = history,
                nav = nav,
            )
        }
    }
}

private fun NavController.navigateMain(route: String) {
    navigate(route) {
        launchSingleTop = true
        popUpTo("home") { saveState = true }
        restoreState = true
    }
}

@Composable
private fun HomeContentList(
    content: HomeContent,
    history: List<WatchEntry>,
    nav: NavController,
) {
    // El inicio tiene pocas secciones: componerlas todas de antemano evita el
    // tirón de componer una fila nueva al mover el foco entre secciones.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Hero(content.home.featured.firstOrNull(), nav)

        if (history.isNotEmpty()) {
            SectionHeader("Continuar viendo")
            FitRow(minItemWidth = 280.dp) { width ->
                items(history, key = { it.slug }, contentType = { "history" }) { entry ->
                    EpisodeWideCard(
                        title = entry.title,
                        episodeNumber = entry.episodeNumber,
                        imageUrl = "https://cdn.animeav1.com/thumbnails/${entry.mediaId}.jpg",
                        progress = entry.progressFraction,
                        subtitle = if (entry.durationMs > 0) {
                            "Visto ${(entry.progressFraction * 100).toInt()}%"
                        } else null,
                        onClick = { nav.navigate("player/${entry.slug}/${entry.episodeNumber}") },
                        width = width,
                    )
                }
            }
        }

        if (content.home.latestEpisodes.isNotEmpty()) {
            SectionHeader("Últimos episodios")
            FitRow(minItemWidth = 280.dp) { width ->
                items(content.home.latestEpisodes, key = { "ep-${it.id}" }, contentType = { "episode" }) { episode ->
                    LatestEpisodeCard(episode, nav, width)
                }
            }
        }

        if (content.home.latestMedia.isNotEmpty()) {
            SectionHeader("Recién agregados")
            FitRow(minItemWidth = 160.dp) { width ->
                items(content.home.latestMedia, key = { "media-${it.id}" }, contentType = { "media" }) { media ->
                    MediaPoster(media, nav, width = width)
                }
            }
        }

        if (content.popular.isNotEmpty()) {
            SectionHeader("Populares")
            FitRow(minItemWidth = 160.dp) { width ->
                items(content.popular, key = { "pop-${it.id}" }, contentType = { "media" }) { media ->
                    MediaPoster(media, nav, badge = "TOP", width = width)
                }
            }
        }

        Spacer(Modifier.height(56.dp))
    }
}

@Composable
private fun Hero(
    media: MediaDto?,
    nav: NavController,
) {
    if (media == null) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp),
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 48.dp, vertical = 36.dp)
                .widthIn(max = 780.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                media.genres.take(2).forEach { genre ->
                    StaticChip(genre.name)
                }
                media.category?.let { category ->
                    StaticChip(category.name, highlighted = true)
                }
            }
            Spacer(Modifier.height(14.dp))
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
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = media.statusLabel(),
                    color = com.animeav.tv.ui.theme.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                media.episodesCount?.let {
                    Text(
                        text = " · $it episodios",
                        color = com.animeav.tv.ui.theme.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = media.synopsis.orEmpty(),
                color = Color(0xFFD6D6DE),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(
                    text = "Ver ahora",
                    onClick = { nav.navigate("player/${media.slug}/1") },
                )
                SecondaryButton(
                    text = "Más información",
                    onClick = { nav.navigate("detail/${media.slug}") },
                )
            }
        }
    }
}

@Composable
private fun LatestEpisodeCard(episode: LatestEpisodeDto, nav: NavController, width: Dp = 300.dp) {
    val media = episode.media ?: return
    EpisodeWideCard(
        title = media.title,
        episodeNumber = episode.number,
        imageUrl = episode.thumbnailUrl,
        subtitle = "Episodio ${episode.number}",
        onClick = { nav.navigate("player/${media.slug}/${episode.number}") },
        width = width,
    )
}

@Composable
private fun MediaPoster(
    media: MediaDto,
    nav: NavController,
    badge: String? = null,
    width: Dp = 178.dp,
) {
    PosterCard(
        title = media.title,
        imageUrl = media.coverUrl,
        subtitle = media.category?.name ?: media.statusLabel(),
        badge = badge,
        width = width,
        onClick = { nav.navigate("detail/${media.slug}") },
    )
}
