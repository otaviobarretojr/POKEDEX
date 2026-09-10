package com.otaviobarreto.pokedex.data

data class RegionMapZone(
    val id: String,
    val label: String,
    val row: Int,
    val column: Int,
    val x: Float,
    val y: Float,
    val matchTerms: List<String>
) {
    fun matches(location: String): Boolean {
        val normalized = normalize(location)
        return matchTerms.any { normalize(it) in normalized }
    }

    private fun normalize(value: String): String = value
        .lowercase()
        .replace("-", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

object RegionMapCatalog {
    fun zones(context: GameContext?): List<RegionMapZone> = when (context?.regionLabel) {
        "Paldea" -> paldea
        "Kitakami" -> kitakami
        "Blueberry" -> blueberry
        else -> emptyList()
    }

    private val paldea = listOf(
        RegionMapZone("north", "North Province", 0, 1, 0.67f, 0.24f, listOf("north province")),
        RegionMapZone("glaseado", "Glaseado Mountain", 0, 2, 0.54f, 0.18f, listOf("glaseado")),
        RegionMapZone("casseroya", "Casseroya Lake", 1, 0, 0.27f, 0.29f, listOf("casseroya")),
        RegionMapZone("tagtree", "Tagtree Thicket", 1, 1, 0.70f, 0.38f, listOf("tagtree")),
        RegionMapZone("east", "East Province", 1, 2, 0.82f, 0.48f, listOf("east province")),
        RegionMapZone("west", "West Province", 2, 0, 0.23f, 0.52f, listOf("west province")),
        RegionMapZone("mesagoza", "Central Paldea", 2, 1, 0.50f, 0.70f, listOf("mesagoza", "south province area one", "south province area three")),
        RegionMapZone("asado", "Asado Desert", 2, 2, 0.24f, 0.60f, listOf("asado")),
        RegionMapZone("south", "South Province", 3, 0, 0.50f, 0.81f, listOf("south province")),
        RegionMapZone("poco", "Poco Path / Coast", 3, 1, 0.57f, 0.90f, listOf("poco path", "south paldean sea")),
        RegionMapZone("zero", "Area Zero", 3, 2, 0.49f, 0.52f, listOf("area zero", "great crater"))
    )

    private val kitakami = listOf(
        RegionMapZone("timeless", "Timeless Woods", 0, 1, 0.61f, 0.15f, listOf("timeless woods")),
        RegionMapZone("barrens", "Paradise Barrens", 0, 2, 0.79f, 0.27f, listOf("paradise barrens")),
        RegionMapZone("mountain", "Oni Mountain", 1, 1, 0.50f, 0.44f, listOf("oni mountain", "crystal pool", "infernal pass")),
        RegionMapZone("gorge", "Fellhorn Gorge", 1, 2, 0.72f, 0.48f, listOf("fellhorn gorge")),
        RegionMapZone("fields", "Wistful Fields", 2, 0, 0.20f, 0.50f, listOf("wistful fields")),
        RegionMapZone("apple", "Apple Hills", 2, 1, 0.38f, 0.63f, listOf("apple hills")),
        RegionMapZone("mossfell", "Mossfell Confluence", 2, 2, 0.74f, 0.72f, listOf("mossfell")),
        RegionMapZone("road", "Kitakami Road", 3, 0, 0.27f, 0.82f, listOf("kitakami road")),
        RegionMapZone("revelers", "Reveler's Road", 3, 1, 0.49f, 0.78f, listOf("reveler", "revelers road")),
        RegionMapZone("waterhead", "Chilling Waterhead", 3, 2, 0.63f, 0.88f, listOf("chilling waterhead"))
    )

    private val blueberry = listOf(
        RegionMapZone("polar", "Polar Biome", 0, 1, 0.50f, 0.18f, listOf("polar biome")),
        RegionMapZone("canyon", "Canyon Biome", 1, 0, 0.22f, 0.50f, listOf("canyon biome")),
        RegionMapZone("plaza", "Central Plaza", 1, 1, 0.50f, 0.50f, listOf("central plaza", "terarium")),
        RegionMapZone("coastal", "Coastal Biome", 1, 2, 0.78f, 0.50f, listOf("coastal biome")),
        RegionMapZone("savanna", "Savanna Biome", 2, 1, 0.50f, 0.80f, listOf("savanna biome"))
    )
}
