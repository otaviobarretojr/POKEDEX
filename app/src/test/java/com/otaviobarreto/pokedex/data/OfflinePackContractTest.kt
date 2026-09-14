package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class OfflinePackContractTest {
    @Test fun packStatusOnlyVerifiesCompleteCurrentPacks() {
        val ok = OfflineGamePackManager.PackStatus(
            downloaded = true,
            pokemonCount = 400,
            packVersion = 20,
            completeCount = 400
        )
        val incomplete = ok.copy(completeCount = 399)
        val old = ok.copy(packVersion = 19)
        assertTrue(ok.verified)
        assertFalse(incomplete.verified)
        assertFalse(old.verified)
    }

    @Test fun progressFractionIsClampedAndSafe() {
        assertEquals(0f, OfflineGamePackManager.Progress(0,0,"x").fraction)
        assertEquals(.5f, OfflineGamePackManager.Progress(5,10,"x").fraction)
        assertEquals(1f, OfflineGamePackManager.Progress(15,10,"x").fraction)
    }

    @Test fun auditSummaryExplainsPrimaryRepairReason() {
        val audit = OfflineGamePackManager.PackAudit(
            valid=false,
            completedIds=9,
            expectedCount=10,
            currentVersion=true,
            hasRegionManifest=true,
            pinnedResources=2,
            expectedResources=2,
            cachedImages=10,
            expectedImages=10,
            cachedJourneyVisuals=1,
            expectedJourneyVisuals=1,
            cachedFormArtworks=1,
            expectedFormArtworks=1
        )
        assertTrue(audit.summary.contains("1 Pokémon"))
    }
    @Test fun everyGameDeclaresRegionalAndReferenceBaseResources() {
        AppGameCatalog.adventureGames.forEach { game ->
            val urls = OfflineGamePackManager.requiredBaseResourceUrls(game)
            val expectedRegionUrls = game.regions.mapNotNull { region ->
                GameContext.fromSource(region.source)?.let(GameDexService::cacheUrl)
            }.toSet()
            assertTrue(game.label, urls.containsAll(expectedRegionUrls))
            assertTrue(game.label, urls.containsAll(JourneyReadinessAudit.referenceCatalogUrls()))
        }
    }

    @Test fun packStatusTracksSharedReuseWithoutAffectingVerification() {
        val status=OfflineGamePackManager.PackStatus(
            downloaded=true,
            pokemonCount=300,
            packVersion=20,
            completeCount=300,
            reusedCount=220,
            downloadedNewCount=80
        )
        assertTrue(status.verified)
        assertEquals(220,status.reusedCount)
        assertEquals(80,status.downloadedNewCount)
    }

    @Test fun generalLibraryStatusOnlyVerifiesCompleteCurrentV20() {
        val ok=OfflineGamePackManager.GeneralLibraryStatus(
            ready=true,
            total=700,
            complete=700,
            packVersion=20
        )
        assertTrue(ok.verified)
        assertFalse(ok.copy(complete=699).verified)
        assertFalse(ok.copy(packVersion=19).verified)
        assertFalse(ok.copy(ready=false).verified)
    }

}
