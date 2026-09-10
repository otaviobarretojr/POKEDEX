package com.otaviobarreto.pokedex.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
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

private val BoxBg=Color(0xFFFBF8FF);private val BoxSurface=Color(0xFFF1EEFA);private val BoxSurfaceStrong=Color(0xFFE9E4F5);private val BoxMint=Color(0xFF67D7CB);private val BoxInk=Color(0xFF171522);private val BoxMuted=Color(0xFF777286)
private data class BoxV2Region(val label:String,val source:String,val badge:String)
private data class BoxV2GameGroup(val label:String,val accent:Color,val accent2:Color,val regions:List<BoxV2Region>)
private val boxV2GameGroups=listOf(
 BoxV2GameGroup("Scarlet / Violet",Color(0xFFB54C5D),Color(0xFF6D55B9),listOf(BoxV2Region("Paldea","Scarlet / Violet · Paldea","Jogo base"),BoxV2Region("Kitakami","Scarlet / Violet · Kitakami","DLC · The Teal Mask"),BoxV2Region("Blueberry","Scarlet / Violet · Blueberry","DLC · The Indigo Disk"))),
 BoxV2GameGroup("Sword / Shield",Color(0xFF35A9C7),Color(0xFFD84E72),listOf(BoxV2Region("Galar","Sword / Shield · Galar","Jogo base"))),
 BoxV2GameGroup("Let's Go Pikachu / Eevee",Color(0xFFE0A929),Color(0xFF9A765A),listOf(BoxV2Region("Kanto","Let's Go Pikachu / Eevee · Kanto","Jogo base"))),
 BoxV2GameGroup("Legends Arceus",Color(0xFF527F7C),Color(0xFF9A7953),listOf(BoxV2Region("Hisui","Legends Arceus · Hisui","Jogo base"))))

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BoxesV2Screen(onPokemonClick:(Int,String?)->Unit){
 var game by remember{mutableStateOf(boxV2GameGroups.first())};var region by remember{mutableStateOf(game.regions.first())};var dex by remember{mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())};var loading by remember{mutableStateOf(true)};var page by remember{mutableIntStateOf(0)};var gameMenu by remember{mutableStateOf(false)};var regionMenu by remember{mutableStateOf(false)};var allBoxesOpen by remember{mutableStateOf(false)};var searchOpen by remember{mutableStateOf(false)};var quickPokemon by remember{mutableStateOf<GameDexService.GameDexEntry?>(null)};val captured=CollectionStore.capturedIds
 LaunchedEffect(region.source){loading=true;page=0;val c=GameContext.fromSource(region.source);dex=if(c==null)emptyList()else runCatching{withContext(Dispatchers.IO){GameDexService.loadGameDex(c)}}.getOrElse{emptyList()};loading=false}
 val pages=((dex.size+29)/30).coerceAtLeast(1);val p=page.coerceIn(0,pages-1);val caught=dex.count{it.nationalId in captured};val left=(dex.size-caught).coerceAtLeast(0);val target=if(dex.isEmpty())0f else caught.toFloat()/dex.size;val progress by animateFloatAsState(target,tween(500),label="progress");val accent by animateColorAsState(game.accent,tween(250),label="accent");val accent2 by animateColorAsState(game.accent2,tween(250),label="accent2")
 Column(Modifier.fillMaxSize().background(BoxBg).padding(horizontal=12.dp)){
  Column(Modifier.padding(top=5.dp,bottom=3.dp)){Text("POKEDEX",fontSize=27.sp,lineHeight=28.sp,fontWeight=FontWeight.Black,color=BoxInk);Text("C A T C H  E M  ·  T O D A S  A S  R E G I Õ E S",fontSize=7.sp,color=BoxMuted)}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
   ExposedDropdownMenuBox(gameMenu,{gameMenu=!gameMenu},Modifier.weight(1.16f)){Surface(Modifier.menuAnchor().fillMaxWidth().height(58.dp),RoundedCornerShape(15.dp),accent.copy(alpha=.11f),border=androidx.compose.foundation.BorderStroke(1.5.dp,accent.copy(alpha=.72f))){Row(Modifier.fillMaxSize().padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(50),color=accent.copy(alpha=.16f),modifier=Modifier.size(34.dp)){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.CatchingPokemon,null,tint=accent)}};Column(Modifier.weight(1f).padding(start=9.dp)){Text("Jogo",fontSize=9.sp,color=accent,fontWeight=FontWeight.Bold);Text(game.label,fontSize=14.sp,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)};Icon(Icons.Default.ArrowDropDown,null,tint=accent)}};ExposedDropdownMenu(gameMenu,{gameMenu=false}){boxV2GameGroups.forEach{o->DropdownMenuItem({Text(o.label,fontWeight=FontWeight.SemiBold)},{game=o;region=o.regions.first();gameMenu=false})}}}
   ExposedDropdownMenuBox(regionMenu,{regionMenu=!regionMenu},Modifier.weight(.84f)){OutlinedTextField(region.label,{},Modifier.menuAnchor().fillMaxWidth().height(58.dp),readOnly=true,singleLine=true,label={Text(if(game.regions.size>1)"DLC / região" else "Região",fontSize=10.sp)},textStyle=LocalTextStyle.current.copy(fontSize=14.sp,fontWeight=FontWeight.Bold),trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(regionMenu)},shape=RoundedCornerShape(15.dp));ExposedDropdownMenu(regionMenu,{regionMenu=false}){game.regions.forEach{o->DropdownMenuItem({Column{Text(o.label,fontWeight=FontWeight.SemiBold);Text(o.badge,fontSize=10.sp,color=BoxMuted)}},{region=o;regionMenu=false})}}}
  }
  Card(Modifier.fillMaxWidth().padding(top=6.dp),RoundedCornerShape(22.dp),CardDefaults.cardColors(containerColor=BoxSurface)){Row(Modifier.fillMaxWidth().height(70.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(51.dp),contentAlignment=Alignment.Center){CircularProgressIndicator({progress},Modifier.fillMaxSize(),color=accent,trackColor=BoxSurfaceStrong,strokeWidth=5.dp);Text("${(progress*100).toInt()}%",fontSize=11.sp,fontWeight=FontWeight.Black)};VerticalDivider(Modifier.padding(start=12.dp).height(40.dp),color=BoxSurfaceStrong);BoxV2Stat(Icons.Default.CatchingPokemon,"${dex.size}","Total",Modifier.weight(1f),accent2);VerticalDivider(Modifier.height(40.dp),color=BoxSurfaceStrong);BoxV2Stat(null,"$caught","Capturados",Modifier.weight(1f),BoxMint);VerticalDivider(Modifier.height(40.dp),color=BoxSurfaceStrong);BoxV2Stat(null,"$left","Restantes",Modifier.weight(1f),BoxMuted);Surface(shape=RoundedCornerShape(10.dp),color=Color.White.copy(alpha=.65f),modifier=Modifier.width(58.dp).height(46.dp)){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Default.Landscape,null,tint=accent,modifier=Modifier.size(13.dp));Text(region.label,fontSize=8.sp,fontWeight=FontWeight.Bold,maxLines=1);Text(if(region.badge.startsWith("DLC"))"DLC" else "Base",fontSize=7.sp,color=BoxMuted)}}}}
  Row(Modifier.fillMaxWidth().height(50.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){FilledTonalIconButton({page=p-1},Modifier.size(37.dp),enabled=p>0){Icon(Icons.Default.ChevronLeft,"Anterior")};Surface(shape=RoundedCornerShape(13.dp),color=BoxSurface,modifier=Modifier.height(40.dp)){Row(Modifier.fillMaxHeight().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Inventory2,null,tint=accent,modifier=Modifier.size(21.dp));Spacer(Modifier.width(8.dp));Text("Box ${p+1}",fontSize=14.sp,fontWeight=FontWeight.Black)}};FilledTonalIconButton({page=p+1},Modifier.size(37.dp),enabled=p<pages-1){Icon(Icons.Default.ChevronRight,"Próxima")}}
  Box(Modifier.weight(1f).fillMaxWidth()){when{loading->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=accent)};dex.isEmpty()->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Não foi possível carregar esta Pokédex regional.")};else->AnimatedContent(p,{fadeIn(tween(180)) togetherWith fadeOut(tween(140))},label="box",modifier=Modifier.fillMaxSize()){pg->BoxV2Grid(dex.drop(pg*30).take(30),captured,{pk->onPokemonClick(pk.nationalId,region.source)},{pk->quickPokemon=pk})}}}
  Row(Modifier.fillMaxWidth().height(55.dp).padding(top=4.dp,bottom=3.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilledTonalButton({allBoxesOpen=true},Modifier.weight(1f).fillMaxHeight(),shape=RoundedCornerShape(18.dp)){Icon(Icons.Default.GridView,null);Spacer(Modifier.width(8.dp));Text("Todas as Boxes",fontSize=12.sp,fontWeight=FontWeight.Bold)};FilledTonalButton({searchOpen=true},Modifier.weight(1f).fillMaxHeight(),shape=RoundedCornerShape(18.dp)){Icon(Icons.Default.Search,null);Spacer(Modifier.width(8.dp));Text("Pesquisar",fontSize=12.sp,fontWeight=FontWeight.Bold)}}
 }
 if(allBoxesOpen)ModalBottomSheet({allBoxesOpen=false},containerColor=BoxBg){Column(Modifier.fillMaxWidth().padding(20.dp,8.dp)){Text("Todas as Boxes",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("${game.label} · ${region.label}",color=BoxMuted);LazyColumn(Modifier.heightIn(max=520.dp)){items((0 until pages).toList()){pg->val es=dex.drop(pg*30).take(30);val n=es.count{it.nationalId in captured};Card(Modifier.fillMaxWidth().padding(vertical=3.dp).combinedClickable(onClick={page=pg;allBoxesOpen=false})){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("Box ${pg+1}",fontWeight=FontWeight.Bold);Text("$n/${es.size} capturados")}}}}}}
 if(searchOpen)BoxV2Search(dex,captured,{searchOpen=false},{pk->val i=dex.indexOfFirst{it.nationalId==pk.nationalId};if(i>=0)page=i/30;searchOpen=false},{pk->searchOpen=false;onPokemonClick(pk.nationalId,region.source)})
 quickPokemon?.let{pk->QuickCaptureDialog(pk,pk.nationalId in captured,{quickPokemon=null}){CollectionStore.toggleCaptured(pk.nationalId);quickPokemon=null}}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun BoxV2Grid(e:List<GameDexService.GameDexEntry>,c:Set<Int>,open:(GameDexService.GameDexEntry)->Unit,longPress:(GameDexService.GameDexEntry)->Unit){Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(4.dp)){repeat(5){r->Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){repeat(6){col->val i=r*6+col;Box(Modifier.weight(1f).fillMaxHeight()){e.getOrNull(i)?.let{pk->BoxV2Slot(pk,pk.nationalId in c,{open(pk)},{longPress(pk)})}?:Surface(Modifier.fillMaxSize(),RoundedCornerShape(11.dp),color=BoxSurface){}}}}}}}
