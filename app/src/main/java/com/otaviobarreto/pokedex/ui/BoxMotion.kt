package com.otaviobarreto.pokedex.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.GameDexService

@Composable
internal fun AnimatedBoxGrid(
    page:Int,
    dex:List<GameDexService.GameDexEntry>,
    captured:Set<Int>,
    source:String,
    available:Set<Int>?=null,
    open:(GameDexService.GameDexEntry)->Unit,
    hold:(GameDexService.GameDexEntry)->Unit
){
    // Living Dex pages are intentionally swapped without AnimatedContent.
    // The grid is already local/cached; animating old + new pages together
    // doubled composition/image work and made a Box change look like reload.
    QBGrid(
        entries=dex.drop(page*30).take(30),
        captured=captured,
        source=source,
        specialFilter=false,
        specialIds=emptySet(),
        available=available,
        open=open,
        hold=hold
    )
}


@Composable
internal fun LivingDexFilterActions(
    relevantPages:Int,
    missing:Int,
    accent:androidx.compose.ui.graphics.Color,
    nextMissing:()->Unit,
    showBoxes:()->Unit
){
    androidx.compose.foundation.layout.Row(
        androidx.compose.ui.Modifier.fillMaxWidth(),
        horizontalArrangement=androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
    ){
        androidx.compose.material3.FilledTonalButton(nextMissing){
            androidx.compose.material3.Text("Próximo faltante · $missing")
        }
        androidx.compose.material3.FilledTonalButton(showBoxes){
            androidx.compose.material3.Text("$relevantPages Boxes")
        }
    }
}
