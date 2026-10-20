package de.kai.arbeitszeitgeofence.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import de.kai.arbeitszeitgeofence.ArbeitszeitApp
import de.kai.arbeitszeitgeofence.MainActivity
import de.kai.arbeitszeitgeofence.R
import de.kai.arbeitszeitgeofence.data.ActiveStateEntity
import de.kai.arbeitszeitgeofence.domain.TrackingSource
import de.kai.arbeitszeitgeofence.domain.WorkSessionManager
import de.kai.arbeitszeitgeofence.geofence.GeofenceStateStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate

class TrackingForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Alle Aktionen laufen nacheinander, damit sich Read-Modify-Write auf active_state nicht überholt.
    private val actionMutex = Mutex()

    @Volatile
    private var latestStartId = 0

    @Volatile
    private var isForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        val action = intent?.action ?: ACTION_REFRESH

        enterForeground()

        serviceScope.launch {
            actionMutex.withLock {
                try {
                    executeAction(action, startId)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Log.e(TAG, "Action $action failed", exception)
                    finish(startId)
                }

                // Ohne Foreground-Status darf der Service nicht weiterlaufen (sonst ForegroundServiceDidNotStartInTime).
                if (!isForeground) {
                    stopSelf(startId)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Der Typ "location" erfordert eine Standortberechtigung und ist aus dem Hintergrund nur mit
     * ACCESS_BACKGROUND_LOCATION erlaubt. Schlägt das fehl, wird die Aktion trotzdem verarbeitet
     * (die Zustände liegen in der Datenbank), der Service beendet sich danach selbst.
     */
    private fun enterForeground() {
        isForeground = try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(null),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
            true
        } catch (exception: SecurityException) {
            Log.e(TAG, "startForeground rejected (missing location permission?)", exception)
            false
        } catch (exception: ForegroundServiceStartNotAllowedException) {
            Log.e(TAG, "startForeground not allowed from background", exception)
            false
        }
    }

    /** Beendet den Service nur, wenn danach kein neuerer Start-Befehl wartet. */
    private fun finish(startId: Int) {
        if (startId == latestStartId) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf(startId)
    }

    private suspend fun executeAction(action: String, startId: Int) {
        val app = applicationContext as ArbeitszeitApp
        val dao = app.database.workTimeDao()
        val now = Instant.now()
        val today = LocalDate.now()
        val currentState = dao.getActiveState() ?: WorkSessionManager.initialState()

        when (action) {
            ACTION_START -> {
                GeofenceStateStore.setSuppressAutoStart(this, false)
                val newState = WorkSessionManager.startWork(
                    state = currentState,
                    now = now,
                    insideGeofence = currentState.insideGeofence
                )
                dao.upsertActiveState(newState)
                showNotification(newState)
            }

            ACTION_GEOFENCE_ENTER -> {
                GeofenceStateStore.setSuppressAutoStart(this, false)
                val enteredState = currentState.copy(insideGeofence = true)

                // Kurz raus und wieder rein (GPS-Jitter, kurzer Gang): letzten Geofence-Block fortsetzen.
                val resumeCandidate = if (enteredState.isTracking) null else dao.getLastEntry()
                val newState = if (resumeCandidate != null && WorkSessionManager.canResume(resumeCandidate, now)) {
                    val resumedState = WorkSessionManager.resumeFromEntry(enteredState, resumeCandidate)
                    app.database.withTransaction {
                        dao.deleteEntry(resumeCandidate.id)
                        dao.upsertActiveState(resumedState)
                    }
                    resumedState
                } else {
                    val startedState = WorkSessionManager.startWork(
                        state = enteredState,
                        now = now,
                        insideGeofence = true
                    )
                    dao.upsertActiveState(startedState)
                    startedState
                }
                showNotification(newState)
            }

            ACTION_PAUSE_START -> {
                val newState = WorkSessionManager.startBreak(currentState, now)
                dao.upsertActiveState(newState)
                if (newState.isTracking) showNotification(newState) else finish(startId)
            }

            ACTION_PAUSE_STOP -> {
                val newState = WorkSessionManager.stopBreak(currentState, now)
                dao.upsertActiveState(newState)
                if (newState.isTracking) showNotification(newState) else finish(startId)
            }

            ACTION_STOP -> {
                stopCurrentWork(
                    state = currentState,
                    now = now,
                    source = TrackingSource.Manual,
                    comment = "Manuell per Benachrichtigung gestoppt",
                    markDayClosed = false,
                    startId = startId
                )
            }

            ACTION_DAY_CLOSE_MANUAL -> {
                dayCloseCurrentWork(
                    state = currentState,
                    now = now,
                    today = today,
                    comment = "Manueller Tagesabschluss",
                    startId = startId
                )
            }

            ACTION_DAY_CLOSE_AUTO -> {
                dayCloseCurrentWork(
                    state = currentState,
                    now = now,
                    today = today,
                    comment = "Automatischer Tagesabschluss",
                    startId = startId
                )
            }

            ACTION_GEOFENCE_EXIT -> {
                GeofenceStateStore.setSuppressAutoStart(this, false)
                val exitedState = currentState.copy(insideGeofence = false)

                if (exitedState.isTracking) {
                    stopCurrentWork(
                        state = exitedState,
                        now = now,
                        source = TrackingSource.Geofence,
                        comment = "Arbeitsbereich verlassen - Arbeitszeit automatisch angehalten",
                        markDayClosed = false,
                        startId = startId
                    )
                } else {
                    dao.upsertActiveState(exitedState)
                    finish(startId)
                }
            }

            ACTION_REFRESH -> {
                if (currentState.isTracking) {
                    showNotification(currentState)
                } else {
                    finish(startId)
                }
            }
        }
    }

    private suspend fun dayCloseCurrentWork(
        state: ActiveStateEntity,
        now: Instant,
        today: LocalDate,
        comment: String,
        startId: Int
    ) {
        val todayString = today.toString()

        if (state.closedLocalDate == todayString) {
            finish(startId)
            return
        }

        stopCurrentWork(
            state = state,
            now = now,
            source = TrackingSource.DayClose,
            comment = comment,
            markDayClosed = true,
            startId = startId
        )
    }

    private suspend fun stopCurrentWork(
        state: ActiveStateEntity,
        now: Instant,
        source: TrackingSource,
        comment: String,
        markDayClosed: Boolean,
        startId: Int
    ) {
        val app = applicationContext as ArbeitszeitApp
        val dao = app.database.workTimeDao()
        val todayString = LocalDate.now().toString()

        val stopResult = WorkSessionManager.stopWork(
            state = state,
            now = now,
            localDate = LocalDate.now(),
            source = source,
            comment = comment
        )

        val stoppedState = stopResult.first
        val entry = stopResult.second

        val finalState = if (markDayClosed) {
            stoppedState.copy(closedLocalDate = todayString)
        } else {
            stoppedState
        }

        // Eintrag und Zustand gemeinsam schreiben, damit bei einem Abbruch kein halber Stand entsteht.
        app.database.withTransaction {
            if (entry != null) {
                dao.insertEntry(entry)
            }
            dao.upsertActiveState(finalState)
        }

        // Bewusstes Beenden am Arbeitsplatz darf nicht durch den Anwesenheitsabgleich überschrieben werden.
        if (source != TrackingSource.Geofence) {
            GeofenceStateStore.setSuppressAutoStart(this, true)
        }

        finish(startId)
    }

    private fun showNotification(state: ActiveStateEntity) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: ActiveStateEntity?): Notification {
        val isTracking = state?.isTracking == true
        val isBreakRunning = state?.isBreakRunning == true
        val isInsideGeofence = state?.insideGeofence == true

        val contentText = when {
            state == null -> "Arbeitszeit wird aktualisiert"
            !isTracking -> "Arbeitszeit angehalten"
            isBreakRunning -> "Pause laeuft - Arbeitszeit bleibt aktiv"
            isInsideGeofence -> "Arbeitszeit laeuft - Arbeitsplatz erkannt"
            else -> "Arbeitszeit laeuft"
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Arbeitszeit Geofence")
            .setContentText(contentText)
            .setContentIntent(activityPendingIntent())
            .setOngoing(isTracking)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        if (isTracking) {
            notificationBuilder.addAction(0, "Stop", servicePendingIntent(ACTION_STOP, 2001))

            if (isBreakRunning) {
                notificationBuilder.addAction(0, "Pause Stop", servicePendingIntent(ACTION_PAUSE_STOP, 2003))
            } else {
                notificationBuilder.addAction(0, "Pause Start", servicePendingIntent(ACTION_PAUSE_START, 2002))
            }
        }

        return notificationBuilder.build()
    }

    private fun activityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TrackingForegroundService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Arbeitszeiterfassung",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Zeigt eine laufende Arbeitszeiterfassung mit Schnellaktionen an."
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "TrackingService"

        const val ACTION_START = "de.kai.arbeitszeitgeofence.action.START"
        const val ACTION_STOP = "de.kai.arbeitszeitgeofence.action.STOP"
        const val ACTION_PAUSE_START = "de.kai.arbeitszeitgeofence.action.PAUSE_START"
        const val ACTION_PAUSE_STOP = "de.kai.arbeitszeitgeofence.action.PAUSE_STOP"
        const val ACTION_REFRESH = "de.kai.arbeitszeitgeofence.action.REFRESH"
        const val ACTION_GEOFENCE_ENTER = "de.kai.arbeitszeitgeofence.action.GEOFENCE_ENTER"
        const val ACTION_GEOFENCE_EXIT = "de.kai.arbeitszeitgeofence.action.GEOFENCE_EXIT"
        const val ACTION_DAY_CLOSE_MANUAL = "de.kai.arbeitszeitgeofence.action.DAY_CLOSE_MANUAL"
        const val ACTION_DAY_CLOSE_AUTO = "de.kai.arbeitszeitgeofence.action.DAY_CLOSE_AUTO"

        const val NOTIFICATION_ID = 41001
        private const val CHANNEL_ID = "worktime_tracking"

        fun start(context: Context, action: String) {
            val intent = Intent(context, TrackingForegroundService::class.java).setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
