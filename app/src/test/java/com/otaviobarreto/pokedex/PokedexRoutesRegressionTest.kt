package com.otaviobarreto.pokedex

import org.junit.Assert.*
import org.junit.Test

class PokedexRoutesRegressionTest {
    @Test fun mainAndSecondaryRoutesStayDisjoint() {
        assertTrue(PokedexRoutes.main.isNotEmpty())
        assertTrue(PokedexRoutes.secondary.isNotEmpty())
        assertTrue(PokedexRoutes.main.intersect(PokedexRoutes.secondary).isEmpty())
    }

    @Test fun requiredMainDestinationsRemainStable() {
        assertEquals(setOf("home","pokedex","collection","boxes","central"), PokedexRoutes.main)
    }

    @Test fun secondaryRoutesRemainRecognized() {
        PokedexRoutes.secondary.forEach { assertTrue(PokedexRoutes.isSecondary(it)) }
        assertFalse(PokedexRoutes.isSecondary(PokedexRoutes.HOME))
        assertFalse(PokedexRoutes.isSecondary(null))
    }
}
