package com.otaviobarreto.pokedex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object PokedexDataStore {
    @Volatile private var nationalDexCache: List<PokeApiService.DexIndexEntry>? = null
    private val nationalDexLock = Any()
    private val pokemonCache = ConcurrentHashMap<Int, PokeApiService.RemotePokemonDetail>()
    private val speciesCache = ConcurrentHashMap<Int, PokeApiService.SpeciesInfo>()
    private val evolutionCache = ConcurrentHashMap<String, List<PokeApiService.EvolutionStage>>()
    private val encounterCache = ConcurrentHashMap<Int, List<PokeApiService.EncounterLocation>>()
    private val typeCache = ConcurrentHashMap<String, Set<Int>>()
    private val prefetchSemaphore = Semaphore(permits = 6)

    fun nationalDex(): List<PokeApiService.DexIndexEntry> {
        nationalDexCache?.let { return it }
        synchronized(nationalDexLock) {
            nationalDexCache?.let { return it }
            return PokeApiService.loadNationalDex().also { nationalDexCache = it }
        }
    }

    fun pokemon(id: Int): PokeApiService.RemotePokemonDetail =
        pokemonCache.computeIfAbsent(id) {
            PersistentApiCache.promoteLocal(PokeApiService.pokemonUrl(it))
            PokeApiService.loadPokemon(it)
        }

    fun species(id: Int): PokeApiService.SpeciesInfo =
        speciesCache.computeIfAbsent(id) {
            PersistentApiCache.promoteLocal(PokeApiService.speciesUrl(it))
            PokeApiService.loadSpecies(it)
        }

    fun evolutions(url: String): List<PokeApiService.EvolutionStage> =
        evolutionCache.computeIfAbsent(url) { PokeApiService.loadEvolutionChain(it) }

    fun encounters(id: Int): List<PokeApiService.EncounterLocation> =
        encounterCache.computeIfAbsent(id) { PokeApiService.loadEncounters(it) }

    fun pokemonIdsForType(type: String): Set<Int> {
        val key = type.lowercase()
        return typeCache.computeIfAbsent(key) { PokeApiService.loadPokemonIdsForType(type) }
    }

    fun cachedNationalDex(): List<PokeApiService.DexIndexEntry>? = nationalDexCache

    fun cachedIndexEntry(id: Int): PokeApiService.DexIndexEntry? =
        nationalDexCache?.firstOrNull { it.id == id }

    fun cachedPokemon(id: Int) = pokemonCache[id]
    fun cachedSpecies(id: Int) = speciesCache[id]
    fun cachedEncounters(id: Int) = encounterCache[id]
    fun cachedEvolutions(url: String?) = url?.let { evolutionCache[it] }

    data class CacheStats(
        val pokemon:Int,
        val species:Int,
        val evolutions:Int,
        val encounters:Int,
        val types:Int
    ) {
        val total:Int get() = pokemon + species + evolutions + encounters + types
    }

    fun cacheStats(): CacheStats = CacheStats(
        pokemon=pokemonCache.size,
        species=speciesCache.size,
        evolutions=evolutionCache.size,
        encounters=encounterCache.size,
        types=typeCache.size
    )

    /**
     * Warm only the data required to render the detail screen hero + Info/Stats/Moves.
     * Evolution and encounter payloads are deliberately deferred so scrolling through
     * a list cannot saturate the network/cache with expensive secondary requests.
     */
    suspend fun prefetchCoreDetails(id: Int) {
        if (pokemonCache.containsKey(id) && speciesCache.containsKey(id)) return
        withContext(Dispatchers.IO) {
            prefetchSemaphore.withPermit {
                runCatching {
                    coroutineScope {
                        val pokemonJob = async { pokemon(id) }
                        val speciesJob = async { species(id) }
                        val formsJob = async { runCatching { PokemonFormsService.load(id) } }
                        pokemonJob.await()
                        speciesJob.await()
                        formsJob.await()
                    }
                }
            }
        }
    }

    /**
     * Compatibility entry point used by list/grid screens.
     * Since v1.1 this intentionally performs only the core warm-up.
     */
    suspend fun prefetchDetails(id: Int) = prefetchCoreDetails(id)

    suspend fun prefetchDetailWindow(centerId:Int, radius:Int=1) {
        val ids=(centerId-radius..centerId+radius)
            .filter { it in 1..PokeApiService.MAX_NATIONAL_DEX_ID }
        coroutineScope {
            ids.map { id -> async { prefetchCoreDetails(id) } }.awaitAll()
        }
    }

    suspend fun prefetchBoxWindow(
        dex:List<GameDexService.GameDexEntry>,
        page:Int,
        pageSize:Int=30
    ) {
        if(dex.isEmpty()) return
        val pageCount=((dex.size+pageSize-1)/pageSize).coerceAtLeast(1)
        val targetPages=listOf(page-1,page,page+1)
            .filter { it in 0 until pageCount }
            .distinct()
        val ids=targetPages.flatMap { target ->
            dex.drop(target*pageSize).take(pageSize).map { it.nationalId }
        }.distinct()
        ids.chunked(8).forEach { chunk ->
            coroutineScope {
                chunk.map { id -> async { prefetchCoreDetails(id) } }.awaitAll()
            }
        }
    }

    suspend fun hydratePinned(ids: Collection<Int>) {
        ids.distinct().take(24).forEach { id ->
            val pokemonPinned = PersistentApiCache.isPinned(PokeApiService.pokemonUrl(id))
            val speciesPinned = PersistentApiCache.isPinned(PokeApiService.speciesUrl(id))
            if (pokemonPinned && speciesPinned) prefetchCoreDetails(id)
        }
    }

    /**
     * Full warm-up used by offline packs and explicit background preparation.
     */
    suspend fun prefetchFullDetails(id: Int) {
        withContext(Dispatchers.IO) {
            prefetchSemaphore.withPermit {
                runCatching {
                    coroutineScope {
                        val pokemonJob = async { pokemon(id) }
                        val speciesJob = async { species(id) }
                        val encounterJob = async { encounters(id) }
                        pokemonJob.await()
                        val s = speciesJob.await()
                        val evolutionJob = async {
                            s.evolutionChainUrl?.let { evolutions(it) } ?: emptyList()
                        }
                        encounterJob.await()
                        evolutionJob.await()
                    }
                }
            }
        }
    }

    fun clearSessionCache() {
        nationalDexCache = null
        pokemonCache.clear()
        speciesCache.clear()
        evolutionCache.clear()
        encounterCache.clear()
        typeCache.clear()
    }
}
