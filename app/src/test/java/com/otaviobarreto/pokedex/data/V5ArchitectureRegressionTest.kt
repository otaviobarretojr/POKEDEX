package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertTrue
import org.junit.Test

class V5ArchitectureRegressionTest {
    @Test fun catalogHasAValidDefaultGame() {
        assertTrue(AppGameCatalog.games.isNotEmpty())
        assertTrue(AppGameCatalog.games.first().label.isNotBlank())
    }
}
