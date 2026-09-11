package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class DataIntegrityRulesTest {
    @Test fun regionalScopeDoesNotLeakGlobalCaptures() {
        val global=setOf(25,133,150)
        val contextual=mapOf(
            "Scarlet / Violet · Paldea" to setOf(25,133),
            "Sword / Shield · Galar" to setOf(25)
        )
        assertEquals(setOf(25,133),DataIntegrityRules.capturedForScope(global,contextual,"Scarlet / Violet · Paldea"))
        assertEquals(setOf(25),DataIntegrityRules.capturedForScope(global,contextual,"Sword / Shield · Galar"))
        assertEquals(global,DataIntegrityRules.capturedForScope(global,contextual,null))
    }

    @Test fun boxRemovalCannotEraseExistingOwnership() {
        assertTrue(DataIntegrityRules.shouldEnsureOwned(alreadyOwned=true,hasBoxReference=false,hasContextualReference=false))
        assertTrue(DataIntegrityRules.shouldEnsureOwned(alreadyOwned=false,hasBoxReference=true,hasContextualReference=false))
        assertTrue(DataIntegrityRules.shouldEnsureOwned(alreadyOwned=false,hasBoxReference=false,hasContextualReference=true))
        assertFalse(DataIntegrityRules.shouldEnsureOwned(alreadyOwned=false,hasBoxReference=false,hasContextualReference=false))
    }

    @Test fun staleJourneyIdsDoNotInflateProgress() {
        val valid=listOf("sv-01","sv-02","sv-03")
        val completed=setOf("sv-01","removed-old-step","another-old-step")
        assertEquals(1,DataIntegrityRules.completedCount(valid,completed))
    }
}
