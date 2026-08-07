package de.kai.arbeitszeitgeofence

import de.kai.arbeitszeitgeofence.ui.MatrixButton
import de.kai.arbeitszeitgeofence.ui.MatrixTextField
import de.kai.arbeitszeitgeofence.ui.MatrixBackground
import de.kai.arbeitszeitgeofence.ui.MatrixSurface
import de.kai.arbeitszeitgeofence.ui.HeroStatusCard
import de.kai.arbeitszeitgeofence.ui.DashboardKpiCard
import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color as ComposeColor
import de.kai.arbeitszeitgeofence.ui.MatrixGreen
import de.kai.arbeitszeitgeofence.ui.MatrixRed

import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import de.kai.arbeitszeitgeofence.data.SettingsEntity
import de.kai.arbeitszeitgeofence.data.WorkTimeDao
import de.kai.arbeitszeitgeofence.data.WorkTimeEntryEntity
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

private enum class AppScreen { Times, Settings }
private enum class TimeView(val label: String) { Day("Tag"), Week("Woche"), Month("Monat"), Year("Jahr") }
private data class DayOverride(val localDate: String, val type: String, val comment: String)
private data class WorkdaySettings(
    val monday: Boolean = true,
    val tuesday: Boolean = true,
    val wednesday: Boolean = true,
    val thursday: Boolean = true,
    val friday: Boolean = true,
    val saturday: Boolean = false,
    val sunday: Boolean = false
)
private data class DailySummary(
    val date: LocalDate,
    val dayOverride: DayOverride?,
    val entries: List<WorkTimeEntryEntity>,
    val grossMinutes: Int,
    val breakMinutes: Int,
    val netMinutes: Int,
    val targetMinutes: Int,
    val balanceMinutes: Int
)
private data class PeriodSummary(
    val dayCount: Int,
    val regularWorkdayCount: Int,
    val overrideCount: Int,
    val grossMinutes: Int,
    val breakMinutes: Int,
    val netMinutes: Int,
    val targetMinutes: Int,
    val balanceMinutes: Int
)

private val entryDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

class MainActivity : ComponentActivity() {
    private var pendingCsvExport: String? = null

    private val createCsvDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val csvContent = pendingCsvExport
        pendingCsvExport = null
        if (uri != null && csvContent != null) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS))

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
                var targetText by remember { mutableStateOf("480") }
                var defaultBreakText by remember { mutableStateOf("45") }
                var dayCloseText by remember { mutableStateOf("23:59") }
                var geofenceRegistered by remember { mutableStateOf(readGeofenceRegistered()) }
                var geofenceRegisteredAt by remember { mutableStateOf(readGeofenceRegisteredAt()) }
                var workdaySettings by remember { mutableStateOf(readWorkdaySettings()) }
                var dayOverrides by remember { mutableStateOf(readDayOverrides()) }

                LaunchedEffect(Unit) {
                    if (dao.getActiveState() == null) dao.upsertActiveState(WorkSessionManager.initialState())
                    if (dao.getSettings() == null) dao.upsertSettings(SettingsEntity())
                    DayCloseWorker.scheduleNext(this@MainActivity)
                }

                LaunchedEffect(settings) {
                    val effective = settings ?: SettingsEntity()
                    radiusText = effective.geofenceRadiusMeters.toInt().toString()
                    targetText = effective.targetMinutesPerDay.toString()
                    defaultBreakText = effective.defaultBreakMinutes.toString()
                    dayCloseText = effective.autoDayCloseTime
                }

                val state = activeState ?: WorkSessionManager.initialState()
                val effectiveSettings = settings ?: SettingsEntity()

                Column(
                    modifier = Modifier.fillMaxSize().background(MatrixBackground).statusBarsPadding().navigationBarsPadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Arbeitszeit Matrix", style = MaterialTheme.typography.headlineMedium, color = MatrixGreen
					)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MatrixButton(text = "Zeiten",onClick = {selectedScreen = AppScreen.Times})
                        MatrixButton(text = "Einstellungen",onClick = {selectedScreen = AppScreen.Settings})}
