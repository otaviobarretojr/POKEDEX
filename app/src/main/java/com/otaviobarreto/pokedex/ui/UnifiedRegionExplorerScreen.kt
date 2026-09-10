package com.otaviobarreto.pokedex.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.otaviobarreto.pokedex.data.GameContext

private enum class RegionExplorerEngine {
    PALDEA,
    RECREATED_DLC,
    LEGACY_REGIONAL
}

private fun resolveRegionEngine(context: GameContext?): RegionExplorerEngine =
    when (context?.regionLabel) {
        "Paldea" -> RegionExplorerEngine.PALDEA
        "Kitakami", "Blueberry" -> RegionExplorerEngine.RECREATED_DLC
        else -> RegionExplorerEngine.LEGACY_REGIONAL
    }

/**
 * Single public entry point for every regional explorer.
 * The concrete engines can be migrated independently without changing navigation.
 */
@Composable
fun UnifiedRegionExplorerScreen(
    source: String,
    onBack: () -> Unit,
    onPokemonClick: (Int, String) -> Unit
) {
    val context = remember(source) { GameContext.fromSource(source) }
    val engine = remember(context) { resolveRegionEngine(context) }

    when (engine) {
        RegionExplorerEngine.PALDEA ->
            RegionExplorerV3Screen(source, onBack, onPokemonClick)

        RegionExplorerEngine.RECREATED_DLC ->
            RegionExplorerV4Screen(source, onBack, onPokemonClick)

        RegionExplorerEngine.LEGACY_REGIONAL ->
            RegionExplorerV2Screen(source, onBack, onPokemonClick)
    }
}
