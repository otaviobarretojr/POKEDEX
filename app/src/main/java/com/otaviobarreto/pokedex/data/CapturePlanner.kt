package com.otaviobarreto.pokedex.data

enum class ObtainMethod { CAPTURE, EVOLUTION, TRANSFER, TRADE_OR_SPECIAL, UNKNOWN }

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
        ObtainMethod.TRANSFER -> "Transferir pelo Pokémon HOME"
        ObtainMethod.TRADE_OR_SPECIAL -> "Troca / método especial"
        ObtainMethod.UNKNOWN -> "Método não confirmado"
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
            val targetRoutes=EvolutionResolutionEngine.routesForTarget(routes,entry.nationalId)
            val evolution=targetRoutes.firstOrNull(EvolutionResolutionEngine::executable)
            val transferOnly=targetRoutes.firstOrNull{it.availability==EvolutionAvailability.TRANSFER_ONLY}
            when{
                filtered.isNotEmpty()->{
                    val locations=filtered.map{LocationIntelligence.displayName(it.location)}.filter{it.isNotBlank()}.distinct().take(4)
                    CapturePlanEntry(entry.nationalId,entry.name,ObtainMethod.CAPTURE,locations.firstOrNull()?.let{"Capturar em $it"} ?: "Captura disponível neste contexto.",locations)
                }
                evolution!=null->CapturePlanEntry(entry.nationalId,entry.name,ObtainMethod.EVOLUTION,evolution.summary)
                transferOnly!=null->CapturePlanEntry(entry.nationalId,entry.name,ObtainMethod.TRANSFER,"Evolução indisponível neste jogo. Obtenha em um jogo compatível e transfira pelo Pokémon HOME.")
                encounters.isNotEmpty() && LocationIntelligence.coverage(context)==LocationIntelligence.Coverage.PARTIAL->
                    CapturePlanEntry(entry.nationalId,entry.name,ObtainMethod.UNKNOWN,"A fonte possui encontros, mas nenhum foi confirmado para esta região. Abra os detalhes antes de planejar a obtenção.")
                else->CapturePlanEntry(entry.nationalId,entry.name,ObtainMethod.TRADE_OR_SPECIAL,"Sem captura ou evolução confirmada neste contexto. Verifique troca, requisito especial ou disponibilidade específica da versão.")
            }
        }
        memory[source]=PlanCache(dexIds,captured,entries)
        return entries
    }
}
