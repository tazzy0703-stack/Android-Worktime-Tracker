package de.kai.arbeitszeitgeofence.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.time.LocalDate

@Composable
fun HeroStatusCard(
    isTracking: Boolean,
    pauseMinutes: Int,
    insideGeofence: Boolean
) {

    MatrixCard {

        MatrixHeader(
            if (isTracking)
                "🟢 ARBEITSZEIT AKTIV"
            else
                "⚫ GESTOPPT"
        )

        MatrixText(
            "Pause: $pauseMinutes Minuten"
        )

        MatrixText(
            if (insideGeofence)
                "📍 Arbeitsplatz erkannt"
            else
                "📍 Außerhalb Geofence"
        )
    }
}

@Composable
fun KpiGrid() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        KpiCard(
            title = "Heute",
            value = "07:42"
        )

        KpiCard(
            title = "Woche",
            value = "38:21"
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        KpiCard(
            title = "Monat",
            value = "154:33"
        )

        KpiCard(
            title = "Saldo",
            value = "+08:15",
            color = MatrixGreen
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = MatrixText
) {

    MatrixCard {

        MatrixHeader(title)

        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun MonthProgressCard() {

    val today = LocalDate.now()

    val progress =
        today.dayOfMonth.toFloat() /
                today.lengthOfMonth().toFloat()

    MatrixCard {

        MatrixHeader("📅 Monatsfortschritt")

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )

        MatrixText(
            "${(progress * 100).toInt()} %"
        )
    }
}