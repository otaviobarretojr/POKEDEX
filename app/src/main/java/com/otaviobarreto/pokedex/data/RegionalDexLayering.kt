package com.otaviobarreto.pokedex.data

object RegionalDexLayering {

    fun exclusiveIdsForRegion(
        game: AppGame,
        regionSource: String,
        entriesBySource: Map<String, List<GameDexService.GameDexEntry>>
    ): Set<Int> {
        val selectedIndex = game.regions.indexOfFirst { it.source == regionSource }
        if (selectedIndex < 0) return emptySet()

        val selectedIds = entriesBySource[regionSource].orEmpty()
            .mapTo(linkedSetOf()) { it.nationalId }

        if (selectedIndex == 0) return selectedIds

        val seenBefore = game.regions
            .take(selectedIndex)
            .flatMap { region -> entriesBySource[region.source].orEmpty() }
            .mapTo(hashSetOf()) { it.nationalId }

        return selectedIds.filterTo(linkedSetOf()) { it !in seenBefore }
    }

    fun exclusiveEntriesForRegion(
        game: AppGame,
        regionSource: String,
        entriesBySource: Map<String, List<GameDexService.GameDexEntry>>
    ): List<GameDexService.GameDexEntry> {
        val exclusiveIds = exclusiveIdsForRegion(game, regionSource, entriesBySource)
        return entriesBySource[regionSource].orEmpty()
            .filter { it.nationalId in exclusiveIds }
    }
}
