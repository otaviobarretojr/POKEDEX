package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun CompanionContextHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    progress: Pair<Int, Int>? = null,
    artwork: (@Composable BoxScope.() -> Unit)? = null
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = PokedexDesignTokens.Companion.ContextualBackdropAlpha), Color.Transparent)
                ),
                RoundedCornerShape(PokedexDesignTokens.Companion.HeroRadius)
            )
            .padding(PokedexDesignTokens.Spacing.Xl)
    ) {
        artwork?.invoke(this)
        Column(Modifier.fillMaxWidth(0.72f)) {
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
            Spacer(Modifier.height(PokedexDesignTokens.Spacing.Sm))
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            subtitle?.let {
                Spacer(Modifier.height(PokedexDesignTokens.Spacing.Sm))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            progress?.let {
                Spacer(Modifier.height(PokedexDesignTokens.Spacing.Lg))
                CompanionProgress(it.first, it.second, accent = accent)
            }
        }
    }
}

@Composable
internal fun PokemonHeroHeader(
    number: String,
    name: String,
    types: List<String>,
    modifier: Modifier = Modifier,
    status: String? = null,
    artwork: (@Composable BoxScope.() -> Unit)? = null
) {
    val accent = PokedexDesignTokens.Colors.type(types.firstOrNull())
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(accent.copy(alpha = .18f), Color.Transparent)),
                RoundedCornerShape(PokedexDesignTokens.Companion.HeroRadius)
            )
            .padding(PokedexDesignTokens.Spacing.Xl)
    ) {
        artwork?.invoke(this)
        Column(Modifier.fillMaxWidth(0.68f)) {
            Text(number, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(PokedexDesignTokens.Spacing.Md))
            Row(horizontalArrangement = Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)) {
                types.take(2).forEach { type ->
                    Surface(shape = RoundedCornerShape(PokedexDesignTokens.Radius.Pill), color = PokedexDesignTokens.Colors.type(type).copy(alpha = .16f)) {
                        Text(type, Modifier.padding(horizontal = PokedexDesignTokens.Spacing.Md, vertical = PokedexDesignTokens.Spacing.Sm), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            status?.let {
                Spacer(Modifier.height(PokedexDesignTokens.Spacing.Md))
                Text(it, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
