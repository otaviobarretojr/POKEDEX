package com.otaviobarreto.pokedex.ui

import androidx.compose.runtime.Composable
import com.otaviobarreto.pokedex.data.GameDexService

/**
 * Box navigation is intentionally immediate. The old AnimatedContent kept two
 * 30-slot grids alive during each transition and made fast devices feel slower.
 */
@Composable
internal fun AnimatedBoxGrid(
    page:Int,
    dex:List<GameDexService.GameDexEntry>,
    captured:Set<Int>,
    source:String,
    open:(GameDexService.GameDexEntry)->Unit,
    hold:(GameDexService.GameDexEntry)->Unit
){
    QBGrid(
        entries=dex.drop(page*30).take(30),
        captured=captured,
        source=source,
        specialFilter=false,
        specialIds=emptySet(),
        open=open,
        hold=hold
    )
}
