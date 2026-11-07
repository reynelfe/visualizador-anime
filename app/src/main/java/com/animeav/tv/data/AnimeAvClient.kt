package com.animeav.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

class AnimeAvClient(
    private val http: OkHttpClient,
    private val json: Json,
) {

    suspend fun home(): HomeDto = withContext(Dispatchers.IO) {
        json.decodeFromJsonElement<HomeDto>(request("/__data.json"))
    }

    suspend fun catalog(
        page: Int = 1,
        genre: String? = null,
        category: String? = null,
        year: Int? = null,
        status: Int? = null,
        order: String? = null,
    ): CatalogDto = withContext(Dispatchers.IO) {
        val params = buildList {
            add("page=$page")
            genre?.let { add("genre=${it.urlEncoded()}") }
            category?.let { add("category=${it.urlEncoded()}") }
            year?.let { add("year=$it") }
            status?.let { add("status=$it") }
            order?.let { add("order=${it.urlEncoded()}") }
        }
        val path = "/catalogo/__data.json?" + params.joinToString("&")
        json.decodeFromJsonElement<CatalogDto>(request(path))
    }

    suspend fun search(query: String): List<SearchResultDto> = withContext(Dispatchers.IO) {
        val payload = buildJsonObject { put("query", query) }.toString()
        json.decodeFromJsonElement<List<SearchResultDto>>(
            requestRaw("/api/search", method = "POST", body = payload),
        )
    }

    suspend fun media(slug: String): MediaDto = withContext(Dispatchers.IO) {
        val root = request("/media/${slug.urlEncoded()}/__data.json")
        json.decodeFromJsonElement<MediaPageDto>(root).media
    }

    suspend fun episode(slug: String, number: Int): EpisodePageDto = withContext(Dispatchers.IO) {
        val root = request("/media/${slug.urlEncoded()}/$number/__data.json")
        json.decodeFromJsonElement<EpisodePageDto>(root)
    }

    private suspend fun request(
        path: String,
        method: String = "GET",
        body: String? = null,
    ): JsonElement {
        val element = requestRaw(path, method, body)
        val root = element as? JsonObject ?: error("Formato inesperado al consultar $path")
        return Devalue.decode(root)
    }

    private suspend fun requestRaw(
        path: String,
        method: String = "GET",
        body: String? = null,
    ): JsonElement = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(BASE_URL + path)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            .header("Referer", "$BASE_URL/")

        when (method) {
            "POST" -> builder.post(
                (body ?: "").toRequestBody("application/json; charset=utf-8".toMediaType()),
            )
            else -> builder.get()
        }

        http.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code} al consultar $path")
            }
            val text = response.body?.string()?.takeIf { it.isNotBlank() }
                ?: error("Respuesta vacía de $path")
            json.parseToJsonElement(text)
        }
    }

    companion object {
        const val BASE_URL = "https://animeav1.com"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; SHIELD Android TV) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        private fun String.urlEncoded(): String = URLEncoder.encode(this, "UTF-8")
    }
}
