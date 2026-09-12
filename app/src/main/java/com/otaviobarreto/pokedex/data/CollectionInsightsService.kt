package com.otaviobarreto.pokedex.data

data class CollectionInsights(
    val totalCaptured: Int,
    val gamesWithProgress: Int,
    val totalGames: Int,
    val contextualRegistrations: Int,
    val duplicates: Int,
    val unboxed: Int
)

object CollectionInsightsService {
    fun current(): CollectionInsights {
        val contextual = CollectionStore.contextualCapturedIds
        return CollectionInsights(
            totalCaptured = CollectionStore.capturedIds.size,
            gamesWithProgress = AppGameCatalog.adventureGames.count { game ->
                game.regions.any { region -> contextual[region.source].orEmpty().isNotEmpty() }
            },
            totalGames = AppGameCatalog.adventureGames.size,
            contextualRegistrations = contextual.values.sumOf { it.size },
            duplicates = CollectionStore.duplicateIds().size,
            unboxed = CollectionStore.unboxedCapturedIds().size
        )
    }
}
