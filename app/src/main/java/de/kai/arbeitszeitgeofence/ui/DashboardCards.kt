package de.kai.arbeitszeitgeofence.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HeroStatusCard(
    isTracking: Boolean,
    pauseMinutes: Int,
    insideGeofence: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text =
                if (isTracking)
                    "ARBEITSZEIT AKTIV"
                else
                    "GESTOPPT",
                style = MaterialTheme.typography.titleLarge
            )

            Text("Pause: $pauseMinutes Minuten")

            Text(
                if (insideGeofence)
                    "Arbeitsplatz erkannt"
                else
                    "Ausserhalb Geofence"
            )
        }
    }
}