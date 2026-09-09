package com.otaviobarreto.pokedex.data

data class PokemonFilter(
    val query: String = "",
    val generation: Int? = null,
    val type: String? = null
)

object PokemonRepository {
    fun all(): List<PokemonSummary> = PokemonCatalog.all

    fun byId(id: Int): PokemonSummary? = PokemonCatalog.find(id)

    fun search(filter: PokemonFilter): List<PokemonSummary> {
        val normalizedQuery = filter.query.trim().removePrefix("#")

        return PokemonCatalog.all.filter { pokemon ->
            val matchesQuery = normalizedQuery.isBlank() ||
                pokemon.name.contains(normalizedQuery, ignoreCase = true) ||
                pokemon.id.toString() == normalizedQuery

            val matchesGeneration = filter.generation == null ||
                pokemon.generation == filter.generation

            val matchesType = filter.type == null ||
                pokemon.types.any { it.equals(filter.type, ignoreCase = true) }

            matchesQuery && matchesGeneration && matchesType
        }.sortedBy { it.id }
    }

    fun generations(): List<Int> =
        PokemonCatalog.all.map { it.generation }.distinct().sorted()

    fun types(): List<String> =
        PokemonCatalog.all.flatMap { it.types }.distinct().sorted()
}
