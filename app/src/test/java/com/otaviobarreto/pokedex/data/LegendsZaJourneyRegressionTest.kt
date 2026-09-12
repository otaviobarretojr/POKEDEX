package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegendsZaJourneyRegressionTest {
    @Test
    fun legendsZaHasFullBaseAndMegaDimensionRoute() {
        val steps=JourneyCatalog.steps("Pokémon Legends: Z-A")
        assertEquals(57,steps.size)
        assertEquals("za-01",steps.first().id)
        assertEquals("za-dlc-14",steps.last().id)
        assertTrue(steps.any{it.id=="za-37" && it.title=="Operation Protect Lumiose"})
        assertTrue(steps.any{it.id=="za-42" && it.title=="To Keep the World in Balance"})
    }

    @Test
    fun legendsZaStarterGuidancePrefersTepig() {
        val starters=JourneyStarterCatalog.forGame("Pokémon Legends: Z-A")
        assertEquals(3,starters.size)
        assertEquals(498,JourneyStarterCatalog.bestForGame("Pokémon Legends: Z-A")?.pokemonId)
    }

    @Test
    fun legendsZaStarterEvolutionLinesAreProtected() {
        assertEquals(listOf(152,153,154),JourneyTeamProgressCatalog.starterLine(152))
        assertEquals(listOf(498,499,500),JourneyTeamProgressCatalog.starterLine(498))
        assertEquals(listOf(158,159,160),JourneyTeamProgressCatalog.starterLine(158))
    }

    @Test
    fun keyPromotionAndDlcBossesHaveDetails() {
        listOf("za-06","za-10","za-14","za-19","za-24","za-30","za-35","za-dlc-01","za-dlc-12").forEach{
            assertNotNull("Missing detail for $it",JourneyObjectiveDetailsCatalog.detail(it))
        }
    }
}
