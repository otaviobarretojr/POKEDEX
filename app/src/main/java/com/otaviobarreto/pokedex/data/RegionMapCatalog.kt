package com.otaviobarreto.pokedex.data

data class RegionMapZone(
    val id: String,
    val label: String,
    val row: Int,
    val column: Int,
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
        RegionMapZone("north", "North Province", 0, 1, listOf("north province")),
        RegionMapZone("glaseado", "Glaseado Mountain", 0, 2, listOf("glaseado")),
        RegionMapZone("casseroya", "Casseroya Lake", 1, 0, listOf("casseroya")),
        RegionMapZone("tagtree", "Tagtree Thicket", 1, 1, listOf("tagtree")),
        RegionMapZone("east", "East Province", 1, 2, listOf("east province")),
        RegionMapZone("west", "West Province", 2, 0, listOf("west province")),
        RegionMapZone("mesagoza", "Central Paldea", 2, 1, listOf("mesagoza", "south province area one", "south province area three")),
        RegionMapZone("asado", "Asado Desert", 2, 2, listOf("asado")),
        RegionMapZone("south", "South Province", 3, 0, listOf("south province")),
        RegionMapZone("poco", "Poco Path / Coast", 3, 1, listOf("poco path", "south paldean sea")),
        RegionMapZone("zero", "Area Zero", 3, 2, listOf("area zero", "great crater"))
    )

    private val kitakami = listOf(
        RegionMapZone("timeless", "Timeless Woods", 0, 1, listOf("timeless woods")),
        RegionMapZone("barrens", "Paradise Barrens", 0, 2, listOf("paradise barrens")),
        RegionMapZone("mountain", "Oni Mountain", 1, 1, listOf("oni mountain", "crystal pool", "infernal pass")),
        RegionMapZone("gorge", "Fellhorn Gorge", 1, 2, listOf("fellhorn gorge")),
        RegionMapZone("fields", "Wistful Fields", 2, 0, listOf("wistful fields")),
        RegionMapZone("apple", "Apple Hills", 2, 1, listOf("apple hills")),
        RegionMapZone("mossfell", "Mossfell Confluence", 2, 2, listOf("mossfell")),
        RegionMapZone("road", "Kitakami Road", 3, 0, listOf("kitakami road")),
        RegionMapZone("revelers", "Reveler's Road", 3, 1, listOf("reveler", "revelers road")),
        RegionMapZone("waterhead", "Chilling Waterhead", 3, 2, listOf("chilling waterhead"))
    )

    private val blueberry = listOf(
        RegionMapZone("polar", "Polar Biome", 0, 1, listOf("polar biome")),
        RegionMapZone("canyon", "Canyon Biome", 1, 0, listOf("canyon biome")),
        RegionMapZone("plaza", "Central Plaza", 1, 1, listOf("central plaza", "terarium")),
        RegionMapZone("coastal", "Coastal Biome", 1, 2, listOf("coastal biome")),
        RegionMapZone("savanna", "Savanna Biome", 2, 1, listOf("savanna biome"))
    )
}
