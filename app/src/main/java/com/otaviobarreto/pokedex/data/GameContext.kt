package com.otaviobarreto.pokedex.data

/** Maps collection/box labels to game-version context used by encounter data. */
data class GameContext(
    val label: String,
    private val versions: Set<String>
) {
    fun matchesVersion(version: String): Boolean = normalize(version) in versions

    companion object {
        fun fromSource(source: String?): GameContext? {
            val normalized = normalize(source.orEmpty())
            if (normalized.isBlank()) return null

            return when {
                "scarlet" in normalized || "violet" in normalized -> GameContext(
                    label = "Scarlet / Violet",
                    versions = setOf("scarlet", "violet")
                )
                "lets go" in normalized -> GameContext(
                    label = "Let's Go Pikachu / Eevee",
                    versions = setOf("lets go pikachu", "lets go eevee")
                )
                "sword" in normalized || "shield" in normalized -> GameContext(
                    label = "Sword / Shield",
                    versions = setOf("sword", "shield")
                )
                "arceus" in normalized -> GameContext(
                    label = "Legends Arceus",
                    versions = setOf("legends arceus")
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
