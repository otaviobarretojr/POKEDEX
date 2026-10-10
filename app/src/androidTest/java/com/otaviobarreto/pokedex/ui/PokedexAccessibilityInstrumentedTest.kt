package com.otaviobarreto.pokedex.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class PokedexAccessibilityInstrumentedTest {
    @get:Rule val composeRule=createComposeRule()

    @Test
    fun bottomNavigation_hasLabelsAndMinimumTouchHeight(){
        val items=listOf(
            DexNavItem("home","Início",Icons.Default.Home),
            DexNavItem("pokedex","Pokédex",Icons.Default.MenuBook),
            DexNavItem("collection","Coleção",Icons.Default.AutoAwesome),
            DexNavItem("boxes","Box",Icons.Default.GridView),
            DexNavItem("central","Config.",Icons.Default.Settings)
        )
        composeRule.setContent{PokedexTheme{DexBottomBar(items,"home") { }}}
        items.forEach{item->
            composeRule.onNodeWithTag("bottom_nav_"+item.route)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
            composeRule.onNodeWithContentDescription(item.label).assertIsDisplayed()
        }
    }

    @Test
    fun largeFont_keepsPrimaryLabelsDiscoverable(){
        composeRule.setContent{
            val base=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density,2f)){
                PokedexTheme{
                    DexStatusPane(
                        title="Coleção pronta",
                        message="Conteúdo permanece legível com fonte ampliada."
                    )
                }
            }
        }
        composeRule.onNodeWithText("Coleção pronta").assertIsDisplayed()
        composeRule.onNodeWithText("Conteúdo permanece legível com fonte ampliada.").assertIsDisplayed()
    }
}
