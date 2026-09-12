package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyWalkthroughCatalogTest {
    @Test
    fun everyMainPaldeaObjectiveHasWalkthrough() {
        val main = JourneyCatalog.steps("Scarlet / Violet")
            .filter { it.id.matches(Regex("sv-\\d{2}")) }

        assertEquals(18, main.size)
        main.forEach { step ->
            val guide = JourneyWalkthroughCatalog.forStep(step.id)
            assertNotNull("Missing walkthrough for " + step.id, guide)
            assertTrue("Walkthrough too short for " + step.id, guide!!.steps.size >= 4)
            assertTrue("Tips missing for " + step.id, guide.tips.isNotEmpty())
        }
    }

    @Test
    fun larryGuideContainsDirectSecretMenuAnswer() {
        val guide = JourneyWalkthroughCatalog.forStep("sv-11")!!
        val text = (guide.steps + guide.tips).joinToString(" ")
        listOf("Grilled Rice Balls", "Medium", "Fire Blast", "Lemon").forEach {
            assertTrue(text.contains(it))
        }
    }

    @Test
    fun kofuGuideIncludesAuctionFlow() {
        val guide = JourneyWalkthroughCatalog.forStep("sv-09")!!
        val text = (guide.steps + guide.tips).joinToString(" ")
        assertTrue(text.contains("Porto Marinada"))
        assertTrue(text.contains("50.000"))
        assertTrue(text.contains("35.000"))
    }
}
