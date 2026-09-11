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
    val encounterCount: Int,
    val sampleLocations: List<String> = emptyList(),
    val methods: List<String> = emptyList(),
    val minLevel: Int? = null,
    val maxLevel: Int? = null
)

object CaptureGuideService {
    suspend fun find(pokemonId: Int): List<CaptureAvailability> = withContext(Dispatchers.IO) {
        val encounters = runCatching { PokedexDataStore.encounters(pokemonId) }.getOrDefault(emptyList())
        coroutineScope {
            AppGameCatalog.adventureGames.flatMap { game ->
                game.regions.map { region ->
                    async {
                        val context = GameContext.fromSource(region.source) ?: return@async null
                        val dex = runCatching { GameDexService.loadGameDex(context) }.getOrDefault(emptyList())
                        if (dex.none { it.nationalId == pokemonId }) return@async null
                        val matching = encounters.mapNotNull { e ->
                            val details = e.details.filter { context.matchesVersion(it.version) }
                            if (e.versions.none(context::matchesVersion) && details.isEmpty()) null else e to details
                        }
                        val details = matching.flatMap { it.second }
                        CaptureAvailability(
                            game.label,
                            region.label,
                            region.source,
                            matching.size,
                            sampleLocations = matching.map { it.first.location }.distinct().take(4),
                            methods = details.map { it.method }.filter { it.isNotBlank() }.distinct().take(4),
                            minLevel = details.map { it.minLevel }.filter { it > 0 }.minOrNull(),
                            maxLevel = details.map { it.maxLevel }.filter { it > 0 }.maxOrNull()
                        )
                    }
                }
            }.awaitAll().filterNotNull().distinctBy { it.source }
        }
    }
}
