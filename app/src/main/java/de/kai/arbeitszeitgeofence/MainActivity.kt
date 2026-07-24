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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kai.arbeitszeitgeofence.data.SettingsEntity
import de.kai.arbeitszeitgeofence.domain.TimeCalculator
import de.kai.arbeitszeitgeofence.domain.TrackingSource
import de.kai.arbeitszeitgeofence.domain.WorkSessionManager
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permission result is handled in later UI hardening.
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
                val scope = rememberCoroutineScope()
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
                        text = "Status: ${
                            if (state.isTracking) {
                                "Arbeitszeit läuft"
                            } else {
                                "Gestoppt"
                            }
                        } / Geofence: ${
                            if (state.insideGeofence) {
                                "Innen"
                            } else {
                                "Außen"
                            }
                        }"
                    )

                    Text(
                        text = "Pause: ${state.accumulatedBreakMinutes} min${
                            if (state.isBreakRunning) {
                                " + laufend"
                            } else {
                                ""
                            }
                        }"
                    )

                    Text(text = message)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val current = dao.getActiveState()
                                        ?: WorkSessionManager.initialState()

                                    val newState = WorkSessionManager.startWork(
                                        state = current,
                                        now = Instant.now(),
                                        insideGeofence = current.insideGeofence
                                    )

                                    dao.upsertActiveState(newState)
                                    message = "Manuell gestartet"
                                }
                            }
                        ) {
                            Text("Start")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val current = dao.getActiveState()
                                        ?: WorkSessionManager.initialState()

                                    val stopResult = WorkSessionManager.stopWork(
                                        state = current,
                                        now = Instant.now(),
                                        localDate = LocalDate.now(),
                                        source = TrackingSource.Manual,
                                        comment = "Manuell gestoppt"
                                    )

                                    val newState = stopResult.first
                                    val entry = stopResult.second

                                    if (entry != null) {
                                        dao.insertEntry(entry)
                                    }

                                    dao.upsertActiveState(newState)
                                    message = "Manuell gestoppt"
                                }
                            }
                        ) {
                            Text("Stop")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val current = dao.getActiveState()
                                        ?: WorkSessionManager.initialState()

                                    val newState = WorkSessionManager.startBreak(
                                        state = current,
                                        now = Instant.now()
                                    )

                                    dao.upsertActiveState(newState)
                                    message = "Pause gestartet"
                                }
                            }
                        ) {
                            Text("Pause Start")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val current = dao.getActiveState()
                                        ?: WorkSessionManager.initialState()

                                    val newState = WorkSessionManager.stopBreak(
                                        state = current,
                                        now = Instant.now()
                                    )

                                    dao.upsertActiveState(newState)
                                    message = "Pause gestoppt"
                                }
                            }
                        ) {
                            Text("Pause Stop")
                        }
                    }

                    Text(
                        text = "Einträge",
                        style = MaterialTheme.typography.titleLarge
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
