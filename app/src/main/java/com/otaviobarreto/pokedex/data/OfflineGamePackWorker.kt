package com.otaviobarreto.pokedex.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class OfflineGamePackWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_GAME = "game"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_LABEL = "label"
    }

    override suspend fun doWork(): Result {
        val gameLabel = inputData.getString(KEY_GAME) ?: return Result.failure()
        OfflineGamePackManager.initialize(applicationContext)
        val game = AppGameCatalog.games.firstOrNull { it.label == gameLabel } ?: return Result.failure()

        return runCatching {
            OfflineGamePackManager.download(game) { p ->
                setProgressAsync(
                    workDataOf(
                        KEY_DONE to p.done,
                        KEY_TOTAL to p.total,
                        KEY_LABEL to p.label
                    )
                )
            }
            Result.success()
        }.getOrElse {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}
