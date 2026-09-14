package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class EvolutionRuleCatalogTest {

    @Test
    fun plainLevelHasDedicatedFilter() {
        val rule=ContextualEvolutionRule(
            sourcePokemonId=909,
            targetPokemonId=910,
            methods=setOf(PokeApiService.EvolutionMethod.LEVEL),
            summary="Nível 16",
            rawRequirement="Subir ao nível 16"
        )
        assertEquals("LEVEL",EvolutionRuleCatalog.filterBucket(rule))
        assertEquals(PokeApiService.EvolutionMethod.LEVEL,EvolutionRuleCatalog.primaryMethod(rule.methods))
    }

    @Test
    fun targetRepresentsPendingEvolution() {
        val rule=ContextualEvolutionRule(
            sourcePokemonId=909,
            targetPokemonId=910,
            methods=setOf(PokeApiService.EvolutionMethod.LEVEL),
            summary="Nível 16",
            rawRequirement="Subir ao nível 16"
        )
        assertEquals(909,rule.sourcePokemonId)
        assertEquals(910,rule.targetPokemonId)
    }

    @Test
    fun summariesStayCompactAndReadable() {
        assertEquals("Nível 16",EvolutionRuleCatalog.simplify("Subir ao nível 16"))
        assertEquals(
            "Trocar segurando Metal Coat",
            EvolutionRuleCatalog.simplify("Troca • Segurando Metal Coat")
        )
        assertEquals(
            "Caminhar 1.000 passos e subir de nível",
            EvolutionRuleCatalog.simplify("Caminhar 1.000 passos com Pawmo no modo Let's Go • Depois subir de nível")
        )
        assertEquals(
            "Nível 38 no multiplayer",
            EvolutionRuleCatalog.simplify("Subir Finizen ao nível 38 ou mais enquanto estiver em uma sessão multiplayer/Union Circle")
        )
    }

    @Test
    fun fallbackClassifierRecognizesPlainLevel() {
        assertEquals(
            setOf(PokeApiService.EvolutionMethod.LEVEL),
            PokeApiService.auditFallbackMethods("Subir ao nível 36")
        )
    }

    @Test
    fun contextualOverridesRemainGameSpecific() {
        val sv=GameContext.fromSource("Scarlet / Violet · Paldea")
        val hisui=GameContext.fromSource("Legends Arceus · Hisui")
        val za=GameContext.fromSource("Pokémon Legends: Z-A · Lumiose")

        assertEquals("Subir de nível conhecendo Barb Barrage",PokeApiService.auditSpecialRequirement(904,sv))
        assertTrue(PokeApiService.auditSpecialRequirement(904,hisui)?.contains("Strong Style")==true)
        assertEquals("Acertar 20 alvos com Barb Barrage",PokeApiService.auditSpecialRequirement(904,za))
    }
}
