package com.otaviobarreto.pokedex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.data.AppStatePreferences
import com.otaviobarreto.pokedex.ui.JourneyScreen
import com.otaviobarreto.pokedex.ui.PokedexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TrainerTodayJourneyReturnInstrumentedTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun journeyOpenedFromTrainerToday_hasVisibleReturnAction() {
        AppStatePreferences.activeGame = AppGameCatalog.adventureGames.first().label
        var exited = false

        composeRule.setContent {
            PokedexTheme {
                JourneyScreen(
                    startInGames = false,
                    onPokemonClick = { _, _ -> },
                    onOpenTeamGuide = { _, _, _ -> },
                    onOpenBoxes = { _, _ -> },
                    onExit = { exited = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Voltar")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertTrue("Trainer Today Journey must return to its caller", exited)
        }
    }
}
