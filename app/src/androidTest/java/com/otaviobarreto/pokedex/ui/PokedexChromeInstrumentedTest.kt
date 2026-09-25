package com.otaviobarreto.pokedex.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PokedexChromeInstrumentedTest {
    @get:Rule val composeRule=createComposeRule()

    @Test fun primaryNavigation_rendersAndDispatchesSelection(){
        var selected="journey"
        val items=listOf(
            DexNavItem("journey","Jornada",Icons.Default.Explore),
            DexNavItem("box","Box",Icons.Default.CatchingPokemon),
            DexNavItem("settings","Configurações",Icons.Default.Settings)
        )
        composeRule.setContent{
            PokedexTheme{DexBottomBar(items,selected){selected=it.route}}
        }
        composeRule.onNodeWithText("Jornada").assertIsDisplayed()
        composeRule.onNodeWithText("Box").assertIsDisplayed().performClick()
        composeRule.runOnIdle{assertEquals("box",selected)}
        composeRule.onNodeWithText("Configurações").assertIsDisplayed()
    }

    @Test fun statusPane_exposesLoadingState(){
        composeRule.setContent{
            PokedexTheme{DexStatusPane("Preparando coleção","Carregando dados locais.",loading=true)}
        }
        composeRule.onNodeWithText("Preparando coleção").assertIsDisplayed()
        composeRule.onNodeWithText("Carregando dados locais.").assertIsDisplayed()
    }
}
