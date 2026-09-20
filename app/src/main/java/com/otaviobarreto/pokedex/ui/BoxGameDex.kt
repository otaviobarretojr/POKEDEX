package com.otaviobarreto.pokedex.ui

import com.otaviobarreto.pokedex.data.AppGame
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService

internal data class BoxGameDex(
    val all: List<GameDexService.GameDexEntry>,
    val filtered: List<GameDexService.GameDexEntry>
)

internal suspend fun loadBoxGameDex(game: AppGame, selectedSource: String): BoxGameDex {
    val regional = game.regions.map { region ->
        val ctx = GameContext.fromSource(region.source)
        region.source to (if (ctx == null) emptyList() else runCatching { GameDexService.loadGameDex(ctx) }.getOrElse { emptyList() })
    }
    val seen = linkedSetOf<Int>()
    val uniqueByRegion = linkedMapOf<String, List<GameDexService.GameDexEntry>>()
    regional.forEach { (source, entries) ->
        uniqueByRegion[source] = entries.filter { seen.add(it.nationalId) }
    }
    val all = uniqueByRegion.values.flatten()
    return BoxGameDex(all = all, filtered = uniqueByRegion[selectedSource].orEmpty())
}
