package com.otaviobarreto.pokedex.data

enum class JourneyVisualRole { GYM_LEADER, TEAM_STAR_BOSS, TITAN }

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

    private val assets=mapOf(
        "sv-01" to JourneyVisualAsset("sv-01",JourneyVisualRole.GYM_LEADER,"Katy",OFFICIAL+"chara_katy.png","Bug Gym"),
        "sv-02" to JourneyVisualAsset("sv-02",JourneyVisualRole.TITAN,"Klawf",ART+"950.png","Stony Cliff Titan"),
        "sv-03" to JourneyVisualAsset("sv-03",JourneyVisualRole.GYM_LEADER,"Brassius",OFFICIAL+"chara_brassius.png","Grass Gym"),
        "sv-04" to JourneyVisualAsset("sv-04",JourneyVisualRole.TITAN,"Bombirdier",ART+"962.png","Open Sky Titan"),
        "sv-05" to JourneyVisualAsset("sv-05",JourneyVisualRole.TEAM_STAR_BOSS,"Giacomo",OFFICIAL+"chara_giacomo.png","Segin Squad"),
        "sv-06" to JourneyVisualAsset("sv-06",JourneyVisualRole.GYM_LEADER,"Iono",OFFICIAL+"chara_iono.png","Electric Gym"),
        "sv-07" to JourneyVisualAsset("sv-07",JourneyVisualRole.TEAM_STAR_BOSS,"Mela",OFFICIAL+"chara_mela.png","Schedar Squad"),
        "sv-08" to JourneyVisualAsset("sv-08",JourneyVisualRole.TITAN,"Orthworm",ART+"968.png","Lurking Steel Titan"),
        "sv-09" to JourneyVisualAsset("sv-09",JourneyVisualRole.GYM_LEADER,"Kofu",OFFICIAL+"chara_kofu.png","Water Gym"),
        "sv-10" to JourneyVisualAsset("sv-10",JourneyVisualRole.TEAM_STAR_BOSS,"Atticus",OFFICIAL+"chara_atticus.png","Navi Squad"),
        "sv-11" to JourneyVisualAsset("sv-11",JourneyVisualRole.GYM_LEADER,"Larry",OFFICIAL+"chara_larry.png","Normal Gym"),
        "sv-12" to JourneyVisualAsset("sv-12",JourneyVisualRole.GYM_LEADER,"Ryme",OFFICIAL+"chara_ryme.png","Ghost Gym"),
        "sv-13" to JourneyVisualAsset("sv-13",JourneyVisualRole.TITAN,"Great Tusk / Iron Treads",ART+"984.png","Quaking Earth Titan"),
        "sv-14" to JourneyVisualAsset("sv-14",JourneyVisualRole.GYM_LEADER,"Tulip",OFFICIAL+"chara_tulip.png","Psychic Gym"),
        "sv-15" to JourneyVisualAsset("sv-15",JourneyVisualRole.GYM_LEADER,"Grusha",OFFICIAL+"chara_grusha.png","Ice Gym"),
        "sv-16" to JourneyVisualAsset("sv-16",JourneyVisualRole.TEAM_STAR_BOSS,"Ortega",OFFICIAL+"chara_ortega.png","Ruchbah Squad"),
        "sv-17" to JourneyVisualAsset("sv-17",JourneyVisualRole.TITAN,"Dondozo & Tatsugiri",ART+"977.png","False Dragon Titan"),
        "sv-18" to JourneyVisualAsset("sv-18",JourneyVisualRole.TEAM_STAR_BOSS,"Eri",OFFICIAL+"chara_eri.png","Caph Squad")
    )
}
