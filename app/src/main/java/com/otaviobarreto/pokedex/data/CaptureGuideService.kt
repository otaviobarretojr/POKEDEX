package com.otaviobarreto.pokedex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CaptureAvailability(
    val game: String,
    val region: String,
    val source: String,
    val encounterCount: Int
)

object CaptureGuideService {
    suspend fun find(pokemonId: Int): List<CaptureAvailability> = withContext(Dispatchers.IO) {
        buildList {
            AppGameCatalog.games.forEach { game ->
                game.regions.forEach { region ->
                    val context = GameContext.fromSource(region.source) ?: return@forEach
                    val dex = runCatching { GameDexService.loadGameDex(context) }.getOrDefault(emptyList())
                    if (dex.any { it.nationalId == pokemonId }) {
                        val encounters = runCatching { PokedexDataStore.encounters(pokemonId) }.getOrDefault(emptyList())
                        val filtered = encounters.count { e ->
                            e.versions.any(context::matchesVersion) || e.details.any { context.matchesVersion(it.version) }
                        }
                        add(CaptureAvailability(game.label, region.label, region.source, filtered))
                    }
                }
            }
        }.distinctBy { it.source }
    }
}
