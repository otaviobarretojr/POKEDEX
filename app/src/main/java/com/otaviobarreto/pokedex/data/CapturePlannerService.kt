package com.otaviobarreto.pokedex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

enum class CapturePlanCategory { AVAILABLE_HERE, OTHER_GAME_OR_TRADE, ALREADY_OWNED }
data class CapturePlanStep(val pokemon: PokeApiService.DexIndexEntry, val category: CapturePlanCategory, val priority: Int)
data class CapturePlan(
    val game: String,
    val obtainableMissing: List<PokeApiService.DexIndexEntry>,
    val externalMissing: List<PokeApiService.DexIndexEntry>,
    val alreadyCaptured: Int,
    val supportedTotal: Int,
    val steps: List<CapturePlanStep> = emptyList()
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
        val obtainable = missing.filter { it.id in availableIds }
        val external = missing.filter { it.id !in availableIds }
        CapturePlan(
            game = gameLabel,
            obtainableMissing = obtainable,
            externalMissing = external,
            alreadyCaptured = dex.count { it.id in CollectionStore.capturedIds },
            supportedTotal = dex.size,
            steps = obtainable.mapIndexed { index, p -> CapturePlanStep(p, CapturePlanCategory.AVAILABLE_HERE, index + 1) } +
                external.mapIndexed { index, p -> CapturePlanStep(p, CapturePlanCategory.OTHER_GAME_OR_TRADE, obtainable.size + index + 1) }
        )
    }
}
