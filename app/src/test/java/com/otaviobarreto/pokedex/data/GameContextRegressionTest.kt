package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class GameContextRegressionTest {
    @Test fun scarletVioletIncludesAllThreeRegions() {
        val game = AppGameCatalog.games.first { it.label == "Scarlet / Violet" }
        assertEquals(listOf("Paldea","Kitakami","Blueberry"), game.regions.map { it.label })
    }

    @Test fun gameContextsResolveCoreRegions() {
        assertEquals("Paldea", GameContext.fromSource("Scarlet / Violet · Paldea")?.regionLabel)
        assertEquals("Kitakami", GameContext.fromSource("Scarlet / Violet · Kitakami")?.regionLabel)
        assertEquals("Blueberry", GameContext.fromSource("Scarlet / Violet · Blueberry")?.regionLabel)
        assertEquals("Hisui", GameContext.fromSource("Legends Arceus · Hisui")?.regionLabel)
    }

    @Test fun everyCatalogRegionHasContext() {
        AppGameCatalog.games.flatMap { it.regions }.forEach { region ->
            assertNotNull("Missing GameContext for " + region.source, GameContext.fromSource(region.source))
        }
    }
    @Test fun specialEvolutionClassificationAvoidsFalsePositives() {
        val rage = PokeApiService.auditFallbackMethods("Usar Rage Fist 20 vezes • Depois subir de nível")
        assertTrue(PokeApiService.EvolutionMethod.MOVE in rage)
        assertTrue(PokeApiService.EvolutionMethod.ACTION in rage)
        assertFalse(PokeApiService.EvolutionMethod.ITEM in rage)

        val multiplayer = PokeApiService.auditFallbackMethods("Subir Finizen ao nível 38 ou mais enquanto estiver em uma sessão multiplayer/Union Circle")
        assertTrue(PokeApiService.EvolutionMethod.MULTIPLAYER in multiplayer)
        assertFalse(PokeApiService.EvolutionMethod.LOCATION in multiplayer)
    }

    @Test fun contextualSpecialEvolutionRequirementsMatchGameRules() {
        val sv = GameContext.fromSource("Scarlet / Violet · Paldea")
        val la = GameContext.fromSource("Legends Arceus · Hisui")
        val za = GameContext.fromSource("Pokémon Legends: Z-A · Lumiose")

        assertEquals("Subir de nível conhecendo Barb Barrage", PokeApiService.auditSpecialRequirement(904, sv))
        assertTrue(PokeApiService.auditSpecialRequirement(904, la)?.contains("Strong Style")==true)
        assertEquals("Acertar 20 alvos com Barb Barrage", PokeApiService.auditSpecialRequirement(904, za))
        assertTrue(PokeApiService.auditSpecialRequirement(899, sv)?.startsWith("Evolução indisponível")==true)
        assertTrue(PokeApiService.auditSpecialRequirement(899, la)?.contains("Psyshield Bash")==true)
    }

}
