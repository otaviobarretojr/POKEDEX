package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class JourneyReadinessAuditTest {
    @Test
    fun scarletVioletResolvesAllSupportedRegions() {
        val audit = JourneyReadinessAudit.scarletViolet()
        assertTrue(audit.valid)
        assertEquals(listOf("Paldea", "Kitakami", "Blueberry"), audit.regions)
        assertEquals(listOf("paldea", "kitakami", "blueberry"), audit.slugs)
        assertTrue(audit.missingSources.isEmpty())
    }

    @Test
    fun everyAdventureRegionCanBePreloaded() {
        val expected = AppGameCatalog.adventureGames.sumOf { it.regions.size }
        val contexts = JourneyReadinessAudit.allAdventureContexts()
        assertTrue(contexts.isNotEmpty())
        assertTrue(contexts.size <= expected)
        AppGameCatalog.adventureGames.flatMap { it.regions }.forEach { region ->
            assertNotNull(GameContext.fromSource(region.source))
        }
    }

    @Test
    fun scarletUsesBundledCardArtInsteadOfLegacyRemoteCovers() {
        val urls = JourneyReadinessAudit.journeyVisualUrls("Scarlet / Violet")
        val legacyCovers = GameCoverCatalog.coversFor("Scarlet / Violet")
        assertTrue(legacyCovers.isNotEmpty())
        assertTrue(legacyCovers.none { it in urls })
        assertTrue(urls.isNotEmpty())
    }
}
