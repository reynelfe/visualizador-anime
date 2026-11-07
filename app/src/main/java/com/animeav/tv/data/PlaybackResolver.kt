package com.animeav.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

sealed interface PlaybackSource {
    data class Native(
        val url: String,
        val server: String,
        val headers: Map<String, String>,
    ) : PlaybackSource

    data class Web(
        val url: String,
        val server: String,
    ) : PlaybackSource
}

enum class ServerPreference {
    AUTO,
    NATIVE,
    WEB,
}

class PlaybackResolver(
    private val jsonClient: OkHttpClient,
) {
    private val mediaClient: OkHttpClient = jsonClient.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Elige la mejor fuente disponible. Primero intenta MP4Upload, que entrega
     * un MP4 progresivo que ExoPlayer reproduce sin problemas. Si no hay,
     * devuelve el embed HLS (zilla) para el reproductor WebView.
     */
    suspend fun resolve(
        embeds: List<EmbedDto>,
        preference: ServerPreference = ServerPreference.AUTO,
    ): PlaybackSource? {
        if (embeds.isEmpty()) return null

        val mp4Upload = embeds.firstOrNull { it.server.equals("MP4Upload", true) }
        val zilla = embeds.firstOrNull {
            it.server.equals("HLS", true) || it.url.contains("zilla-networks.com", true)
        }
        val other = embeds.firstOrNull {
            it !== mp4Upload && it !== zilla && !it.url.isBlank()
        }

        val hlsNative = zilla?.let { embed ->
            val m3u8 = zillaM3u8(embed.url) ?: return@let null
            PlaybackSource.Native(
                url = m3u8,
                server = embed.server,
                headers = mapOf(
                    "User-Agent" to AnimeAvClient.USER_AGENT,
                    "Referer" to "https://player.zilla-networks.com/",
                    "Origin" to "https://player.zilla-networks.com",
                    "Accept" to "*/*",
                    "Accept-Language" to "es-ES,es;q=0.9,en;q=0.8",
                    "Sec-Fetch-Dest" to "video",
                    "Sec-Fetch-Mode" to "no-cors",
                    "Sec-Fetch-Site" to "same-origin",
                ),
            )
        }

        when (preference) {
            ServerPreference.WEB -> return webSource(zilla ?: other ?: mp4Upload)
            ServerPreference.NATIVE -> {
                if (mp4Upload != null) {
                    nativeFromMp4Upload(mp4Upload)?.let { return it }
                }
                if (hlsNative != null) return hlsNative
                return webSource(zilla ?: other ?: mp4Upload)
            }
            ServerPreference.AUTO -> {
                if (hlsNative != null) return hlsNative
                if (mp4Upload != null) {
                    nativeFromMp4Upload(mp4Upload)?.let { return it }
                }
                if (other != null) return webSource(other)
                return null
            }
        }
    }

    private fun webSource(embed: EmbedDto?): PlaybackSource? {
        if (embed == null || embed.url.isBlank()) return null
        val url = if (embed.url.startsWith("http")) embed.url else "https:${embed.url}"
        return PlaybackSource.Web(url = url, server = embed.server)
    }

    fun zillaM3u8(url: String): String? {
        val id = ZILLA_PLAY_REGEX.find(url)?.groupValues?.getOrNull(1) ?: return null
        return "https://player.zilla-networks.com/m3u8/$id"
    }

    private suspend fun nativeFromMp4Upload(embed: EmbedDto): PlaybackSource.Native? {
        val url = embed.url.takeIf { it.startsWith("http") } ?: "https:${embed.url}"
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", AnimeAvClient.USER_AGENT)
                    .header("Referer", "${AnimeAvClient.BASE_URL}/")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .get()
                    .build()

                val html = mediaClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body?.string()
                } ?: return@withContext null

                val direct = extractMp4(html) ?: return@withContext null
                PlaybackSource.Native(
                    url = direct,
                    server = embed.server,
                    headers = mapOf(
                        "User-Agent" to AnimeAvClient.USER_AGENT,
                        "Referer" to "https://www.mp4upload.com/",
                        "Accept" to "*/*",
                    ),
                )
            }.getOrNull()
        }
    }

    private fun extractMp4(html: String): String? {
        val document = runCatching { Jsoup.parse(html) }.getOrNull()
        val fromVideo = document?.selectFirst("video[src]")?.attr("src")
            ?.takeIf { it.startsWith("http") }
        if (fromVideo != null) return fromVideo

        val fromSource = document?.selectFirst("source[src]")?.attr("src")
            ?.takeIf { it.startsWith("http") }
        if (fromSource != null) return fromSource

        return MP4_REGEX.find(html)?.value
    }

    companion object {
        private val ZILLA_PLAY_REGEX = Regex("""/play/([A-Fa-f0-9]{32})""")
        private val MP4_REGEX = Regex("""https://[A-Za-z0-9.-]+\.mp4upload\.com:\d+/d/[^"']+/video\.mp4""")
    }
}