@Composable private fun BoxV2Stat(icon:ImageVector?,v:String,l:String,m:Modifier,t:Color){Column(m,horizontalAlignment=Alignment.CenterHorizontally){if(icon!=null)Icon(icon,null,tint=t,modifier=Modifier.size(13.dp))else Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(t));Text(v,fontWeight=FontWeight.Black,fontSize=12.sp);Text(l,fontSize=7.sp,color=BoxMuted,maxLines=1)}}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun BoxV2Slot(pk:GameDexService.GameDexEntry,c:Boolean,open:()->Unit,longPress:()->Unit){val haptic=LocalHapticFeedback.current;Surface(Modifier.fillMaxSize().combinedClickable(onClick=open,onLongClick={haptic.performHapticFeedback(HapticFeedbackType.LongPress);longPress()}),RoundedCornerShape(11.dp),color=if(c)Color(0xFFEAE6FA)else BoxSurface){Column(Modifier.fillMaxSize().padding(2.dp),horizontalAlignment=Alignment.CenterHorizontally){AsyncImage(pk.spriteUrl,pk.name,Modifier.weight(1f).fillMaxWidth(.94f).alpha(if(c)1f else .24f),colorFilter=if(c)null else ColorFilter.colorMatrix(ColorMatrix().apply{setToSaturation(0f)}));Text(formatPokemonName(pk.name),fontSize=8.sp,lineHeight=8.sp,maxLines=1,overflow=TextOverflow.Ellipsis,color=if(c)BoxInk else BoxMuted);Text("#${pk.gameNumber.toString().padStart(3,'0')}",fontSize=8.sp,color=BoxMuted)}}}

