package com.otaviobarreto.pokedex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class AcquisitionOption(
    val game:String,
    val region:String,
    val source:String,
    val regionalNumber:Int
)

data class CaptureTarget(
    val pokemonId:Int,
    val preferred:AcquisitionOption?,
    val alternatives:List<AcquisitionOption>
){
    val totalOptions:Int get() = alternatives.size
}

enum class AcquisitionKind {
    REGIONAL_CAPTURE,
    SPECIAL_OR_TRANSFER
}

data class GameRouteStep(
    val game:String,
    val source:String?,
    val region:String?,
    val targetIds:List<Int>
){
    val count:Int get() = targetIds.size
}

data class NextCollectionAction(
    val title:String,
    val subtitle:String,
    val game:String?,
    val source:String?,
    val targetIds:List<Int>
)

object CollectionAdvisor {
    private val availability=ConcurrentHashMap<Int,MutableList<AcquisitionOption>>()
    @Volatile private var warmed=false

    fun cachedOptions(pokemonId:Int):List<AcquisitionOption> =
        availability[pokemonId].orEmpty()
            .distinctBy{it.source}
            .sortedWith(compareBy<AcquisitionOption>{it.game}.thenBy{it.region})

    fun isWarm():Boolean = warmed

    suspend fun warmAllGames() = withContext(Dispatchers.IO) {
        val local=mutableMapOf<Int,MutableList<AcquisitionOption>>()
        AppGameCatalog.adventureGames.forEach{game->
            game.regions.forEach{region->
                val context=GameContext.fromSource(region.source) ?: return@forEach
                val dex=runCatching {
                    GameDexService.cached(context) ?: GameDexService.loadGameDex(context)
                }.getOrNull() ?: return@forEach
                dex.forEach{entry->
                    local.getOrPut(entry.nationalId){mutableListOf()}.add(
                        AcquisitionOption(
                            game=game.label,
                            region=region.label,
                            source=region.source,
                            regionalNumber=entry.gameNumber
                        )
                    )
                }
            }
        }
        availability.clear()
        local.forEach{(id,options)->availability[id]=options}
        warmed=true
    }

    fun preferredOption(pokemonId:Int):AcquisitionOption? =
        cachedOptions(pokemonId).firstOrNull()

    fun capturePlan(pokemonIds:List<Int>,limit:Int=12):List<CaptureTarget> =
        pokemonIds.asSequence()
            .distinct()
            .filter{it !in CollectionStore.capturedIds}
            .map{ id ->
                val options=cachedOptions(id)
                CaptureTarget(
                    pokemonId=id,
                    preferred=options.firstOrNull(),
                    alternatives=options
                )
            }
            .sortedWith(
                compareByDescending<CaptureTarget>{it.preferred!=null}
                    .thenBy{it.totalOptions.takeIf{count->count>0} ?: Int.MAX_VALUE}
                    .thenBy{it.pokemonId}
            )
            .take(limit)
            .toList()

    fun acquisitionKind(pokemonId:Int):AcquisitionKind =
        if(cachedOptions(pokemonId).isNotEmpty()) AcquisitionKind.REGIONAL_CAPTURE
        else AcquisitionKind.SPECIAL_OR_TRANSFER

    fun acquisitionLabel(pokemonId:Int):String =
        when(acquisitionKind(pokemonId)){
            AcquisitionKind.REGIONAL_CAPTURE -> "Captura/registro regional disponível na sua base"
            AcquisitionKind.SPECIAL_OR_TRANSFER -> "Sem entrada regional na sua base: verificar evolução, troca, HOME, evento ou método especial"
        }

    fun gameRoutePlan(pokemonIds:List<Int>):List<GameRouteStep>{
        val remaining=pokemonIds.asSequence()
            .distinct()
            .filter{it !in CollectionStore.capturedIds}
            .toMutableSet()
        val steps=mutableListOf<GameRouteStep>()
        while(remaining.isNotEmpty()){
            val gameCoverage=mutableMapOf<String,MutableList<Pair<Int,AcquisitionOption>>>()
            remaining.forEach{id->
                cachedOptions(id).forEach{option->
                    gameCoverage.getOrPut(option.game){mutableListOf()}.add(id to option)
                }
            }
            val best=gameCoverage.maxByOrNull{(_,pairs)->pairs.map{it.first}.distinct().size} ?: break
            val unique=best.value.distinctBy{it.first}
            val ids=unique.map{it.first}.filter{it in remaining}
            if(ids.isEmpty()) break
            val preferred=unique.first().second
            steps+=GameRouteStep(
                game=best.key,
                source=preferred.source,
                region=preferred.region,
                targetIds=ids.sorted()
            )
            remaining.removeAll(ids.toSet())
        }
        if(remaining.isNotEmpty()){
            steps+=GameRouteStep(
                game="Transferência / especial",
                source=null,
                region=null,
                targetIds=remaining.sorted()
            )
        }
        return steps
    }

    fun nextAction(pokemonIds:List<Int>):NextCollectionAction{
        val route=gameRoutePlan(pokemonIds)
        val first=route.firstOrNull()
        if(first==null){
            return NextCollectionAction(
                title="Coleção em dia",
                subtitle="Nenhuma espécie pendente foi encontrada.",
                game=null,
                source=null,
                targetIds=emptyList()
            )
        }
        return if(first.source!=null){
            NextCollectionAction(
                title="Jogue ${first.game}",
                subtitle="Você pode avançar ${first.count} espécie(s) pendente(s) nesta etapa.",
                game=first.game,
                source=first.source,
                targetIds=first.targetIds
            )
        }else{
            NextCollectionAction(
                title="Resolver espécies especiais",
                subtitle="${first.count} espécie(s) exigem evolução, troca, HOME, evento ou outro método fora das Pokédex regionais carregadas.",
                game=null,
                source=null,
                targetIds=first.targetIds
            )
        }
    }

    fun recommendation(pokemonId:Int):String {
        val options=cachedOptions(pokemonId)
        if(options.isEmpty()){
            return if(warmed) "Não consta nas Pokédex regionais carregadas; verifique evolução, transferência ou disponibilidade especial."
            else "Preparando disponibilidade nos seus jogos…"
        }
        val first=options.first()
        val extra=(options.size-1).coerceAtLeast(0)
        return buildString {
            append("Melhor opção na sua base: ")
            append(first.game)
            append(" · ")
            append(first.region)
            append(" #")
            append(first.regionalNumber)
            if(extra>0) append(" · +").append(extra).append(" alternativa(s)")
        }
    }
}
