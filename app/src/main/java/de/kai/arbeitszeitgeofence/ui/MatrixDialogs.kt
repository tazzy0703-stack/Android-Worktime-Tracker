package de.kai.arbeitszeitgeofence.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun MatrixDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                color = MatrixGreen
            )
        },
        text = {
            Text(
                text,
                color = MatrixText
            )
        },
        confirmButton = confirmButton
    )
}