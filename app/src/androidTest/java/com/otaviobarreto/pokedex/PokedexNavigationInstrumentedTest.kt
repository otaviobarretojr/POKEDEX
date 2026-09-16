package com.otaviobarreto.pokedex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import java.io.FileOutputStream
import androidx.test.core.app.ActivityScenario
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PokedexNavigationInstrumentedTest {
    @get:Rule val composeRule=createEmptyComposeRule()
    private var scenario:ActivityScenario<MainActivity>?=null

    @Before fun launchVerifiedWarmApp(){
        val context=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("content_bootstrap_v2",android.content.Context.MODE_PRIVATE)
            .edit().putString("ready_signature","instrumented-verified-local-library").commit()
        scenario=ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun closeApp(){
        scenario?.close()
    }

    private fun captureGoldenCandidate(name:String){
        val context=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir=File(context.getExternalFilesDir(null),"visual-regression").apply{mkdirs()}
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
        composeRule.onNodeWithText("Pokédex").performClick(); composeRule.waitForIdle(); captureGoldenCandidate("pokedex")
        composeRule.onNodeWithText("Coleção").performClick(); composeRule.waitForIdle(); captureGoldenCandidate("collection")
        composeRule.onNodeWithText("Box").performClick(); composeRule.waitForIdle(); captureGoldenCandidate("box")
        composeRule.onNodeWithText("Config.").performClick(); composeRule.waitForIdle(); captureGoldenCandidate("settings")
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
