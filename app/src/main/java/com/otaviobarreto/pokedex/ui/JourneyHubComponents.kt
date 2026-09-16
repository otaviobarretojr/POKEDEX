package com.otaviobarreto.pokedex.ui

import android.util.Base64

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JourneyGamePicker(
    onSelect:(String)->Unit,
    onPokemonClick:(Int,String?)->Unit,
    onOpenBoxes:(String,String?)->Unit
){
    val activeGamePreview=AppGameCatalog.adventureGames
        .firstOrNull{it.label==AppStatePreferences.activeGame}
        ?.takeIf{JourneyProgressStore.isStarted(it.label)}
    val dexIdsBySource by rememberJourneyDexIdsBySource(activeGamePreview)
    val activeGame=activeGamePreview
    val activeSources=activeGame?.regions?.map{it.source}.orEmpty()
    val activeRegionSource=activeGame?.let{game->
        AppStatePreferences.activeRegionForGame(game.label)
            ?.takeIf{it in activeSources}
            ?: game.regions.firstOrNull()?.source
    }
    val journeyRevision=JourneyProgressStore.revision
    val activeSteps=remember(activeGame?.label,journeyRevision){activeGame?.let{JourneyCatalog.steps(it.label)}.orEmpty()}
    val activeCompleted=remember(activeGame?.label,journeyRevision){activeGame?.let{JourneyProgressStore.completed(it.label)}.orEmpty()}
    val nextStep=remember(activeSteps,activeCompleted){activeSteps.firstOrNull{it.id !in activeCompleted}}
    val journeyDone=remember(activeSteps,activeCompleted){DataIntegrityRules.completedCount(activeSteps.map{it.id},activeCompleted)}
    val journeyRatio=if(activeSteps.isEmpty())0f else journeyDone.toFloat()/activeSteps.size
    val animatedJourneyRatio by animateFloatAsState(targetValue=journeyRatio,label="companionJourney")
    val capturedBySource=CollectionStore.contextualCapturedIds
    val gameDexIds=remember(activeGame?.label,dexIdsBySource){activeGame?.regions.orEmpty().flatMap{dexIdsBySource[it.source].orEmpty()}.toSet()}
    val gameCapturedIds=remember(activeGame?.label,capturedBySource){activeGame?.regions.orEmpty().flatMap{capturedBySource[it.source].orEmpty()}.toSet()}
    val gameDexTotal=gameDexIds.size
    val gameDexCaptured=remember(gameDexIds,gameCapturedIds){gameDexIds.count{it in gameCapturedIds}}
    val gameDexRatio=if(gameDexTotal<=0)0f else gameDexCaptured.toFloat()/gameDexTotal
    val animatedDexRatio by animateFloatAsState(targetValue=gameDexRatio,label="companionDex")

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
                    Text(
                        if(activeGame==null) "Escolha um jogo para começar uma nova Jornada."
                        else "Continue exatamente de onde parou.",
                        style=MaterialTheme.typography.bodyMedium,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if(activeGame==null){
                item(key="no_active_journey"){
                    Surface(
                        modifier=Modifier.fillMaxWidth(),
                        shape=RoundedCornerShape(20.dp),
                        color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.45f)
                    ){
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Icon(Icons.Default.PlayCircle,null,Modifier.size(28.dp),tint=MaterialTheme.colorScheme.primary)
                            Column(Modifier.padding(start=12.dp)){
                                Text("Nenhuma Jornada ativa",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                                Text("Abra um jogo abaixo e toque em Começar Jornada.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            activeGame?.let{game->
                item(key="active_companion"){
                    val accent=PokedexDesignTokens.Colors.game(game.label)
                    Surface(
                        modifier=Modifier.fillMaxWidth(),
                        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Xl),
                        color=MaterialTheme.colorScheme.surface
                    ){
                        Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){
                            Crossfade(targetState=game.label,label="activeGameHero"){heroGameLabel->
                                val denseCover=heroGameLabel=="Scarlet / Violet" || GameCoverCatalog.coversFor(heroGameLabel).size>=2
                                Box(
                                    Modifier.fillMaxWidth().height(170.dp)
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
                            Spacer(Modifier.height(12.dp))
                            Text(
                                game.label,
                                style=MaterialTheme.typography.headlineSmall,
                                fontWeight=FontWeight.Black,
                                maxLines=1,
                                overflow=TextOverflow.Ellipsis
                            )
                            Text(
                                "Seu Companion de aventura",
                                style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier=Modifier.padding(top=2.dp)
                            )
                            nextStep?.let{step->
                                CompanionSectionHeader(
                                    title="Próximo objetivo",
                                    supporting="O que fazer agora",
                                    modifier=Modifier.padding(top=10.dp,bottom=2.dp)
                                )
                                JourneyObjectivePreviewCard(
                                    step=step,
                                    accent=accent,
                                    modifier=Modifier.fillMaxWidth()
                                )
                            } ?: Text(
                                "Jornada principal concluída",
                                style=MaterialTheme.typography.bodyMedium,
                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier=Modifier.padding(top=6.dp)
                            )
                            CompanionProgressSection(
                                game=game,
                                journeyRatio=animatedJourneyRatio,
                                journeyDone=journeyDone,
                                journeyTotal=activeSteps.size,
                                dexIdsBySource=dexIdsBySource,
                                capturedBySource=capturedBySource,
                                dexRatio=animatedDexRatio,
                                dexCaptured=gameDexCaptured,
                                dexTotal=gameDexTotal,
                                accent=accent,
                                modifier=Modifier.fillMaxWidth().padding(top=12.dp)
                            )
                            PrimaryCompanionAction(
                                label="Continuar Jornada",
                                onClick={onSelect(game.label)},
                                modifier=Modifier.padding(top=14.dp),
                                leading={Icon(Icons.Default.Explore,null,Modifier.size(18.dp))}
                            )
                        }
                    }
                }
            }

            item{
                CompanionSectionHeader(
                    title=if(activeGame==null)"Escolha um jogo" else "Outras jornadas",
                    supporting=if(activeGame==null)"Comece uma aventura para ativar seu Companion." else "Continue ou inicie outra aventura quando quiser.",
                    modifier=Modifier.padding(top=4.dp,bottom=1.dp)
                )
            }
            item(key="game_showcase"){
                var showAllGames by rememberSaveable { mutableStateOf(false) }
                val availableGames=AppGameCatalog.adventureGames.filter{it.label!=activeGame?.label}
                Column(Modifier.fillMaxWidth()){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text("Jogos",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black,modifier=Modifier.weight(1f))
                        TextButton(onClick={showAllGames=true}){ Text("Ver todos") }
                    }
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=4.dp)){
                        items(availableGames.take(3),key={it.label}){game->
                            JourneyGamePosterCard(game=game,onClick={onSelect(game.label)},modifier=Modifier.width(176.dp))
                        }
                    }
                }
                if(showAllGames){
                    JourneyAllGamesDialog(games=AppGameCatalog.adventureGames,onDismiss={showAllGames=false},onSelect={label->showAllGames=false;onSelect(label)})
                }
            }
            item{Spacer(Modifier.height(20.dp))}
        }

    }
}