Text(
    text = message,
    color = MatrixGreen
)
					HeroStatusCard(
					isTracking = state.isTracking,
					pauseMinutes = state.accumulatedBreakMinutes,
					insideGeofence = state.insideGeofence )

					when (selectedScreen) {
                        AppScreen.Times -> TimesScreen(
                            modifier = Modifier.weight(1f),
                            dao = dao,
                            entries = entries,
                            settings = effectiveSettings,
                            workdaySettings = workdaySettings,
                            dayOverrides = dayOverrides,
                            onMessage = { message = it }
                        )
                        AppScreen.Settings -> SettingsScreen(
                            modifier = Modifier.weight(1f),
                            dao = dao,
                            currentSettings = effectiveSettings,
                            radiusText = radiusText,
                            onRadiusTextChange = { radiusText = it },
                            targetText = targetText,
                            onTargetTextChange = { targetText = it },
                            defaultBreakText = defaultBreakText,
                            onDefaultBreakTextChange = { defaultBreakText = it },
                            dayCloseText = dayCloseText,
                            onDayCloseTextChange = { dayCloseText = it },
                            geofenceRegistered = geofenceRegistered,
                            geofenceRegisteredAt = geofenceRegisteredAt,
                            onGeofenceStatusChange = { registered, registeredAt ->
                                geofenceRegistered = registered
                                geofenceRegisteredAt = registeredAt
                                writeGeofenceStatus(registered, registeredAt)
                            },
                            workdaySettings = workdaySettings,
                            onWorkdaySettingsChange = { newSettings ->
                                workdaySettings = newSettings
                                writeWorkdaySettings(newSettings)
                            },
                            dayOverrides = dayOverrides,
                            onDayOverridesChange = { newOverrides ->
                                dayOverrides = newOverrides
                                writeDayOverrides(newOverrides)
                            },
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
        dao: WorkTimeDao,
        entries: List<WorkTimeEntryEntity>,
        settings: SettingsEntity,
        workdaySettings: WorkdaySettings,
        dayOverrides: List<DayOverride>,
        onMessage: (String) -> Unit
    ) {
        var selectedTimeView by remember { mutableStateOf(TimeView.Week) }
        var selectedDay by remember { mutableStateOf<DailySummary?>(null) }
        var editEntry by remember { mutableStateOf<WorkTimeEntryEntity?>(null) }
        var confirmDeleteAll by remember { mutableStateOf(false) }
        val dailySummaries = buildDailySummaries(entries, selectedTimeView, settings, workdaySettings, dayOverrides)
        val periodSummary = calculatePeriodSummary(dailySummaries)

        LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
item {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        MatrixButton(
            text = "Start",
            onClick = {
                TrackingForegroundService.start(
                    this@MainActivity,
                    TrackingForegroundService.ACTION_START
                )
                onMessage("Manuell gestartet")
            }
        )

        MatrixButton(
            text = "Stop",
            onClick = {
                TrackingForegroundService.start(
                    this@MainActivity,
                    TrackingForegroundService.ACTION_STOP
                )
                onMessage("Manuell gestoppt")
            }
        )

        MatrixButton(
            text = "Pause Start",
            onClick = {
                TrackingForegroundService.start(
                    this@MainActivity,
                    TrackingForegroundService.ACTION_PAUSE_START
                )
                onMessage("Pause gestartet")
            }
        )

        MatrixButton(
            text = "Pause Stop",
            onClick = {
                TrackingForegroundService.start(
                    this@MainActivity,
                    TrackingForegroundService.ACTION_PAUSE_STOP
                )
                onMessage("Pause gestoppt")
            }
        )
    }
}


item {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        MatrixButton(
            text = "Tagesabschluss",
            onClick = {
                TrackingForegroundService.start(
                    this@MainActivity,
                    TrackingForegroundService.ACTION_DAY_CLOSE_MANUAL
                )

                DayCloseWorker.scheduleNext(
                    this@MainActivity
                )

                onMessage(
                    "Tagesabschluss ausgefuehrt"
                )
            }
        )

        MatrixButton(
            text = "Export",
            onClick = {
                exportCsv(
                    selectedTimeView,
                    dailySummaries
                )

                onMessage(
                    "CSV Export vorbereitet"
                )
            }
        )
    }
}
            item {

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {

        TimeView.entries.forEachIndexed { index, view ->

         SegmentedButton(
    selected = selectedTimeView == view,
    onClick = {
        selectedTimeView = view
    },
    shape = SegmentedButtonDefaults.itemShape(
        index = index,
        count = TimeView.entries.size
    ),
    colors = SegmentedButtonDefaults.colors(
        activeContainerColor = MatrixGreen,
        activeContentColor = MatrixBackground,
        inactiveContainerColor = MatrixSurface,
        inactiveContentColor = MatrixGreen
    ),
    border = BorderStroke(
        1.dp,
        MatrixGreen.copy(alpha = 0.5f)
    )
)
			{
                Text(view.label)
            }
        }
    }
}
item {

    MatrixButton(
        text = "Alle Eintraege loeschen",
        onClick = {
            confirmDeleteAll = true
        }
    )
}
            item { PeriodSummaryCard(selectedTimeView, periodSummary) }
            item { Text("Tage ${selectedTimeView.label}", style = MaterialTheme.typography.titleLarge, color = MatrixGreen) }
            items(dailySummaries) { dailySummary -> DailySummaryCard(dailySummary, onDetails = { selectedDay = dailySummary }) }
        }

        selectedDay?.let { summary ->
            DayDetailsDialog(summary, dao, onDismiss = { selectedDay = null }, onEdit = { entry -> editEntry = entry }, onMessage = onMessage)
        }
        editEntry?.let { entry -> EditEntryDialog(entry = entry, dao = dao, onDismiss = { editEntry = null }, onMessage = onMessage) }

        if (confirmDeleteAll) {
            AlertDialog(
                onDismissRequest = { confirmDeleteAll = false },
                title = { Text("Alle Eintraege loeschen?") },
                text = { Text("Diese Aktion entfernt alle Arbeitszeit-Eintraege. Einstellungen und Sondertage bleiben erhalten.") },
                confirmButton = {
                    TextButton(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            dao.deleteAllEntries()
                            dao.upsertActiveState(WorkSessionManager.initialState())
                            runOnUiThread { onMessage("Alle Eintraege geloescht"); confirmDeleteAll = false }
                        }
                    }) { Text("Loeschen") }
                },
                dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Abbrechen") } }
            )
        }
    }

    @Composable
    private fun DailySummaryCard(summary: DailySummary, onDetails: () -> Unit) {
        Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MatrixSurface
    )
) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${weekdayLabel(summary.date.dayOfWeek)}, ${summary.date}", color = ComposeColor.White)
                summary.dayOverride?.let { Text("Sondertag: ${it.type}${if (it.comment.isNotBlank()) " - ${it.comment}" else ""}") }
                Text("Arbeitszeit: ${formatMinutes(summary.netMinutes)} | Pause: ${formatMinutes(summary.breakMinutes)}", color = ComposeColor.White)
                Text(
    "Soll: ${formatMinutes(summary.targetMinutes)}"
)

