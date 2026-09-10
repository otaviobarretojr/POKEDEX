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

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BoxesV2Screen(onPokemonClick:(Int,String?)->Unit){
 var game by remember{mutableStateOf(boxV2GameGroups.first())};var region by remember{mutableStateOf(game.regions.first())};var dex by remember{mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())};var loading by remember{mutableStateOf(true)};var page by remember{mutableIntStateOf(0)};var gameMenu by remember{mutableStateOf(false)};var regionMenu by remember{mutableStateOf(false)};var allBoxesOpen by remember{mutableStateOf(false)};var searchOpen by remember{mutableStateOf(false)};val captured=CollectionStore.capturedIds
 LaunchedEffect(region.source){loading=true;page=0;val c=GameContext.fromSource(region.source);dex=if(c==null)emptyList()else runCatching{withContext(Dispatchers.IO){GameDexService.loadGameDex(c)}}.getOrElse{emptyList()};loading=false}
 val pageCount=((dex.size+29)/30).coerceAtLeast(1);val safePage=page.coerceIn(0,pageCount-1);val pageEntries=dex.drop(safePage*30).take(30);val caught=dex.count{it.nationalId in captured};val remaining=(dex.size-caught).coerceAtLeast(0);val progress=if(dex.isEmpty())0f else caught.toFloat()/dex.size;val first=pageEntries.firstOrNull()?.gameNumber?:safePage*30+1;val last=pageEntries.lastOrNull()?.gameNumber?:((safePage+1)*30).coerceAtMost(dex.size)
 Column(Modifier.fillMaxSize().padding(horizontal=10.dp)){
  Text("POKEDEX",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=4.dp,bottom=2.dp))
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   ExposedDropdownMenuBox(expanded=gameMenu,onExpandedChange={gameMenu=!gameMenu},modifier=Modifier.weight(1.15f)){OutlinedTextField(value=game.label,onValueChange={},modifier=Modifier.menuAnchor().fillMaxWidth(),readOnly=true,singleLine=true,label={Text("Jogo",style=MaterialTheme.typography.labelSmall)},textStyle=MaterialTheme.typography.bodyLarge,trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(gameMenu)});ExposedDropdownMenu(expanded=gameMenu,onDismissRequest={gameMenu=false}){boxV2GameGroups.forEach{o->DropdownMenuItem(text={Text(o.label)},onClick={game=o;region=o.regions.first();gameMenu=false})}}}
   ExposedDropdownMenuBox(expanded=regionMenu,onExpandedChange={regionMenu=!regionMenu},modifier=Modifier.weight(.85f)){OutlinedTextField(value=region.label,onValueChange={},modifier=Modifier.menuAnchor().fillMaxWidth(),readOnly=true,singleLine=true,label={Text(if(game.regions.size>1)"DLC / região" else "Região",style=MaterialTheme.typography.labelSmall)},textStyle=MaterialTheme.typography.bodyLarge,trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(regionMenu)});ExposedDropdownMenu(expanded=regionMenu,onDismissRequest={regionMenu=false}){game.regions.forEach{o->DropdownMenuItem(text={Column{Text(o.label,fontWeight=FontWeight.SemiBold);Text(o.badge,style=MaterialTheme.typography.labelSmall)}},onClick={region=o;regionMenu=false})}}}
  }
  Card(Modifier.fillMaxWidth().padding(top=5.dp),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){
   Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){
    Box(Modifier.size(46.dp),contentAlignment=Alignment.Center){CircularProgressIndicator(progress={progress},modifier=Modifier.fillMaxSize(),strokeWidth=5.dp);Text("${(progress*100).toInt()}%",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium)}
    Column(Modifier.weight(1.35f).padding(start=9.dp)){Text("$caught / ${dex.size}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text("${region.label} · ${region.badge}",style=MaterialTheme.typography.labelSmall,maxLines=1)}
    BoxV2Stat("$remaining","Restantes",Modifier.weight(.8f))
   }
  }
  Row(Modifier.fillMaxWidth().height(46.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
   FilledTonalIconButton(onClick={page=safePage-1},enabled=safePage>0,modifier=Modifier.size(38.dp)){Icon(Icons.Default.ChevronLeft,"Anterior")}
   Text("Box ${safePage+1}  ·  ${first.toString().padStart(3,'0')}–${last.toString().padStart(3,'0')}",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleSmall)
   FilledTonalIconButton(onClick={page=safePage+1},enabled=safePage<pageCount-1,modifier=Modifier.size(38.dp)){Icon(Icons.Default.ChevronRight,"Próxima")}
  }
  when{loading->Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){CircularProgressIndicator()};dex.isEmpty()->Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Text("Não foi possível carregar esta Pokédex regional.")};else->LazyVerticalGrid(columns=GridCells.Fixed(6),modifier=Modifier.weight(1f).fillMaxWidth(),userScrollEnabled=false,horizontalArrangement=Arrangement.spacedBy(3.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){items(pageEntries,key={it.gameNumber}){p->BoxV2Slot(p,p.nationalId in captured){onPokemonClick(p.nationalId,region.source)}}}}
  Row(Modifier.fillMaxWidth().padding(top=4.dp,bottom=4.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilledTonalButton(onClick={allBoxesOpen=true},modifier=Modifier.weight(1f).height(44.dp),contentPadding=PaddingValues(horizontal=10.dp)){Icon(Icons.Default.GridView,null);Spacer(Modifier.width(6.dp));Text("Todas as Boxes",style=MaterialTheme.typography.labelLarge)};FilledTonalButton(onClick={searchOpen=true},modifier=Modifier.weight(1f).height(44.dp),contentPadding=PaddingValues(horizontal=10.dp)){Icon(Icons.Default.Search,null);Spacer(Modifier.width(6.dp));Text("Pesquisar",style=MaterialTheme.typography.labelLarge)}}
 }
 if(allBoxesOpen)ModalBottomSheet(onDismissRequest={allBoxesOpen=false}){Column(Modifier.fillMaxWidth().padding(20.dp,8.dp)){Text("Todas as Boxes",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("${game.label} · ${region.label} · $pageCount Boxes");LazyColumn(Modifier.fillMaxWidth().heightIn(max=520.dp)){items((0 until pageCount).toList()){p->val es=dex.drop(p*30).take(30);val c=es.count{it.nationalId in captured};Card(Modifier.fillMaxWidth().padding(vertical=3.dp).clickable{page=p;allBoxesOpen=false}){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("Box ${p+1}",fontWeight=FontWeight.SemiBold);Text("$c/${es.size} capturados")}}}}}}
 if(searchOpen)BoxV2Search(dex,captured,{searchOpen=false},{p->val i=dex.indexOfFirst{it.nationalId==p.nationalId};if(i>=0)page=i/30;searchOpen=false},{p->searchOpen=false;onPokemonClick(p.nationalId,region.source)})
}
@Composable private fun BoxV2Stat(v:String,l:String,m:Modifier=Modifier){Column(m,horizontalAlignment=Alignment.CenterHorizontally){Text(v,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);Text(l,style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.Center)}}
@Composable private fun BoxV2Slot(p:GameDexService.GameDexEntry,c:Boolean,onClick:()->Unit){Card(modifier=Modifier.fillMaxWidth().aspectRatio(.86f).clickable(onClick=onClick),shape=RoundedCornerShape(10.dp),colors=CardDefaults.cardColors(containerColor=if(c)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)){Box(Modifier.fillMaxSize()){AsyncImage(model=p.spriteUrl,contentDescription=p.name,modifier=Modifier.align(Alignment.Center).fillMaxWidth(.9f).aspectRatio(1f).alpha(if(c)1f else .18f),colorFilter=if(c)null else ColorFilter.colorMatrix(ColorMatrix().apply{setToSaturation(0f)}));Text(p.gameNumber.toString().padStart(3,'0'),Modifier.align(Alignment.BottomCenter).fillMaxWidth(),textAlign=TextAlign.Center,style=MaterialTheme.typography.labelSmall)}}}
@Composable private fun BoxV2Search(d:List<GameDexService.GameDexEntry>,c:Set<Int>,dismiss:()->Unit,select:(GameDexService.GameDexEntry)->Unit,open:(GameDexService.GameDexEntry)->Unit){var q by remember{mutableStateOf("")};val x=q.trim().removePrefix("#");val r=remember(d,q){if(x.isBlank())emptyList()else d.filter{it.name.contains(x,true)||it.gameNumber.toString()==x||it.nationalId.toString()==x}.take(12)};AlertDialog(onDismissRequest=dismiss,title={Text("Pesquisar Pokémon")},text={Column{OutlinedTextField(value=q,onValueChange={q=it},modifier=Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Nome ou número")});LazyColumn(Modifier.heightIn(max=360.dp)){items(r,key={it.nationalId}){p->Row(Modifier.fillMaxWidth().clickable{select(p)}.padding(7.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(model=p.spriteUrl,contentDescription=p.name,modifier=Modifier.size(48.dp));Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.SemiBold);Text("#${p.gameNumber.toString().padStart(3,'0')} · ${if(p.nationalId in c)"Capturado" else "Faltando"}")};TextButton(onClick={open(p)}){Text("Ficha")}}}}}},confirmButton={},dismissButton={TextButton(onClick=dismiss){Text("Fechar")}})}
