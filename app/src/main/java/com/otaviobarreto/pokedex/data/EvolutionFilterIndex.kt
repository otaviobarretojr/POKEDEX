package com.otaviobarreto.pokedex.data

object EvolutionFilterIndex {
    private data class FilterCacheEntry(
        val dexIds:Set<Int>,
        val buckets:Map<String,Set<Int>>
    )
    private val memory=mutableMapOf<String,FilterCacheEntry>()
    private data class RouteCacheEntry(
        val dexIds:Set<Int>,
        val routes:List<EvolutionRoute>
    )
    private val routeMemory=mutableMapOf<String,RouteCacheEntry>()

    fun cached(source:String):Map<String,Set<Int>>? = memory[source]?.buckets
    fun cachedRoutes(source:String):List<EvolutionRoute>? = routeMemory[source]?.routes

    fun clear(source:String?=null){
        if(source==null){
            memory.clear()
            routeMemory.clear()
        }else{
            memory.remove(source)
            routeMemory.remove(source)
        }
    }

    fun buildRoutes(
        source:String,
        dex:List<GameDexService.GameDexEntry>
    ):List<EvolutionRoute>{
        val context=GameContext.fromSource(source)
        val dexIds=dex.mapTo(linkedSetOf()){it.nationalId}
        routeMemory[source]?.takeIf{it.dexIds==dexIds}?.let{return it.routes}

        val urls=dex.mapNotNull{entry->
            runCatching{PokedexDataStore.species(entry.nationalId).evolutionChainUrl}.getOrNull()
        }.distinct()
        val routes=urls.flatMap{url->
            runCatching{EvolutionResolutionEngine.load(url,context)}.getOrElse{emptyList()}
        }.filter{route->
            route.sourcePokemonId in dexIds && route.targetPokemonId in dexIds
        }.distinctBy{
            listOf(
                it.sourcePokemonId,
                it.targetPokemonId,
                it.summary,
                it.availability.name,
                it.sourceFormKey.orEmpty(),
                it.targetFormKey.orEmpty()
            ).joinToString(":")
        }
        routeMemory[source]=RouteCacheEntry(dexIds,routes)
        return routes
    }

    fun buildRules(
        source:String,
        dex:List<GameDexService.GameDexEntry>
    ):List<ContextualEvolutionRule> = buildRoutes(source,dex).map{route->
        ContextualEvolutionRule(
            sourcePokemonId=route.sourcePokemonId,
            targetPokemonId=route.targetPokemonId,
            methods=route.methods,
            summary=route.summary,
            rawRequirement=route.detail
        )
    }

    fun build(
        source:String,
        dex:List<GameDexService.GameDexEntry>
    ):Map<String,Set<Int>>{
        val dexIds=dex.mapTo(linkedSetOf()){it.nationalId}
        memory[source]?.takeIf{it.dexIds==dexIds}?.let{return it.buckets}

        val result=mutableMapOf<String,MutableSet<Int>>()
        buildRoutes(source,dex)
            .filter(EvolutionResolutionEngine::executable)
            .forEach{route->
                val target=route.targetPokemonId
                result.getOrPut("ALL"){mutableSetOf()}.add(target)
                val legacy=ContextualEvolutionRule(
                    route.sourcePokemonId,route.targetPokemonId,route.methods,route.summary,route.detail
                )
                result.getOrPut(EvolutionRuleCatalog.filterBucket(legacy)){mutableSetOf()}.add(target)
                route.methods.forEach{method->
                    result.getOrPut(method.name){mutableSetOf()}.add(target)
                }
            }
        val buckets=result.mapValues{it.value.toSet()}
        memory[source]=FilterCacheEntry(dexIds,buckets)
        return buckets
    }
}
