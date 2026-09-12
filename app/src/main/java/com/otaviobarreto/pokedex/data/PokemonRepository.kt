package com.otaviobarreto.pokedex.data

data class PokemonFilter(
    val query: String = "",
    val generation: Int? = null,
    val type: String? = null
)

object PokemonRepository {
    private val detailedById = PokemonCatalog.all.associateBy { it.id }

    private val nationalDex: List<PokemonSummary> by lazy {
        NationalDexCatalog.all.map { species ->
            detailedById[species.id] ?: PokemonSummary(
                id = species.id,
                name = species.displayName,
                generation = species.generation,
                types = emptyList(),
                hp = 0,
                attack = 0,
                defense = 0,
                specialAttack = 0,
                specialDefense = 0,
                speed = 0,
                abilities = emptyList()
            )
        }
    }

    fun all(): List<PokemonSummary> = nationalDex

    fun byId(id: Int): PokemonSummary? =
        nationalDex.getOrNull(id - 1)?.takeIf { it.id == id }

    fun search(filter: PokemonFilter): List<PokemonSummary> {
        val normalizedQuery = filter.query.trim().removePrefix("#")

        return nationalDex.filter { pokemon ->
            val matchesQuery = normalizedQuery.isBlank() ||
                pokemon.name.contains(normalizedQuery, ignoreCase = true) ||
                pokemon.id.toString() == normalizedQuery ||
                pokemon.types.any { it.contains(normalizedQuery, ignoreCase = true) }

            val matchesGeneration = filter.generation == null ||
                pokemon.generation == filter.generation

            val matchesType = filter.type == null ||
                pokemon.types.any { it.equals(filter.type, ignoreCase = true) }

            matchesQuery && matchesGeneration && matchesType
        }
    }

    fun generations(): List<Int> =
        nationalDex.map { it.generation }.distinct().sorted()

    fun types(): List<String> =
        PokemonCatalog.all.flatMap { it.types }.distinct().sorted()
}
