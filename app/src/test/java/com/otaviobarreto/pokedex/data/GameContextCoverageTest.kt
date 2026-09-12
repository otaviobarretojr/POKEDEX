package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GameContextCoverageTest {
    @Test
    fun everyConfiguredRegionResolvesToContext() {
        AppGameCatalog.games.forEach { game ->
            game.regions.forEach { region ->
                val context = GameContext.fromSource(region.source)
                assertNotNull(game.label + " / " + region.label, context)
                assertEquals(game.label, context!!.label)
            }
        }
    }
}
