package com.otaviobarreto.pokedex.data

object EvolutionFilterIndex {
    private val memory=mutableMapOf<String,Map<String,Set<Int>>>()

    fun cached(source:String):Map<String,Set<Int>>? = memory[source]

    fun clear(source:String?=null){
        if(source==null) memory.clear() else memory.remove(source)
    }

    fun build(
        source:String,
        dex:List<GameDexService.GameDexEntry>
    ):Map<String,Set<Int>>{
        memory[source]?.let{return it}
        val context=GameContext.fromSource(source)
        val dexIds=dex.mapTo(hashSetOf()){it.nationalId}
        val result=mutableMapOf<String,MutableSet<Int>>()
        val byChain=dex.groupBy{entry->
            runCatching{PokedexDataStore.species(entry.nationalId).evolutionChainUrl}.getOrNull()
        }
        byChain.keys.filterNotNull().forEach{url->
            runCatching{PokeApiService.loadEvolutionSourceMethods(url,context)}
                .getOrElse{emptyList()}
                .forEach{info->
                    if(info.sourcePokemonId in dexIds){
                        result.getOrPut("ALL"){mutableSetOf()}.add(info.sourcePokemonId)
                        result.getOrPut(info.method.name){mutableSetOf()}.add(info.sourcePokemonId)
                    }
                }
        }
        return result.mapValues{it.value.toSet()}.also{memory[source]=it}
    }
}
