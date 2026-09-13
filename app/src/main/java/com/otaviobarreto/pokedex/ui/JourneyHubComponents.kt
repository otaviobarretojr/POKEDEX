package com.otaviobarreto.pokedex.ui

import android.util.Base64

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun JourneyGamePicker(
    onSelect:(String)->Unit,
    onPokemonClick:(Int,String?)->Unit,
    onOpenBoxes:(String,String?)->Unit
){
    val captured=CollectionStore.contextualCapturedIds
    val dexIdsByGame by rememberJourneyDexIdsByGame()
    val activeGamePreview=AppGameCatalog.adventureGames.firstOrNull{it.label==AppStatePreferences.activeGame}
        ?: AppGameCatalog.adventureGames.firstOrNull()
    val dexIdsBySource by rememberJourneyDexIdsBySource(activeGamePreview)
    val national=remember { PokedexDataStore.cachedNationalDex().orEmpty() }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showCollectionProfile by rememberSaveable { mutableStateOf(false) }
    val activeGame=activeGamePreview
    val activeSources=activeGame?.regions?.map{it.source}.orEmpty()
    val activeRegionSource=activeGame?.let{game->
        AppStatePreferences.activeRegionForGame(game.label)
            ?.takeIf{it in activeSources}
            ?: game.regions.firstOrNull()?.source
    }
    val activeOwned=remember(captured,activeSources){activeSources.flatMap{captured[it].orEmpty()}.toSet()}
    val activeDexIds=activeGame?.let{dexIdsByGame[it.label].orEmpty()}.orEmpty()
    val missingIds=remember(activeDexIds,activeOwned){activeDexIds.sorted().filter{it !in activeOwned}}
    val nextMissing=remember(missingIds,activeRegionSource,dexIdsBySource,captured){
        val regional=dexIdsBySource[activeRegionSource].orEmpty()
        missingIds.firstOrNull{it in regional && it !in captured[activeRegionSource].orEmpty()}
            ?: missingIds.firstOrNull()
    }
    val nextMissingSource=remember(nextMissing,activeRegionSource,dexIdsBySource,activeSources,captured){
        nextMissing?.let{id->
            activeRegionSource?.takeIf{id in dexIdsBySource[it].orEmpty() && id !in captured[it].orEmpty()}
                ?: activeSources.firstOrNull{source->id in dexIdsBySource[source].orEmpty() && id !in captured[source].orEmpty()}
                ?: activeRegionSource
        }
    }
    val journeyRevision=JourneyProgressStore.revision
    val activeSteps=remember(activeGame?.label,journeyRevision){activeGame?.let{JourneyCatalog.steps(it.label)}.orEmpty()}
    val activeCompleted=remember(activeGame?.label,journeyRevision){activeGame?.let{JourneyProgressStore.completed(it.label)}.orEmpty()}
    val nextStep=remember(activeSteps,activeCompleted){activeSteps.firstOrNull{it.id !in activeCompleted}}
    val journeyDone=remember(activeSteps,activeCompleted){DataIntegrityRules.completedCount(activeSteps.map{it.id},activeCompleted)}
    val journeyRatio=if(activeSteps.isEmpty())0f else journeyDone.toFloat()/activeSteps.size
    val collectionRatio=if(activeDexIds.isEmpty())0f else activeOwned.count{it in activeDexIds}.toFloat()/activeDexIds.size
    val activeRegionDexIds=dexIdsBySource[activeRegionSource].orEmpty()
    val activeRegionOwned=captured[activeRegionSource].orEmpty()
    val regionalMissingIds=remember(activeRegionDexIds,activeRegionOwned){
        activeRegionDexIds.sorted().filter{it !in activeRegionOwned}
    }
    val regionRatio=if(activeRegionDexIds.isEmpty())0f else activeRegionOwned.count{it in activeRegionDexIds}.toFloat()/activeRegionDexIds.size
    val plannerTargets=remember(regionalMissingIds,missingIds,activeRegionSource,dexIdsBySource,activeSources){
        val preferred=if(regionalMissingIds.isNotEmpty()) regionalMissingIds else missingIds
        preferred.take(5).map{id->
            val source=activeRegionSource?.takeIf{id in dexIdsBySource[it].orEmpty()}
                ?: activeSources.firstOrNull{s->id in dexIdsBySource[s].orEmpty()}
            id to source
        }
    }
    val animatedJourneyRatio by animateFloatAsState(targetValue=journeyRatio,label="companionJourney")
    val animatedCollectionRatio by animateFloatAsState(targetValue=collectionRatio,label="companionCollection")
    val animatedRegionRatio by animateFloatAsState(targetValue=regionRatio,label="companionRegion")
    val searchResultIds=remember(searchQuery,national){
        val q=searchQuery.trim()
        if(q.length<2) emptyList()
        else if(national.isNotEmpty()){
            national.filter{
                it.name.contains(q,ignoreCase=true) || it.id.toString()==q.removePrefix("#")
            }.take(8).map{it.id}
        }else{
            PokemonRepository.search(PokemonFilter(query=q)).take(8).map{it.id}
        }
    }
    val shinyTotal=VariantCollectionStore.ownedVariants.count{it.shiny}
    val formTotal=VariantCollectionStore.formCount()

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.background,
                    (activeGame?.let{PokedexDesignTokens.Colors.game(it.label)} ?: MaterialTheme.colorScheme.primary).copy(alpha=.10f),
                    MaterialTheme.colorScheme.background
                )
            )
        )
    ){
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding=PaddingValues(horizontal=16.dp,vertical=18.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)
        ){
            item{
                Column(Modifier.fillMaxWidth()){
                    Surface(shape=RoundedCornerShape(999.dp),color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.72f)){
                        Row(Modifier.padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){
                            Icon(Icons.Default.AutoAwesome,null,Modifier.size(15.dp),tint=MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(5.dp))
                            Text("COMPANION",style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Sua aventura",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black)
                    Text("Continue exatamente de onde parou e encontre o que falta sem procurar em vários menus.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            activeGame?.let{game->
                item(key="active_companion"){
                    val accent=PokedexDesignTokens.Colors.game(game.label)
                    Card(
                        modifier=Modifier.fillMaxWidth(),
                        shape=RoundedCornerShape(24.dp),
                        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),
                        elevation=CardDefaults.cardElevation(defaultElevation=PokedexDesignTokens.Elevation.Low)
                    ){
                        Column(Modifier.fillMaxWidth().padding(16.dp)){
                            Crossfade(targetState=game.label,label="activeGameHero"){heroGameLabel->
                                val denseCover=heroGameLabel=="Scarlet / Violet" || GameCoverCatalog.coversFor(heroGameLabel).size>=2
                                Box(
                                    Modifier.fillMaxWidth().height(190.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                ){
                                    JourneyGameCover(
                                        gameLabel=heroGameLabel,
                                        modifier=Modifier.fillMaxSize()
                                    )
                                    JourneyHeroArtwork(
                                        ids=JourneyGameVisualCatalog.forGame(heroGameLabel).heroPokemonIds,
                                        alphaScale=if(denseCover).42f else .82f,
                                        modifier=Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(170.dp)
                                    )
                                    Box(
                                        Modifier.matchParentSize().background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.surface.copy(alpha=.06f),
                                                    MaterialTheme.colorScheme.surface.copy(alpha=.84f)
                                                )
                                            )
                                        )
                                    )
                                    Surface(
                                        modifier=Modifier.align(Alignment.TopStart).padding(10.dp),
                                        shape=RoundedCornerShape(999.dp),
                                        color=MaterialTheme.colorScheme.surface.copy(alpha=.88f)
                                    ){
                                        Text(
                                            "JOGO ATIVO",
                                            Modifier.padding(horizontal=10.dp,vertical=6.dp),
                                            style=MaterialTheme.typography.labelSmall,
                                            fontWeight=FontWeight.Black,
                                            color=accent
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Surface(shape=RoundedCornerShape(14.dp),color=accent.copy(alpha=.14f)){
                                    Icon(Icons.Default.PlayArrow,null,Modifier.padding(10.dp).size(24.dp),tint=accent)
                                }
                                Column(Modifier.weight(1f).padding(start=12.dp)){
                                    Text("CONTINUAR",style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Black,color=accent)
                                    Text(game.label,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black,maxLines=2,overflow=TextOverflow.Ellipsis)
                                    Text(nextStep?.let{"Próximo objetivo · "+it.title}?:"Jornada principal concluída",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            CompanionMetricGroup(
                                journeyValue=(animatedJourneyRatio*100).toInt().toString()+"%",
                                journeySubtitle=journeyDone.toString()+"/"+activeSteps.size,
                                dexValue=activeDexIds.size.toString(),
                                dexSubtitle="Pokémon totais no jogo",
                                pendingValue=nextMissing?.let{"#"+it} ?: "OK",
                                pendingSubtitle=if(nextMissing==null)"Completa" else "Próximo alvo"
                            )
                            Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                Button(onClick={onSelect(game.label)},modifier=Modifier.weight(1f)){
                                    Icon(Icons.Default.Explore,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Continuar")
                                }
                                FilledTonalButton(onClick={onOpenBoxes(game.label,activeRegionSource)},modifier=Modifier.weight(1f)){
                                    Icon(Icons.Default.GridView,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Box")
                                }
                            }
                            Crossfade(targetState=nextMissing,label="nextPending"){pendingId->
                                pendingId?.let{id->
                                    val pendingSource=activeRegionSource?.takeIf{
                                        id in dexIdsBySource[it].orEmpty() && id !in captured[it].orEmpty()
                                    } ?: activeSources.firstOrNull{source->
                                        id in dexIdsBySource[source].orEmpty() && id !in captured[source].orEmpty()
                                    } ?: nextMissingSource
                                    val entry=national.firstOrNull{it.id==id}
                                    val fallback=PokemonRepository.byId(id)
                                    val displayName=(entry?.name ?: fallback?.name ?: "#"+id)
                                        .replaceFirstChar{it.uppercase()}
                                    val regionLabel=activeGame.regions.firstOrNull{it.source==pendingSource}?.label
                                        ?: "Região ativa"
                                    Card(
                                        modifier=Modifier.fillMaxWidth().padding(top=8.dp)
                                            .animateContentSize()
                                            .clickable{onPokemonClick(id,pendingSource)},
                                        shape=RoundedCornerShape(16.dp),
                                        colors=CardDefaults.cardColors(containerColor=accent.copy(alpha=.10f))
                                    ){
                                        Row(
                                            Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=9.dp),
                                            verticalAlignment=Alignment.CenterVertically
                                        ){
                                            AsyncImage(
                                                model=entry?.spriteUrl
                                                    ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/"+id+".png",
                                                contentDescription=displayName,
                                                modifier=Modifier.size(46.dp),
                                                contentScale=ContentScale.Fit
                                            )
                                            Column(Modifier.weight(1f).padding(start=10.dp)){
                                                Text("PRÓXIMA PENDÊNCIA",style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Black,color=accent)
                                                Text(displayName,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleSmall)
                                                Text("#"+id.toString().padStart(4,'0')+" · "+regionLabel,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Icon(Icons.Default.ChevronRight,null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if(activeGame!=null && activeDexIds.isNotEmpty()){
                item(key="living_dex_planner"){
                    val livingDexCaptured=activeOwned.count{it in activeDexIds}
                    val livingDexMissing=activeDexIds.size-livingDexCaptured
                    Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
                        Column(Modifier.fillMaxWidth().padding(14.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.secondaryContainer){
                                    Icon(Icons.Default.Checklist,null,Modifier.padding(8.dp).size(19.dp),tint=MaterialTheme.colorScheme.secondary)
                                }
                                Column(Modifier.weight(1f).padding(start=10.dp)){
                                    Text("Living Dex Planner",fontWeight=FontWeight.Black)
                                    Text(
                                        when{
                                            activeDexIds.isEmpty() -> "Progresso da sua Living Dex."
                                            livingDexMissing<=0 -> "Living Dex completa para este jogo."
                                            else -> "Faltam "+livingDexMissing+" de "+activeDexIds.size+" Pokémon."
                                        },
                                        style=MaterialTheme.typography.bodySmall,
                                        color=MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if(activeDexIds.isNotEmpty()){
                                Row(
                                    Modifier.fillMaxWidth().padding(top=12.dp),
                                    horizontalArrangement=Arrangement.spacedBy(8.dp)
                                ){
                                    CompanionProgressMini(
                                        label="Living Dex total",
                                        value=livingDexCaptured.toString()+"/"+activeDexIds.size,
                                        progress=animatedCollectionRatio,
                                        modifier=Modifier.weight(1f)
                                    )
                                }
                            }
                            if(plannerTargets.isNotEmpty()){
                                Text("PRÓXIMOS ALVOS",style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=12.dp,bottom=7.dp))
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                                    plannerTargets.forEach{(id,targetSource)->
                                        Surface(
                                            modifier=Modifier.weight(1f).clickable{onPokemonClick(id,targetSource)},
                                            shape=RoundedCornerShape(14.dp),
                                            color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.58f)
                                        ){
                                            Column(Modifier.padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                                AsyncImage(
                                                    model="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/"+id+".png",
                                                    contentDescription=PokemonRepository.byId(id)?.name,
                                                    modifier=Modifier.size(36.dp),
                                                    contentScale=ContentScale.Fit
                                                )
                                                Text("#"+id,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key="universal_search"){
                OutlinedTextField(
                    value=searchQuery,
                    onValueChange={searchQuery=it},
                    modifier=Modifier.fillMaxWidth(),
                    singleLine=true,
                    leadingIcon={Icon(Icons.Default.Search,null)},
                    trailingIcon={
                        if(searchQuery.isNotBlank()){
                            IconButton(onClick={searchQuery=""}){Icon(Icons.Default.Close,"Limpar busca")}
                        }
                    },
                    label={Text("Busca rápida")},
                    placeholder={Text("Nome ou número do Pokémon")},
                    shape=RoundedCornerShape(18.dp),
                    supportingText={
                        if(national.isEmpty()) Text("Usando catálogo local enquanto a Dex termina de carregar.")
                    }
                )
            }
            if(searchResultIds.isNotEmpty()){
                item(key="search_results"){
                    Card(shape=RoundedCornerShape(18.dp)){
                        Column(Modifier.fillMaxWidth()){
                            searchResultIds.forEachIndexed{index,pokemonId->
                                val cached=national.firstOrNull{it.id==pokemonId}
                                val fallback=PokemonRepository.byId(pokemonId)
                                val pokemonName=(cached?.name ?: fallback?.name ?: "#"+pokemonId)
                                    .replaceFirstChar{it.uppercase()}
                                Row(
                                    Modifier.fillMaxWidth().clickable{
                                        searchQuery=""
                                        onPokemonClick(pokemonId,null)
                                    }.padding(horizontal=14.dp,vertical=11.dp),
                                    verticalAlignment=Alignment.CenterVertically
                                ){
                                    AsyncImage(
                                        model=cached?.spriteUrl
                                            ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/"+pokemonId+".png",
                                        contentDescription=pokemonName,
                                        modifier=Modifier.size(42.dp),
                                        contentScale=ContentScale.Fit
                                    )
                                    Column(Modifier.weight(1f).padding(start=10.dp)){
                                        Text(pokemonName,fontWeight=FontWeight.Bold)
                                        Text("#"+pokemonId.toString().padStart(4,'0'),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight,null)
                                }
                                if(index<searchResultIds.lastIndex) HorizontalDivider(Modifier.padding(horizontal=14.dp))
                            }
                        }
                    }
                }
            }

            item{
                Text("TODOS OS JOGOS",style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp,bottom=1.dp))
            }
            items(AppGameCatalog.adventureGames,key={it.label}){game->
                val progress=rememberJourneyCollectionProgress(game=game,capturedBySource=captured,ids=dexIdsByGame[game.label].orEmpty())
                JourneyGameReferenceCard(game=game,progress=progress,onClick={onSelect(game.label)})
            }
            item(key="collection_profile"){
                val totalCaptured=captured.values.flatten().toSet().size
                Surface(
                    modifier=Modifier.fillMaxWidth().animateContentSize().clickable{showCollectionProfile=!showCollectionProfile},
                    shape=RoundedCornerShape(20.dp),
                    color=MaterialTheme.colorScheme.surface.copy(alpha=.90f),
                    tonalElevation=PokedexDesignTokens.Elevation.Low
                ){
                    Column(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=11.dp)){
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer){
                                Icon(Icons.Default.CatchingPokemon,null,Modifier.padding(9.dp).size(20.dp),tint=MaterialTheme.colorScheme.primary)
                            }
                            Column(Modifier.weight(1f).padding(start=10.dp)){
                                Text("Perfil da coleção",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$totalCaptured Pokémon registrados",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
                            }
                            Icon(if(showCollectionProfile)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
                        }
                        if(showCollectionProfile){
                            HorizontalDivider(Modifier.padding(vertical=10.dp))
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                CompanionMetric("Shiny",shinyTotal.toString(),"Registrados",Modifier.weight(1f))
                                CompanionMetric("Formas",formTotal.toString(),"Colecionadas",Modifier.weight(1f))
                                CompanionMetric("Jogos",AppGameCatalog.adventureGames.size.toString(),"Na Jornada",Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item{Spacer(Modifier.height(20.dp))}
        }
    }
}

@Composable
private fun CompanionProgressMini(
    label:String,
    value:String,
    progress:Float,
    modifier:Modifier=Modifier
){
    Surface(modifier=modifier,shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.52f)){
        Column(Modifier.padding(horizontal=10.dp,vertical=9.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(value,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Black)
            }
            LinearProgressIndicator(
                progress={progress.coerceIn(0f,1f)},
                modifier=Modifier.fillMaxWidth().padding(top=7.dp).height(6.dp),
                strokeCap=androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
private fun CompanionMetricGroup(
    journeyValue:String,
    journeySubtitle:String,
    dexValue:String,
    dexSubtitle:String,
    pendingValue:String,
    pendingSubtitle:String
){
    BoxWithConstraints(Modifier.fillMaxWidth()){
        if(maxWidth<360.dp){
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    CompanionMetric("Jornada",journeyValue,journeySubtitle,Modifier.weight(1f))
                    CompanionMetric("Pokédex",dexValue,dexSubtitle,Modifier.weight(1f))
                }
                CompanionMetric("Pendência",pendingValue,pendingSubtitle,Modifier.fillMaxWidth())
            }
        }else{
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                CompanionMetric("Jornada",journeyValue,journeySubtitle,Modifier.weight(1f))
                CompanionMetric("Pokédex",dexValue,dexSubtitle,Modifier.weight(1f))
                CompanionMetric("Pendência",pendingValue,pendingSubtitle,Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompanionMetric(label:String,value:String,subtitle:String,modifier:Modifier=Modifier){
    Surface(modifier=modifier,shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.52f)){
        Column(Modifier.padding(horizontal=10.dp,vertical=10.dp)){
            Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
            Text(value,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
            Text(subtitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun JourneyGameReferenceCard(
    game: AppGame,
    progress: JourneyCollectionProgress,
    onClick: () -> Unit
) {
    val heroIds = JourneyGameVisualCatalog.forGame(game.label).heroPokemonIds
    val routeRevision = JourneyProgressStore.revision
    val routeSteps = remember(game.label, routeRevision) { JourneyCatalog.steps(game.label) }
    val completedSteps = remember(game.label, routeRevision) { JourneyProgressStore.completed(game.label) }
    val nextJourneyStep = remember(routeSteps, completedSteps) { routeSteps.firstOrNull { it.id !in completedSteps } }
    val journeyDone = remember(routeSteps, completedSteps) { DataIntegrityRules.completedCount(routeSteps.map { it.id }, completedSteps) }
    val journeyRatio = if(routeSteps.isEmpty()) 0f else journeyDone.toFloat()/routeSteps.size
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        val cardHeight = if (compact) PokedexDesignTokens.Journey.CardHeightCompact else PokedexDesignTokens.Journey.CardHeight
        val coverWidth = if (compact) PokedexDesignTokens.Journey.CoverWidthCompact else PokedexDesignTokens.Journey.CoverWidth
        val heroWidth = if (compact) PokedexDesignTokens.Journey.HeroWidthCompact else PokedexDesignTokens.Journey.HeroWidth
        val fadeWidth = if (compact) PokedexDesignTokens.Journey.FadeWidthCompact else PokedexDesignTokens.Journey.FadeWidth

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(PokedexDesignTokens.Journey.CardRadius),
            colors = CardDefaults.cardColors(containerColor = PokedexDesignTokens.Journey.CardSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = PokedexDesignTokens.Elevation.Low)
        ) {
            Box(Modifier.fillMaxSize()) {
            JourneyHeroArtwork(
                ids = heroIds,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(heroWidth)
            )

            Box(
                Modifier
                    .fillMaxHeight()
                    .width(fadeWidth)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                PokedexDesignTokens.Journey.CardSurface,
                                PokedexDesignTokens.Journey.CardSurface.copy(alpha = .74f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = PokedexDesignTokens.Journey.CardHorizontalPadding,
                        vertical = PokedexDesignTokens.Journey.CardVerticalPadding
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                JourneyGameCover(
                    gameLabel = game.label,
                    modifier = Modifier
                        .width(coverWidth)
                        .fillMaxHeight()
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, end = 10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = game.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    if(routeSteps.isNotEmpty()){
                        Text(
                            text = nextJourneyStep?.let { "Próximo: " + it.title } ?: "Jornada principal concluída",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        nextJourneyStep?.let { step ->
                            Text(
                                text = JourneyTeamProgressCatalog.chapterFor(step.id) + " · " + step.levelLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top=6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(
                                progress = { journeyRatio },
                                modifier = Modifier.weight(1f).height(7.dp),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = journeyDone.toString() + "/" + routeSteps.size,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier
                                    .padding(start = 2.dp, end = 2.dp)
                                    .widthIn(min = 42.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }

                }

            }
        }
    }
    }
}



private object JourneyLocalArtworkCache {
    @Volatile
    private var scarletBytes: ByteArray? = null

    fun scarlet(context: android.content.Context): ByteArray? {
        scarletBytes?.let { return it }
        return synchronized(this) {
            scarletBytes ?: runCatching {
                val encoded = (1..3).joinToString(separator = "") { part ->
                    context.assets.open("journey/scarlet_user_art_" + part + ".b64")
                        .bufferedReader()
                        .use { it.readText() }
                }
                Base64.decode(encoded, Base64.DEFAULT).also { scarletBytes = it }
            }.getOrNull()
        }
    }
}

private fun compactJourneyRegionLabel(label: String): String = when (label) {
    "Isle of Armor" -> "ARMOR"
    "Crown Tundra" -> "TUNDRA"
    "Hyperspace" -> "HYPERSPACE"
    else -> label.uppercase()
}

@Composable
private fun JourneyHeroArtwork(
    ids: List<Int>,
    modifier: Modifier = Modifier,
    alphaScale: Float = 1f
) {
    if (ids.isEmpty()) return
    Box(modifier) {
        ids.take(2).forEachIndexed { index, id ->
            AsyncImage(
                model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" + id + ".png",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (ids.size > 1) .72f else 1f)
                    .align(if (index == 0) Alignment.CenterEnd else Alignment.CenterStart)
                    .alpha((if (ids.size > 1) .34f else .28f) * alphaScale),
                contentScale = ContentScale.Fit
            )
        }
    }
}


@Composable
private fun JourneyGameCover(
    gameLabel: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userScarletArtwork = remember(gameLabel, context.applicationContext) {
        if (gameLabel == "Scarlet / Violet") {
            JourneyLocalArtworkCache.scarlet(context.applicationContext)
        } else null
    }
    val covers = GameCoverCatalog.coversFor(gameLabel)
    if (covers.isEmpty()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(PokedexDesignTokens.Journey.ArtworkRadius))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SportsEsports, contentDescription = null)
        }
        return
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PokedexDesignTokens.Journey.ArtworkRadius))
            .background(PokedexDesignTokens.Journey.ArtworkBackdrop)
    ) {
        if (userScarletArtwork != null) {
            AsyncImage(
                model = userScarletArtwork,
                contentDescription = "Arte enviada pelo usuário para Scarlet / Violet",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Row(Modifier.fillMaxSize()) {
                covers.take(2).forEach { cover ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        AsyncImage(
                            model = cover,
                            contentDescription = "Arte oficial de $gameLabel",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = .04f),
                            Color.Black.copy(alpha = .16f)
                        )
                    )
                )
        )
    }
}

@Composable
internal fun JourneyGameMenu(
    game:AppGame,
    onBack:()->Unit,
    onRoute:()->Unit,
    onTeam:()->Unit,
    onBoxes:()->Unit,
    onRegion:(String)->Unit
){
    val accent=PokedexDesignTokens.Colors.game(game.label)
    val route=JourneyCatalog.steps(game.label)
    val routeRevision=JourneyProgressStore.revision
    val completed=remember(game.label,routeRevision){JourneyProgressStore.completed(game.label)}
    val routeDone=DataIntegrityRules.completedCount(route.map{it.id},completed)
    val routeProgress=if(route.isEmpty())0f else routeDone.toFloat()/route.size
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.background,
                    accent.copy(alpha=.12f),
                    MaterialTheme.colorScheme.background
                )
            )
        )
    ){
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=16.dp,vertical=14.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        item{
            Card(
                shape=RoundedCornerShape(26.dp),
                colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface.copy(alpha=.96f)),
                elevation=CardDefaults.cardElevation(defaultElevation=PokedexDesignTokens.Elevation.Low)
            ){
                Column(Modifier.fillMaxWidth().padding(16.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Voltar")}
                        Column(Modifier.weight(1f).padding(start=4.dp)){
                            Text(game.label,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall)
                            Text("Central da Jornada",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(shape=RoundedCornerShape(999.dp),color=accent.copy(alpha=.14f)){
                            Text(
                                (routeProgress*100).toInt().toString()+"%",
                                Modifier.padding(horizontal=10.dp,vertical=6.dp),
                                style=MaterialTheme.typography.labelMedium,
                                fontWeight=FontWeight.Bold,
                                color=accent
                            )
                        }
                    }
                    if(route.isNotEmpty()){
                        LinearProgressIndicator(
                            progress={routeProgress},
                            modifier=Modifier.fillMaxWidth().padding(top=10.dp).height(7.dp),
                            strokeCap=androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(
                            routeDone.toString()+" de "+route.size+" objetivos concluídos",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier=Modifier.padding(top=7.dp)
                        )
                        val nextStep=route.firstOrNull{it.id !in completed}
                        nextStep?.let{step->
                            Surface(
                                shape=RoundedCornerShape(16.dp),
                                color=accent.copy(alpha=.12f),
                                modifier=Modifier.fillMaxWidth().padding(top=10.dp)
                            ){
                                Column(Modifier.padding(horizontal=12.dp,vertical=9.dp)){
                                    Text("PRÓXIMO OBJETIVO",style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Black,color=accent)
                                    Text(step.title,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleSmall,modifier=Modifier.padding(top=2.dp))
                                    Text(JourneyTeamProgressCatalog.chapterFor(step.id)+" · "+step.levelLabel,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        item{
            JourneyActionCard(
                icon=Icons.Default.Route,
                title="Melhor rota",
                subtitle=if(route.isNotEmpty()) "Sequência recomendada por nível, com progresso salvo." else "Estrutura pronta; rota detalhada deste jogo entra na próxima curadoria.",
                enabled=route.isNotEmpty(),
                onClick=onRoute
            )
        }
        item{
            JourneyActionCard(
                icon=Icons.Default.Groups,
                title="Time ideal",
                subtitle="Escolha o inicial e a fase da história. Veja trocas, golpes, item e função de cada Pokémon.",
                onClick=onTeam
            )
        }
        item{
            val captured=CollectionStore.contextualCapturedIds
            val boxProgress by rememberJourneyCollectionProgress(game,captured)
            Card(
                Modifier.fillMaxWidth().clickable(onClick=onBoxes),
                shape=RoundedCornerShape(22.dp),
                colors=CardDefaults.cardColors(containerColor=accent.copy(alpha=.11f))
            ){
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                    Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.72f)){
                        Icon(Icons.Default.GridView,null,Modifier.padding(13.dp))
                    }
                    Column(Modifier.weight(1f).padding(horizontal=12.dp)){
                        Text("Boxes do jogo",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                        if(boxProgress.total>0){
                            Text(
                                boxProgress.captured.toString()+" de "+boxProgress.total+" Pokémon · "+(boxProgress.ratio*100).toInt()+"%",
                                style=MaterialTheme.typography.bodySmall
                            )
                            LinearProgressIndicator(
                                progress={boxProgress.ratio},
                                modifier=Modifier.fillMaxWidth().padding(top=8.dp).height(7.dp),
                                strokeCap=androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }else{
                            Text("Abra a coleção principal deste jogo.",style=MaterialTheme.typography.bodySmall)
                        }
                    }
                    Icon(Icons.Default.ChevronRight,null)
                }
            }
        }
        if(game.regions.isNotEmpty()){
            item{Text("Regiões e conteúdos",fontWeight=FontWeight.Bold)}
            items(game.regions,key={it.source}){region->
                Card(Modifier.fillMaxWidth().clickable{onRegion(region.source)},shape=RoundedCornerShape(18.dp)){
                    Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.Map,null)
                        Column(Modifier.weight(1f).padding(start=10.dp)){
                            Text(region.label,fontWeight=FontWeight.SemiBold)
                            Text(region.subtitle,style=MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ChevronRight,null)
                    }
                }
            }
        }
        item{Spacer(Modifier.height(20.dp))}
    }
    }
}


private data class JourneyCollectionProgress(
    val captured:Int=0,
    val total:Int=0,
){
    val ratio:Float get()=if(total<=0)0f else captured.toFloat()/total
}

@Composable
private fun rememberJourneyDexIdsBySource(game: AppGame?): State<Map<String, Set<Int>>> {
    val state=remember(game?.label){mutableStateOf<Map<String,Set<Int>>>(emptyMap())}
    LaunchedEffect(game?.label){
        state.value=withContext(Dispatchers.IO){
            game?.regions.orEmpty().associate { region ->
                val ctx=GameContext.fromSource(region.source)
                region.source to if(ctx==null) emptySet() else runCatching{
                    GameDexService.loadGameDex(ctx).map{it.nationalId}.toSet()
                }.getOrDefault(emptySet())
            }
        }
    }
    return state
}

@Composable
private fun rememberJourneyDexIdsByGame(): State<Map<String, Set<Int>>> =
    produceState(initialValue = emptyMap()) {
        value = withContext(Dispatchers.IO) {
            AppGameCatalog.adventureGames.associate { game ->
                game.label to game.regions.flatMap { region ->
                    val ctx = GameContext.fromSource(region.source)
                    if (ctx == null) emptyList()
                    else runCatching { GameDexService.loadGameDex(ctx).map { it.nationalId } }
                        .getOrDefault(emptyList())
                }.toSet()
            }
        }
    }

@Composable
private fun rememberJourneyCollectionProgress(
    game: AppGame,
    capturedBySource: Map<String, Set<Int>>,
    ids: Set<Int>
): JourneyCollectionProgress {
    val registered = remember(game.label, capturedBySource) {
        game.regions.flatMap { capturedBySource[it.source].orEmpty() }.toSet()
    }
    return remember(ids, registered) {
        JourneyCollectionProgress(
            captured=ids.count { it in registered },
            total=ids.size
        )
    }
}

@Composable
private fun rememberJourneyCollectionProgress(
    game:AppGame,
    capturedBySource:Map<String,Set<Int>>
):State<JourneyCollectionProgress>{
    var ids by remember(game.label){mutableStateOf<Set<Int>>(emptySet())}
    LaunchedEffect(game.label){
        ids=withContext(Dispatchers.IO){
            game.regions.flatMap{region->
                val ctx=GameContext.fromSource(region.source)
                if(ctx==null) emptyList() else runCatching{GameDexService.loadGameDex(ctx).map{it.nationalId}}.getOrDefault(emptyList())
            }.toSet()
        }
    }
    val registered=remember(game.label,capturedBySource){
        game.regions.flatMap{capturedBySource[it.source].orEmpty()}.toSet()
    }
    return remember(ids,registered){
        mutableStateOf(
            JourneyCollectionProgress(
                captured=ids.count{it in registered},
                total=ids.size
            )
        )
    }
}

@Composable
private fun JourneyActionCard(
    icon:androidx.compose.ui.graphics.vector.ImageVector,
    title:String,
    subtitle:String,
    enabled:Boolean=true,
    onClick:()->Unit
){
    Card(
        Modifier.fillMaxWidth().clickable(enabled=enabled,onClick=onClick),
        shape=RoundedCornerShape(22.dp),
        colors=CardDefaults.cardColors(containerColor=if(enabled)MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceVariant)
    ){
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.primaryContainer){
                Icon(icon,null,Modifier.padding(13.dp),tint=MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f).padding(horizontal=12.dp)){
                Text(title,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                Text(subtitle,style=MaterialTheme.typography.bodySmall)
            }
            if(enabled)Icon(Icons.Default.ChevronRight,null)
        }
    }
}
