package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.AppStatePreferences
import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import com.otaviobarreto.pokedex.data.PokedexDataStore
import com.otaviobarreto.pokedex.data.PokemonFormsService
import com.otaviobarreto.pokedex.data.PokemonFormVariant
import com.otaviobarreto.pokedex.data.VariantCollectionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val QBbg=Color(0xFFFBF8FF);private val QBsurface=Color(0xFFF1EEFA);private val QBink=Color(0xFF171522);private val QBmuted=Color(0xFF777286);private val QBmint=Color(0xFF67D7CB)
private data class QBRegion(val label:String,val source:String,val badge:String)
private data class QBGame(val label:String,val accent:Color,val regions:List<QBRegion>)
private fun qbAccent(game:String):Color=when(game){
 "Scarlet / Violet"->Color(0xFFB54C5D)
 "Sword / Shield"->Color(0xFF35A9C7)
 "Let's Go Pikachu / Eevee"->Color(0xFFE0A929)
 "Legends Arceus"->Color(0xFF527F7C)
 "Brilliant Diamond / Shining Pearl"->Color(0xFF5968C7)
 "Pokémon Legends: Z-A"->Color(0xFF2D7F8E)
 "FireRed / LeafGreen"->Color(0xFFCC5B43)
 "Pokémon Champions"->Color(0xFF7857D8)
 else->Color(0xFF5B55E7)
}
private val qbGames=AppGameCatalog.games.map{game->
 QBGame(game.label,qbAccent(game.label),game.regions.map{QBRegion(it.label,it.source,it.subtitle)})
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BoxesV2Screen(onPokemonClick:(Int,String?)->Unit){
 val preferredGame=AppStatePreferences.activeGame.takeIf{g->qbGames.any{it.label==g}} ?: qbGames.first().label
 var gameLabel by rememberSaveable{mutableStateOf(preferredGame)}
 val game=remember(gameLabel){qbGames.firstOrNull{it.label==gameLabel}?:qbGames.first()}
 val preferredRegion=AppStatePreferences.activeRegionForGame(game.label)
 var regionSource by rememberSaveable{mutableStateOf(game.regions.firstOrNull{it.source==preferredRegion}?.source ?: game.regions.first().source)}
 val region=remember(game.label,regionSource){game.regions.firstOrNull{it.source==regionSource}?:game.regions.first()}
 var dex by remember{mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())};var loading by remember{mutableStateOf(true)};var page by rememberSaveable{mutableIntStateOf(AppStatePreferences.boxPage(regionSource))};var gameMenu by remember{mutableStateOf(false)};var regionMenu by remember{mutableStateOf(false)};var search by remember{mutableStateOf(false)};var allBoxes by remember{mutableStateOf(false)};var captureTarget by remember{mutableStateOf<GameDexService.GameDexEntry?>(null)}
 LaunchedEffect(region.source,game.label){
  loading=true
  AppStatePreferences.activeGame=game.label
  AppStatePreferences.setActiveRegionForGame(game.label,region.source)
  page=AppStatePreferences.boxPage(region.source)
  val ctx=GameContext.fromSource(region.source)
  dex=if(ctx==null)emptyList()else runCatching{withContext(Dispatchers.IO){GameDexService.loadGameDex(ctx)}}.getOrElse{emptyList()}
  val pageCount=((dex.size+29)/30).coerceAtLeast(1)
  if(page>=pageCount) page=pageCount-1
  AppStatePreferences.setBoxPage(region.source,page)
  loading=false
 }
 val pages=((dex.size+29)/30).coerceAtLeast(1);val current=page.coerceIn(0,pages-1)
 LaunchedEffect(region.source,current){AppStatePreferences.setBoxPage(region.source,current)}
 val entries=dex.drop(current*30).take(30);val missingDetails=entries.filter{PokedexDataStore.cachedPokemon(it.nationalId)==null||PokedexDataStore.cachedSpecies(it.nationalId)==null};LaunchedEffect(entries){missingDetails.take(18).forEach{PokedexDataStore.prefetchDetails(it.nationalId)};delay(160);missingDetails.drop(18).forEach{PokedexDataStore.prefetchDetails(it.nationalId)}};val capturedIds=CollectionStore.contextualCapturedIds[region.source].orEmpty();val caught=dex.count{it.nationalId in capturedIds};val progress=if(dex.isEmpty())0f else caught.toFloat()/dex.size
 Column(Modifier.fillMaxSize().background(QBbg).padding(horizontal=6.dp)){
  Row(
   Modifier.fillMaxWidth().padding(top=4.dp,bottom=3.dp),
   horizontalArrangement=Arrangement.spacedBy(5.dp)
  ){
   ExposedDropdownMenuBox(gameMenu,{gameMenu=!gameMenu},Modifier.weight(1.18f)){
    OutlinedTextField(
     game.label,{},Modifier.menuAnchor().fillMaxWidth().heightIn(min=42.dp),
     readOnly=true,singleLine=true,label={Text("Jogo",fontSize=9.sp)},
     textStyle=MaterialTheme.typography.bodySmall,
     trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(gameMenu)},
     shape=RoundedCornerShape(12.dp)
    )
    ExposedDropdownMenu(gameMenu,{gameMenu=false}){
     qbGames.forEach{g->
      DropdownMenuItem(
       text={Text(g.label,fontWeight=FontWeight.SemiBold)},
       onClick={gameLabel=g.label;AppStatePreferences.activeGame=g.label;regionSource=g.regions.firstOrNull{it.source==AppStatePreferences.activeRegionForGame(g.label)}?.source?:g.regions.first().source;page=AppStatePreferences.boxPage(regionSource);AppStatePreferences.setActiveRegionForGame(g.label,regionSource);gameMenu=false}
      )
     }
    }
   }
   ExposedDropdownMenuBox(regionMenu,{regionMenu=!regionMenu},Modifier.weight(.82f)){
    OutlinedTextField(
     region.label,{},Modifier.menuAnchor().fillMaxWidth().heightIn(min=42.dp),
     readOnly=true,singleLine=true,
     label={Text(if(game.regions.size>1)"Região / DLC" else "Região",fontSize=9.sp)},
     textStyle=MaterialTheme.typography.bodySmall,
     trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(regionMenu)},
     shape=RoundedCornerShape(12.dp)
    )
    ExposedDropdownMenu(regionMenu,{regionMenu=false}){
     game.regions.forEach{r->
      DropdownMenuItem(
       text={Column{
        Text(r.label,fontWeight=FontWeight.SemiBold)
        if(r.badge.isNotBlank())Text(r.badge,fontSize=10.sp,color=QBmuted)
       }},
       onClick={regionSource=r.source;AppStatePreferences.setActiveRegionForGame(game.label,r.source);page=AppStatePreferences.boxPage(r.source);regionMenu=false}
      )
     }
    }
   }
  }
  Row(
   Modifier.fillMaxWidth().height(54.dp).padding(horizontal=4.dp),
   verticalAlignment=Alignment.CenterVertically
  ){
   Column(Modifier.weight(1f)){
    Text(
     "Box "+(current+1)+" / "+pages,
     fontSize=16.sp,
     lineHeight=17.sp,
     fontWeight=FontWeight.Black,
     color=QBink
    )
    Text(
     "Deslize para navegar entre as Boxes",
     fontSize=8.5.sp,
     color=QBmuted
    )
   }
   Box(Modifier.size(48.dp),contentAlignment=Alignment.Center){
    CircularProgressIndicator(
     progress={progress.coerceIn(0f,1f)},
     modifier=Modifier.fillMaxSize(),
     strokeWidth=4.dp,
     color=game.accent,
     trackColor=game.accent.copy(alpha=.12f)
    )
    Column(
     horizontalAlignment=Alignment.CenterHorizontally,
     verticalArrangement=Arrangement.Center
    ){
     Text(
      ((progress*100).toInt()).toString()+"%",
      fontSize=9.sp,
      lineHeight=10.sp,
      fontWeight=FontWeight.Black,
      color=game.accent
     )
     Spacer(Modifier.height(1.dp))
     Text(
      caught.toString()+"/"+dex.size,
      fontSize=7.sp,
      lineHeight=8.sp,
      color=QBmuted
     )
    }
   }
  }
  var dragTotal by remember { mutableFloatStateOf(0f) }
  Box(
   Modifier
    .weight(1f)
    .fillMaxWidth()
    .pointerInput(current,pages,loading){
     detectHorizontalDragGestures(
      onDragStart={dragTotal=0f},
      onHorizontalDrag={change,dragAmount->
       change.consume()
       dragTotal+=dragAmount
      },
      onDragEnd={
       val threshold=90f
       if(!loading){
        if(dragTotal < -threshold && current < pages-1) page=current+1
        else if(dragTotal > threshold && current > 0) page=current-1
       }
       dragTotal=0f
      },
      onDragCancel={dragTotal=0f}
     )
    }
  ){
   when{
    loading->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=game.accent)}
    dex.isEmpty()->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Não foi possível carregar esta Pokédex regional.")}
    else->QBGrid(entries,capturedIds,region.source,{pk->onPokemonClick(pk.nationalId,region.source)},{pk->captureTarget=pk})
   }
  }
  Row(
   Modifier.fillMaxWidth().height(40.dp).padding(bottom=1.dp),
   horizontalArrangement=Arrangement.spacedBy(4.dp)
  ){
   FilledTonalButton(
    {search=true},
    Modifier.weight(1f).fillMaxHeight(),
    shape=RoundedCornerShape(13.dp)
   ){
    Icon(Icons.Default.Search,null,Modifier.size(17.dp))
    Spacer(Modifier.width(5.dp))
    Text("Pesquisar",fontWeight=FontWeight.Bold,fontSize=12.sp)
   }
   FilledTonalButton(
    {allBoxes=true},
    Modifier.weight(1f).fillMaxHeight(),
    shape=RoundedCornerShape(13.dp)
   ){
    Icon(Icons.Default.GridView,null,Modifier.size(17.dp))
    Spacer(Modifier.width(5.dp))
    Text("Todas as Boxes",fontWeight=FontWeight.Bold,fontSize=12.sp)
   }
  }
 }
 if(search)QBSearch(dex,capturedIds,region.source,{search=false},{pk->val i=dex.indexOfFirst{it.nationalId==pk.nationalId};if(i>=0)page=i/30;search=false},{pk->search=false;onPokemonClick(pk.nationalId,region.source)})
 if(allBoxes)QBAllBoxes(
  dex=dex,
  current=current,
  captured=capturedIds,
  accent=game.accent,
  dismiss={allBoxes=false},
  select={targetPage->page=targetPage;allBoxes=false}
 )
 captureTarget?.let{pk->
  QBVariantManager(
   pk=pk,
   source=region.source,
   dismiss={captureTarget=null}
  )
 }
}