@Composable
private fun CompanionProgressSection(
    game:AppGame,
    journeyRatio:Float,
    journeyDone:Int,
    journeyTotal:Int,
    dexIdsBySource:Map<String,Set<Int>>,
    capturedBySource:Map<String,Set<Int>>,
    dexRatio:Float,
    dexCaptured:Int,
    dexTotal:Int,
    accent:Color,
    modifier:Modifier=Modifier
){
    Surface(modifier=modifier,shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.28f)){
        Column(Modifier.fillMaxWidth().padding(horizontal=13.dp,vertical=12.dp)){
            CompanionSectionHeader(
                title="SEU PROGRESSO",
                supporting="Jornada e Pokédex deste jogo"
            )
            Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text("Jornada",style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text((journeyRatio*100).toInt().toString()+"% · "+journeyDone+"/"+journeyTotal,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Black)
                    }
                    LinearProgressIndicator(progress={journeyRatio.coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(top=5.dp).height(6.dp),strokeCap=StrokeCap.Round)
                }
            }
            HorizontalDivider(Modifier.padding(vertical=10.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f))
            Row(verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.size(66.dp),contentAlignment=Alignment.Center){
                    Canvas(Modifier.fillMaxSize()){
                        val stroke=8.dp.toPx()
                        drawArc(color=accent.copy(alpha=.15f),startAngle=-90f,sweepAngle=360f,useCenter=false,style=Stroke(stroke,cap=StrokeCap.Round))
                        drawArc(color=accent,startAngle=-90f,sweepAngle=360f*dexRatio.coerceIn(0f,1f),useCenter=false,style=Stroke(stroke,cap=StrokeCap.Round))
                    }
                    Text((dexRatio*100).toInt().toString()+"%",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                }
                Column(Modifier.weight(1f).padding(start=13.dp)){
                    Text("Pokédex do jogo",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                    Text(if(dexTotal>0) dexCaptured.toString()+" de "+dexTotal+" registrados" else "Carregando progresso…",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if(game.regions.size>1 && dexTotal>0){
                Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    game.regions.forEach{region->
                        val ids=dexIdsBySource[region.source].orEmpty()
                        if(ids.isNotEmpty()){
                            val caught=ids.count{it in capturedBySource[region.source].orEmpty()}
                            val regionalRatio=caught.toFloat()/ids.size
                            Surface(modifier=Modifier.weight(1f),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.72f)){
                                Column(Modifier.padding(horizontal=8.dp,vertical=7.dp)){
                                    Text(region.label,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
                                    LinearProgressIndicator(progress={regionalRatio},modifier=Modifier.fillMaxWidth().padding(top=4.dp).height(4.dp),strokeCap=StrokeCap.Round)
                                    Text(caught.toString()+"/"+ids.size,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=3.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyObjectivePreviewCard(
    step:JourneyStep,
    accent:Color,
    modifier:Modifier=Modifier
){
    val preview=JourneyObjectivePreviewCatalog.forStep(step) ?: return
    Surface(
        modifier=modifier,
        shape=RoundedCornerShape(18.dp),
        color=accent.copy(alpha=.08f)
    ){
        Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Surface(shape=RoundedCornerShape(12.dp),color=accent.copy(alpha=.14f)){
                    Icon(
                        imageVector=when(step.kind){
                            JourneyChallengeKind.GYM->Icons.Default.EmojiEvents
                            JourneyChallengeKind.TITAN->Icons.Default.Landscape
                            JourneyChallengeKind.STAR->Icons.Default.Groups
                            else->Icons.Default.Flag
                        },
                        contentDescription=null,
                        modifier=Modifier.padding(8.dp).size(18.dp),
                        tint=accent
                    )
                }
                Column(Modifier.weight(1f).padding(start=10.dp)){
                    Text(
                        preview.label,
                        style=MaterialTheme.typography.titleSmall,
                        fontWeight=FontWeight.Black
                    )
                    Text(
                        preview.subtitle,
                        style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines=1,
                        overflow=TextOverflow.Ellipsis
                    )
                }
            }
            if(preview.members.isNotEmpty()){
                Row(
                    Modifier.fillMaxWidth().padding(top=9.dp),
                    horizontalArrangement=Arrangement.spacedBy(7.dp)
                ){
                    preview.members.take(5).forEach{member->
                        Surface(
                            modifier=Modifier.weight(1f),
                            shape=RoundedCornerShape(12.dp),
                            color=MaterialTheme.colorScheme.surface.copy(alpha=.86f)
                        ){
                            Column(
                                Modifier.padding(vertical=6.dp),
                                horizontalAlignment=Alignment.CenterHorizontally
                            ){
                                AsyncImage(
                                    model="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/"+member.pokemonId+".png",
                                    contentDescription=PokemonRepository.byId(member.pokemonId)?.name,
                                    modifier=Modifier.size(38.dp),
                                    contentScale=ContentScale.Fit
                                )
                                Text(
                                    member.level,
                                    style=MaterialTheme.typography.labelSmall,
                                    fontWeight=FontWeight.Bold,
                                    textAlign=TextAlign.Center
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
private fun CompanionProgressMini(
    label:String,
    value:String,
    progress:Float,
    modifier:Modifier=Modifier
){
    Surface(modifier=modifier,shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.52f)){
        Column(
            Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp),
            horizontalAlignment=Alignment.CenterHorizontally
        ){
            Text(
                label,
                style=MaterialTheme.typography.labelSmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines=1,
                overflow=TextOverflow.Ellipsis,
                textAlign=TextAlign.Center,
                modifier=Modifier.fillMaxWidth()
            )
            Text(
                value,
                style=MaterialTheme.typography.titleSmall,
                fontWeight=FontWeight.Black,
                textAlign=TextAlign.Center,
                modifier=Modifier.fillMaxWidth().padding(top=2.dp)
            )
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
    dexSubtitle:String
){
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement=Arrangement.spacedBy(10.dp)
    ){
        CompanionMetric("Jornada",journeyValue,journeySubtitle,Modifier.weight(1f))
        CompanionMetric("Pokédex",dexValue,dexSubtitle,Modifier.weight(1f))
    }
}

@Composable
private fun CompanionMetric(label:String,value:String,subtitle:String,modifier:Modifier=Modifier){
    Surface(
        modifier=modifier.heightIn(min=86.dp),
        shape=RoundedCornerShape(16.dp),
        color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.52f)
    ){
        Column(
            Modifier.fillMaxSize().padding(horizontal=8.dp,vertical=10.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.Center
        ){
            Text(
                label,
                style=MaterialTheme.typography.labelSmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines=1,
                textAlign=TextAlign.Center,
                modifier=Modifier.fillMaxWidth()
            )
            Text(
                value,
                style=MaterialTheme.typography.titleMedium,
                fontWeight=FontWeight.Black,
                textAlign=TextAlign.Center,
                modifier=Modifier.fillMaxWidth().padding(vertical=2.dp)
            )
            Text(
                subtitle,
                style=MaterialTheme.typography.labelSmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines=2,
                overflow=TextOverflow.Ellipsis,
                textAlign=TextAlign.Center,
                modifier=Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun JourneyGamePosterCard(game:AppGame,onClick:()->Unit,modifier:Modifier=Modifier){
    val started=JourneyProgressStore.isStarted(game.label)
    Card(modifier=modifier.clickable(onClick=onClick),shape=RoundedCornerShape(22.dp),elevation=CardDefaults.cardElevation(defaultElevation=3.dp)){
        Column{
            JourneyGameCover(gameLabel=game.label,modifier=Modifier.fillMaxWidth().aspectRatio(.70f))
            Column(Modifier.padding(12.dp)){
                Text(game.label,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(if(started)"Continuar Jornada" else "Começar Jornada",style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=6.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JourneyAllGamesDialog(games:List<AppGame>,onDismiss:()->Unit,onSelect:(String)->Unit){
    ModalBottomSheet(onDismissRequest=onDismiss,dragHandle={BottomSheetDefaults.DragHandle()}){
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f).padding(horizontal=16.dp)){
            Text("Jogos Pokémon",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
            Text("Escolha uma aventura",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(bottom=14.dp))
            LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=32.dp)){
                items(games.chunked(2)){row->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                        row.forEach{game->JourneyGamePosterCard(game,onClick={onSelect(game.label)},modifier=Modifier.weight(1f))}
                        if(row.size==1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyGameReferenceCard(
    game: AppGame,
    onClick: () -> Unit
) {
    val heroIds = JourneyGameVisualCatalog.forGame(game.label).heroPokemonIds
    val routeRevision = JourneyProgressStore.revision
    val routeSteps = remember(game.label, routeRevision) { JourneyCatalog.steps(game.label) }
    val completedSteps = remember(game.label, routeRevision) { JourneyProgressStore.completed(game.label) }
    val nextJourneyStep = remember(routeSteps, completedSteps) { routeSteps.firstOrNull { it.id !in completedSteps } }
    val journeyDone = remember(routeSteps, completedSteps) { DataIntegrityRules.completedCount(routeSteps.map { it.id }, completedSteps) }
    val journeyRatio = if(routeSteps.isEmpty()) 0f else journeyDone.toFloat()/routeSteps.size
    val started=JourneyProgressStore.isStarted(game.label)
    val configuring=JourneyProgressStore.isConfiguring(game.label)
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
                    when{
                        configuring->{
                            Text(
                                "Configuração pendente",
                                style=MaterialTheme.typography.labelMedium,
                                fontWeight=FontWeight.Black,
                                color=MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                "Escolha o inicial e confirme para iniciar.",
                                style=MaterialTheme.typography.labelSmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines=2,
                                overflow=TextOverflow.Ellipsis,
                                modifier=Modifier.padding(top=2.dp)
                            )
                        }
                        !started->{
                            Text(
                                "Começar Jornada",
                                style=MaterialTheme.typography.labelMedium,
                                fontWeight=FontWeight.Black,
                                color=MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Escolha seu inicial e monte o primeiro time.",
                                style=MaterialTheme.typography.labelSmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines=2,
                                overflow=TextOverflow.Ellipsis,
                                modifier=Modifier.padding(top=2.dp)
                            )
                        }
                        routeSteps.isNotEmpty()->{
                            Text(
                                text = nextJourneyStep?.let { "Próximo: " + it.title } ?: "Jornada concluída",
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
                                    modifier = Modifier.widthIn(min = 42.dp),
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
    val pairedCovers = covers.size >= 2
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
        } else if (pairedCovers) {
            Row(
                Modifier.fillMaxSize().padding(horizontal=4.dp,vertical=3.dp),
                horizontalArrangement=Arrangement.spacedBy(3.dp)
            ) {
                covers.take(2).forEach { cover ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = cover,
                            contentDescription = "Arte oficial de $gameLabel",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        } else {
            AsyncImage(
                model = covers.first(),
                contentDescription = "Arte oficial de $gameLabel",
                modifier = Modifier.fillMaxSize().padding(3.dp),
                contentScale = ContentScale.Fit
            )
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
    val started=remember(game.label,routeRevision){JourneyProgressStore.isStarted(game.label)}
    val configuring=remember(game.label,routeRevision){JourneyProgressStore.isConfiguring(game.label)}
    val routeDone=DataIntegrityRules.completedCount(route.map{it.id},completed)
    val routeProgress=if(route.isEmpty())0f else routeDone.toFloat()/route.size
    val starterOptions=remember(game.label){TeamCampaignCatalog.starters(game.label)}
    var selectedStarterId by rememberSaveable(game.label){
        mutableIntStateOf(AppStatePreferences.journeyStarterForGame(game.label) ?: -1)
    }
    val smart=remember(game.label,routeRevision){JourneySmartProgress.context(game.label)}
    val setupPhase=if(started)smart.phase else CampaignPhase.EARLY
    val baseSuggestedTeam=remember(game.label,selectedStarterId,setupPhase){
        if(selectedStarterId>0) TeamCampaignCatalog.preset(game.label,selectedStarterId,setupPhase) else null
    }
    val dynamicSuggestedTeam=remember(game.label,selectedStarterId,routeRevision,started){
        if(started && selectedStarterId>0) JourneyDynamicTeamCatalog.suggestion(game.label,selectedStarterId) else null
    }
    val suggestedTeam=baseSuggestedTeam?.let{base->
        if(dynamicSuggestedTeam!=null && dynamicSuggestedTeam.preset?.phase==base.phase){
            base.copy(slots=dynamicSuggestedTeam.adjustedSlots)
        }else base
    }
    val active=started && AppStatePreferences.activeGame==game.label
    var confirmReset by rememberSaveable(game.label){mutableStateOf(false)}
    var confirmEnd by rememberSaveable(game.label){mutableStateOf(false)}

    if(confirmReset){
        AlertDialog(
            onDismissRequest={confirmReset=false},
            title={Text("Reiniciar progresso?")},
            text={Text("A Jornada continuará ativa, mas todos os objetivos concluídos voltarão a ficar pendentes.")},
            confirmButton={TextButton(onClick={JourneyProgressStore.resetProgress(game.label);confirmReset=false}){Text("Reiniciar")}},
            dismissButton={TextButton(onClick={confirmReset=false}){Text("Cancelar")}}
        )
    }
    if(confirmEnd){
        AlertDialog(
            onDismissRequest={confirmEnd=false},
            title={Text("Encerrar Jornada?")},
            text={Text("O progresso desta Jornada será removido. Você poderá começar novamente depois.")},
            confirmButton={TextButton(onClick={
                JourneyProgressStore.endJourney(game.label)
                AppStatePreferences.clearJourneyStarterForGame(game.label)
                selectedStarterId=-1
                confirmEnd=false
            }){Text("Encerrar")}},
            dismissButton={TextButton(onClick={confirmEnd=false}){Text("Cancelar")}}
        )
    }

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
                                Text(
                                    when{
                                        configuring -> "Configuração da Jornada"
                                        !started -> "Nova Jornada"
                                        active -> "Jogo atual · Central da Jornada"
                                        else -> "Jornada salva"
                                    },
                                    style=MaterialTheme.typography.labelMedium,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(shape=RoundedCornerShape(999.dp),color=accent.copy(alpha=.14f)){
                                Text(
                                    when{
                                        configuring -> "CONFIGURANDO"
                                        !started -> "NOVO"
                                        active -> "ATUAL"
                                        routeProgress>=1f -> "100%"
                                        else -> (routeProgress*100).toInt().toString()+"%"
                                    },
                                    Modifier.padding(horizontal=10.dp,vertical=6.dp),
                                    style=MaterialTheme.typography.labelSmall,
                                    fontWeight=FontWeight.Black,
                                    color=accent
                                )
                            }
                        }

                        if(started && route.isNotEmpty()){
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
                            } ?: Text(
                                "Jornada concluída",
                                style=MaterialTheme.typography.titleSmall,
                                fontWeight=FontWeight.Bold,
                                color=accent,
                                modifier=Modifier.padding(top=10.dp)
                            )
                        }
                    }
                }
            }

            if(!started && !configuring){
                item{
                    Card(
                        shape=RoundedCornerShape(22.dp),
                        colors=CardDefaults.cardColors(containerColor=accent.copy(alpha=.11f))
                    ){
                        Column(Modifier.fillMaxWidth().padding(16.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.76f)){
                                    Icon(Icons.Default.PlayCircle,null,Modifier.padding(11.dp),tint=accent)
                                }
                                Column(Modifier.weight(1f).padding(start=12.dp)){
                                    Text("Começar Jornada",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                                    Text("Configure seu inicial e conheça o time sugerido antes de tornar este o jogo atual.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Button(
                                onClick={
                                    AppStatePreferences.clearJourneyStarterForGame(game.label)
                                    selectedStarterId=-1
                                    JourneyProgressStore.beginConfiguration(game.label,reset=true)
                                },
                                modifier=Modifier.fillMaxWidth().padding(top=12.dp).height(50.dp)
                            ){
                                Icon(Icons.Default.PlayArrow,null)
                                Spacer(Modifier.width(7.dp))
                                Text("Configurar Jornada")
                            }
                        }
                    }
                }
            }else if(configuring){
                if(starterOptions.isNotEmpty()){
                    item{
                        JourneyStarterSetupCard(
                            gameLabel=game.label,
                            starters=starterOptions,
                            selectedStarterId=selectedStarterId,
                            accent=accent,
                            onSelect={id->
                                selectedStarterId=id
                                AppStatePreferences.setJourneyStarterForGame(game.label,id)
                            }
                        )
                    }
                }

                suggestedTeam?.let{team->
                    item{
                        JourneyProgressTeamCard(
                            team=team,
                            accent=accent,
                            onOpenTeam=onTeam,
                            stepLabel="2 · Conheça seu time sugerido"
                        )
                    }
                }

                item{
                    Button(
                        onClick={
                            AppStatePreferences.activeGame=game.label
                            AppStatePreferences.setActiveRegionForGame(game.label,game.regions.firstOrNull()?.source)
                            JourneyProgressStore.confirmStart(game.label)
                            onRoute()
                        },
                        enabled=starterOptions.isEmpty() || selectedStarterId>0,
                        modifier=Modifier.fillMaxWidth().height(54.dp)
                    ){
                        Icon(Icons.Default.RocketLaunch,null)
                        Spacer(Modifier.width(8.dp))
                        Text("Iniciar aventura")
                    }
                    Text(
                        if(selectedStarterId>0)"Seu progresso começa no primeiro objetivo e o time será atualizado ao longo da campanha."
                        else "Escolha seu inicial para liberar o início da aventura.",
                        style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier=Modifier.fillMaxWidth().padding(top=6.dp),
                        textAlign=TextAlign.Center
                    )
                    TextButton(
                        onClick={
                            JourneyProgressStore.cancelConfiguration(game.label)
                            AppStatePreferences.clearJourneyStarterForGame(game.label)
                            selectedStarterId=-1
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text("Cancelar configuração")}
                }
            }else{
                suggestedTeam?.let{team->
                    item{
                        JourneyProgressTeamCard(
                            team=team,
                            accent=accent,
                            onOpenTeam=onTeam,
                            stepLabel="Seu time · "+team.phase.label
                        )
                    }
                }

                item{
                    Button(
                        onClick={
                            AppStatePreferences.activeGame=game.label
                            if(AppStatePreferences.activeRegionForGame(game.label)==null){
                                AppStatePreferences.setActiveRegionForGame(game.label,game.regions.firstOrNull()?.source)
                            }
                            onRoute()
                        },
                        modifier=Modifier.fillMaxWidth().height(54.dp)
                    ){
                        Icon(Icons.Default.Explore,null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when{
                                routeProgress>=1f -> "Revisar Jornada"
                                active -> "Continuar Jornada"
                                else -> "Retomar Jornada"
                            }
                        )
                    }
                    Text(
                        if(routeProgress>=1f)"Veja novamente a rota completa."
                        else "Abre diretamente sua rota no objetivo atual.",
                        style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier=Modifier.fillMaxWidth().padding(top=5.dp),
                        textAlign=TextAlign.Center
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
                                        boxProgress.captured.toString()+" de "+boxProgress.total+" Pokémon registrados neste jogo",
                                        style=MaterialTheme.typography.bodySmall
                                    )
                                    LinearProgressIndicator(
                                        progress={boxProgress.ratio},
                                        modifier=Modifier.fillMaxWidth().padding(top=8.dp).height(7.dp),
                                        strokeCap=androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                }else{
                                    Text("Abra a coleção geral deste jogo.",style=MaterialTheme.typography.bodySmall)
                                }
                            }
                            Icon(Icons.Default.ChevronRight,null)
                        }
                    }
                }

                if(game.regions.isNotEmpty()){
                    item{
                        Text(
                            "Regiões e conteúdos",
                            fontWeight=FontWeight.Bold,
                            modifier=Modifier.padding(top=2.dp)
                        )
                        Text(
                            "Atalhos para abrir a Box diretamente em cada região ou DLC.",
                            style=MaterialTheme.typography.labelSmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier=Modifier.padding(top=2.dp)
                        )
                    }
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
            }
            if(started){
                item{
                    Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f))){
                        Column(Modifier.fillMaxWidth().padding(14.dp)){
                            Text("Gerenciar Jornada",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                            Text("Controle o progresso sem afetar suas Boxes ou Pokédex.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=2.dp,bottom=8.dp))
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                OutlinedButton(onClick={confirmReset=true},modifier=Modifier.weight(1f)){Text("Reiniciar")}
                                OutlinedButton(onClick={confirmEnd=true},modifier=Modifier.weight(1f)){Text("Encerrar")}
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
private fun JourneyStarterSetupCard(
    gameLabel:String,
    starters:List<Pair<String,Int>>,
    selectedStarterId:Int,
    accent:Color,
    onSelect:(Int)->Unit
){
    Card(shape=RoundedCornerShape(22.dp)){
        Column(Modifier.fillMaxWidth().padding(14.dp)){
            Text(
                "1 · Escolha seu inicial",
                fontWeight=FontWeight.Black,
                style=MaterialTheme.typography.titleMedium
            )
            Text(
                if(selectedStarterId>0)"Inicial selecionado. Revise o time sugerido abaixo."
                else "A recomendação do time começa pela sua escolha.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                modifier=Modifier.padding(top=2.dp,bottom=10.dp)
            )
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                starters.take(3).forEach{(name,id)->
                    val selected=selectedStarterId==id
                    Surface(
                        modifier=Modifier.weight(1f).clickable{onSelect(id)},
                        shape=RoundedCornerShape(16.dp),
                        color=if(selected)accent.copy(alpha=.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f),
                        border=if(selected)androidx.compose.foundation.BorderStroke(1.5.dp,accent) else null
                    ){
                        Column(
                            Modifier.fillMaxWidth().padding(vertical=9.dp,horizontal=6.dp),
                            horizontalAlignment=Alignment.CenterHorizontally
                        ){
                            AsyncImage(
                                model="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+id+".png",
                                contentDescription=name,
                                modifier=Modifier.size(68.dp),
                                contentScale=ContentScale.Fit
                            )
                            Text(name,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                            if(selected){
                                Icon(Icons.Default.CheckCircle,"Selecionado",Modifier.padding(top=4.dp).size(18.dp),tint=accent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyProgressTeamCard(
    team:CampaignTeamPreset,
    accent:Color,
    onOpenTeam:()->Unit,
    stepLabel:String
){
    Card(
        modifier=Modifier.fillMaxWidth().clickable(onClick=onOpenTeam),
        shape=RoundedCornerShape(22.dp),
        colors=CardDefaults.cardColors(containerColor=accent.copy(alpha=.08f))
    ){
        Column(Modifier.fillMaxWidth().padding(14.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text(stepLabel,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                    Text(
                        "Baseado no seu inicial e no ponto atual da campanha.",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight,null,tint=accent)
            }
            Row(
                Modifier.fillMaxWidth().padding(top=10.dp),
                horizontalArrangement=Arrangement.spacedBy(6.dp)
            ){
                team.slots.take(6).forEach{slot->
                    Surface(
                        modifier=Modifier.weight(1f),
                        shape=RoundedCornerShape(12.dp),
                        color=MaterialTheme.colorScheme.surface.copy(alpha=.82f)
                    ){
                        AsyncImage(
                            model="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+slot.pokemonId+".png",
                            contentDescription=PokemonRepository.byId(slot.pokemonId)?.name,
                            modifier=Modifier.fillMaxWidth().aspectRatio(1f).padding(4.dp),
                            contentScale=ContentScale.Fit
                        )
                    }
                }
            }
            Text(
                when(team.phase){
                    CampaignPhase.EARLY->"Pokémon acessíveis cedo; o app sugere evoluções e trocas conforme você avança."
                    CampaignPhase.MID->"Seu núcleo evoluiu e recebe coberturas melhores para o meio da história."
                    CampaignPhase.LATE->"Composição atualizada para reta final, chefes e encerramento da campanha."
                },
                style=MaterialTheme.typography.labelSmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                modifier=Modifier.padding(top=9.dp)
            )
            Text(
                "Toque para ver o plano completo.",
                style=MaterialTheme.typography.labelSmall,
                fontWeight=FontWeight.Bold,
                color=accent,
                modifier=Modifier.padding(top=4.dp)
            )
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
