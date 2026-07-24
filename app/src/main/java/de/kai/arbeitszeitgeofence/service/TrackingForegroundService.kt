package de.kai.arbeitszeitgeofence.service

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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import de.kai.arbeitszeitgeofence.ArbeitszeitApp
import de.kai.arbeitszeitgeofence.MainActivity
import de.kai.arbeitszeitgeofence.R
import de.kai.arbeitszeitgeofence.data.ActiveStateEntity
import de.kai.arbeitszeitgeofence.domain.TrackingSource
import de.kai.arbeitszeitgeofence.domain.WorkSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

class TrackingForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_REFRESH

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(null),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        serviceScope.launch {
            executeAction(action)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun executeAction(action: String) {
        val app = applicationContext as ArbeitszeitApp
        val dao = app.database.workTimeDao()
        val now = Instant.now()
        val currentState = dao.getActiveState() ?: WorkSessionManager.initialState()

        when (action) {
            ACTION_START -> {
                val newState = WorkSessionManager.startWork(
                    state = currentState,
                    now = now,
                    insideGeofence = currentState.insideGeofence
                )
                dao.upsertActiveState(newState)
                showNotification(newState)
            }

            ACTION_PAUSE_START -> {
                val newState = WorkSessionManager.startBreak(
                    state = currentState,
                    now = now
                )
                dao.upsertActiveState(newState)
                showNotification(newState)
            }

            ACTION_PAUSE_STOP -> {
                val newState = WorkSessionManager.stopBreak(
                    state = currentState,
                    now = now
                )
                dao.upsertActiveState(newState)
                showNotification(newState)
            }

            ACTION_STOP -> {
                val stopResult = WorkSessionManager.stopWork(
                    state = currentState,
                    now = now,
                    localDate = LocalDate.now(),
                    source = TrackingSource.Manual,
                    comment = "Manuell per Benachrichtigung gestoppt"
                )

                val newState = stopResult.first
                val entry = stopResult.second

                if (entry != null) {
                    dao.insertEntry(entry)
                }

                dao.upsertActiveState(newState)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_REFRESH -> {
                if (currentState.isTracking) {
                    showNotification(currentState)
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun showNotification(state: ActiveStateEntity) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: ActiveStateEntity?): Notification {
        val isTracking = state?.isTracking == true
        val isBreakRunning = state?.isBreakRunning == true

        val contentText = when {
            !isTracking -> "Arbeitszeit wird gestartet"
            isBreakRunning -> "Pause laeuft - Arbeitszeit bleibt aktiv"
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
            notificationBuilder.addAction(
                0,
                "Stop",
                servicePendingIntent(ACTION_STOP, 2001)
            )

            if (isBreakRunning) {
                notificationBuilder.addAction(
                    0,
                    "Pause Stop",
                    servicePendingIntent(ACTION_PAUSE_STOP, 2003)
                )
            } else {
                notificationBuilder.addAction(
                    0,
                    "Pause Start",
                    servicePendingIntent(ACTION_PAUSE_START, 2002)
                )
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

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
        const val ACTION_START = "de.kai.arbeitszeitgeofence.action.START"
        const val ACTION_STOP = "de.kai.arbeitszeitgeofence.action.STOP"
        const val ACTION_PAUSE_START = "de.kai.arbeitszeitgeofence.action.PAUSE_START"
        const val ACTION_PAUSE_STOP = "de.kai.arbeitszeitgeofence.action.PAUSE_STOP"
        const val ACTION_REFRESH = "de.kai.arbeitszeitgeofence.action.REFRESH"

        private const val CHANNEL_ID = "worktime_tracking"
        private const val NOTIFICATION_ID = 41001

        fun start(context: Context, action: String) {
            val intent = Intent(context, TrackingForegroundService::class.java).setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
