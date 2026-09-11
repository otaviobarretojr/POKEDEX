package com.otaviobarreto.pokedex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class CaptureAvailability(
    val game: String,
    val region: String,
    val source: String,
    val encounterCount: Int
)

object CaptureGuideService {
    suspend fun find(pokemonId: Int): List<CaptureAvailability> = withContext(Dispatchers.IO) {
        val encounters = runCatching { PokedexDataStore.encounters(pokemonId) }.getOrDefault(emptyList())
        coroutineScope {
            AppGameCatalog.games.flatMap { game ->
                game.regions.map { region ->
                    async {
                        val context = GameContext.fromSource(region.source) ?: return@async null
                        val dex = runCatching { GameDexService.loadGameDex(context) }.getOrDefault(emptyList())
                        if (dex.none { it.nationalId == pokemonId }) return@async null
                        val filtered = encounters.count { e ->
                            e.versions.any(context::matchesVersion) || e.details.any { context.matchesVersion(it.version) }
                        }
                        CaptureAvailability(game.label, region.label, region.source, filtered)
                    }
                }
            }.awaitAll().filterNotNull().distinctBy { it.source }
        }
    }
}
