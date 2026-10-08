package com.otaviobarreto.pokedex

import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.data.RemoteOfflinePackageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StableFoundationContractTest {
    @Test
    fun pokedexToolsStayOutsideBottomNavigation() {
        listOf(
            PokedexRoutes.SEARCH,
            PokedexRoutes.EVOLUTION_CENTER,
            PokedexRoutes.GAME_DEX,
            PokedexRoutes.POKEMON,
            PokedexRoutes.FORM_DETAIL,
            PokedexRoutes.REFERENCE
        ).forEach { route ->
            assertTrue(route, PokedexRoutes.isSecondary(route))
        }
    }

    @Test
    fun mainNavigationContractHasFivePokedexDestinations() {
        assertEquals(
            setOf(
                PokedexRoutes.HOME,
                PokedexRoutes.POKEDEX,
                PokedexRoutes.COLLECTION,
                PokedexRoutes.BOXES,
                PokedexRoutes.CENTRAL
            ),
            PokedexRoutes.main
        )
    }

    @Test
    fun gameDexPackageKeysStayUniqueAndChampionsStaysOut() {
        val games=AppGameCatalog.adventureGames
        val keys=games.map{RemoteOfflinePackageCatalog.packageKeyForGame(it.label)}
        assertEquals(keys.size,keys.distinct().size)
        assertFalse(games.any{it.label.contains("Champions",ignoreCase=true)})
    }
}
