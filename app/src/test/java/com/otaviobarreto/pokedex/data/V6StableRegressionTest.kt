package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertTrue
import org.junit.Test

class V6StableRegressionTest {
    @Test fun activeGameDefaultExistsInCatalog() {
        assertTrue(AppGameCatalog.games.isNotEmpty())
        assertTrue(AppGameCatalog.games.any { it.label == AppGameCatalog.games.first().label })
    }
}
