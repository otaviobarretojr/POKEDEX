package com.otaviobarreto.pokedex.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
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
        var selected="home"
        val items=listOf(
            DexNavItem("home","Início",Icons.Default.Home),
            DexNavItem("pokedex","Pokédex",Icons.Default.MenuBook),
            DexNavItem("collection","Coleção",Icons.Default.AutoAwesome),
            DexNavItem("boxes","Box",Icons.Default.GridView),
            DexNavItem("central","Config.",Icons.Default.Settings)
        )
        composeRule.setContent{
            PokedexTheme{DexBottomBar(items,selected){selected=it.route}}
        }
        composeRule.onNodeWithText("Início").assertIsDisplayed()
        composeRule.onNodeWithText("Box").assertIsDisplayed().performClick()
        composeRule.runOnIdle{assertEquals("boxes",selected)}
        composeRule.onNodeWithText("Config.").assertIsDisplayed()
    }

    @Test fun statusPane_exposesLoadingState(){
        composeRule.setContent{
            PokedexTheme{DexStatusPane("Preparando coleção","Carregando dados locais.",loading=true)}
        }
        composeRule.onNodeWithText("Preparando coleção").assertIsDisplayed()
        composeRule.onNodeWithText("Carregando dados locais.").assertIsDisplayed()
    }
}
