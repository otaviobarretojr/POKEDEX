package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class EvolutionResolutionEngineTest {

    @Test
    fun availabilityDistinguishesExecutableTransferAndUnavailableRoutes() {
        assertEquals(
            EvolutionAvailability.AVAILABLE,
            EvolutionResolutionEngine.availabilityFor(
                "Subir ao nível 16",
                setOf(PokeApiService.EvolutionMethod.LEVEL)
            )
        )
        assertEquals(
            EvolutionAvailability.AVAILABLE_WITH_CONDITION,
            EvolutionResolutionEngine.availabilityFor(
                "Caminhar 1.000 passos e subir de nível",
                setOf(PokeApiService.EvolutionMethod.ACTION)
            )
        )
        assertEquals(
            EvolutionAvailability.TRANSFER_ONLY,
            EvolutionResolutionEngine.availabilityFor(
                "Evolução indisponível neste jogo; evolua em Legends: Arceus e transfira pelo Pokémon HOME",
                emptySet()
            )
        )
        assertEquals(
            EvolutionAvailability.NOT_AVAILABLE,
            EvolutionResolutionEngine.availabilityFor(
                "Evolução indisponível neste jogo",
                emptySet()
            )
        )
    }

    @Test
    fun preferredRoutePrioritizesExecutableOverTransferOnly() {
        val executable=EvolutionRoute(
            1,2,
            setOf(PokeApiService.EvolutionMethod.LEVEL),
            "Nível 16","Subir ao nível 16",
            EvolutionAvailability.AVAILABLE,
            "Game","Region"
        )
        val transfer=EvolutionRoute(
            1,2,
            emptySet(),
            "Transferir pelo HOME","Evolução indisponível; transferir pelo HOME",
            EvolutionAvailability.TRANSFER_ONLY,
            "Game","Region"
        )
        assertEquals(executable,EvolutionResolutionEngine.preferredRoute(listOf(transfer,executable)))
    }

    @Test
    fun executableExcludesTransferAndUnavailableRoutes() {
        val available=EvolutionRoute(
            1,2,setOf(PokeApiService.EvolutionMethod.LEVEL),
            "Nível 16","Subir ao nível 16",
            EvolutionAvailability.AVAILABLE,"Game","Region"
        )
        val transfer=available.copy(availability=EvolutionAvailability.TRANSFER_ONLY)
        val unavailable=available.copy(availability=EvolutionAvailability.NOT_AVAILABLE)

        assertTrue(EvolutionResolutionEngine.executable(available))
        assertFalse(EvolutionResolutionEngine.executable(transfer))
        assertFalse(EvolutionResolutionEngine.executable(unavailable))
    }

    @Test
    fun specialContextualRulesRemainSpecificPerGame() {
        val sv=GameContext.fromSource("Scarlet / Violet · Paldea")
        val hisui=GameContext.fromSource("Legends Arceus · Hisui")
        val za=GameContext.fromSource("Pokémon Legends: Z-A · Lumiose")

        assertEquals("Subir de nível conhecendo Barb Barrage",PokeApiService.auditSpecialRequirement(904,sv))
        assertTrue(PokeApiService.auditSpecialRequirement(904,hisui)?.contains("Strong Style")==true)
        assertEquals("Usar Barb Barrage 20 vezes",PokeApiService.auditSpecialRequirement(904,za))
    }

    @Test
    fun gameCatalogKeepsSharedSaveRegionsGroupedTogether() {
        val scarlet=AppGameCatalog.games.first{it.label=="Scarlet / Violet"}
        assertEquals(
            setOf(
                "Scarlet / Violet · Paldea",
                "Scarlet / Violet · Kitakami",
                "Scarlet / Violet · Blueberry"
            ),
            scarlet.regions.map{it.source}.toSet()
        )

        val sword=AppGameCatalog.games.first{it.label=="Sword / Shield"}
        assertEquals(3,sword.regions.size)
    }
}
