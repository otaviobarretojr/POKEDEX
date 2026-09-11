package com.otaviobarreto.pokedex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class CaptureRouteGroup(
    val region: String,
    val source: String,
    val pokemon: List<PokeApiService.DexIndexEntry>
)

object CaptureRouteService {
    suspend fun build(gameLabel: String): List<CaptureRouteGroup> = withContext(Dispatchers.IO) {
        val game = AppGameCatalog.games.firstOrNull { it.label == gameLabel } ?: return@withContext emptyList()
        val dex = PokedexDataStore.nationalDex().associateBy { it.id }
        val missing = dex.keys - CollectionStore.capturedIds
        coroutineScope {
            game.regions.map { region ->
                async {
                    val ctx = GameContext.fromSource(region.source) ?: return@async null
                    val ids = runCatching { GameDexService.loadGameDex(ctx).map { it.nationalId } }.getOrDefault(emptyList())
                    val entries = ids.filter { it in missing }.mapNotNull(dex::get)
                    CaptureRouteGroup(region.label, region.source, entries)
                }
            }.awaitAll().filterNotNull().filter { it.pokemon.isNotEmpty() }
        }
    }
}