Text(
    text = "Saldo: ${formatSignedMinutes(summary.balanceMinutes)}",
    color = when {
        summary.balanceMinutes > 0 -> MatrixGreen
        summary.balanceMinutes < 0 -> MatrixRed
        else -> ComposeColor.Gray
    }
)
                Text("Bloecke: ${summary.entries.size}", color = ComposeColor.White)
                if (summary.entries.isNotEmpty()) {MatrixButton(text = "Details",onClick = onDetails) }
            }
        }
    }

@Composable
private fun PeriodSummaryCard(
    view: TimeView,
    summary: PeriodSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MatrixSurface
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = "Zusammenfassung ${view.label}",
                style = MaterialTheme.typography.titleLarge,
                color = MatrixGreen
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    DashboardKpiCard(
                        title = "Netto",
                        value = formatMinutes(summary.netMinutes)
                    )
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    DashboardKpiCard(
                        title = "Saldo",
                        value = formatSignedMinutes(summary.balanceMinutes),
                        color = when {
                            summary.balanceMinutes > 0 -> MatrixGreen
                            summary.balanceMinutes < 0 -> MatrixRed
                            else -> ComposeColor.Gray
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    DashboardKpiCard(
                        title = "Pause",
                        value = formatMinutes(summary.breakMinutes)
                    )
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    DashboardKpiCard(
                        title = "Soll",
                        value = formatMinutes(summary.targetMinutes)
                    )
                }
            }

            Text(
                text = "Tage: ${summary.dayCount} | Arbeitstage: ${summary.regularWorkdayCount} | Sondertage: ${summary.overrideCount}",
                color = ComposeColor.White
            )
        }
    }
}

    @Composable
    private fun DayDetailsDialog(summary: DailySummary, dao: WorkTimeDao, onDismiss: () -> Unit, onEdit: (WorkTimeEntryEntity) -> Unit, onMessage: (String) -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Details ${summary.date}") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text("Netto: ${formatMinutes(summary.netMinutes)} | Soll: ${formatMinutes(summary.targetMinutes)} | Saldo: ${formatSignedMinutes(summary.balanceMinutes)}") }
                    items(summary.entries) { entry -> EntryCard(entry, dao, onEdit = { onEdit(entry) }, onMessage = onMessage) }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Schliessen") } }
        )
    }

    @Composable
