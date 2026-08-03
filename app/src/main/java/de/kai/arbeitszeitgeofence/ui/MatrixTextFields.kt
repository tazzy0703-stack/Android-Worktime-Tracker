package de.kai.arbeitszeitgeofence.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun MatrixTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = label,
                color = Color.White
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,

            focusedLabelColor = MatrixGreen,
            unfocusedLabelColor = Color.White,

            focusedBorderColor = MatrixGreen,
            unfocusedBorderColor = MatrixGreen,

            focusedContainerColor = MatrixSurface,
            unfocusedContainerColor = MatrixSurface
        )
    )
}