package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class EvolutionCoverageAuditTest {

    @Test
    fun curatedContextMatrixHasNoStructuralIssues() {
        assertTrue(EvolutionCoverageAudit.inspectCuratedContexts().toString(),EvolutionCoverageAudit.inspectCuratedContexts().isEmpty())
    }

    @Test
    fun formSensitiveCuratedRulesCarrySourceFormIdentity() {
        val sv=GameContext.fromSource("Scarlet / Violet · Paldea")
        val hisui=GameContext.fromSource("Legends Arceus · Hisui")
        val galar=GameContext.fromSource("Sword / Shield · Galar")

        assertEquals("basculin-white-striped",EvolutionCuratedCatalog.ruleFor(902,sv)?.sourceFormKey)
        assertEquals("qwilfish-hisui",EvolutionCuratedCatalog.ruleFor(904,hisui)?.sourceFormKey)
        assertEquals("farfetchd-galar",EvolutionCuratedCatalog.ruleFor(865,galar)?.sourceFormKey)
        assertEquals("yamask-galar",EvolutionCuratedCatalog.ruleFor(867,galar)?.sourceFormKey)
    }

    @Test
    fun gameSpecificMethodsDoNotLeakAcrossContexts() {
        val sv=GameContext.fromSource("Scarlet / Violet · Paldea")
        val hisui=GameContext.fromSource("Legends Arceus · Hisui")

        assertTrue(EvolutionCuratedCatalog.ruleFor(899,sv)?.requirement?.contains("indisponível",true)==true)
        assertTrue(EvolutionCuratedCatalog.ruleFor(899,hisui)?.requirement?.contains("Agile Style",true)==true)

        assertTrue(EvolutionCuratedCatalog.ruleFor(901,sv)?.requirement?.contains("indisponível",true)==true)
        assertTrue(EvolutionCuratedCatalog.ruleFor(901,hisui)?.requirement?.contains("Peat Block",true)==true)

        assertTrue(EvolutionCuratedCatalog.ruleFor(902,sv)?.requirement?.contains("subir de nível",true)==true)
        assertFalse(EvolutionCuratedCatalog.ruleFor(902,hisui)?.requirement?.contains("subir de nível",true)==true)
    }

    @Test
    fun branchingTargetsRemainIndependentRoutes() {
        val routes=listOf(
            EvolutionRoute(133,134,setOf(PokeApiService.EvolutionMethod.ITEM),"Usar Water Stone","Usar Water Stone",EvolutionAvailability.AVAILABLE_WITH_CONDITION,"Game","Region"),
            EvolutionRoute(133,135,setOf(PokeApiService.EvolutionMethod.ITEM),"Usar Thunder Stone","Usar Thunder Stone",EvolutionAvailability.AVAILABLE_WITH_CONDITION,"Game","Region"),
            EvolutionRoute(133,136,setOf(PokeApiService.EvolutionMethod.ITEM),"Usar Fire Stone","Usar Fire Stone",EvolutionAvailability.AVAILABLE_WITH_CONDITION,"Game","Region")
        )
        assertEquals(1,EvolutionResolutionEngine.routesForTarget(routes,134).size)
        assertEquals(1,EvolutionResolutionEngine.routesForTarget(routes,135).size)
        assertEquals(1,EvolutionResolutionEngine.routesForTarget(routes,136).size)
        assertTrue(EvolutionCoverageAudit.inspect(routes).isEmpty())
    }
}
