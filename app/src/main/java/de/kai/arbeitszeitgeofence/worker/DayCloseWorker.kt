package de.kai.arbeitszeitgeofence.worker

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.kai.arbeitszeitgeofence.ArbeitszeitApp
import de.kai.arbeitszeitgeofence.domain.TrackingSource
import de.kai.arbeitszeitgeofence.domain.WorkSessionManager
import de.kai.arbeitszeitgeofence.geofence.GeofenceStateStore
import de.kai.arbeitszeitgeofence.service.TrackingForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class DayCloseWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ArbeitszeitApp
        val dao = app.database.workTimeDao()

        // WorkManager garantiert nur "frühestens": Der Worker kann Minuten oder Stunden nach dem
        // geplanten Zeitpunkt laufen (Doze). Datum und Endzeit richten sich deshalb nach dem
        // geplanten Abschlusszeitpunkt und nicht nach der Ausführungszeit.
        val scheduledAt = Instant.ofEpochMilli(inputData.getLong(KEY_SCHEDULED_AT, System.currentTimeMillis()))
        val closeDate: LocalDate = scheduledAt.atZone(ZoneId.systemDefault()).toLocalDate()
        val closeDateString = closeDate.toString()
        val state = dao.getActiveState() ?: WorkSessionManager.initialState()

        if (state.closedLocalDate != closeDateString) {
            val trackingSince = state.activeStartEpochMillis

            if (state.isTracking && trackingSince != null) {
                // Eine Sitzung, die erst nach dem Abschlusszeitpunkt begonnen hat, gehört zum Folgetag.
                if (trackingSince <= scheduledAt.toEpochMilli()) {
                    val (stoppedState, entry) = WorkSessionManager.stopWork(
                        state = state,
                        now = scheduledAt,
                        localDate = closeDate,
                        source = TrackingSource.DayClose,
                        comment = "Automatischer Tagesabschluss"
                    )

                    app.database.withTransaction {
                        if (entry != null) {
                            dao.insertEntry(entry)
                        }
                        dao.upsertActiveState(stoppedState.copy(closedLocalDate = closeDateString))
                    }

                    GeofenceStateStore.setSuppressAutoStart(applicationContext, true)

                    val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
                    notificationManager.cancel(TrackingForegroundService.NOTIFICATION_ID)
                    applicationContext.stopService(Intent(applicationContext, TrackingForegroundService::class.java))
                }
            } else {
                dao.upsertActiveState(state.copy(closedLocalDate = closeDateString))
            }
        }

        scheduleNextNow(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "daily-day-close"
        private const val KEY_SCHEDULED_AT = "scheduledAtEpochMillis"

        /** Für Aufrufer ohne Coroutine (UI-Klicks). */
        fun scheduleNext(context: Context) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                scheduleNextNow(appContext)
            }
        }

        /** Plant den nächsten Abschluss und kehrt erst nach dem Einreihen zurück (auch aus dem Worker aufrufbar). */
        suspend fun scheduleNextNow(context: Context) {
            val appContext = context.applicationContext
            val dao = (appContext as ArbeitszeitApp).database.workTimeDao()
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
            val scheduledAtMillis = nextRun.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val request = OneTimeWorkRequestBuilder<DayCloseWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_SCHEDULED_AT to scheduledAtMillis))
                .build()

            WorkManager.getInstance(appContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
