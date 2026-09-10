package com.otaviobarreto.pokedex.data

/** Maps collection/game labels to version context and regional Pokédex data. */
data class GameContext(
    val label: String,
    private val versions: Set<String>,
    val pokedexSlug: String,
    val regionLabel: String = label
) {
    fun matchesVersion(version: String): Boolean = normalize(version) in versions

    companion object {
        fun fromSource(source: String?): GameContext? {
            val normalized = normalize(source.orEmpty())
            if (normalized.isBlank()) return null

            return when {
                "kitakami" in normalized -> GameContext(
                    label = "Scarlet / Violet",
                    versions = setOf("scarlet", "violet"),
                    pokedexSlug = "kitakami",
                    regionLabel = "Kitakami"
                )
                "blueberry" in normalized -> GameContext(
                    label = "Scarlet / Violet",
                    versions = setOf("scarlet", "violet"),
                    pokedexSlug = "blueberry",
                    regionLabel = "Blueberry"
                )
                "paldea" in normalized || "scarlet" in normalized || "violet" in normalized -> GameContext(
                    label = "Scarlet / Violet",
                    versions = setOf("scarlet", "violet"),
                    pokedexSlug = "paldea",
                    regionLabel = "Paldea"
                )
                "lets go" in normalized -> GameContext(
                    label = "Let's Go Pikachu / Eevee",
                    versions = setOf("lets go pikachu", "lets go eevee"),
                    pokedexSlug = "letsgo-kanto",
                    regionLabel = "Kanto"
                )
                "sword" in normalized || "shield" in normalized || "galar" in normalized -> GameContext(
                    label = "Sword / Shield",
                    versions = setOf("sword", "shield"),
                    pokedexSlug = "galar",
                    regionLabel = "Galar"
                )
                "arceus" in normalized || "hisui" in normalized -> GameContext(
                    label = "Legends Arceus",
                    versions = setOf("legends arceus"),
                    pokedexSlug = "hisui",
                    regionLabel = "Hisui"
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
