package com.otaviobarreto.pokedex.data

data class ScarletVioletAudit(
    val valid: Boolean,
    val regions: List<String>,
    val slugs: List<String>,
    val missingSources: List<String>
)

object JourneyReadinessAudit {
    private val scarletExpected = linkedMapOf(
        "Paldea" to "paldea",
        "Kitakami" to "kitakami",
        "Blueberry" to "blueberry"
    )

    fun scarletViolet(): ScarletVioletAudit {
        val game = AppGameCatalog.adventureGames.firstOrNull { it.label == "Scarlet / Violet" }
            ?: return ScarletVioletAudit(false, emptyList(), emptyList(), scarletExpected.keys.toList())

        val resolved = game.regions.mapNotNull { region ->
            GameContext.fromSource(region.source)?.let { region.label to it.pokedexSlug }
        }
        val resolvedMap = resolved.toMap()
        val missing = scarletExpected.filter { (region, slug) -> resolvedMap[region] != slug }.keys.toList()
        return ScarletVioletAudit(
            valid = missing.isEmpty() && resolvedMap.size == scarletExpected.size,
            regions = resolvedMap.keys.toList(),
            slugs = resolvedMap.values.toList(),
            missingSources = missing
        )
    }

    fun allAdventureContexts(): List<GameContext> =
        AppGameCatalog.adventureGames
            .flatMap { game -> game.regions.mapNotNull { GameContext.fromSource(it.source) } }
            .distinctBy { it.pokedexSlug }

    fun journeyVisualUrls(gameLabel: String): List<String> {
        val covers = if (gameLabel == "Scarlet / Violet") emptyList() else GameCoverCatalog.coversFor(gameLabel)
        val heroes = JourneyGameVisualCatalog.forGame(gameLabel).heroPokemonIds.map { id ->
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
        }
        return (covers + heroes).distinct()
    }

    fun referenceCatalogUrls(): List<String> = listOf(
        "https://pokeapi.co/api/v2/move?limit=2500",
        "https://pokeapi.co/api/v2/ability?limit=2500",
        "https://pokeapi.co/api/v2/item?limit=2500"
    )
}
