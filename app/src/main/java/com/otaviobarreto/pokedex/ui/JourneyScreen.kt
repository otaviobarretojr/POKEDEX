package com.otaviobarreto.pokedex.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.otaviobarreto.pokedex.data.*

private enum class JourneyView { GAMES, GAME_MENU, ROUTE, DETAIL, MAP }

@Composable
fun JourneyScreen(
    onPokemonClick:(Int,String?)->Unit,
    onOpenTeamGuide:(String,String?)->Unit,
    onOpenBoxes:(String,String?)->Unit
){
    var selectedGame by rememberSaveable { mutableStateOf<String?>(null) }
    var view by rememberSaveable { mutableStateOf(JourneyView.GAMES) }
    var selectedStepId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailReturnView by rememberSaveable { mutableStateOf(JourneyView.ROUTE) }
    val routeListState=rememberLazyListState()
    var explicitGameSelectionRevision by remember { mutableIntStateOf(0) }
    var mapSelectedStepId by rememberSaveable { mutableStateOf<String?>(null) }
    var mapSelectionInitialized by rememberSaveable { mutableStateOf(false) }
    var mapZoom by rememberSaveable { mutableFloatStateOf(1f) }
    var mapPanX by rememberSaveable { mutableFloatStateOf(0f) }
    var mapPanY by rememberSaveable { mutableFloatStateOf(0f) }
    val game=AppGameCatalog.adventureGames.firstOrNull{it.label==selectedGame}

    LaunchedEffect(explicitGameSelectionRevision){
        if(explicitGameSelectionRevision==0) return@LaunchedEffect
        routeListState.scrollToItem(0)
        mapSelectedStepId=null
        mapSelectionInitialized=false
        mapZoom=1f
        mapPanX=0f
        mapPanY=0f
    }

    BackHandler(enabled=view!=JourneyView.GAMES){
        when(view){
            JourneyView.GAME_MENU -> { selectedGame=null; view=JourneyView.GAMES }
            JourneyView.ROUTE -> view=JourneyView.GAME_MENU
            JourneyView.MAP -> view=JourneyView.GAME_MENU
            JourneyView.DETAIL -> {
                selectedStepId=null
                view=detailReturnView
            }
            JourneyView.GAMES -> Unit
        }
    }

    when(view){
        JourneyView.GAMES -> JourneyGamePicker(
            onSelect={
                explicitGameSelectionRevision++
                selectedGame=it
                AppStatePreferences.activeGame=it
                view=JourneyView.GAME_MENU
            }
        )
        JourneyView.GAME_MENU -> if(game!=null) JourneyGameMenu(
            game=game,
            onBack={view=JourneyView.GAMES},
            onRoute={view=JourneyView.ROUTE},
            onMap={view=JourneyView.MAP},
            onTeam={onOpenTeamGuide(game.label,JourneySmartProgress.context(game.label).phase.name)},
            onBoxes={onOpenBoxes(game.label,AppStatePreferences.activeRegionForGame(game.label) ?: game.regions.firstOrNull()?.source)},
            onRegion={regionSource->onOpenBoxes(game.label,regionSource)}
        ) else { view=JourneyView.GAMES }
        JourneyView.ROUTE -> if(game!=null) JourneyRoute(
            game=game,
            onBack={view=JourneyView.GAME_MENU},
            onTeam={onOpenTeamGuide(game.label,JourneySmartProgress.context(game.label).phase.name)},
            listState=routeListState,
            onOpenStep={stepId->detailReturnView=JourneyView.ROUTE;selectedStepId=stepId;view=JourneyView.DETAIL}
        ) else { view=JourneyView.GAMES }
        JourneyView.DETAIL -> if(game!=null && selectedStepId!=null){
            val step=JourneyCatalog.steps(game.label).firstOrNull{it.id==selectedStepId}
            if(step!=null) JourneyObjectiveDetailScreen(
                game=game,
                step=step,
                onBack={selectedStepId=null;view=detailReturnView},
                onTeam={onOpenTeamGuide(game.label,JourneySmartProgress.context(game.label).phase.name)},
                onPokemonClick=onPokemonClick
            ) else view=JourneyView.ROUTE
        } else { view=JourneyView.GAMES }
        JourneyView.MAP -> if(game!=null) JourneyMapScreen(
            game=game,
            selectedStepId=mapSelectedStepId,
            selectionInitialized=mapSelectionInitialized,
            zoom=mapZoom,
            pan=Offset(mapPanX,mapPanY),
            onSelectedStepChange={mapSelectedStepId=it},
            onSelectionInitialized={mapSelectionInitialized=true},
            onZoomChange={mapZoom=it},
            onPanChange={mapPanX=it.x;mapPanY=it.y},
            onBack={view=JourneyView.GAME_MENU},
            onOpenStep={stepId->detailReturnView=JourneyView.MAP;selectedStepId=stepId;view=JourneyView.DETAIL}
        ) else { view=JourneyView.GAMES }
    }
}

