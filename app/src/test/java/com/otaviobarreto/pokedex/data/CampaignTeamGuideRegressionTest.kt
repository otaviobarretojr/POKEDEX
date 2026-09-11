package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class CampaignTeamGuideRegressionTest {
    @Test fun allSwitchGamesHaveStartersAndAllPhases() {
        TeamCampaignCatalog.switchGames.forEach { game ->
            val starters = TeamCampaignCatalog.starters(game)
            assertTrue("No starters for $game", starters.isNotEmpty())
            starters.forEach { (_, id) ->
                CampaignPhase.values().forEach { phase ->
                    val preset = TeamCampaignCatalog.preset(game, id, phase)
                    assertNotNull("Missing $game / $id / $phase", preset)
                    assertEquals(6, preset!!.slots.size)
                }
            }
        }
    }

    @Test fun switchGuideContainsLegendsZA() {
        assertTrue("Pokémon Legends: Z-A" in TeamCampaignCatalog.switchGames)
        assertEquals(setOf(152,498,158), TeamCampaignCatalog.starters("Pokémon Legends: Z-A").map { it.second }.toSet())
    }
}
