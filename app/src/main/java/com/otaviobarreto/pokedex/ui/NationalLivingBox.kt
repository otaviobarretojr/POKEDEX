package com.otaviobarreto.pokedex.ui

import com.otaviobarreto.pokedex.data.GameDexService
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.PokemonRepository

internal fun nationalLivingDex(): List<GameDexService.GameDexEntry> =
    (1..PokeApiService.MAX_NATIONAL_DEX_ID).map { id ->
        val pokemon=PokemonRepository.byId(id)
        GameDexService.GameDexEntry(
            nationalId=id,
            gameNumber=id,
            name=pokemon?.name ?: "Pokémon #"+id.toString().padStart(4,'0')
        )
    }

internal fun livingDexGameMask(
    national:List<GameDexService.GameDexEntry>,
    gameEntries:List<GameDexService.GameDexEntry>
):Set<Int>{
    val available=gameEntries.mapTo(linkedSetOf()){it.nationalId}
    return national.asSequence().map{it.nationalId}.filter{it in available}.toSet()
}


internal fun relevantLivingBoxPages(available:Set<Int>, total:Int=PokeApiService.MAX_NATIONAL_DEX_ID):List<Int> =
    (0 until ((total+29)/30)).filter { page ->
        val first=page*30+1
        val last=minOf(first+29,total)
        (first..last).any { it in available }
    }

internal fun nextMissingNationalId(
    available:Set<Int>,
    captured:Set<Int>,
    after:Int=0
):Int? = available.asSequence().filter { it !in captured && it>after }.minOrNull()
    ?: available.asSequence().filter { it !in captured }.minOrNull()


internal fun adjacentRelevantPage(current:Int, relevant:List<Int>, forward:Boolean):Int? {
    if(relevant.isEmpty()) return null
    return if(forward) relevant.firstOrNull { it>current } else relevant.lastOrNull { it<current }
}


internal const val ALL_GAMES_LABEL="Todos os jogos"
internal fun allNationalIds():Set<Int>=(1..PokeApiService.MAX_NATIONAL_DEX_ID).toSet()
