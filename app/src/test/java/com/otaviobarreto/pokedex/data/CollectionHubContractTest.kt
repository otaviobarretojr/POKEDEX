package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionHubContractTest {
    @Test fun livingDexPlanMatchesCollectionSourceOfTruth(){
        val plan=LivingDexPlanner.current()
        assertEquals(CollectionStore.capturedIds.size,plan.capturedSpecies)
        assertEquals(PokeApiService.MAX_NATIONAL_DEX_ID,plan.totalSpecies)
    }

    @Test fun collectionInsightsUseTheSameNationalDexTotal(){
        val insights=CollectionInsightsService.current()
        assertEquals(PokeApiService.MAX_NATIONAL_DEX_ID,insights.nationalDexTotal)
        assertTrue(insights.livingDexRatio in 0f..1f)
    }

    @Test fun generationTotalsCoverNationalDex(){
        val plan=LivingDexPlanner.current()
        assertEquals(plan.totalSpecies,plan.byGeneration.sumOf{it.total})
    }
}
