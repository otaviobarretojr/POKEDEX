package com.otaviobarreto.pokedex.data

data class JourneyGameVisual(
    val heroPokemonIds: List<Int>
)

object JourneyGameVisualCatalog {
    fun forGame(gameLabel: String): JourneyGameVisual = visuals[gameLabel] ?: JourneyGameVisual(emptyList())

    private val visuals = mapOf(
        "Pokémon Legends: Z-A" to JourneyGameVisual(listOf(448)),
        "Scarlet / Violet" to JourneyGameVisual(listOf(1007, 1008)),
        "Sword / Shield" to JourneyGameVisual(listOf(888, 889)),
        "Let's Go Pikachu / Eevee" to JourneyGameVisual(listOf(25, 133)),
        "Legends Arceus" to JourneyGameVisual(listOf(493)),
        "Brilliant Diamond / Shining Pearl" to JourneyGameVisual(listOf(483, 484)),
        "FireRed / LeafGreen" to JourneyGameVisual(listOf(6, 3))
    )
}
