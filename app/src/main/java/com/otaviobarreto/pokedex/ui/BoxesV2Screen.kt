package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val QBbg=Color(0xFFFBF8FF);private val QBsurface=Color(0xFFF1EEFA);private val QBink=Color(0xFF171522);private val QBmuted=Color(0xFF777286);private val QBmint=Color(0xFF67D7CB)
private data class QBRegion(val label:String,val source:String,val badge:String)
private data class QBGame(val label:String,val accent:Color,val regions:List<QBRegion>)
private val qbGames=listOf(
 QBGame("Scarlet / Violet",Color(0xFFB54C5D),listOf(QBRegion("Paldea","Scarlet / Violet · Paldea","Jogo base"),QBRegion("Kitakami","Scarlet / Violet · Kitakami","DLC · The Teal Mask"),QBRegion("Blueberry","Scarlet / Violet · Blueberry","DLC · The Indigo Disk"))),
 QBGame("Sword / Shield",Color(0xFF35A9C7),listOf(QBRegion("Galar","Sword / Shield · Galar","Jogo base"))),
 QBGame("Let's Go Pikachu / Eevee",Color(0xFFE0A929),listOf(QBRegion("Kanto","Let's Go Pikachu / Eevee · Kanto","Jogo base"))),
 QBGame("Legends Arceus",Color(0xFF527F7C),listOf(QBRegion("Hisui","Legends Arceus · Hisui","Jogo base"))))

