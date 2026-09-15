package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GameDexScreen(onBack:()->Unit,onPokemonClick:(Int,String?)->Unit){
 val game=AppStatePreferences.activeGame
 val regions=remember(game){AppGameCatalog.games.firstOrNull{it.label==game}?.regions.orEmpty()}
 var source by remember(game){mutableStateOf(AppStatePreferences.activeRegionForGame(game) ?: regions.firstOrNull()?.source)}
 var dex by remember(source){mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())}
 var plan by remember(source){mutableStateOf<List<CapturePlanEntry>>(emptyList())}
 var missingOnly by remember{mutableStateOf(false)}
 var routeMode by remember{mutableStateOf(false)}
 var loading by remember(source){mutableStateOf(source!=null)}
 LaunchedEffect(source){
  val s=source ?: return@LaunchedEffect
  loading=true
  withContext(Dispatchers.IO){
   val ctx=GameContext.fromSource(s)
   val loaded=ctx?.let{GameDexService.loadGameDex(it)}.orEmpty()
   dex=loaded
   plan=CapturePlanner.plan(s,loaded)
  }
  loading=false
 }
 val shown=remember(dex,missingOnly,source,CollectionStore.contextualCapturedIds){
  if(!missingOnly) dex else dex.filterNot{source!=null && CollectionStore.isCapturedIn(source!!,it.nationalId)}
 }
 Scaffold(topBar={TopAppBar(title={Text("Pokédex do jogo")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}})}){pad->
  Column(Modifier.fillMaxSize().padding(pad)){
   Text(game,Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
   if(regions.size>1) ScrollableTabRow(selectedTabIndex=regions.indexOfFirst{it.source==source}.coerceAtLeast(0),edgePadding=12.dp){
    regions.forEach{r->Tab(selected=r.source==source,onClick={source=r.source;AppStatePreferences.setActiveRegionForGame(game,r.source)},text={Text(r.label)})}
   }
   Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    FilterChip(missingOnly,{missingOnly=!missingOnly},{Text("Faltantes")})
    FilterChip(routeMode,{routeMode=!routeMode},{Text("Melhor rota")},leadingIcon={Icon(Icons.Default.Route,null)})
   }
   when{
    loading->DexStatusPane("Carregando Pokédex","Preparando dados da região selecionada.",Modifier.fillMaxSize().padding(16.dp),true)
    routeMode->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     items(plan,key={it.pokemonId}){p->Card(Modifier.fillMaxWidth().clickable{onPokemonClick(p.pokemonId,source)}){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){PokemonArtwork(PokemonRepository.byId(p.pokemonId)?.spriteUrl,null,Modifier.size(54.dp),pokemonId=p.pokemonId);Column(Modifier.weight(1f).padding(start=10.dp)){Text(p.name,fontWeight=FontWeight.Bold);Text(p.summary,style=MaterialTheme.typography.bodySmall)};Icon(Icons.Default.LocationOn,null)}}}
    }
    else->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
     items(shown,key={it.nationalId}){p->val caught=source?.let{CollectionStore.isCapturedIn(it,p.nationalId)}==true;ListItem(headlineContent={Text(p.name,fontWeight=FontWeight.Bold)},supportingContent={Text("#"+p.localNumber.toString().padStart(3,'0')+" · National #"+p.nationalId)},leadingContent={PokemonArtwork(PokemonRepository.byId(p.nationalId)?.spriteUrl,null,Modifier.size(52.dp),pokemonId=p.nationalId)},trailingContent={Text(if(caught)"✓" else "—")},modifier=Modifier.clickable{onPokemonClick(p.nationalId,source)})}
    }
   }
  }
 }
}
