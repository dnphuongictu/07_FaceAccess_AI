package com.faceaccess.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = FaceAccessPrimary,
    onPrimary = FaceAccessOnPrimary,
    background = FaceAccessBackground,
    onBackground = FaceAccessOnSurface,
    surface = FaceAccessSurface,
    onSurface = FaceAccessOnSurface,
)

private val LightColors = lightColorScheme(
    primary = FaceAccessPrimary,
    onPrimary = FaceAccessOnPrimary,
)

@Composable
fun FaceAccessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FaceAccessTypography,
        content = content,
    )
}
