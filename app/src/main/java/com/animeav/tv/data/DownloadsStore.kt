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
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class DownloadEntry(
    val fileName: String = "",
    val slug: String = "",
    val title: String = "",
    val episodeNumber: Int = 1,
    val mediaId: Int = 0,
    val status: String = STATUS_QUEUED,
    val bytes: Long = 0L,
    val totalBytes: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val progress: Float
        get() = if (totalBytes > 0L) (bytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val isCompleted: Boolean get() = status == STATUS_COMPLETED

    companion object {
        const val STATUS_QUEUED = "queued"
        const val STATUS_DOWNLOADING = "downloading"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"
    }
}

@Serializable
private data class DownloadsData(
    val entries: List<DownloadEntry> = emptyList(),
)

class DownloadsStore(
    context: Context,
    private val json: Json,
) {
    private val directory = File(context.filesDir, "downloads")
    private val file = File(context.filesDir, "downloads.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _entries = MutableStateFlow<List<DownloadEntry>>(emptyList())
    val entries: StateFlow<List<DownloadEntry>> = _entries.asStateFlow()

    init {
        directory.mkdirs()
        scope.launch {
            mutex.withLock {
                runCatching {
                    if (file.exists()) {
                        val data = json.decodeFromString<DownloadsData>(file.readText())
                        _entries.value = data.entries.sortedByDescending { it.updatedAt }
                    }
                }
            }
        }
    }

    fun fileFor(fileName: String): File = File(directory, fileName)

    fun find(fileName: String): DownloadEntry? =
        _entries.value.firstOrNull { it.fileName == fileName }

    fun upsert(entry: DownloadEntry) {
        _entries.value = (
            listOf(entry.copy(updatedAt = System.currentTimeMillis())) +
                _entries.value.filterNot { it.fileName == entry.fileName }
            ).sortedByDescending { it.updatedAt }
        persist()
    }

    fun updateProgress(fileName: String, bytes: Long, totalBytes: Long) {
        update(fileName, persist = false) {
            it.copy(
                status = DownloadEntry.STATUS_DOWNLOADING,
                bytes = bytes,
                totalBytes = if (totalBytes > 0) totalBytes else it.totalBytes,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    fun persistNow() {
        persist()
    }

    fun markCompleted(fileName: String) {
        update(fileName) {
            it.copy(
                status = DownloadEntry.STATUS_COMPLETED,
                bytes = if (it.totalBytes > 0) it.totalBytes else it.bytes,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    fun markFailed(fileName: String) {
        update(fileName) {
            it.copy(status = DownloadEntry.STATUS_FAILED, updatedAt = System.currentTimeMillis())
        }
    }

    fun remove(fileName: String) {
        _entries.value = _entries.value.filterNot { it.fileName == fileName }
        fileFor(fileName).delete()
        persist()
    }

    private fun update(
        fileName: String,
        persist: Boolean = true,
        transform: (DownloadEntry) -> DownloadEntry,
    ) {
        _entries.value = _entries.value.map { entry ->
            if (entry.fileName == fileName) transform(entry) else entry
        }
        if (persist) persist()
    }

    private fun persist() {
        val data = DownloadsData(entries = _entries.value)
        scope.launch {
            mutex.withLock {
                runCatching {
                    val encoded = json.encodeToString(DownloadsData.serializer(), data)
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
