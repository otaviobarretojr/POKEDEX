package com.otaviobarreto.pokedex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import java.io.FileOutputStream
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PokedexNavigationInstrumentedTest {
    @get:Rule val composeRule=createComposeRule()

    @Before fun launchStableAppSurface(){
        composeRule.setContent{
            com.otaviobarreto.pokedex.ui.PokedexTheme{PokedexApp()}
        }
    }

    private fun captureGoldenCandidate(name:String){
        val context=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir=File("/sdcard/Download/pokedex-visual-regression").apply{mkdirs()}
        val file=File(dir,name+".png")
        FileOutputStream(file).use{out->
            composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(android.graphics.Bitmap.CompressFormat.PNG,100,out)
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendStatus(2,android.os.Bundle().apply{putString("visual_snapshot",file.absolutePath)})
    }

    private fun waitForMainNavigation(){
        composeRule.waitUntil(timeoutMillis=10_000){
            composeRule.onAllNodesWithText("Jornada").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Jornada").assertIsDisplayed()
    }

    @Test fun visualSnapshots_primaryDestinations(){
        waitForMainNavigation()
        composeRule.waitForIdle()
        captureGoldenCandidate("journey")
        composeRule.onNodeWithText("Jogos").performClick(); composeRule.waitForIdle(); captureGoldenCandidate("games")
        composeRule.onNodeWithText("Pokédex").performClick(); composeRule.waitForIdle(); captureGoldenCandidate("pokedex")
        composeRule.onNodeWithText("Coleção").performClick(); composeRule.waitForIdle(); captureGoldenCandidate("collection")
        composeRule.onNodeWithText("Box").performClick(); composeRule.waitForIdle(); captureGoldenCandidate("box")
        composeRule.onNodeWithText("Config.").performClick(); composeRule.waitForIdle()
        // Runtime cache/timing metrics intentionally remain live in production; exclude Settings from pixel baseline.

    }

    @Test fun primaryRoutes_areReachableAndBottomNavigationSurvives(){
        waitForMainNavigation()
        composeRule.onNodeWithText("Jogos").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Pokédex").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Coleção").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Box").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Config.").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Jornada").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Jogos").assertIsDisplayed()
        composeRule.onNodeWithText("Pokédex").assertIsDisplayed()
        composeRule.onNodeWithText("Coleção").assertIsDisplayed()
        composeRule.onNodeWithText("Box").assertIsDisplayed()
        composeRule.onNodeWithText("Config.").assertIsDisplayed()
    }

    @Test fun routeSwitching_doesNotLosePrimaryNavigation(){
        waitForMainNavigation()
        repeat(2){
            composeRule.onNodeWithText("Box").assertIsDisplayed().performClick()
            composeRule.onNodeWithText("Jogos").assertIsDisplayed().performClick()
            composeRule.onNodeWithText("Coleção").assertIsDisplayed().performClick()
            composeRule.onNodeWithText("Jornada").assertIsDisplayed().performClick()
        }
        composeRule.onNodeWithText("Pokédex").assertIsDisplayed()
        composeRule.onNodeWithText("Config.").assertIsDisplayed()
    }
}
