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

$MainActivityPath = Join-Path $ProjectRoot 'app\src\main\java\de\kai\arbeitszeitgeofence\MainActivity.kt'
$MainActivity = @'
package de.kai.arbeitszeitgeofence

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import de.kai.arbeitszeitgeofence.data.SettingsEntity
import de.kai.arbeitszeitgeofence.data.WorkTimeDao
import de.kai.arbeitszeitgeofence.domain.TimeCalculator
import de.kai.arbeitszeitgeofence.domain.WorkSessionManager
import de.kai.arbeitszeitgeofence.geofence.GeofenceManager
import de.kai.arbeitszeitgeofence.service.TrackingForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

private enum class AppScreen {
    Times,
    Settings
}

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permission status screen follows in a later hardening step.
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
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val geofenceManager = GeofenceManager(this)

        setContent {
            MaterialTheme {
                val entries by dao.observeEntries().collectAsState(initial = emptyList())
                val activeState by dao.observeActiveState().collectAsState(initial = null)
                val settings by dao.observeSettings().collectAsState(initial = null)
                var message by remember { mutableStateOf("Bereit") }
                var selectedScreen by remember { mutableStateOf(AppScreen.Times) }
                var radiusDraft by remember { mutableFloatStateOf(120f) }

                LaunchedEffect(Unit) {
                    if (dao.getActiveState() == null) {
                        dao.upsertActiveState(WorkSessionManager.initialState())
                    }

                    if (dao.getSettings() == null) {
                        dao.upsertSettings(SettingsEntity())
                    }
                }

                LaunchedEffect(settings?.geofenceRadiusMeters) {
                    radiusDraft = settings?.geofenceRadiusMeters ?: 120f
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { selectedScreen = AppScreen.Times }
                        ) {
                            Text("Zeiten")
                        }

                        Button(
                            onClick = { selectedScreen = AppScreen.Settings }
                        ) {
                            Text("Einstellungen")
                        }
                    }

                    Text(
                        text = "Status: ${if (state.isTracking) "Arbeitszeit laeuft" else "Gestoppt"} / " +
                            "Geofence: ${if (state.insideGeofence) "Innen" else "Aussen"}"
                    )

                    Text(
                        text = "Pause: ${state.accumulatedBreakMinutes} min" +
                            if (state.isBreakRunning) " + laufend" else ""
                    )

                    Text(text = message)

                    when (selectedScreen) {
                        AppScreen.Times -> {
                            TimesScreen(
                                dao = dao,
                                entries = entries,
                                targetMinutesPerDay = effectiveSettings.targetMinutesPerDay,
                                onMessage = { message = it }
                            )
                        }

                        AppScreen.Settings -> {
                            SettingsScreen(
                                dao = dao,
                                currentSettings = effectiveSettings,
                                radiusDraft = radiusDraft,
                                onRadiusChange = { radiusDraft = it },
                                fusedLocationClient = fusedLocationClient,
                                geofenceManager = geofenceManager,
                                onMessage = { message = it }
                            )
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TimesScreen(
        dao: WorkTimeDao,
        entries: List<de.kai.arbeitszeitgeofence.data.WorkTimeEntryEntity>,
        targetMinutesPerDay: Int,
        onMessage: (String) -> Unit
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    TrackingForegroundService.start(
                        this@MainActivity,
                        TrackingForegroundService.ACTION_START
                    )
                    onMessage("Manuell gestartet")
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
                    onMessage("Manuell gestoppt")
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
                    onMessage("Pause gestartet")
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
                    onMessage("Pause gestoppt")
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
                    targetMinutes = targetMinutesPerDay
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

    @androidx.compose.runtime.Composable
    private fun SettingsScreen(
        dao: WorkTimeDao,
        currentSettings: SettingsEntity,
        radiusDraft: Float,
        onRadiusChange: (Float) -> Unit,
        fusedLocationClient: FusedLocationProviderClient,
        geofenceManager: GeofenceManager,
        onMessage: (String) -> Unit
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Arbeitsplatz-Geofence",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text("Radius: ${radiusDraft.toInt()} m")
                        Slider(
                            value = radiusDraft,
                            onValueChange = onRadiusChange,
                            valueRange = 50f..500f
                        )

                        Text(
                            text = "Koordinate: " +
                                if (currentSettings.geofenceLatitude != null && currentSettings.geofenceLongitude != null) {
                                    "${currentSettings.geofenceLatitude}, ${currentSettings.geofenceLongitude}"
                                } else {
                                    "nicht gesetzt"
                                }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    saveCurrentLocationAsWorkplace(
                                        fusedLocationClient = fusedLocationClient,
                                        dao = dao,
                                        currentSettings = currentSettings,
                                        radiusMeters = radiusDraft,
                                        onMessage = onMessage
                                    )
                                }
                            ) {
                                Text("Aktuelle Position speichern")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val latitude = currentSettings.geofenceLatitude
                                    val longitude = currentSettings.geofenceLongitude

                                    if (latitude == null || longitude == null) {
                                        onMessage("Keine Arbeitsplatz-Koordinate gesetzt")
                                    } else {
                                        try {
                                            geofenceManager.registerWorkplaceGeofence(
                                                latitude = latitude,
                                                longitude = longitude,
                                                radiusMeters = radiusDraft
                                            )
                                            onMessage("Geofence registriert")
                                        } catch (exception: SecurityException) {
                                            onMessage("Standortberechtigung fehlt: ${exception.message}")
                                        }
                                    }
                                }
                            ) {
                                Text("Geofence registrieren")
                            }

                            Button(
                                onClick = {
                                    geofenceManager.unregisterWorkplaceGeofence()
                                    onMessage("Geofence entfernt")
                                }
                            ) {
                                Text("Geofence entfernen")
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Arbeitszeitparameter",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text("Sollzeit pro Tag: ${currentSettings.targetMinutesPerDay} Minuten")
                        Text("Standardpause: ${currentSettings.defaultBreakMinutes} Minuten")
                        Text("Tagesabschluss: ${currentSettings.autoDayCloseTime}")
                        Text("Hinweis: Bearbeitbare Parameter folgen in einem separaten Patch.")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveCurrentLocationAsWorkplace(
        fusedLocationClient: FusedLocationProviderClient,
        dao: WorkTimeDao,
        currentSettings: SettingsEntity,
        radiusMeters: Float,
        onMessage: (String) -> Unit
    ) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    onMessage("Keine letzte Position verfuegbar. Standort kurz aktivieren und erneut versuchen.")
                    return@addOnSuccessListener
                }

                CoroutineScope(Dispatchers.IO).launch {
                    dao.upsertSettings(
                        currentSettings.copy(
                            geofenceLatitude = location.latitude,
                            geofenceLongitude = location.longitude,
                            geofenceRadiusMeters = radiusMeters
                        )
                    )

                    runOnUiThread {
                        onMessage("Arbeitsplatz-Koordinate gespeichert")
                    }
                }
            }
            .addOnFailureListener { exception ->
                onMessage("Position konnte nicht gelesen werden: ${exception.message}")
            }
    }
}
'@
Write-Utf8File -Path $MainActivityPath -Content $MainActivity

$BuildGradlePath = Join-Path $ProjectRoot 'app\build.gradle.kts'
$BuildGradle = Get-Content $BuildGradlePath -Raw
$BuildGradle = $BuildGradle -replace 'versionCode = \d+', 'versionCode = 5'
$BuildGradle = $BuildGradle -replace 'versionName = "[^"]+"', 'versionName = "0.3.1"'
Set-Content -Path $BuildGradlePath -Value $BuildGradle -Encoding UTF8

Write-Host "v0.3.1 Settings-Screen Patch wurde angewendet."
Write-Host "Naechste Schritte: git status, git add ., git commit, git push."