private fun EntryCard(
    entry: WorkTimeEntryEntity,
    dao: WorkTimeDao,
    onEdit: () -> Unit,
    onMessage: (String) -> Unit
) {

    val gross =
        ((entry.endEpochMillis - entry.startEpochMillis) / 60000L)
            .toInt()
            .coerceAtLeast(0)

    val pause =
        entry.breakMinutes.coerceIn(
            0,
            gross
        )

    val net = gross - pause

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MatrixSurface
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = "${formatTime(entry.startEpochMillis)} - ${formatTime(entry.endEpochMillis)} | ${entry.source}",
                color = ComposeColor.White
            )

            Text(
                text = "Netto: ${formatMinutes(net)} | Pause: ${formatMinutes(pause)}",
                color = ComposeColor.White
            )

            if (entry.comment.isNotBlank()) {

                Text(
                    text = entry.comment,
                    color = ComposeColor.White
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                MatrixButton(
                    text = "Bearbeiten",
                    onClick = onEdit
                )

                MatrixButton(
                    text = "Loeschen",
                    onClick = {

                        CoroutineScope(
                            Dispatchers.IO
                        ).launch {

                            dao.deleteEntry(entry.id)

                            runOnUiThread {
                                onMessage(
                                    "Eintrag geloescht"
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

    @Composable
    private fun EditEntryDialog(entry: WorkTimeEntryEntity, dao: WorkTimeDao, onDismiss: () -> Unit, onMessage: (String) -> Unit) {
        val zoneId = ZoneId.systemDefault()
        var startText by remember { mutableStateOf(formatEpoch(entry.startEpochMillis, zoneId)) }
        var endText by remember { mutableStateOf(formatEpoch(entry.endEpochMillis, zoneId)) }
        var breakText by remember { mutableStateOf(entry.breakMinutes.toString()) }
        var commentText by remember { mutableStateOf(entry.comment) }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Eintrag bearbeiten") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Format: yyyy-MM-dd HH:mm")
                    OutlinedTextField(startText, { startText = it }, label = { Text("Start") }, singleLine = true)
                    OutlinedTextField(endText, { endText = it }, label = { Text("Ende") }, singleLine = true)
                    OutlinedTextField(breakText, { breakText = it.filter { char -> char.isDigit() }.take(4) }, label = { Text("Pausenminuten") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(commentText, { commentText = it }, label = { Text("Kommentar") })
                    errorText?.let { Text(it) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val start = parseDateTime(startText, zoneId)
                    val end = parseDateTime(endText, zoneId)
                    val breakMinutes = breakText.toIntOrNull()
                    when {
                        start == null -> errorText = "Startzeit ungueltig"
                        end == null -> errorText = "Endzeit ungueltig"
                        end.isBefore(start) -> errorText = "Ende liegt vor Start"
                        breakMinutes == null -> errorText = "Pause ungueltig"
                        else -> CoroutineScope(Dispatchers.IO).launch {
                            dao.updateEntry(entry.copy(localDate = start.atZone(zoneId).toLocalDate().toString(), startEpochMillis = start.toEpochMilli(), endEpochMillis = end.toEpochMilli(), breakMinutes = breakMinutes, comment = commentText, modifiedAtEpochMillis = Instant.now().toEpochMilli()))
                            runOnUiThread { onMessage("Eintrag aktualisiert"); onDismiss() }
                        }
                    }
                }) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
        )
    }

    @Composable
    private fun SettingsScreen(
        modifier: Modifier,
        dao: WorkTimeDao,
        currentSettings: SettingsEntity,
        radiusText: String,
        onRadiusTextChange: (String) -> Unit,
        targetText: String,
        onTargetTextChange: (String) -> Unit,
        defaultBreakText: String,
        onDefaultBreakTextChange: (String) -> Unit,
        dayCloseText: String,
        onDayCloseTextChange: (String) -> Unit,
        geofenceRegistered: Boolean,
        geofenceRegisteredAt: Long?,
        onGeofenceStatusChange: (Boolean, Long?) -> Unit,
        workdaySettings: WorkdaySettings,
        onWorkdaySettingsChange: (WorkdaySettings) -> Unit,
        dayOverrides: List<DayOverride>,
        onDayOverridesChange: (List<DayOverride>) -> Unit,
        fusedLocationClient: FusedLocationProviderClient,
        geofenceManager: GeofenceManager,
        onMessage: (String) -> Unit
    ) {
        LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { GeofenceSettingsCard(dao, currentSettings, radiusText, onRadiusTextChange, geofenceRegistered, geofenceRegisteredAt, onGeofenceStatusChange, fusedLocationClient, geofenceManager, onMessage) }
            item { WorkSettingsCard(dao, currentSettings, targetText, onTargetTextChange, defaultBreakText, onDefaultBreakTextChange, dayCloseText, onDayCloseTextChange, onMessage) }
            item { WorkdaySettingsCard(workdaySettings, onWorkdaySettingsChange, onMessage) }
            item { DayOverrideSettingsCard(dayOverrides, onDayOverridesChange, onMessage) }
            item { AppResetCard(dao, onDayOverridesChange, onWorkdaySettingsChange, onMessage) }
        }
    }

    @Composable
private fun GeofenceSettingsCard(
    dao: WorkTimeDao,
    settings: SettingsEntity,
    radiusText: String,
    onRadiusTextChange: (String) -> Unit,
    geofenceRegistered: Boolean,
    geofenceRegisteredAt: Long?,
    onGeofenceStatusChange: (Boolean, Long?) -> Unit,
    client: FusedLocationProviderClient,
    geofenceManager: GeofenceManager,
    onMessage: (String) -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MatrixSurface
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                "Arbeitsplatz-Geofence",
                style = MaterialTheme.typography.titleLarge,
                color = MatrixGreen
            )

            Text(
                if (geofenceRegistered)
                    "Geofence Status: Aktiv"
                else
                    "Geofence Status: Nicht registriert",
                color = ComposeColor.White
            )

            geofenceRegisteredAt?.let {

                Text(
                    "Registriert am: ${
                        formatEpoch(
                            it,
                            ZoneId.systemDefault()
                        )
                    }",
                    color = ComposeColor.White
                )
            }

            GeofenceMapPreview(
                dao,
                settings,
                settings.geofenceRadiusMeters,
                onMessage
            )

            MatrixTextField(
                value = radiusText,
                label = "Radius in Metern",
                onValueChange = {
                    onRadiusTextChange(
                        it.filter { char -> char.isDigit() }
                            .take(4)
                    )
                }
            )

            MatrixButton(
                text = "Radius speichern",
                onClick = {
                    saveRadius(
                        dao,
                        settings,
                        radiusText,
                        onMessage
                    )
                }
            )

            MatrixButton(
                text = "Aktuelle Position speichern",
                onClick = {
                    saveCurrentLocationAsWorkplace(
                        client,
                        dao,
                        settings,
                        radiusText,
                        onMessage
                    )
                }
            )

            Text(
                "Koordinate: ${settings.geofenceLatitude ?: "nicht gesetzt"}, ${settings.geofenceLongitude ?: "nicht gesetzt"}",
                color = ComposeColor.White
            )

            MatrixButton(
                text = "Geofence registrieren",
                onClick = {

                    val lat = settings.geofenceLatitude
                    val lon = settings.geofenceLongitude

                    if (lat == null || lon == null) {

                        onMessage(
                            "Keine Arbeitsplatz-Koordinate gesetzt"
                        )

                    } else {

                        try {

                            geofenceManager.registerWorkplaceGeofence(
                                lat,
                                lon,
                                settings.geofenceRadiusMeters
                            )

                            val now =
                                Instant.now().toEpochMilli()

                            onGeofenceStatusChange(
                                true,
                                now
                            )

                            onMessage(
                                "Geofence erfolgreich registriert"
                            )

                        } catch (ex: SecurityException) {

                            onGeofenceStatusChange(
                                false,
                                null
                            )

                            onMessage(
                                "Standortberechtigung fehlt: ${ex.message}"
                            )
                        }
                    }
                }
            )

            MatrixButton(
                text = "Geofence entfernen",
                onClick = {
                    geofenceManager.unregisterWorkplaceGeofence()

                    onGeofenceStatusChange(
                        false,
                        null
                    )

                    onMessage(
                        "Geofence entfernt"
                    )
                }
            )
        }
    }
}

    @Composable
private fun WorkSettingsCard(
    dao: WorkTimeDao,
    settings: SettingsEntity,
    targetText: String,
    onTargetTextChange: (String) -> Unit,
    breakText: String,
    onBreakTextChange: (String) -> Unit,
    dayCloseText: String,
    onDayCloseTextChange: (String) -> Unit,
    onMessage: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MatrixSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                "Arbeitszeitparameter",
                style = MaterialTheme.typography.titleLarge,
                color = MatrixGreen
            )

            MatrixTextField(
                value = targetText,
                label = "Sollzeit pro Arbeitstag in Minuten",
                onValueChange = {
                    onTargetTextChange(
                        it.filter { char -> char.isDigit() }
                            .take(4)
                    )
                }
            )

            MatrixTextField(
                value = breakText,
                label = "Standardpause in Minuten",
                onValueChange = {
                    onBreakTextChange(
                        it.filter { char -> char.isDigit() }
                            .take(4)
                    )
                }
            )

            MatrixTextField(
                value = dayCloseText,
                label = "Tagesabschluss HH:mm",
                onValueChange = onDayCloseTextChange
            )

            MatrixButton(
                text = "Arbeitszeitparameter speichern",
                onClick = {
                    saveWorkSettings(
                        dao,
                        settings,
                        targetText,
                        breakText,
                        dayCloseText,
                        onMessage
                    )
                }
            )
        }
    }
}
    @Composable
    private fun WorkdaySettingsCard(workdaySettings: WorkdaySettings, onWorkdaySettingsChange: (WorkdaySettings) -> Unit, onMessage: (String) -> Unit) {
        Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MatrixSurface
    )
) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Regulaere Arbeitstage", style = MaterialTheme.typography.titleLarge, color = MatrixGreen)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WorkdayButton("Mo", workdaySettings.monday) { onWorkdaySettingsChange(workdaySettings.copy(monday = !workdaySettings.monday)); onMessage("Arbeitstage gespeichert") }
                    WorkdayButton("Di", workdaySettings.tuesday) { onWorkdaySettingsChange(workdaySettings.copy(tuesday = !workdaySettings.tuesday)); onMessage("Arbeitstage gespeichert") }
                    WorkdayButton("Mi", workdaySettings.wednesday) { onWorkdaySettingsChange(workdaySettings.copy(wednesday = !workdaySettings.wednesday)); onMessage("Arbeitstage gespeichert") }
                    WorkdayButton("Do", workdaySettings.thursday) { onWorkdaySettingsChange(workdaySettings.copy(thursday = !workdaySettings.thursday)); onMessage("Arbeitstage gespeichert") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WorkdayButton("Fr", workdaySettings.friday) { onWorkdaySettingsChange(workdaySettings.copy(friday = !workdaySettings.friday)); onMessage("Arbeitstage gespeichert") }
                    WorkdayButton("Sa", workdaySettings.saturday) { onWorkdaySettingsChange(workdaySettings.copy(saturday = !workdaySettings.saturday)); onMessage("Arbeitstage gespeichert") }
                    WorkdayButton("So", workdaySettings.sunday) { onWorkdaySettingsChange(workdaySettings.copy(sunday = !workdaySettings.sunday)); onMessage("Arbeitstage gespeichert") }
                }
            }
        }
    }

