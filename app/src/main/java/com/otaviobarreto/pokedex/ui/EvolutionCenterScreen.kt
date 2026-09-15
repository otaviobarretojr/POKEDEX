package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
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
@Composable fun EvolutionCenterScreen(onBack:()->Unit,onPokemonClick:(Int,String?)->Unit){
 val game=AppStatePreferences.activeGame
 val source=AppStatePreferences.activeRegionForGame(game) ?: AppGameCatalog.adventureGames.firstOrNull{it.label==game}?.regions?.firstOrNull()?.source
 var routes by remember(source){mutableStateOf<List<EvolutionRoute>>(emptyList())}
 var loading by remember(source){mutableStateOf(source!=null)}
 LaunchedEffect(source){
  if(source==null){loading=false;return@LaunchedEffect}
  routes=withContext(Dispatchers.IO){
   val ctx=GameContext.fromSource(source) ?: return@withContext emptyList()
   val dex=GameDexService.loadGameDex(ctx)
   EvolutionFilterIndex.buildRoutes(source,dex).filter(EvolutionResolutionEngine::executable)
  }
  loading=false
 }
 Scaffold(topBar={TopAppBar(title={Text("Central de evolução")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}})}){pad->
  when{
   source==null->DexStatusPane("Selecione uma Jornada","A Central de evolução usa o jogo ativo para mostrar somente métodos válidos.",Modifier.fillMaxSize().padding(pad).padding(16.dp))
   loading->DexStatusPane("Preparando evoluções","Organizando os métodos válidos para "+game+".",Modifier.fillMaxSize().padding(pad).padding(16.dp),loading=true)
   routes.isEmpty()->DexStatusPane("Nenhuma evolução encontrada","Não há rotas executáveis disponíveis para este contexto.",Modifier.fillMaxSize().padding(pad).padding(16.dp))
   else->LazyColumn(Modifier.fillMaxSize().padding(pad),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    item{DexGlassSurface(Modifier.fillMaxWidth()){DexSectionEyebrow(game);Text("O que você pode evoluir",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text(source,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
    items(routes,key={it.sourcePokemonId.toString()+":"+it.targetPokemonId+":"+it.summary}){r->
     Card(Modifier.fillMaxWidth().clickable{onPokemonClick(r.sourcePokemonId,source)}){
      Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
       PokemonArtwork(model=PokemonRepository.byId(r.sourcePokemonId)?.spriteUrl,contentDescription=null,modifier=Modifier.size(54.dp),pokemonId=r.sourcePokemonId)
       Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text((PokemonRepository.byId(r.sourcePokemonId)?.name?:"#"+r.sourcePokemonId)+" → "+(PokemonRepository.byId(r.targetPokemonId)?.name?:"#"+r.targetPokemonId),fontWeight=FontWeight.Bold);Text(r.summary,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
       Icon(Icons.Default.AutoAwesome,null,tint=MaterialTheme.colorScheme.primary)
      }
     }
    }
   }
  }
 }
}
