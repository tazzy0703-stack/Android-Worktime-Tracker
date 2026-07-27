package de.kai.arbeitszeitgeofence.worker

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.kai.arbeitszeitgeofence.ArbeitszeitApp
import de.kai.arbeitszeitgeofence.domain.TrackingSource
import de.kai.arbeitszeitgeofence.domain.WorkSessionManager
import de.kai.arbeitszeitgeofence.service.TrackingForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class DayCloseWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ArbeitszeitApp
        val dao = app.database.workTimeDao()
        val now = Instant.now()
        val today = LocalDate.now()
        val todayString = today.toString()
        val state = dao.getActiveState() ?: WorkSessionManager.initialState()

        if (state.closedLocalDate != todayString) {
            if (state.isTracking) {
                val stopResult = WorkSessionManager.stopWork(
                    state = state,
                    now = now,
                    localDate = today,
                    source = TrackingSource.DayClose,
                    comment = "Automatischer Tagesabschluss"
                )

                val entry = stopResult.second
                if (entry != null) {
                    dao.insertEntry(entry)
                }

                dao.upsertActiveState(
                    stopResult.first.copy(closedLocalDate = todayString)
                )

                val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
                notificationManager.cancel(TrackingForegroundService.NOTIFICATION_ID)
                applicationContext.stopService(Intent(applicationContext, TrackingForegroundService::class.java))
            } else {
                dao.upsertActiveState(state.copy(closedLocalDate = todayString))
            }
        }

        scheduleNext(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "daily-day-close"

        fun scheduleNext(context: Context) {
            val app = context.applicationContext as ArbeitszeitApp
            val dao = app.database.workTimeDao()

            CoroutineScope(Dispatchers.IO).launch {
                val settings = dao.getSettings()
                val closeTime = runCatching {
                    LocalTime.parse(settings?.autoDayCloseTime ?: "23:59")
                }.getOrDefault(LocalTime.of(23, 59))

                val now = LocalDateTime.now()
                var nextRun = LocalDate.now().atTime(closeTime)
                if (!nextRun.isAfter(now)) {
                    nextRun = nextRun.plusDays(1)
                }

                val delayMillis = Duration.between(now, nextRun).toMillis().coerceAtLeast(0)
                val request = OneTimeWorkRequestBuilder<DayCloseWorker>()
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }
}
