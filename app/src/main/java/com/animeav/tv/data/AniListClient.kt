package com.animeav.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
)

@Serializable
data class AniListCover(
    val extraLarge: String? = null,
    val large: String? = null,
)

@Serializable
data class AniListDate(
    val year: Int? = null,
)

@Serializable
data class AniListMedia(
    val id: Int = 0,
    val title: AniListTitle? = null,
    val description: String? = null,
    val coverImage: AniListCover? = null,
    val bannerImage: String? = null,
    val averageScore: Int? = null,
    val genres: List<String> = emptyList(),
    val episodes: Int? = null,
    val status: String? = null,
    val startDate: AniListDate? = null,
)

@Serializable
data class AniListViewer(
    val id: Int = 0,
    val name: String = "",
)

class AniListClient(
    private val http: OkHttpClient,
    private val json: Json,
) {

    suspend fun mediaByMalId(malId: Int): AniListMedia? = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}malId: Int) {
              Media(idMal: ${'$'}malId, type: ANIME) {
                id
                title { romaji english native }
                description(asHtml: false)
                coverImage { extraLarge large }
                bannerImage
                averageScore
                genres
                episodes
                status
                startDate { year }
              }
            }
        """.trimIndent()

        val payload = buildJsonObject {
            put("query", query)
            putJsonObject("variables") { put("malId", malId) }
        }.toString()

        val element = post(payload) ?: return@withContext null
        val media = element.jsonObject["data"]?.jsonObject?.get("Media")
            ?: return@withContext null
        runCatching { json.decodeFromJsonElement<AniListMedia>(media) }.getOrNull()
    }

    suspend fun viewer(token: String): AniListViewer? = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("query", "query { Viewer { id name } }")
        }.toString()

        val element = post(payload, token) ?: return@withContext null
        val viewer = element.jsonObject["data"]?.jsonObject?.get("Viewer")
            ?: return@withContext null
        runCatching { json.decodeFromJsonElement<AniListViewer>(viewer) }.getOrNull()
    }

    suspend fun saveProgress(
        token: String,
        mediaId: Int,
        progress: Int,
    ): Boolean = withContext(Dispatchers.IO) {
        val query = """
            mutation (${'$'}mediaId: Int, ${'$'}progress: Int) {
              SaveMediaListEntry(mediaId: ${'$'}mediaId, progress: ${'$'}progress) {
                id
                progress
              }
            }
        """.trimIndent()

        val payload = buildJsonObject {
            put("query", query)
            putJsonObject("variables") {
                put("mediaId", mediaId)
                put("progress", progress)
            }
        }.toString()

        val element = post(payload, token) ?: return@withContext false
        element.jsonObject["data"]?.jsonObject?.get("SaveMediaListEntry") != null
    }

    private fun post(payload: String, token: String? = null): JsonElement? =
        runCatching {
            val builder = Request.Builder()
                .url(GRAPHQL_URL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))

            if (token != null) {
                builder.header("Authorization", "Bearer $token")
            }

            http.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val text = response.body?.string() ?: return@use null
                json.parseToJsonElement(text)
            }
        }.getOrNull()

    companion object {
        private const val GRAPHQL_URL = "https://graphql.anilist.co"
    }
}
