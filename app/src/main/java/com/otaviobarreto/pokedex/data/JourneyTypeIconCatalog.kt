package com.otaviobarreto.pokedex.data

object JourneyTypeIconCatalog {
    private const val BASE="https://pokesprite.tootaio.com/sprites/types/generation-ix/scarlet-violet/"

    fun allUrls():List<String> = (1..18).map { BASE + it + ".png" }

    fun iconUrl(typeLabel:String):String?{
        val key=typeLabel.lowercase()
        val id=when{
            "normal" in key -> 1
            "lutador" in key -> 2
            "voador" in key -> 3
            "veneno" in key -> 4
            "terra" in key -> 5
            "pedra" in key -> 6
            "inseto" in key -> 7
            "fantasma" in key -> 8
            "aço" in key || "aco" in key -> 9
            "fogo" in key -> 10
            "água" in key || "agua" in key -> 11
            "planta" in key -> 12
            "elétrico" in key || "eletrico" in key -> 13
            "psíquico" in key || "psiquico" in key -> 14
            "gelo" in key -> 15
            "dragão" in key || "dragao" in key -> 16
            "sombrio" in key -> 17
            "fada" in key -> 18
            else -> null
        } ?: return null
        return BASE+id+".png"
    }
}
