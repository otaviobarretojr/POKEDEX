package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyTeamProgressCatalogTest {
    @Test
    fun starterLinesAreProtectedAcrossPhases() {
        assertEquals(listOf(906,907,908),JourneyTeamProgressCatalog.starterLine(906))
        assertEquals(listOf(909,910,911),JourneyTeamProgressCatalog.starterLine(909))
        assertEquals(listOf(912,913,914),JourneyTeamProgressCatalog.starterLine(912))
        assertTrue(JourneyTeamProgressCatalog.isStarterLinePokemon(909,911))
    }

    @Test
    fun starterEvolutionTracksCampaignPhase() {
        assertEquals(909,JourneyTeamProgressCatalog.starterMemberForPhase(909,CampaignPhase.EARLY))
        assertEquals(910,JourneyTeamProgressCatalog.starterMemberForPhase(909,CampaignPhase.MID))
        assertEquals(911,JourneyTeamProgressCatalog.starterMemberForPhase(909,CampaignPhase.LATE))
    }

    @Test
    fun capturePlanNeverSuggestsFuturePokemonTooEarly() {
        val step=JourneyCatalog.steps("Scarlet / Violet").first{it.id=="sv-03"}
        val recs=JourneyTeamProgressCatalog.catchRecommendationsBefore(step)
        assertTrue(recs.all{it.availableBeforeStepOrder<=step.order})
    }

    @Test
    fun routeChaptersSeparateFinalesAndDlc() {
        assertEquals("PALDEA · 18 OBJETIVOS",JourneyTeamProgressCatalog.chapterFor("sv-01"))
        assertEquals("FINAIS DAS 3 HISTÓRIAS",JourneyTeamProgressCatalog.chapterFor("sv-pg-01"))
        assertEquals("AREA ZERO · THE WAY HOME",JourneyTeamProgressCatalog.chapterFor("sv-pg-04"))
        assertEquals("DLC · THE TEAL MASK",JourneyTeamProgressCatalog.chapterFor("sv-dlc-01"))
        assertEquals("DLC · THE INDIGO DISK",JourneyTeamProgressCatalog.chapterFor("sv-dlc-08"))
    }
}
