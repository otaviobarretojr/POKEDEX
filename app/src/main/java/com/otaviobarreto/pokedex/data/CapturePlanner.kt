package com.otaviobarreto.pokedex.data

enum class ObtainMethod { CAPTURE, EVOLUTION, TRANSFER_OR_TRADE, UNKNOWN }

data class CapturePlanEntry(
    val pokemonId:Int,
    val name:String,
    val method:ObtainMethod,
    val summary:String,
    val locations:List<String> = emptyList()
){
    val routeGroup:String get() = when(method){
        ObtainMethod.CAPTURE -> locations.firstOrNull()?.let{"Capturar · $it"} ?: "Capturar"
        ObtainMethod.EVOLUTION -> "Evoluir"
        ObtainMethod.TRANSFER_OR_TRADE -> "Troca / transferência"
        ObtainMethod.UNKNOWN -> "Método especial"
    }
}

object CapturePlanner {
    private data class PlanCache(
        val dexIds:List<Int>,
        val captured:Set<Int>,
        val entries:List<CapturePlanEntry>
    )
    private val memory=mutableMapOf<String,PlanCache>()

    fun plan(source:String,dex:List<GameDexService.GameDexEntry>):List<CapturePlanEntry>{
        val dexIds=dex.map{it.nationalId}
        val captured=CollectionStore.capturedIn(source)
        memory[source]?.takeIf{it.dexIds==dexIds && it.captured==captured}?.let{return it.entries}
        val context=GameContext.fromSource(source)
        val missing=dex.filterNot{it.nationalId in captured}
        val routes=runCatching{EvolutionFilterIndex.buildRoutes(source,dex)}.getOrDefault(emptyList())
        val entries=missing.map{entry->
            val encounters=runCatching{PokeApiService.loadEncounters(entry.nationalId)}.getOrDefault(emptyList())
            val filtered=LocationIntelligence.filter(encounters,context)
            val evolution=routes.firstOrNull{it.targetPokemonId==entry.nationalId && EvolutionResolutionEngine.executable(it)}
            when{
                filtered.isNotEmpty()->{
                    val locations=filtered.map{LocationIntelligence.displayName(it.location)}.filter{it.isNotBlank()}.distinct().take(4)
                    CapturePlanEntry(entry.nationalId,entry.name,ObtainMethod.CAPTURE,locations.firstOrNull()?.let{"Capturar em $it"} ?: "Captura disponível neste contexto.",locations)
                }
                evolution!=null->CapturePlanEntry(entry.nationalId,entry.name,ObtainMethod.EVOLUTION,evolution.summary)
                else->CapturePlanEntry(entry.nationalId,entry.name,ObtainMethod.TRANSFER_OR_TRADE,"Sem encontro direto confirmado neste contexto. Verifique troca, transferência ou requisito especial.")
            }
        }
        memory[source]=PlanCache(dexIds,captured,entries)
        return entries
    }
}
