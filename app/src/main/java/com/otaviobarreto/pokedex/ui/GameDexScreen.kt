package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class GameDexFilter(val label:String){
    ALL("Todos"), MISSING("Faltantes"), CAPTURED("Capturados"), EVOLUTION("Evolução")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GameDexScreen(onBack:()->Unit,onPokemonClick:(Int,String?)->Unit){
    val game=AppStatePreferences.activeGame
    val regions=remember(game){AppGameCatalog.games.firstOrNull{it.label==game}?.regions.orEmpty()}
    var source by remember(game){mutableStateOf(AppStatePreferences.activeRegionForGame(game) ?: regions.firstOrNull()?.source)}
    var dex by remember(source){mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())}
    var plan by remember(source){mutableStateOf<List<CapturePlanEntry>>(emptyList())}
    var filter by rememberSaveable(game){mutableStateOf(GameDexFilter.ALL)}
    var routeMode by rememberSaveable(game){mutableStateOf(false)}
    var loading by remember(source){mutableStateOf(source!=null)}
    val capturedBySource=CollectionStore.contextualCapturedIds
    val captured=remember(source,capturedBySource){source?.let{capturedBySource[it]}.orEmpty()}

    LaunchedEffect(source,captured){
        val s=source ?: return@LaunchedEffect
        loading=true
        val result=withContext(Dispatchers.IO){
            val ctx=GameContext.fromSource(s)
            val loaded=ctx?.let{GameDexService.loadGameDex(it)}.orEmpty()
            loaded to CapturePlanner.plan(s,loaded)
        }
        dex=result.first
        plan=result.second
        loading=false
    }

    val evolutionIds=remember(plan){plan.asSequence().filter{it.method==ObtainMethod.EVOLUTION}.map{it.pokemonId}.toSet()}
    val shown=remember(dex,filter,captured,evolutionIds){
        when(filter){
            GameDexFilter.ALL->dex
            GameDexFilter.MISSING->dex.filterNot{it.nationalId in captured}
            GameDexFilter.CAPTURED->dex.filter{it.nationalId in captured}
            GameDexFilter.EVOLUTION->dex.filter{it.nationalId in evolutionIds}
        }
    }
    val routeGroups=remember(plan){
        plan.groupBy{it.routeGroup}.entries.sortedWith(
            compareBy<Map.Entry<String,List<CapturePlanEntry>>>(
                {group->
                    when(group.value.firstOrNull()?.method){
                        ObtainMethod.CAPTURE -> 0
                        ObtainMethod.EVOLUTION -> 1
                        ObtainMethod.TRANSFER -> 2
                        ObtainMethod.TRADE_OR_SPECIAL -> 3
                        ObtainMethod.UNKNOWN, null -> 4
                    }
                },
                {it.key}
            )
        )
    }

    Scaffold(topBar={
        TopAppBar(title={Text("Pokédex do jogo")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}})
    }){pad->
        Column(Modifier.fillMaxSize().padding(pad)){
            Text(game,Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
            if(regions.size>1) ScrollableTabRow(selectedTabIndex=regions.indexOfFirst{it.source==source}.coerceAtLeast(0),edgePadding=12.dp){
                regions.forEach{r->Tab(selected=r.source==source,onClick={source=r.source;AppStatePreferences.setActiveRegionForGame(game,r.source)},text={Text(r.label)})}
            }
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilterChip(selected=routeMode,onClick={routeMode=!routeMode},label={Text("Melhor rota")},leadingIcon={Icon(Icons.Default.Route,null)})
            }
            if(!routeMode){
                LazyRow(contentPadding=PaddingValues(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    items(GameDexFilter.entries,key={it.name}){item->
                        FilterChip(selected=filter==item,onClick={filter=item},label={Text(item.label)})
                    }
                }
            }
            when{
                loading->DexStatusPane("Carregando Pokédex","Preparando dados da região selecionada.",Modifier.fillMaxSize().padding(16.dp),true)
                routeMode && routeGroups.isEmpty()->DexStatusPane("Rota concluída","Não há Pokémon faltantes nesta região.",Modifier.fillMaxSize().padding(16.dp),false)
                routeMode->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    item(key="route_intro",contentType="route_intro"){
                        Text("Prioridade prática: capture por local, depois evolua e deixe transferências ou métodos especiais para o final.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(start=4.dp,end=4.dp,bottom=4.dp))
                    }
                    routeGroups.forEach{group->
                        item(key="route_${group.key}",contentType="route_header"){
                            Column(Modifier.fillMaxWidth().padding(top=4.dp)){
                                Text(group.key,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                                Text("${group.value.size} Pokémon",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        items(group.value,key={it.pokemonId},contentType={"route_item"}){p->
                            Card(Modifier.fillMaxWidth().clickable{onPokemonClick(p.pokemonId,source)}){
                                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                                    PokemonArtwork(PokemonRepository.byId(p.pokemonId)?.spriteUrl,null,Modifier.size(54.dp),pokemonId=p.pokemonId)
                                    Column(Modifier.weight(1f).padding(start=10.dp)){Text(p.name,fontWeight=FontWeight.Bold);Text(p.summary,style=MaterialTheme.typography.bodySmall)}
                                    if(p.method==ObtainMethod.CAPTURE) Icon(Icons.Default.LocationOn,null)
                                }
                            }
                        }
                    }
                }
                shown.isEmpty()->DexStatusPane("Nenhum resultado","Não há Pokémon para este filtro.",Modifier.fillMaxSize().padding(16.dp),false)
                else->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    items(shown,key={it.nationalId},contentType={"dex_item"}){p->
                        val caught=p.nationalId in captured
                        ListItem(
                            headlineContent={Text(p.name,fontWeight=FontWeight.Bold)},
                            supportingContent={Text("#"+p.gameNumber.toString().padStart(3,'0')+" · National #"+p.nationalId)},
                            leadingContent={PokemonArtwork(PokemonRepository.byId(p.nationalId)?.spriteUrl,null,Modifier.size(52.dp),pokemonId=p.nationalId)},
                            trailingContent={Text(if(caught)"✓" else "—")},
                            modifier=Modifier.clickable{onPokemonClick(p.nationalId,source)}
                        )
                    }
                }
            }
        }
    }
}
