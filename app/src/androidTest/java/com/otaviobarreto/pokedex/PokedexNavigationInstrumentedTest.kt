package com.otaviobarreto.pokedex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
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

    private fun nav(route:String)=composeRule.onNodeWithTag("bottom_nav_$route")

    private fun waitForMainNavigation(){
        composeRule.waitUntil(timeoutMillis=10_000){
            composeRule.onAllNodesWithTag("bottom_nav_${PokedexRoutes.HOME}").fetchSemanticsNodes().isNotEmpty()
        }
        nav(PokedexRoutes.HOME).assertIsDisplayed()
    }

    @Test fun visualSnapshots_primaryDestinations(){
        waitForMainNavigation()
        composeRule.waitForIdle()
        captureGoldenCandidate("journey")
        nav(PokedexRoutes.GAMES).performClick(); composeRule.waitForIdle(); captureGoldenCandidate("games")
        nav(PokedexRoutes.POKEDEX).performClick(); composeRule.waitForIdle(); captureGoldenCandidate("pokedex")
        nav(PokedexRoutes.COLLECTION).performClick(); composeRule.waitForIdle(); captureGoldenCandidate("collection")
        nav(PokedexRoutes.BOXES).performClick(); composeRule.waitForIdle(); captureGoldenCandidate("box")
        nav(PokedexRoutes.CENTRAL).performClick(); composeRule.waitForIdle()
        // Runtime cache/timing metrics intentionally remain live in production; exclude Settings from pixel baseline.

    }

    @Test fun primaryRoutes_areReachableAndBottomNavigationSurvives(){
        waitForMainNavigation()
        nav(PokedexRoutes.GAMES).assertIsDisplayed().performClick()
        nav(PokedexRoutes.POKEDEX).assertIsDisplayed().performClick()
        nav(PokedexRoutes.COLLECTION).assertIsDisplayed().performClick()
        nav(PokedexRoutes.BOXES).assertIsDisplayed().performClick()
        nav(PokedexRoutes.CENTRAL).assertIsDisplayed().performClick()
        nav(PokedexRoutes.HOME).assertIsDisplayed().performClick()
        nav(PokedexRoutes.GAMES).assertIsDisplayed()
        nav(PokedexRoutes.POKEDEX).assertIsDisplayed()
        nav(PokedexRoutes.COLLECTION).assertIsDisplayed()
        nav(PokedexRoutes.BOXES).assertIsDisplayed()
        nav(PokedexRoutes.CENTRAL).assertIsDisplayed()
    }

    @Test fun routeSwitching_doesNotLosePrimaryNavigation(){
        waitForMainNavigation()
        repeat(2){
            nav(PokedexRoutes.BOXES).assertIsDisplayed().performClick()
            nav(PokedexRoutes.GAMES).assertIsDisplayed().performClick()
            nav(PokedexRoutes.COLLECTION).assertIsDisplayed().performClick()
            nav(PokedexRoutes.HOME).assertIsDisplayed().performClick()
        }
        nav(PokedexRoutes.POKEDEX).assertIsDisplayed()
        nav(PokedexRoutes.CENTRAL).assertIsDisplayed()
    }
}
