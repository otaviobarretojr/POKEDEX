package com.otaviobarreto.pokedex.data

enum class ObtainMethod { CAPTURE, EVOLUTION, TRANSFER_OR_TRADE, UNKNOWN }
data class CapturePlanEntry(
 val pokemonId:Int,
 val name:String,
 val method:ObtainMethod,
 val summary:String,
 val locations:List<String> = emptyList()
)

object CapturePlanner {
 fun plan(source:String,dex:List<GameDexService.GameDexEntry>):List<CapturePlanEntry>{
  val context=GameContext.fromSource(source)
  val missing=dex.filterNot{CollectionStore.isCapturedIn(source,it.nationalId)}
  val routes=runCatching{EvolutionFilterIndex.buildRoutes(source,dex)}.getOrDefault(emptyList())
  return missing.map{entry->
   val encounters=runCatching{PokeApiService.loadEncounters(entry.nationalId)}.getOrDefault(emptyList())
   val filtered=LocationIntelligence.filter(encounters,context)
   val evolution=routes.firstOrNull{it.targetPokemonId==entry.nationalId && EvolutionResolutionEngine.executable(it)}
   when{
    filtered.isNotEmpty()->CapturePlanEntry(entry.nationalId,entry.name,ObtainMethod.CAPTURE,"Capturar em "+LocationIntelligence.displayName(filtered.first().location),filtered.take(4).map{LocationIntelligence.displayName(it.location)})
    evolution!=null->CapturePlanEntry(entry.nationalId,entry.name,ObtainMethod.EVOLUTION,evolution.summary)
    else->CapturePlanEntry(entry.nationalId,entry.name,ObtainMethod.TRANSFER_OR_TRADE,"Verifique troca, transferência, evento ou método especial.")
   }
  }
 }
}
