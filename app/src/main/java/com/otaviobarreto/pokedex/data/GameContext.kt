package com.otaviobarreto.pokedex.data

/** Maps collection/box labels to game-version context used by encounter data. */
data class GameContext(
    val label: String,
    val versions: Set<String>
) {
    companion object {
        fun fromSource(source: String?): GameContext? {
            val normalized = source?.trim()?.lowercase().orEmpty()
            if (normalized.isBlank()) return null

            return when {
                normalized.contains("scarlet") || normalized.contains("violet") -> GameContext(
                    label = "Scarlet / Violet",
                    versions = setOf("scarlet", "violet")
                )
                normalized.contains("let's go") || normalized.contains("lets go") -> GameContext(
                    label = "Let's Go Pikachu / Eevee",
                    versions = setOf("lets-go-pikachu", "lets-go-eevee")
                )
                normalized.contains("sword") || normalized.contains("shield") -> GameContext(
                    label = "Sword / Shield",
                    versions = setOf("sword", "shield")
                )
                normalized.contains("arceus") -> GameContext(
                    label = "Legends Arceus",
                    versions = setOf("legends-arceus")
                )
                else -> null
            }
        }
    }
}
