package com.otaviobarreto.pokedex.data

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

object OfflineGamePackManager {
    private const val PREFS = "offline_game_packs_v2"
    private const val PACK_VERSION = 20
    private const val DOWNLOAD_CONCURRENCY = 10
    private const val FORM_DOWNLOAD_CONCURRENCY = 4
    private const val ESTIMATED_SHARED_POKEMON_BYTES = 620L * 1024L
    private const val ESTIMATED_GAME_BASE_BYTES = 6L * 1024L * 1024L
    private var context: Context? = null

    data class PackStatus(
        val downloaded: Boolean,
        val downloadedAt: Long = 0L,
        val pokemonCount: Int = 0,
        val packVersion: Int = 0,
        val completeCount: Int = 0,
        val reusedCount: Int = 0,
        val downloadedNewCount: Int = 0
    ) {
        val verified: Boolean
            get() = downloaded && packVersion == PACK_VERSION && pokemonCount > 0 && completeCount == pokemonCount
    }

    data class PackAudit(
        val valid: Boolean,
        val completedIds: Int,
        val expectedCount: Int,
        val currentVersion: Boolean,
        val hasRegionManifest: Boolean,
        val pinnedResources: Int,
        val expectedResources: Int,
        val cachedImages: Int,
        val expectedImages: Int,
        val cachedJourneyVisuals: Int,
        val expectedJourneyVisuals: Int,
        val cachedFormArtworks: Int,
        val expectedFormArtworks: Int
    ) {
        val summary: String
            get() = when {
                valid -> "Pacote íntegro"
                expectedCount == 0 -> "Pacote ainda não iniciado"
                completedIds < expectedCount -> "Faltam ${expectedCount - completedIds} Pokémon"
                !currentVersion -> "Pacote precisa ser atualizado"
                !hasRegionManifest -> "Manifesto regional incompleto"
                pinnedResources < expectedResources -> "Recursos locais incompletos"
                cachedImages < expectedImages -> "Imagens offline incompletas"
                cachedJourneyVisuals < expectedJourneyVisuals -> "Visuais da Jornada incompletos"
                cachedFormArtworks < expectedFormArtworks -> "Artes de formas/Shiny incompletas"
                else -> "Pacote precisa de reparo"
            }
    }

    data class Progress(
        val done: Int,
        val total: Int,
        val label: String
    ) {
        val fraction: Float
            get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    }

    data class GeneralLibraryStatus(
        val ready:Boolean,
        val total:Int,
        val complete:Int,
        val packVersion:Int
    ){
        val verified:Boolean
            get() = ready && packVersion==PACK_VERSION && total>0 && complete==total
    }

    data class DownloadEstimate(
        val totalBytes:Long,
        val remainingBytes:Long,
        val remainingPokemon:Int,
        val reusedPokemon:Int
    ){
        val downloadedBytes:Long
            get()=(totalBytes-remainingBytes).coerceAtLeast(0L)
    }

    fun estimateGeneral():DownloadEstimate {
        val status=generalStatus()
        val manifest=generalManifestIds()
        val totalCount=when{
            manifest.isNotEmpty() -> manifest.size
            status.total>0 -> status.total
            else -> 1025
        }
        val completed=when{
            manifest.isNotEmpty() -> manifest.count{sharedPokemonAssets(it)!=null}
            else -> status.complete.coerceAtMost(totalCount)
        }
        val remaining=(totalCount-completed).coerceAtLeast(0)
        val totalBytes=totalCount*ESTIMATED_SHARED_POKEMON_BYTES
        return DownloadEstimate(
            totalBytes=totalBytes,
            remainingBytes=remaining*ESTIMATED_SHARED_POKEMON_BYTES,
            remainingPokemon=remaining,
            reusedPokemon=completed
        )
    }

