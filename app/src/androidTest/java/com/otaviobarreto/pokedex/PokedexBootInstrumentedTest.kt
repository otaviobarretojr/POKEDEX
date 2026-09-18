package com.otaviobarreto.pokedex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import org.junit.Rule
import org.junit.Test

class PokedexBootInstrumentedTest {
    @get:Rule val composeRule=createEmptyComposeRule()

    @Test fun mainActivityBoot_rendersWithoutCrash(){
        ActivityScenario.launch(MainActivity::class.java).use{
            composeRule.waitUntil(timeoutMillis=10_000){
                composeRule.onAllNodesWithText("SUA JORNADA, ORGANIZADA.")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("POKEDEX",substring=true,useUnmergedTree=true)
                .assertIsDisplayed()
        }
    }
}
