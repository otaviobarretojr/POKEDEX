package com.otaviobarreto.pokedex.data

/**
 * Central read-through cache for the current online-backed phase.
 * It prevents repeated network calls during the same app session and gives
 * every feature (Pokédex, Living Dex, Boxes and Teams) one shared entry point.
 * A persistent Room implementation can replace the maps later without
 * changing the UI contract.
 */
object PokedexDataStore {
    private var nationalDexCache: List<PokeApiService.DexIndexEntry>? = null
    private val pokemonCache = mutableMapOf<Int, PokeApiService.RemotePokemonDetail>()
    private val speciesCache = mutableMapOf<Int, PokeApiService.SpeciesInfo>()
    private val evolutionCache = mutableMapOf<String, List<PokeApiService.EvolutionStage>>()
    private val encounterCache = mutableMapOf<Int, List<PokeApiService.EncounterLocation>>()
    private val typeCache = mutableMapOf<String, Set<Int>>()

    @Synchronized
    fun nationalDex(): List<PokeApiService.DexIndexEntry> {
        nationalDexCache?.let { return it }
        return PokeApiService.loadNationalDex().also { nationalDexCache = it }
    }

    @Synchronized
    fun pokemon(id: Int): PokeApiService.RemotePokemonDetail =
        pokemonCache[id] ?: PokeApiService.loadPokemon(id).also { pokemonCache[id] = it }

    @Synchronized
    fun species(id: Int): PokeApiService.SpeciesInfo =
        speciesCache[id] ?: PokeApiService.loadSpecies(id).also { speciesCache[id] = it }

    @Synchronized
    fun evolutions(url: String): List<PokeApiService.EvolutionStage> =
        evolutionCache[url] ?: PokeApiService.loadEvolutionChain(url).also { evolutionCache[url] = it }

    @Synchronized
    fun encounters(id: Int): List<PokeApiService.EncounterLocation> =
        encounterCache[id] ?: PokeApiService.loadEncounters(id).also { encounterCache[id] = it }

    @Synchronized
    fun pokemonIdsForType(type: String): Set<Int> {
        val key = type.lowercase()
        return typeCache[key] ?: PokeApiService.loadPokemonIdsForType(type).also { typeCache[key] = it }
    }

    @Synchronized
    fun clearSessionCache() {
        nationalDexCache = null
        pokemonCache.clear()
        speciesCache.clear()
        evolutionCache.clear()
        encounterCache.clear()
        typeCache.clear()
    }
}