    fun estimateGame(gameLabel:String):DownloadEstimate {
        val game=AppGameCatalog.adventureGames.firstOrNull{it.label==gameLabel}
            ?: return DownloadEstimate(0,0,0,0)
        val ids=manifestIds(gameLabel)
        val expected=if(ids.isNotEmpty()) ids else emptySet()
        val reusable=expected.count{sharedPokemonAssets(it)!=null}
        val missingPokemon=(expected.size-reusable).coerceAtLeast(0)
        val totalBytes=ESTIMATED_GAME_BASE_BYTES + expected.size*ESTIMATED_SHARED_POKEMON_BYTES
        val remainingBytes=ESTIMATED_GAME_BASE_BYTES + missingPokemon*ESTIMATED_SHARED_POKEMON_BYTES
        return DownloadEstimate(
            totalBytes=totalBytes,
            remainingBytes=remainingBytes,
            remainingPokemon=missingPokemon,
            reusedPokemon=reusable
        )
    }

    fun formatBytes(bytes:Long):String {
        val safe=bytes.coerceAtLeast(0L)
        return if(safe>=1024L*1024L*1024L)
            String.format("%.2f GB",safe/1024.0/1024.0/1024.0)
        else
            String.format("%.0f MB",safe/1024.0/1024.0)
    }

    fun generalStatus():GeneralLibraryStatus {
        val p=prefs()
        return GeneralLibraryStatus(
            ready=p.getBoolean("general_ready",false),
            total=p.getInt("general_count",0),
            complete=p.getInt("general_complete",0),
            packVersion=p.getInt("general_version",0)
        )
    }

    fun generalManifestIds():Set<Int> =
        prefs().getStringSet("general_manifest_ids", emptySet()).orEmpty()
            .mapNotNull{it.toIntOrNull()}.toSet()

    fun generalAudit():Boolean {
        val status=generalStatus()
        val ids=generalManifestIds()
        if(!status.verified || ids.size!=status.total) return false
        if(JourneyReadinessAudit.referenceCatalogUrls().any{
            !PersistentApiCache.has(it) || !PersistentApiCache.isPinned(it)
        }) return false
        return ids.all{sharedPokemonAssets(it)!=null}
    }

    fun initialize(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
    }

    fun requiredBaseResourceUrls(game: AppGame): Set<String> {
        val regionUrls=game.regions.mapNotNull { region ->
            GameContext.fromSource(region.source)?.let(GameDexService::cacheUrl)
        }
        return (regionUrls + JourneyReadinessAudit.referenceCatalogUrls()).toSet()
    }

