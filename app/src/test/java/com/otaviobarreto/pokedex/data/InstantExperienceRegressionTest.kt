package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class InstantExperienceRegressionTest {
    @Test fun pokeApiCoreUrlsRemainStable() {
        assertEquals("https://pokeapi.co/api/v2/pokemon/25", PokeApiService.pokemonUrl(25))
        assertEquals("https://pokeapi.co/api/v2/pokemon-species/25", PokeApiService.speciesUrl(25))
        assertEquals("https://pokeapi.co/api/v2/pokemon/25/encounters", PokeApiService.encountersUrl(25))
    }

    @Test fun gameDexCacheUrlUsesContextSlug() {
        val ctx = GameContext.fromSource("Scarlet / Violet · Paldea")
        assertNotNull(ctx)
        assertEquals("https://pokeapi.co/api/v2/pokedex/paldea", GameDexService.cacheUrl(ctx!!))
    }
}
