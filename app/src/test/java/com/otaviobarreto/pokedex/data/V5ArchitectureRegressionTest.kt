package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class V5ArchitectureRegressionTest {
    @Test fun capturePlanCategoriesRemainStable() {
        assertEquals(3, CapturePlanCategory.values().size)
        assertNotNull(CapturePlanCategory.valueOf("AVAILABLE_HERE"))
        assertNotNull(CapturePlanCategory.valueOf("OTHER_GAME_OR_TRADE"))
    }

    @Test fun catalogHasAValidDefaultCompanionGame() {
        assertTrue(AppGameCatalog.games.isNotEmpty())
        assertTrue(AppGameCatalog.games.first().label.isNotBlank())
    }
}
