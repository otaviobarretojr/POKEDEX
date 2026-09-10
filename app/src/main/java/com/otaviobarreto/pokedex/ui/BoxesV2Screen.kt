package com.otaviobarreto.pokedex.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BoxBg = Color(0xFFFBF8FF)
private val BoxSurface = Color(0xFFF1EEFA)
private val BoxSurfaceStrong = Color(0xFFE9E4F5)
private val BoxAccent = Color(0xFF7563C9)
private val BoxMint = Color(0xFF67D7CB)
private val BoxInk = Color(0xFF171522)
private val BoxMuted = Color(0xFF777286)

private data class BoxV2GameGroup(val label:String,val regions:List<BoxV2Region>)
private data class BoxV2Region(val label:String,val source:String,val badge:String)
private val boxV2GameGroups=listOf(
    BoxV2GameGroup("Scarlet / Violet",listOf(
        BoxV2Region("Paldea","Scarlet / Violet · Paldea","Jogo base"),
        BoxV2Region("Kitakami","Scarlet / Violet · Kitakami","DLC · The Teal Mask"),
        BoxV2Region("Blueberry","Scarlet / Violet · Blueberry","DLC · The Indigo Disk")
    )),
    BoxV2GameGroup("Sword / Shield",listOf(BoxV2Region("Galar","Sword / Shield · Galar","Jogo base"))),
    BoxV2GameGroup("Let's Go Pikachu / Eevee",listOf(BoxV2Region("Kanto","Let's Go Pikachu / Eevee · Kanto","Jogo base"))),
    BoxV2GameGroup("Legends Arceus",listOf(BoxV2Region("Hisui","Legends Arceus · Hisui","Jogo base")))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxesV2Screen(onPokemonClick:(Int,String?)->Unit){
    var game by remember{mutableStateOf(boxV2GameGroups.first())}
    var region by remember{mutableStateOf(game.regions.first())}
    var dex by remember{mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var page by remember{mutableIntStateOf(0)}
    var gameMenu by remember{mutableStateOf(false)}
    var regionMenu by remember{mutableStateOf(false)}
    var allBoxesOpen by remember{mutableStateOf(false)}
    var searchOpen by remember{mutableStateOf(false)}
    val captured=CollectionStore.capturedIds

    LaunchedEffect(region.source){
        loading=true;page=0
        val context=GameContext.fromSource(region.source)
        dex=if(context==null) emptyList() else runCatching{withContext(Dispatchers.IO){GameDexService.loadGameDex(context)}}.getOrElse{emptyList()}
        loading=false
    }

    val pageCount=((dex.size+29)/30).coerceAtLeast(1)
    val safePage=page.coerceIn(0,pageCount-1)
    val pageEntries=dex.drop(safePage*30).take(30)
    val caught=dex.count{it.nationalId in captured}
    val remaining=(dex.size-caught).coerceAtLeast(0)
    val rawProgress=if(dex.isEmpty())0f else caught.toFloat()/dex.size
    val animatedProgress by animateFloatAsState(rawProgress,tween(500),label="dexProgress")
    val first=pageEntries.firstOrNull()?.gameNumber?:safePage*30+1
    val last=pageEntries.lastOrNull()?.gameNumber?:((safePage+1)*30).coerceAtMost(dex.size)

    Column(Modifier.fillMaxSize().background(BoxBg).padding(horizontal=12.dp)){
        Row(Modifier.fillMaxWidth().padding(top=5.dp,bottom=2.dp),verticalAlignment=Alignment.Top){
            Column(Modifier.weight(1f)){
                Text("POKEDEX",fontSize=27.sp,lineHeight=28.sp,fontWeight=FontWeight.Black,color=BoxInk)
                Text("C A T C H  E M  ·  T O D A S  A S  R E G I Õ E S",fontSize=7.sp,lineHeight=9.sp,color=BoxMuted,maxLines=1)
            }
            Surface(shape=RoundedCornerShape(16.dp),color=BoxSurfaceStrong,modifier=Modifier.padding(top=1.dp).clickable{gameMenu=true}){
                Row(Modifier.padding(horizontal=10.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Default.CatchingPokemon,null,tint=BoxAccent,modifier=Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(game.label,fontWeight=FontWeight.Bold,fontSize=12.sp,color=BoxInk)
                }
            }
        }

        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            ExposedDropdownMenuBox(expanded=gameMenu,onExpandedChange={gameMenu=!gameMenu},modifier=Modifier.weight(1f)){
                OutlinedTextField(value=game.label,onValueChange={},modifier=Modifier.menuAnchor().fillMaxWidth().height(58.dp),readOnly=true,singleLine=true,label={Text("Jogo",fontSize=11.sp)},textStyle=LocalTextStyle.current.copy(fontSize=15.sp,fontWeight=FontWeight.SemiBold),trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(gameMenu)},shape=RoundedCornerShape(10.dp))
                ExposedDropdownMenu(expanded=gameMenu,onDismissRequest={gameMenu=false}){boxV2GameGroups.forEach{o->DropdownMenuItem(text={Text(o.label)},onClick={game=o;region=o.regions.first();gameMenu=false})}}
            }
            ExposedDropdownMenuBox(expanded=regionMenu,onExpandedChange={regionMenu=!regionMenu},modifier=Modifier.weight(1f)){
                OutlinedTextField(value=region.label,onValueChange={},modifier=Modifier.menuAnchor().fillMaxWidth().height(58.dp),readOnly=true,singleLine=true,label={Text(if(game.regions.size>1)"DLC / região" else "Região",fontSize=11.sp)},textStyle=LocalTextStyle.current.copy(fontSize=15.sp,fontWeight=FontWeight.SemiBold),trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(regionMenu)},shape=RoundedCornerShape(10.dp))
                ExposedDropdownMenu(expanded=regionMenu,onDismissRequest={regionMenu=false}){game.regions.forEach{o->DropdownMenuItem(text={Column{Text(o.label,fontWeight=FontWeight.SemiBold);Text(o.badge,fontSize=11.sp,color=BoxMuted)}},onClick={region=o;regionMenu=false})}}
            }
        }

        Card(Modifier.fillMaxWidth().padding(top=6.dp),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=BoxSurface)){
            Row(Modifier.fillMaxWidth().height(78.dp).padding(horizontal=11.dp),verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.size(54.dp),contentAlignment=Alignment.Center){
                    CircularProgressIndicator(progress={animatedProgress},modifier=Modifier.fillMaxSize(),strokeWidth=6.dp,color=BoxAccent,trackColor=BoxSurfaceStrong)
                    Text("${(animatedProgress*100).toInt()}%",fontSize=12.sp,fontWeight=FontWeight.Black,color=BoxInk)
                }
                Column(Modifier.weight(1.05f).padding(start=9.dp)){Text("$caught / ${dex.size}",fontSize=20.sp,fontWeight=FontWeight.Black,color=BoxInk);Text("capturados",fontSize=10.sp,color=BoxMuted)}
                VerticalDivider(Modifier.height(48.dp),color=BoxSurfaceStrong)
                BoxV2Stat(Icons.Default.CatchingPokemon,"${dex.size}","Total",Modifier.weight(.72f),BoxMuted)
                VerticalDivider(Modifier.height(48.dp),color=BoxSurfaceStrong)
                BoxV2Stat(null,"$caught","Capturados",Modifier.weight(.78f),BoxMint)
                VerticalDivider(Modifier.height(48.dp),color=BoxSurfaceStrong)
                BoxV2Stat(null,"$remaining","Restantes",Modifier.weight(.78f),BoxMuted)
                Surface(shape=RoundedCornerShape(12.dp),color=Color.White.copy(alpha=.72f),modifier=Modifier.width(74.dp)){
                    Column(Modifier.padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Place,null,tint=BoxAccent,modifier=Modifier.size(18.dp));Text(region.label,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(if(region.badge.startsWith("DLC"))"DLC" else "Jogo base",fontSize=8.sp,color=BoxMuted)}
                }
            }
        }

        Row(Modifier.fillMaxWidth().height(52.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            FilledTonalIconButton(onClick={page=safePage-1},enabled=safePage>0,modifier=Modifier.size(38.dp),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=BoxSurfaceStrong,contentColor=BoxInk)){Icon(Icons.Default.ChevronLeft,"Anterior")}
            Surface(shape=RoundedCornerShape(13.dp),color=BoxSurface,modifier=Modifier.height(42.dp)){
                Row(Modifier.padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Inventory2,null,tint=BoxAccent,modifier=Modifier.size(22.dp));Spacer(Modifier.width(7.dp));Column{Text("Box ${safePage+1}",fontSize=14.sp,fontWeight=FontWeight.Black,color=BoxInk);Text("Pokémon ${first.toString().padStart(3,'0')} – ${last.toString().padStart(3,'0')}",fontSize=9.sp,color=BoxMuted)}}
            }
            FilledTonalIconButton(onClick={page=safePage+1},enabled=safePage<pageCount-1,modifier=Modifier.size(38.dp),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=BoxSurfaceStrong,contentColor=BoxInk)){Icon(Icons.Default.ChevronRight,"Próxima")}
        }

        Box(Modifier.weight(1f).fillMaxWidth()){
            when{
                loading->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=BoxAccent)}
                dex.isEmpty()->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Não foi possível carregar esta Pokédex regional.")}
                else->AnimatedContent(targetState=safePage,transitionSpec={fadeIn(tween(180)) togetherWith fadeOut(tween(140))},label="boxPage",modifier=Modifier.fillMaxSize()){
                    BoxV2Grid(entries=dex.drop(it*30).take(30),captured=captured,onPokemonClick={p->onPokemonClick(p.nationalId,region.source)})
                }
            }
        }

        Row(Modifier.fillMaxWidth().height(55.dp).padding(top=4.dp,bottom=3.dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){
            FilledTonalButton(onClick={allBoxesOpen=true},modifier=Modifier.weight(1f).fillMaxHeight(),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.filledTonalButtonColors(containerColor=BoxSurfaceStrong,contentColor=BoxInk),contentPadding=PaddingValues(horizontal=9.dp)){
                Icon(Icons.Default.GridView,null,Modifier.size(20.dp));Spacer(Modifier.width(7.dp));Column{Text("Todas as Boxes",fontSize=11.sp,fontWeight=FontWeight.Bold);Text("Veja todas as $pageCount boxes",fontSize=8.sp,color=BoxMuted)}
            }
            FilledTonalButton(onClick={searchOpen=true},modifier=Modifier.weight(1f).fillMaxHeight(),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.filledTonalButtonColors(containerColor=BoxSurfaceStrong,contentColor=BoxInk),contentPadding=PaddingValues(horizontal=9.dp)){
                Icon(Icons.Default.Search,null,Modifier.size(21.dp));Spacer(Modifier.width(7.dp));Column{Text("Pesquisar",fontSize=11.sp,fontWeight=FontWeight.Bold);Text("Encontre um Pokémon",fontSize=8.sp,color=BoxMuted)}
            }
            Column(Modifier.width(75.dp),verticalArrangement=Arrangement.Center){Text("Box ${safePage+1} de $pageCount",fontSize=9.sp,color=BoxMuted);LinearProgressIndicator(progress={if(pageCount<=1)1f else (safePage+1).toFloat()/pageCount},modifier=Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(9.dp)),color=BoxAccent,trackColor=BoxSurfaceStrong)}
        }
    }

    if(allBoxesOpen)ModalBottomSheet(onDismissRequest={allBoxesOpen=false},containerColor=BoxBg){Column(Modifier.fillMaxWidth().padding(20.dp,8.dp)){Text("Todas as Boxes",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("${game.label} · ${region.label} · $pageCount Boxes",color=BoxMuted);LazyColumn(Modifier.fillMaxWidth().heightIn(max=520.dp)){items((0 until pageCount).toList()){p->val es=dex.drop(p*30).take(30);val c=es.count{it.nationalId in captured};Card(Modifier.fillMaxWidth().padding(vertical=3.dp).clickable{page=p;allBoxesOpen=false},colors=CardDefaults.cardColors(containerColor=BoxSurface)){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("Box ${p+1}",fontWeight=FontWeight.SemiBold);Text("$c/${es.size} capturados",color=BoxMuted)}}}}}}
    if(searchOpen)BoxV2Search(dex,captured,{searchOpen=false},{p->val i=dex.indexOfFirst{it.nationalId==p.nationalId};if(i>=0)page=i/30;searchOpen=false},{p->searchOpen=false;onPokemonClick(p.nationalId,region.source)})
}

