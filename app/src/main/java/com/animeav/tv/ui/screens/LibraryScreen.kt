package com.animeav.tv.ui.screens

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.animeav.tv.AnimeAvApp
import com.animeav.tv.ui.components.AppTopBar
import com.animeav.tv.ui.components.EmptyState
import com.animeav.tv.ui.components.FitRow
import com.animeav.tv.ui.components.EpisodeWideCard
import com.animeav.tv.ui.components.PosterCard
import com.animeav.tv.ui.components.SecondaryButton
import com.animeav.tv.ui.components.SectionHeader
import com.animeav.tv.ui.theme.AnimeRed
import com.animeav.tv.ui.theme.SurfaceElevated
import com.animeav.tv.ui.theme.TextSecondary

@Composable
fun LibraryScreen(nav: NavController) {
    val app = LocalContext.current.applicationContext as AnimeAvApp
    val history by app.library.history.collectAsStateWithLifecycle()
    val myList by app.library.myList.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        AppTopBar(currentRoute = "library") { route ->
            nav.navigate(route) {
                launchSingleTop = true
                popUpTo("home") { saveState = true }
                restoreState = true
            }
        }

        if (history.isEmpty() && myList.isEmpty()) {
            EmptyState("Todavía no has guardado ningún anime ni empezado a ver nada")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().focusGroup(),
                contentPadding = PaddingValues(bottom = 56.dp),
            ) {
                if (myList.isNotEmpty()) {
                    item("my-list-title") { SectionHeader("Mi lista") }
                    item("my-list-row") {
                        FitRow(minItemWidth = 160.dp) { width ->
                            items(myList, key = { it.slug }, contentType = { "media" }) { saved ->
                                PosterCard(
                                    title = saved.title,
                                    imageUrl = "https://cdn.animeav1.com/covers/${saved.mediaId}.jpg",
                                    onClick = { nav.navigate("detail/${saved.slug}") },
                                    width = width,
                                )
                            }
                        }
                    }
                }

                if (history.isNotEmpty()) {
                    item("history-title") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Historial", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.weight(1f))
                            SecondaryButton(
                                text = "Borrar historial",
                                onClick = { app.library.clearHistory() },
                            )
                        }
                    }
                    item("history-row") {
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
                                    width = width,
                                    onClick = {
                                        nav.navigate("player/${entry.slug}/${entry.episodeNumber}")
                                    },
                                )
                            }
                        }
                    }
                }

                item("spacer") { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
