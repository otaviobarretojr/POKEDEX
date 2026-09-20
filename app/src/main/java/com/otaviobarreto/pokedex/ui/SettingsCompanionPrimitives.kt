package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

/** Quiet, Nintendo-style grouping for Settings. Keeps actions readable without dashboard-card noise. */
@Composable
fun CompanionSettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f)
    ) {
        Column(content = { content() })
    }
}

@Composable
fun CompanionSettingsRow(
    title: String,
    supporting: String? = null,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = PokedexDesignTokens.Spacing.Lg,
                vertical = PokedexDesignTokens.Spacing.Md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(PokedexDesignTokens.Spacing.Md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (!supporting.isNullOrBlank()) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun CompanionSettingsNavigationRow(
    title: String,
    supporting: String? = null,
    icon: ImageVector,
    modifier: Modifier = Modifier
) = CompanionSettingsRow(
    title = title,
    supporting = supporting,
    icon = icon,
    modifier = modifier,
    trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
)

@Composable
fun CompanionSettingsDivider() {
    Divider(
        modifier = Modifier.padding(horizontal = PokedexDesignTokens.Spacing.Lg),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f)
    )
}
