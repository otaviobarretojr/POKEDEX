package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.PokedexDataStore
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.PokemonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PokedexV2Screen(onPokemonClick:(Int)->Unit,onOpenReference:()->Unit={}){
 var query by remember{mutableStateOf("")}
 var generation by remember{mutableIntStateOf(0)}
 var status by remember{mutableIntStateOf(0)}
 var dex by remember{mutableStateOf<List<PokeApiService.DexIndexEntry>>(emptyList())}
 var loading by remember{mutableStateOf(true)}
 val captured=CollectionStore.capturedIds
 LaunchedEffect(Unit){loading=true;dex=runCatching{withContext(Dispatchers.IO){PokedexDataStore.nationalDex()}}.getOrElse{PokemonRepository.all().map{PokeApiService.DexIndexEntry(it.id,it.name,it.generation)}};loading=false}
 val filtered=remember(dex,query,generation,status,captured){val q=query.trim().removePrefix("#");dex.filter{p->(q.isBlank()||p.name.contains(q,true)||p.id.toString()==q)&&(generation==0||p.generation==generation)&&when(status){1->p.id in captured;2->p.id !in captured;else->true}}}
 Column(Modifier.fillMaxSize().background(Color(0xFFF8F8FC))){
  Column(Modifier.padding(horizontal=16.dp,vertical=10.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("POKEDEX",fontSize=29.sp,fontWeight=FontWeight.Black,color=Color(0xFF151426));Text("C A T C H  E M  ·  T O D A S  A S  R E G I Õ E S",fontSize=7.sp,color=Color(0xFF72778B))};FilledTonalButton(onOpenReference,shape=RoundedCornerShape(16.dp)){Icon(Icons.Default.MenuBook,null);Spacer(Modifier.width(6.dp));Text("Dados")}}
   OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(top=10.dp),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Nome ou número nacional")},shape=RoundedCornerShape(18.dp))
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=7.dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.FilterAlt,null);FilterChip(selected=status==0,onClick={status=0},label={Text("Todos")});FilterChip(selected=status==1,onClick={status=1},label={Text("Capturados")});FilterChip(selected=status==2,onClick={status=2},label={Text("Faltantes")})}
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=5.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){(0..9).forEach{g->FilterChip(selected=generation==g,onClick={generation=g},label={Text(if(g==0)"Todas Gerações" else "G$g")})}}
   Text(if(loading)"Carregando…" else "${filtered.size} Pokémon",style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=6.dp))
  }
  if(loading)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}else LazyVerticalGrid(GridCells.Fixed(4),Modifier.fillMaxSize().padding(horizontal=10.dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
   items(filtered,key={it.id}){p->val caught=p.id in captured;Card(Modifier.fillMaxWidth().aspectRatio(.78f).clickable{onPokemonClick(p.id)},shape=RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(containerColor=if(caught)Color(0xFFEAE8FB) else Color(0xFFF1F0F8))){Column(Modifier.fillMaxSize().padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally){AsyncImage(model=p.spriteUrl,contentDescription=p.name,contentScale=ContentScale.Fit,modifier=Modifier.weight(1f).fillMaxWidth(.9f).padding(3.dp).alpha(if(caught)1f else .26f),colorFilter=if(caught)null else ColorFilter.colorMatrix(ColorMatrix().apply{setToSaturation(0f)}));Text(p.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelMedium);Text("#${p.id.toString().padStart(4,'0')} · G${p.generation}",style=MaterialTheme.typography.labelSmall)}}}
   item{Spacer(Modifier.height(12.dp))}
  }
 }
}