@OptIn(ExperimentalMaterial3Api::class,ExperimentalFoundationApi::class)
@Composable fun BoxesV2Screen(onPokemonClick:(Int,String?)->Unit){
 var game by remember{mutableStateOf(qbGames.first())};var region by remember{mutableStateOf(game.regions.first())};var dex by remember{mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())};var loading by remember{mutableStateOf(true)};var page by remember{mutableIntStateOf(0)};var gameMenu by remember{mutableStateOf(false)};var regionMenu by remember{mutableStateOf(false)};var quick by remember{mutableStateOf<GameDexService.GameDexEntry?>(null)};var search by remember{mutableStateOf(false)}
 val captured=CollectionStore.capturedIds
 LaunchedEffect(region.source){loading=true;page=0;val ctx=GameContext.fromSource(region.source);dex=if(ctx==null)emptyList()else runCatching{withContext(Dispatchers.IO){GameDexService.loadGameDex(ctx)}}.getOrElse{emptyList()};loading=false}
 val pages=((dex.size+29)/30).coerceAtLeast(1);val current=page.coerceIn(0,pages-1);val entries=dex.drop(current*30).take(30);val caught=dex.count{it.nationalId in captured};val progress=if(dex.isEmpty())0f else caught.toFloat()/dex.size
 Column(Modifier.fillMaxSize().background(QBbg).padding(horizontal=12.dp)){
  Column(Modifier.padding(top=5.dp,bottom=5.dp)){Text("POKEDEX",fontSize=27.sp,lineHeight=28.sp,fontWeight=FontWeight.Black,color=QBink);Text("C A T C H  E M  ·  T O D A S  A S  R E G I Õ E S",fontSize=7.sp,color=QBmuted)}
  Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
   ExposedDropdownMenuBox(gameMenu,{gameMenu=!gameMenu},Modifier.weight(1.15f)){OutlinedTextField(game.label,{},Modifier.menuAnchor().fillMaxWidth(),readOnly=true,singleLine=true,label={Text("Jogo")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(gameMenu)},shape=RoundedCornerShape(15.dp));ExposedDropdownMenu(gameMenu,{gameMenu=false}){qbGames.forEach{g->DropdownMenuItem({Text(g.label,fontWeight=FontWeight.SemiBold)},{game=g;region=g.regions.first();gameMenu=false})}}}
   ExposedDropdownMenuBox(regionMenu,{regionMenu=!regionMenu},Modifier.weight(.85f)){OutlinedTextField(region.label,{},Modifier.menuAnchor().fillMaxWidth(),readOnly=true,singleLine=true,label={Text(if(game.regions.size>1)"DLC / região" else "Região")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(regionMenu)},shape=RoundedCornerShape(15.dp));ExposedDropdownMenu(regionMenu,{regionMenu=false}){game.regions.forEach{r->DropdownMenuItem({Column{Text(r.label,fontWeight=FontWeight.SemiBold);Text(r.badge,fontSize=10.sp,color=QBmuted)}},{region=r;regionMenu=false})}}}
  }
  Card(Modifier.fillMaxWidth().padding(top=7.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=QBsurface)){Row(Modifier.fillMaxWidth().height(68.dp).padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp),contentAlignment=Alignment.Center){CircularProgressIndicator({progress},Modifier.fillMaxSize(),color=game.accent,trackColor=game.accent.copy(alpha=.12f),strokeWidth=5.dp);Text("${(progress*100).toInt()}%",fontSize=10.sp,fontWeight=FontWeight.Black)};Spacer(Modifier.width(14.dp));QBStat("${dex.size}","Total",Modifier.weight(1f));QBStat("$caught","Capturados",Modifier.weight(1f),QBmint);QBStat("${(dex.size-caught).coerceAtLeast(0)}","Restantes",Modifier.weight(1f));Surface(shape=RoundedCornerShape(10.dp),color=Color.White.copy(alpha=.7f)){Column(Modifier.padding(horizontal=9.dp,vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Landscape,null,tint=game.accent,modifier=Modifier.size(14.dp));Text(region.label,fontSize=8.sp,fontWeight=FontWeight.Bold,maxLines=1)}}}}
  Row(Modifier.fillMaxWidth().height(50.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){FilledTonalIconButton({page=current-1},enabled=current>0){Icon(Icons.Default.ChevronLeft,"Anterior")};Surface(shape=RoundedCornerShape(13.dp),color=QBsurface){Row(Modifier.padding(horizontal=18.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Inventory2,null,tint=game.accent);Spacer(Modifier.width(7.dp));Text("Box ${current+1}",fontWeight=FontWeight.Black)}};FilledTonalIconButton({page=current+1},enabled=current<pages-1){Icon(Icons.Default.ChevronRight,"Próxima")}}
  Box(Modifier.weight(1f).fillMaxWidth()){if(loading)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=game.accent)}else if(dex.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Não foi possível carregar esta Pokédex regional.")}else QBGrid(entries,captured,{onPokemonClick(it.nationalId,region.source)},{quick=it})}
  FilledTonalButton({search=true},Modifier.fillMaxWidth().height(50.dp).padding(bottom=3.dp),shape=RoundedCornerShape(18.dp)){Icon(Icons.Default.Search,null);Spacer(Modifier.width(8.dp));Text("Pesquisar",fontWeight=FontWeight.Bold)}
 }
 quick?.let{pk->val inGameBox=pk.nationalId in CollectionStore.boxes[game.label].orEmpty();QBQuickDialog(pk,inGameBox,{quick=null}){if(inGameBox)CollectionStore.removeFromBox(game.label,pk.nationalId)else CollectionStore.addToBox(game.label,pk.nationalId);quick=null}}
 if(search)QBSearch(dex,captured,{search=false},{pk->val i=dex.indexOfFirst{it.nationalId==pk.nationalId};if(i>=0)page=i/30;search=false},{pk->search=false;onPokemonClick(pk.nationalId,region.source)})
}

