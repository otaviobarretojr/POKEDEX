package com.otaviobarreto.pokedex.data

/** Maps collection/game labels to version context and regional Pokédex data. */
data class GameContext(
    val label: String,
    private val versions: Set<String>,
    val pokedexSlug: String,
    val regionLabel: String = label,
    private val versionGroups: Set<String> = emptySet()
) {
    fun matchesVersion(version: String): Boolean = normalize(version) in versions
    fun matchesVersionGroup(versionGroup: String): Boolean {
        val normalized = normalize(versionGroup)
        return normalized in versionGroups || versions.any { it in normalized }
    }

    companion object {
        fun fromSource(source: String?): GameContext? {
            val normalized = normalize(source.orEmpty())
            if (normalized.isBlank()) return null

            return when {
                "kitakami" in normalized -> GameContext(
                    label = "Scarlet / Violet",
                    versions = setOf("scarlet", "violet"),
                    pokedexSlug = "kitakami",
                    regionLabel = "Kitakami",
                    versionGroups = setOf("scarlet violet")
                )
                "blueberry" in normalized -> GameContext(
                    label = "Scarlet / Violet",
                    versions = setOf("scarlet", "violet"),
                    pokedexSlug = "blueberry",
                    regionLabel = "Blueberry",
                    versionGroups = setOf("scarlet violet")
                )
                "paldea" in normalized || "scarlet" in normalized || "violet" in normalized -> GameContext(
                    label = "Scarlet / Violet",
                    versions = setOf("scarlet", "violet"),
                    pokedexSlug = "paldea",
                    regionLabel = "Paldea",
                    versionGroups = setOf("scarlet violet")
                )
                "lets go" in normalized -> GameContext(
                    label = "Let's Go Pikachu / Eevee",
                    versions = setOf("lets go pikachu", "lets go eevee"),
                    pokedexSlug = "letsgo-kanto",
                    regionLabel = "Kanto",
                    versionGroups = setOf("lets go pikachu lets go eevee")
                )
                "isle of armor" in normalized -> GameContext(
                    label = "Sword / Shield",
                    versions = setOf("sword", "shield"),
                    pokedexSlug = "isle-of-armor",
                    regionLabel = "Isle of Armor",
                    versionGroups = setOf("sword shield")
                )
                "crown tundra" in normalized -> GameContext(
                    label = "Sword / Shield",
                    versions = setOf("sword", "shield"),
                    pokedexSlug = "crown-tundra",
                    regionLabel = "Crown Tundra",
                    versionGroups = setOf("sword shield")
                )
                "sword" in normalized || "shield" in normalized || "galar" in normalized -> GameContext(
                    label = "Sword / Shield",
                    versions = setOf("sword", "shield"),
                    pokedexSlug = "galar",
                    regionLabel = "Galar",
                    versionGroups = setOf("sword shield")
                )
                "black" in normalized || "white" in normalized || "unova" in normalized -> GameContext(
                    label = "Black / White",
                    versions = setOf("black", "white"),
                    pokedexSlug = "original-unova",
                    regionLabel = "Unova",
                    versionGroups = setOf("black white")
                )
                "kalos central" in normalized -> GameContext(
                    label = "X / Y",
                    versions = setOf("x", "y"),
                    pokedexSlug = "kalos-central",
                    regionLabel = "Central Kalos",
                    versionGroups = setOf("x y")
                )
                "kalos coastal" in normalized -> GameContext(
                    label = "X / Y",
                    versions = setOf("x", "y"),
                    pokedexSlug = "kalos-coastal",
                    regionLabel = "Coastal Kalos",
                    versionGroups = setOf("x y")
                )
                "kalos mountain" in normalized -> GameContext(
                    label = "X / Y",
                    versions = setOf("x", "y"),
                    pokedexSlug = "kalos-mountain",
                    regionLabel = "Mountain Kalos",
                    versionGroups = setOf("x y")
                )
                "omega ruby" in normalized || "alpha sapphire" in normalized || "hoenn" in normalized -> GameContext(
                    label = "Omega Ruby / Alpha Sapphire",
                    versions = setOf("omega ruby", "alpha sapphire"),
                    pokedexSlug = "updated-hoenn",
                    regionLabel = "Hoenn",
                    versionGroups = setOf("omega ruby alpha sapphire")
                )
                "brilliant diamond" in normalized || "shining pearl" in normalized || "sinnoh" in normalized -> GameContext(
                    label = "Brilliant Diamond / Shining Pearl",
                    versions = setOf("brilliant diamond", "shining pearl"),
                    pokedexSlug = "original-sinnoh",
                    regionLabel = "Sinnoh",
                    versionGroups = setOf("brilliant diamond and shining pearl")
                )
                "arceus" in normalized || "hisui" in normalized -> GameContext(
                    label = "Legends Arceus",
                    versions = setOf("legends arceus"),
                    pokedexSlug = "hisui",
                    regionLabel = "Hisui",
                    versionGroups = setOf("legends arceus")
                )
                else -> null
            }
        }

        private fun normalize(value: String): String = value
            .lowercase()
            .replace("'", "")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
