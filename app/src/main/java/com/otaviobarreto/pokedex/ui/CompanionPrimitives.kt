package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun CompanionSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingLabel: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            supportingLabel?.let {
                Spacer(Modifier.height(PokedexDesignTokens.Spacing.Xs))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier.defaultMinSize(minHeight = PokedexDesignTokens.Companion.MinimumTouchTarget)
            ) { Text(actionLabel) }
        }
    }
}

@Composable
internal fun CompanionProgress(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    label: String? = null
) {
    val safeTotal = total.coerceAtLeast(0)
    val safeCurrent = current.coerceIn(0, safeTotal.coerceAtLeast(current))
    val fraction = if (safeTotal == 0) 0f else (safeCurrent.toFloat() / safeTotal).coerceIn(0f, 1f)
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            label?.let {
                Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } ?: Spacer(Modifier.weight(1f))
            Text("$safeCurrent / $safeTotal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(PokedexDesignTokens.Spacing.Sm))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = accent,
            trackColor = accent.copy(alpha = PokedexDesignTokens.Companion.SubtleAccentSurfaceAlpha)
        )
    }
}

@Composable
internal fun PrimaryCompanionAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = PokedexDesignTokens.Companion.MinimumTouchTarget),
        shape = PokedexDesignTokens.Shapes.large
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
