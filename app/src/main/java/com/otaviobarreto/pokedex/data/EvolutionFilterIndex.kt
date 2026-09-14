package com.otaviobarreto.pokedex.data

object EvolutionFilterIndex {
    private val memory=mutableMapOf<String,Map<String,Set<Int>>>()
    private val ruleMemory=mutableMapOf<String,List<ContextualEvolutionRule>>()

    fun cached(source:String):Map<String,Set<Int>>? = memory[source]
    fun cachedRules(source:String):List<ContextualEvolutionRule>? = ruleMemory[source]

    fun clear(source:String?=null){
        if(source==null){
            memory.clear()
            ruleMemory.clear()
        }else{
            memory.remove(source)
            ruleMemory.remove(source)
        }
    }

    fun buildRules(
        source:String,
        dex:List<GameDexService.GameDexEntry>
    ):List<ContextualEvolutionRule>{
        ruleMemory[source]?.let{return it}
        val context=GameContext.fromSource(source)
        val dexIds=dex.mapTo(hashSetOf()){it.nationalId}
        val urls=dex.mapNotNull{entry->
            runCatching{PokedexDataStore.species(entry.nationalId).evolutionChainUrl}.getOrNull()
        }.distinct()
        val rules=urls.flatMap{url->
            runCatching{EvolutionRuleCatalog.load(url,context)}.getOrElse{emptyList()}
        }.filter{rule->
            rule.sourcePokemonId in dexIds && rule.targetPokemonId in dexIds
        }.distinctBy{Triple(it.sourcePokemonId,it.targetPokemonId,it.summary)}
        return rules.also{ruleMemory[source]=it}
    }

    fun build(
        source:String,
        dex:List<GameDexService.GameDexEntry>
    ):Map<String,Set<Int>>{
        memory[source]?.let{return it}
        val result=mutableMapOf<String,MutableSet<Int>>()
        buildRules(source,dex).forEach{rule->
            val target=rule.targetPokemonId
            result.getOrPut("ALL"){mutableSetOf()}.add(target)
            result.getOrPut(EvolutionRuleCatalog.filterBucket(rule)){mutableSetOf()}.add(target)
            rule.methods.forEach{method->
                result.getOrPut(method.name){mutableSetOf()}.add(target)
            }
        }
        return result.mapValues{it.value.toSet()}.also{memory[source]=it}
    }
}
