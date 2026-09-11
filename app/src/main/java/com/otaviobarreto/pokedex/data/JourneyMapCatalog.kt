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
        JourneyMapPoint("sv-01",.24f,.68f),
        JourneyMapPoint("sv-02",.63f,.66f),
        JourneyMapPoint("sv-03",.71f,.60f),
        JourneyMapPoint("sv-04",.16f,.54f),
        JourneyMapPoint("sv-05",.20f,.48f),
        JourneyMapPoint("sv-06",.79f,.49f),
        JourneyMapPoint("sv-07",.72f,.43f),
        JourneyMapPoint("sv-08",.66f,.36f),
        JourneyMapPoint("sv-09",.28f,.39f),
        JourneyMapPoint("sv-10",.62f,.28f),
        JourneyMapPoint("sv-11",.36f,.31f),
        JourneyMapPoint("sv-12",.49f,.17f),
        JourneyMapPoint("sv-13",.23f,.63f),
        JourneyMapPoint("sv-14",.18f,.29f),
        JourneyMapPoint("sv-15",.46f,.11f),
        JourneyMapPoint("sv-16",.54f,.08f),
        JourneyMapPoint("sv-17",.38f,.15f),
        JourneyMapPoint("sv-18",.70f,.18f)
    )
}
