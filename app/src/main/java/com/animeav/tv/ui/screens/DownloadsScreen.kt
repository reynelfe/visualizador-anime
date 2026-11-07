package com.animeav.tv.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.animeav.tv.AnimeAvApp
import com.animeav.tv.LocalPlayerActivity
import com.animeav.tv.data.DownloadEntry
import com.animeav.tv.ui.components.AppTopBar
import com.animeav.tv.ui.components.EmptyState
import com.animeav.tv.ui.components.SecondaryButton
import com.animeav.tv.ui.components.SectionHeader
import com.animeav.tv.ui.components.rememberImageRequest
import com.animeav.tv.ui.theme.AnimeRed
import com.animeav.tv.ui.theme.SurfaceDark
import com.animeav.tv.ui.theme.TextSecondary
import com.animeav.tv.work.DownloadScheduler

@Composable
fun DownloadsScreen(nav: NavController) {
    val app = LocalContext.current.applicationContext as AnimeAvApp
    val entries by app.downloadsStore.entries.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        AppTopBar(currentRoute = "downloads") { route ->
            nav.navigate(route) {
                launchSingleTop = true
                popUpTo("home") { saveState = true }
                restoreState = true
            }
        }

        if (entries.isEmpty()) {
            EmptyState("No tienes descargas todavía. Abre un episodio en MP4Upload y pulsa “Descargar”.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 56.dp),
            ) {
                item("downloads-title") {
                    SectionHeader("Descargas · ${entries.size}")
                }
                items(entries, key = { it.fileName }, contentType = { "download" }) { entry ->
                    DownloadRow(entry = entry, app = app)
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(entry: DownloadEntry, app: AnimeAvApp) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(190.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(com.animeav.tv.ui.theme.SurfaceElevated),
        ) {
            AsyncImage(
                model = rememberImageRequest(
                    "https://cdn.animeav1.com/thumbnails/${entry.mediaId}.jpg",
                    380,
                    214,
                ),
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Episodio ${entry.episodeNumber} · ${statusLabel(entry)}",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!entry.isCompleted) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { entry.progress },
                    color = AnimeRed,
                    trackColor = com.animeav.tv.ui.theme.SurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (entry.isCompleted) {
                SecondaryButton(
                    text = "Reproducir",
                    onClick = {
                        val file = app.downloadsStore.fileFor(entry.fileName)
                        if (file.exists()) {
                            context.startActivity(
                                LocalPlayerActivity.intent(
                                    context,
                                    file.absolutePath,
                                    "${entry.title} · EP ${entry.episodeNumber}",
                                ),
                            )
                        } else {
                            Toast.makeText(context, "El archivo ya no existe", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
            SecondaryButton(
                text = if (entry.isCompleted) "Eliminar" else "Cancelar",
                onClick = {
                    DownloadScheduler.cancel(context, entry.fileName)
                    app.downloadsStore.remove(entry.fileName)
                },
            )
        }
    }
}

private fun statusLabel(entry: DownloadEntry): String = when (entry.status) {
    DownloadEntry.STATUS_COMPLETED -> "Descargado"
    DownloadEntry.STATUS_DOWNLOADING -> "Descargando ${(entry.progress * 100).toInt()}%"
    DownloadEntry.STATUS_FAILED -> "Error de descarga"
    else -> "En cola"
}