@Composable
private fun JourneyRoute(game:AppGame,onBack:()->Unit,onTeam:()->Unit,listState:LazyListState,onOpenStep:(String)->Unit){
    val revision=JourneyProgressStore.revision
    val steps=remember(game.label,revision){JourneyCatalog.steps(game.label)}
    val completed=remember(game.label,revision){JourneyProgressStore.completed(game.label)}
    val completedCount=DataIntegrityRules.completedCount(steps.map{it.id},completed)
    val progress=if(steps.isEmpty())0f else completedCount.toFloat()/steps.size
    val nextStep=steps.firstOrNull{it.id !in completed}
    val smart=remember(game.label,revision){JourneySmartProgress.context(game.label)}
    var selectedStarterId by rememberSaveable(game.label){
        mutableIntStateOf(
            AppStatePreferences.journeyStarterForGame(game.label)
                ?: JourneyStarterCatalog.bestForGame(game.label)?.pokemonId
                ?: TeamCampaignCatalog.starters(game.label).firstOrNull()?.second
                ?: -1
        )
    }
    var showCompleted by rememberSaveable(game.label){mutableStateOf(false)}
    val hiddenCompletedCount=completedCount
    val visibleSteps=remember(steps,completed,showCompleted){
        if(showCompleted) steps else steps.filterNot{it.id in completed}
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        state=listState,
        contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),
        verticalArrangement=Arrangement.spacedBy(0.dp)
    ){
        item{
            Row(
                Modifier.fillMaxWidth().padding(bottom=10.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Voltar")}
                Column(Modifier.weight(1f)){
                    Text("Melhor rota",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall)
                    Text(game.label+" · "+JourneyCatalog.routeLabel(game.label),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick=onTeam){
                    Icon(Icons.Default.Groups,null,Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Meu time")
                }
            }
        }

        item{
            Card(
                shape=RoundedCornerShape(24.dp),
                colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),
                modifier=Modifier.fillMaxWidth().padding(bottom=12.dp)
            ){
                Column(Modifier.fillMaxWidth().padding(16.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text("Progresso da campanha",style=MaterialTheme.typography.labelMedium)
                            Text(
                                completedCount.toString()+" / "+steps.size+" objetivos",
                                fontWeight=FontWeight.Black,
                                style=MaterialTheme.typography.titleLarge
                            )
                        }
                        Surface(
                            shape=RoundedCornerShape(18.dp),
                            color=MaterialTheme.colorScheme.surface.copy(alpha=.72f)
                        ){
                            Text(
                                (progress*100).toInt().toString()+"%",
                                modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp),
                                fontWeight=FontWeight.Bold
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress={progress},
                        modifier=Modifier.fillMaxWidth().padding(top=12.dp).height(8.dp)
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top=12.dp),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                    ){
                        JourneyCountPill(Icons.Default.EmojiEvents,"8 Ginásios")
                        JourneyCountPill(Icons.Default.Landscape,"5 Titãs")
                        JourneyCountPill(Icons.Default.Stars,"5 Team Star")
                    }
                }
            }
        }

        val starterOptions=JourneyStarterCatalog.forGame(game.label)
        if(starterOptions.isNotEmpty()){
            item{
                JourneyStarterGuideCard(
                    starters=starterOptions,
                    selectedStarterId=selectedStarterId,
                    onSelectStarter={id->
                        selectedStarterId=id
                        AppStatePreferences.setJourneyStarterForGame(game.label,id)
                    }
                )
            }
        }

        item{
            Card(
                shape=RoundedCornerShape(20.dp),
                colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer),
                modifier=Modifier.fillMaxWidth().padding(bottom=12.dp)
            ){
                Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                    Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.72f)){
                        Icon(Icons.Default.AutoAwesome,null,Modifier.padding(10.dp))
                    }
                    Column(Modifier.weight(1f).padding(start=10.dp)){
                        Text("FASE AUTOMÁTICA · "+smart.phaseLabel.uppercase(),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
                        Text(smart.recommendation,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=3.dp))
                    }
                    TextButton(onClick=onTeam){Text("Time ideal")}
                }
            }
        }

        nextStep?.let{step->
            val prep=JourneyPreparationCatalog.forStep(step.id)
            val detail=JourneyObjectiveDetailsCatalog.detail(step.id)
            val walkthrough=JourneyWalkthroughCatalog.forStep(step.id)
            item{
                Card(
                    shape=RoundedCornerShape(20.dp),
                    colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.tertiaryContainer),
                    modifier=Modifier.fillMaxWidth().padding(bottom=14.dp).clickable{onOpenStep(step.id)}
                ){
                    Column(Modifier.fillMaxWidth().padding(14.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Icon(Icons.Default.NearMe,null)
                            Column(Modifier.weight(1f).padding(start=10.dp)){
                                Text("PRÓXIMO PASSO INTELIGENTE",style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
                                Text(step.title+" · "+step.levelLabel,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                                Text(step.location,style=MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.ChevronRight,null)
                        }
                        if(prep!=null){
                            HorizontalDivider(Modifier.padding(vertical=10.dp))
                            Text("Prepare-se com "+prep.counters.joinToString(" / "),fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.bodySmall)
                            Text(prep.tip,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=3.dp))
                            if(detail?.opponents?.isNotEmpty()==true){
                                Text("Principal ameaça: "+detail.opponents.last().name+" · "+detail.opponents.last().level,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=5.dp))
                            }
                            walkthrough?.tips?.firstOrNull()?.let{tip->
                                Text("Dica: "+tip,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=5.dp))
                            }
                            val catches=JourneyTeamProgressCatalog.newlyRelevantCatches(step)
                                .filterNot{it.pokemonId in CollectionStore.capturedIds}
                            if(catches.isNotEmpty()){
                                HorizontalDivider(Modifier.padding(vertical=9.dp))
                                Text("CAPTURE NO CAMINHO",fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall)
                                catches.take(2).forEach{rec->
                                    val name=PokedexDataStore.cachedNationalDex().orEmpty().firstOrNull{it.id==rec.pokemonId}?.name ?: "#"+rec.pokemonId
                                    Text(name+" · "+rec.area,style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=4.dp))
                                    Text(rec.reason,style=MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        if(hiddenCompletedCount>0){
            item{
                FilledTonalButton(
                    onClick={showCompleted=!showCompleted},
                    modifier=Modifier.fillMaxWidth().padding(bottom=12.dp)
                ){
                    Icon(if(showCompleted)Icons.Default.VisibilityOff else Icons.Default.Visibility,null)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if(showCompleted) "Ocultar concluídos"
                        else "Mostrar concluídos ("+hiddenCompletedCount+")"
                    )
                }
            }
        }

        if(visibleSteps.isEmpty() && steps.isNotEmpty()){
            item{
                Card(
                    shape=RoundedCornerShape(20.dp),
                    colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer),
                    modifier=Modifier.fillMaxWidth().padding(vertical=8.dp)
                ){
                    Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
                        Icon(Icons.Default.TaskAlt,null,Modifier.size(34.dp))
                        Text("Rota concluída!",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=8.dp))
                        Text("Todos os objetivos estão concluídos. Use “Mostrar concluídos” para revisar a rota.",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=4.dp))
                    }
                }
            }
        }

        items(visibleSteps,key={it.id}){step->
            val done=step.id in completed
            val isNext=nextStep?.id==step.id
            Column{
                val section=when(step.id){
                    "sv-01","sv-pg-01","sv-pg-04","sv-pg-05","sv-dlc-01","sv-dlc-08","sv-epi-01" -> JourneyTeamProgressCatalog.chapterFor(step.id)
                    else->null
                }
                section?.let{
                    Text(it,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=10.dp,bottom=8.dp,start=42.dp))
                }
                JourneyStepCard(
                    step=step,
                    visual=JourneyVisualAssetCatalog.forStep(step.id),
                    done=done,
                    isNext=isNext,
                    onOpen={onOpenStep(step.id)},
                    onToggle={JourneyProgressStore.toggle(game.label,step.id)}
                )
            }
        }

        item{
            TextButton(
                onClick={JourneyProgressStore.clear(game.label)},
                modifier=Modifier.fillMaxWidth().padding(top=10.dp)
            ){
                Icon(Icons.Default.RestartAlt,null)
                Spacer(Modifier.width(6.dp))
                Text("Zerar progresso desta rota")
            }
        }
        item{Spacer(Modifier.height(28.dp))}
    }
}

