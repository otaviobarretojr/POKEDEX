package com.otaviobarreto.pokedex.data

enum class JourneyVisualRole { GYM_LEADER, TEAM_STAR_BOSS, TITAN, STORY, TOURNAMENT, RAID, EXPLORATION, DLC_CHARACTER, LEGENDARY, EPILOGUE }

data class JourneyVisualAsset(
    val stepId:String,
    val role:JourneyVisualRole,
    val subject:String,
    val imageUrl:String,
    val emblemLabel:String,
    val sourceLabel:String="Official Pokémon Scarlet / Violet"
)

object JourneyVisualAssetCatalog {
    fun forStep(stepId:String):JourneyVisualAsset?=assets[stepId]

    private const val OFFICIAL="https://www.pokemon.co.jp/ex/sv/ja/assets/img/character/"
    private const val ART="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"
    private const val DLC_OFFICIAL="https://www.pokemon.co.jp/ex/sv_dlc/assets/img/character/"

    private val assets=mapOf(
        "sv-01" to JourneyVisualAsset("sv-01",JourneyVisualRole.GYM_LEADER,"Katy","https://www.pokemon.co.jp/ex/sv/assets/img/character/230112_01/img_01.jpg","Bug Gym"),
        "sv-02" to JourneyVisualAsset("sv-02",JourneyVisualRole.TITAN,"Klawf",ART+"950.png","Stony Cliff Titan"),
        "sv-03" to JourneyVisualAsset("sv-03",JourneyVisualRole.GYM_LEADER,"Brassius","https://www.pokemon.co.jp/ex/sv/assets/img/character/220907_02/ja/img_01.jpg","Grass Gym"),
        "sv-04" to JourneyVisualAsset("sv-04",JourneyVisualRole.TITAN,"Bombirdier",ART+"962.png","Open Sky Titan"),
        "sv-05" to JourneyVisualAsset("sv-05",JourneyVisualRole.TEAM_STAR_BOSS,"Giacomo","https://www.pokemon.co.jp/ex/sv/assets/img/character/230112_06/img_01.jpg","Segin Squad"),
        "sv-06" to JourneyVisualAsset("sv-06",JourneyVisualRole.GYM_LEADER,"Iono","https://www.pokemon.co.jp/ex/sv/assets/img/character/221014_01/ja/img_01.jpg","Electric Gym"),
        "sv-07" to JourneyVisualAsset("sv-07",JourneyVisualRole.TEAM_STAR_BOSS,"Mela","https://www.pokemon.co.jp/ex/sv/assets/img/character/220907_03/ja/img_01.jpg","Schedar Squad"),
        "sv-08" to JourneyVisualAsset("sv-08",JourneyVisualRole.TITAN,"Orthworm",ART+"968.png","Lurking Steel Titan"),
        "sv-09" to JourneyVisualAsset("sv-09",JourneyVisualRole.GYM_LEADER,"Kofu","https://www.pokemon.co.jp/ex/sv/assets/img/character/230112_02/img_01.jpg","Water Gym"),
        "sv-10" to JourneyVisualAsset("sv-10",JourneyVisualRole.TEAM_STAR_BOSS,"Atticus","https://www.pokemon.co.jp/ex/sv/assets/img/character/230112_08/img_01.jpg","Navi Squad"),
        "sv-11" to JourneyVisualAsset("sv-11",JourneyVisualRole.GYM_LEADER,"Larry","https://www.pokemon.co.jp/ex/sv/assets/img/character/230112_03/img_01.jpg","Normal Gym"),
        "sv-12" to JourneyVisualAsset("sv-12",JourneyVisualRole.GYM_LEADER,"Ryme","https://www.pokemon.co.jp/ex/sv/assets/img/character/230112_05/img_01.jpg","Ghost Gym"),
        "sv-13" to JourneyVisualAsset("sv-13",JourneyVisualRole.TITAN,"Great Tusk / Iron Treads",ART+"984.png","Quaking Earth Titan"),
        "sv-14" to JourneyVisualAsset("sv-14",JourneyVisualRole.GYM_LEADER,"Tulip","https://www.pokemon.co.jp/ex/sv/assets/img/character/230112_04/img_01.jpg","Psychic Gym"),
        "sv-15" to JourneyVisualAsset("sv-15",JourneyVisualRole.GYM_LEADER,"Grusha","https://www.pokemon.co.jp/ex/sv/assets/img/character/220803_05/ja/img_01.jpg","Ice Gym"),
        "sv-16" to JourneyVisualAsset("sv-16",JourneyVisualRole.TEAM_STAR_BOSS,"Ortega","https://www.pokemon.co.jp/ex/sv/assets/img/character/230112_09/img_01.jpg","Ruchbah Squad"),
        "sv-17" to JourneyVisualAsset("sv-17",JourneyVisualRole.TITAN,"Dondozo & Tatsugiri",ART+"977.png","False Dragon Titan"),
        "sv-18" to JourneyVisualAsset("sv-18",JourneyVisualRole.TEAM_STAR_BOSS,"Eri","https://www.pokemon.co.jp/ex/sv/assets/img/character/230112_07/img_01.jpg","Caph Squad"),
        "sv-pg-01" to JourneyVisualAsset("sv-pg-01",JourneyVisualRole.STORY,"Geeta","https://www.pokemon.co.jp/ex/sv/assets/img/character/220907_01/ja/img_01.jpg","Pokémon League"),
        "sv-pg-02" to JourneyVisualAsset("sv-pg-02",JourneyVisualRole.STORY,"Arven","https://www.pokemon.co.jp/ex/sv/assets/img/character/220803_01/ja/img_01.jpg","Path of Legends Finale"),
        "sv-pg-03" to JourneyVisualAsset("sv-pg-03",JourneyVisualRole.STORY,"Penny","https://www.pokemon.co.jp/ex/sv/assets/img/character/220803_02/ja/img_01.jpg","Starfall Street Finale"),
        "sv-pg-04" to JourneyVisualAsset("sv-pg-04",JourneyVisualRole.STORY,"Koraidon / Miraidon",ART+"1007.png","The Way Home · Area Zero"),
        "sv-pg-05" to JourneyVisualAsset("sv-pg-05",JourneyVisualRole.GYM_LEADER,"Gym Leaders","https://www.pokemon.co.jp/ex/sv/assets/img/character/230112_03/img_01.jpg","8 Gym Rematches"),
        "sv-pg-06" to JourneyVisualAsset("sv-pg-06",JourneyVisualRole.TOURNAMENT,"Academy Ace Tournament","https://www.pokemon.co.jp/ex/sv/assets/img/character/220601_02/ja/img_01.jpg","Academy Tournament"),
        "sv-pg-07" to JourneyVisualAsset("sv-pg-07",JourneyVisualRole.RAID,"Black Crystal Tera Raid",ART+"1000.png","6★ Tera Raids"),
        "sv-pg-08" to JourneyVisualAsset("sv-pg-08",JourneyVisualRole.EXPLORATION,"Treasures of Ruin",ART+"1004.png","Paldea Endgame"),
        "sv-dlc-01" to JourneyVisualAsset("sv-dlc-01",JourneyVisualRole.DLC_CHARACTER,"Carmine",DLC_OFFICIAL+"230228_02.png","The Teal Mask"),
        "sv-dlc-02" to JourneyVisualAsset("sv-dlc-02",JourneyVisualRole.LEGENDARY,"Ogerpon",ART+"1017.png","Festival of Masks"),
        "sv-dlc-03" to JourneyVisualAsset("sv-dlc-03",JourneyVisualRole.DLC_CHARACTER,"Kieran",DLC_OFFICIAL+"230228_03.png","Crystal Pool"),
        "sv-dlc-04" to JourneyVisualAsset("sv-dlc-04",JourneyVisualRole.DLC_CHARACTER,"Kieran",DLC_OFFICIAL+"230228_03.png","Loyalty Plaza"),
        "sv-dlc-05" to JourneyVisualAsset("sv-dlc-05",JourneyVisualRole.LEGENDARY,"The Loyal Three",ART+"1014.png","Okidogi · Munkidori · Fezandipiti"),
        "sv-dlc-06" to JourneyVisualAsset("sv-dlc-06",JourneyVisualRole.LEGENDARY,"Ogerpon",ART+"1017.png","Final de The Teal Mask"),
        "sv-dlc-07" to JourneyVisualAsset("sv-dlc-07",JourneyVisualRole.DLC_CHARACTER,"Perrin",DLC_OFFICIAL+"230808_05.png","Bloodmoon Ursaluna"),
        "sv-dlc-08" to JourneyVisualAsset("sv-dlc-08",JourneyVisualRole.DLC_CHARACTER,"Briar",DLC_OFFICIAL+"230808_01.png","Blueberry Academy"),
        "sv-dlc-09" to JourneyVisualAsset("sv-dlc-09",JourneyVisualRole.DLC_CHARACTER,"Crispin",DLC_OFFICIAL+"230808_02.png?v=230813","BB Elite Four"),
        "sv-dlc-10" to JourneyVisualAsset("sv-dlc-10",JourneyVisualRole.DLC_CHARACTER,"Amarys",DLC_OFFICIAL+"230808_03.png","BB Elite Four"),
        "sv-dlc-11" to JourneyVisualAsset("sv-dlc-11",JourneyVisualRole.DLC_CHARACTER,"Lacey",DLC_OFFICIAL+"230622_02.png","BB Elite Four"),
        "sv-dlc-12" to JourneyVisualAsset("sv-dlc-12",JourneyVisualRole.DLC_CHARACTER,"Drayton",DLC_OFFICIAL+"230808_04.png","BB Elite Four"),
        "sv-dlc-13" to JourneyVisualAsset("sv-dlc-13",JourneyVisualRole.DLC_CHARACTER,"Kieran",DLC_OFFICIAL+"230228_03.png","BB League Champion"),
        "sv-dlc-14" to JourneyVisualAsset("sv-dlc-14",JourneyVisualRole.DLC_CHARACTER,"Briar",DLC_OFFICIAL+"230808_01.png","Area Zero Underdepths"),
        "sv-dlc-15" to JourneyVisualAsset("sv-dlc-15",JourneyVisualRole.LEGENDARY,"Terapagos",ART+"1024.png","Final de The Indigo Disk"),
        "sv-dlc-16" to JourneyVisualAsset("sv-dlc-16",JourneyVisualRole.EXPLORATION,"Blueberry Endgame",ART+"1024.png","League Club · Lendários"),
        "sv-epi-01" to JourneyVisualAsset("sv-epi-01",JourneyVisualRole.EPILOGUE,"Pecharunt",ART+"1025.png","Mochi Mayhem"),
        "sv-epi-02" to JourneyVisualAsset("sv-epi-02",JourneyVisualRole.EPILOGUE,"Pecharunt",ART+"1025.png","Mochi Mayhem"),
        "sv-epi-03" to JourneyVisualAsset("sv-epi-03",JourneyVisualRole.DLC_CHARACTER,"Nemona","https://www.pokemon.co.jp/ex/sv/assets/img/character/220601_02/ja/img_01.jpg","Mochi Mayhem"),
        "sv-epi-04" to JourneyVisualAsset("sv-epi-04",JourneyVisualRole.EPILOGUE,"Pecharunt",ART+"1025.png","Final do epílogo")
    )
}
