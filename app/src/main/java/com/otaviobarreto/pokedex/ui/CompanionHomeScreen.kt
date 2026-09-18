package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.otaviobarreto.pokedex.data.AppGame
import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.data.AppStatePreferences
import com.otaviobarreto.pokedex.data.JourneyCatalog
import com.otaviobarreto.pokedex.data.JourneyProgressStore
import com.otaviobarreto.pokedex.data.JourneyStep

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
    val currentStep = remember(game?.label, revision) {
        game?.let { current ->
            val completed = JourneyProgressStore.completed(current.label)
            JourneyCatalog.steps(current.label).firstOrNull { it.id !in completed }
        }
    }
    val regionSource = remember(game?.label, currentStep?.id, revision) {
        game?.let { current ->
            companionRegionForJourneyStep(current, currentStep)
                ?: AppStatePreferences.activeRegionForGame(current.label)
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

private fun companionRegionForJourneyStep(game: AppGame, step: JourneyStep?): String? {
    val stepId = step?.id ?: return null
    val index = when (game.label) {
        "Pokémon Legends: Z-A" -> if (stepId.startsWith("za-dlc-")) 1 else 0
        "Scarlet / Violet" -> when {
            stepId.startsWith("sv-epi-") -> 1
            stepId.startsWith("sv-dlc-") -> {
                val number = stepId.removePrefix("sv-dlc-").toIntOrNull() ?: 0
                if (number in 1..7) 1 else 2
            }
            else -> 0
        }
        "Sword / Shield" -> when {
            stepId.startsWith("swsh-ioa-") -> 1
            stepId.startsWith("swsh-ct-") -> 2
            else -> 0
        }
        else -> 0
    }
    return game.regions.getOrNull(index)?.source
}
