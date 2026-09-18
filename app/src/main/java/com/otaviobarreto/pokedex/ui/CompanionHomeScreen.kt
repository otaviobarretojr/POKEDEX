package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.data.AppStatePreferences
import com.otaviobarreto.pokedex.data.JourneyProgressStore

@Composable
fun CompanionHomeScreen() {
    val revision = JourneyProgressStore.revision
    val configuredGame = AppStatePreferences.activeGame
    val game = remember(revision, configuredGame) {
        AppGameCatalog.adventureGames.firstOrNull { it.label == configuredGame && JourneyProgressStore.isStarted(it.label) }
            ?: AppGameCatalog.adventureGames.firstOrNull { JourneyProgressStore.isStarted(it.label) }
            ?: AppGameCatalog.adventureGames.firstOrNull { it.label == configuredGame }
            ?: AppGameCatalog.adventureGames.firstOrNull()
    }
    val regionSource = remember(game?.label, revision) {
        game?.let { current ->
            AppStatePreferences.activeRegionForGame(current.label)
                ?.takeIf { source -> current.regions.any { it.source == source } }
                ?: current.regions.firstOrNull()?.source
        }
    }

    Box(Modifier.fillMaxSize()) {
        game?.let { current ->
            key(current.label, regionSource) {
                PokemonLivingCompanionCard(
                    gameLabel = current.label,
                    pokemonId = 25,
                    pokemonName = "Pikachu",
                    regionSource = regionSource,
                    immersive = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
