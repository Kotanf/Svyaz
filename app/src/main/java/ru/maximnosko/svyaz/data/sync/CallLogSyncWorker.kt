package ru.maximnosko.svyaz.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maximnosko.svyaz.data.SystemDataProvider

class CallLogSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val systemDataProvider = SystemDataProvider(applicationContext)
            val callLogs = systemDataProvider.fetchCallLogs()
            Log.d("CallLogSyncWorker", "Synced ${callLogs.size} call log entries")
            Result.success()
        } catch (e: Exception) {
            Log.e("CallLogSyncWorker", "Failed to sync call logs", e)
            Result.retry()
        }
    }
}
