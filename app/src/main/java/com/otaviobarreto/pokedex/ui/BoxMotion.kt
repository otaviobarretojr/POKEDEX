package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.otaviobarreto.pokedex.data.GameDexService

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LivingDexPager(
    page:Int,
    pageCount:Int,
    dex:List<GameDexService.GameDexEntry>,
    captured:Set<Int>,
    source:String,
    available:Set<Int>?=null,
    relevantPages:List<Int>,
    onPageSelected:(Int)->Unit,
    open:(GameDexService.GameDexEntry)->Unit,
    hold:(GameDexService.GameDexEntry)->Unit
){
    val pagerState=rememberPagerState(initialPage=page.coerceIn(0,(pageCount-1).coerceAtLeast(0)),pageCount={pageCount})
    LaunchedEffect(page){
        if(page in 0 until pageCount && pagerState.currentPage!=page) pagerState.scrollToPage(page)
    }
    LaunchedEffect(pagerState.settledPage,relevantPages){
        val settled=pagerState.settledPage
        if(settled in relevantPages && settled!=page) onPageSelected(settled)
    }
    HorizontalPager(
        state=pagerState,
        modifier=Modifier.fillMaxSize(),
        beyondViewportPageCount=1,
        key={it}
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
    accent:Color,
    nextMissing:()->Unit,
    showBoxes:()->Unit
){
    androidx.compose.foundation.layout.Row(
        androidx.compose.ui.Modifier.fillMaxSize(),
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
