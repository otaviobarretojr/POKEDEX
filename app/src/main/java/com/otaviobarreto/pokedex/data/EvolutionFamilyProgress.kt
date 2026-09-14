package com.otaviobarreto.pokedex.data

object EvolutionFamilyProgress {

    fun orderEntries(
        dex:List<GameDexService.GameDexEntry>,
        routes:List<EvolutionRoute>
    ):List<GameDexService.GameDexEntry>{
        if(dex.isEmpty()) return emptyList()

        val dexIds=dex.mapTo(linkedSetOf()){it.nationalId}
        val validRoutes=routes.filter{
            it.sourcePokemonId in dexIds && it.targetPokemonId in dexIds
        }

        val parents=validRoutes.groupBy{it.targetPokemonId}
            .mapValues{(_,values)->values.map{it.sourcePokemonId}.toSet()}
        val children=validRoutes.groupBy{it.sourcePokemonId}
            .mapValues{(_,values)->values.map{it.targetPokemonId}.toSet()}

        val regionalOrder=dex.withIndex().associate{it.value.nationalId to it.index}

        fun component(start:Int):Set<Int>{
            val seen=linkedSetOf<Int>()
            val queue=ArrayDeque<Int>()
            queue.add(start)
            while(queue.isNotEmpty()){
                val current=queue.removeFirst()
                if(!seen.add(current)) continue
                parents[current].orEmpty().forEach(queue::add)
                children[current].orEmpty().forEach(queue::add)
            }
            return seen
        }

        val components=mutableListOf<Set<Int>>()
        val assigned=mutableSetOf<Int>()
        dex.forEach{entry->
            if(entry.nationalId !in assigned){
                val group=component(entry.nationalId)
                components+=group
                assigned+=group
            }
        }

        fun depth(id:Int,group:Set<Int>):Int{
            val roots=group.filter{parents[it].orEmpty().none{parent->parent in group}}
            if(id in roots) return 0
            var frontier=roots.toSet()
            val visited=frontier.toMutableSet()
            var d=0
            while(frontier.isNotEmpty()){
                if(id in frontier) return d
                val next=frontier.flatMap{children[it].orEmpty()}
                    .filter{it in group && it !in visited}
                    .toSet()
                visited+=next
                frontier=next
                d++
            }
            return Int.MAX_VALUE/4
        }

        val sortedComponents=components.sortedBy{group->
            group.minOfOrNull{regionalOrder[it] ?: Int.MAX_VALUE} ?: Int.MAX_VALUE
        }

        val ids=sortedComponents.flatMap{group->
            group.sortedWith(
                compareBy<Int>{depth(it,group)}
                    .thenBy{regionalOrder[it] ?: Int.MAX_VALUE}
                    .thenBy{it}
            )
        }

        val byId=dex.associateBy{it.nationalId}
        return ids.mapNotNull(byId::get)
    }

    fun missingEntries(
        dex:List<GameDexService.GameDexEntry>,
        routes:List<EvolutionRoute>,
        owned:Set<Int>
    ):List<GameDexService.GameDexEntry> =
        orderEntries(dex,routes).filter{it.nationalId !in owned}
}
