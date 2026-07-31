package de.kai.arbeitszeitgeofence.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

    MatrixCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            MatrixHeader(title)

            MatrixText(
                text = "Netto: $netTime"
            )

            MatrixText(
                text = "Pause: $pauseTime"
            )

            Text(
                text = "Saldo: $balance",
                color = balanceColor,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "$bookings Buchungen",
                color = MatrixOrange
            )
        }
    }
}