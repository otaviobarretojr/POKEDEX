package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class BoxV2GameGroup(val label:String,val regions:List<BoxV2Region>)
private data class BoxV2Region(val label:String,val source:String,val badge:String)
private val boxV2GameGroups=listOf(
 BoxV2GameGroup("Scarlet / Violet",listOf(BoxV2Region("Paldea","Scarlet / Violet · Paldea","Jogo base"),BoxV2Region("Kitakami","Scarlet / Violet · Kitakami","DLC · The Teal Mask"),BoxV2Region("Blueberry","Scarlet / Violet · Blueberry","DLC · The Indigo Disk"))),
 BoxV2GameGroup("Sword / Shield",listOf(BoxV2Region("Galar","Sword / Shield · Galar","Jogo base"))),
 BoxV2GameGroup("Let's Go Pikachu / Eevee",listOf(BoxV2Region("Kanto","Let's Go Pikachu / Eevee · Kanto","Jogo base"))),
 BoxV2GameGroup("Legends Arceus",listOf(BoxV2Region("Hisui","Legends Arceus · Hisui","Jogo base")))
)
@OptIn(ExperimentalMaterial3Api::class) @Composable fun BoxesV2Screen(onPokemonClick:(Int,String?)->Unit){
 var game by remember{mutableStateOf(boxV2GameGroups.first())};var region by remember{mutableStateOf(game.regions.first())};var dex by remember{mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())};var loading by remember{mutableStateOf(true)};var page by remember{mutableIntStateOf(0)};var gameMenu by remember{mutableStateOf(false)};var regionMenu by remember{mutableStateOf(false)};var allBoxesOpen by remember{mutableStateOf(false)};var searchOpen by remember{mutableStateOf(false)};val captured=CollectionStore.capturedIds
 LaunchedEffect(region.source){loading=true;page=0;val c=GameContext.fromSource(region.source);dex=if(c==null) emptyList() else runCatching{withContext(Dispatchers.IO){GameDexService.loadGameDex(c)}}.getOrElse{emptyList()};loading=false}
 val pageCount=((dex.size+29)/30).coerceAtLeast(1);val safePage=page.coerceIn(0,pageCount-1);val pageEntries=dex.drop(safePage*30).take(30);val caught=dex.count{it.nationalId in captured};val remaining=(dex.size-caught).coerceAtLeast(0);val progress=if(dex.isEmpty())0f else caught.toFloat()/dex.size;val first=pageEntries.firstOrNull()?.gameNumber?:safePage*30+1;val last=pageEntries.lastOrNull()?.gameNumber?:((safePage+1)*30).coerceAtMost(dex.size)
 Column(Modifier.fillMaxSize().padding(horizontal=12.dp)){
  Row(Modifier.fillMaxWidth().padding(top=6.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   ExposedDropdownMenuBox(gameMenu,{gameMenu=!gameMenu},Modifier.weight(1.15f)){OutlinedTextField(game.label,{},Modifier.menuAnchor().fillMaxWidth(),readOnly=true,singleLine=true,label={Text("Jogo")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(gameMenu)});ExposedDropdownMenu(gameMenu,{gameMenu=false}){boxV2GameGroups.forEach{o->DropdownMenuItem({Text(o.label)},{game=o;region=o.regions.first();gameMenu=false})}}}
   ExposedDropdownMenuBox(regionMenu,{regionMenu=!regionMenu},Modifier.weight(.85f)){OutlinedTextField(region.label,{},Modifier.menuAnchor().fillMaxWidth(),readOnly=true,singleLine=true,label={Text(if(game.regions.size>1)"DLC / região" else "Região")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(regionMenu)});ExposedDropdownMenu(regionMenu,{regionMenu=false}){game.regions.forEach{o->DropdownMenuItem({Column{Text(o.label,fontWeight=FontWeight.SemiBold);Text(o.badge,style=MaterialTheme.typography.labelSmall)}},{region=o;regionMenu=false})}}}
  }
  Text("${region.label} · ${region.badge}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(start=4.dp,top=4.dp))
  Card(Modifier.fillMaxWidth().padding(top=6.dp),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Row(Modifier.fillMaxWidth().padding(14.dp,10.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(58.dp),contentAlignment=Alignment.Center){CircularProgressIndicator({progress},Modifier.fillMaxSize(),strokeWidth=6.dp);Text("${(progress*100).toInt()}%",fontWeight=FontWeight.Bold)};Column(Modifier.weight(1.2f).padding(start=10.dp)){Text("$caught / ${dex.size}",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("capturados",style=MaterialTheme.typography.labelMedium)};BoxV2Stat("${dex.size}","Total",Modifier.weight(.8f));BoxV2Stat("$caught","Capt.",Modifier.weight(.8f));BoxV2Stat("$remaining","Rest.",Modifier.weight(.8f))}}
  Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){FilledTonalIconButton({page=safePage-1},enabled=safePage>0){Icon(Icons.Default.ChevronLeft,"Anterior")};Column(horizontalAlignment=Alignment.CenterHorizontally){Text("Box ${safePage+1}",fontWeight=FontWeight.Bold);Text(if(dex.isEmpty())"—" else "Pokémon ${first.toString().padStart(3,'0')} – ${last.toString().padStart(3,'0')}",style=MaterialTheme.typography.labelMedium)};FilledTonalIconButton({page=safePage+1},enabled=safePage<pageCount-1){Icon(Icons.Default.ChevronRight,"Próxima")}}
  when{loading->Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){CircularProgressIndicator()};dex.isEmpty()->Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Text("Não foi possível carregar esta Pokédex regional.")};else->LazyVerticalGrid(GridCells.Fixed(6),Modifier.weight(1f).fillMaxWidth(),userScrollEnabled=false,horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){items(pageEntries,key={it.gameNumber}){p->BoxV2Slot(p,p.nationalId in captured){onPokemonClick(p.nationalId,region.source)}}}}
  Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){FilledTonalButton({allBoxesOpen=true},Modifier.weight(1f)){Icon(Icons.Default.GridView,null);Spacer(Modifier.width(7.dp));Text("Todas as Boxes")};FilledTonalButton({searchOpen=true},Modifier.weight(1f)){Icon(Icons.Default.Search,null);Spacer(Modifier.width(7.dp));Text("Pesquisar")}}
 }
 if(allBoxesOpen)ModalBottomSheet({allBoxesOpen=false}){Column(Modifier.fillMaxWidth().padding(20.dp,8.dp)){Text("Todas as Boxes",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("${game.label} · ${region.label} · $pageCount Boxes");LazyColumn(Modifier.fillMaxWidth().heightIn(max=520.dp)){items((0 until pageCount).toList()){p->val es=dex.drop(p*30).take(30);val c=es.count{it.nationalId in captured};Card(Modifier.fillMaxWidth().padding(vertical=3.dp).clickable{page=p;allBoxesOpen=false}){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("Box ${p+1}",fontWeight=FontWeight.SemiBold);Text("$c/${es.size} capturados")}}}}}}
 if(searchOpen)BoxV2Search(dex,captured,{searchOpen=false},{p->val i=dex.indexOfFirst{it.nationalId==p.nationalId};if(i>=0)page=i/30;searchOpen=false},{p->searchOpen=false;onPokemonClick(p.nationalId,region.source)})
}
@Composable private fun BoxV2Stat(v:String,l:String,m:Modifier=Modifier){Column(m,horizontalAlignment=Alignment.CenterHorizontally){Text(v,fontWeight=FontWeight.Bold);Text(l,style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.Center)}}
@Composable private fun BoxV2Slot(p:GameDexService.GameDexEntry,c:Boolean,onClick:()->Unit){Card(Modifier.fillMaxWidth().aspectRatio(.82f).clickable(onClick=onClick),shape=RoundedCornerShape(12.dp),colors=CardDefaults.cardColors(containerColor=if(c)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)){Box(Modifier.fillMaxSize()){AsyncImage(p.spriteUrl,p.name,Modifier.align(Alignment.Center).fillMaxWidth(.94f).aspectRatio(1f).alpha(if(c)1f else .18f),colorFilter=if(c)null else ColorFilter.colorMatrix(ColorMatrix().apply{setToSaturation(0f)}));Text(p.gameNumber.toString().padStart(3,'0'),Modifier.align(Alignment.BottomCenter).fillMaxWidth(),textAlign=TextAlign.Center,style=MaterialTheme.typography.labelSmall)}}}
@Composable private fun BoxV2Search(d:List<GameDexService.GameDexEntry>,c:Set<Int>,dismiss:()->Unit,select:(GameDexService.GameDexEntry)->Unit,open:(GameDexService.GameDexEntry)->Unit){var q by remember{mutableStateOf("")};val x=q.trim().removePrefix("#");val r=remember(d,q){if(x.isBlank())emptyList() else d.filter{it.name.contains(x,true)||it.gameNumber.toString()==x||it.nationalId.toString()==x}.take(12)};AlertDialog(dismiss,{Text("Pesquisar Pokémon")},{Column{OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Nome ou número")});LazyColumn(Modifier.heightIn(max=360.dp)){items(r,key={it.nationalId}){p->Row(Modifier.fillMaxWidth().clickable{select(p)}.padding(7.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(p.spriteUrl,p.name,Modifier.size(48.dp));Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.SemiBold);Text("#${p.gameNumber.toString().padStart(3,'0')} · ${if(p.nationalId in c)"Capturado" else "Faltando"}")};TextButton({open(p)}){Text("Ficha")}}}}}}, {},{TextButton(dismiss){Text("Fechar")}})}
