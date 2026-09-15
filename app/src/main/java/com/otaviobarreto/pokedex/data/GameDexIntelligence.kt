package com.otaviobarreto.pokedex.data

data class GameDexOverview(
 val game:String,
 val regions:List<RegionDexOverview>
)
data class RegionDexOverview(
 val region:AppRegion,
 val total:Int,
 val caught:Int,
 val missing:Int
)

object GameDexIntelligence {
 fun overview(gameLabel:String):GameDexOverview{
  val game=AppGameCatalog.games.firstOrNull{it.label==gameLabel} ?: return GameDexOverview(gameLabel,emptyList())
  val regions=game.regions.map{region->
   val ctx=GameContext.fromSource(region.source)
   val dex=ctx?.let{runCatching{GameDexService.cached(it) ?: GameDexService.loadGameDex(it)}.getOrNull()}.orEmpty()
   val ids=dex.map{it.nationalId}.toSet()
   val caught=ids.count{PokemonCollectionStore.isCaught(it)}
   RegionDexOverview(region,ids.size,caught,(ids.size-caught).coerceAtLeast(0))
  }
  return GameDexOverview(gameLabel,regions)
 }
}
