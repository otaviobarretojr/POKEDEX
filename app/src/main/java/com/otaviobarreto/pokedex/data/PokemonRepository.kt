package com.otaviobarreto.pokedex.data

data class PokemonFilter(
    val query: String = "",
    val generation: Int? = null,
    val type: String? = null
)

object PokemonRepository {
    private val nationalDex: List<PokemonSummary> by lazy {
        NationalDexCatalog.all.map { species ->
            val metadata = PokemonMetadataCatalog.byId(species.id)
            PokemonSummary(
                id = species.id,
                name = species.displayName,
                generation = species.generation,
                types = metadata?.types.orEmpty(),
                hp = metadata?.hp ?: 0,
                attack = metadata?.attack ?: 0,
                defense = metadata?.defense ?: 0,
                specialAttack = metadata?.specialAttack ?: 0,
                specialDefense = metadata?.specialDefense ?: 0,
                speed = metadata?.speed ?: 0,
                abilities = metadata?.abilities.orEmpty()
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
                pokemon.types.any { it.contains(normalizedQuery, ignoreCase = true) } ||
                pokemon.abilities.any { it.contains(normalizedQuery, ignoreCase = true) }

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
        nationalDex.flatMap { it.types }.distinct().sorted()
}
