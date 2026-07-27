package de.kai.arbeitszeitgeofence

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
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

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

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
                var radiusText by remember { mutableStateOf("120") }

                LaunchedEffect(Unit) {
                    if (dao.getActiveState() == null) {
                        dao.upsertActiveState(WorkSessionManager.initialState())
                    }

                    if (dao.getSettings() == null) {
                        dao.upsertSettings(SettingsEntity())
                    }
                }

                LaunchedEffect(settings?.geofenceRadiusMeters) {
                    radiusText = (settings?.geofenceRadiusMeters ?: 120f).toInt().toString()
                }

                val state = activeState ?: WorkSessionManager.initialState()
                val effectiveSettings = settings ?: SettingsEntity()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Arbeitszeit Geofence",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { selectedScreen = AppScreen.Times }) {
                            Text("Zeiten")
                        }

                        Button(onClick = { selectedScreen = AppScreen.Settings }) {
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
                        AppScreen.Times -> TimesScreen(
                            entries = entries,
                            targetMinutesPerDay = effectiveSettings.targetMinutesPerDay,
                            onMessage = { message = it }
                        )

                        AppScreen.Settings -> SettingsScreen(
                            dao = dao,
                            currentSettings = effectiveSettings,
                            radiusText = radiusText,
                            onRadiusTextChange = { radiusText = it },
                            fusedLocationClient = fusedLocationClient,
                            geofenceManager = geofenceManager,
                            onMessage = { message = it }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TimesScreen(
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

    @Composable
    private fun SettingsScreen(
        dao: WorkTimeDao,
        currentSettings: SettingsEntity,
        radiusText: String,
        onRadiusTextChange: (String) -> Unit,
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

                        GeofenceMapPreview(
                            dao = dao,
                            currentSettings = currentSettings,
                            radiusMeters = currentSettings.geofenceRadiusMeters,
                            onMessage = onMessage
                        )

                        OutlinedTextField(
                            value = radiusText,
                            onValueChange = { value ->
                                onRadiusTextChange(value.filter { it.isDigit() }.take(4))
                            },
                            label = { Text("Radius in Metern") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    saveRadius(
                                        dao = dao,
                                        currentSettings = currentSettings,
                                        radiusText = radiusText,
                                        onMessage = onMessage
                                    )
                                }
                            ) {
                                Text("Radius speichern")
                            }

                            Button(
                                onClick = {
                                    saveCurrentLocationAsWorkplace(
                                        fusedLocationClient = fusedLocationClient,
                                        dao = dao,
                                        currentSettings = currentSettings,
                                        radiusText = radiusText,
                                        onMessage = onMessage
                                    )
                                }
                            ) {
                                Text("Aktuelle Position speichern")
                            }
                        }

                        Text(
                            text = "Koordinate: " +
                                if (currentSettings.geofenceLatitude != null && currentSettings.geofenceLongitude != null) {
                                    "${currentSettings.geofenceLatitude}, ${currentSettings.geofenceLongitude}"
                                } else {
                                    "nicht gesetzt"
                                }
                        )

                        Text("Hinweis: Marker per langem Druck auf die Karte setzen.")

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
                                                radiusMeters = currentSettings.geofenceRadiusMeters
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

                        Text("© OpenStreetMap contributors")
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

    @Composable
    private fun GeofenceMapPreview(
        dao: WorkTimeDao,
        currentSettings: SettingsEntity,
        radiusMeters: Float,
        onMessage: (String) -> Unit
    ) {
        val latitude = currentSettings.geofenceLatitude
        val longitude = currentSettings.geofenceLongitude

        if (latitude == null || longitude == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Noch keine Karte verfuegbar.")
                    Text("Bitte zuerst aktuelle Position speichern oder spaeter per Karte setzen.")
                }
            }
            return
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            factory = { context ->
                Configuration.getInstance().userAgentValue = context.packageName

                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)
                    minZoomLevel = 3.0
                    maxZoomLevel = 20.0
                }
            },
            update = { mapView: MapView ->
                updateGeofenceMap(
                    mapView = mapView,
                    dao = dao,
                    currentSettings = currentSettings,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radiusMeters,
                    onMessage = onMessage
                )
            }
        )
    }

    private fun updateGeofenceMap(
        mapView: MapView,
        dao: WorkTimeDao,
        currentSettings: SettingsEntity,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        onMessage: (String) -> Unit
    ) {
        val center = GeoPoint(latitude, longitude)

        mapView.controller.setZoom(18.0)
        mapView.controller.setCenter(center)
        mapView.overlays.clear()

        val circle = Polygon(mapView).apply {
            points = Polygon.pointsAsCircle(center, radiusMeters.toDouble())
            fillColor = 0x3333B5E5
            strokeColor = 0xFF0288D1.toInt()
            strokeWidth = 4f
        }

        val marker = Marker(mapView).apply {
            position = center
            title = "Arbeitsplatz"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        val longPressOverlay = LongPressOverlay { geoPoint ->
            saveMarkerFromMap(
                dao = dao,
                currentSettings = currentSettings,
                geoPoint = geoPoint,
                onMessage = onMessage
            )
        }

        mapView.overlays.add(circle)
        mapView.overlays.add(marker)
        mapView.overlays.add(longPressOverlay)
        mapView.invalidate()
    }

    private fun saveRadius(
        dao: WorkTimeDao,
        currentSettings: SettingsEntity,
        radiusText: String,
        onMessage: (String) -> Unit
    ) {
        val radius = radiusText.toIntOrNull()

        if (radius == null || radius !in 25..1000) {
            onMessage("Radius muss zwischen 25 und 1000 Metern liegen")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            dao.upsertSettings(
                currentSettings.copy(geofenceRadiusMeters = radius.toFloat())
            )

            runOnUiThread {
                onMessage("Radius gespeichert")
            }
        }
    }

    private fun saveMarkerFromMap(
        dao: WorkTimeDao,
        currentSettings: SettingsEntity,
        geoPoint: GeoPoint,
        onMessage: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.upsertSettings(
                currentSettings.copy(
                    geofenceLatitude = geoPoint.latitude,
                    geofenceLongitude = geoPoint.longitude
                )
            )

            runOnUiThread {
                onMessage("Arbeitsplatz-Marker aus Karte gespeichert")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveCurrentLocationAsWorkplace(
        fusedLocationClient: FusedLocationProviderClient,
        dao: WorkTimeDao,
        currentSettings: SettingsEntity,
        radiusText: String,
        onMessage: (String) -> Unit
    ) {
        val radius = radiusText.toIntOrNull()

        if (radius == null || radius !in 25..1000) {
            onMessage("Radius muss zwischen 25 und 1000 Metern liegen")
            return
        }

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
                            geofenceRadiusMeters = radius.toFloat()
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

    private class LongPressOverlay(
        private val onLongPressGeoPoint: (GeoPoint) -> Unit
    ) : Overlay() {
        override fun onLongPress(event: MotionEvent, mapView: MapView): Boolean {
            val geoPoint = mapView.projection.fromPixels(
                event.x.toInt(),
                event.y.toInt()
            ) as GeoPoint

            onLongPressGeoPoint(geoPoint)
            return true
        }
    }
}
