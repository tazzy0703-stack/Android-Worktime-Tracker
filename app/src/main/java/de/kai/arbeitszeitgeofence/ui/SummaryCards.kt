package de.kai.arbeitszeitgeofence.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SummaryCard(
    title: String,
    netTime: String,
    pauseTime: String,
    balance: String,
    bookings: Int
) {

    val balanceColor =
        if (balance.startsWith("-"))
            MatrixRed
        else
            MatrixGreen

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
                title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Text(
                "Netto: $netTime",
                color = Color.White
            )

            Text(
                "Pause: $pauseTime",
                color = Color.White
            )

            Text(
                "Saldo: $balance",
                color = balanceColor
            )

            Text(
                "$bookings Buchungen",
                color = Color.LightGray
            )
        }
    }
}