package com.otaviobarreto.pokedex.data

data class GenerationDexProgress(
    val generation:Int,
    val captured:Int,
    val total:Int,
    val shiny:Int
)

data class LivingDexPlan(
    val capturedSpecies:Int,
    val totalSpecies:Int,
    val missingSpecies:List<Int>,
    val shinySpecies:Int,
    val formRegistrations:Int,
    val speciesWithForms:Int,
    val byGeneration:List<GenerationDexProgress>
){
    val speciesRatio:Float
        get() = if(totalSpecies<=0) 0f else (capturedSpecies.toFloat()/totalSpecies).coerceIn(0f,1f)
}

object LivingDexPlanner {
    fun current(limit:Int=24):LivingDexPlan {
        val total=PokeApiService.MAX_NATIONAL_DEX_ID
        val captured=CollectionStore.capturedIds.filterTo(linkedSetOf()){it in 1..total}
        val missing=buildList {
            for(id in 1..total){
                if(id !in captured){
                    add(id)
                    if(size>=limit) break
                }
            }
        }
        val variants=VariantCollectionStore.ownedVariants
        val shinyIds=variants.asSequence().filter{it.shiny && it.speciesId in 1..total}.map{it.speciesId}.distinct().toSet()
        val byGeneration=(1..9).map{gen->
            val ids=(1..total).filter{generationForNationalDexId(it)==gen}
            GenerationDexProgress(
                generation=gen,
                captured=ids.count{it in captured},
                total=ids.size,
                shiny=ids.count{it in shinyIds}
            )
        }
        return LivingDexPlan(
            capturedSpecies=captured.size,
            totalSpecies=total,
            missingSpecies=missing,
            shinySpecies=shinyIds.size,
            formRegistrations=variants.asSequence().filter{!it.shiny && !it.isDefault}.map{listOf(it.source,it.speciesId.toString(),it.formPokemonId.toString(),it.formKey.lowercase())}.distinct().count(),
            speciesWithForms=variants.asSequence().filter{!it.shiny && !it.isDefault}.map{it.speciesId}.distinct().count(),
            byGeneration=byGeneration
        )
    }

    fun nextMissing(fromId:Int=1):Int? {
        val captured=CollectionStore.capturedIds
        val total=PokeApiService.MAX_NATIONAL_DEX_ID
        return (fromId.coerceAtLeast(1)..total).firstOrNull{it !in captured}
            ?: (1 until fromId.coerceAtMost(total+1)).firstOrNull{it !in captured}
    }
}
