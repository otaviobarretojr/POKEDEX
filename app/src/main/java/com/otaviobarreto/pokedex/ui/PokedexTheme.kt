package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PokedexLightColors = lightColorScheme(
    primary=PokedexDesignTokens.Colors.Primary,
    onPrimary=PokedexDesignTokens.Colors.OnPrimary,
    primaryContainer=PokedexDesignTokens.Colors.PrimaryContainer,
    onPrimaryContainer=PokedexDesignTokens.Colors.OnPrimaryContainer,
    secondary=PokedexDesignTokens.Colors.Secondary,
    onSecondary=PokedexDesignTokens.Colors.OnSecondary,
    secondaryContainer=PokedexDesignTokens.Colors.SecondaryContainer,
    onSecondaryContainer=PokedexDesignTokens.Colors.OnSecondaryContainer,
    tertiary=PokedexDesignTokens.Colors.Tertiary,
    onTertiary=PokedexDesignTokens.Colors.OnTertiary,
    background=PokedexDesignTokens.Colors.Background,
    onBackground=PokedexDesignTokens.Colors.OnBackground,
    surface=PokedexDesignTokens.Colors.Surface,
    onSurface=PokedexDesignTokens.Colors.OnSurface,
    surfaceVariant=PokedexDesignTokens.Colors.SurfaceVariant,
    onSurfaceVariant=PokedexDesignTokens.Colors.OnSurfaceVariant,
    outline=PokedexDesignTokens.Colors.Outline,
    outlineVariant=PokedexDesignTokens.Colors.OutlineVariant,
    error=PokedexDesignTokens.Colors.Error
)

private val PokedexDarkColors = darkColorScheme(
    primary=PokedexDesignTokens.Colors.DarkPrimary,
    onPrimary=PokedexDesignTokens.Colors.OnPrimaryContainer,
    primaryContainer=PokedexDesignTokens.Colors.DarkPrimaryContainer,
    onPrimaryContainer=PokedexDesignTokens.Colors.DarkOnSurface,
    secondary=ColorTokens.darkSecondary,
    secondaryContainer=ColorTokens.darkSecondaryContainer,
    tertiary=ColorTokens.darkTertiary,
    background=PokedexDesignTokens.Colors.DarkBackground,
    onBackground=PokedexDesignTokens.Colors.DarkOnSurface,
    surface=PokedexDesignTokens.Colors.DarkSurface,
    onSurface=PokedexDesignTokens.Colors.DarkOnSurface,
    surfaceVariant=PokedexDesignTokens.Colors.DarkSurfaceVariant,
    onSurfaceVariant=PokedexDesignTokens.Colors.DarkOnSurfaceVariant,
    outline=PokedexDesignTokens.Colors.DarkOutline,
    outlineVariant=PokedexDesignTokens.Colors.DarkOutlineVariant
)

private object ColorTokens {
    val darkSecondary=androidx.compose.ui.graphics.Color(0xFFAFC6FF)
    val darkSecondaryContainer=androidx.compose.ui.graphics.Color(0xFF24345B)
    val darkTertiary=androidx.compose.ui.graphics.Color(0xFF6FD8CE)
}

@Composable
fun PokedexTheme(content: @Composable () -> Unit){
    MaterialTheme(
        colorScheme=if(isSystemInDarkTheme()) PokedexDarkColors else PokedexLightColors,
        typography=PokedexDesignTokens.AppTypography,
        shapes=PokedexDesignTokens.Shapes,
        content=content
    )
}
