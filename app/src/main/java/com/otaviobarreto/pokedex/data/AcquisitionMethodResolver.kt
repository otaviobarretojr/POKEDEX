package com.otaviobarreto.pokedex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

enum class DetailedAcquisitionMethod {
    CAPTURE,
    EVOLUTION,
    TRADE,
    SPECIAL_OR_TRANSFER
}

data class AcquisitionAdvice(
    val pokemonId:Int,
    val method:DetailedAcquisitionMethod,
    val label:String,
    val requirement:String?=null,
    val sourcePokemonId:Int?=null,
    val sourceOwned:Boolean=false
)

object AcquisitionMethodResolver {
    private val cache=ConcurrentHashMap<Int,AcquisitionAdvice>()

    fun cached(id:Int):AcquisitionAdvice? = cache[id]

    suspend fun resolve(id:Int):AcquisitionAdvice = withContext(Dispatchers.IO) {
        cache[id]?.let{return@withContext it}

        val species=runCatching{PokedexDataStore.species(id)}.getOrNull()
        val chain=species?.evolutionChainUrl?.let { url ->
            runCatching{PokedexDataStore.evolutions(url)}.getOrNull()
        }.orEmpty()

        val index=chain.indexOfFirst{it.pokemonId==id}
        val stage=chain.getOrNull(index)
        val previous=if(index>0) chain.getOrNull(index-1) else null
        val previousOwned=previous?.pokemonId?.let{it in CollectionStore.capturedIds}==true
        val requirement=stage?.requirement

        val advice=when{
            previousOwned && !requirement.isNullOrBlank() -> {
                val isTrade=requirement.contains("Troca",true) || requirement.contains("Trocar",true)
                AcquisitionAdvice(
                    pokemonId=id,
                    method=if(isTrade) DetailedAcquisitionMethod.TRADE else DetailedAcquisitionMethod.EVOLUTION,
                    label=if(isTrade) "Troca/evolução a partir de ${previous?.name}" else "Evolua ${previous?.name}",
                    requirement=requirement,
                    sourcePokemonId=previous?.pokemonId,
                    sourceOwned=true
                )
            }
            CollectionAdvisor.cachedOptions(id).isNotEmpty() -> {
                AcquisitionAdvice(
                    pokemonId=id,
                    method=DetailedAcquisitionMethod.CAPTURE,
                    label=CollectionAdvisor.recommendation(id)
                )
            }
            previous!=null && !requirement.isNullOrBlank() -> {
                val isTrade=requirement.contains("Troca",true) || requirement.contains("Trocar",true)
                AcquisitionAdvice(
                    pokemonId=id,
                    method=if(isTrade) DetailedAcquisitionMethod.TRADE else DetailedAcquisitionMethod.EVOLUTION,
                    label=(if(isTrade)"Obtenha ${previous.name} e faça a troca/evolução" else "Obtenha ${previous.name} e evolua"),
                    requirement=requirement,
                    sourcePokemonId=previous.pokemonId,
                    sourceOwned=false
                )
            }
            else -> AcquisitionAdvice(
                pokemonId=id,
                method=DetailedAcquisitionMethod.SPECIAL_OR_TRANSFER,
                label="Verificar HOME, evento, troca, breeding ou método especial"
            )
        }
        cache[id]=advice
        advice
    }

    suspend fun resolveBatch(ids:List<Int>,limit:Int=48):List<AcquisitionAdvice> =
        ids.asSequence().distinct().take(limit).map{resolve(it)}.toList()

    fun invalidate(id:Int?=null){
        if(id==null) cache.clear() else cache.remove(id)
    }
}
