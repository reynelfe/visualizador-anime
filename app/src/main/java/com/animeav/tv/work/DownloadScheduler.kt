package com.animeav.tv.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.animeav.tv.data.DownloadEntry

object DownloadScheduler {

    fun enqueue(context: Context, entry: DownloadEntry, url: String) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_URL to url,
                    DownloadWorker.KEY_FILE to entry.fileName,
                    DownloadWorker.KEY_TITLE to entry.title,
                ),
            )
            .addTag(entry.fileName)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(entry.fileName),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context, fileName: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(fileName))
    }

    private fun workName(fileName: String): String = "animeav-download-$fileName"
}
