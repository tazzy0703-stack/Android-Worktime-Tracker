package de.kai.arbeitszeitgeofence.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MatrixBackground = Color(0xFF0B0F0C)
val MatrixSurface = Color(0xFF141C18)
val MatrixSurfaceRaised = Color(0xFF1B2721)
val MatrixGreen = Color(0xFF7AE582)
val MatrixGreenDark = Color(0xFF3CBF70)
val MatrixRed = Color(0xFFFF6B6B)
val MatrixBlue = Color(0xFF7CC7FF)
val MatrixOrange = Color(0xFFFFC46B)
val MatrixText = Color(0xFFF1F5F2)
val MatrixMutedText = Color(0xFFB8C5BC)
val MatrixInput = Color(0xFF18231D)
val MatrixButton = Color(0xFF20352A)
val MatrixDialog = Color(0xFF141C18)
val MatrixOutline = Color(0xFF3A5144)

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
