package com.otaviobarreto.pokedex.data

data class JourneyMapPoint(
    val stepId:String,
    val x:Float,
    val y:Float
)

object JourneyMapCatalog {
    fun backgroundUrl(game:String):String? = when(game){
        "Scarlet / Violet" -> null // Paldea usa o mapa oficial incorporado ao APK.
        else -> null
    }

    fun embeddedAsset(game:String):String? = when(game){
        "Scarlet / Violet" -> "maps/paldea_journey_map"
        else -> null
    }

    fun aspectRatio(game:String):Float = when(game){
        "Scarlet / Violet" -> 900f/831f
        else -> 1.414f
    }

    fun points(game:String):List<JourneyMapPoint> = when(game){
        "Scarlet / Violet" -> paldea
        else -> emptyList()
    }

    // Coordenadas calibradas sobre o mapa oficial enviado pelo usuário.
    // O mapa já contém os retratos/ícones; estes pontos são somente hitboxes e estados.
    private val paldea=listOf(
        JourneyMapPoint("sv-01",.310f,.775f), // Katy · Cortondo
        JourneyMapPoint("sv-02",.670f,.741f), // Klawf · Stony Cliff
        JourneyMapPoint("sv-03",.749f,.793f), // Brassius · Artazon
        JourneyMapPoint("sv-04",.161f,.543f), // Bombirdier · Open Sky
        JourneyMapPoint("sv-05",.295f,.635f), // Giacomo · Segin Squad
        JourneyMapPoint("sv-06",.797f,.575f), // Iono · Levincia
        JourneyMapPoint("sv-07",.735f,.690f), // Mela · Schedar Squad
        JourneyMapPoint("sv-08",.794f,.518f), // Orthworm · Lurking Steel
        JourneyMapPoint("sv-09",.344f,.538f), // Kofu · Cascarrafa
        JourneyMapPoint("sv-10",.662f,.444f), // Atticus · Navi Squad
        JourneyMapPoint("sv-11",.426f,.446f), // Larry · Medali
        JourneyMapPoint("sv-12",.556f,.253f), // Ryme · Montenevera
        JourneyMapPoint("sv-13",.146f,.662f), // Great Tusk / Iron Treads
        JourneyMapPoint("sv-14",.228f,.893f), // Tulip · Alfornada
        JourneyMapPoint("sv-15",.598f,.349f), // Grusha · Glaseado
        JourneyMapPoint("sv-16",.484f,.139f), // Ortega · Ruchbah Squad
        JourneyMapPoint("sv-17",.354f,.281f), // Dondozo / Tatsugiri
        JourneyMapPoint("sv-18",.866f,.343f)  // Eri · Caph Squad
    )
}
