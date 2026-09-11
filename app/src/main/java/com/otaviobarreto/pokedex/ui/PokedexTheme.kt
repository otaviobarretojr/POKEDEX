package com.otaviobarreto.pokedex.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PokedexLightColors = lightColorScheme(
    primary = PokedexDesignTokens.Colors.Primary,
    onPrimary = PokedexDesignTokens.Colors.OnPrimary,
    primaryContainer = PokedexDesignTokens.Colors.PrimaryContainer,
    onPrimaryContainer = PokedexDesignTokens.Colors.OnPrimaryContainer,
    secondary = PokedexDesignTokens.Colors.Secondary,
    onSecondary = PokedexDesignTokens.Colors.OnSecondary,
    secondaryContainer = PokedexDesignTokens.Colors.SecondaryContainer,
    onSecondaryContainer = PokedexDesignTokens.Colors.OnSecondaryContainer,
    tertiary = PokedexDesignTokens.Colors.Tertiary,
    onTertiary = PokedexDesignTokens.Colors.OnTertiary,
    background = PokedexDesignTokens.Colors.Background,
    onBackground = PokedexDesignTokens.Colors.OnBackground,
    surface = PokedexDesignTokens.Colors.Surface,
    onSurface = PokedexDesignTokens.Colors.OnSurface,
    surfaceVariant = PokedexDesignTokens.Colors.SurfaceVariant,
    onSurfaceVariant = PokedexDesignTokens.Colors.OnSurfaceVariant,
    outline = PokedexDesignTokens.Colors.Outline,
    outlineVariant = PokedexDesignTokens.Colors.OutlineVariant,
    error = PokedexDesignTokens.Colors.Error
)

@Composable
fun PokedexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PokedexLightColors,
        typography = PokedexDesignTokens.Typography,
        shapes = PokedexDesignTokens.Shapes,
        content = content
    )
}
