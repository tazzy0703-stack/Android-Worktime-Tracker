package de.kai.arbeitszeitgeofence.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                text =
                if (isTracking)
                    "🟢 ARBEITSZEIT AKTIV"
                else
                    "⚫ GESTOPPT",
                style = MaterialTheme.typography.titleLarge,
                color = MatrixGreen
            )

            Text(
                text = "Pause: $pauseMinutes Minuten",
                color = Color.White
            )

            Text(
                text =
                if (insideGeofence)
                    "📍 Arbeitsplatz erkannt"
                else
                    "📍 Außerhalb Geofence",
                color = Color.White
            )
        }
    }
}

@Composable
fun GeofenceStatusCard(
    radius: Int,
    active: Boolean
) {
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
                "📍 Arbeitsplatz",
                color = MatrixBlue
            )

            Text(
                if (active)
                    "Geofence aktiv"
                else
                    "Geofence nicht aktiv"
            )

            Text(
                "Radius: ${radius} m"
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
                "${(progress * 100).toInt()} %"
            )
        }
    }
}