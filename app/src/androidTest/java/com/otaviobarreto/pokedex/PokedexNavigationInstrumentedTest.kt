package com.otaviobarreto.pokedex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PokedexNavigationInstrumentedTest {
    @get:Rule val composeRule=createAndroidComposeRule<MainActivity>()

    @Before fun provisionVerifiedWarmLaunchState(){
        val context=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("content_bootstrap_v2",android.content.Context.MODE_PRIVATE)
            .edit().putString("ready_signature","instrumented-verified-local-library").commit()
    }

    private fun waitForMainNavigation(){
        composeRule.waitUntil(timeoutMillis=10_000){
            composeRule.onAllNodesWithText("Jornada").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Jornada").assertIsDisplayed()
    }

    @Test fun primaryRoutes_areReachableAndBottomNavigationSurvives(){
        waitForMainNavigation()
        composeRule.onNodeWithText("Pokédex").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Coleção").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Box").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Config.").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Jornada").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Pokédex").assertIsDisplayed()
        composeRule.onNodeWithText("Coleção").assertIsDisplayed()
        composeRule.onNodeWithText("Box").assertIsDisplayed()
        composeRule.onNodeWithText("Config.").assertIsDisplayed()
    }

    @Test fun routeSwitching_doesNotLosePrimaryNavigation(){
        waitForMainNavigation()
        repeat(2){
            composeRule.onNodeWithText("Box").assertIsDisplayed().performClick()
            composeRule.onNodeWithText("Coleção").assertIsDisplayed().performClick()
            composeRule.onNodeWithText("Jornada").assertIsDisplayed().performClick()
        }
        composeRule.onNodeWithText("Pokédex").assertIsDisplayed()
        composeRule.onNodeWithText("Config.").assertIsDisplayed()
    }
}
