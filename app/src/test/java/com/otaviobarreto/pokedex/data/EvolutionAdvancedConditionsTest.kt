package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class EvolutionAdvancedConditionsTest {

    @Test
    fun genderBasedEvolutionsAreFirstClassRules() {
        assertEquals(
            listOf("Combee fêmea sobe ao nível 21"),
            EvolutionCuratedCatalog.rulesFor(416,GameContext.fromSource("Sword / Shield · Galar")).map{it.requirement}
        )
        assertEquals(
            listOf("Usar Dawn Stone em Kirlia macho"),
            EvolutionCuratedCatalog.rulesFor(475,GameContext.fromSource("Brilliant Diamond / Shining Pearl · Sinnoh")).map{it.requirement}
        )
        assertEquals(
            listOf("Usar Dawn Stone em Snorunt fêmea"),
            EvolutionCuratedCatalog.rulesFor(478,GameContext.fromSource("Brilliant Diamond / Shining Pearl · Sinnoh")).map{it.requirement}
        )
        assertTrue(PokeApiService.EvolutionMethod.GENDER in PokeApiService.auditFallbackMethods("Usar Dawn Stone em Kirlia macho"))
        assertTrue(PokeApiService.EvolutionMethod.GENDER in PokeApiService.auditFallbackMethods("Combee fêmea sobe ao nível 21"))
    }

    @Test
    fun heldItemMethodsChangeInLegendsArceus() {
        val hisui=GameContext.fromSource("Legends Arceus · Hisui")
        val bdsp=GameContext.fromSource("Brilliant Diamond / Shining Pearl · Sinnoh")

        assertEquals("Usar Razor Claw em Sneasel durante a noite",EvolutionCuratedCatalog.ruleFor(461,hisui)?.requirement)
        assertTrue(EvolutionCuratedCatalog.ruleFor(461,bdsp)?.requirement?.contains("segurando Razor Claw")==true)

        assertEquals("Usar Razor Fang em Gligar durante a noite",EvolutionCuratedCatalog.ruleFor(472,hisui)?.requirement)
        assertTrue(EvolutionCuratedCatalog.ruleFor(472,bdsp)?.requirement?.contains("segurando Razor Fang")==true)
    }

    @Test
    fun rockruffRoutesKeepThreeDestinationForms() {
        val sv=GameContext.fromSource("Scarlet / Violet · Paldea")
        val routes=EvolutionCuratedCatalog.rulesFor(745,sv)

        assertEquals(3,routes.size)
        assertEquals(
            setOf("lycanroc-midday","lycanroc-midnight","lycanroc-dusk"),
            routes.mapNotNull{it.targetFormKey}.toSet()
        )
        assertTrue(routes.any{it.sourceFormKey=="rockruff-own-tempo" && it.targetFormKey=="lycanroc-dusk"})
    }

    @Test
    fun toxelRoutesKeepAmpedAndLowKeyFormsSeparate() {
        val context=GameContext.fromSource("Sword / Shield · Galar")
        val routes=EvolutionCuratedCatalog.rulesFor(849,context)

        assertEquals(2,routes.size)
        assertEquals(
            setOf("toxtricity-amped","toxtricity-low-key"),
            routes.mapNotNull{it.targetFormKey}.toSet()
        )
    }

    @Test
    fun branchAuditRejectsMissingTargetForm() {
        val invalid=EvolutionRoute(
            sourcePokemonId=744,
            targetPokemonId=745,
            methods=setOf(PokeApiService.EvolutionMethod.LEVEL_CONDITION),
            summary="Nível 25 ao entardecer",
            detail="Rockruff com Own Tempo sobe ao nível 25 ou mais ao entardecer",
            availability=EvolutionAvailability.AVAILABLE_WITH_CONDITION,
            contextLabel="Scarlet / Violet",
            regionLabel="Paldea",
            sourceFormKey="rockruff-own-tempo",
            targetFormKey=null
        )
        assertTrue(EvolutionCoverageAudit.inspect(listOf(invalid)).any{it.code=="MISSING_TARGET_FORM"})
    }
}
