package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class EvolutionModernSpecialItemsTest {

    @Test
    fun applinRoutesAreContextualAndItemBased() {
        val swsh=GameContext.fromSource("Sword / Shield · Galar")
        val sv=GameContext.fromSource("Scarlet / Violet · Paldea")
        listOf(swsh,sv).forEach{context->
            assertTrue(EvolutionCuratedCatalog.rulesFor(841,context).single().requirement.contains("Tart Apple"))
            assertTrue(EvolutionCuratedCatalog.rulesFor(842,context).single().requirement.contains("Sweet Apple"))
        }
        assertTrue(EvolutionCuratedCatalog.rulesFor(1011,sv).single().requirement.contains("Syrupy Apple"))
        assertTrue(EvolutionCuratedCatalog.rulesFor(1011,swsh).isEmpty())
    }

    @Test
    fun sinisteaPreservesFormSpecificPot() {
        val context=GameContext.fromSource("Sword / Shield · Galar")
        val routes=EvolutionCuratedCatalog.rulesFor(855,context)
        assertEquals(2,routes.size)
        assertTrue(routes.any{it.requirement.contains("Phony") && it.requirement.contains("Cracked Pot")})
        assertTrue(routes.any{it.requirement.contains("Antique") && it.requirement.contains("Chipped Pot")})
    }

    @Test
    fun paldeaDlcSpecialItemsAreExplicit() {
        val context=GameContext.fromSource("Blueberry · Scarlet / Violet")
        val expected=mapOf(
            936 to "Auspicious Armor",
            937 to "Malicious Armor",
            1011 to "Syrupy Apple",
            1018 to "Metal Alloy"
        )
        expected.forEach{(target,item)->
            val rule=EvolutionCuratedCatalog.rulesFor(target,context).single()
            assertTrue("#$target",rule.requirement.contains(item))
            assertTrue("#$target",PokeApiService.EvolutionMethod.ITEM in PokeApiService.auditFallbackMethods(rule.requirement))
        }
    }

    @Test
    fun poltchageistFormUsesCorrectTeacup() {
        val context=GameContext.fromSource("Kitakami · Scarlet / Violet")
        val routes=EvolutionCuratedCatalog.rulesFor(1013,context)
        assertEquals(2,routes.size)
        assertTrue(routes.any{it.requirement.contains("Counterfeit") && it.requirement.contains("Unremarkable Teacup")})
        assertTrue(routes.any{it.requirement.contains("Artisan") && it.requirement.contains("Masterpiece Teacup")})
        routes.forEach{
            assertTrue(PokeApiService.EvolutionMethod.ITEM in PokeApiService.auditFallbackMethods(it.requirement))
        }
    }
}
