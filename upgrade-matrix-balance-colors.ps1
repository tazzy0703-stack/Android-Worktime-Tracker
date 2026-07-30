$File = ".\app\src\main\java\de\kai\arbeitszeitgeofence\MainActivity.kt"

$Content = Get-Content $File -Raw

# Imports ergänzen
if ($Content -notmatch "MatrixGreen") {

    $ImportBlock = @"

import androidx.compose.ui.graphics.Color
import de.kai.arbeitszeitgeofence.ui.MatrixGreen
import de.kai.arbeitszeitgeofence.ui.MatrixRed

"@

    $Content = $Content.Replace(
        "import androidx.compose.ui.unit.dp",
        "import androidx.compose.ui.unit.dp$ImportBlock"
    )
}

# DailySummaryCard Saldo ersetzen
$OldDaily = 'Text("Soll: ${formatMinutes(summary.targetMinutes)} | Ueberstunden: ${formatSignedMinutes(summary.balanceMinutes)}")'

$NewDaily = @'
Text(
    "Soll: ${formatMinutes(summary.targetMinutes)}"
)

Text(
    text = "Saldo: ${formatSignedMinutes(summary.balanceMinutes)}",
    color = when {
        summary.balanceMinutes > 0 -> MatrixGreen
        summary.balanceMinutes < 0 -> MatrixRed
        else -> Color.Gray
    }
)
'@

$Content = $Content.Replace($OldDaily,$NewDaily)

# PeriodSummaryCard Saldo ersetzen
$OldPeriod = 'Text("Saldo: ${formatSignedMinutes(summary.balanceMinutes)}")'

$NewPeriod = @'
Text(
    text = "Saldo: ${formatSignedMinutes(summary.balanceMinutes)}",
    color = when {
        summary.balanceMinutes > 0 -> MatrixGreen
        summary.balanceMinutes < 0 -> MatrixRed
        else -> Color.Gray
    }
)
'@

$Content = $Content.Replace($OldPeriod,$NewPeriod)

Set-Content $File $Content -Encoding UTF8

Write-Host ""
Write-Host "Matrix Salden eingebaut." -ForegroundColor Green
Write-Host ""