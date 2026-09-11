package com.otaviobarreto.pokedex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class CapturePlan(
    val game: String,
    val obtainableMissing: List<PokeApiService.DexIndexEntry>,
    val externalMissing: List<PokeApiService.DexIndexEntry>,
    val alreadyCaptured: Int,
    val supportedTotal: Int
)

object CapturePlannerService {
    suspend fun build(gameLabel: String): CapturePlan = withContext(Dispatchers.IO) {
        val game = AppGameCatalog.games.firstOrNull { it.label == gameLabel }
            ?: return@withContext CapturePlan(gameLabel, emptyList(), emptyList(), 0, 0)
        val dex = PokedexDataStore.nationalDex()
        val availableIds = coroutineScope {
            game.regions.map { region ->
                async {
                    GameContext.fromSource(region.source)?.let {
                        runCatching { GameDexService.loadGameDex(it).map { entry -> entry.nationalId } }
                            .getOrDefault(emptyList())
                    }.orEmpty()
                }
            }.awaitAll().flatten().toSet()
        }
        val missing = dex.filter { it.id !in CollectionStore.capturedIds }
        CapturePlan(
            game = gameLabel,
            obtainableMissing = missing.filter { it.id in availableIds },
            externalMissing = missing.filter { it.id !in availableIds },
            alreadyCaptured = dex.count { it.id in CollectionStore.capturedIds },
            supportedTotal = dex.size
        )
    }
}
