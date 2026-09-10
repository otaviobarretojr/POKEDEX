package com.otaviobarreto.pokedex.data

data class AppRegion(
    val label: String,
    val source: String,
    val subtitle: String
)

data class AppGame(
    val label: String,
    val regions: List<AppRegion>
)

object AppGameCatalog {
    val games = listOf(
        AppGame(
            "Scarlet / Violet",
            listOf(
                AppRegion("Paldea", "Scarlet / Violet · Paldea", "Jogo base"),
                AppRegion("Kitakami", "Scarlet / Violet · Kitakami", "DLC · The Teal Mask"),
                AppRegion("Blueberry", "Scarlet / Violet · Blueberry", "DLC · The Indigo Disk")
            )
        ),
        AppGame(
            "Sword / Shield",
            listOf(
                AppRegion("Galar", "Sword / Shield · Galar", "Jogo base"),
                AppRegion("Isle of Armor", "Sword / Shield · Isle of Armor", "DLC · The Isle of Armor"),
                AppRegion("Crown Tundra", "Sword / Shield · Crown Tundra", "DLC · The Crown Tundra")
            )
        ),
        AppGame("Let's Go Pikachu / Eevee", listOf(AppRegion("Kanto", "Let's Go Pikachu / Eevee · Kanto", "Jogo base"))),
        AppGame("Legends Arceus", listOf(AppRegion("Hisui", "Legends Arceus · Hisui", "Jogo base"))),
        AppGame("Brilliant Diamond / Shining Pearl", listOf(AppRegion("Sinnoh", "Brilliant Diamond / Shining Pearl · Sinnoh", "Jogo base")))
    )
}
