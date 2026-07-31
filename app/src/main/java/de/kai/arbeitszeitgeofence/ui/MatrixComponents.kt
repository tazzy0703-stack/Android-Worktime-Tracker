package de.kai.arbeitszeitgeofence.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MatrixCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MatrixSurface
        ),
        border = BorderStroke(
            1.dp,
            MatrixGreenDark
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            content()
        }
    }
}

@Composable
fun MatrixButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MatrixGreen,
            contentColor = Color.Black
        ),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 10.dp
        )
    ) {
        Text(text)
    }
}

@Composable
fun MatrixHeader(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MatrixGreen
    )
}

@Composable
fun MatrixText(
    text: String
) {
    Text(
        text = text,
        color = MatrixText
    )
}