@Composable
private fun WorkdayButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (enabled)
                    MatrixGreen
                else
                    MatrixRed
        )
    ) {

        Text(label)
    }
}

   @Composable
private fun DayOverrideSettingsCard(
    dayOverrides: List<DayOverride>,
    onDayOverridesChange: (List<DayOverride>) -> Unit,
    onMessage: (String) -> Unit
) {

    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var typeText by remember { mutableStateOf("Urlaub") }
    var commentText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MatrixSurface
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "Sondertage / Abwesenheiten",
                style = MaterialTheme.typography.titleLarge,
                color = MatrixGreen
            )

            MatrixTextField(
                value = dateText,
                label = "Datum yyyy-MM-dd",
                onValueChange = {
                    dateText = it
                }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                listOf(
                    "Urlaub",
                    "Feiertag",
                    "Krank",
                    "Frei"
                ).forEach { type ->

                    MatrixButton(
                        text = type,
                        onClick = {
                            typeText = type
                        }
                    )
                }
            }

            MatrixTextField(
                value = typeText,
                label = "Typ",
                onValueChange = {
                    typeText = it
                }
            )

            MatrixTextField(
                value = commentText,
                label = "Kommentar",
                onValueChange = {
                    commentText = it
                }
            )

            MatrixButton(
                text = "Sondertag speichern",
                onClick = {

                    if (
                        runCatching {
                            LocalDate.parse(dateText)
                        }.isFailure
                    ) {
                        onMessage("Datum ungueltig")
                    } else {

                        val updated =
                            dayOverrides.filterNot {
                                it.localDate == dateText
                            } + DayOverride(
                                dateText,
                                typeText,
                                commentText
                            )

                        onDayOverridesChange(
                            updated.sortedByDescending {
                                it.localDate
                            }
                        )

                        onMessage(
                            "Sondertag gespeichert"
                        )
                    }
                }
            )

            dayOverrides
                .take(10)
                .forEach { dayOverride ->

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Text(
                            text = "${dayOverride.localDate} | ${dayOverride.type}",
                            color = ComposeColor.White
                        )

                        MatrixButton(
                            text = "Loeschen",
                            onClick = {

                                onDayOverridesChange(
                                    dayOverrides.filterNot {
                                        it.localDate ==
                                                dayOverride.localDate
                                    }
                                )

                                onMessage(
                                    "Sondertag geloescht"
                                )
                            }
                        )
                    }
                }
        }
    }
}
    @Composable
    private fun AppResetCard(dao: WorkTimeDao, onDayOverridesChange: (List<DayOverride>) -> Unit, onWorkdaySettingsChange: (WorkdaySettings) -> Unit, onMessage: (String) -> Unit) {
        var confirmReset by remember { mutableStateOf(false) }
        Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MatrixSurface
    )
) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("App Reset", style = MaterialTheme.typography.titleLarge, color = MatrixGreen)
                Text("Loescht alle Eintraege, lokale Arbeitstage/Sondertage und setzt Einstellungen zurueck.",color = ComposeColor.White)
                MatrixButton(text = "App Reset",onClick = {confirmReset = true})
            }
        }
        if (confirmReset) {
            AlertDialog(
                onDismissRequest = { confirmReset = false },
                title = { Text("App wirklich zuruecksetzen?") },
                text = { Text("Alle Eintraege und lokalen Sonder-Einstellungen werden zurueckgesetzt.",color = ComposeColor.White) },
                confirmButton = {
                    TextButton(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            dao.deleteAllEntries()
                            dao.upsertActiveState(WorkSessionManager.initialState())
                            dao.upsertSettings(SettingsEntity())
                            clearLocalPlanningSettings()
                            runOnUiThread {
                                onDayOverridesChange(emptyList())
                                onWorkdaySettingsChange(WorkdaySettings())
                                onMessage("App Reset abgeschlossen")
                                confirmReset = false
                            }
                        }
                    }) { Text("Reset") }
                },
                dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Abbrechen") } }
            )
        }
    }

    private fun exportCsv(view: TimeView, dailySummaries: List<DailySummary>) {
        if (view == TimeView.Day) return
        val csv = buildDailyCsv(dailySummaries)
        val fileName = "arbeitszeit_export_${view.label.lowercase()}_${LocalDate.now()}.csv"
        pendingCsvExport = csv
        createCsvDocumentLauncher.launch(fileName)
    }

    private fun buildDailyCsv(dailySummaries: List<DailySummary>): String {
        val header = listOf("Datum", "Wochentag", "StartErsterBlock", "EndeLetzterBlock", "BruttoMinuten", "PauseMinuten", "NettoMinuten", "SollMinuten", "SaldoMinuten", "AnzahlBloecke", "Sondertag", "Kommentar")
        val lines = mutableListOf(header.joinToString(";") { csvQuote(it) })
        dailySummaries.forEach { day ->
            val row = listOf(
                day.date.toString(),
                weekdayLabel(day.date.dayOfWeek),
                day.entries.minByOrNull { it.startEpochMillis }?.let { formatEpoch(it.startEpochMillis, ZoneId.systemDefault()) } ?: "",
                day.entries.maxByOrNull { it.endEpochMillis }?.let { formatEpoch(it.endEpochMillis, ZoneId.systemDefault()) } ?: "",
                day.grossMinutes.toString(),
                day.breakMinutes.toString(),
                day.netMinutes.toString(),
                day.targetMinutes.toString(),
                day.balanceMinutes.toString(),
                day.entries.size.toString(),
                day.dayOverride?.type ?: "",
                day.dayOverride?.comment ?: ""
            )
            lines.add(row.joinToString(";") { csvQuote(it) })
        }
        return "\uFEFF" + lines.joinToString("\n")
    }

    private fun csvQuote(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    private fun saveWorkSettings(dao: WorkTimeDao, settings: SettingsEntity, targetText: String, breakText: String, dayCloseText: String, onMessage: (String) -> Unit) {
        val target = targetText.toIntOrNull()
        val pause = breakText.toIntOrNull()
        val closeIsValid = runCatching { java.time.LocalTime.parse(dayCloseText) }.isSuccess
        when {
            target == null || target !in 0..1440 -> onMessage("Sollzeit muss zwischen 0 und 1440 Minuten liegen")
            pause == null || pause !in 0..1440 -> onMessage("Pause muss zwischen 0 und 1440 Minuten liegen")
            !closeIsValid -> onMessage("Tagesabschluss muss im Format HH:mm sein")
            else -> CoroutineScope(Dispatchers.IO).launch {
                dao.upsertSettings(settings.copy(targetMinutesPerDay = target, defaultBreakMinutes = pause, autoDayCloseTime = dayCloseText))
                DayCloseWorker.scheduleNext(this@MainActivity)
                runOnUiThread { onMessage("Arbeitszeitparameter gespeichert") }
            }
        }
    }

    @Composable
    private fun GeofenceMapPreview(dao: WorkTimeDao, settings: SettingsEntity, radiusMeters: Float, onMessage: (String) -> Unit) {
        val lat = settings.geofenceLatitude
        val lon = settings.geofenceLongitude
        if (lat == null || lon == null) {
            Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MatrixSurface
    )
) { Column(modifier = Modifier.padding(12.dp)) { Text("Noch keine Karte verfuegbar.",color = ComposeColor.White); Text("Bitte zuerst aktuelle Position speichern oder per Karte setzen.",color = ComposeColor.White) } }
            return
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            factory = { ctx -> Configuration.getInstance().userAgentValue = ctx.packageName; MapView(ctx).apply { setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true); setBuiltInZoomControls(false); minZoomLevel = 3.0; maxZoomLevel = 20.0 } },
            update = { map -> updateGeofenceMap(map, dao, settings, lat, lon, radiusMeters, onMessage) }
        )
    }

    private fun updateGeofenceMap(map: MapView, dao: WorkTimeDao, settings: SettingsEntity, lat: Double, lon: Double, radius: Float, onMessage: (String) -> Unit) {
        val center = GeoPoint(lat, lon)
        map.controller.setZoom(18.0)
        map.controller.setCenter(center)
        map.overlays.clear()
        map.overlays.add(Polygon(map).apply { points = Polygon.pointsAsCircle(center, radius.toDouble()); fillColor = 0x3333B5E5; strokeColor = 0xFF0288D1.toInt(); strokeWidth = 4f })
        map.overlays.add(Marker(map).apply { position = center; title = "Arbeitsplatz"; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) })
        map.overlays.add(LongPressOverlay { point -> saveMarkerFromMap(dao, settings, point, onMessage) })
        map.invalidate()
    }

    private fun saveRadius(dao: WorkTimeDao, settings: SettingsEntity, radiusText: String, onMessage: (String) -> Unit) {
        val radius = radiusText.toIntOrNull()
        if (radius == null || radius !in 25..1000) { onMessage("Radius muss zwischen 25 und 1000 Metern liegen"); return }
        CoroutineScope(Dispatchers.IO).launch { dao.upsertSettings(settings.copy(geofenceRadiusMeters = radius.toFloat())); runOnUiThread { onMessage("Radius gespeichert") } }
    }

    private fun saveMarkerFromMap(dao: WorkTimeDao, settings: SettingsEntity, point: GeoPoint, onMessage: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch { dao.upsertSettings(settings.copy(geofenceLatitude = point.latitude, geofenceLongitude = point.longitude)); writeGeofenceStatus(false, null); runOnUiThread { onMessage("Arbeitsplatz-Marker aus Karte gespeichert. Geofence bitte erneut registrieren.") } }
    }

    @SuppressLint("MissingPermission")
    private fun saveCurrentLocationAsWorkplace(client: FusedLocationProviderClient, dao: WorkTimeDao, settings: SettingsEntity, radiusText: String, onMessage: (String) -> Unit) {
        val radius = radiusText.toIntOrNull()
        if (radius == null || radius !in 25..1000) { onMessage("Radius muss zwischen 25 und 1000 Metern liegen"); return }
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) { onMessage("Keine letzte Position verfuegbar. Standort kurz aktivieren und erneut versuchen."); return@addOnSuccessListener }
            CoroutineScope(Dispatchers.IO).launch { dao.upsertSettings(settings.copy(geofenceLatitude = loc.latitude, geofenceLongitude = loc.longitude, geofenceRadiusMeters = radius.toFloat())); writeGeofenceStatus(false, null); runOnUiThread { onMessage("Arbeitsplatz-Koordinate gespeichert. Geofence bitte erneut registrieren.") } }
        }.addOnFailureListener { onMessage("Position konnte nicht gelesen werden: ${it.message}") }
    }

    private fun buildDailySummaries(entries: List<WorkTimeEntryEntity>, view: TimeView, settings: SettingsEntity, workdaySettings: WorkdaySettings, dayOverrides: List<DayOverride>): List<DailySummary> {
        val zoneId = ZoneId.systemDefault()
        val overrideMap = dayOverrides.associateBy { it.localDate }
        val today = LocalDate.now()
        val periodDates = periodDates(today, view)
        val groupedEntries = entries.groupBy { Instant.ofEpochMilli(it.startEpochMillis).atZone(zoneId).toLocalDate() }
        val allDates = (periodDates + groupedEntries.keys).filter { isDateInView(it, today, view) }.distinct().sortedDescending()
        return allDates.mapNotNull { date ->
            val dayEntries = groupedEntries[date].orEmpty().sortedBy { it.startEpochMillis }
            val override = overrideMap[date.toString()]
            val target = targetForDate(settings.targetMinutesPerDay, workdaySettings, override, date)
            val showDay = dayEntries.isNotEmpty() || override != null
            if (!showDay) return@mapNotNull null
            val gross = dayEntries.sumOf { ((it.endEpochMillis - it.startEpochMillis) / 60000L).toInt().coerceAtLeast(0) }
            val pause = dayEntries.sumOf { entry -> entry.breakMinutes.coerceIn(0, ((entry.endEpochMillis - entry.startEpochMillis) / 60000L).toInt().coerceAtLeast(0)) }
            val net = gross - pause
            DailySummary(date, override, dayEntries, gross, pause, net, target, net - target)
        }
    }

    private fun periodDates(referenceDate: LocalDate, view: TimeView): List<LocalDate> {
        val weekFields = WeekFields.ISO
        return when (view) {
            TimeView.Day -> listOf(referenceDate)
            TimeView.Week -> { val start = referenceDate.with(weekFields.dayOfWeek(), 1); (0..6).map { start.plusDays(it.toLong()) } }
            TimeView.Month -> (1..referenceDate.lengthOfMonth()).map { referenceDate.withDayOfMonth(it) }
            TimeView.Year -> (1..referenceDate.lengthOfYear()).map { referenceDate.withDayOfYear(it) }
        }
    }

    private fun isDateInView(date: LocalDate, referenceDate: LocalDate, view: TimeView): Boolean {
        val weekFields = WeekFields.ISO
        return when (view) {
            TimeView.Day -> date == referenceDate
            TimeView.Week -> date.get(weekFields.weekOfWeekBasedYear()) == referenceDate.get(weekFields.weekOfWeekBasedYear()) && date.get(weekFields.weekBasedYear()) == referenceDate.get(weekFields.weekBasedYear())
            TimeView.Month -> date.year == referenceDate.year && date.month == referenceDate.month
            TimeView.Year -> date.year == referenceDate.year
        }
    }

    private fun targetForDate(targetMinutesPerDay: Int, workdays: WorkdaySettings, override: DayOverride?, date: LocalDate): Int {
        if (override != null) return 0
        return if (isRegularWorkday(workdays, date.dayOfWeek)) targetMinutesPerDay else 0
    }

    private fun isRegularWorkday(workdays: WorkdaySettings, dayOfWeek: DayOfWeek): Boolean = when (dayOfWeek) {
        DayOfWeek.MONDAY -> workdays.monday
        DayOfWeek.TUESDAY -> workdays.tuesday
        DayOfWeek.WEDNESDAY -> workdays.wednesday
        DayOfWeek.THURSDAY -> workdays.thursday
        DayOfWeek.FRIDAY -> workdays.friday
        DayOfWeek.SATURDAY -> workdays.saturday
        DayOfWeek.SUNDAY -> workdays.sunday
    }

    private fun calculatePeriodSummary(days: List<DailySummary>): PeriodSummary = PeriodSummary(
        dayCount = days.size,
        regularWorkdayCount = days.count { it.targetMinutes > 0 },
        overrideCount = days.count { it.dayOverride != null },
        grossMinutes = days.sumOf { it.grossMinutes },
        breakMinutes = days.sumOf { it.breakMinutes },
        netMinutes = days.sumOf { it.netMinutes },
        targetMinutes = days.sumOf { it.targetMinutes },
        balanceMinutes = days.sumOf { it.balanceMinutes }
    )

    private fun readGeofenceRegistered(): Boolean = getSharedPreferences("geofence_state", MODE_PRIVATE).getBoolean("registered", false)
    private fun readGeofenceRegisteredAt(): Long? = getSharedPreferences("geofence_state", MODE_PRIVATE).getLong("registeredAt", 0L).takeIf { it > 0L }
    private fun writeGeofenceStatus(registered: Boolean, registeredAt: Long?) { getSharedPreferences("geofence_state", MODE_PRIVATE).edit().putBoolean("registered", registered).putLong("registeredAt", registeredAt ?: 0L).apply() }

    private fun readWorkdaySettings(): WorkdaySettings {
        val prefs = getSharedPreferences("planning_state", MODE_PRIVATE)
        return WorkdaySettings(
            monday = prefs.getBoolean("workMonday", true),
            tuesday = prefs.getBoolean("workTuesday", true),
            wednesday = prefs.getBoolean("workWednesday", true),
            thursday = prefs.getBoolean("workThursday", true),
            friday = prefs.getBoolean("workFriday", true),
            saturday = prefs.getBoolean("workSaturday", false),
            sunday = prefs.getBoolean("workSunday", false)
        )
    }

    private fun writeWorkdaySettings(settings: WorkdaySettings) {
        getSharedPreferences("planning_state", MODE_PRIVATE).edit()
            .putBoolean("workMonday", settings.monday)
            .putBoolean("workTuesday", settings.tuesday)
            .putBoolean("workWednesday", settings.wednesday)
            .putBoolean("workThursday", settings.thursday)
            .putBoolean("workFriday", settings.friday)
            .putBoolean("workSaturday", settings.saturday)
            .putBoolean("workSunday", settings.sunday)
            .apply()
    }

    private fun readDayOverrides(): List<DayOverride> {
        val raw = getSharedPreferences("planning_state", MODE_PRIVATE).getStringSet("dayOverrides", emptySet()).orEmpty()
        return raw.mapNotNull { value ->
            val parts = value.split("|", limit = 3)
            if (parts.size >= 2) DayOverride(parts[0], parts[1], parts.getOrElse(2) { "" }) else null
        }.sortedByDescending { it.localDate }
    }

    private fun writeDayOverrides(dayOverrides: List<DayOverride>) {
        val raw = dayOverrides.map { listOf(it.localDate, it.type, it.comment).joinToString("|") }.toSet()
        getSharedPreferences("planning_state", MODE_PRIVATE).edit().putStringSet("dayOverrides", raw).apply()
    }

    private fun clearLocalPlanningSettings() {
        getSharedPreferences("planning_state", MODE_PRIVATE).edit().clear().apply()
        writeGeofenceStatus(false, null)
    }

    private fun formatEpoch(epochMillis: Long, zoneId: ZoneId): String = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime().format(entryDateTimeFormatter)
    private fun formatTime(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime().toString()
    private fun parseDateTime(value: String, zoneId: ZoneId): Instant? = runCatching { LocalDateTime.parse(value, entryDateTimeFormatter).atZone(zoneId).toInstant() }.getOrNull()
    private fun weekdayLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) { DayOfWeek.MONDAY -> "Mo"; DayOfWeek.TUESDAY -> "Di"; DayOfWeek.WEDNESDAY -> "Mi"; DayOfWeek.THURSDAY -> "Do"; DayOfWeek.FRIDAY -> "Fr"; DayOfWeek.SATURDAY -> "Sa"; DayOfWeek.SUNDAY -> "So" }
    private fun formatMinutes(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"
    private fun formatSignedMinutes(minutes: Int): String { val sign = if (minutes >= 0) "+" else "-"; val abs = kotlin.math.abs(minutes); return "${sign}${abs / 60}h ${abs % 60}m" }

    private class LongPressOverlay(private val onLongPressGeoPoint: (GeoPoint) -> Unit) : Overlay() {
        override fun onLongPress(event: MotionEvent, mapView: MapView): Boolean {
            val point = mapView.projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
            onLongPressGeoPoint(point)
            return true
        }
    }
}


