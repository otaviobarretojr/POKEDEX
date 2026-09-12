package com.otaviobarreto.pokedex.data

data class LivingDexPlan(
    val capturedSpecies:Int,
    val totalSpecies:Int,
    val missingSpecies:List<Int>,
    val shinySpecies:Int,
    val formRegistrations:Int,
    val speciesWithForms:Int
){
    val speciesRatio:Float
        get() = if(totalSpecies<=0) 0f else (capturedSpecies.toFloat()/totalSpecies).coerceIn(0f,1f)
}

object LivingDexPlanner {
    fun current(limit:Int=24):LivingDexPlan {
        val captured=CollectionStore.capturedIds
        val total=PokeApiService.MAX_NATIONAL_DEX_ID
        val missing=buildList {
            for(id in 1..total){
                if(id !in captured){
                    add(id)
                    if(size>=limit) break
                }
            }
        }
        val variants=VariantCollectionStore.ownedVariants
        return LivingDexPlan(
            capturedSpecies=captured.size,
            totalSpecies=total,
            missingSpecies=missing,
            shinySpecies=variants.asSequence().filter{it.shiny}.map{it.speciesId}.distinct().count(),
            formRegistrations=variants.asSequence().filterNot{it.shiny}.map{Triple(it.source,it.speciesId,it.formPokemonId)}.distinct().count(),
            speciesWithForms=variants.asSequence().map{it.speciesId}.distinct().count()
        )
    }

    fun nextMissing(fromId:Int=1):Int? {
        val captured=CollectionStore.capturedIds
        val total=PokeApiService.MAX_NATIONAL_DEX_ID
        return (fromId.coerceAtLeast(1)..total).firstOrNull{it !in captured}
            ?: (1 until fromId.coerceAtMost(total+1)).firstOrNull{it !in captured}
    }
}
