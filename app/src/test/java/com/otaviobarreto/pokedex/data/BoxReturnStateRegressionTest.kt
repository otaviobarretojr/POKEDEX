package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class BoxReturnStateRegressionTest {
    @Test fun switchCatalogHasStableGameAndRegionIdentifiers() {
        AppGameCatalog.games.forEach { game ->
            assertTrue(game.label.isNotBlank())
            assertTrue(game.regions.isNotEmpty())
            assertEquals(game.regions.map { it.source }.distinct().size, game.regions.size)
        }
    }
}