@Composable private fun QuickCaptureDialog(pk:GameDexService.GameDexEntry,captured:Boolean,dismiss:()->Unit,confirm:()->Unit){AlertDialog(onDismissRequest=dismiss,icon={AsyncImage(pk.spriteUrl,pk.name,Modifier.size(92.dp))},title={Text(formatPokemonName(pk.name),fontWeight=FontWeight.Bold)},text={Text(if(captured)"Este Pokémon já está na sua coleção. Deseja remover?" else "Adicionar este Pokémon à sua coleção?",textAlign=androidx.compose.ui.text.style.TextAlign.Center)},confirmButton={Button(confirm){Icon(if(captured)Icons.Default.RemoveCircleOutline else Icons.Default.AddCircle,null);Spacer(Modifier.width(6.dp));Text(if(captured)"Remover" else "Adicionar")}},dismissButton={TextButton(dismiss){Text("Cancelar")}})}
private fun formatPokemonName(n:String)=n.split("-"," ").joinToString(" "){it.replaceFirstChar{c->if(c.isLowerCase())c.titlecase()else c.toString()}}
@Composable private fun BoxV2Search(d:List<GameDexService.GameDexEntry>,c:Set<Int>,dismiss:()->Unit,select:(GameDexService.GameDexEntry)->Unit,open:(GameDexService.GameDexEntry)->Unit){var q by remember{mutableStateOf("")};val x=q.trim().removePrefix("#");val results=remember(d,q){if(x.isBlank())emptyList()else d.filter{it.name.contains(x,true)||it.gameNumber.toString()==x||it.nationalId.toString()==x}.take(12)};AlertDialog(dismiss,title={Text("Pesquisar Pokémon")},text={Column{OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Nome ou número")});LazyColumn(Modifier.heightIn(max=360.dp)){items(results,key={it.nationalId}){pk->Row(Modifier.fillMaxWidth().combinedClickable(onClick={select(pk)}).padding(7.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(pk.spriteUrl,pk.name,Modifier.size(48.dp));Column(Modifier.weight(1f)){Text(formatPokemonName(pk.name),fontWeight=FontWeight.SemiBold);Text("#${pk.gameNumber.toString().padStart(3,'0')} · ${if(pk.nationalId in c)"Capturado" else "Faltando"}")};TextButton({open(pk)}){Text("Ficha")}}}}}},confirmButton={},dismissButton={TextButton(dismiss){Text("Fechar")}})}
