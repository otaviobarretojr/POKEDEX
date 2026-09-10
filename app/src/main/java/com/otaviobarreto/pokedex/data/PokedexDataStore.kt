package com.otaviobarreto.pokedex.data

import kotlinx.coroutines.Dispatchers
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
        pokemonCache.computeIfAbsent(id) { PokeApiService.loadPokemon(it) }

    fun species(id: Int): PokeApiService.SpeciesInfo =
        speciesCache.computeIfAbsent(id) { PokeApiService.loadSpecies(it) }

    fun evolutions(url: String): List<PokeApiService.EvolutionStage> =
        evolutionCache.computeIfAbsent(url) { PokeApiService.loadEvolutionChain(it) }

    fun encounters(id: Int): List<PokeApiService.EncounterLocation> =
        encounterCache.computeIfAbsent(id) { PokeApiService.loadEncounters(it) }

    fun pokemonIdsForType(type: String): Set<Int> {
        val key = type.lowercase()
        return typeCache.computeIfAbsent(key) { PokeApiService.loadPokemonIdsForType(type) }
    }

    fun cachedPokemon(id: Int) = pokemonCache[id]
    fun cachedSpecies(id: Int) = speciesCache[id]
    fun cachedEncounters(id: Int) = encounterCache[id]
    fun cachedEvolutions(url: String?) = url?.let { evolutionCache[it] }

    suspend fun prefetchDetails(id: Int) {
        if (pokemonCache.containsKey(id) && speciesCache.containsKey(id) && encounterCache.containsKey(id)) return
        withContext(Dispatchers.IO) {
            prefetchSemaphore.withPermit {
                runCatching {
                    pokemon(id)
                    val s = species(id)
                    s.evolutionChainUrl?.let(::evolutions)
                    encounters(id)
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
