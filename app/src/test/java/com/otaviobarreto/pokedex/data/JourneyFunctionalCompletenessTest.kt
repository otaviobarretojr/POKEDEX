package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyFunctionalCompletenessTest {

    @Test
    fun everyAdventureGameHasAUsableJourney(){
        AppGameCatalog.adventureGames.forEach{game->
            val steps=JourneyCatalog.steps(game.label)
            assertTrue("Empty Journey for "+game.label,steps.isNotEmpty())
            assertEquals(
                "Duplicate Journey ids in "+game.label,
                steps.size,
                steps.map{it.id}.distinct().size
            )
        }
    }

    @Test
    fun everyJourneyObjectiveHasFunctionalSupport(){
        AppGameCatalog.adventureGames.forEach{game->
            JourneyCatalog.steps(game.label).forEach{step->
                assertNotNull("Preparation missing for "+game.label+" / "+step.id,JourneyPreparationCatalog.forStep(step.id))
                assertNotNull("Details missing for "+game.label+" / "+step.id,JourneyObjectiveDetailsCatalog.detail(step.id))
                val guide=JourneyWalkthroughCatalog.forStep(step.id)
                assertNotNull("Walkthrough missing for "+game.label+" / "+step.id,guide)
                assertTrue("Walkthrough too short for "+step.id,guide!!.steps.size>=4)
                assertNotNull("Preview missing for "+game.label+" / "+step.id,JourneyObjectivePreviewCatalog.forStep(step))
            }
        }
    }

    @Test
    fun objectiveSpecificSmartContextUsesTheRequestedStep(){
        AppGameCatalog.adventureGames.forEach{game->
            JourneyCatalog.steps(game.label).take(3).forEach{step->
                val ctx=JourneySmartProgress.contextForStep(game.label,step.id)
                assertEquals(step.id,ctx.nextStep?.id)
                assertTrue(ctx.phaseLabel.isNotBlank())
                assertTrue(ctx.recommendation.contains(step.title))
            }
        }
    }
}
