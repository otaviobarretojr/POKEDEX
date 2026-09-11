package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

private val PokedexShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val PokedexLightColors = lightColorScheme(
    primary = Color(0xFF5B55E7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE8FB),
    onPrimaryContainer = Color(0xFF241F66),
    secondary = Color(0xFF263C8C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6EAFA),
    onSecondaryContainer = Color(0xFF18224F),
    tertiary = Color(0xFF159D9B),
    onTertiary = Color.White,
    background = Color(0xFFF8F8FC),
    onBackground = Color(0xFF151426),
    surface = Color.White,
    onSurface = Color(0xFF151426),
    surfaceVariant = Color(0xFFF1F0F8),
    onSurfaceVariant = Color(0xFF50566A),
    outline = Color(0xFF797A8D),
    outlineVariant = Color(0xFFD8D7E2),
    error = Color(0xFFB3261E)
)

@Composable
fun PokedexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PokedexLightColors,
        shapes = PokedexShapes,
        content = content
    )
}
