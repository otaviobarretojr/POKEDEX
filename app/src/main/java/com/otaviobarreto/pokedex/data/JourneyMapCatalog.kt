package com.otaviobarreto.pokedex.data

data class JourneyMapPoint(
    val stepId:String,
    val x:Float,
    val y:Float
)

object JourneyMapCatalog {
    fun backgroundUrl(game:String):String? = when(game){
        "Scarlet / Violet" -> "https://www.pokemon.co.jp/ex/sv/assets/img/story/ja/220803_02/story_img_01.jpg"
        else -> null
    }

    fun points(game:String):List<JourneyMapPoint> = when(game){
        "Scarlet / Violet" -> paldea
        else -> emptyList()
    }

    private val paldea=listOf(
        JourneyMapPoint("sv-01",.39f,.75f), // Cortondo
        JourneyMapPoint("sv-02",.64f,.72f), // Stony Cliff
        JourneyMapPoint("sv-03",.72f,.64f), // Artazon
        JourneyMapPoint("sv-04",.23f,.53f), // Open Sky Titan
        JourneyMapPoint("sv-05",.18f,.47f), // Segin Squad
        JourneyMapPoint("sv-06",.84f,.55f), // Levincia
        JourneyMapPoint("sv-07",.78f,.42f), // Schedar Squad
        JourneyMapPoint("sv-08",.67f,.43f), // Lurking Steel Titan
        JourneyMapPoint("sv-09",.29f,.54f), // Cascarrafa
        JourneyMapPoint("sv-10",.70f,.30f), // Navi Squad
        JourneyMapPoint("sv-11",.35f,.43f), // Medali
        JourneyMapPoint("sv-12",.50f,.22f), // Montenevera
        JourneyMapPoint("sv-13",.34f,.64f), // Asado Desert
        JourneyMapPoint("sv-14",.18f,.76f), // Alfornada
        JourneyMapPoint("sv-15",.49f,.15f), // Glaseado
        JourneyMapPoint("sv-16",.61f,.18f), // Ruchbah Squad
        JourneyMapPoint("sv-17",.30f,.27f), // Casseroya Lake
        JourneyMapPoint("sv-18",.82f,.27f)  // Caph Squad
    )
}
