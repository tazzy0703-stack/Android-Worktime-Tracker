#requires -Version 7.0
$ErrorActionPreference = 'Stop'

$ProjectRoot = Get-Location
$MainActivityPath = Join-Path $ProjectRoot 'app\src\main\java\de\kai\arbeitszeitgeofence\MainActivity.kt'
$BuildGradlePath = Join-Path $ProjectRoot 'app\build.gradle.kts'

if (-not (Test-Path $MainActivityPath)) {
    throw "MainActivity.kt nicht gefunden. Bitte im Repository-Root ausfuehren."
}
if (-not (Test-Path $BuildGradlePath)) {
    throw "app/build.gradle.kts nicht gefunden. Bitte im Repository-Root ausfuehren."
}

$Content = Get-Content -Path $MainActivityPath -Raw

if ($Content -notmatch 'import androidx\.compose\.foundation\.layout\.statusBarsPadding') {
    $Content = $Content -replace 'import androidx\.compose\.foundation\.layout\.padding', "import androidx.compose.foundation.layout.padding`nimport androidx.compose.foundation.layout.statusBarsPadding`nimport androidx.compose.foundation.layout.navigationBarsPadding"
}

$OldModifier = @'
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
'@

$NewModifier = @'
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(16.dp),
'@

if ($Content.Contains($OldModifier)) {
    $Content = $Content.Replace($OldModifier, $NewModifier)
}
else {
    Write-Warning "Der erwartete Modifier-Block wurde nicht exakt gefunden. Versuche regex-basierten Patch."
    $Content = $Content -replace '\.fillMaxSize\(\)\s*\.padding\(16\.dp\)', ".fillMaxSize()`n                        .statusBarsPadding()`n                        .navigationBarsPadding()`n                        .padding(16.dp)"
}

Set-Content -Path $MainActivityPath -Value $Content -Encoding UTF8

$BuildGradle = Get-Content -Path $BuildGradlePath -Raw
$BuildGradle = $BuildGradle -replace 'versionCode = \d+', 'versionCode = 6'
$BuildGradle = $BuildGradle -replace 'versionName = "[^"]+"', 'versionName = "0.3.2"'
Set-Content -Path $BuildGradlePath -Value $BuildGradle -Encoding UTF8

Write-Host "v0.3.2 Systemleisten-Patch wurde angewendet."
Write-Host "Pruefung: Select-String -Path .\app\src\main\java\de\kai\arbeitszeitgeofence\MainActivity.kt -Pattern 'statusBarsPadding|navigationBarsPadding'"
Write-Host "Danach: git add ., git commit, git push."
