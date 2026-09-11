package com.otaviobarreto.pokedex.data

enum class AppGameKind { ADVENTURE, BATTLE }

data class AppRegion(
    val label: String,
    val source: String,
    val subtitle: String
)

data class AppGame(
    val label: String,
    val regions: List<AppRegion>,
    val subtitle: String = "",
    val kind: AppGameKind = AppGameKind.ADVENTURE
)

object AppGameCatalog {
    /**
     * Current Pokémon titles available on Nintendo Switch / Switch 2 that map
     * cleanly to a Pokédex or supported battle roster in this app.
     */
    val games = listOf(
        AppGame(
            "Pokémon Legends: Z-A",
            listOf(
                AppRegion("Lumiose", "Pokémon Legends: Z-A · Lumiose", "Jogo base · Lumiose Pokédex"),
                AppRegion("Hyperspace", "Pokémon Legends: Z-A · Hyperspace", "Mega Dimension · Hyperspace Pokédex")
            ),
            subtitle = "Lumiose + Mega Dimension · Switch / Switch 2"
        ),
        AppGame(
            "Scarlet / Violet",
            listOf(
                AppRegion("Paldea", "Scarlet / Violet · Paldea", "Jogo base"),
                AppRegion("Kitakami", "Scarlet / Violet · Kitakami", "DLC · The Teal Mask"),
                AppRegion("Blueberry", "Scarlet / Violet · Blueberry", "DLC · The Indigo Disk")
            ),
            subtitle = "Paldea + The Hidden Treasure of Area Zero"
        ),
        AppGame(
            "Sword / Shield",
            listOf(
                AppRegion("Galar", "Sword / Shield · Galar", "Jogo base"),
                AppRegion("Isle of Armor", "Sword / Shield · Isle of Armor", "DLC · The Isle of Armor"),
                AppRegion("Crown Tundra", "Sword / Shield · Crown Tundra", "DLC · The Crown Tundra")
            ),
            subtitle = "Galar + Expansion Pass"
        ),
        AppGame(
            "Let's Go Pikachu / Eevee",
            listOf(AppRegion("Kanto", "Let's Go Pikachu / Eevee · Kanto", "Jogo base")),
            subtitle = "Kanto"
        ),
        AppGame(
            "Legends Arceus",
            listOf(AppRegion("Hisui", "Legends Arceus · Hisui", "Jogo base")),
            subtitle = "Hisui"
        ),
        AppGame(
            "Brilliant Diamond / Shining Pearl",
            listOf(AppRegion("Sinnoh", "Brilliant Diamond / Shining Pearl · Sinnoh", "Jogo base")),
            subtitle = "Sinnoh"
        ),
        AppGame(
            "FireRed / LeafGreen",
            listOf(AppRegion("Kanto", "FireRed / LeafGreen · Kanto", "Kanto + Sevii Islands · HOME em outubro de 2026")),
            subtitle = "Kanto + Sevii Islands · relançamento Switch 2026"
        ),
        AppGame(
            "Pokémon Champions",
            listOf(AppRegion("Roster", "Pokémon Champions · Roster", "Roster Ranch · Pokémon compatíveis")),
            subtitle = "Batalhas · Roster Ranch · HOME",
            kind = AppGameKind.BATTLE
        )
    )

    val adventureGames: List<AppGame> get() = games.filter { it.kind == AppGameKind.ADVENTURE }
    val battleGames: List<AppGame> get() = games.filter { it.kind == AppGameKind.BATTLE }
}
