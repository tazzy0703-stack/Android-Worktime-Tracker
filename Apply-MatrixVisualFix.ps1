$mainActivity = ".\app\src\main\java\de\kai\arbeitszeitgeofence\MainActivity.kt"

$content = Get-Content $mainActivity -Raw

# Karten-Text lesbar machen

$content = $content.Replace(
'Text("Arbeitszeit: ${formatMinutes(summary.netMinutes)} | Pause: ${formatMinutes(summary.breakMinutes)}")',
'Text("Arbeitszeit: ${formatMinutes(summary.netMinutes)} | Pause: ${formatMinutes(summary.breakMinutes)}", color = ComposeColor.White)'
)

$content = $content.Replace(
'Text("Bloecke: ${summary.entries.size}")',
'Text("Bloecke: ${summary.entries.size}", color = ComposeColor.White)'
)

$content = $content.Replace(
'Text("${weekdayLabel(summary.date.dayOfWeek)}, ${summary.date}")',
'Text("${weekdayLabel(summary.date.dayOfWeek)}, ${summary.date}", color = ComposeColor.White)'
)

# Period Summary

$content = $content.Replace(
'Text("Brutto: ${formatMinutes(summary.grossMinutes)}")',
'Text("Brutto: ${formatMinutes(summary.grossMinutes)}", color = ComposeColor.White)'
)

$content = $content.Replace(
'Text("Pause: ${formatMinutes(summary.breakMinutes)}")',
'Text("Pause: ${formatMinutes(summary.breakMinutes)}", color = ComposeColor.White)'
)

$content = $content.Replace(
'Text("Netto: ${formatMinutes(summary.netMinutes)}")',
'Text("Netto: ${formatMinutes(summary.netMinutes)}", color = ComposeColor.White)'
)

$content = $content.Replace(
'Text("Soll: ${formatMinutes(summary.targetMinutes)}")',
'Text("Soll: ${formatMinutes(summary.targetMinutes)}", color = ComposeColor.White)'
)

# Geofence Koordinate

$content = $content.Replace(
'Text("Koordinate: ${settings.geofenceLatitude ?: "nicht gesetzt"}, ${settings.geofenceLongitude ?: "nicht gesetzt"}")',
'Text("Koordinate: ${settings.geofenceLatitude ?: "nicht gesetzt"}, ${settings.geofenceLongitude ?: "nicht gesetzt"}", color = ComposeColor.White)'
)


Set-Content $mainActivity $content -Encoding UTF8

Write-Host "Matrix Visual Fix erfolgreich angewendet"
