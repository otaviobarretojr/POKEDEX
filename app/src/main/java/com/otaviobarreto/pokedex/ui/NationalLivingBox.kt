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
