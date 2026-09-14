package com.otaviobarreto.pokedex.data

object RegionalDexLayering {

    data class LayerResult(
        val exclusiveSpeciesIds:Set<Int>,
        val exclusiveEntries:List<GameDexService.GameDexEntry>,
        val novelFormIdentities:Set<Pair<Int,String>>
    ){
        val novelFormTargetIds:Set<Int> get()=novelFormIdentities.mapTo(linkedSetOf()){it.first}
    }

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

    fun layeredResult(
        game:AppGame,
        regionSource:String,
        entriesBySource:Map<String,List<GameDexService.GameDexEntry>>,
        routesBySource:Map<String,List<EvolutionRoute>>
    ):LayerResult{
        val selectedIndex=game.regions.indexOfFirst{it.source==regionSource}
        if(selectedIndex<0) return LayerResult(emptySet(),emptyList(),emptySet())

        val speciesIds=exclusiveIdsForRegion(game,regionSource,entriesBySource)
        val entries=exclusiveEntriesForRegion(game,regionSource,entriesBySource)

        if(selectedIndex==0){
            return LayerResult(speciesIds,entries,emptySet())
        }

        val previousFormKeys=game.regions.take(selectedIndex)
            .flatMap{region->routesBySource[region.source].orEmpty()}
            .mapNotNull{route->route.targetFormKey?.let{route.targetPokemonId to it}}
            .toSet()

        val novelFormIdentities=routesBySource[regionSource].orEmpty()
            .mapNotNull{route->
                val formKey=route.targetFormKey ?: return@mapNotNull null
                (route.targetPokemonId to formKey).takeIf{it !in previousFormKeys}
            }
            .toSet()

        return LayerResult(
            exclusiveSpeciesIds=speciesIds,
            exclusiveEntries=entries,
            novelFormIdentities=novelFormIdentities
        )
    }
}