@Composable private fun QBStat(value:String,label:String,modifier:Modifier,color:Color=QBink){Column(modifier,horizontalAlignment=Alignment.CenterHorizontally){Text(value,fontWeight=FontWeight.Black,fontSize=12.sp,color=color);Text(label,fontSize=8.sp,color=QBmuted,maxLines=1)}}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun QBGrid(entries:List<GameDexService.GameDexEntry>,captured:Set<Int>,open:(GameDexService.GameDexEntry)->Unit,longPress:(GameDexService.GameDexEntry)->Unit){Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(4.dp)){repeat(5){r->Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){repeat(6){col->val pk=entries.getOrNull(r*6+col);if(pk==null)Surface(Modifier.weight(1f).fillMaxHeight(),RoundedCornerShape(11.dp),color=QBsurface){}else Box(Modifier.weight(1f).fillMaxHeight()){QBSlot(pk,pk.nationalId in captured,{open(pk)},{longPress(pk)})}}}}}}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun QBSlot(pk:GameDexService.GameDexEntry,captured:Boolean,open:()->Unit,longPress:()->Unit){val haptic=LocalHapticFeedback.current;Surface(Modifier.fillMaxSize().combinedClickable(onClick=open,onLongClick={haptic.performHapticFeedback(HapticFeedbackType.LongPress);longPress()}),shape=RoundedCornerShape(11.dp),color=if(captured)Color(0xFFEAE6FA)else QBsurface){Column(Modifier.fillMaxSize().padding(2.dp),horizontalAlignment=Alignment.CenterHorizontally){AsyncImage(pk.spriteUrl,pk.name,Modifier.weight(1f).fillMaxWidth(.94f).alpha(if(captured)1f else .24f),colorFilter=if(captured)null else ColorFilter.colorMatrix(ColorMatrix().apply{setToSaturation(0f)}));Text(pretty(pk.name),fontSize=8.sp,lineHeight=8.sp,maxLines=1,overflow=TextOverflow.Ellipsis,color=if(captured)QBink else QBmuted);Text("#${pk.gameNumber.toString().padStart(3,'0')}",fontSize=8.sp,color=QBmuted)}}}

@Composable private fun QBQuickDialog(pk:GameDexService.GameDexEntry,inBox:Boolean,dismiss:()->Unit,confirm:()->Unit){AlertDialog(onDismissRequest=dismiss,icon={AsyncImage(pk.spriteUrl,pk.name,Modifier.size(96.dp))},title={Text(pretty(pk.name),fontWeight=FontWeight.Bold)},text={Text(if(inBox)"Este Pokémon já está nesta Box. Deseja remover?" else "Adicionar este Pokémon nesta Box?")},confirmButton={Button(confirm){Icon(if(inBox)Icons.Default.RemoveCircleOutline else Icons.Default.AddCircle,null);Spacer(Modifier.width(6.dp));Text(if(inBox)"Remover" else "Adicionar")}},dismissButton={TextButton(dismiss){Text("Cancelar")}})}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun QBSearch(dex:List<GameDexService.GameDexEntry>,captured:Set<Int>,dismiss:()->Unit,select:(GameDexService.GameDexEntry)->Unit,open:(GameDexService.GameDexEntry)->Unit){var q by remember{mutableStateOf("")};val key=q.trim().removePrefix("#");val results=if(key.isBlank())emptyList()else dex.filter{it.name.contains(key,true)||it.gameNumber.toString()==key||it.nationalId.toString()==key}.take(10);AlertDialog(onDismissRequest=dismiss,title={Text("Pesquisar Pokémon")},text={Column{OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Nome ou número")});Spacer(Modifier.height(8.dp));results.forEach{pk->Row(Modifier.fillMaxWidth().combinedClickable(onClick={select(pk)}).padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(pk.spriteUrl,pk.name,Modifier.size(44.dp));Column(Modifier.weight(1f)){Text(pretty(pk.name),fontWeight=FontWeight.SemiBold);Text(if(pk.nationalId in captured)"Capturado" else "Faltando",fontSize=11.sp,color=QBmuted)};TextButton({open(pk)}){Text("Ficha")}}}}},confirmButton={},dismissButton={TextButton(dismiss){Text("Fechar")}})}
private fun pretty(name:String)=name.split("-"," ").joinToString(" "){it.replaceFirstChar{c->if(c.isLowerCase())c.titlecase()else c.toString()}}
