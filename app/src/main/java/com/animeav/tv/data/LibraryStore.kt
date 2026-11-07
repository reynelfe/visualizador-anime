package com.animeav.tv.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class WatchEntry(
    val slug: String = "",
    val title: String = "",
    val mediaId: Int = 0,
    val episodeNumber: Int = 1,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

@Serializable
data class SavedMedia(
    val slug: String = "",
    val title: String = "",
    val mediaId: Int = 0,
)

@Serializable
private data class LibraryData(
    val history: List<WatchEntry> = emptyList(),
    val myList: List<SavedMedia> = emptyList(),
)

class LibraryStore(
    context: Context,
    private val json: Json,
) {
    private val file = File(context.filesDir, "library.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _history = MutableStateFlow<List<WatchEntry>>(emptyList())
    val history: StateFlow<List<WatchEntry>> = _history.asStateFlow()

    private val _myList = MutableStateFlow<List<SavedMedia>>(emptyList())
    val myList: StateFlow<List<SavedMedia>> = _myList.asStateFlow()

    init {
        scope.launch {
            mutex.withLock {
                runCatching {
                    if (file.exists()) {
                        val data = json.decodeFromString<LibraryData>(file.readText())
                        _history.value = data.history.sortedByDescending { it.updatedAt }
                        _myList.value = data.myList
                    }
                }
            }
        }
    }

    fun saveProgress(
        slug: String,
        title: String,
        mediaId: Int,
        episodeNumber: Int,
        positionMs: Long,
        durationMs: Long,
    ) {
        val entry = WatchEntry(
            slug = slug,
            title = title,
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(0L),
            updatedAt = System.currentTimeMillis(),
        )
        _history.value = (listOf(entry) + _history.value.filterNot { it.slug == slug })
            .sortedByDescending { it.updatedAt }
            .take(60)
        persist()
    }

    fun progressFor(slug: String): WatchEntry? =
        _history.value.firstOrNull { it.slug == slug }

    fun isInMyList(slug: String): Boolean =
        _myList.value.any { it.slug == slug }

    fun toggleMyList(slug: String, title: String, mediaId: Int) {
        _myList.value = if (isInMyList(slug)) {
            _myList.value.filterNot { it.slug == slug }
        } else {
            _myList.value + SavedMedia(slug = slug, title = title, mediaId = mediaId)
        }
        persist()
    }

    fun clearHistory() {
        _history.value = emptyList()
        persist()
    }

    private fun persist() {
        val data = LibraryData(history = _history.value, myList = _myList.value)
        scope.launch {
            mutex.withLock {
                runCatching {
                    val encoded = json.encodeToString(LibraryData.serializer(), data)
                    val tmp = File(file.parentFile, "${file.name}.tmp")
                    tmp.writeText(encoded)
                    if (!tmp.renameTo(file)) {
                        file.writeText(encoded)
                        tmp.delete()
                    }
                }
            }
        }
    }
}
