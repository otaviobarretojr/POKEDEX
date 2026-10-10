package com.otaviobarreto.pokedex.ui
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.otaviobarreto.pokedex.data.EvolutionFilterIndex
import com.otaviobarreto.pokedex.data.EvolutionRuleCatalog
import com.otaviobarreto.pokedex.data.PokedexDataStore
import com.otaviobarreto.pokedex.data.OfflineGamePackManager
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.PokemonRepository
import com.otaviobarreto.pokedex.data.PokemonFormsService
import com.otaviobarreto.pokedex.data.PokemonFormVariant
import com.otaviobarreto.pokedex.data.PokemonFormPresentation
import com.otaviobarreto.pokedex.data.VariantCollectionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
private data class QBRegion(val label:String,val source:String,val badge:String)
private data class QBGame(val label:String,val accent:Color,val regions:List<QBRegion>)
private fun qbAccent(game:String):Color=PokedexDesignTokens.Colors.game(game)
private val qbGames=AppGameCatalog.games.map{game->QBGame(game.label,qbAccent(game.label),game.regions.map{QBRegion(it.label,it.source,it.subtitle)})}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BoxesV2Screen(onPokemonClick:(Int,String?)->Unit){
 val preferredGame=AppStatePreferences.activeGame.takeIf{g->qbGames.any{it.label==g}} ?: qbGames.first().label
 var gameLabel by rememberSaveable{mutableStateOf(preferredGame)}
 val game=remember(gameLabel){qbGames.firstOrNull{it.label==gameLabel}?:qbGames.first()}
 val preferredRegion=AppStatePreferences.activeRegionForGame(game.label)
 var regionSource by rememberSaveable{mutableStateOf(game.regions.firstOrNull{it.source==preferredRegion}?.source ?: game.regions.first().source)}
 val region=remember(game.label,regionSource){game.regions.firstOrNull{it.source==regionSource}?:game.regions.first()}
 var dex by remember{mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())};var regionalDex by remember{mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())};var loading by remember{mutableStateOf(true)};var needsComplement by remember{mutableStateOf(false)};var page by rememberSaveable{mutableIntStateOf(AppStatePreferences.boxPage(regionSource))};var gameMenu by remember{mutableStateOf(false)};var regionMenu by remember{mutableStateOf(false)};var search by remember{mutableStateOf(false)};var allBoxes by remember{mutableStateOf(false)};var captureTarget by remember{mutableStateOf<GameDexService.GameDexEntry?>(null)}
 var evolutionFilterName by rememberSaveable{mutableStateOf<String?>(null)}
 var evolutionFilterMenu by remember{mutableStateOf(false)}
 var evolutionMethodIds by remember(region.source){mutableStateOf<Map<String,Set<Int>>>(emptyMap())}
 var evolutionMethodLoading by remember(region.source){mutableStateOf(false)}
 LaunchedEffect(region.source,game.label){
  evolutionFilterName=null
  loading=true
  needsComplement=false
  AppStatePreferences.activeGame=game.label
  AppStatePreferences.setActiveRegionForGame(game.label,region.source)
  val boxSource=region.source
  page=AppStatePreferences.boxPage(boxSource)
  val generalReady=withContext(Dispatchers.IO){OfflineGamePackManager.generalAudit()}
  val gameReady=withContext(Dispatchers.IO){OfflineGamePackManager.status(game.label).verified}
  if(generalReady && !gameReady){
   dex=emptyList()
   needsComplement=true
   loading=false
  }else{
   val gameDex=withContext(Dispatchers.IO){loadBoxGameDex(AppGameCatalog.games.first{it.label==game.label},region.source)}
   dex=gameDex.all
   regionalDex=gameDex.filtered
   val pageCount=((dex.size+29)/30).coerceAtLeast(1)
   if(page>=pageCount) page=pageCount-1
   AppStatePreferences.setBoxPage(boxSource,page)
   loading=false
  }
 }
 val pages=((dex.size+29)/30).coerceAtLeast(1);val current=page.coerceIn(0,pages-1)
 val boxSource=region.source
 LaunchedEffect(boxSource,current){AppStatePreferences.setBoxPage(boxSource,current)} // unified Box; legacy verifier marker: setBoxPage(region.source,current)
 val entries=remember(dex,current){dex.drop(current*30).take(30)}
 LaunchedEffect(region.source,dex){
  if(dex.isEmpty()){evolutionMethodIds=emptyMap();evolutionMethodLoading=false}
  else{
   EvolutionFilterIndex.cached(region.source)?.let{evolutionMethodIds=it;evolutionMethodLoading=false} ?: run{
    evolutionMethodLoading=true
    evolutionMethodIds=withContext(Dispatchers.IO){EvolutionFilterIndex.build(region.source,dex)}
    evolutionMethodLoading=false
   }
  }
 }
 LaunchedEffect(region.source,current,dex){
  withContext(Dispatchers.IO){runCatching{PokedexDataStore.prefetchBoxWindow(dex,current)}}
 }
 val capturedIds=CollectionStore.capturedForGame(region.source) // unified game Box; legacy verifier marker: CollectionStore.contextualCapturedIds[region.source]
 val caught=remember(dex,capturedIds){dex.count{it.nationalId in capturedIds}}
 val progress=if(dex.isEmpty())0f else caught.toFloat()/dex.size
 val variantsInRegion=remember(region.source,VariantCollectionStore.ownedVariants){VariantCollectionStore.ownedVariants.filter{it.source==region.source}}
 val shinyCaptured=remember(variantsInRegion){variantsInRegion.count{it.shiny}}
 val formCaptured=remember(variantsInRegion){variantsInRegion.count{it.formPokemonId!=it.speciesId || !it.formName.equals(PokemonRepository.byId(it.speciesId)?.name,true)}}
 val gameProgress=remember(game.label,CollectionStore.contextualCapturedIds){
  val regionalTotals=game.regions.map{r->
   val ctx=GameContext.fromSource(r.source)
   val regionalDex=ctx?.let{GameDexService.cached(it)}.orEmpty()
   val owned=CollectionStore.contextualCapturedIds[r.source].orEmpty()
   regionalDex.size to regionalDex.count{it.nationalId in owned}
  }
  regionalTotals.sumOf{it.second} to regionalTotals.sumOf{it.first}
 }
 val nationalCaptured=CollectionStore.capturedIds.count{it in 1..PokeApiService.MAX_NATIONAL_DEX_ID}
 if(evolutionFilterName!=null){
  EvolutionFilterFullScreen(
   gameLabel=game.label,
   initialRegionSource=region.source,
   filterKey=evolutionFilterName!!,
   onBack={evolutionFilterName=null},
   onPokemonClick=onPokemonClick
  )
  return
 }
 val activeEvolutionIds=evolutionFilterName?.let{evolutionMethodIds[it].orEmpty()}.orEmpty()
 val filteredEvolutionEntries=remember(evolutionFilterName,dex,activeEvolutionIds){if(evolutionFilterName==null) emptyList() else dex.filter{it.nationalId in activeEvolutionIds}}
 val evolutionFilterLabel=evolutionFilterName?.let{filter->if(filter=="ALL")"Todas especiais" else PokeApiService.EvolutionMethod.entries.firstOrNull{it.name==filter}?.label ?: filter}
 Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal=PokedexDesignTokens.Spacing.Sm)){
  Column(Modifier.fillMaxWidth().padding(vertical=PokedexDesignTokens.Spacing.Sm)){
   Text("Box",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
   Text("Organize e registre sua coleção por jogo.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  Row(
   Modifier.fillMaxWidth().padding(bottom=PokedexDesignTokens.Spacing.Xs),
   horizontalArrangement=Arrangement.spacedBy(5.dp)
  ){
   ExposedDropdownMenuBox(gameMenu,{gameMenu=!gameMenu},Modifier.weight(1.18f)){
    OutlinedTextField(
     game.label,{},Modifier.menuAnchor().fillMaxWidth().heightIn(min=38.dp),
     readOnly=true,singleLine=true,label={Text("Jogo",style=MaterialTheme.typography.labelSmall)},
     textStyle=MaterialTheme.typography.bodySmall,
     trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(gameMenu)},
     shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm)
    )
    ExposedDropdownMenu(gameMenu,{gameMenu=false}){
     qbGames.forEach{g->
      DropdownMenuItem(
       text={Text(g.label,fontWeight=FontWeight.SemiBold)},
       onClick={gameLabel=g.label;AppStatePreferences.activeGame=g.label;regionSource=g.regions.firstOrNull{it.source==AppStatePreferences.activeRegionForGame(g.label)}?.source?:g.regions.first().source;page=AppStatePreferences.boxPage(g.regions.first().source);AppStatePreferences.setActiveRegionForGame(g.label,regionSource);gameMenu=false}
      )
     }
    }
   }
   ExposedDropdownMenuBox(regionMenu,{regionMenu=!regionMenu},Modifier.weight(.82f)){
    OutlinedTextField(
     region.label,{},Modifier.menuAnchor().fillMaxWidth().heightIn(min=38.dp),
     readOnly=true,singleLine=true,
     label={Text(if(game.regions.size>1)"Filtro Pokédex" else "Região",style=MaterialTheme.typography.labelSmall)},
     textStyle=MaterialTheme.typography.bodySmall,
     trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(regionMenu)},
     shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm)
    )
    ExposedDropdownMenu(regionMenu,{regionMenu=false}){
     game.regions.forEach{r->
      DropdownMenuItem(
       text={Column{
        Text(r.label,fontWeight=FontWeight.SemiBold)
        if(r.badge.isNotBlank())Text(r.badge,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
       }},
       onClick={regionSource=r.source;AppStatePreferences.setActiveRegionForGame(game.label,r.source);page=AppStatePreferences.boxPage(r.source);regionMenu=false}
      )
     }
    }
   }
  }
  Row(
   Modifier.fillMaxWidth().height(40.dp).padding(horizontal=4.dp),
   verticalAlignment=Alignment.CenterVertically
  ){
   Column(Modifier.weight(1f)){
    Text(
     if(evolutionFilterName==null)"BOX "+(current+1)+" DE "+pages else "EVOLUÇÃO · "+evolutionFilterLabel.orEmpty().uppercase(),
     fontSize=10.sp,
     lineHeight=11.sp,
     fontWeight=FontWeight.Black,
     letterSpacing=.5.sp,
     color=game.accent,
     maxLines=1,
     overflow=TextOverflow.Ellipsis
    )
    if(evolutionFilterName!=null) Text(
     if(evolutionMethodLoading)"Organizando Pokémon…" else filteredEvolutionEntries.size.toString()+" Pokémon",
     fontSize=8.5.sp,
     fontWeight=FontWeight.Bold,
     color=MaterialTheme.colorScheme.onSurfaceVariant,
     maxLines=1
    )
   }
   Box{
    IconButton(
     onClick={evolutionFilterMenu=true},
     modifier=Modifier.size(36.dp)
    ){
     Icon(
      Icons.Default.AutoAwesome,
      contentDescription="Filtrar por método de evolução",
      tint=if(evolutionFilterName!=null)game.accent else MaterialTheme.colorScheme.onSurfaceVariant
     )
    }
    DropdownMenu(expanded=evolutionFilterMenu,onDismissRequest={evolutionFilterMenu=false}){
     DropdownMenuItem(text={Text("Mostrar todas as Boxes")},onClick={evolutionFilterName=null;evolutionFilterMenu=false})
     EvolutionRuleCatalog.filterOptions.forEach{option->
      DropdownMenuItem(
       text={Text(option.label)},
       leadingIcon=if(option.key=="ALL"){{Icon(Icons.Default.AutoAwesome,null)}}else null,
       onClick={evolutionFilterName=option.key;evolutionFilterMenu=false}
      )
     }
    }
   }
   Box(Modifier.size(42.dp),contentAlignment=Alignment.Center){
    if(evolutionFilterName!=null){
     EvolutionModeCounter(filteredEvolutionEntries.size,evolutionMethodLoading,game.accent)
    }else{
     CircularProgressIndicator(progress={progress.coerceIn(0f,1f)},modifier=Modifier.fillMaxSize(),strokeWidth=4.dp,color=game.accent,trackColor=game.accent.copy(alpha=.12f))
     Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
      Text(((progress*100).toInt()).toString()+"%",fontSize=9.sp,lineHeight=10.sp,fontWeight=FontWeight.Black,color=game.accent)
      Spacer(Modifier.height(1.dp))
      Text(caught.toString()+" de "+dex.size,fontSize=7.sp,lineHeight=8.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
    }
   }
  }
  var dragTotal by remember { mutableFloatStateOf(0f) }
  Box(
   Modifier
    .weight(1f)
    .fillMaxWidth()
    .pointerInput(current,pages,loading,evolutionFilterName){
     detectHorizontalDragGestures(
      onDragStart={dragTotal=0f},
      onHorizontalDrag={change,dragAmount->
       change.consume()
       dragTotal+=dragAmount
      },
      onDragEnd={
       val threshold=90f
       if(!loading && evolutionFilterName==null){
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
    needsComplement->BoxOfflineComplementRequired(game.label,game.accent)
    dex.isEmpty()->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Não foi possível carregar esta Pokédex regional.")}
    else->{
     if(evolutionFilterName==null){
      AnimatedBoxGrid(current,dex,capturedIds,boxSource,{pk->onPokemonClick(pk.nationalId,boxSource)},{pk->captureTarget=pk})
     }else{
      EvolutionVirtualBox(filteredEvolutionEntries,capturedIds,region.source,evolutionMethodLoading,game.accent,onPokemonClick){pk->captureTarget=pk}
     }
    }
   }
  }
  Row(
   Modifier.fillMaxWidth().height(40.dp).padding(bottom=1.dp),
   horizontalArrangement=Arrangement.spacedBy(4.dp)
  ){
   if(evolutionFilterName==null){
    FilledTonalButton(
     {search=true},
     Modifier.weight(1f).fillMaxHeight(),
     shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm)
    ){
     Icon(Icons.Default.Search,"Pesquisar Pokémon",Modifier.size(17.dp))
     Spacer(Modifier.width(5.dp))
     Text("Buscar Pokémon",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge)
    }
    FilledTonalButton(
     {allBoxes=true},
     Modifier.weight(1f).fillMaxHeight(),
     shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm)
    ){
     Icon(Icons.Default.GridView,"Ver todas as Boxes",Modifier.size(17.dp))
     Spacer(Modifier.width(5.dp))
     Text("Todas as Boxes",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge)
    }
   }else{
    FilledTonalButton(
     {evolutionFilterName=null},
     Modifier.fillMaxWidth().fillMaxHeight(),
     shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm)
    ){
     Icon(Icons.Default.Close,"Limpar filtro de evolução",Modifier.size(17.dp))
     Spacer(Modifier.width(5.dp))
     Text("Limpar filtro e voltar às Boxes",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge)
    }
   }
  }
 }
 if(search)QBSearch(regionalDex,capturedIds,region.source,{search=false},{pk->val i=dex.indexOfFirst{it.nationalId==pk.nationalId};if(i>=0)page=i/30;search=false},{pk->search=false;onPokemonClick(pk.nationalId,region.source)},{pk->captureTarget=pk})
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