    fun audit(gameLabel: String): PackAudit {
        val p = prefs()
        val expected = p.getInt(key(gameLabel, "count"), 0)
        val completed = p.getStringSet(key(gameLabel, "completed_ids"), emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .distinct()
            .size
        val current = p.getInt(key(gameLabel, "version"), 0) == PACK_VERSION
        val regions = !p.getString(key(gameLabel, "regions"), null).isNullOrBlank()
        val declaredResources = p.getStringSet(key(gameLabel, "resource_urls"), emptySet()).orEmpty()
        val game = AppGameCatalog.adventureGames.firstOrNull { it.label == gameLabel }
        val resources = declaredResources + game?.let(::requiredBaseResourceUrls).orEmpty()
        val pinned = resources.count { PersistentApiCache.has(it) && PersistentApiCache.isPinned(it) }
        val ids = manifestIds(gameLabel)
        val cachedImages = ids.count(::hasOfflineArtwork)
        val visualUrls = prefs().getStringSet(key(gameLabel, "visual_urls"), emptySet()).orEmpty()
        val cachedJourneyVisuals = visualUrls.count(::hasOfflineVisual)
        val formArtworkKeys = p.getStringSet(key(gameLabel, "form_artwork_keys"), emptySet()).orEmpty()
        val cachedFormArtworks = formArtworkKeys.count(::hasOfflineCacheKey)
        val valid = expected > 0 &&
            completed == expected &&
            current &&
            regions &&
            resources.isNotEmpty() &&
            pinned == resources.size &&
            ids.size == expected &&
            cachedImages == ids.size &&
            cachedJourneyVisuals == visualUrls.size &&
            cachedFormArtworks == formArtworkKeys.size
        return PackAudit(
            valid,
            completed,
            expected,
            current,
            regions,
            pinned,
            resources.size,
            cachedImages,
            ids.size,
            cachedJourneyVisuals,
            visualUrls.size,
            cachedFormArtworks,
            formArtworkKeys.size
        )
    }

    fun manifestIds(gameLabel: String): Set<Int> =
        prefs().getStringSet(key(gameLabel, "manifest_ids"), emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }.toSet()

    fun resourceUrls(gameLabel: String): Set<String> =
        prefs().getStringSet(key(gameLabel, "resource_urls"), emptySet()).orEmpty()

    fun formArtworkKeys(gameLabel: String): Set<String> =
        prefs().getStringSet(key(gameLabel, "form_artwork_keys"), emptySet()).orEmpty()

    private data class SharedPokemonAssets(
        val resources:Set<String>,
        val formArtworkKeys:Set<String>
    )

    private fun sharedPokemonAssets(id:Int):SharedPokemonAssets? {
        val p=prefs()
        val resources=p.getStringSet(sharedKey(id,"resource_urls"), emptySet()).orEmpty()
        val formKeys=p.getStringSet(sharedKey(id,"form_artwork_keys"), emptySet()).orEmpty()
        val requiredCore=setOf(
            PokeApiService.pokemonUrl(id),
            PokeApiService.speciesUrl(id),
            PokeApiService.encountersUrl(id)
        )
        if(!resources.containsAll(requiredCore)) return null
        if(resources.any{!PersistentApiCache.has(it) || !PersistentApiCache.isPinned(it)}) return null
        if(!hasOfflineArtwork(id)) return null
        if(formKeys.any{!hasOfflineCacheKey(it)}) return null
        return SharedPokemonAssets(resources,formKeys)
    }

    private fun persistSharedPokemonAssets(
        id:Int,
        resources:Set<String>,
        formKeys:Set<String>
    ){
        prefs().edit()
            .putStringSet(sharedKey(id,"resource_urls"),resources)
            .putStringSet(sharedKey(id,"form_artwork_keys"),formKeys)
            .apply()
    }

    fun registerImportedGeneralPokemon(
        id:Int,
        resources:Set<String>,
        formKeys:Set<String>
    ){
        persistSharedPokemonAssets(id,resources,formKeys)
    }

    fun finalizeImportedGeneral(ids:Set<Int>,serverVersion:Int?=null){
        val edit=prefs().edit()
            .putBoolean("general_ready",true)
            .putInt("general_count",ids.size)
            .putInt("general_complete",ids.size)
            .putInt("general_version",PACK_VERSION)
            .putStringSet("general_manifest_ids",ids.map(Int::toString).toSet())
        serverVersion?.let{edit.putInt("general_server_version",it)}
        edit.apply()
    }

    fun generalServerVersion():Int =
        prefs().getInt("general_server_version",0)

    private fun formArtworkKey(
        speciesId: Int,
        formPokemonId: Int,
        formKey: String,
        shiny: Boolean
    ): String {
        val safeFormKey = formKey.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return "pokemon-form-offline-$speciesId-$formPokemonId-$safeFormKey-" +
            if (shiny) "shiny" else "normal"
    }

    private suspend fun downloadSharedPokemonAssets(
        id:Int,
        appContext:Context
    ):SharedPokemonAssets = coroutineScope {
        val pokemonJob=async{PokedexDataStore.pokemon(id)}
        val speciesJob=async{PokedexDataStore.species(id)}
        val encounterJob=async{PokedexDataStore.encounters(id)}
        val formsJob=async{PokemonFormsService.collectible(id)}
        val pokemon=pokemonJob.await()
        val species=speciesJob.await()
        val evolutionUrl=species.evolutionChainUrl
        evolutionUrl?.let{PokedexDataStore.evolutions(it)}
        encounterJob.await()
        val forms=formsJob.await()

        val resources=buildSet {
            add(PokeApiService.pokemonUrl(id))
            add(PokeApiService.speciesUrl(id))
            add(PokeApiService.encountersUrl(id))
            evolutionUrl?.let(::add)
            addAll(PokemonFormsService.resourceUrlsFor(id))
        }
        PersistentApiCache.pinAll(resources)

        check(
            appContext.imageLoader.execute(
                ImageRequest.Builder(appContext)
                    .data(pokemon.spriteUrl)
                    .diskCacheKey("pokemon-offline-$id")
                    .memoryCacheKey("pokemon-offline-$id")
                    .build()
            ) is SuccessResult
        ){"Falha ao armazenar imagem #$id"}

        val formKeys=linkedSetOf<String>()
        val formSemaphore=Semaphore(FORM_DOWNLOAD_CONCURRENCY)
        coroutineScope {
            forms.filter{it.countsForLivingDex}.mapNotNull{form->
                val formId=form.pokemonId ?: return@mapNotNull null
                async {
                    formSemaphore.withPermit {
                        val normalUrl=form.spriteUrl
                            ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+formId+".png"
                        val shinyUrl=form.shinySpriteUrl
                            ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/shiny/"+formId+".png"
                        val normalKey=formArtworkKey(id,formId,form.formKey,false)
                        val shinyKey=formArtworkKey(id,formId,form.formKey,true)

                        val normal=async {
                            appContext.imageLoader.execute(
                                ImageRequest.Builder(appContext)
                                    .data(normalUrl)
                                    .diskCacheKey(normalKey)
                                    .memoryCacheKey(normalKey)
                                    .build()
                            )
                        }
                        val shiny=async {
                            appContext.imageLoader.execute(
                                ImageRequest.Builder(appContext)
                                    .data(shinyUrl)
                                    .diskCacheKey(shinyKey)
                                    .memoryCacheKey(shinyKey)
                                    .build()
                            )
                        }
                        check(normal.await() is SuccessResult){"Falha ao armazenar arte de forma #$id"}
                        check(shiny.await() is SuccessResult){"Falha ao armazenar arte Shiny de forma #$id"}

                        synchronized(formKeys){
                            formKeys += normalKey
                            formKeys += shinyKey
                        }
                    }
                }
            }.awaitAll()
        }

        SharedPokemonAssets(resources,formKeys).also{
            persistSharedPokemonAssets(id,it.resources,it.formArtworkKeys)
        }
    }

    suspend fun downloadGeneral(onProgress:(Progress)->Unit)=withContext(Dispatchers.IO){
        val appContext=requireNotNull(context)
        onProgress(Progress(0,1,"Mapeando jogos suportados"))

        val contexts=AppGameCatalog.adventureGames
            .flatMap{game->game.regions.mapNotNull{GameContext.fromSource(it.source)}}
            .distinctBy{it.pokedexSlug}

        val ids=linkedSetOf<Int>()
        contexts.forEachIndexed{index,ctx->
            onProgress(
                Progress(
                    index,
                    contexts.size.coerceAtLeast(1),
                    "Mapeando "+ctx.regionLabel
                )
            )
            GameDexService.loadGameDex(ctx).forEach{ids += it.nationalId}
        }

        listOf("move","ability","item").forEach{ReferenceCatalogService.load(it)}
        PersistentApiCache.pinAll(JourneyReadinessAudit.referenceCatalogUrls())

        val sortedIds=ids.sorted()
        prefs().edit()
            .putStringSet("general_manifest_ids",sortedIds.map(Int::toString).toSet())
            .putInt("general_count",sortedIds.size)
            .putInt("general_version",PACK_VERSION)
            .putBoolean("general_ready",false)
            .apply()

        val reusable=sortedIds.filter{sharedPokemonAssets(it)!=null}.toMutableSet()
        var completed=reusable.size
        prefs().edit().putInt("general_complete",completed).apply()

        onProgress(
            Progress(
                completed,
                sortedIds.size.coerceAtLeast(1),
                if(reusable.isNotEmpty())
                    reusable.size.toString()+" já disponíveis · "+(sortedIds.size-reusable.size)+" para baixar"
                else
                    "Iniciando biblioteca geral"
            )
        )

        val pending=sortedIds.filterNot{it in reusable}
        val semaphore=Semaphore(DOWNLOAD_CONCURRENCY)
        val failed=mutableListOf<Int>()
        val lock=Any()

        coroutineScope {
            pending.map{id->
                async {
                    semaphore.withPermit {
                        val ok=runCatching{
                            repeat(2){attempt->
                                try{
                                    downloadSharedPokemonAssets(id,appContext)
                                    return@runCatching true
                                }catch(t:Throwable){
                                    if(attempt==1) throw t
                                }
                            }
                            false
                        }.getOrDefault(false)

                        val done=synchronized(lock){
                            if(ok){
                                reusable += id
                                completed=reusable.size
                                prefs().edit().putInt("general_complete",completed).apply()
                            }else{
                                failed += id
                            }
                            completed
                        }
                        onProgress(Progress(done,sortedIds.size.coerceAtLeast(1),"Biblioteca geral"))
                    }
                }
            }.awaitAll()
        }

        if(failed.isNotEmpty()){
            prefs().edit()
                .putBoolean("general_ready",false)
                .putInt("general_complete",completed)
                .putInt("general_version",PACK_VERSION)
                .apply()
            error("Falha ao salvar "+failed.size+" Pokémon da biblioteca geral.")
        }

        val generalResources=sortedIds.flatMapTo(linkedSetOf()){id->
            prefs().getStringSet(sharedKey(id,"resource_urls"), emptySet()).orEmpty()
        } + JourneyReadinessAudit.referenceCatalogUrls()

        prefs().edit()
            .putBoolean("general_ready",true)
            .putInt("general_complete",sortedIds.size)
            .putInt("general_count",sortedIds.size)
            .putInt("general_version",PACK_VERSION)
            .putStringSet("general_resource_urls",generalResources.toSet())
            .apply()

        onProgress(Progress(sortedIds.size,sortedIds.size,"Biblioteca geral pronta"))
    }

    fun removeGeneral(){
        val ids=generalManifestIds()
        val gameIds=AppGameCatalog.games.asSequence()
            .flatMap{manifestIds(it.label).asSequence()}
            .toSet()
        val removable=ids-gameIds

        removable.forEach{id->
            val assets=sharedPokemonAssets(id)
            assets?.resources?.let{PersistentApiCache.unpinAll(it,deleteFiles=true)}
            context?.imageLoader?.diskCache?.let{disk->
                runCatching{disk.remove("pokemon-offline-$id")}
                assets?.formArtworkKeys.orEmpty().forEach{key->runCatching{disk.remove(key)}}
            }
            prefs().edit()
                .remove(sharedKey(id,"resource_urls"))
                .remove(sharedKey(id,"form_artwork_keys"))
                .apply()
        }

        val generalOnlyRefs=JourneyReadinessAudit.referenceCatalogUrls().filter{url->
            AppGameCatalog.games.none{game->url in resourceUrls(game.label)}
        }
        PersistentApiCache.unpinAll(generalOnlyRefs,deleteFiles=true)

        prefs().edit()
            .remove("general_ready")
            .remove("general_count")
            .remove("general_complete")
            .remove("general_version")
            .remove("general_manifest_ids")
            .remove("general_resource_urls")
            .remove("general_server_version")
            .apply()
    }

    fun status(gameLabel: String): PackStatus {
        val prefs = prefs()
        return PackStatus(
            downloaded = prefs.getBoolean(key(gameLabel, "ready"), false),
            downloadedAt = prefs.getLong(key(gameLabel, "at"), 0L),
            pokemonCount = prefs.getInt(key(gameLabel, "count"), 0),
            packVersion = prefs.getInt(key(gameLabel, "version"), 0),
            completeCount = prefs.getInt(key(gameLabel, "complete"), 0),
            reusedCount = prefs.getInt(key(gameLabel, "reused"), 0),
            downloadedNewCount = prefs.getInt(key(gameLabel, "downloaded_new"), 0)
        )
    }

    suspend fun download(game: AppGame, onProgress: (Progress) -> Unit) = withContext(Dispatchers.IO) {
        val appContext = requireNotNull(context)
        val contexts = game.regions.mapNotNull { GameContext.fromSource(it.source) }
        require(contexts.isNotEmpty()) { "Nenhuma região reconhecida para ${game.label}" }

        onProgress(Progress(0, 1, "Preparando biblioteca offline"))
        listOf("move", "ability", "item").forEach { kind ->
            ReferenceCatalogService.load(kind)
        }

        onProgress(Progress(0, 1, "Preparando ${game.label}"))
        val regionalDexes = contexts.mapIndexed { index, ctx ->
            onProgress(Progress(index, contexts.size.coerceAtLeast(1), "Baixando ${ctx.regionLabel}"))
            GameDexService.loadGameDex(ctx)
        }

        val baseResources=requiredBaseResourceUrls(game)
        PersistentApiCache.pinAll(baseResources)
        prefs().edit()
            .putStringSet(
                key(game.label, "resource_urls"),
                prefs().getStringSet(key(game.label, "resource_urls"), emptySet()).orEmpty() + baseResources
            )
            .apply()

        val ids = regionalDexes.flatten().map { it.nationalId }.distinct().sorted()
        val visualUrls = JourneyReadinessAudit.journeyVisualUrls(game.label)
        prefs().edit()
            .putStringSet(key(game.label, "manifest_ids"), ids.map(Int::toString).toSet())
            .putStringSet(key(game.label, "visual_urls"), visualUrls.toSet())
            .apply()

        onProgress(Progress(0, visualUrls.size.coerceAtLeast(1), "Salvando visuais da Jornada"))
        visualUrls.forEachIndexed { index, url ->
            val request = ImageRequest.Builder(appContext)
                .data(url)
                .diskCacheKey(journeyVisualKey(url))
                .memoryCacheKey(journeyVisualKey(url))
                .build()
            check(appContext.imageLoader.execute(request) is SuccessResult) {
                "Falha ao armazenar visual da Jornada"
            }
            onProgress(Progress(index + 1, visualUrls.size.coerceAtLeast(1), "Salvando visuais da Jornada"))
        }
        val total = ids.size.coerceAtLeast(1)
        val completedKey = key(game.label, "completed_ids")
        val storedCompleted = prefs().getStringSet(completedKey, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .filter { it in ids }
            .toMutableSet()
        val previousStatus = status(game.label)
        val previousAudit = audit(game.label)
        val repairing=previousStatus.downloaded && !previousAudit.valid
        val alreadyCompleted = if(repairing){
            // Rebuild a broken finished package, but still allow verified shared assets
            // from the global library to be attached again below.
            prefs().edit()
                .putStringSet(completedKey, emptySet())
                .putStringSet(key(game.label, "resource_urls"), baseResources)
                .putStringSet(key(game.label, "form_artwork_keys"), emptySet())
                .putInt(key(game.label, "complete"), 0)
                .putBoolean(key(game.label, "ready"), false)
                .apply()
            mutableSetOf()
        }else{
            storedCompleted
        }
        val lock = Any()
        val formArtworkKeys = (
            if(repairing) emptySet()
            else prefs().getStringSet(key(game.label, "form_artwork_keys"), emptySet()).orEmpty()
        ).toMutableSet()

        // Incremental shared library: attach Pokémon already complete from another game
        // without re-downloading their global data/artwork.
        val reusable = ids.asSequence()
            .filterNot { it in alreadyCompleted }
            .mapNotNull { id -> sharedPokemonAssets(id)?.let { id to it } }
            .toList()
        if(reusable.isNotEmpty()){
            val reusedResources=reusable.flatMapTo(linkedSetOf()){it.second.resources}
            val reusedFormKeys=reusable.flatMapTo(linkedSetOf()){it.second.formArtworkKeys}
            alreadyCompleted += reusable.map{it.first}
            formArtworkKeys += reusedFormKeys
            val currentResources=prefs().getStringSet(key(game.label, "resource_urls"), emptySet()).orEmpty()
            prefs().edit()
                .putStringSet(key(game.label, "resource_urls"), currentResources + reusedResources)
                .putStringSet(key(game.label, "form_artwork_keys"), formArtworkKeys.toSet())
                .putStringSet(completedKey, alreadyCompleted.map(Int::toString).toSet())
                .putInt(key(game.label, "complete"), alreadyCompleted.size)
                .putInt(key(game.label, "count"), ids.size)
                .putInt(key(game.label, "version"), PACK_VERSION)
                .apply()
            onProgress(
                Progress(
                    alreadyCompleted.size,
                    total,
                    "${reusable.size} Pokémon reaproveitados · ${ids.size-alreadyCompleted.size} para baixar"
                )
            )
        }

        val pendingIds = ids.filterNot { it in alreadyCompleted }
        val semaphore = Semaphore(permits = DOWNLOAD_CONCURRENCY)
        var completed = alreadyCompleted.size
        val failedIds = mutableListOf<Int>()
        var downloadedNew = 0

        onProgress(
            Progress(
                completed,
                total,
                when {
                    reusable.isNotEmpty() -> "${reusable.size} reaproveitados · ${pendingIds.size} para baixar"
                    completed > 0 -> "Retomando de $completed / $total"
                    else -> "Iniciando pacote offline"
                }
            )
        )

        suspend fun downloadPokemon(id:Int):Boolean {
            repeat(2){attempt->
                val assets=runCatching{
                    downloadSharedPokemonAssets(id,appContext)
                }.getOrNull()
                if(assets!=null){
                    synchronized(lock){
                        formArtworkKeys += assets.formArtworkKeys
                        val allResources=prefs().getStringSet(
                            key(game.label,"resource_urls"),
                            emptySet()
                        ).orEmpty() + assets.resources
                        prefs().edit()
                            .putStringSet(key(game.label,"resource_urls"),allResources)
                            .putStringSet(key(game.label,"form_artwork_keys"),formArtworkKeys.toSet())
                            .apply()
                    }
                    return true
                }
                if(attempt==0){
                    onProgress(Progress(completed,total,"Repetindo itens com falha…"))
                }
            }
            return false
        }

        coroutineScope {
            pendingIds.map { id ->
                async {
                    semaphore.withPermit {
                        val ok = downloadPokemon(id)
                        val done = synchronized(lock) {
                            if (!ok) {
                                failedIds += id
                            } else {
                                alreadyCompleted += id
                                downloadedNew++
                                prefs().edit()
                                    .putStringSet(completedKey, alreadyCompleted.map(Int::toString).toSet())
                                    .putInt(key(game.label, "complete"), alreadyCompleted.size)
                                    .putInt(key(game.label, "count"), ids.size)
                                    .putInt(key(game.label, "version"), PACK_VERSION)
                                    .apply()
                            }
                            completed = alreadyCompleted.size
                            completed
                        }
                        onProgress(Progress(done, total, "Dados + imagens dos Pokémon"))
                    }
                }
            }.awaitAll()
        }

        if (failedIds.isNotEmpty()) {
            prefs().edit()
                .putBoolean(key(game.label, "ready"), false)
                .putInt(key(game.label, "count"), ids.size)
                .putInt(key(game.label, "complete"), alreadyCompleted.size)
                .putInt(key(game.label, "version"), PACK_VERSION)
                .putInt(key(game.label, "reused"), reusable.size)
                .putInt(key(game.label, "downloaded_new"), downloadedNew)
                .apply()
            error("Falha ao salvar ${failedIds.size} de ${ids.size} Pokémon. Tente atualizar o pacote.")
        }

        prefs().edit()
            .putBoolean(key(game.label, "ready"), true)
            .putLong(key(game.label, "at"), System.currentTimeMillis())
            .putInt(key(game.label, "count"), ids.size)
            .putInt(key(game.label, "complete"), ids.size)
            .putInt(key(game.label, "version"), PACK_VERSION)
            .putInt(key(game.label, "reused"), reusable.size)
            .putInt(key(game.label, "downloaded_new"), downloadedNew)
            .putString(key(game.label, "regions"), contexts.joinToString("|") { it.pokedexSlug })
            .putStringSet(key(game.label, "manifest_ids"), ids.map(Int::toString).toSet())
            .putStringSet(completedKey, ids.map(Int::toString).toSet())
            .apply()

        onProgress(Progress(total, total, "Pacote offline verificado"))
    }

    private fun hasOfflineArtwork(id: Int): Boolean {
        val disk = context?.imageLoader?.diskCache ?: return false
        return runCatching {
            disk.openSnapshot("pokemon-offline-$id")?.use { true } ?: false
        }.getOrDefault(false)
    }

    private fun hasOfflineVisual(url: String): Boolean =
        hasOfflineCacheKey(journeyVisualKey(url))

    private fun hasOfflineCacheKey(cacheKey: String): Boolean {
        val disk = context?.imageLoader?.diskCache ?: return false
        return runCatching {
            disk.openSnapshot(cacheKey)?.use { true } ?: false
        }.getOrDefault(false)
    }

    private fun journeyVisualKey(url: String): String = "journey-offline-" + url.hashCode()

    fun remove(gameLabel: String) {
        val urls = resourceUrls(gameLabel)
        val ids = manifestIds(gameLabel)
        val visualUrls = prefs().getStringSet(key(gameLabel, "visual_urls"), emptySet()).orEmpty()
        val artworkKeys = formArtworkKeys(gameLabel)
        val sharedUrls = (
            AppGameCatalog.games.asSequence()
                .map { it.label }
                .filter { it != gameLabel }
                .flatMap { resourceUrls(it).asSequence() }
                .toSet()
        ) + prefs().getStringSet("general_resource_urls", emptySet()).orEmpty()
        val sharedIds = (
            AppGameCatalog.games.asSequence()
                .map { it.label }
                .filter { it != gameLabel }
                .flatMap { manifestIds(it).asSequence() }
                .toSet()
        ) + generalManifestIds()
        val sharedVisualUrls = AppGameCatalog.games.asSequence()
            .map { it.label }
            .filter { it != gameLabel }
            .flatMap { label ->
                prefs().getStringSet(key(label, "visual_urls"), emptySet()).orEmpty().asSequence()
            }
            .toSet()
        val sharedArtworkKeys = AppGameCatalog.games.asSequence()
            .map { it.label }
            .filter { it != gameLabel }
            .flatMap { formArtworkKeys(it).asSequence() }
            .toSet()
        PersistentApiCache.unpinAll(urls - sharedUrls, deleteFiles = true)
        context?.imageLoader?.diskCache?.let { disk ->
            (ids - sharedIds).forEach { id ->
                runCatching { disk.remove("pokemon-offline-$id") }
                prefs().edit()
                    .remove(sharedKey(id,"resource_urls"))
                    .remove(sharedKey(id,"form_artwork_keys"))
                    .apply()
            }
            (visualUrls - sharedVisualUrls).forEach { url -> runCatching { disk.remove(journeyVisualKey(url)) } }
            (artworkKeys - sharedArtworkKeys).forEach { cacheKey -> runCatching { disk.remove(cacheKey) } }
        }
        prefs().edit()
            .remove(key(gameLabel, "ready"))
            .remove(key(gameLabel, "at"))
            .remove(key(gameLabel, "count"))
            .remove(key(gameLabel, "complete"))
            .remove(key(gameLabel, "version"))
            .remove(key(gameLabel, "regions"))
            .remove(key(gameLabel, "completed_ids"))
            .remove(key(gameLabel, "manifest_ids"))
            .remove(key(gameLabel, "resource_urls"))
            .remove(key(gameLabel, "visual_urls"))
            .remove(key(gameLabel, "form_artwork_keys"))
            .remove(key(gameLabel, "reused"))
            .remove(key(gameLabel, "downloaded_new"))
            .remove(key(gameLabel, "running"))
            .remove(key(gameLabel, "runtime_done"))
            .remove(key(gameLabel, "runtime_total"))
            .remove(key(gameLabel, "runtime_label"))
            .apply()
    }

    private fun prefs() =
        requireNotNull(context) { "OfflineGamePackManager not initialized" }
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(game: String, suffix: String): String =
        game.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_') + "_" + suffix

    private fun sharedKey(id:Int,suffix:String):String =
        "shared_pokemon_"+id+"_"+suffix
}
