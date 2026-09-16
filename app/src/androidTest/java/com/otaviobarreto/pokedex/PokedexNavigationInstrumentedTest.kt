package com.otaviobarreto.pokedex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class PokedexNavigationInstrumentedTest {
    @get:Rule val composeRule=createAndroidComposeRule<MainActivity>()

    @Test fun primaryRoutes_areReachableAndBottomNavigationSurvives(){
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
        repeat(2){
            composeRule.onNodeWithText("Box").assertIsDisplayed().performClick()
            composeRule.onNodeWithText("Coleção").assertIsDisplayed().performClick()
            composeRule.onNodeWithText("Jornada").assertIsDisplayed().performClick()
        }
        composeRule.onNodeWithText("Pokédex").assertIsDisplayed()
        composeRule.onNodeWithText("Config.").assertIsDisplayed()
    }
}