@Composable
private fun BoxV2Grid(entries:List<GameDexService.GameDexEntry>,captured:Set<Int>,onPokemonClick:(GameDexService.GameDexEntry)->Unit){
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(4.dp)){
        repeat(5){row->
            Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                repeat(6){col->
                    val index=row*6+col
                    Box(Modifier.weight(1f).fillMaxHeight()){
                        entries.getOrNull(index)?.let{p->BoxV2Slot(p,p.nationalId in captured){onPokemonClick(p)}} ?: Surface(Modifier.fillMaxSize(),shape=RoundedCornerShape(11.dp),color=BoxSurface.copy(alpha=.45f)){}
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxV2Stat(icon:androidx.compose.ui.graphics.vector.ImageVector?,value:String,label:String,modifier:Modifier=Modifier,tint:Color){
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally){if(icon!=null)Icon(icon,null,tint=tint,modifier=Modifier.size(14.dp))else Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(tint));Text(value,fontWeight=FontWeight.Black,fontSize=13.sp,color=BoxInk);Text(label,fontSize=8.sp,color=BoxMuted,textAlign=TextAlign.Center,maxLines=1)}
}

@Composable
private fun BoxV2Slot(p:GameDexService.GameDexEntry,c:Boolean,onClick:()->Unit){
    Surface(modifier=Modifier.fillMaxSize().clickable(onClick=onClick),shape=RoundedCornerShape(11.dp),color=if(c) Color(0xFFEAE6FA) else BoxSurface){
        Column(Modifier.fillMaxSize().padding(horizontal=2.dp,vertical=2.dp),horizontalAlignment=Alignment.CenterHorizontally){
            AsyncImage(model=p.spriteUrl,contentDescription=p.name,modifier=Modifier.weight(1f).fillMaxWidth(.94f).alpha(if(c)1f else .24f),colorFilter=if(c)null else ColorFilter.colorMatrix(ColorMatrix().apply{setToSaturation(0f)}))
            Text(formatPokemonName(p.name),fontSize=8.sp,lineHeight=8.sp,color=if(c)BoxInk else BoxMuted,maxLines=1,overflow=TextOverflow.Ellipsis,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
            Text("#${p.gameNumber.toString().padStart(3,'0')}",fontSize=8.sp,lineHeight=9.sp,color=BoxMuted,textAlign=TextAlign.Center)
        }
    }
}

private fun formatPokemonName(name:String):String=name.split("-"," ").joinToString(" "){part->part.replaceFirstChar{if(it.isLowerCase())it.titlecase()else it.toString()}}

@Composable
private fun BoxV2Search(d:List<GameDexService.GameDexEntry>,c:Set<Int>,dismiss:()->Unit,select:(GameDexService.GameDexEntry)->Unit,open:(GameDexService.GameDexEntry)->Unit){
    var q by remember{mutableStateOf("")};val x=q.trim().removePrefix("#");val r=remember(d,q){if(x.isBlank())emptyList()else d.filter{it.name.contains(x,true)||it.gameNumber.toString()==x||it.nationalId.toString()==x}.take(12)}
    AlertDialog(onDismissRequest=dismiss,title={Text("Pesquisar Pokémon")},text={Column{OutlinedTextField(value=q,onValueChange={q=it},modifier=Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Nome ou número")});LazyColumn(Modifier.heightIn(max=360.dp)){items(r,key={it.nationalId}){p->Row(Modifier.fillMaxWidth().clickable{select(p)}.padding(7.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(model=p.spriteUrl,contentDescription=p.name,modifier=Modifier.size(48.dp));Column(Modifier.weight(1f)){Text(formatPokemonName(p.name),fontWeight=FontWeight.SemiBold);Text("#${p.gameNumber.toString().padStart(3,'0')} · ${if(p.nationalId in c)"Capturado" else "Faltando"}")};TextButton(onClick={open(p)}){Text("Ficha")}}}}}},confirmButton={},dismissButton={TextButton(onClick=dismiss){Text("Fechar")}})
}
