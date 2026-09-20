package com.otaviobarreto.pokedex.ui
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable; import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn; import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalHapticFeedback; import androidx.compose.ui.platform.LocalContext
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
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.AppStatePreferences
import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import com.otaviobarreto.pokedex.data.EvolutionFilterIndex
import com.otaviobarreto.pokedex.data.EvolutionRuleCatalog
import com.otaviobarreto.pokedex.data.PokedexDataStore; import com.otaviobarreto.pokedex.data.StartupPreloader
import com.otaviobarreto.pokedex.data.OfflineGamePackManager
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.PokemonRepository
import com.otaviobarreto.pokedex.data.PokemonFormsService
import com.otaviobarreto.pokedex.data.PokemonFormVariant
import com.otaviobarreto.pokedex.data.PokemonFormPresentation
import com.otaviobarreto.pokedex.data.VariantCollectionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
private data class QBRegion(val label:String,val source:String,val badge:String)
private data class QBGame(val label:String,val accent:Color,val regions:List<QBRegion>)
private fun qbAccent(game:String):Color=PokedexDesignTokens.Colors.game(game)
private val qbGames=AppGameCatalog.games.map{game->QBGame(game.label,qbAccent(game.label),game.regions.map{QBRegion(it.label,it.source,it.subtitle)})}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BoxesV2Screen(onPokemonClick:(Int,String?)->Unit){
 val preferredGame=AppStatePreferences.activeGame.takeIf{g->qbGames.any{it.label==g}} ?: qbGames.first().label
 var allGames by rememberSaveable{mutableStateOf(false)};var gameLabel by rememberSaveable{mutableStateOf(preferredGame)}
 val game=remember(gameLabel){qbGames.firstOrNull{it.label==gameLabel}?:qbGames.first()}
 val preferredRegion=AppStatePreferences.activeRegionForGame(game.label)
 var regionSource by rememberSaveable{mutableStateOf(game.regions.firstOrNull{it.source==preferredRegion}?.source ?: game.regions.first().source)}
 val region=remember(game.label,regionSource){game.regions.firstOrNull{it.source==regionSource}?:game.regions.first()}
 var dex by remember{mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())};var gameDexIds by remember{mutableStateOf<Set<Int>>(emptySet())};var regionalDex by remember{mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())};var loading by remember{mutableStateOf(true)};var needsComplement by remember{mutableStateOf(false)};var page by rememberSaveable{mutableIntStateOf(AppStatePreferences.boxPage(regionSource))};var gameMenu by remember{mutableStateOf(false)};var regionMenu by remember{mutableStateOf(false)};var search by remember{mutableStateOf(false)};var allBoxes by remember{mutableStateOf(false)};var captureTarget by remember{mutableStateOf<GameDexService.GameDexEntry?>(null)}
 var evolutionFilterName by rememberSaveable{mutableStateOf<String?>(null)}
