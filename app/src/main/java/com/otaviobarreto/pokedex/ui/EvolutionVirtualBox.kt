package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.GameDexService

@Composable
internal fun EvolutionVirtualBox(
    entries:List<GameDexService.GameDexEntry>,
    captured:Set<Int>,
    source:String,
    loading:Boolean,
    accent:androidx.compose.ui.graphics.Color,
    onPokemonClick:(Int,String?)->Unit,
    onHold:(GameDexService.GameDexEntry)->Unit
){
    Box(Modifier.fillMaxSize()){
        when{
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter),color=accent)
            entries.isEmpty() -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                Text(
                    "Nenhum Pokémon desta região usa este método de evolução.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding=PaddingValues(vertical=4.dp),
                verticalArrangement=Arrangement.spacedBy(3.dp)
            ){
                items(entries.chunked(6)){rowEntries->
                    Row(
                        Modifier.fillMaxWidth().height(92.dp),
                        horizontalArrangement=Arrangement.spacedBy(2.dp)
                    ){
                        repeat(6){col->
                            val pk=rowEntries.getOrNull(col)
                            if(pk==null){
                                Spacer(Modifier.weight(1f).fillMaxHeight())
                            }else{
                                QBSlot(
                                    pk=pk,
                                    captured=pk.nationalId in captured,
                                    source=source,
                                    specialEvolution=true,
                                    specialFilter=false,
                                    open={onPokemonClick(pk.nationalId,source)},
                                    hold={onHold(pk)},
                                    modifier=Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EvolutionModeCounter(
    count:Int,
    loading:Boolean,
    accent:androidx.compose.ui.graphics.Color
){
    Column(horizontalAlignment=Alignment.CenterHorizontally){
        Text(
            if(loading)"…" else count.toString(),
            style=MaterialTheme.typography.titleMedium,
            fontWeight=FontWeight.Black,
            color=accent
        )
        Text(
            "Pokémon",
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
