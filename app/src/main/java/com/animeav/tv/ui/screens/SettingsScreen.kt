package com.animeav.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
import com.animeav.tv.AniListAuthActivity
import com.animeav.tv.AnimeAvApp
import com.animeav.tv.data.ServerPreference
import com.animeav.tv.ui.components.AppTopBar
import com.animeav.tv.ui.components.Chip
import com.animeav.tv.ui.components.PrimaryButton
import com.animeav.tv.ui.components.SecondaryButton
import com.animeav.tv.ui.components.SectionHeader
import com.animeav.tv.ui.theme.AnimeRed
import com.animeav.tv.ui.theme.SurfaceDark
import com.animeav.tv.ui.theme.SurfaceElevated
import com.animeav.tv.ui.theme.TextSecondary

@Composable
fun SettingsScreen(nav: NavController) {
    val app = LocalContext.current.applicationContext as AnimeAvApp
    val context = LocalContext.current
    var server by remember { mutableStateOf(app.settings.serverPreference) }
    var quality by remember { mutableStateOf(app.settings.maxQuality) }
    var anilistClientId by remember { mutableStateOf(app.settings.anilistClientId) }
    var anilistToken by remember { mutableStateOf(app.settings.anilistToken) }
    var anilistUser by remember { mutableStateOf(app.settings.anilistUserName) }

    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        anilistToken = app.settings.anilistToken
        anilistUser = app.settings.anilistUserName
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        AppTopBar(currentRoute = "settings") { route ->
            nav.navigate(route) {
                launchSingleTop = true
                popUpTo("home") { saveState = true }
                restoreState = true
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().focusGroup(),
            contentPadding = PaddingValues(bottom = 56.dp),
        ) {
            item { SectionHeader("Reproducción") }
            item {
                SettingCard(
                    title = "Servidor preferido",
                    description = "Automático intenta primero el HLS de AnimeAV1 (ExoPlayer + Cronet) " +
                        "y luego MP4Upload como alternativa.",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(
                            text = "Automático",
                            selected = server == ServerPreference.AUTO,
                            onClick = {
                                server = ServerPreference.AUTO
                                app.settings.serverPreference = ServerPreference.AUTO
                            },
                        )
                        Chip(
                            text = "Nativo (MP4)",
                            selected = server == ServerPreference.NATIVE,
                            onClick = {
                                server = ServerPreference.NATIVE
                                app.settings.serverPreference = ServerPreference.NATIVE
                            },
                        )
                        Chip(
                            text = "Web (HLS)",
                            selected = server == ServerPreference.WEB,
                            onClick = {
                                server = ServerPreference.WEB
                                app.settings.serverPreference = ServerPreference.WEB
                            },
                        )
                    }
                }
            }
            item {
                SettingCard(
                    title = "Calidad máxima",
                    description = "La calidad real depende del servidor elegido y del ancho de banda de tu conexión.",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("auto" to "Automática", "1080" to "1080p", "720" to "720p", "480" to "480p")
                            .forEach { (value, label) ->
                                Chip(
                                    text = label,
                                    selected = quality == value,
                                    onClick = {
                                        quality = value
                                        app.settings.maxQuality = value
                                    },
                                )
                            }
                    }
                }
            }

            item { SectionHeader("AniList") }
            item {
                SettingCard(
                    title = "Sincronización con AniList",
                    description = "Guarda tu progreso en AniList. Crea una app en anilist.co/settings/developer " +
                        "con redirect URI https://anilist.co/api/v2/oauth/pin y pega aquí su Client ID.",
                ) {
                    Column {
                        if (anilistToken.isNullOrBlank()) {
                            BasicTextField(
                                value = anilistClientId,
                                onValueChange = {
                                    anilistClientId = it
                                    app.settings.anilistClientId = it
                                },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                                cursorBrush = SolidColor(AnimeRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceElevated, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                decorationBox = { inner ->
                                    Box {
                                        if (anilistClientId.isEmpty()) {
                                            Text(
                                                "Client ID de AniList",
                                                color = TextSecondary,
                                                fontSize = 18.sp,
                                            )
                                        }
                                        inner()
                                    }
                                },
                            )
                            Spacer(Modifier.height(12.dp))
                            PrimaryButton(
                                text = "Conectar AniList",
                                onClick = {
                                    if (anilistClientId.isNotBlank()) {
                                        authLauncher.launch(
                                            AniListAuthActivity.intent(context, anilistClientId),
                                        )
                                    }
                                },
                            )
                        } else {
                            Text(
                                text = "Conectado como ${anilistUser ?: "usuario de AniList"}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(12.dp))
                            SecondaryButton(
                                text = "Desconectar",
                                onClick = {
                                    app.settings.anilistToken = null
                                    app.settings.anilistUserName = null
                                    anilistToken = null
                                    anilistUser = null
                                },
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Datos") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 8.dp),
                ) {
                    SecondaryButton(
                        text = "Borrar historial",
                        onClick = { app.library.clearHistory() },
                    )
                }
            }

            item { SectionHeader("Acerca de") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceDark)
                        .padding(24.dp),
                ) {
                    Text("AnimeAV TV · 1.0.0", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Cliente no oficial para Android TV / Google TV. " +
                            "No aloja ningún archivo de video ni audio: los datos, imágenes y streams " +
                            "se obtienen en tiempo real de animeav1.com y de sus servidores de video " +
                            "de terceros. La app no está afiliada a AnimeAV1.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Uso personal y educativo. Respeta las leyes de tu país y los términos " +
                            "de los servicios que utilices.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .padding(22.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(description, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        content()
    }
}
