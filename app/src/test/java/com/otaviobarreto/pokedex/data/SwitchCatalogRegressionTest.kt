package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class SwitchCatalogRegressionTest {
    private val expected = setOf(
        "Pokémon Legends: Z-A",
        "Scarlet / Violet",
        "Sword / Shield",
        "Let's Go Pikachu / Eevee",
        "Legends Arceus",
        "Brilliant Diamond / Shining Pearl",
        "FireRed / LeafGreen",
        "Pokémon Champions"
    )

    @Test fun catalogMatchesCurrentCollectionCompatibleSwitchLineup() {
        assertEquals(expected, AppGameCatalog.games.map { it.label }.toSet())
    }

    @Test fun newSwitchContextsResolveToCurrentPokeApiDexes() {
        assertEquals("lumiose-city", GameContext.fromSource("Pokémon Legends: Z-A · Lumiose")?.pokedexSlug)
        assertEquals("hyperspace", GameContext.fromSource("Pokémon Legends: Z-A · Hyperspace")?.pokedexSlug)
        assertEquals("kanto", GameContext.fromSource("FireRed / LeafGreen · Kanto")?.pokedexSlug)
        assertEquals("champions", GameContext.fromSource("Pokémon Champions · Roster")?.pokedexSlug)
    }

    @Test fun legacyNonSwitchGamesAreNotInPrimaryCatalog() {
        val labels = AppGameCatalog.games.map { it.label }
        assertFalse("Black / White" in labels)
        assertFalse("X / Y" in labels)
        assertFalse("Omega Ruby / Alpha Sapphire" in labels)
    }
}
