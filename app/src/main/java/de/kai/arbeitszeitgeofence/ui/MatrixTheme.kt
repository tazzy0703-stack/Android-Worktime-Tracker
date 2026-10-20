package de.kai.arbeitszeitgeofence.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MatrixBackground = Color(0xFF0D1B14)
val MatrixSurface = Color(0xFF15271D)
val MatrixSurfaceRaised = Color(0xFF1D3427)
val MatrixGreen = Color(0xFF8ADBA0)
val MatrixGreenDark = Color(0xFF5ABF7A)
val MatrixRed = Color(0xFFFF6B6B)
val MatrixBlue = Color(0xFF7CC7FF)
val MatrixOrange = Color(0xFFFFC46B)
val MatrixText = Color(0xFFF1F5F2)
val MatrixMutedText = Color(0xFFB8C5BC)
val MatrixInput = Color(0xFF18231D)
val MatrixButton = Color(0xFF1C3526)
val MatrixDialog = Color(0xFF15271D)
val MatrixOutline = Color(0xFF385B44)

private val MatrixColorScheme = darkColorScheme(
    primary = MatrixGreen,
    onPrimary = MatrixBackground,
    primaryContainer = MatrixButton,
    onPrimaryContainer = MatrixText,
    secondary = MatrixBlue,
    onSecondary = MatrixBackground,
    tertiary = MatrixOrange,
    onTertiary = MatrixBackground,
    background = MatrixBackground,
    onBackground = MatrixText,
    surface = MatrixSurface,
    onSurface = MatrixText,
    surfaceVariant = MatrixSurfaceRaised,
    onSurfaceVariant = MatrixMutedText,
    outline = MatrixOutline,
    error = MatrixRed,
    onError = MatrixBackground
)

@Composable
fun MatrixTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MatrixColorScheme, content = content)
}
