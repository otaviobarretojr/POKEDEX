package com.otaviobarreto.pokedex.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

object OfflineGamePackManager {
    private const val PREFS = "offline_game_packs_v1"
    private var context: Context? = null

    data class PackStatus(val downloaded: Boolean, val downloadedAt: Long = 0L, val pokemonCount: Int = 0)
    data class Progress(val done: Int, val total: Int, val label: String) {
        val fraction: Float get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    }

    fun initialize(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
    }

    fun status(gameLabel: String): PackStatus {
        val prefs = prefs()
        return PackStatus(
            downloaded = prefs.getBoolean(key(gameLabel, "ready"), false),
            downloadedAt = prefs.getLong(key(gameLabel, "at"), 0L),
            pokemonCount = prefs.getInt(key(gameLabel, "count"), 0)
        )
    }

    suspend fun download(game: AppGame, onProgress: (Progress) -> Unit) = withContext(Dispatchers.IO) {
        val contexts = game.regions.mapNotNull { GameContext.fromSource(it.source) }
        require(contexts.isNotEmpty()) { "Nenhuma região reconhecida para ${game.label}" }

        onProgress(Progress(0, 1, "Preparando ${game.label}"))
        val regionalDexes = contexts.mapIndexed { index, ctx ->
            onProgress(Progress(index, contexts.size.coerceAtLeast(1), "Baixando ${ctx.regionLabel}"))
            GameDexService.loadGameDex(ctx)
        }

        val ids = regionalDexes.flatten().map { it.nationalId }.distinct().sorted()
        val total = ids.size.coerceAtLeast(1)
        val semaphore = Semaphore(permits = 4)
        var completed = 0
        var failures = 0
        val lock = Any()

        coroutineScope {
            ids.map { id ->
                async {
                    semaphore.withPermit {
                        val ok = runCatching {\n                            val species = PokedexDataStore.species(id)\n                            PokedexDataStore.pokemon(id)\n                            PokedexDataStore.encounters(id)\n                            species.evolutionChainUrl?.let { PokedexDataStore.evolutions(it) }\n                        }.isSuccess\n                        val done = synchronized(lock) {\n                            if (!ok) failures++\n                            ++completed\n                        }
                        onProgress(Progress(done, total, "Salvando dados dos Pokémon"))
                    }
                }
            }.awaitAll()
        }

        if (failures > 0) error("Falha ao salvar " + failures + " de " + ids.size + " Pokémon. Tente novamente.")\n\n        prefs().edit()
            .putBoolean(key(game.label, "ready"), true)
            .putLong(key(game.label, "at"), System.currentTimeMillis())
            .putInt(key(game.label, "count"), ids.size)
            .apply()

        onProgress(Progress(total, total, "Disponível offline"))
    }

    fun remove(gameLabel: String) {
        prefs().edit()
            .remove(key(gameLabel, "ready"))
            .remove(key(gameLabel, "at"))
            .remove(key(gameLabel, "count"))
            .apply()
    }

    private fun prefs() = requireNotNull(context) { "OfflineGamePackManager not initialized" }
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(game: String, suffix: String): String =
        game.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_') + "_" + suffix
}
