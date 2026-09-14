package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class EvolutionFeebasContextTest {

    @Test
    fun bdspUsesBeautyRoute() {
        val context=GameContext.fromSource("Brilliant Diamond / Shining Pearl · Sinnoh")
        val routes=EvolutionCuratedCatalog.rulesFor(350,context)
        assertEquals(1,routes.size)
        assertTrue(routes.single().requirement.contains("Beauty 170"))
    }

    @Test
    fun orasKeepsBeautyAndPrismScaleAlternatives() {
        val context=GameContext.fromSource("Omega Ruby / Alpha Sapphire · Hoenn")
        val routes=EvolutionCuratedCatalog.rulesFor(350,context)
        assertEquals(2,routes.size)
        assertTrue(routes.any{it.requirement.contains("Beauty 170")})
        assertTrue(routes.any{it.requirement.contains("Prism Scale")})
    }

    @Test
    fun modernNonContestGamesPreferPrismScaleTrade() {
        listOf(
            "Black / White · Unova",
            "X / Y · Kalos Central",
            "Sword / Shield · Galar",
            "Scarlet / Violet · Paldea"
        ).forEach{source->
            val context=GameContext.fromSource(source)
            val routes=EvolutionCuratedCatalog.rulesFor(350,context)
            assertEquals(source,1,routes.size)
            assertTrue(source,routes.single().requirement.contains("Prism Scale"))
            val methods=PokeApiService.auditFallbackMethods(routes.single().requirement)
            assertTrue(PokeApiService.EvolutionMethod.TRADE in methods)
            assertTrue(PokeApiService.EvolutionMethod.ITEM in methods)
        }
    }
}
