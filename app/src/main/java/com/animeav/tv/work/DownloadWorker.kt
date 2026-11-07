package com.animeav.tv.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.animeav.tv.AnimeAvApp
import com.animeav.tv.data.AnimeAvClient
import okhttp3.Request
import java.io.File

class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Episodio"

        val app = applicationContext as AnimeAvApp
        val store = app.downloadsStore
        val destination = store.fileFor(fileName)
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, "$fileName.part")

        setForeground(createForegroundInfo(title, 0))

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", AnimeAvClient.USER_AGENT)
            .header("Referer", "https://www.mp4upload.com/")
            .header("Accept", "*/*")
            .build()

        return runCatching {
            app.http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    store.markFailed(fileName)
                    return Result.failure()
                }
                val body = response.body ?: run {
                    store.markFailed(fileName)
                    return Result.failure()
                }
                val total = body.contentLength()
                var bytes = 0L
                var lastNotification = 0L
                var lastPersist = 0L

                body.byteStream().use { input ->
                    temporary.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (!isStopped) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            bytes += read

                            val now = System.currentTimeMillis()
                            if (now - lastNotification >= 500L) {
                                val percent = if (total > 0L) {
                                    (bytes * 100 / total).toInt().coerceIn(0, 100)
                                } else {
                                    0
                                }
                                store.updateProgress(fileName, bytes, total)
                                if (now - lastPersist >= 5000L) {
                                    store.persistNow()
                                    lastPersist = now
                                }
                                setProgress(workDataOf(KEY_PROGRESS to percent))
                                setForeground(createForegroundInfo(title, percent))
                                lastNotification = now
                            }
                        }
                    }
                }

                if (!isStopped && temporary.length() > 0L) {
                    if (destination.exists()) destination.delete()
                    if (temporary.renameTo(destination)) {
                        store.markCompleted(fileName)
                        Result.success()
                    } else {
                        store.markFailed(fileName)
                        Result.failure()
                    }
                } else {
                    temporary.delete()
                    store.markFailed(fileName)
                    Result.failure()
                }
            }
        }.getOrElse {
            temporary.delete()
            store.markFailed(fileName)
            Result.failure()
        }
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Descargando $title")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Descargas de episodios",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Progreso de los episodios guardados para ver sin conexión"
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_FILE = "file"
        const val KEY_TITLE = "title"
        const val KEY_PROGRESS = "progress"

        private const val CHANNEL_ID = "animeav_downloads"
        private const val NOTIFICATION_ID = 4101
    }
}
