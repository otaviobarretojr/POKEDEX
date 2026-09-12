package com.otaviobarreto.pokedex.data

data class GameCollectionProgress(
    val game: String,
    val captured: Int,
    val regionsWithProgress: Int,
    val totalRegions: Int
)

data class CollectionInsights(
    val totalCaptured: Int,
    val gamesWithProgress: Int,
    val totalGames: Int,
    val contextualRegistrations: Int,
    val duplicates: Int,
    val unboxed: Int,
    val nationalDexTotal: Int,
    val livingDexRatio: Float,
    val ownedForms: Int,
    val shinyVariants: Int,
    val speciesWithVariants: Int,
    val byGame: List<GameCollectionProgress>
)

object CollectionInsightsService {
    fun current(): CollectionInsights {
        val contextual = CollectionStore.contextualCapturedIds
        val byGame = AppGameCatalog.adventureGames.map { game ->
            val regionSets = game.regions.map { region -> contextual[region.source].orEmpty() }
            GameCollectionProgress(
                game = game.label,
                captured = regionSets.flatten().toSet().size,
                regionsWithProgress = regionSets.count { it.isNotEmpty() },
                totalRegions = game.regions.size
            )
        }
        return CollectionInsights(
            totalCaptured = CollectionStore.capturedIds.size,
            gamesWithProgress = byGame.count { it.captured > 0 },
            totalGames = AppGameCatalog.adventureGames.size,
            contextualRegistrations = contextual.values.sumOf { it.size },
            duplicates = CollectionStore.duplicateIds().size,
            unboxed = CollectionStore.unboxedCapturedIds().size,
            nationalDexTotal = PokeApiService.MAX_NATIONAL_DEX_ID,
            livingDexRatio = (CollectionStore.capturedIds.size.toFloat() / PokeApiService.MAX_NATIONAL_DEX_ID.coerceAtLeast(1)).coerceIn(0f,1f),
            ownedForms = VariantCollectionStore.formCount(),
            shinyVariants = VariantCollectionStore.shinyCount(),
            speciesWithVariants = VariantCollectionStore.speciesWithVariants(),
            byGame = byGame
        )
    }
}
