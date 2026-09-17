package com.syncmyshit.app.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.syncmyshit.app.SyncApplication
import java.util.concurrent.TimeUnit

class PeriodicSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SyncApplication ?: return Result.failure()
        return try {
            val result = app.syncRepository.syncAllProfiles()
            if (result.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

object SyncWorkManagerHelper {

    private const val WORK_NAME = "syncmyshit_periodic_sync"

    fun schedulePeriodicSync(context: Context, intervalMinutes: Long = 30, wifiOnly: Boolean = false) {
        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<PeriodicSyncWorker>(
            intervalMinutes.coerceAtLeast(15),
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
