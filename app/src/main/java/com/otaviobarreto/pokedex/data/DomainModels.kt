package com.otaviobarreto.pokedex.data

data class LocalizedName(
    val language: String,
    val value: String
)

data class PokemonStats(
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val specialAttack: Int,
    val specialDefense: Int,
    val speed: Int
)

data class PokemonRecord(
    val id: Int,
    val generation: Int,
    val names: List<LocalizedName>,
    val primaryTypeId: Int,
    val secondaryTypeId: Int?,
    val stats: PokemonStats,
    val versionGroupIds: List<Int>,
    val alternateFormIds: List<Int> = emptyList(),
    val megaId: Int? = null
)

data class AbilityRecord(
    val id: Int,
    val generation: Int,
    val names: List<LocalizedName>,
    val description: String
)

data class MoveRecord(
    val id: Int,
    val generation: Int,
    val names: List<LocalizedName>,
    val description: String,
    val typeId: Int,
    val damageClassId: Int,
    val power: Int?,
    val accuracy: Int?,
    val pp: Int?
)

data class ItemRecord(
    val id: Int,
    val categoryId: Int,
    val internalName: String,
    val names: List<LocalizedName>,
    val description: String
)

data class EvolutionLink(
    val fromPokemonId: Int,
    val toPokemonId: Int,
    val trigger: String,
    val minimumLevel: Int? = null,
    val itemId: Int? = null
)

data class GameVersionRecord(
    val id: Int,
    val name: String,
    val versionGroupId: Int,
    val generation: Int
)

data class EncounterRecord(
    val pokemonId: Int,
    val versionId: Int,
    val locationId: Int,
    val method: String,
    val minimumLevel: Int? = null,
    val maximumLevel: Int? = null
)
