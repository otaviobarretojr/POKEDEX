package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Stateless Nintendo Companion visual primitives.
 * Callers own data, IO, artwork resolution and navigation.
 */
@Composable
fun CompanionSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            supporting?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun CompanionProgress(
    current: Int,
    total: Int,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val safeTotal = total.coerceAtLeast(0)
    val safeCurrent = if (safeTotal == 0) 0 else current.coerceIn(0, safeTotal)
    val progress = if (safeTotal == 0) 0f else safeCurrent.toFloat() / safeTotal
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text("$safeCurrent / $safeTotal", style = MaterialTheme.typography.labelLarge, color = accent)
        }
        Spacer(Modifier.height(PokedexDesignTokens.Spacing.Sm))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = accent,
            trackColor = accent.copy(alpha = .12f)
        )
    }
}

@Composable
fun ContextFilter(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, fontWeight = if (selected) FontWeight.Black else FontWeight.Bold) },
        modifier = modifier,
        shape = RoundedCornerShape(PokedexDesignTokens.Radius.Pill),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent.copy(alpha = .14f),
            selectedLabelColor = accent
        )
    )
}

@Composable
fun PrimaryCompanionAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable (() -> Unit))? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(PokedexDesignTokens.Radius.Md)
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(PokedexDesignTokens.Spacing.Sm))
        }
        Text(label, fontWeight = FontWeight.Black)
    }
}

@Composable
fun CompanionEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    visual: (@Composable (() -> Unit))? = null
) {
    Column(
        modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        visual?.let { it(); Spacer(Modifier.height(PokedexDesignTokens.Spacing.Md)) }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(PokedexDesignTokens.Spacing.Xs))
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(PokedexDesignTokens.Spacing.Md))
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
