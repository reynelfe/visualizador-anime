package com.animeav.tv.data

import kotlinx.serialization.Serializable

@Serializable
data class GenreDto(
    val id: Int = 0,
    val name: String = "",
    val slug: String = "",
    val type: Int = 0,
)

@Serializable
data class CategoryDto(
    val id: Int = 0,
    val name: String = "",
    val slug: String = "",
)

@Serializable
data class MediaRefDto(
    val id: Int = 0,
    val slug: String = "",
    val title: String = "",
    val startDate: String? = null,
    val poster: String? = null,
)

@Serializable
data class EpisodeLiteDto(
    val id: Int = 0,
    val number: Int = 0,
    val title: String? = null,
)

@Serializable
data class RelationDto(
    val type: Int = 0,
    val destination: MediaRefDto? = null,
)

@Serializable
data class MediaDto(
    val id: Int = 0,
    val slug: String = "",
    val title: String = "",
    val aka: Map<String, String> = emptyMap(),
    val synopsis: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val trailer: String? = null,
    val status: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val score: Double? = null,
    val votes: Int? = null,
    val episodesCount: Int? = null,
    val malId: Int? = null,
    val anilistId: Int? = null,
    val mature: Boolean = false,
    val genres: List<GenreDto> = emptyList(),
    val category: CategoryDto? = null,
    val episodes: List<EpisodeLiteDto> = emptyList(),
    val relations: List<RelationDto> = emptyList(),
)

@Serializable
data class LatestEpisodeDto(
    val id: Int = 0,
    val number: Int = 0,
    val media: MediaRefDto? = null,
    val createdAt: String? = null,
    val publishedAt: String? = null,
    val commentsCount: Int = 0,
)

@Serializable
data class HomeDto(
    val featured: List<MediaDto> = emptyList(),
    val latestEpisodes: List<LatestEpisodeDto> = emptyList(),
    val latestMedia: List<MediaDto> = emptyList(),
)

@Serializable
data class PaginationDto(
    val currentPage: Int = 1,
    val recordsPerPage: Int = 20,
    val totalPages: Int = 1,
    val totalRecords: Int = 0,
)

@Serializable
data class FiltersDto(
    val genres: List<Int> = emptyList(),
    val categories: List<Int> = emptyList(),
    val status: Int? = null,
    val years: List<Int> = emptyList(),
    val letter: String? = null,
)

@Serializable
data class CatalogDto(
    val results: List<MediaDto> = emptyList(),
    val total: Int = 0,
    val categoriesIdsMap: Map<String, CategoryDto> = emptyMap(),
    val genresIdsMap: Map<String, GenreDto> = emptyMap(),
    val filters: FiltersDto? = null,
    val years: List<Int> = emptyList(),
    val orderKey: String = "default",
    val pagination: PaginationDto? = null,
)

@Serializable
data class MediaPageDto(
    val media: MediaDto = MediaDto(),
)

@Serializable
data class EpisodeDto(
    val id: Int = 0,
    val mediaId: Int = 0,
    val title: String? = null,
    val number: Int = 0,
    val season: Int? = null,
    val relativeNumber: Int? = null,
    val variants: Map<String, Int> = emptyMap(),
    val filler: Boolean = false,
)

@Serializable
data class EmbedDto(
    val server: String = "",
    val url: String = "",
)

@Serializable
data class EpisodePageDto(
    val media: MediaDto = MediaDto(),
    val episode: EpisodeDto = EpisodeDto(),
    val embeds: Map<String, List<EmbedDto>> = emptyMap(),
    val downloads: Map<String, List<EmbedDto>> = emptyMap(),
)

@Serializable
data class SearchResultDto(
    val id: String = "",
    val title: String = "",
    val slug: String = "",
)

/** URL de portada. La CDN usa el id numérico del media. */
val MediaDto.coverUrl: String
    get() = poster?.takeIf { it.isNotBlank() } ?: "https://cdn.animeav1.com/covers/$id.jpg"

val MediaDto.backdropUrl: String
    get() = backdrop?.takeIf { it.isNotBlank() } ?: "https://cdn.animeav1.com/backdrops/$id.jpg"

val MediaRefDto.coverUrl: String
    get() = poster?.takeIf { it.isNotBlank() } ?: "https://cdn.animeav1.com/covers/$id.jpg"

val LatestEpisodeDto.thumbnailUrl: String
    get() = "https://cdn.animeav1.com/thumbnails/${media?.id ?: 0}.jpg"

fun MediaDto.statusLabel(): String = when (status) {
    1 -> "En emisión"
    2 -> "Finalizado"
    3 -> "Próximamente"
    else -> "Desconocido"
}