@Composable
private fun QBGrid(
    entries:List<GameDexService.GameDexEntry>,
    captured:Set<Int>,
    source:String,
    open:(GameDexService.GameDexEntry)->Unit,
    hold:(GameDexService.GameDexEntry)->Unit
){
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(2.dp)){
        repeat(5){row->
            Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)){
                repeat(6){col->
                    val pk=entries.getOrNull(row*6+col)
                    if(pk==null){
                        Surface(Modifier.weight(1f).fillMaxHeight(),RoundedCornerShape(9.dp),color=QBsurface){}
                    }else{
                        QBSlot(
                            pk=pk,
                            captured=pk.nationalId in captured,
                            source=source,
                            open={open(pk)},
                            hold={hold(pk)},
                            modifier=Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QBSlot(
    pk:GameDexService.GameDexEntry,
    captured:Boolean,
    source:String,
    open:()->Unit,
    hold:()->Unit,
    modifier:Modifier=Modifier
){
    val ownedVariants=VariantCollectionStore.ownedVariants
    val variant=remember(source,pk.nationalId,ownedVariants){
        VariantCollectionStore.preferred(source,pk.nationalId)
    }
    val imageModel=variant?.artworkUrl ?: pk.spriteUrl
    val isShiny=variant?.shiny==true
    Surface(
        modifier.combinedClickable(onClick=open,onLongClick=hold),
        shape=RoundedCornerShape(9.dp),
        color=if(captured)Color(0xFFEAE6FA)else QBsurface
    ){
        Box(Modifier.fillMaxSize()){
            AsyncImage(
                model=imageModel,
                contentDescription=pk.name,
                modifier=Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.78f)
                    .align(Alignment.TopCenter)
                    .padding(horizontal=2.dp,vertical=2.dp)
                    .alpha(if(captured)1f else .22f),
                contentScale=ContentScale.Fit,
                colorFilter=if(captured)null else ColorFilter.colorMatrix(ColorMatrix().apply{setToSaturation(0f)})
            )
            Surface(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color=Color.White.copy(alpha=.90f)
            ){
                Column(
                    Modifier.padding(vertical=2.dp,horizontal=1.dp),
                    horizontalAlignment=Alignment.CenterHorizontally
                ){
                    Text(
                        pretty(pk.name),
                        fontSize=7.5.sp,
                        lineHeight=8.sp,
                        maxLines=1,
                        overflow=TextOverflow.Ellipsis,
                        color=if(captured)QBink else QBmuted
                    )
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(2.dp)){
                        Text(
                            "#"+pk.gameNumber.toString().padStart(3,'0'),
                            fontSize=7.sp,
                            lineHeight=7.sp,
                            color=QBmuted
                        )
                        if(isShiny){
                            Text("★",fontSize=7.sp,lineHeight=7.sp,color=Color(0xFFB78900),fontWeight=FontWeight.Black)
                        }
                        if(variant!=null && variant.formPokemonId!=pk.nationalId){
                            Text("F",fontSize=6.sp,lineHeight=7.sp,color=Color(0xFF5B55E7),fontWeight=FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QBVariantManager(
    pk:GameDexService.GameDexEntry,
    source:String,
    dismiss:()->Unit
){
    val variantsState=VariantCollectionStore.ownedVariants
    var forms by remember(pk.nationalId){mutableStateOf<List<PokemonFormVariant>?>(PokemonFormsService.cached(pk.nationalId))}
    var loading by remember(pk.nationalId){mutableStateOf(forms==null)}
    LaunchedEffect(pk.nationalId){
        if(forms==null){
            loading=true
            forms=runCatching{
                withContext(Dispatchers.IO){PokemonFormsService.collectible(pk.nationalId)}
            }.getOrElse{
                listOf(PokemonFormVariant(pretty(pk.name),pk.nationalId,true))
            }
            loading=false
        }
    }
    val available=(forms.orEmpty().ifEmpty{
        listOf(PokemonFormVariant(pretty(pk.name),pk.nationalId,true))
    }).filter{it.pokemonId!=null}

    Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Surface(
            Modifier
                .fillMaxWidth(.92f)
                .widthIn(max=560.dp)
                .heightIn(max=620.dp),
            shape=RoundedCornerShape(24.dp),
            color=QBbg
        ){
            Column(Modifier.fillMaxWidth().padding(16.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(pretty(pk.name),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
                        Text("Formas e Shiny",fontSize=11.sp,color=QBmuted)
                    }
                    IconButton(dismiss){Icon(Icons.Default.Close,"Fechar")}
                }
                Text(
                    "Você pode registrar mais de uma forma e manter Normal + Shiny ao mesmo tempo.",
                    style=MaterialTheme.typography.bodySmall,
                    color=QBmuted,
                    modifier=Modifier.padding(bottom=8.dp)
                )
                val isCaptured=CollectionStore.isCapturedIn(source,pk.nationalId)
                if(isCaptured){
                    OutlinedButton(
                        onClick={
                            VariantCollectionStore.removeAll(source,pk.nationalId)
                            dismiss()
                        },
                        modifier=Modifier.fillMaxWidth(),
                        shape=RoundedCornerShape(14.dp)
                    ){
                        Icon(Icons.Default.DeleteOutline,null,Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remover Pokémon da Box")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if(loading){
                    Box(
                        Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment=Alignment.Center
                    ){CircularProgressIndicator()}
                }else{
                    LazyColumn(
                        modifier=Modifier.weight(1f,fill=false),
                        verticalArrangement=Arrangement.spacedBy(8.dp)
                    ){
                        items(available,key={it.pokemonId!!}){form->
                            val formId=form.pokemonId!!
                            val normalOwned=VariantCollectionStore.isOwned(source,pk.nationalId,formId,form.name,false)
                            val shinyOwned=VariantCollectionStore.isOwned(source,pk.nationalId,formId,form.name,true)
                            Card(shape=RoundedCornerShape(18.dp)){
                                Row(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment=Alignment.CenterVertically
                                ){
                                    AsyncImage(
                                        model="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+formId+".png",
                                        contentDescription=form.name,
                                        modifier=Modifier.size(58.dp),
                                        contentScale=ContentScale.Fit
                                    )
                                    Column(Modifier.weight(1f).padding(start=8.dp)){
                                        Text(
                                            when{
                                                form.isDefault -> form.name+" · padrão"
                                                !form.countsForLivingDex -> form.name+" · temporária"
                                                else -> form.name
                                            },
                                            fontWeight=FontWeight.Bold,
                                            maxLines=1,
                                            overflow=TextOverflow.Ellipsis
                                        )
                                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                            FilterChip(
                                                selected=normalOwned,
                                                onClick={
                                                    VariantCollectionStore.toggle(
                                                        source,pk.nationalId,formId,form.name,false
                                                    )
                                                },
                                                label={Text("Normal",fontSize=10.sp)},
                                                leadingIcon=if(normalOwned){{Icon(Icons.Default.Check,null,Modifier.size(14.dp))}}else null
                                            )
                                            FilterChip(
                                                selected=shinyOwned,
                                                onClick={
                                                    VariantCollectionStore.toggle(
                                                        source,pk.nationalId,formId,form.name,true
                                                    )
                                                },
                                                label={Text("★ Shiny",fontSize=10.sp)},
                                                leadingIcon=if(shinyOwned){{Icon(Icons.Default.AutoAwesome,null,Modifier.size(14.dp))}}else null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item{
                            val any=variantsState.any{it.source==source && it.speciesId==pk.nationalId}
                            if(!any && CollectionStore.isCapturedIn(source,pk.nationalId)){
                                Text(
                                    "Este Pokémon já estava marcado como capturado em uma versão antiga. Selecione Normal ou Shiny para migrá-lo ao novo sistema de variantes.",
                                    style=MaterialTheme.typography.bodySmall,
                                    color=QBmuted,
                                    modifier=Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QBAllBoxes(
    dex:List<GameDexService.GameDexEntry>,
    current:Int,
    captured:Set<Int>,
    accent:Color,
    dismiss:()->Unit,
    select:(Int)->Unit
){
    val pages=((dex.size+29)/30).coerceAtLeast(1)
    Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Surface(
            Modifier.fillMaxWidth(.96f).fillMaxHeight(.90f),
            shape=RoundedCornerShape(24.dp),
            color=QBbg
        ){
            Column(Modifier.fillMaxSize().padding(16.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("Todas as Boxes",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,color=QBink)
                        Text("Toque em uma Box para abrir",fontSize=11.sp,color=QBmuted)
                    }
                    IconButton(dismiss){Icon(Icons.Default.Close,"Fechar")}
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
                    items((0 until pages).toList(),key={it}){index->
                        val entries=dex.drop(index*30).take(30)
                        val total=entries.size.coerceAtLeast(1)
                        val owned=entries.count{it.nationalId in captured}
                        val pct=owned.toFloat()/total
                        Card(
                            Modifier.fillMaxWidth().clickable{select(index)},
                            shape=RoundedCornerShape(18.dp),
                            colors=CardDefaults.cardColors(
                                containerColor=if(index==current)accent.copy(alpha=.10f) else Color.White
                            )
                        ){
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=12.dp),
                                verticalAlignment=Alignment.CenterVertically
                            ){
                                Box(Modifier.size(46.dp),contentAlignment=Alignment.Center){
                                    CircularProgressIndicator(
                                        progress={pct.coerceIn(0f,1f)},
                                        modifier=Modifier.fillMaxSize(),
                                        strokeWidth=4.dp,
                                        color=accent,
                                        trackColor=accent.copy(alpha=.12f)
                                    )
                                    Text((pct*100).toInt().toString()+"%",fontSize=8.sp,fontWeight=FontWeight.Black,color=accent)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)){
                                    Text("Box "+(index+1),fontWeight=FontWeight.Bold,fontSize=16.sp,color=QBink)
                                    Text(owned.toString()+" / "+entries.size+" capturados",fontSize=11.sp,color=QBmuted)
                                }
                                if(index==current)AssistChip(onClick={},label={Text("Atual",fontSize=10.sp)})
                                else Icon(Icons.Default.ArrowForwardIos,null,tint=QBmuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QBSearch(
    dex:List<GameDexService.GameDexEntry>,
    captured:Set<Int>,
    source:String,
    dismiss:()->Unit,
    select:(GameDexService.GameDexEntry)->Unit,
    open:(GameDexService.GameDexEntry)->Unit
){
    var q by remember{mutableStateOf("")}
    var status by remember{mutableStateOf("Todos")}
    var order by remember{mutableStateOf("Regional")}
    val key=q.trim().removePrefix("#")
    val results=remember(key,status,order,dex,captured){
        dex.asSequence()
            .filter{
                key.isBlank() || it.name.contains(key,true) ||
                    it.gameNumber.toString()==key || it.nationalId.toString()==key
            }
            .filter{
                when(status){
                    "Capturados" -> it.nationalId in captured
                    "Faltando" -> it.nationalId !in captured
                    "Shiny" -> VariantCollectionStore.ownedVariants.any{v->v.source==source && v.speciesId==it.nationalId && v.shiny}
                    "Ambos" -> {
                        val variants=VariantCollectionStore.ownedVariants.filter{v->v.source==source && v.speciesId==it.nationalId}
                        variants.any{!it.shiny} && variants.any{it.shiny}
                    }
                    "Com formas" -> VariantCollectionStore.ownedVariants.any{v->v.source==source && v.speciesId==it.nationalId && v.formPokemonId!=it.nationalId}
                    else -> true
                }
            }
            .let{seq->
                when(order){
                    "Nome" -> seq.sortedBy{it.name.lowercase()}
                    "Nacional" -> seq.sortedBy{it.nationalId}
                    else -> seq.sortedBy{it.gameNumber}
                }
            }
            .take(30).toList()
    }

    AlertDialog(
        onDismissRequest=dismiss,
        title={Text("Pesquisar Pokémon")},
        text={Column{
            OutlinedTextField(
                q,{q=it},Modifier.fillMaxWidth(),singleLine=true,
                leadingIcon={Icon(Icons.Default.Search,null)},
                placeholder={Text("Nome ou número")}
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                listOf("Todos","Capturados","Faltando","Shiny","Ambos","Com formas").forEach{option->
                    FilterChip(selected=status==option,onClick={status=option},label={Text(option,fontSize=10.sp)})
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                listOf("Regional","Nacional","Nome").forEach{option->
                    FilterChip(selected=order==option,onClick={order=option},label={Text(option,fontSize=10.sp)})
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(results.size.toString()+" resultado(s)",fontSize=10.sp,color=QBmuted)
            LazyColumn(Modifier.heightIn(max=360.dp)){
                items(results,key={it.nationalId}){pk->
                    Row(
                        Modifier.fillMaxWidth().clickable{select(pk)}.padding(vertical=5.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        AsyncImage(pk.spriteUrl,pk.name,Modifier.size(48.dp))
                        Column(Modifier.weight(1f)){
                            Text(pretty(pk.name),fontWeight=FontWeight.SemiBold)
                            Text(
                                (if(pk.nationalId in captured)"Capturado" else "Faltando")+
                                    " · Regional #"+pk.gameNumber+" · Nacional #"+pk.nationalId,
                                fontSize=10.sp,color=QBmuted
                            )
                        }
                        TextButton({open(pk)}){Text("Ficha")}
                    }
                }
            }
        }},
        confirmButton={},
        dismissButton={TextButton(dismiss){Text("Fechar")}}
    )
}

private fun pretty(name:String)=name.split("-"," ").joinToString(" "){
    it.replaceFirstChar{c->if(c.isLowerCase())c.titlecase()else c.toString()}
}
