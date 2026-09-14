package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class AvailabilityCoverageAuditTest {

    private val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!

    @Test fun completeCoverageHasNoHoles() {
        val dex=listOf(
            GameDexService.GameDexEntry(1,1,"Bulbasaur"),
            GameDexService.GameDexEntry(2,2,"Ivysaur")
        )
        val records=dex.map{entry->
            CanonicalAvailability(
                pokemonId=entry.nationalId,
                context=context,
                inRegionalDex=true,
                regionalNumber=entry.gameNumber,
                version=null,
                acquisitionKind=CanonicalAcquisitionKind.EVENT_SPECIAL,
                acquisitionLabel="Método conhecido",
                confidence=AvailabilityConfidence.PARTIAL
            )
        }
        val summary=AvailabilityCoverageAudit.summarize(dex,records)
        assertTrue(summary.complete)
        assertEquals(2,summary.resolved)
        AvailabilityCoverageAudit.assertNoCoverageHoles(dex,records)
    }

    @Test(expected=IllegalStateException::class)
    fun missingRecordFailsCoverageContract() {
        val dex=listOf(GameDexService.GameDexEntry(1,1,"Bulbasaur"))
        AvailabilityCoverageAudit.assertNoCoverageHoles(dex,emptyList())
    }
}
