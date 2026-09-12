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

    fun recommendation(pokemonId:Int):String {
        val options=cachedOptions(pokemonId)
        if(options.isEmpty()){
            return if(warmed) "Não consta nas Pokédex regionais carregadas; verifique evolução, transferência ou disponibilidade especial."
            else "Preparando disponibilidade nos seus jogos…"
        }
        val first=options.first()
        val extra=(options.size-1).coerceAtLeast(0)
        return buildString {
            append("Disponível em ")
            append(first.game)
            append(" · ")
            append(first.region)
            append(" #")
            append(first.regionalNumber)
            if(extra>0) append(" · +").append(extra).append(" opção(ões)")
        }
    }
}
