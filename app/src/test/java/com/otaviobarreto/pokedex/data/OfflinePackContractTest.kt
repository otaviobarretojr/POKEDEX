package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class OfflinePackContractTest {
    @Test fun packStatusOnlyVerifiesCompleteCurrentPacks() {
        val ok = OfflineGamePackManager.PackStatus(
            downloaded = true,
            pokemonCount = 400,
            packVersion = 18,
            completeCount = 400
        )
        val incomplete = ok.copy(completeCount = 399)
        val old = ok.copy(packVersion = 17)
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
}
