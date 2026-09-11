package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class CompanionRegressionTest {
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
}
