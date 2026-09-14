package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteOfflinePackageCatalogTest {
    @Test
    fun everyAdventureGameMapsToExpectedServerPackage() {
        val expected=mapOf(
            "Pokémon Legends: Z-A" to "legends-za",
            "Scarlet / Violet" to "scarlet-violet",
            "Sword / Shield" to "sword-shield",
            "Let's Go Pikachu / Eevee" to "lets-go",
            "Legends Arceus" to "legends-arceus",
            "Brilliant Diamond / Shining Pearl" to "bdsp",
            "FireRed / LeafGreen" to "firered-leafgreen"
        )
        AppGameCatalog.adventureGames.forEach{game->
            assertEquals(
                game.label,
                expected.getValue(game.label),
                RemoteOfflinePackageCatalog.packageKeyForGame(game.label)
            )
        }
    }
}
