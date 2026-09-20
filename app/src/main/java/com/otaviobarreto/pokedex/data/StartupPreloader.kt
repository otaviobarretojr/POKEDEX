package com.otaviobarreto.pokedex.data

import android.content.Context
import android.os.SystemClock
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

        progress(.08f, "Abrindo dados locais")
        runCatching { PokedexDataStore.nationalDex() }
        val nationalSnapshot = PokedexDataStore.cachedNationalDex().orEmpty()
        val nationalByName = nationalSnapshot.associateBy { it.name.lowercase() }

        progress(.14f, "Preparando sua coleção")
        runCatching { CollectionAdvisor.warmAllGames() }

        val activeGame = AppStatePreferences.activeGame
        val game = AppGameCatalog.games.firstOrNull { it.label == activeGame }
        val journeySteps = JourneyCatalog.steps(activeGame)
        val journeyCompleted = JourneyProgressStore.completed(activeGame)
        val nextJourneyIndex = journeySteps.indexOfFirst { it.id !in journeyCompleted }.let { if (it < 0) 0 else it }
        val journeyWarmSteps = journeySteps
            .drop((nextJourneyIndex - 1).coerceAtLeast(0))
            .take(7)

        // Materializa os catálogos que Minha Jornada usa antes da primeira composição.
        journeyWarmSteps.forEach { step ->
            JourneyObjectiveDetailsCatalog.detail(step.id)
            JourneyPreparationCatalog.forStep(step.id)
            JourneyWalkthroughCatalog.forStep(step.id)
            JourneyVisualAssetCatalog.forStep(step.id)
        }
        JourneySmartProgress.context(activeGame)
        JourneyStarterCatalog.forGame(activeGame)

        progress(.20f, "Preparando sua Jornada")
        val activeContexts = game?.regions.orEmpty()
            .mapNotNull { region -> GameContext.fromSource(region.source) }

        coroutineScope {
            activeContexts.map { ctx ->
                async { runCatching { GameDexService.loadGameDex(ctx) } }
            }.awaitAll()
        }

        val gameDexIds = activeContexts.flatMap { ctx ->
            GameDexService.cached(ctx).orEmpty()
        }.map { it.nationalId }.distinct()

        progress(.40f, "Preparando contexto ativo")

        val activeRegionSource = game?.let { AppStatePreferences.activeRegionForGame(it.label) }
        val activeContext = GameContext.fromSource(activeRegionSource)
        val activeDex = activeContext?.let { GameDexService.cached(it).orEmpty() }.orEmpty()
        val activePage = activeRegionSource?.let { AppStatePreferences.boxPage(it) } ?: 0
        val activePageIds = activeDex.drop(activePage.coerceAtLeast(0) * 30).take(30).map { it.nationalId }
        val ownedVariantIds = VariantCollectionStore.ownedVariants
            .asSequence()
            .filter { activeRegionSource==null || it.source==activeRegionSource }
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
        }.distinct().take(24)

        progress(.60f, "Aquecendo detalhes dos Pokémon")
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
                val local = .66f + ((index + 1f) / groups) * .17f
                progress(local, "Aquecendo detalhes dos Pokémon")
            }
        }

        val activeCoverUrls = GameCoverCatalog.coversFor(activeGame)
        val activeGameHeroUrls = listOfNotNull(GameCoverCatalog.heroFor(activeGame))
        val activeHeroUrls = JourneyGameVisualCatalog.forGame(activeGame).heroPokemonIds
            .map { id ->
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" + id + ".png"
            }
        val activeRouteArtworkUrls = journeyWarmSteps
            .mapNotNull { JourneyVisualAssetCatalog.forStep(it.id)?.imageUrl }

        val activeOpponentIds = journeyWarmSteps
            .flatMap { step -> JourneyObjectiveDetailsCatalog.detail(step.id)?.opponents.orEmpty() }
            .mapNotNull { member ->
                val simple = member.name
                    .substringBefore(" / ")
                    .substringBefore(" & ")
                    .substringBefore(" · ")
                    .trim()
                nationalByName[simple.lowercase()]?.id
            }
            .distinct()
            .take(18)

        val activeOpponentArtworkUrls = activeOpponentIds.map { id ->
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" + id + ".png"
        }

        val activeJourneyArtworkUrls = (
            activeCoverUrls + activeGameHeroUrls + activeHeroUrls + activeRouteArtworkUrls + activeOpponentArtworkUrls
        ).distinct()
        val ownedShinyIds = VariantCollectionStore.ownedVariants
            .asSequence().filter { it.shiny }.map { it.speciesId }.distinct().take(12).toList()
        val collectionArtworkUrls = buildList {
            listOf(1,4,7,26,157,724).forEach { id ->
                add("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+id+".png")
            }
            (listOf(25,94,448)+ownedShinyIds).distinct().forEach { id ->
                add("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/shiny/"+id+".png")
            }
        }
        val criticalArtworkUrls=(activeJourneyArtworkUrls+collectionArtworkUrls).distinct()

        progress(.84f, "Aquecendo sua Jornada")
        val artworkSemaphore = Semaphore(4)
        supervisorScope {
            criticalArtworkUrls.mapIndexed { index, artwork ->
                async {
                    artworkSemaphore.withPermit {
                        runCatching {
                            context.imageLoader.execute(
                                ImageRequest.Builder(context)
                                    .data(OfflineLibraryManager.resolveAny(context,artwork) ?: artwork)
                                    .size(320)
                                    .build()
                            )
                        }
                    }
                    val local = .84f + ((index + 1f) / criticalArtworkUrls.size.coerceAtLeast(1)) * .07f
                    progress(local, "Aquecendo sua Jornada")
                }
            }.awaitAll()
        }

        progress(.91f, "Preparando biblioteca de jogos")
        val gameLibraryArtworkUrls = AppGameCatalog.adventureGames
            .flatMap { game ->
                GameCoverCatalog.coversFor(game.label) + listOfNotNull(GameCoverCatalog.heroFor(game.label))
            }
            .distinct()
        supervisorScope {
            gameLibraryArtworkUrls.map { artwork ->
                async {
                    artworkSemaphore.withPermit {
                        runCatching {
                            context.imageLoader.execute(
                                ImageRequest.Builder(context)
                                    .data(artwork)
                                    .size(640)
                                    .diskCacheKey(artwork)
                                    .build()
                            )
                        }
                    }
                }
            }.awaitAll()
        }

        progress(.94f, "Preparando imagens")
        val spriteIds = priorityIds.take(12)
        val spriteSemaphore = Semaphore(4)
        supervisorScope {
            spriteIds.mapIndexed { index, id ->
                async {
                    val sprite = PokedexDataStore.cachedPokemon(id)?.spriteUrl
                    if (sprite != null) {
                        spriteSemaphore.withPermit {
                            runCatching {
                                context.imageLoader.execute(
                                    ImageRequest.Builder(context)
                                        .data(sprite)
                                        .size(192)
                                        .build()
                                )
                            }
                        }
                    }
                    val local = .94f + ((index + 1f) / spriteIds.size.coerceAtLeast(1)) * .05f
                    progress(local, "Preparando imagens")
                }
            }.awaitAll()
        }

        progress(1f, "Tudo pronto")
        lastWarmDurationMs = SystemClock.elapsedRealtime() - startedAt
    }


    suspend fun warmLivingDexFilter(
        context:Context,
        ids:Set<Int>,
        onProgress:suspend (Int,Int)->Unit = {_,_->}
    ){
        if(ids.isEmpty()) return
        val ordered=ids.filter{it in 1..PokeApiService.MAX_NATIONAL_DEX_ID}.sorted()
        val semaphore=Semaphore(8)
        var completed=0
        ordered.chunked(48).forEach { batch ->
            supervisorScope {
                batch.map { id -> async {
                    semaphore.withPermit {
                        val url="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
                        runCatching {
                            context.imageLoader.execute(
                                ImageRequest.Builder(context)
                                    .data(OfflineLibraryManager.resolveAny(context,"pokemon-offline-$id") ?: OfflineLibraryManager.resolveAny(context,url) ?: url)
                                    .size(160)
                                    .memoryCacheKey("pokemon-offline-$id")
                                    .diskCacheKey("pokemon-offline-$id")
                                    .build()
                            )
                        }
                    }
                }}.awaitAll()
            }
            completed=(completed+batch.size).coerceAtMost(ordered.size)
            onProgress(completed,ordered.size)
        }
    }

    suspend fun warmBoxWindow(context:Context,dex:List<GameDexService.GameDexEntry>,page:Int){
        if(dex.isEmpty()) return
        val pages=listOf(page-1,page,page+1).filter{it>=0}.distinct()
        val ids=pages.flatMap{p->dex.drop(p*30).take(30).map{it.nationalId}}.distinct()
        val semaphore=Semaphore(6)
        supervisorScope{
            ids.map{id->async{
                semaphore.withPermit{
                    val url="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
                    runCatching{context.imageLoader.execute(ImageRequest.Builder(context).data(OfflineLibraryManager.resolveAny(context,"pokemon-offline-$id") ?: OfflineLibraryManager.resolveAny(context,url) ?: url).size(160).memoryCacheKey("pokemon-offline-$id").diskCacheKey("pokemon-offline-$id").build())}
                }
            }}.awaitAll()
        }
    }

    fun launchWarmInBackground(context: Context) {
        val appContext=context.applicationContext
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
            val pageIds = activeDex.drop(activePage.coerceAtLeast(0) * 30).take(30).map { it.nationalId }
            val ids = buildList {
                addAll(RecentActivityStore.recentPokemon.take(16))
                addAll(pageIds)
                addAll(OfflineGamePackManager.manifestIds(activeGame).take(24))
            }.distinct().take(48)

            ids.chunked(6).forEach { chunk ->
                coroutineScope {
                    chunk.map { id ->
                        async { runCatching { PokedexDataStore.prefetchCoreDetails(id) } }
                    }.awaitAll()
                }
            }

            val imageSemaphore = Semaphore(permits = 3)
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