var evolutionFilterMenu by remember{mutableStateOf(false)}
 var evolutionMethodIds by remember(region.source){mutableStateOf<Map<String,Set<Int>>>(emptyMap())}
 var evolutionMethodLoading by remember(region.source){mutableStateOf(false)}
 val boxContext=LocalContext.current.applicationContext
 LaunchedEffect(region.source,game.label,allGames){
  evolutionFilterName=null
  loading=true
  needsComplement=false
  AppStatePreferences.activeGame=game.label
  AppStatePreferences.setActiveRegionForGame(game.label,region.source)
  val boxSource=game.regions.first().source
  page=AppStatePreferences.boxPage(boxSource)
  val generalReady=withContext(Dispatchers.IO){OfflineGamePackManager.generalAudit()}
  val gameReady=withContext(Dispatchers.IO){OfflineGamePackManager.status(game.label).verified}
  if(generalReady && !gameReady){
   dex=emptyList()
   needsComplement=true
   loading=false
  }else{
   val gameDex=withContext(Dispatchers.IO){loadBoxGameDex(AppGameCatalog.games.first{it.label==game.label},region.source)}
   dex=nationalLivingDex()
   gameDexIds=if(allGames)allNationalIds() else gameDex.all.mapTo(linkedSetOf()){it.nationalId}
   regionalDex=gameDex.filtered
   val pageCount=((dex.size+29)/30).coerceAtLeast(1)
   if(page>=pageCount) page=pageCount-1
   AppStatePreferences.setBoxPage(boxSource,page)
   loading=false
  }
 }
 val pages=((dex.size+29)/30).coerceAtLeast(1);val current=page.coerceIn(0,pages-1)
 val boxSource=game.regions.first().source
 LaunchedEffect(boxSource,current){AppStatePreferences.setBoxPage(boxSource,current)} // unified Box; legacy verifier marker: setBoxPage(region.source,current)
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
 LaunchedEffect(region.source,current,dex){withContext(Dispatchers.IO){runCatching{PokedexDataStore.prefetchBoxWindow(dex,current)};runCatching{StartupPreloader.warmBoxWindow(boxContext,dex,current)}}}
 val relevantPages=remember(gameDexIds){relevantLivingBoxPages(gameDexIds)}
 val capturedIds=CollectionStore.capturedIds.toSet() // National Living Dex; legacy verifier marker: CollectionStore.contextualCapturedIds[region.source]
 val caught=remember(dex,capturedIds){dex.count{it.nationalId in capturedIds}};val missingFiltered=remember(gameDexIds,capturedIds){gameDexIds.count{it !in capturedIds}}
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
 val filteredEvolutionEntries=if(evolutionFilterName==null) emptyList() else dex.filter{it.nationalId in activeEvolutionIds}
 val evolutionFilterLabel=evolutionFilterName?.let{filter->if(filter=="ALL")"Todas especiais" else PokeApiService.EvolutionMethod.entries.firstOrNull{it.name==filter}?.label ?: filter}
 Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal=PokedexDesignTokens.Spacing.Sm)){
  Column(Modifier.fillMaxWidth().padding(vertical=PokedexDesignTokens.Spacing.Sm)){
   Text("Box",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
   Text("National Living Dex · 30 Pokémon por Box.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  Row(
   Modifier.fillMaxWidth().padding(bottom=PokedexDesignTokens.Spacing.Xs),
   horizontalArrangement=Arrangement.spacedBy(5.dp)
  ){
   ExposedDropdownMenuBox(gameMenu,{gameMenu=!gameMenu},Modifier.weight(1.18f)){
    OutlinedTextField(
     if(allGames)ALL_GAMES_LABEL else game.label,{},Modifier.menuAnchor().fillMaxWidth().heightIn(min=38.dp),
     readOnly=true,singleLine=true,label={Text("Jogo",style=MaterialTheme.typography.labelSmall)},
     textStyle=MaterialTheme.typography.bodySmall,
     trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(gameMenu)},
     shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm)
    )
    ExposedDropdownMenu(gameMenu,{gameMenu=false}){
     DropdownMenuItem(text={Text(ALL_GAMES_LABEL,fontWeight=FontWeight.SemiBold)},onClick={allGames=true;gameMenu=false;page=0})
     qbGames.forEach{g->
      DropdownMenuItem(
       text={Text(g.label,fontWeight=FontWeight.SemiBold)},
       onClick={allGames=false;gameLabel=g.label;AppStatePreferences.activeGame=g.label;regionSource=g.regions.firstOrNull{it.source==AppStatePreferences.activeRegionForGame(g.label)}?.source?:g.regions.first().source;page=AppStatePreferences.boxPage(g.regions.first().source);AppStatePreferences.setActiveRegionForGame(g.label,regionSource);gameMenu=false}
      )
     }
    }
   }
   if(!allGames) ExposedDropdownMenuBox(regionMenu,{regionMenu=!regionMenu},Modifier.weight(.82f)){
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
  Box(Modifier.weight(1f).fillMaxWidth()){
   when{
    loading->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=game.accent)}
    needsComplement->BoxOfflineComplementRequired(game.label,game.accent)
    dex.isEmpty()->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Não foi possível carregar esta Pokédex regional.")}
    else->{
     if(evolutionFilterName==null){
      LivingDexPager(
       page=current,
       pageCount=pages,
       dex=dex,
       captured=capturedIds,
       source=boxSource,
       available=gameDexIds,
       relevantPages=relevantPages,
       onPageSelected={candidate->
        val target=if(candidate in relevantPages) candidate
        else relevantPages.minByOrNull{kotlin.math.abs(it-candidate)} ?: current
        page=target
       },
       open={pk->onPokemonClick(pk.nationalId,boxSource)},
       hold={pk->captureTarget=pk}
      )
     }else{
      EvolutionVirtualBox(filteredEvolutionEntries,capturedIds,region.source,evolutionMethodLoading,game.accent,onPokemonClick){pk->captureTarget=pk}
     }
    }
   }
  }
  if(evolutionFilterName==null) LivingDexFilterActions(relevantPages.size,missingFiltered,game.accent,{nextMissingNationalId(gameDexIds,capturedIds,current*30)?.let{page=(it-1)/30}},{allBoxes=true})
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
     Text("Boxes do filtro",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge)
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
  relevantPages=relevantPages,available=gameDexIds,
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
@Composable private fun BoxCompanionHeader(game:String,region:String,caught:Int,total:Int,accent:Color)=CompanionContextHeader(title="Box",eyebrow="Coleção por jogo",subtitle=game+" · "+region,modifier=Modifier.padding(top=PokedexDesignTokens.Spacing.Xs,bottom=PokedexDesignTokens.Spacing.Xs),accent=accent,progress={CompanionProgress(current=caught,total=total,label="Pokédex do jogo",accent=accent)})
@Composable
internal fun QBGrid(
    entries:List<GameDexService.GameDexEntry>,
    captured:Set<Int>,
    source:String,
    specialFilter:Boolean,
    specialIds:Set<Int>,
    available:Set<Int>?=null,
    open:(GameDexService.GameDexEntry)->Unit, hold:(GameDexService.GameDexEntry)->Unit
){
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(2.dp)){
        repeat(5){row->
            Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)){
                repeat(6){col->
                    val pk=entries.getOrNull(row*6+col)
                    if(pk==null){
                        Surface(Modifier.weight(1f).fillMaxHeight(),RoundedCornerShape(9.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f)){}
                    }else{
                        QBSlot(
                            pk=pk,
                            captured=pk.nationalId in captured,
                            source=source,
                            specialEvolution=pk.nationalId in specialIds,
                            specialFilter=specialFilter,
                            available=available==null || pk.nationalId in available,
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
internal fun QBSlot(
    pk:GameDexService.GameDexEntry,
    captured:Boolean,
    source:String,
    specialEvolution:Boolean,
    specialFilter:Boolean,
    available:Boolean=true,
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
    val slotAlpha by animateFloatAsState(
        if(!available).28f else if(specialFilter&&!specialEvolution).18f else 1f,
        tween(PokedexDesignTokens.Motion.Fast),
        label="boxSlotAlpha"
    )
    val slotScale=if(captured)1f else .985f
    val slotColor=if(captured)MaterialTheme.colorScheme.primaryContainer.copy(alpha=.62f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.46f)
    val interaction=remember(pk.nationalId,source){MutableInteractionSource()}
    val haptic=LocalHapticFeedback.current
    Surface(
        modifier
            .dexInteractiveSurface(interactionSource=interaction,pressedScale=.955f)
            .alpha(slotAlpha)
            .scale(slotScale)
            .semantics {
                contentDescription = buildString {
                    append(pk.name)
                    append(", número ")
                    append(pk.gameNumber)
                    append(if(captured) ", capturado" else ", não capturado")
                    if(isShiny) append(", Shiny")
                    if(variant!=null && variant.formPokemonId!=pk.nationalId) append(", forma alternativa")
                    if(specialEvolution) append(", evolução especial")
                }
            }
            .combinedClickable(
                interactionSource=interaction,
                indication=null,
                onClick={
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    open()
                },
                onLongClick={
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    hold()
                }
            ),
        shape=RoundedCornerShape(9.dp),
        color=slotColor
    ){
        Box(Modifier.fillMaxSize()){
            PokemonArtwork(
                model=imageModel,
                contentDescription=pk.name,
                pokemonId=pk.nationalId,
                modifier=Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.80f)
                    .align(Alignment.TopCenter)
                    .padding(horizontal=2.dp,vertical=2.dp)
                    .alpha(if(!available).12f else if(captured)1f else .34f),
                contentScale=ContentScale.Fit,
                colorFilter=if(captured&&available)null else ColorFilter.colorMatrix(ColorMatrix().apply{setToSaturation(0f)})
            )
            if(specialEvolution){
                Surface(
                    Modifier.align(Alignment.TopEnd).padding(3.dp),
                    shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm),
                    color=PokedexDesignTokens.Colors.SpecialGold.copy(alpha=.16f)
                ){Text("✦",Modifier.padding(horizontal=PokedexDesignTokens.Spacing.Xs,vertical=1.dp),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Black,color=PokedexDesignTokens.Colors.SpecialGold)}
            }
            Surface(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color=MaterialTheme.colorScheme.surface.copy(alpha=if(captured).94f else .82f)
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
                        color=if(captured)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(2.dp)){
                        Text(
                            "#"+pk.gameNumber.toString().padStart(3,'0'),
                            fontSize=7.sp,
                            lineHeight=7.sp,
                            color=MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if(isShiny){
                            Surface(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm),color=PokedexDesignTokens.Colors.ShinyGold.copy(alpha=.14f)){
                                Text("★",Modifier.padding(horizontal=2.dp),style=MaterialTheme.typography.labelSmall,color=PokedexDesignTokens.Colors.ShinyGold,fontWeight=FontWeight.Black)
                            }
                        }
                        if(variant!=null && variant.formPokemonId!=pk.nationalId){
                            Surface(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm),color=MaterialTheme.colorScheme.primary.copy(alpha=.12f)){
                                Text("F",Modifier.padding(horizontal=2.dp),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Black)
                            }
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
    val haptic=LocalHapticFeedback.current
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
            shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
            color=MaterialTheme.colorScheme.background
        ){
            Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(pretty(pk.name),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
                        Text("Formas e Shiny",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(dismiss){Icon(Icons.Default.Close,"Fechar")}
                }
                Text(
                    "Você pode registrar mais de uma forma e manter Normal + Shiny ao mesmo tempo.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier=Modifier.padding(bottom=8.dp)
                )
                val isCaptured=CollectionStore.isCapturedInGame(source,pk.nationalId)
                if(isCaptured){
                    OutlinedButton(
                        onClick={
                            VariantCollectionStore.removeAll(source,pk.nationalId)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dismiss()
                        },
                        modifier=Modifier.fillMaxWidth(),
                        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm)
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
                        verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)
                    ){
                        items(available,key={it.formKey}){form->
                            val formId=form.pokemonId!!
                            val normalOwned=VariantCollectionStore.isOwned(source,pk.nationalId,formId,form.name,false)
                            val shinyOwned=VariantCollectionStore.isOwned(source,pk.nationalId,formId,form.name,true)
                            Card(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md)){
                                Row(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment=Alignment.CenterVertically
                                ){
                                    Surface(
                                        modifier=Modifier.size(64.dp),
                                        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm),
                                        color=MaterialTheme.colorScheme.surface
                                    ){
                                        Box(
                                            Modifier.fillMaxSize().padding(4.dp),
                                            contentAlignment=Alignment.Center
                                        ){
                                            PokemonArtwork(
                                                model=form.spriteUrl ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+formId+".png",
                                                contentDescription=form.name,
                                                pokemonId=formId,
                                                modifier=Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    Column(Modifier.weight(1f).padding(start=8.dp)){
                                        Text(
                                            PokemonFormPresentation.label(pretty(pk.name),form.name,false)+
                                                when{
                                                    form.isDefault -> " · padrão"
                                                    !form.countsForLivingDex -> " · temporária"
                                                    else -> ""
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
                                                        source,pk.nationalId,formId,form.name,false,
                                                        formKey=form.formKey,
                                                        normalArtworkUrl=form.spriteUrl,
                                                        shinyArtworkUrl=form.shinySpriteUrl,
                                                        isDefault=form.isDefault
                                                    )
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    dismiss()
                                                },
                                                label={Text("Normal",fontSize=10.sp)},
                                                leadingIcon=if(normalOwned){{Icon(Icons.Default.Check,null,Modifier.size(14.dp))}}else null
                                            )
                                            FilterChip(
                                                selected=shinyOwned,
                                                onClick={
                                                    VariantCollectionStore.toggle(
                                                        source,pk.nationalId,formId,form.name,true,
                                                        formKey=form.formKey,
                                                        normalArtworkUrl=form.spriteUrl,
                                                        shinyArtworkUrl=form.shinySpriteUrl
                                                    )
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    dismiss()
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
                            if(!any && CollectionStore.isCapturedInGame(source,pk.nationalId)){
                                Text(
                                    "Este Pokémon já estava marcado como capturado em uma versão antiga. Selecione Normal ou Shiny para migrá-lo ao novo sistema de variantes.",
                                    style=MaterialTheme.typography.bodySmall,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier=Modifier.padding(PokedexDesignTokens.Spacing.Sm)
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
    relevantPages:List<Int>,available:Set<Int>,
    dismiss:()->Unit,
    select:(Int)->Unit
){
    Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Surface(
            Modifier.fillMaxWidth(.96f).fillMaxHeight(.90f),
            shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
            color=MaterialTheme.colorScheme.background
        ){
            Column(Modifier.fillMaxSize().padding(PokedexDesignTokens.Spacing.Lg)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("Boxes do filtro",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.onSurface)
                        Text(relevantPages.size.toString()+" Boxes com Pokémon · toque para abrir",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(dismiss){Icon(Icons.Default.Close,"Fechar")}
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)){
                    items(relevantPages,key={it}){index->
                        val entries=dex.drop(index*30).take(30)
                        val filtered=entries.filter{it.nationalId in available};val total=filtered.size.coerceAtLeast(1);val owned=filtered.count{it.nationalId in captured}
                        val pct=owned.toFloat()/total
                        Card(
                            Modifier.fillMaxWidth().clickable{select(index)},
                            shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),
                            colors=CardDefaults.cardColors(
                                containerColor=if(index==current)accent.copy(alpha=.14f) else MaterialTheme.colorScheme.surface
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
                                    Text("Box "+(index+1),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurface)
                                    Text(owned.toString()+" / "+filtered.size+" do filtro capturados",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if(index==current)AssistChip(onClick={},label={Text("Atual",style=MaterialTheme.typography.labelSmall)})
                                else Icon(Icons.Default.ArrowForwardIos,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QBSearch(
    dex:List<GameDexService.GameDexEntry>,
    captured:Set<Int>,
    source:String,
    dismiss:()->Unit,
    select:(GameDexService.GameDexEntry)->Unit,
    open:(GameDexService.GameDexEntry)->Unit,
    hold:(GameDexService.GameDexEntry)->Unit
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
                    "Faltantes" -> it.nationalId !in captured
                    "Shiny" -> VariantCollectionStore.ownedVariants.any{v->v.source==source && v.speciesId==it.nationalId && v.shiny}
                    "Normal" -> VariantCollectionStore.ownedVariants.any{v->v.source==source && v.speciesId==it.nationalId && !v.shiny}
                    "Formas" -> VariantCollectionStore.ownedVariants.any{v->
                        v.source==source && v.speciesId==it.nationalId &&
                            (v.formPokemonId!=it.nationalId || !v.formName.equals(pretty(it.name),true))
                    }
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
            LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(listOf("Todos","Capturados","Faltantes","Normal","Shiny","Formas")){option->FilterChip(selected=status==option,onClick={status=option},label={Text(option,style=MaterialTheme.typography.labelSmall)})}}
            LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(listOf("Regional","Nacional","Nome")){option->FilterChip(selected=order==option,onClick={order=option},label={Text(option,style=MaterialTheme.typography.labelSmall)})}}
            Spacer(Modifier.height(6.dp))
            Text(results.size.toString()+" resultado(s)",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(Modifier.heightIn(max=360.dp)){
                items(results,key={it.nationalId}){pk->
                    val haptic=LocalHapticFeedback.current
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(onClick={select(pk)},onLongClick={haptic.performHapticFeedback(HapticFeedbackType.LongPress);hold(pk)}).padding(vertical=5.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        PokemonArtwork(pk.spriteUrl,pk.name,Modifier.size(48.dp),pokemonId=pk.nationalId)
                        Column(Modifier.weight(1f)){
                            Text(pretty(pk.name),fontWeight=FontWeight.SemiBold)
                            Text(
                                (if(pk.nationalId in captured)"Capturado" else "Faltando")+
                                    " · Regional #"+pk.gameNumber+" · Nacional #"+pk.nationalId,
                                fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant
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
private fun pretty(name:String)=name.split("-"," ").joinToString(" "){it.replaceFirstChar{c->if(c.isLowerCase())c.titlecase()else c.toString()}}