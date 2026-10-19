package de.kai.arbeitszeitgeofence.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
        label = { Text(text = label) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MatrixText,
            unfocusedTextColor = MatrixText,
            focusedLabelColor = MatrixGreen,
            unfocusedLabelColor = MatrixMutedText,
            focusedBorderColor = MatrixGreen,
            unfocusedBorderColor = MatrixOutline,
            cursorColor = MatrixGreen,
            focusedContainerColor = MatrixInput,
            unfocusedContainerColor = MatrixInput
        )
    )
}
