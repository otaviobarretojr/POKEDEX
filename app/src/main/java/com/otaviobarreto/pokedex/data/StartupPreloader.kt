package com.otaviobarreto.pokedex.data

import android.content.Context
import android.os.SystemClock
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class StartupPreloadProgress(
    val fraction: Float,
    val label: String
)

object StartupPreloader {
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile var lastWarmDurationMs: Long = 0L
        private set

    suspend fun warm(
        context: Context,
        onProgress: (StartupPreloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val startedAt = SystemClock.elapsedRealtime()

        suspend fun progress(value: Float, label: String) {
            withContext(Dispatchers.Main.immediate) {
                onProgress(StartupPreloadProgress(value.coerceIn(0f, 1f), label))
            }
        }

        progress(.08f, "Abrindo Pokédex")
        runCatching { PokedexDataStore.nationalDex() }

        progress(.18f, "Preparando sua coleção")
        runCatching { CollectionAdvisor.warmAllGames() }

        val activeGame = AppStatePreferences.activeGame
        val game = AppGameCatalog.games.firstOrNull { it.label == activeGame }
        val activeContexts = game?.regions.orEmpty()
            .mapNotNull { region -> GameContext.fromSource(region.source) }

        progress(.30f, "Preparando Pokédex do jogo")
        coroutineScope {
            activeContexts.map { ctx ->
                async { runCatching { GameDexService.loadGameDex(ctx) } }
            }.awaitAll()
        }

        val gameDexIds = activeContexts.flatMap { ctx ->
            GameDexService.cached(ctx).orEmpty()
        }.map { it.nationalId }.distinct()

        val activeRegionSource = game?.let { AppStatePreferences.activeRegionForGame(it.label) }
        val activeContext = GameContext.fromSource(activeRegionSource)
        val activeDex = activeContext?.let { GameDexService.cached(it).orEmpty() }.orEmpty()
        val activePage = activeRegionSource?.let { AppStatePreferences.boxPage(it) } ?: 0
        val activePageIds = activeDex
            .drop(activePage.coerceAtLeast(0) * 30)
            .take(30)
            .map { it.nationalId }
        val ownedVariantIds = VariantCollectionStore.ownedVariants
            .asSequence()
            .filter { activeRegionSource == null || it.source == activeRegionSource }
            .map { it.speciesId }
            .distinct()
            .take(12)
            .toList()

        val priorityIds = buildList {
            addAll(RecentActivityStore.recentPokemon.take(8))
            addAll(activePageIds.take(12))
            addAll(ownedVariantIds.take(8))
            addAll(OfflineGamePackManager.manifestIds(activeGame).take(8))
            addAll(gameDexIds.take(8))
            addAll(CollectionStore.capturedIds.take(8))
        }.distinct().take(24)

        progress(.48f, "Aquecendo detalhes dos Pokémon")
        coroutineScope {
            priorityIds.take(4).map { id ->
                async {
                    runCatching {
                        PokedexDataStore.prefetchFullDetails(id)
                        PokemonFormsService.collectible(id)
                    }
                }
            }.awaitAll()

            priorityIds.drop(4).chunked(6).forEachIndexed { index, chunk ->
                chunk.map { id ->
                    async {
                        runCatching {
                            PokedexDataStore.prefetchCoreDetails(id)
                            PokemonFormsService.collectible(id)
                        }
                    }
                }.awaitAll()
                val groups = ((priorityIds.drop(4).size + 5) / 6).coerceAtLeast(1)
                progress(.55f + ((index + 1f) / groups) * .22f, "Aquecendo detalhes dos Pokémon")
            }
        }

        progress(.80f, "Preparando imagens")
        val artworkSemaphore = Semaphore(4)
        supervisorScope {
            priorityIds.take(16).mapIndexed { index, id ->
                async {
                    artworkSemaphore.withPermit {
                        val artwork =
                            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" +
                                id + ".png"
                        runCatching {
                            context.imageLoader.execute(
                                ImageRequest.Builder(context)
                                    .data(OfflineLibraryManager.resolveAny(context, artwork) ?: artwork)
                                    .size(320)
                                    .build()
                            )
                        }
                    }
                    progress(
                        .80f + ((index + 1f) / priorityIds.take(16).size.coerceAtLeast(1)) * .19f,
                        "Preparando imagens"
                    )
                }
            }.awaitAll()
        }

        progress(1f, "Pokédex pronta")
        lastWarmDurationMs = SystemClock.elapsedRealtime() - startedAt
    }

    fun launchWarmInBackground(context: Context) {
        val appContext = context.applicationContext
        backgroundScope.launch {
            runCatching { warm(appContext) { } }
            launchExtendedWarm(appContext)
        }
    }

    fun launchExtendedWarm(context: Context) {
        val appContext = context.applicationContext
        backgroundScope.launch {
            val activeGame = AppStatePreferences.activeGame
            val activeRegionSource = AppGameCatalog.games
                .firstOrNull { it.label == activeGame }
                ?.let { AppStatePreferences.activeRegionForGame(it.label) }
            val activeContext = GameContext.fromSource(activeRegionSource)
            val activeDex = activeContext?.let { GameDexService.cached(it).orEmpty() }.orEmpty()
            val activePage = activeRegionSource?.let { AppStatePreferences.boxPage(it) } ?: 0
            val pageIds = activeDex
                .drop(activePage.coerceAtLeast(0) * 30)
                .take(30)
                .map { it.nationalId }

            val ids = buildList {
                addAll(RecentActivityStore.recentPokemon.take(16))
                addAll(pageIds)
                addAll(OfflineGamePackManager.manifestIds(activeGame).take(24))
                addAll(CollectionStore.capturedIds.take(16))
            }.distinct().take(48)

            ids.chunked(6).forEach { chunk ->
                coroutineScope {
                    chunk.map { id ->
                        async { runCatching { PokedexDataStore.prefetchCoreDetails(id) } }
                    }.awaitAll()
                }
            }

            val imageSemaphore = Semaphore(3)
            supervisorScope {
                ids.take(24).map { id ->
                    async {
                        val sprite = PokedexDataStore.cachedPokemon(id)?.spriteUrl ?: return@async
                        imageSemaphore.withPermit {
                            runCatching {
                                appContext.imageLoader.execute(
                                    ImageRequest.Builder(appContext)
                                        .data(sprite)
                                        .size(192)
                                        .build()
                                )
                            }
                        }
                    }
                }.awaitAll()
            }
        }
    }
}
