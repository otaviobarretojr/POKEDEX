package com.otaviobarreto.pokedex.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.otaviobarreto.pokedex.data.GameContext

/**
 * Single routing point for regional map implementations.
 * Legacy engines remain available while regions are migrated, but callers
 * no longer need to know which generation of the explorer serves a region.
 */
@Composable
fun UnifiedRegionExplorerScreen(
    source: String,
    onBack: () -> Unit,
    onPokemonClick: (Int, String) -> Unit
) {
    val context = remember(source) { GameContext.fromSource(source) }
    when (context?.regionLabel) {
        "Paldea" -> RegionExplorerV3Screen(source, onBack, onPokemonClick)
        "Kitakami", "Blueberry" -> RegionExplorerV4Screen(source, onBack, onPokemonClick)
        else -> RegionExplorerV2Screen(source, onBack, onPokemonClick)
    }
}
