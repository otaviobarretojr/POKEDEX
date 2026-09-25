package com.otaviobarreto.pokedex

import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.data.RemoteOfflinePackageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StableFoundationContractTest {
    @Test
    fun secondaryToolsStayOutsideBottomNavigation() {
        listOf(
            PokedexRoutes.SEARCH,
            PokedexRoutes.EVOLUTION_CENTER,
            PokedexRoutes.GAME_DEX,
            PokedexRoutes.POKEMON,
            PokedexRoutes.FORM_DETAIL,
            PokedexRoutes.REFERENCE,
            PokedexRoutes.CAMPAIGN_GUIDE
        ).forEach { route ->
            assertTrue(route, PokedexRoutes.isSecondary(route))
        }
    }

    @Test
    fun mainNavigationContractHasSixDestinations() {
        assertEquals(
            setOf(
                PokedexRoutes.HOME,
                PokedexRoutes.GAMES,
                PokedexRoutes.POKEDEX,
                PokedexRoutes.COLLECTION,
                PokedexRoutes.BOXES,
                PokedexRoutes.CENTRAL
            ),
            PokedexRoutes.main
        )
    }

    @Test
    fun adventurePackageKeysStayUniqueAndChampionsStaysOut() {
        val games=AppGameCatalog.adventureGames
        val keys=games.map{RemoteOfflinePackageCatalog.packageKeyForGame(it.label)}
        assertEquals(keys.size,keys.distinct().size)
        assertFalse(games.any{it.label.contains("Champions",ignoreCase=true)})
    }
}
