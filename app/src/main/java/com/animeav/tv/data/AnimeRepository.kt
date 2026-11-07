package com.animeav.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jsoup.Jsoup

class AnimeRepository(
    private val client: AnimeAvClient,
    private val resolver: PlaybackResolver,
    private val anilist: AniListClient,
) {

    private val mediaCache = mutableMapOf<String, MediaDto>()
    private val mediaCacheMutex = Mutex()
    suspend fun home(): HomeDto = client.home()

    suspend fun catalog(
        page: Int = 1,
        genre: String? = null,
        category: String? = null,
        year: Int? = null,
        status: Int? = null,
        order: String? = null,
    ): CatalogDto = client.catalog(page, genre, category, year, status, order)

    suspend fun search(query: String): List<SearchResultDto> = client.search(query)

    suspend fun media(slug: String): MediaDto {
        mediaCacheMutex.withLock { mediaCache[slug] }?.let { return it }

        val media = client.media(slug)
        val enriched = enrichWithAniList(media)

        mediaCacheMutex.withLock {
            if (mediaCache.size >= 80) {
                mediaCache.remove(mediaCache.keys.firstOrNull())
            }
            mediaCache[slug] = enriched
        }
        return enriched
    }

    private suspend fun enrichWithAniList(media: MediaDto): MediaDto = withContext(Dispatchers.Default) {
        val malId = media.malId ?: return@withContext media
        val anilistMedia = runCatching { anilist.mediaByMalId(malId) }.getOrNull()
            ?: return@withContext media

        val description = anilistMedia.description
            ?.let { runCatching { Jsoup.parse(it).text() }.getOrNull() }
            ?.takeIf { it.isNotBlank() }

        media.copy(
            poster = anilistMedia.coverImage?.extraLarge
                ?: anilistMedia.coverImage?.large
                ?: media.poster,
            backdrop = anilistMedia.bannerImage ?: media.backdrop,
            score = media.score ?: anilistMedia.averageScore?.div(10.0),
            synopsis = media.synopsis?.takeIf { it.isNotBlank() } ?: description,
            genres = (media.genres + anilistMedia.genres.map { GenreDto(name = it) })
                .distinctBy { it.name },
            anilistId = anilistMedia.id,
        )
    }

    suspend fun episode(slug: String, number: Int): EpisodePageDto =
        client.episode(slug, number)

    suspend fun resolvePlayback(
        episodePage: EpisodePageDto,
        preference: ServerPreference,
    ): PlaybackSource? {
        val embeds = episodePage.embeds["SUB"]
            ?: episodePage.embeds["DUB"]
            ?: episodePage.embeds.values.firstOrNull()
            ?: emptyList()
        return resolver.resolve(embeds, preference)
    }
}
