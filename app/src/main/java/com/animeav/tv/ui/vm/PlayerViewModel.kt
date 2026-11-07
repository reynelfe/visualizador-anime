package com.animeav.tv.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animeav.tv.data.AnimeRepository
import com.animeav.tv.data.EmbedDto
import com.animeav.tv.data.EpisodeDto
import com.animeav.tv.data.MediaDto
import com.animeav.tv.data.PlaybackSource
import com.animeav.tv.data.ServerPreference
import com.animeav.tv.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val slug: String = "",
    val number: Int = 1,
    val loading: Boolean = true,
    val error: String? = null,
    val media: MediaDto? = null,
    val episode: EpisodeDto? = null,
    val embeds: List<EmbedDto> = emptyList(),
    val source: PlaybackSource? = null,
    val selectedServer: String? = null,
    val preference: ServerPreference = ServerPreference.AUTO,
)

class PlayerViewModel(
    private val repo: AnimeRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    fun load(slug: String, number: Int, server: String? = null) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    slug = slug,
                    number = number,
                    loading = true,
                    error = null,
                    source = null,
                )
            }
            runCatching {
                val page = repo.episode(slug, number)
                val embeds = page.embeds["SUB"]
                    ?: page.embeds["DUB"]
                    ?: page.embeds.values.firstOrNull()
                    ?: emptyList()
                val preference = settings.serverPreference
                val source = if (server != null) {
                    val embed = embeds.firstOrNull { it.server == server }
                    if (embed != null) {
                        repo.resolvePlayback(
                            page.copy(embeds = mapOf("SUB" to listOf(embed))),
                            preference,
                        )
                    } else {
                        repo.resolvePlayback(page, preference)
                    }
                } else {
                    repo.resolvePlayback(page, preference)
                }

                PlayerUiState(
                    slug = slug,
                    number = number,
                    loading = false,
                    media = page.media,
                    episode = page.episode,
                    embeds = embeds,
                    source = source,
                    selectedServer = when (val s = source) {
                        is PlaybackSource.Native -> s.server
                        is PlaybackSource.Web -> s.server
                        null -> null
                    },
                    preference = preference,
                )
            }.onSuccess { state ->
                _state.value = state
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "No se pudo cargar el episodio",
                    )
                }
            }
        }
    }

    fun selectServer(server: String) {
        val snapshot = _state.value
        if (snapshot.slug.isBlank()) return
        load(snapshot.slug, snapshot.number, server)
    }

    fun retry() {
        val snapshot = _state.value
        if (snapshot.slug.isBlank()) return
        load(snapshot.slug, snapshot.number)
    }
}
