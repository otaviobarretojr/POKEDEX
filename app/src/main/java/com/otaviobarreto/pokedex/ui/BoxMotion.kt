package com.otaviobarreto.pokedex.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
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
    AnimatedContent(
        targetState=page,
        transitionSpec={
            val forward=targetState>initialState
            val enter=slideInHorizontally(tween(PokedexDesignTokens.Motion.Standard)){if(forward) it/5 else -it/5}+fadeIn(tween(PokedexDesignTokens.Motion.Fast))
            val exit=slideOutHorizontally(tween(PokedexDesignTokens.Motion.Standard)){if(forward) -it/5 else it/5}+fadeOut(tween(PokedexDesignTokens.Motion.Fast))
            enter togetherWith exit
        },
        label="boxPageTransition"
    ){boxPage->
        QBGrid(
            entries=dex.drop(boxPage*30).take(30),
            captured=captured,
            source=source,
            specialFilter=false,
            specialIds=emptySet(),
            available=available,
            open=open,
            hold=hold
        )
    }
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
        androidx.compose.material3.FilledTonalButton(nextMissing,androidx.compose.ui.Modifier.weight(1f)){
            androidx.compose.material3.Text("Próximo faltante · $missing")
        }
        androidx.compose.material3.FilledTonalButton(showBoxes,androidx.compose.ui.Modifier.weight(1f)){
            androidx.compose.material3.Text("$relevantPages Boxes")
        }
    }
}
