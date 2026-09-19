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
            CompanionContextHeader(
                title=game,
                eyebrow="Pokédex do jogo",
                subtitle=source?.let{active->regions.firstOrNull{it.source==active}?.label} ?: "Região ativa",
                modifier=Modifier.padding(horizontal=PokedexDesignTokens.Spacing.Lg,vertical=PokedexDesignTokens.Spacing.Sm),
                progress={
                    val total=dex.size
                    val done=dex.count{it.nationalId in captured}
                    Text(
                        if(total>0) "$done de $total registrados" else "Preparando progresso da região",
                        style=MaterialTheme.typography.labelLarge,
                        fontWeight=FontWeight.Bold
                    )
                }
            )
            if(regions.size>1) ScrollableTabRow(selectedTabIndex=regions.indexOfFirst{it.source==source}.coerceAtLeast(0),edgePadding=12.dp){
                regions.forEach{r->Tab(selected=r.source==source,onClick={source=r.source;AppStatePreferences.setActiveRegionForGame(game,r.source)},text={Text(r.label)})}
            }
            LazyRow(
                contentPadding=PaddingValues(horizontal=PokedexDesignTokens.Spacing.Lg,vertical=PokedexDesignTokens.Spacing.Sm),
                horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)
            ){
                item(key="best_route"){
                    FilterChip(
                        selected=routeMode,
                        onClick={routeMode=!routeMode},
                        label={Text("Melhor rota")},
                        leadingIcon={Icon(Icons.Default.Route,"Melhor rota")}
                    )
                }
                if(!routeMode){
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
                        CompanionSectionHeader(title="Melhor rota",supporting="Capture por local primeiro, depois evolua. Trocas, transferências e métodos especiais ficam por último.",modifier=Modifier.padding(start=4.dp,end=4.dp,bottom=4.dp))
                    }
                    routeGroups.forEach{group->
                        item(key="route_${group.key}",contentType="route_header"){
                            CompanionSectionHeader(title=group.key,supporting="${group.value.size} Pokémon",modifier=Modifier.padding(top=4.dp))
                        }
                        items(group.value,key={it.pokemonId},contentType={"route_item"}){p->
                            Surface(modifier=Modifier.fillMaxWidth().clickable{onPokemonClick(p.pokemonId,source)},shape=androidx.compose.foundation.shape.RoundedCornerShape(PokedexDesignTokens.Radius.Lg),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.34f)){
                                Row(Modifier.padding(PokedexDesignTokens.Spacing.Md),verticalAlignment=Alignment.CenterVertically){
                                    PokemonArtwork(PokemonRepository.byId(p.pokemonId)?.spriteUrl,null,Modifier.size(58.dp),pokemonId=p.pokemonId)
                                    Column(Modifier.weight(1f).padding(start=PokedexDesignTokens.Spacing.Md)){Text(p.name,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleSmall);Text(p.summary,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                                    if(p.method==ObtainMethod.CAPTURE) Icon(Icons.Default.LocationOn,"Captura direta",tint=MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                shown.isEmpty()->DexStatusPane("Nenhum resultado","Não há Pokémon para este filtro.",Modifier.fillMaxSize().padding(16.dp),false)
                else->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    items(shown,key={it.nationalId},contentType={"dex_item"}){p->
                        val caught=p.nationalId in captured
                        Surface(
                            modifier=Modifier.fillMaxWidth().clickable{onPokemonClick(p.nationalId,source)},
                            shape=androidx.compose.foundation.shape.RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
                            color=if(caught) MaterialTheme.colorScheme.primaryContainer.copy(alpha=.34f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.26f)
                        ){
                            Row(Modifier.padding(PokedexDesignTokens.Spacing.Md),verticalAlignment=Alignment.CenterVertically){
                                PokemonArtwork(PokemonRepository.byId(p.nationalId)?.spriteUrl,null,Modifier.size(58.dp),pokemonId=p.nationalId)
                                Column(Modifier.weight(1f).padding(start=PokedexDesignTokens.Spacing.Md)){
                                    Text(p.name,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleSmall)
                                    Text("#"+p.gameNumber.toString().padStart(3,'0')+" · National #"+p.nationalId,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(if(caught)"Registrado" else "Faltando",style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold,color=if(caught) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
