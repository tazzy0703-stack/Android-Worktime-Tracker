$mainActivity = ".\app\src\main\java\de\kai\arbeitszeitgeofence\MainActivity.kt"

$content = Get-Content $mainActivity -Raw

# Benötigte Imports

if ($content -notmatch "ButtonDefaults") {
    $content = $content.Replace(
        "import androidx.compose.material3.Button",
        "import androidx.compose.material3.Button`r`nimport androidx.compose.material3.ButtonDefaults"
    )
}

if ($content -notmatch "background") {
    $content = $content.Replace(
        "import androidx.compose.foundation.layout.Arrangement",
        "import androidx.compose.foundation.background`r`nimport androidx.compose.foundation.layout.Arrangement"
    )
}

# Hintergrund dunkel

$content = $content.Replace(
    "modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)",
    "modifier = Modifier.fillMaxSize().background(MatrixBackground).statusBarsPadding().navigationBarsPadding().padding(16.dp)"
)

# Status-Texte weiß

$content = $content.Replace(
    'Text("Status: ${if (state.isTracking) "Arbeitszeit laeuft" else "Gestoppt"} / Geofence: ${if (state.insideGeofence) "Innen" else "Aussen"}")',
    'Text("Status: ${if (state.isTracking) "Arbeitszeit laeuft" else "Gestoppt"} / Geofence: ${if (state.insideGeofence) "Innen" else "Aussen"}", color = ComposeColor.White)'
)

$content = $content.Replace(
    'Text("Pause: ${state.accumulatedBreakMinutes} min${if (state.isBreakRunning) " + laufend" else ""}")',
    'Text("Pause: ${state.accumulatedBreakMinutes} min${if (state.isBreakRunning) " + laufend" else ""}", color = ComposeColor.White)'
)

$content = $content.Replace(
    'Text(message)',
    'Text(message, color = ComposeColor.White)'
)

# Hauptbuttons grün

$content = $content.Replace(
    'Button(',
    'Button(colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen),'
)

Set-Content $mainActivity $content -Encoding UTF8

Write-Host ""
Write-Host "================================="
Write-Host " Matrix Final Applied"
Write-Host "================================="
Write-Host ""
