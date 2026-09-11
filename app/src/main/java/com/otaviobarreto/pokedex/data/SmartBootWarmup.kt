package com.otaviobarreto.pokedex.data

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

object SmartBootWarmup {
    suspend fun run(context:Context,onProgress:(Float,String)->Unit){
        onProgress(.08f,"Lendo cache local")
        val gameLabel=CompanionPreferences.activeGame
        val game=AppGameCatalog.games.firstOrNull{it.label==gameLabel} ?: AppGameCatalog.adventureGames.first()
        val preferred=CompanionPreferences.activeRegionForGame(game.label)
        val region=game.regions.firstOrNull{it.source==preferred} ?: game.regions.firstOrNull()

        onProgress(.20f,"Preparando sua Jornada")
        withContext(Dispatchers.IO){
            runCatching{JourneyCatalog.steps(game.label)}
            runCatching{PokedexDataStore.nationalDex()}
        }

        onProgress(.38f,"Carregando Pokédex do jogo")
        val dexBySource=withContext(Dispatchers.IO){
            game.regions.associate{r->
                r.source to (GameContext.fromSource(r.source)?.let{ctx->
                    runCatching{GameDexService.loadGameDex(ctx)}.getOrDefault(emptyList())
                } ?: emptyList())
            }
        }

        val activeDex=region?.let{dexBySource[it.source]}.orEmpty()
        val page=region?.let{CompanionPreferences.boxPage(it.source)} ?: 0
        val currentBox=activeDex.drop(page.coerceAtLeast(0)*30).take(30)

        onProgress(.58f,"Aquecendo a Box atual")
        coroutineScope{
            currentBox.take(10).map{entry->
                async(Dispatchers.IO){runCatching{PokedexDataStore.prefetchCoreDetails(entry.nationalId)}}
            }.awaitAll()
        }

        onProgress(.78f,"Preparando artes prioritárias")
        coroutineScope{
            currentBox.take(12).map{entry->
                async(Dispatchers.IO){
                    runCatching{
                        context.imageLoader.execute(
                            ImageRequest.Builder(context)
                                .data(entry.spriteUrl)
                                .build()
                        )
                    }
                }
            }.awaitAll()
        }

        onProgress(.92f,"Restaurando sua última posição")
        val recent=RecentActivityStore.recentPokemon.take(6)
        coroutineScope{
            recent.map{id->async(Dispatchers.IO){runCatching{PokedexDataStore.prefetchCoreDetails(id)}}}.awaitAll()
        }
        onProgress(1f,"Abrindo sua Pokédex")
    }
}
