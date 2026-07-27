package de.kai.arbeitszeitgeofence

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import de.kai.arbeitszeitgeofence.data.SettingsEntity
import de.kai.arbeitszeitgeofence.data.WorkTimeDao
import de.kai.arbeitszeitgeofence.data.WorkTimeEntryEntity
import de.kai.arbeitszeitgeofence.domain.TimeCalculator
import de.kai.arbeitszeitgeofence.domain.WorkSessionManager
import de.kai.arbeitszeitgeofence.geofence.GeofenceManager
import de.kai.arbeitszeitgeofence.service.TrackingForegroundService
import de.kai.arbeitszeitgeofence.worker.DayCloseWorker
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

private enum class AppScreen { Times, Settings }
private enum class TimeView(val label: String) { Day("Tag"), Week("Woche"), Month("Monat"), Year("Jahr") }

private data class TimeSummary(
    val blockCount: Int,
    val workdayCount: Int,
    val grossMinutes: Int,
    val breakMinutes: Int,
    val netMinutes: Int,
    val targetMinutes: Int,
    val balanceMinutes: Int
)

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

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
                    if (dao.getActiveState() == null) dao.upsertActiveState(WorkSessionManager.initialState())
                    if (dao.getSettings() == null) dao.upsertSettings(SettingsEntity())
                    DayCloseWorker.scheduleNext(this@MainActivity)
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
                    Text("Arbeitszeit Geofence", style = MaterialTheme.typography.headlineMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { selectedScreen = AppScreen.Times }) { Text("Zeiten") }
                        Button(onClick = { selectedScreen = AppScreen.Settings }) { Text("Einstellungen") }
                    }

                    Text(
                        "Status: ${if (state.isTracking) "Arbeitszeit laeuft" else "Gestoppt"} / " +
                            "Geofence: ${if (state.insideGeofence) "Innen" else "Aussen"}"
                    )
                    Text("Pause: ${state.accumulatedBreakMinutes} min${if (state.isBreakRunning) " + laufend" else ""}")
                    Text(message)

                    when (selectedScreen) {
                        AppScreen.Times -> TimesScreen(
                            modifier = Modifier.weight(1f),
                            entries = entries,
                            targetMinutesPerDay = effectiveSettings.targetMinutesPerDay,
                            onMessage = { message = it }
                        )

                        AppScreen.Settings -> SettingsScreen(
                            modifier = Modifier.weight(1f),
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
        modifier: Modifier,
        entries: List<WorkTimeEntryEntity>,
        targetMinutesPerDay: Int,
        onMessage: (String) -> Unit
    ) {
        var selectedTimeView by remember { mutableStateOf(TimeView.Day) }
        val filteredEntries = filterEntriesByView(entries, selectedTimeView)
        val summary = calculateSummary(filteredEntries, targetMinutesPerDay)

        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        TrackingForegroundService.start(this@MainActivity, TrackingForegroundService.ACTION_START)
                        onMessage("Manuell gestartet")
                    }) { Text("Start") }

                    Button(onClick = {
                        TrackingForegroundService.start(this@MainActivity, TrackingForegroundService.ACTION_STOP)
                        onMessage("Manuell gestoppt")
                    }) { Text("Stop") }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        TrackingForegroundService.start(this@MainActivity, TrackingForegroundService.ACTION_PAUSE_START)
                        onMessage("Pause gestartet")
                    }) { Text("Pause Start") }

                    Button(onClick = {
                        TrackingForegroundService.start(this@MainActivity, TrackingForegroundService.ACTION_PAUSE_STOP)
                        onMessage("Pause gestoppt")
                    }) { Text("Pause Stop") }
                }
            }

            item {
                Button(onClick = {
                    TrackingForegroundService.start(this@MainActivity, TrackingForegroundService.ACTION_DAY_CLOSE_MANUAL)
                    DayCloseWorker.scheduleNext(this@MainActivity)
                    onMessage("Tagesabschluss ausgefuehrt")
                }) { Text("Tagesabschluss") }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeView.entries.forEach { view ->
                        Button(onClick = { selectedTimeView = view }) { Text(view.label) }
                    }
                }
            }

            item { SummaryCard(selectedTimeView, summary) }
            item { Text("Eintraege ${selectedTimeView.label}", style = MaterialTheme.typography.titleLarge) }

            items(filteredEntries) { entry ->
                val calculation = TimeCalculator.calculate(
                    start = Instant.ofEpochMilli(entry.startEpochMillis),
                    end = Instant.ofEpochMilli(entry.endEpochMillis),
                    breakMinutes = entry.breakMinutes,
                    targetMinutes = targetMinutesPerDay
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${entry.localDate} | ${entry.source}")
                        Text("Netto: ${calculation.netMinutes} min | Saldo Block: ${calculation.balanceMinutes} min | Pause: ${entry.breakMinutes} min")
                        if (entry.comment.isNotBlank()) Text(entry.comment)
                    }
                }
            }
        }
    }

    @Composable
    private fun SummaryCard(view: TimeView, summary: TimeSummary) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Zusammenfassung ${view.label}", style = MaterialTheme.typography.titleLarge)
                Text("Bloecke: ${summary.blockCount}")
                Text("Tage mit Eintraegen: ${summary.workdayCount}")
                Text("Brutto: ${formatMinutes(summary.grossMinutes)}")
                Text("Pause: ${formatMinutes(summary.breakMinutes)}")
                Text("Netto: ${formatMinutes(summary.netMinutes)}")
                Text("Soll: ${formatMinutes(summary.targetMinutes)}")
                Text("Saldo: ${formatSignedMinutes(summary.balanceMinutes)}")
            }
        }
    }

    @Composable
    private fun SettingsScreen(
        modifier: Modifier,
        dao: WorkTimeDao,
        currentSettings: SettingsEntity,
        radiusText: String,
        onRadiusTextChange: (String) -> Unit,
        fusedLocationClient: FusedLocationProviderClient,
        geofenceManager: GeofenceManager,
        onMessage: (String) -> Unit
    ) {
        LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Arbeitsplatz-Geofence", style = MaterialTheme.typography.titleLarge)
                        GeofenceMapPreview(dao, currentSettings, currentSettings.geofenceRadiusMeters, onMessage)

                        OutlinedTextField(
                            value = radiusText,
                            onValueChange = { onRadiusTextChange(it.filter { char -> char.isDigit() }.take(4)) },
                            label = { Text("Radius in Metern") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { saveRadius(dao, currentSettings, radiusText, onMessage) }) { Text("Radius speichern") }
                            Button(onClick = { saveCurrentLocationAsWorkplace(fusedLocationClient, dao, currentSettings, radiusText, onMessage) }) { Text("Aktuelle Position speichern") }
                        }

                        Text("Koordinate: ${currentSettings.geofenceLatitude ?: "nicht gesetzt"}, ${currentSettings.geofenceLongitude ?: "nicht gesetzt"}")
                        Text("Hinweis: Marker per langem Druck auf die Karte setzen.")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val lat = currentSettings.geofenceLatitude
                                val lon = currentSettings.geofenceLongitude
                                if (lat == null || lon == null) {
                                    onMessage("Keine Arbeitsplatz-Koordinate gesetzt")
                                } else {
                                    try {
                                        geofenceManager.registerWorkplaceGeofence(lat, lon, currentSettings.geofenceRadiusMeters)
                                        onMessage("Geofence registriert")
                                    } catch (exception: SecurityException) {
                                        onMessage("Standortberechtigung fehlt: ${exception.message}")
                                    }
                                }
                            }) { Text("Geofence registrieren") }

                            Button(onClick = {
                                geofenceManager.unregisterWorkplaceGeofence()
                                onMessage("Geofence entfernt")
                            }) { Text("Geofence entfernen") }
                        }

                        Text("© OpenStreetMap contributors")
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Arbeitszeitparameter", style = MaterialTheme.typography.titleLarge)
                        Text("Sollzeit pro Tag: ${currentSettings.targetMinutesPerDay} Minuten")
                        Text("Standardpause: ${currentSettings.defaultBreakMinutes} Minuten")
                        Text("Tagesabschluss: ${currentSettings.autoDayCloseTime}")
                        Text("Schicht ueber Mitternacht: vorbereitet, aktuell deaktiviert")
                    }
                }
            }
        }
    }

    @Composable
    private fun GeofenceMapPreview(dao: WorkTimeDao, settings: SettingsEntity, radiusMeters: Float, onMessage: (String) -> Unit) {
        val lat = settings.geofenceLatitude
        val lon = settings.geofenceLongitude
        if (lat == null || lon == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Noch keine Karte verfuegbar.")
                    Text("Bitte zuerst aktuelle Position speichern oder spaeter per Karte setzen.")
                }
            }
            return
        }

        AndroidView(
            modifier = Modifier.fillMaxWidth().height(280.dp),
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
            update = { map -> updateGeofenceMap(map, dao, settings, lat, lon, radiusMeters, onMessage) }
        )
    }

    private fun updateGeofenceMap(mapView: MapView, dao: WorkTimeDao, settings: SettingsEntity, lat: Double, lon: Double, radiusMeters: Float, onMessage: (String) -> Unit) {
        val center = GeoPoint(lat, lon)
        mapView.controller.setZoom(18.0)
        mapView.controller.setCenter(center)
        mapView.overlays.clear()

        mapView.overlays.add(Polygon(mapView).apply {
            points = Polygon.pointsAsCircle(center, radiusMeters.toDouble())
            fillColor = 0x3333B5E5
            strokeColor = 0xFF0288D1.toInt()
            strokeWidth = 4f
        })
        mapView.overlays.add(Marker(mapView).apply {
            position = center
            title = "Arbeitsplatz"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        })
        mapView.overlays.add(LongPressOverlay { point -> saveMarkerFromMap(dao, settings, point, onMessage) })
        mapView.invalidate()
    }

    private fun saveRadius(dao: WorkTimeDao, settings: SettingsEntity, radiusText: String, onMessage: (String) -> Unit) {
        val radius = radiusText.toIntOrNull()
        if (radius == null || radius !in 25..1000) {
            onMessage("Radius muss zwischen 25 und 1000 Metern liegen")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            dao.upsertSettings(settings.copy(geofenceRadiusMeters = radius.toFloat()))
            runOnUiThread { onMessage("Radius gespeichert") }
        }
    }

    private fun saveMarkerFromMap(dao: WorkTimeDao, settings: SettingsEntity, point: GeoPoint, onMessage: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.upsertSettings(settings.copy(geofenceLatitude = point.latitude, geofenceLongitude = point.longitude))
            runOnUiThread { onMessage("Arbeitsplatz-Marker aus Karte gespeichert") }
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveCurrentLocationAsWorkplace(client: FusedLocationProviderClient, dao: WorkTimeDao, settings: SettingsEntity, radiusText: String, onMessage: (String) -> Unit) {
        val radius = radiusText.toIntOrNull()
        if (radius == null || radius !in 25..1000) {
            onMessage("Radius muss zwischen 25 und 1000 Metern liegen")
            return
        }
        client.lastLocation
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    onMessage("Keine letzte Position verfuegbar. Standort kurz aktivieren und erneut versuchen.")
                    return@addOnSuccessListener
                }
                CoroutineScope(Dispatchers.IO).launch {
                    dao.upsertSettings(settings.copy(geofenceLatitude = loc.latitude, geofenceLongitude = loc.longitude, geofenceRadiusMeters = radius.toFloat()))
                    runOnUiThread { onMessage("Arbeitsplatz-Koordinate gespeichert") }
                }
            }
            .addOnFailureListener { onMessage("Position konnte nicht gelesen werden: ${it.message}") }
    }

    private fun filterEntriesByView(entries: List<WorkTimeEntryEntity>, view: TimeView): List<WorkTimeEntryEntity> {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val weekFields = WeekFields.ISO
        val currentWeek = today.get(weekFields.weekOfWeekBasedYear())
        val currentWeekYear = today.get(weekFields.weekBasedYear())

        return entries.filter { entry ->
            val date = Instant.ofEpochMilli(entry.startEpochMillis).atZone(zoneId).toLocalDate()
            when (view) {
                TimeView.Day -> date == today
                TimeView.Week -> date.get(weekFields.weekOfWeekBasedYear()) == currentWeek && date.get(weekFields.weekBasedYear()) == currentWeekYear
                TimeView.Month -> date.year == today.year && date.month == today.month
                TimeView.Year -> date.year == today.year
            }
        }
    }

    private fun calculateSummary(entries: List<WorkTimeEntryEntity>, targetMinutesPerDay: Int): TimeSummary {
        val zoneId = ZoneId.systemDefault()
        var grossMinutes = 0
        var breakMinutes = 0
        var netMinutes = 0

        entries.forEach { entry ->
            val gross = ((entry.endEpochMillis - entry.startEpochMillis) / 60000L).toInt().coerceAtLeast(0)
            val boundedBreak = entry.breakMinutes.coerceIn(0, gross)
            grossMinutes += gross
            breakMinutes += boundedBreak
            netMinutes += gross - boundedBreak
        }

        val workdayCount = entries.map { Instant.ofEpochMilli(it.startEpochMillis).atZone(zoneId).toLocalDate() }.distinct().size
        val targetMinutes = targetMinutesPerDay * workdayCount

        return TimeSummary(entries.size, workdayCount, grossMinutes, breakMinutes, netMinutes, targetMinutes, netMinutes - targetMinutes)
    }

    private fun formatMinutes(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"

    private fun formatSignedMinutes(minutes: Int): String {
        val sign = if (minutes >= 0) "+" else "-"
        val absolute = kotlin.math.abs(minutes)
        return "${sign}${absolute / 60}h ${absolute % 60}m"
    }

    private class LongPressOverlay(private val onLongPressGeoPoint: (GeoPoint) -> Unit) : Overlay() {
        override fun onLongPress(event: MotionEvent, mapView: MapView): Boolean {
            val geoPoint = mapView.projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
            onLongPressGeoPoint(geoPoint)
            return true
        }
    }
}
