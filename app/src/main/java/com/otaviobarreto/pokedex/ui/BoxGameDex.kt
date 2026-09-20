package com.otaviobarreto.pokedex.ui

import com.otaviobarreto.pokedex.data.AppGame
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService

internal data class BoxGameDex(
    val all: List<GameDexService.GameDexEntry>,
    val filtered: List<GameDexService.GameDexEntry>
)

internal suspend fun loadBoxGameDex(game: AppGame, selectedSource: String): BoxGameDex {
    val regional = game.regions.associate { region ->
        val ctx = GameContext.fromSource(region.source)
        region.source to (if (ctx == null) emptyList() else runCatching { GameDexService.loadGameDex(ctx) }.getOrElse { emptyList() })
    }
    // Each regional/DLC Pokédex is an independent ordered view.
    // Capture state remains shared at the game level in CollectionStore.
    val selected = regional[selectedSource].orEmpty()
    return BoxGameDex(all = selected, filtered = selected)
}
