package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class EvolutionRareOutcomeTest {

    @Test
    fun wurmpleBranchesRemainPersonalityDriven() {
        val silcoon=EvolutionCuratedCatalog.rulesFor(266,null).single()
        val cascoon=EvolutionCuratedCatalog.rulesFor(268,null).single()
        assertTrue(silcoon.requirement.contains("personalidade",true))
        assertTrue(cascoon.requirement.contains("personalidade",true))
        assertTrue(PokeApiService.EvolutionMethod.ACTION in PokeApiService.auditFallbackMethods(silcoon.requirement))
        assertTrue(PokeApiService.EvolutionMethod.ACTION in PokeApiService.auditFallbackMethods(cascoon.requirement))
    }

    @Test
    fun shedinjaRequiresPartySpaceAndPokeball() {
        val rule=EvolutionCuratedCatalog.rulesFor(292,null).single()
        assertTrue(rule.requirement.contains("espaço vazio",true))
        assertTrue(rule.requirement.contains("Poké Ball",true))
        assertTrue(PokeApiService.EvolutionMethod.ACTION in PokeApiService.auditFallbackMethods(rule.requirement))
    }

    @Test
    fun alcremieRuleIsAvailableInSwShAndSVOnly() {
        val swsh=GameContext.fromSource("Sword / Shield · Galar")
        val sv=GameContext.fromSource("Scarlet / Violet · Blueberry")
        val hisui=GameContext.fromSource("Legends Arceus · Hisui")

        listOf(swsh,sv).forEach{context->
            val rule=EvolutionCuratedCatalog.rulesFor(869,context).single()
            val methods=PokeApiService.auditFallbackMethods(rule.requirement)
            assertTrue(PokeApiService.EvolutionMethod.ITEM in methods)
            assertTrue(PokeApiService.EvolutionMethod.ACTION in methods)
            assertTrue(PokeApiService.EvolutionMethod.TIME in methods)
        }
        assertTrue(EvolutionCuratedCatalog.rulesFor(869,hisui).isEmpty())
    }

    @Test
    fun rareFormsStayExplicitInScarletViolet() {
        val sv=GameContext.fromSource("Scarlet / Violet · Paldea")

        val maushold=EvolutionCuratedCatalog.rulesFor(925,sv).single()
        assertTrue(maushold.requirement.contains("Family of Three"))
        assertTrue(PokeApiService.EvolutionMethod.ACTION in PokeApiService.auditFallbackMethods(maushold.requirement))

        val dudunsparce=EvolutionCuratedCatalog.rulesFor(982,sv).single()
        assertTrue(dudunsparce.requirement.contains("Hyper Drill"))
        assertTrue(dudunsparce.requirement.contains("Three-Segment"))
        val methods=PokeApiService.auditFallbackMethods(dudunsparce.requirement)
        assertTrue(PokeApiService.EvolutionMethod.MOVE in methods)
        assertTrue(PokeApiService.EvolutionMethod.ACTION in methods)
    }
}
