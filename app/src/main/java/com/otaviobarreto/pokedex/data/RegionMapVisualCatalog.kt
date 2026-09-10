package com.otaviobarreto.pokedex.data

data class RegionMapVisual(
    val imageUrl: String,
    val sourceLabel: String,
    val sourceUrl: String,
    val licenseLabel: String,
    val isOfficial: Boolean
)

object RegionMapVisualCatalog {
    fun visualFor(regionLabel: String): RegionMapVisual? = when (regionLabel) {
        "Paldea" -> RegionMapVisual(
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/9/94/Paldea_Map.png",
            sourceLabel = "Paldea Map · Ztash / Wikimedia Commons",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Paldea_Map.png",
            licenseLabel = "CC0 1.0 · recreação cartográfica em 2048×2048",
            isOfficial = false
        )
        else -> null
    }
}
