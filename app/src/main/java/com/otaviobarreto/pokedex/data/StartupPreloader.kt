package com.otaviobarreto.pokedex.data

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class StartupPreloadProgress(
    val fraction: Float,
    val label: String
)

object StartupPreloader {
    suspend fun warm(
        context: Context,
        onProgress: (StartupPreloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        suspend fun progress(value: Float, label: String) {
            withContext(Dispatchers.Main.immediate) {
                onProgress(StartupPreloadProgress(value.coerceIn(0f, 1f), label))
            }
        }

        progress(.08f, "Abrindo dados locais")
        runCatching { PokedexDataStore.nationalDex() }

        val activeGame = AppStatePreferences.activeGame
        val game = AppGameCatalog.games.firstOrNull { it.label == activeGame }

        progress(.24f, "Preparando sua Jornada")
        val gameDexIds = game?.regions.orEmpty().mapNotNull { region ->
            GameContext.fromSource(region.source)
        }.map { ctx ->
            runCatching { GameDexService.loadGameDex(ctx) }.getOrDefault(emptyList())
        }.flatten().map { it.nationalId }.distinct()

        progress(.43f, "Carregando catálogos essenciais")
        coroutineScope {
            listOf("move", "ability", "item").map { kind ->
                async { runCatching { ReferenceCatalogService.load(kind) } }
            }.awaitAll()
        }

        val priorityIds = buildList {
            addAll(RecentActivityStore.recentPokemon.take(12))
            addAll(OfflineGamePackManager.manifestIds(activeGame).take(18))
            addAll(gameDexIds.take(18))
        }.distinct().take(32)

        progress(.63f, "Aquecendo detalhes dos Pokémon")
        coroutineScope {
            priorityIds.chunked(4).forEachIndexed { index, chunk ->
                chunk.map { id ->
                    async { runCatching { PokedexDataStore.prefetchCoreDetails(id) } }
                }.awaitAll()
                val local = .63f + ((index + 1f) / ((priorityIds.size + 3) / 4).coerceAtLeast(1)) * .20f
                progress(local, "Aquecendo detalhes dos Pokémon")
            }
        }

        progress(.86f, "Preparando imagens")
        priorityIds.take(20).forEachIndexed { index, id ->
            val sprite = PokedexDataStore.cachedPokemon(id)?.spriteUrl ?: return@forEachIndexed
            runCatching {
                context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(sprite)
                        .memoryCacheKey("startup-pokemon-$id")
                        .diskCacheKey("startup-pokemon-$id")
                        .build()
                )
            }
            val local = .86f + ((index + 1f) / priorityIds.take(20).size.coerceAtLeast(1)) * .11f
            progress(local, "Preparando imagens")
        }

        progress(1f, "Tudo pronto")
    }
}
