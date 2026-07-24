#requires -Version 7.0
$ErrorActionPreference = 'Stop'

$ProjectRoot = Get-Location
$ExpectedAppGradle = Join-Path $ProjectRoot 'app\build.gradle.kts'
if (-not (Test-Path $ExpectedAppGradle)) {
    throw "Bitte dieses Skript im Root des Repositories ausfuehren: C:\timetracking_android_project\Android-Worktime-Tracker"
}

function Write-Utf8File {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$Content
    )
    $Directory = Split-Path -Parent $Path
    if (-not (Test-Path $Directory)) {
        New-Item -Path $Directory -ItemType Directory -Force | Out-Null
    }
    Set-Content -Path $Path -Value $Content -Encoding UTF8
}

# 1) Build Gradle: add androidx.core dependency if missing.
$BuildGradlePath = Join-Path $ProjectRoot 'app\build.gradle.kts'
$BuildGradle = Get-Content $BuildGradlePath -Raw
if ($BuildGradle -notmatch 'androidx\.core:core-ktx') {
    $ActivityDependencyPattern = 'implementation\("androidx\.activity:activity-compose:1\.9\.3"\)'
    $ActivityDependencyReplacement = "implementation(""androidx.activity:activity-compose:1.9.3"")`n    implementation(""androidx.core:core-ktx:1.13.1"")"

    $BuildGradle = $BuildGradle -replace $ActivityDependencyPattern, $ActivityDependencyReplacement

    Set-Content -Path $BuildGradlePath -Value $BuildGradle -Encoding UTF8
}

# 2) Manifest: foreground service permissions and service declaration.
$ManifestPath = Join-Path $ProjectRoot 'app\src\main\AndroidManifest.xml'
$Manifest = @'
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

    <application
        android:name=".ArbeitszeitApp"
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".geofence.GeofenceBroadcastReceiver"
            android:exported="false" />

        <service
            android:name=".service.TrackingForegroundService"
            android:exported="false"
            android:foregroundServiceType="location" />
    </application>
</manifest>
'@
Write-Utf8File -Path $ManifestPath -Content $Manifest

# 3) Notification icon.
$IconPath = Join-Path $ProjectRoot 'app\src\main\res\drawable\ic_stat_timer.xml'
$Icon = @'
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M12,1.75A10.25,10.25 0,1 0,12 22.25A10.25,10.25 0,0 0,12 1.75ZM12,4.25A7.75,7.75 0,1 1,12 19.75A7.75,7.75 0,0 1,12 4.25ZM12.75,7H11.25V12.35L15.72,15.03L16.5,13.75L12.75,11.52V7Z" />
</vector>
'@
Write-Utf8File -Path $IconPath -Content $Icon

# 4) Foreground service.
$ServicePath = Join-Path $ProjectRoot 'app\src\main\java\de\kai\arbeitszeitgeofence\service\TrackingForegroundService.kt'
$Service = @'
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
'@
Write-Utf8File -Path $ServicePath -Content $Service

# 5) Replace MainActivity with service-driven controls.
$MainActivityPath = Join-Path $ProjectRoot 'app\src\main\java\de\kai\arbeitszeitgeofence\MainActivity.kt'
$MainActivity = @'
package de.kai.arbeitszeitgeofence

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kai.arbeitszeitgeofence.data.SettingsEntity
import de.kai.arbeitszeitgeofence.domain.TimeCalculator
import de.kai.arbeitszeitgeofence.domain.WorkSessionManager
import de.kai.arbeitszeitgeofence.service.TrackingForegroundService
import java.time.Instant

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permission result is intentionally displayed in a later status screen.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        val app = application as ArbeitszeitApp
        val dao = app.database.workTimeDao()

        setContent {
            MaterialTheme {
                val entries by dao.observeEntries().collectAsState(initial = emptyList())
                val activeState by dao.observeActiveState().collectAsState(initial = null)
                val settings by dao.observeSettings().collectAsState(initial = null)
                var message by remember { mutableStateOf("Bereit") }

                LaunchedEffect(Unit) {
                    if (dao.getActiveState() == null) {
                        dao.upsertActiveState(WorkSessionManager.initialState())
                    }

                    if (dao.getSettings() == null) {
                        dao.upsertSettings(SettingsEntity())
                    }
                }

                val state = activeState ?: WorkSessionManager.initialState()
                val effectiveSettings = settings ?: SettingsEntity()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Arbeitszeit Geofence",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text = "Status: ${if (state.isTracking) "Arbeitszeit laeuft" else "Gestoppt"} / " +
                            "Geofence: ${if (state.insideGeofence) "Innen" else "Aussen"}"
                    )

                    Text(
                        text = "Pause: ${state.accumulatedBreakMinutes} min" +
                            if (state.isBreakRunning) " + laufend" else ""
                    )

                    Text(text = message)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                TrackingForegroundService.start(
                                    this@MainActivity,
                                    TrackingForegroundService.ACTION_START
                                )
                                message = "Manuell gestartet"
                            }
                        ) {
                            Text("Start")
                        }

                        Button(
                            onClick = {
                                TrackingForegroundService.start(
                                    this@MainActivity,
                                    TrackingForegroundService.ACTION_STOP
                                )
                                message = "Manuell gestoppt"
                            }
                        ) {
                            Text("Stop")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                TrackingForegroundService.start(
                                    this@MainActivity,
                                    TrackingForegroundService.ACTION_PAUSE_START
                                )
                                message = "Pause gestartet"
                            }
                        ) {
                            Text("Pause Start")
                        }

                        Button(
                            onClick = {
                                TrackingForegroundService.start(
                                    this@MainActivity,
                                    TrackingForegroundService.ACTION_PAUSE_STOP
                                )
                                message = "Pause gestoppt"
                            }
                        ) {
                            Text("Pause Stop")
                        }
                    }

                    Text(
                        text = "Eintraege",
                        style = MaterialTheme.typography.titleLarge
                    )

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(entries) { entry ->
                            val calculation = TimeCalculator.calculate(
                                start = Instant.ofEpochMilli(entry.startEpochMillis),
                                end = Instant.ofEpochMilli(entry.endEpochMillis),
                                breakMinutes = entry.breakMinutes,
                                targetMinutes = effectiveSettings.targetMinutesPerDay
                            )

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${entry.localDate} | ${entry.source}")
                                    Text(
                                        "Netto: ${calculation.netMinutes} min | " +
                                            "Saldo: ${calculation.balanceMinutes} min | " +
                                            "Pause: ${entry.breakMinutes} min"
                                    )

                                    if (entry.comment.isNotBlank()) {
                                        Text(entry.comment)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
'@
Write-Utf8File -Path $MainActivityPath -Content $MainActivity

# 6) Version bump.
$BuildGradle = Get-Content $BuildGradlePath -Raw
$BuildGradle = $BuildGradle -replace 'versionCode = \d+', 'versionCode = 3'
$BuildGradle = $BuildGradle -replace 'versionName = "[^"]+"', 'versionName = "0.2.0"'
Set-Content -Path $BuildGradlePath -Value $BuildGradle -Encoding UTF8

Write-Host "v0.2.0 Foreground-Notification Patch wurde angewendet."
Write-Host "Naechste Schritte: git status, git add ., git commit, git push."

