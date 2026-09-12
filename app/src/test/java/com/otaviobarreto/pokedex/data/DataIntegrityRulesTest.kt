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

    @Test fun missingOwnershipIsDerivedFromEveryCollectionReference() {
        val missing = DataIntegrityRules.missingGlobalOwnership(
            global = setOf(25),
            boxIds = listOf(25, 133),
            contextualIds = listOf(150),
            variantIds = listOf(869)
        )
        assertEquals(setOf(133,150,869), missing)
    }

    @Test fun variantsRequireContextualOwnershipInTheirOwnSource() {
        val missing = DataIntegrityRules.missingContextualVariantOwnership(
            contextual = mapOf("paldea" to setOf(25, 133)),
            variantPairs = listOf("paldea" to 133, "paldea" to 869, "galar" to 25)
        )
        assertEquals(listOf("paldea" to 869, "galar" to 25), missing)
    }

    @Test fun staleJourneyIdsDoNotInflateProgress() {
        val valid=listOf("sv-01","sv-02","sv-03")
        val completed=setOf("sv-01","removed-old-step","another-old-step")
        assertEquals(1,DataIntegrityRules.completedCount(valid,completed))
    }
}
