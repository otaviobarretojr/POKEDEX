package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class V6StableRegressionTest {
    @Test fun activeGameDefaultExistsInCatalog() {
        assertTrue(AppGameCatalog.games.isNotEmpty())
        assertTrue(AppGameCatalog.games.any { it.label == AppGameCatalog.games.first().label })
    }

    @Test fun routePlannerModelsRegionGroups() {
        val group = CaptureRouteGroup("Paldea", "Scarlet / Violet · Paldea", emptyList())
        assertEquals("Paldea", group.region)
        assertTrue(group.source.contains("Paldea"))
    }

    @Test fun capturePlanKeepsOrderedSteps() {
        val entry = PokeApiService.DexIndexEntry(25, "Pikachu", 1)
        val step = CapturePlanStep(entry, CapturePlanCategory.AVAILABLE_HERE, 1)
        assertEquals(1, step.priority)
        assertEquals(25, step.pokemon.id)
    }
}
