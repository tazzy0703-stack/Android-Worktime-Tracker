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
import com.google.android.gms.location.LocationServices
import de.kai.arbeitszeitgeofence.data.SettingsEntity
import de.kai.arbeitszeitgeofence.domain.TimeCalculator
import de.kai.arbeitszeitgeofence.domain.WorkSessionManager
import de.kai.arbeitszeitgeofence.geofence.GeofenceManager
import de.kai.arbeitszeitgeofence.service.TrackingForegroundService
import kotlinx.coroutines.launch
import java.time.Instant

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Detailed permission status screen follows in a later version.
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
                                onValueChange = { radiusDraft = it },
                                valueRange = 50f..500f
                            )

                            Text(
                                text = "Koordinate: " +
                                    if (effectiveSettings.geofenceLatitude != null && effectiveSettings.geofenceLongitude != null) {
                                        "${effectiveSettings.geofenceLatitude}, ${effectiveSettings.geofenceLongitude}"
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
                                            currentSettings = effectiveSettings,
                                            radiusMeters = radiusDraft,
                                            onMessage = { message = it }
                                        )
                                    }
                                ) {
                                    Text("Aktuelle Position speichern")
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val latitude = effectiveSettings.geofenceLatitude
                                        val longitude = effectiveSettings.geofenceLongitude

                                        if (latitude == null || longitude == null) {
                                            message = "Keine Arbeitsplatz-Koordinate gesetzt"
                                        } else {
                                            try {
                                                geofenceManager.registerWorkplaceGeofence(
                                                    latitude = latitude,
                                                    longitude = longitude,
                                                    radiusMeters = radiusDraft
                                                )
                                                message = "Geofence registriert"
                                            } catch (exception: SecurityException) {
                                                message = "Standortberechtigung fehlt: ${exception.message}"
                                            }
                                        }
                                    }
                                ) {
                                    Text("Geofence registrieren")
                                }

                                Button(
                                    onClick = {
                                        geofenceManager.unregisterWorkplaceGeofence()
                                        message = "Geofence entfernt"
                                    }
                                ) {
                                    Text("Geofence entfernen")
                                }
                            }
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

    @SuppressLint("MissingPermission")
    private fun saveCurrentLocationAsWorkplace(
        fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
        dao: de.kai.arbeitszeitgeofence.data.WorkTimeDao,
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

                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
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