@Composable
private fun JourneyStarterGuideCard(
    starters:List<JourneyStarterRecommendation>,
    selectedStarterId:Int,
    onSelectStarter:(Int)->Unit
){
    var expanded by rememberSaveable{mutableStateOf(false)}
    val recommended=starters.maxByOrNull{it.rating.early*3+it.rating.mid*2+it.rating.late} ?: return
    Card(
        shape=RoundedCornerShape(22.dp),
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier=Modifier.fillMaxWidth().padding(bottom=12.dp)
    ){
        Column(Modifier.fillMaxWidth().padding(15.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Icon(Icons.Default.CatchingPokemon,null)
                Column(Modifier.weight(1f).padding(start=10.dp)){
                    Text("QUAL INICIAL ESCOLHER?",fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                    Text(recommended.name+" · "+recommended.verdict,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                    Text("Compare o impacto no início, meio e fim da campanha.",style=MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick={expanded=!expanded}){Icon(if(expanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)}
            }
            if(expanded){
                starters.forEach{starter->
                    HorizontalDivider(Modifier.padding(vertical=10.dp))
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(starter.name+" → "+starter.finalName,fontWeight=FontWeight.Black)
                            Text(starter.types+" · "+starter.verdict,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                        }
                        FilterChip(
                            selected=selectedStarterId==starter.pokemonId,
                            onClick={onSelectStarter(starter.pokemonId)},
                            label={Text(if(selectedStarterId==starter.pokemonId)"Escolhido" else "Escolher")},
                            leadingIcon=if(selectedStarterId==starter.pokemonId){{Icon(Icons.Default.Check,null)}}else null
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        StarterStagePill("INÍCIO",starter.rating.early)
                        StarterStagePill("MEIO",starter.rating.mid)
                        StarterStagePill("FIM",starter.rating.late)
                    }
                    starter.advantages.forEach{Text("• "+it,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=5.dp))}
                    starter.cautions.forEach{Text("Atenção: "+it,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=4.dp))}
                }
            }
        }
    }
}

@Composable
private fun StarterStagePill(label:String,rating:Int){
    Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer){
        Text(label+" "+("★".repeat(rating)),Modifier.padding(horizontal=8.dp,vertical=5.dp),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
    }
}

@Composable
private fun JourneyCountPill(icon:ImageVector,label:String){
    Surface(
        shape=RoundedCornerShape(14.dp),
        color=MaterialTheme.colorScheme.surface.copy(alpha=.72f)
    ){
        Row(
            Modifier.padding(horizontal=8.dp,vertical=6.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Icon(icon,null,Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(label,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.SemiBold)
        }
    }
}

@Composable
private fun JourneyStepCard(
    step:JourneyStep,
    visual:JourneyVisualAsset?,
    done:Boolean,
    isNext:Boolean,
    onOpen:()->Unit,
    onToggle:()->Unit
){
    val kindIcon=when(step.kind){
        JourneyChallengeKind.GYM->Icons.Default.EmojiEvents
        JourneyChallengeKind.TITAN->Icons.Default.Landscape
        JourneyChallengeKind.STAR->Icons.Default.Stars
        JourneyChallengeKind.STORY->Icons.Default.AutoStories
        JourneyChallengeKind.POSTGAME->Icons.Default.AutoAwesome
        JourneyChallengeKind.DLC->Icons.Default.TravelExplore
        JourneyChallengeKind.EPILOGUE->Icons.Default.CatchingPokemon
    }
    val detail=JourneyObjectiveDetailsCatalog.detail(step.id)
    val national=PokedexDataStore.cachedNationalDex().orEmpty()
    val kindColor=when(step.kind){
        JourneyChallengeKind.GYM->MaterialTheme.colorScheme.primaryContainer
        JourneyChallengeKind.TITAN->MaterialTheme.colorScheme.secondaryContainer
        JourneyChallengeKind.STAR->MaterialTheme.colorScheme.tertiaryContainer
        JourneyChallengeKind.STORY->MaterialTheme.colorScheme.surfaceVariant
        JourneyChallengeKind.POSTGAME->MaterialTheme.colorScheme.tertiaryContainer
        JourneyChallengeKind.DLC->MaterialTheme.colorScheme.secondaryContainer
        JourneyChallengeKind.EPILOGUE->MaterialTheme.colorScheme.primaryContainer
    }

    Row(Modifier.fillMaxWidth()){
        Column(
            Modifier.width(42.dp),
            horizontalAlignment=Alignment.CenterHorizontally
        ){
            Box(
                Modifier.size(32.dp),
                contentAlignment=Alignment.Center
            ){
                Surface(
                    shape=RoundedCornerShape(50),
                    color=if(done)MaterialTheme.colorScheme.primary else kindColor
                ){
                    Box(Modifier.size(30.dp),contentAlignment=Alignment.Center){
                        if(done){
                            Icon(Icons.Default.Check,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.onPrimary)
                        }else{
                            Text(
                                step.order.toString(),
                                fontWeight=FontWeight.Black,
                                style=MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
            Box(
                Modifier.width(2.dp).height(118.dp)
                    .alpha(if(done).65f else .22f)
                    .background(if(done)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            )
        }

        Card(
            Modifier.weight(1f).padding(bottom=10.dp).clickable(onClick=onOpen),
            shape=RoundedCornerShape(22.dp),
            colors=CardDefaults.cardColors(
                containerColor=when{
                    done->MaterialTheme.colorScheme.secondaryContainer
                    isNext->MaterialTheme.colorScheme.primaryContainer.copy(alpha=.58f)
                    else->MaterialTheme.colorScheme.surfaceContainer
                }
            )
        ){
            Column(Modifier.fillMaxWidth().padding(14.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    visual?.let{
                        JourneyVisualThumb(it,Modifier.size(62.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Surface(shape=RoundedCornerShape(12.dp),color=kindColor){
                        Row(
                            Modifier.padding(horizontal=8.dp,vertical=5.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Icon(kindIcon,null,Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(step.kind.label,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape=RoundedCornerShape(12.dp),
                        color=MaterialTheme.colorScheme.surface
                    ){
                        Text(
                            step.levelLabel,
                            modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp),
                            fontWeight=FontWeight.Bold,
                            style=MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Text(
                    step.title,
                    style=MaterialTheme.typography.titleMedium,
                    fontWeight=FontWeight.Black,
                    modifier=Modifier.padding(top=10.dp)
                )
                Text(
                    step.subtitle,
                    style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    Modifier.fillMaxWidth().padding(top=10.dp),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)
                ){
                    JourneyTypeChip(step.typeLabel)
                    JourneyInfoChip(Icons.Default.LocationOn,step.location,Modifier.weight(1f))
                }

                if(step.note.isNotBlank()){
                    Row(Modifier.fillMaxWidth().padding(top=9.dp),verticalAlignment=Alignment.Top){
                        Icon(Icons.Default.Info,null,Modifier.size(16.dp).padding(top=1.dp))
                        Text(
                            step.note,
                            style=MaterialTheme.typography.labelSmall,
                            modifier=Modifier.padding(start=6.dp).weight(1f)
                        )
                    }
                }

                detail?.opponents?.takeIf{it.isNotEmpty()}?.let{members->
                    HorizontalDivider(Modifier.padding(top=10.dp,bottom=8.dp))
                    Text(
                        if(step.kind==JourneyChallengeKind.TITAN)"ALVO" else "EQUIPE",
                        style=MaterialTheme.typography.labelSmall,
                        fontWeight=FontWeight.Black,
                        color=MaterialTheme.colorScheme.primary
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top=7.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                    ){
                        members.take(6).forEach{member->
                            val pokemonId=journeyOpponentPokemonId(member.name,national)
                            JourneyOpponentMiniCard(
                                name=member.name,
                                level=member.level,
                                pokemonId=pokemonId
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top=10.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    Text(
                        when{
                            done->"Concluído"
                            isNext->"Próximo recomendado"
                            else->"Pendente"
                        },
                        style=MaterialTheme.typography.labelMedium,
                        fontWeight=FontWeight.Bold,
                        color=if(done)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick=onToggle,modifier=Modifier.size(36.dp)){
                        Icon(
                            if(done)Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            if(done)"Concluído" else "Marcar como concluído"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyInfoChip(
    icon:ImageVector,
    label:String,
    modifier:Modifier=Modifier
){
    Surface(
        modifier=modifier,
        shape=RoundedCornerShape(12.dp),
        color=MaterialTheme.colorScheme.surface
    ){
        Row(
            Modifier.padding(horizontal=8.dp,vertical=6.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Icon(icon,null,Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label,style=MaterialTheme.typography.labelSmall,maxLines=1)
        }
    }
}



@Composable
private fun JourneyOpponentMiniCard(
    name:String,
    level:String,
    pokemonId:Int?
){
    Surface(
        shape=RoundedCornerShape(14.dp),
        color=MaterialTheme.colorScheme.surface,
        tonalElevation=1.dp,
        modifier=Modifier.width(86.dp)
    ){
        Column(
            Modifier.padding(7.dp),
            horizontalAlignment=Alignment.CenterHorizontally
        ){
            Surface(
                shape=RoundedCornerShape(12.dp),
                color=MaterialTheme.colorScheme.surfaceContainer,
                modifier=Modifier.size(58.dp)
            ){
                if(pokemonId!=null){
                    PokemonArtwork(
                        model="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+pokemonId+".png",
                        contentDescription=name,
                        modifier=Modifier.fillMaxSize().padding(4.dp),
                        pokemonId=pokemonId
                    )
                }else{
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                        Icon(Icons.Default.CatchingPokemon,null,Modifier.size(24.dp))
                    }
                }
            }
            Text(
                name,
                style=MaterialTheme.typography.labelSmall,
                fontWeight=FontWeight.Bold,
                maxLines=1,
                overflow=TextOverflow.Ellipsis,
                modifier=Modifier.padding(top=5.dp)
            )
            Text(level,style=MaterialTheme.typography.labelSmall,maxLines=1)
        }
    }
}

private fun journeyOpponentPokemonId(
    rawName:String,
    national:List<PokeApiService.DexIndexEntry>
):Int?{
    val aliases=mapOf(
        "Nymble" to 919, "Tarountula" to 917, "Teddiursa" to 216,
        "Klawf" to 950, "Petilil" to 548, "Smoliv" to 928, "Sudowoodo" to 185,
        "Bombirdier" to 962, "Pawniard" to 624,
        "Segin Starmobile" to 966,
        "Wattrel" to 940, "Bellibolt" to 939, "Luxio" to 404, "Mismagius" to 429,
        "Torkoal" to 324, "Schedar Starmobile" to 966,
        "Orthworm" to 968, "Veluza" to 976, "Wugtrio" to 961, "Crabominable" to 740,
        "Skuntank" to 435, "Muk" to 89, "Revavroom" to 966, "Navi Starmobile" to 966,
        "Komala" to 775, "Dudunsparce" to 982, "Staraptor" to 398,
        "Banette" to 354, "Mimikyu" to 778, "Houndstone" to 972, "Toxtricity" to 849,
        "Great Tusk" to 984, "Iron Treads" to 990,
        "Farigiraf" to 981, "Gardevoir" to 282, "Espathra" to 956, "Florges" to 671,
        "Frosmoth" to 873, "Beartic" to 614, "Cetitan" to 975, "Altaria" to 334,
        "Azumarill" to 184, "Wigglytuff" to 40, "Dachsbun" to 927, "Ruchbah Starmobile" to 966,
        "Dondozo" to 977, "Tatsugiri" to 978,
        "Toxicroak" to 454, "Passimian" to 766, "Lucario" to 448, "Annihilape" to 979,
        "Caph Starmobile" to 966,
        "Okidogi" to 1014, "Munkidori" to 1015, "Fezandipiti" to 1016,
        "Ogerpon" to 1017, "Terapagos" to 1024, "Pecharunt" to 1025,
        "Bloodmoon Ursaluna" to 901,
        "Spritzee" to 682, "Swirlix" to 684, "Vivillon" to 666,
        "Venipede" to 543, "Kadabra" to 64, "Roselia" to 315, "Furfrou" to 676,
        "Simisage" to 512, "Simipour" to 516, "Simisear" to 514,
        "Houndoom" to 229, "Sharpedo" to 319, "Buneary" to 427, "Drampa" to 780,
        "Slowbro" to 80, "Camerupt" to 323, "Victreebel" to 71,
        "Heliolisk" to 695, "Ampharos" to 181, "Stunfisk" to 618, "Eelektross" to 604,
        "Beedrill" to 15, "Hawlucha" to 701,
        "Heracross" to 214, "Machamp" to 68, "Medicham" to 308, "Falinks" to 870,
        "Mawile" to 303, "Barbaracle" to 689, "Arbok" to 24, "Roserade" to 407, "Scolipede" to 545,
        "Clawitzer" to 693, "Vanillish" to 583, "Emolga" to 587, "Staryu" to 120,
        "Ariados" to 168, "Sableye" to 302, "Krookodile" to 553, "Scrafty" to 560, "Gyarados" to 130,
        "Tyrantrum" to 697, "Noivern" to 715, "Garchomp" to 445, "Dragalge" to 691,
        "Froslass" to 478, "Venusaur" to 3, "Carbink" to 703, "Aurorus" to 699, "Clefable" to 36,
        "Dragonite" to 149, "Tyranitar" to 248, "Starmie" to 121,
        "Pangoro" to 675, "Malamar" to 687, "Pyroar" to 668, "Salamence" to 373, "Charizard" to 6,
        "Absol" to 359, "Gourgeist" to 711, "Chandelure" to 609,
        "Groudon" to 383, "Kyogre" to 382, "Rayquaza" to 384
    )
    aliases[rawName]?.let{return it}
    val simple=rawName
        .substringBefore(" / ")
        .substringBefore(" & ")
        .substringBefore(" · ")
        .trim()
    return national.firstOrNull{it.name.equals(simple,true)}?.id
}

private fun journeySourceForStep(game:AppGame,step:JourneyStep):String?{
    val preferred=AppStatePreferences.activeRegionForGame(game.label)
    if(game.label=="Scarlet / Violet"){
        val regionIndex=when{
            step.id.startsWith("sv-epi-")->1
            step.id.startsWith("sv-dlc-")->{
                val number=step.id.removePrefix("sv-dlc-").toIntOrNull() ?: 0
                if(number in 1..7) 1 else 2
            }
            else->0
        }
        return game.regions.getOrNull(regionIndex)?.source ?: preferred ?: game.regions.firstOrNull()?.source
    }
    return preferred?.takeIf{source->game.regions.any{it.source==source}} ?: game.regions.firstOrNull()?.source
}

@Composable
private fun JourneyObjectiveDetailScreen(
    game:AppGame,
    step:JourneyStep,
    onBack:()->Unit,
    onTeam:()->Unit,
    onPokemonClick:(Int,String?)->Unit
){
    val revision=JourneyProgressStore.revision
    val done=remember(game.label,step.id,revision){step.id in JourneyProgressStore.completed(game.label)}
    val detail=JourneyObjectiveDetailsCatalog.detail(step.id)
    val preparation=JourneyPreparationCatalog.forStep(step.id)
    val walkthrough=JourneyWalkthroughCatalog.forStep(step.id)
    val national=PokedexDataStore.cachedNationalDex().orEmpty()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        item{
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Voltar")}
                Column(Modifier.weight(1f)){
                    Text(step.kind.label.uppercase(),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
                    Text(step.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
                }
                IconButton(onClick={JourneyProgressStore.toggle(game.label,step.id)}){
                    Icon(if(done)Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,if(done)"Concluído" else "Pendente")
                }
            }
        }

        item{
            val visual=JourneyVisualAssetCatalog.forStep(step.id)
            Card(
                shape=RoundedCornerShape(24.dp),
                colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)
            ){
                Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    visual?.let{JourneyVisualHero(it)}
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text(step.subtitle,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                        Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){
                            Text(step.levelLabel,Modifier.padding(horizontal=10.dp,vertical=6.dp),fontWeight=FontWeight.Bold)
                        }
                    }
                    JourneyTypeDetailLine(step.typeLabel)
                    JourneyDetailLine(Icons.Default.LocationOn,"Local",step.location)
                    Text(detail?.summary ?: step.note,style=MaterialTheme.typography.bodyMedium)
                }
            }
        }

        walkthrough?.let{guide->
            item{JourneyDetailSectionTitle(Icons.Default.MenuBook,"Detonado do objetivo")}
            item{
                Card(
                    shape=RoundedCornerShape(18.dp),
                    colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer)
                ){
                    Column(Modifier.fillMaxWidth().padding(14.dp)){
                        Text(guide.title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                        guide.steps.forEachIndexed{index,text->
                            Row(Modifier.fillMaxWidth().padding(top=if(index==0)10.dp else 8.dp),verticalAlignment=Alignment.Top){
                                Surface(shape=RoundedCornerShape(50),color=MaterialTheme.colorScheme.primaryContainer){
                                    Text((index+1).toString(),Modifier.padding(horizontal=8.dp,vertical=4.dp),fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall)
                                }
                                Text(text,Modifier.weight(1f).padding(start=9.dp,top=2.dp),style=MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if(guide.tips.isNotEmpty()){
                            HorizontalDivider(Modifier.padding(vertical=12.dp))
                            Text("DICAS RÁPIDAS",fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                            guide.tips.forEach{tip->
                                Row(Modifier.fillMaxWidth().padding(top=7.dp),verticalAlignment=Alignment.Top){
                                    Icon(Icons.Default.Lightbulb,null,Modifier.size(17.dp))
                                    Text(tip,Modifier.weight(1f).padding(start=7.dp),style=MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        detail?.let{info->
            item{JourneyDetailSectionTitle(Icons.Default.Groups,"Equipe / adversários")}
            items(info.opponents){member->
                val pokemonId=journeyOpponentPokemonId(member.name,national)
                Card(
                    Modifier.fillMaxWidth().then(
                        if(pokemonId!=null) Modifier.clickable{onPokemonClick(pokemonId,journeySourceForStep(game,step))}
                        else Modifier
                    ),
                    shape=RoundedCornerShape(18.dp)
                ){
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                        Surface(
                            shape=RoundedCornerShape(14.dp),
                            color=MaterialTheme.colorScheme.secondaryContainer,
                            modifier=Modifier.size(72.dp)
                        ){
                            if(pokemonId!=null){
                                PokemonArtwork(
                                    model="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+pokemonId+".png",
                                    contentDescription=member.name,
                                    modifier=Modifier.fillMaxSize().padding(6.dp),
                                    pokemonId=pokemonId
                                )
                            }else{
                                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                                    Icon(Icons.Default.CatchingPokemon,null,Modifier.size(30.dp))
                                }
                            }
                        }
                        Column(Modifier.weight(1f).padding(start=12.dp)){
                            Text(member.name,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                            Text(member.level,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                            if(member.detail.isNotBlank())Text(member.detail,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=3.dp))
                        }
                        if(pokemonId!=null)Icon(Icons.Default.ChevronRight,null)
                    }
                }
            }

            item{
                JourneyDetailSectionTitle(Icons.Default.Bolt,"Fraquezas e resposta")
                Card(shape=RoundedCornerShape(18.dp)){
                    Column(Modifier.fillMaxWidth().padding(14.dp)){
                        Text("Tipos recomendados",style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold)
                        Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            info.weakTo.take(4).forEach{type->
                                AssistChip(onClick={},enabled=false,label={Text(type)})
                            }
                        }
                        Text(info.recommended,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(top=10.dp))
                    }
                }
            }

            item{
                JourneyDetailSectionTitle(Icons.Default.CardGiftcard,"O que você ganha")
                Card(shape=RoundedCornerShape(18.dp)){
                    Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.CardGiftcard,null)
                        Text(info.reward,Modifier.padding(start=10.dp).weight(1f),style=MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        preparation?.let{prep->
            item{JourneyDetailSectionTitle(Icons.Default.Build,"Preparação recomendada")}
            item{
                Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer)){
                    Column(Modifier.fillMaxWidth().padding(14.dp)){
                        Text("Nível recomendado · "+prep.recommendedLevel,fontWeight=FontWeight.Bold)
                        Text("Cobertura: "+prep.counters.joinToString(" / "),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=4.dp))
                        Text("Itens: "+prep.items.joinToString(" · "),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=3.dp))
                        Text(prep.tip,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp))
                    }
                }
            }
            item{JourneyDetailSectionTitle(Icons.Default.CatchingPokemon,"Pokémon úteis agora")}
            items(prep.pokemonIds){pokemonId->
                val entry=national.firstOrNull{it.id==pokemonId}
                Card(Modifier.fillMaxWidth().clickable{onPokemonClick(pokemonId,journeySourceForStep(game,step))},shape=RoundedCornerShape(18.dp)){
                    Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                        PokemonArtwork(
                            model=entry?.spriteUrl ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+pokemonId+".png",
                            contentDescription=entry?.name,
                            modifier=Modifier.size(58.dp),
                            pokemonId=pokemonId
                        )
                        Column(Modifier.weight(1f).padding(start=10.dp)){
                            Text(entry?.name ?: "#"+pokemonId,fontWeight=FontWeight.Bold)
                            Text("Opção recomendada para este objetivo",style=MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ChevronRight,null)
                    }
                }
            }
        }

        item{
            Button(
                onClick={JourneyProgressStore.toggle(game.label,step.id)},
                modifier=Modifier.fillMaxWidth().height(52.dp)
            ){
                Icon(if(done)Icons.Default.CheckCircle else Icons.Default.Done,null)
                Spacer(Modifier.width(8.dp))
                Text(if(done)"Marcar como pendente" else "Marcar como concluído")
            }
        }

        if(!done){
            item{
                OutlinedButton(
                    onClick={JourneyProgressStore.completeThrough(game.label,JourneyCatalog.steps(game.label).map{it.id},step.id)},
                    modifier=Modifier.fillMaxWidth().height(50.dp)
                ){
                    Icon(Icons.Default.AutoAwesome,null)
                    Spacer(Modifier.width(8.dp))
                    Text("Concluir progresso até aqui")
                }
            }
        }

        item{
            OutlinedButton(onClick=onTeam,modifier=Modifier.fillMaxWidth().height(50.dp)){
                Icon(Icons.Default.Groups,null)
                Spacer(Modifier.width(8.dp))
                Text("Ver time ideal para esta fase")
            }
        }
        item{Spacer(Modifier.height(20.dp))}
    }
}

@Composable
private fun JourneyDetailSectionTitle(icon:ImageVector,title:String){
    Row(verticalAlignment=Alignment.CenterVertically){
        Icon(icon,null,Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun JourneyDetailLine(icon:ImageVector,label:String,value:String){
    Row(verticalAlignment=Alignment.CenterVertically){
        Icon(icon,null,Modifier.size(17.dp))
        Text(label+":",Modifier.padding(start=7.dp),fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.bodySmall)
        Text(value,Modifier.padding(start=5.dp),style=MaterialTheme.typography.bodySmall)
    }
}


@Composable
private fun JourneyMapScreen(
    game:AppGame,
    selectedStepId:String?,
    selectionInitialized:Boolean,
    zoom:Float,
    pan:Offset,
    onSelectedStepChange:(String?)->Unit,
    onSelectionInitialized:()->Unit,
    onZoomChange:(Float)->Unit,
    onPanChange:(Offset)->Unit,
    onBack:()->Unit,
    onOpenStep:(String)->Unit
){
    val revision=JourneyProgressStore.revision
    val completed=remember(game.label,revision){JourneyProgressStore.completed(game.label)}
    val steps=remember(game.label){JourneyCatalog.steps(game.label)}
    val points=remember(game.label){JourneyMapCatalog.points(game.label)}
    val mappedSteps=steps.filter{step->points.any{it.stepId==step.id}}
    val next=mappedSteps.firstOrNull{it.id !in completed}
    LaunchedEffect(game.label,selectionInitialized){
        if(!selectionInitialized){
            onSelectedStepChange(next?.id)
            onSelectionInitialized()
        }
    }
    val selected=steps.firstOrNull{it.id==selectedStepId}
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(pan)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)){
        BoxWithConstraints(
            Modifier.fillMaxSize()
                .pointerInput(game.label){
                    detectTransformGestures{_,panChange,zoomChange,_->
                        val newZoom=(currentZoom*zoomChange).coerceIn(1f,3.5f)
                        onZoomChange(newZoom)
                        onPanChange(if(newZoom<=1.01f) Offset.Zero else currentPan+panChange)
                    }
                }
        ){
            val mapAspect=1.414f
            val viewportW=maxWidth
            val viewportH=maxHeight
            val fittedH=viewportW/mapAspect
            val mapH=if(fittedH<viewportH) fittedH else viewportH
            val mapW=mapH*mapAspect
            val left=(viewportW-mapW)/2
            val top=(viewportH-mapH)/2

            Box(
                Modifier.offset(x=left,y=top)
                    .size(mapW,mapH)
                    .graphicsLayer{
                        scaleX=zoom;scaleY=zoom
                        translationX=pan.x;translationY=pan.y
                    }
                    .clip(RoundedCornerShape(18.dp))
            ){
                JourneyMapCatalog.backgroundUrl(game.label)?.let{mapUrl->
                    AsyncImage(
                        model=mapUrl,
                        contentDescription="Mapa de Paldea",
                        contentScale=ContentScale.FillBounds,
                        modifier=Modifier.fillMaxSize()
                    )
                }

                points.forEach{point->
                    val step=steps.firstOrNull{it.id==point.stepId} ?: return@forEach
                    val done=step.id in completed
                    val isNext=next?.id==step.id
                    val isSelected=selectedStepId==step.id
                    val marker=if(isNext)48.dp else if(done)32.dp else 38.dp
                    Surface(
                        modifier=Modifier
                            .offset(x=mapW*point.x-marker/2,y=mapH*point.y-marker/2)
                            .size(marker)
                            .clickable{onSelectedStepChange(step.id)},
                        shape=RoundedCornerShape(50),
                        color=when{
                            isNext->MaterialTheme.colorScheme.tertiary
                            done->MaterialTheme.colorScheme.primary.copy(alpha=.82f)
                            else->MaterialTheme.colorScheme.surface.copy(alpha=.94f)
                        },
                        border=if(isSelected) androidx.compose.foundation.BorderStroke(3.dp,MaterialTheme.colorScheme.onSurface) else null,
                        shadowElevation=if(isNext||isSelected)8.dp else 3.dp
                    ){
                        Box(contentAlignment=Alignment.Center){
                            when{
                                done->Icon(Icons.Default.Check,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.onPrimary)
                                step.kind==JourneyChallengeKind.GYM->Icon(Icons.Default.EmojiEvents,null,Modifier.size(19.dp))
                                step.kind==JourneyChallengeKind.TITAN->Icon(Icons.Default.Landscape,null,Modifier.size(19.dp))
                                step.kind==JourneyChallengeKind.STAR->Icon(Icons.Default.Stars,null,Modifier.size(19.dp))
                                else->Icon(Icons.Default.Place,null,Modifier.size(19.dp))
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.align(Alignment.TopStart).fillMaxWidth().padding(12.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                Surface(shape=RoundedCornerShape(50),color=MaterialTheme.colorScheme.surface.copy(alpha=.92f),shadowElevation=4.dp){
                    IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Voltar")}
                }
                Surface(
                    Modifier.padding(start=8.dp),
                    shape=RoundedCornerShape(18.dp),
                    color=MaterialTheme.colorScheme.surface.copy(alpha=.92f),
                    shadowElevation=4.dp
                ){
                    Column(Modifier.padding(horizontal=12.dp,vertical=8.dp)){
                        Text("Mapa da Jornada",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                        Text(game.label,style=MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.92f),shadowElevation=4.dp){
                    Text(
                        completed.count{it in mappedSteps.map{step->step.id}}.toString()+"/"+mappedSteps.size,
                        Modifier.padding(horizontal=12.dp,vertical=10.dp),
                        fontWeight=FontWeight.Black
                    )
                }
            }

            Column(
                Modifier.align(Alignment.CenterEnd).padding(end=12.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)
            ){
                JourneyMapControl(Icons.Default.Add,"Aproximar"){onZoomChange((zoom+.35f).coerceAtMost(3.5f))}
                JourneyMapControl(Icons.Default.Remove,"Afastar"){
                    val newZoom=(zoom-.35f).coerceAtLeast(1f)
                    onZoomChange(newZoom)
                    if(newZoom<=1.01f)onPanChange(Offset.Zero)
                }
                JourneyMapControl(Icons.Default.MyLocation,"Próximo objetivo"){
                    onSelectedStepChange(next?.id)
                    onZoomChange(1f);onPanChange(Offset.Zero)
                }
            }

            selected?.let{step->
                val isDone=step.id in completed
                val isNext=next?.id==step.id
                Card(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal=14.dp,vertical=18.dp),
                    shape=RoundedCornerShape(24.dp),
                    colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface.copy(alpha=.96f)),
                    elevation=CardDefaults.cardElevation(defaultElevation=8.dp)
                ){
                    Column(Modifier.fillMaxWidth().padding(14.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Surface(shape=RoundedCornerShape(14.dp),color=if(isNext)MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer){
                                Icon(
                                    when(step.kind){
                                        JourneyChallengeKind.GYM->Icons.Default.EmojiEvents
                                        JourneyChallengeKind.TITAN->Icons.Default.Landscape
                                        JourneyChallengeKind.STAR->Icons.Default.Stars
                                        else->Icons.Default.Place
                                    },null,Modifier.padding(10.dp)
                                )
                            }
                            Column(Modifier.weight(1f).padding(start=10.dp)){
                                Text(
                                    when{isDone->"CONCLUÍDO";isNext->"PRÓXIMO OBJETIVO";else->step.kind.label.uppercase()},
                                    style=MaterialTheme.typography.labelSmall,
                                    fontWeight=FontWeight.Black,
                                    color=if(isNext)MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(step.title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                                Text(step.location+" · "+step.levelLabel,style=MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick={onSelectedStepChange(null)}){Icon(Icons.Default.Close,"Fechar")}
                        }
                        Row(Modifier.fillMaxWidth().padding(top=10.dp),verticalAlignment=Alignment.CenterVertically){
                            JourneyTypeChip(step.typeLabel)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick={onOpenStep(step.id)}){
                                Text("Ver objetivo")
                                Icon(Icons.Default.ChevronRight,null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyMapControl(icon:ImageVector,description:String,onClick:()->Unit){
    Surface(shape=RoundedCornerShape(50),color=MaterialTheme.colorScheme.surface.copy(alpha=.92f),shadowElevation=4.dp){
        IconButton(onClick=onClick){Icon(icon,description)}
    }
}

@Composable
private fun JourneyVisualThumb(asset:JourneyVisualAsset,modifier:Modifier=Modifier){
    Surface(modifier=modifier,shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){
        AsyncImage(
            model=asset.imageUrl,
            contentDescription=asset.subject,
            contentScale=ContentScale.Fit,
            modifier=Modifier.fillMaxSize().padding(4.dp)
        )
    }
}

@Composable
private fun JourneyVisualHero(asset:JourneyVisualAsset){
    Card(
        Modifier.fillMaxWidth().height(190.dp),
        shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface.copy(alpha=.72f))
    ){
        Row(Modifier.fillMaxSize().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
            AsyncImage(
                model=asset.imageUrl,
                contentDescription=asset.subject,
                contentScale=ContentScale.Fit,
                modifier=Modifier.weight(1f).fillMaxHeight()
            )
            Column(Modifier.weight(.72f).padding(start=10.dp)){
                Text(asset.subject,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge)
                Text(asset.emblemLabel,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=4.dp))
                Text(
                    when(asset.role){
                        JourneyVisualRole.GYM_LEADER->"Líder de Ginásio"
                        JourneyVisualRole.TEAM_STAR_BOSS->"Chefe Team Star"
                        JourneyVisualRole.TITAN->"Pokémon Titã"
                        JourneyVisualRole.STORY->"Objetivo de história"
                        JourneyVisualRole.TOURNAMENT->"Torneio pós-jogo"
                        JourneyVisualRole.RAID->"Tera Raid pós-jogo"
                        JourneyVisualRole.EXPLORATION->"Exploração pós-jogo"
                        JourneyVisualRole.DLC_CHARACTER->"Personagem do DLC"
                        JourneyVisualRole.LEGENDARY->"Pokémon lendário / especial"
                        JourneyVisualRole.EPILOGUE->"Epílogo"
                    },
                    style=MaterialTheme.typography.bodySmall,
                    modifier=Modifier.padding(top=4.dp)
                )
            }
        }
    }
}


@Composable
private fun JourneyTypeChip(typeLabel:String){
    Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){
        Row(Modifier.padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){
            JourneyTypeIconCatalog.iconUrl(typeLabel)?.let{url->
                AsyncImage(
                    model=url,
                    contentDescription=typeLabel,
                    contentScale=ContentScale.Fit,
                    modifier=Modifier.size(18.dp)
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(typeLabel,style=MaterialTheme.typography.labelSmall,maxLines=1)
        }
    }
}

@Composable
private fun JourneyTypeDetailLine(typeLabel:String){
    Row(verticalAlignment=Alignment.CenterVertically){
        JourneyTypeIconCatalog.iconUrl(typeLabel)?.let{url->
            AsyncImage(
                model=url,
                contentDescription=typeLabel,
                contentScale=ContentScale.Fit,
                modifier=Modifier.size(22.dp)
            )
        } ?: Icon(Icons.Default.Category,null,Modifier.size(17.dp))
        Text("Tipo:",Modifier.padding(start=7.dp),fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.bodySmall)
        Text(typeLabel,Modifier.padding(start=5.dp),style=MaterialTheme.typography.bodySmall)
    }
}
