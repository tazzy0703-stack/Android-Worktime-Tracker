package de.kai.arbeitszeitgeofence.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun HeroStatusCard(
    isTracking: Boolean,
    pauseMinutes: Int,
    insideGeofence: Boolean
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MatrixSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                if (isTracking)
                    "🟢 ARBEITSZEIT AKTIV"
                else
                    "⚫ GESTOPPT",
                style = MaterialTheme.typography.titleLarge,
                color = MatrixGreen
            )

            Text(
                "Pause: $pauseMinutes Minuten",
                color = MatrixText
            )

            Text(
                if (insideGeofence)
                    "📍 Arbeitsplatz erkannt"
                else
                    "📍 Außerhalb Geofence",
                color = MatrixText
            )
        }
    }
}

@Composable
fun KpiGrid() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        KpiCard("Heute", "07:42")
        KpiCard("Woche", "38:21")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        KpiCard("Monat", "154:33")
        KpiCard("Saldo", "+08:15", MatrixGreen)
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = MatrixText
) {

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MatrixSurface
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(title,color = MatrixText)

            Text(
                value,
                color = color
            )
        }
    }
}

@Composable
fun MonthProgressCard() {

    val today = LocalDate.now()

    val progress =
        today.dayOfMonth.toFloat() /
        today.lengthOfMonth().toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MatrixSurface
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                "📅 Monatsfortschritt",
                color = MatrixOrange
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "${(progress * 100).toInt()} %",
                color = MatrixText
            )
        }
    }
}