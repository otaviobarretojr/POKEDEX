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
    private const val PACK_VERSION = 2
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

    fun enqueue(gameLabel: String) {
        val appContext = requireNotNull(context)
        val intent = android.content.Intent(appContext, OfflineGameDownloadService::class.java)
            .putExtra(OfflineGameDownloadService.EXTRA_GAME, gameLabel)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) appContext.startForegroundService(intent)
        else appContext.startService(intent)
    }

    fun runtimeProgress(gameLabel: String): Progress? {
        val p = prefs()
        if (!p.getBoolean(key(gameLabel, "running"), false)) return null
        return Progress(
            p.getInt(key(gameLabel, "runtime_done"), 0),
            p.getInt(key(gameLabel, "runtime_total"), 1),
            p.getString(key(gameLabel, "runtime_label"), "Preparando download…") ?: "Preparando download…"
        )
    }

    fun setRuntimeProgress(gameLabel: String, progress: Progress?, running: Boolean) {
        val edit = prefs().edit().putBoolean(key(gameLabel, "running"), running)
        if (progress != null) {
            edit.putInt(key(gameLabel, "runtime_done"), progress.done)
                .putInt(key(gameLabel, "runtime_total"), progress.total)
                .putString(key(gameLabel, "runtime_label"), progress.label)
        }
        edit.apply()
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

        onProgress(Progress(0, 1, "Preparando ${game.label}"))
        val regionalDexes = contexts.mapIndexed { index, ctx ->
            onProgress(Progress(index, contexts.size.coerceAtLeast(1), "Baixando ${ctx.regionLabel}"))
            GameDexService.loadGameDex(ctx)
        }

        val ids = regionalDexes.flatten().map { it.nationalId }.distinct().sorted()
        val total = ids.size.coerceAtLeast(1)
        val semaphore = Semaphore(permits = 6)
        var completed = 0
        val failedIds = mutableListOf<Int>()
        val lock = Any()

        suspend fun downloadPokemon(id: Int): Boolean {
            repeat(2) { attempt ->
                val ok = runCatching {
                    coroutineScope {
                        val pokemonJob = async { PokedexDataStore.pokemon(id) }
                        val speciesJob = async { PokedexDataStore.species(id) }
                        val encounterJob = async { PokedexDataStore.encounters(id) }
                        val pokemon = pokemonJob.await()
                        val species = speciesJob.await()
                        species.evolutionChainUrl?.let { PokedexDataStore.evolutions(it) }
                        encounterJob.await()

                        val request = ImageRequest.Builder(appContext)
                            .data(pokemon.spriteUrl)
                            .diskCacheKey("pokemon-offline-$id")
                            .memoryCacheKey("pokemon-offline-$id")
                            .build()
                        check(appContext.imageLoader.execute(request) is SuccessResult) {
                            "Falha ao armazenar imagem #$id"
                        }
                    }
                }.isSuccess
                if (ok) return true
                if (attempt == 0) onProgress(Progress(completed, total, "Repetindo itens com falha…"))
            }
            return false
        }

        coroutineScope {
            ids.map { id ->
                async {
                    semaphore.withPermit {
                        val ok = downloadPokemon(id)
                        val done = synchronized(lock) {
                            if (!ok) failedIds += id
                            ++completed
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
                .putInt(key(game.label, "complete"), ids.size - failedIds.size)
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
            .apply()

        onProgress(Progress(total, total, "Pacote offline verificado"))
    }

    fun remove(gameLabel: String) {
        prefs().edit()
            .remove(key(gameLabel, "ready"))
            .remove(key(gameLabel, "at"))
            .remove(key(gameLabel, "count"))
            .remove(key(gameLabel, "complete"))
            .remove(key(gameLabel, "version"))
            .remove(key(gameLabel, "regions"))
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
