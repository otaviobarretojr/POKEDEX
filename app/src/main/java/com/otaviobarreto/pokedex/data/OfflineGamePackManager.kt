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
    private const val PACK_VERSION = 18
    private var context: Context? = null

    data class PackStatus(
        val downloaded: Boolean,
        val downloadedAt: Long = 0L,
        val pokemonCount: Int = 0,
        val packVersion: Int = 0,
        val completeCount: Int = 0
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

    fun initialize(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
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
        val resources = p.getStringSet(key(gameLabel, "resource_urls"), emptySet()).orEmpty()
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

    fun status(gameLabel: String): PackStatus {
        val prefs = prefs()
        return PackStatus(
            downloaded = prefs.getBoolean(key(gameLabel, "ready"), false),
            downloadedAt = prefs.getLong(key(gameLabel, "at"), 0L),
            pokemonCount = prefs.getInt(key(gameLabel, "count"), 0),
            packVersion = prefs.getInt(key(gameLabel, "version"), 0),
            completeCount = prefs.getInt(key(gameLabel, "complete"), 0)
        )
    }

    suspend fun download(game: AppGame, onProgress: (Progress) -> Unit) = withContext(Dispatchers.IO) {
        val appContext = requireNotNull(context)
        val contexts = game.regions.mapNotNull { GameContext.fromSource(it.source) }
        require(contexts.isNotEmpty()) { "Nenhuma região reconhecida para ${game.label}" }

        onProgress(Progress(0, 1, "Preparando biblioteca offline"))
        listOf("move", "ability", "item").forEach { kind ->
            runCatching { ReferenceCatalogService.load(kind) }
        }
        PersistentApiCache.pinAll(JourneyReadinessAudit.referenceCatalogUrls())

        onProgress(Progress(0, 1, "Preparando ${game.label}"))
        val regionalDexes = contexts.mapIndexed { index, ctx ->
            onProgress(Progress(index, contexts.size.coerceAtLeast(1), "Baixando ${ctx.regionLabel}"))
            GameDexService.loadGameDex(ctx).also { PersistentApiCache.pin(GameDexService.cacheUrl(ctx)) }
        }

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
        val alreadyCompleted = prefs().getStringSet(completedKey, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .filter { it in ids }
            .toMutableSet()
        val pendingIds = ids.filterNot { it in alreadyCompleted }
        val semaphore = Semaphore(permits = 10)
        var completed = alreadyCompleted.size
        val failedIds = mutableListOf<Int>()
        val formArtworkKeys = prefs().getStringSet(key(game.label, "form_artwork_keys"), emptySet()).orEmpty().toMutableSet()
        val lock = Any()

        onProgress(
            Progress(
                completed,
                total,
                if (completed > 0) "Retomando de $completed / $total" else "Iniciando pacote offline"
            )
        )

        suspend fun downloadPokemon(id: Int): Boolean {
            repeat(2) { attempt ->
                val ok = runCatching {
                    coroutineScope {
                        val pokemonJob = async { PokedexDataStore.pokemon(id) }
                        val speciesJob = async { PokedexDataStore.species(id) }
                        val encounterJob = async { PokedexDataStore.encounters(id) }
                        val formsJob = async { PokemonFormsService.collectible(id) }
                        val pokemon = pokemonJob.await()
                        val species = speciesJob.await()
                        val evolutionUrl = species.evolutionChainUrl
                        evolutionUrl?.let { PokedexDataStore.evolutions(it) }
                        encounterJob.await()
                        val forms = formsJob.await()

                        val resourceUrls = buildSet {
                            add(PokeApiService.pokemonUrl(id))
                            add(PokeApiService.speciesUrl(id))
                            add(PokeApiService.encountersUrl(id))
                            evolutionUrl?.let(::add)
                            addAll(PokemonFormsService.resourceUrlsFor(id))
                        }
                        PersistentApiCache.pinAll(resourceUrls)
                        synchronized(lock) {
                            val allResources = prefs().getStringSet(key(game.label, "resource_urls"), emptySet()).orEmpty() + resourceUrls
                            prefs().edit().putStringSet(key(game.label, "resource_urls"), allResources).apply()
                        }

                        val request = ImageRequest.Builder(appContext)
                            .data(pokemon.spriteUrl)
                            .diskCacheKey("pokemon-offline-$id")
                            .memoryCacheKey("pokemon-offline-$id")
                            .build()
                        check(appContext.imageLoader.execute(request) is SuccessResult) {
                            "Falha ao armazenar imagem #$id"
                        }

                        forms.filter { it.countsForLivingDex }.forEach { form ->
                            val formId = form.pokemonId ?: return@forEach
                            val normalUrl = form.spriteUrl
                                ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" + formId + ".png"
                            val shinyUrl = form.shinySpriteUrl
                                ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/shiny/" + formId + ".png"
                            val normalKey = formArtworkKey(id, formId, form.formKey, false)
                            val shinyKey = formArtworkKey(id, formId, form.formKey, true)

                            check(
                                appContext.imageLoader.execute(
                                    ImageRequest.Builder(appContext)
                                        .data(normalUrl)
                                        .diskCacheKey(normalKey)
                                        .memoryCacheKey(normalKey)
                                        .build()
                                ) is SuccessResult
                            ) { "Falha ao armazenar arte de forma #$id" }

                            check(
                                appContext.imageLoader.execute(
                                    ImageRequest.Builder(appContext)
                                        .data(shinyUrl)
                                        .diskCacheKey(shinyKey)
                                        .memoryCacheKey(shinyKey)
                                        .build()
                                ) is SuccessResult
                            ) { "Falha ao armazenar arte Shiny de forma #$id" }

                            synchronized(lock) {
                                formArtworkKeys += normalKey
                                formArtworkKeys += shinyKey
                                prefs().edit()
                                    .putStringSet(key(game.label, "form_artwork_keys"), formArtworkKeys.toSet())
                                    .apply()
                            }
                        }
                    }
                }.isSuccess
                if (ok) return true
                if (attempt == 0) onProgress(Progress(completed, total, "Repetindo itens com falha…"))
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
                .apply()
            error("Falha ao salvar ${failedIds.size} de ${ids.size} Pokémon. Tente atualizar o pacote.")
        }

        prefs().edit()
            .putBoolean(key(game.label, "ready"), true)
            .putLong(key(game.label, "at"), System.currentTimeMillis())
            .putInt(key(game.label, "count"), ids.size)
            .putInt(key(game.label, "complete"), ids.size)
            .putInt(key(game.label, "version"), PACK_VERSION)
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
        val sharedUrls = AppGameCatalog.games.asSequence()
            .map { it.label }
            .filter { it != gameLabel }
            .flatMap { resourceUrls(it).asSequence() }
            .toSet()
        val sharedIds = AppGameCatalog.games.asSequence()
            .map { it.label }
            .filter { it != gameLabel }
            .flatMap { manifestIds(it).asSequence() }
            .toSet()
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
            (ids - sharedIds).forEach { id -> runCatching { disk.remove("pokemon-offline-$id") } }
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
}
