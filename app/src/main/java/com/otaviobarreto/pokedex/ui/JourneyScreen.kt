package com.otaviobarreto.pokedex.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.otaviobarreto.pokedex.data.*

private enum class JourneyView { GAMES, GAME_MENU, ROUTE, DETAIL }

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
    val game=AppGameCatalog.adventureGames.firstOrNull{it.label==selectedGame}

    LaunchedEffect(explicitGameSelectionRevision){
        if(explicitGameSelectionRevision==0) return@LaunchedEffect
        routeListState.scrollToItem(0)
    }

    BackHandler(enabled=view!=JourneyView.GAMES){
        when(view){
            JourneyView.GAME_MENU -> { selectedGame=null; view=JourneyView.GAMES }
            JourneyView.ROUTE -> view=JourneyView.GAME_MENU
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
    }
}

private data class JourneyRouteStepUi(
    val step: JourneyStep,
    val visual: JourneyVisualAsset?,
    val detail: JourneyObjectiveDetail?,
    val opponentPokemonIds: List<Int?>,
    val chapter: String?
)

@Composable
private fun JourneyRoute(game:AppGame,onBack:()->Unit,onTeam:()->Unit,listState:LazyListState,onOpenStep:(String)->Unit){
    val revision=JourneyProgressStore.revision
    val steps=remember(game.label,revision){JourneyCatalog.steps(game.label)}
    val completed=remember(game.label,revision){JourneyProgressStore.completed(game.label)}
    val completedCount=remember(steps,completed){
        DataIntegrityRules.completedCount(steps.map{it.id},completed)
    }
    val progress=remember(completedCount,steps.size){
        if(steps.isEmpty())0f else completedCount.toFloat()/steps.size
    }
    val nextStep=remember(steps,completed){steps.firstOrNull{it.id !in completed}}
    val smart=remember(game.label,revision){JourneySmartProgress.context(game.label)}
    val national=remember { PokedexDataStore.cachedNationalDex().orEmpty() }
    val nationalByName=remember(national){
        national.associateBy { it.name.lowercase() }
    }
    var selectedStarterId by rememberSaveable(game.label){
        mutableIntStateOf(
            AppStatePreferences.journeyStarterForGame(game.label)
                ?: JourneyStarterCatalog.bestForGame(game.label)?.pokemonId
                ?: TeamCampaignCatalog.starters(game.label).firstOrNull()?.second
                ?: -1
        )
    }
    var showCompleted by rememberSaveable(game.label){mutableStateOf(false)}
    var showUpcoming by rememberSaveable(game.label){mutableStateOf(false)}
    var confirmReset by rememberSaveable(game.label){mutableStateOf(false)}
    val hiddenCompletedCount=completedCount
    val currentIndex=remember(steps,nextStep){nextStep?.let(steps::indexOf) ?: -1}
    val upcomingSteps=remember(steps,completed,currentIndex){
        if(currentIndex<0) emptyList()
        else steps.drop(currentIndex+1).filterNot{it.id in completed}
    }
    val visibleSteps=remember(steps,completed,showCompleted,showUpcoming,upcomingSteps){
        buildList {
            if(showUpcoming) addAll(upcomingSteps)
            if(showCompleted) addAll(steps.filter{it.id in completed})
        }.distinctBy{it.id}
    }
    val currentUi=remember(nextStep,nationalByName){
        nextStep?.let { step ->
            val detail=JourneyObjectiveDetailsCatalog.detail(step.id)
            JourneyRouteStepUi(
                step=step,
                visual=JourneyVisualAssetCatalog.forStep(step.id),
                detail=detail,
                opponentPokemonIds=detail?.opponents?.map { member ->
                    journeyOpponentPokemonId(member.name,nationalByName)
                }.orEmpty(),
                chapter=journeyChapterHeader(step)
            )
        }
    }
    val routeUi=remember(visibleSteps,nationalByName){
        visibleSteps.map { step ->
            val detail=JourneyObjectiveDetailsCatalog.detail(step.id)
            JourneyRouteStepUi(
                step=step,
                visual=JourneyVisualAssetCatalog.forStep(step.id),
                detail=detail,
                opponentPokemonIds=detail?.opponents?.map { member ->
                    journeyOpponentPokemonId(member.name,nationalByName)
                }.orEmpty(),
                chapter=journeyChapterHeader(step)
            )
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        state=listState,
        contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),
        verticalArrangement=Arrangement.spacedBy(0.dp)
    ){
        item(key="route_header",contentType="header"){
            Row(
                Modifier.fillMaxWidth().padding(bottom=10.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Voltar")}
                Column(Modifier.weight(1f)){
                    Text("Minha Jornada",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall)
                    Text(game.label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick=onTeam){
                    Icon(Icons.Default.Groups,null,Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Meu time")
                }
            }
        }

        item(key="route_progress",contentType="summary"){
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
                        Modifier.fillMaxWidth().padding(top=12.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                    ){
                        when(game.label){
                            "Pokémon Legends: Z-A" -> {
                                JourneyCountPill(Icons.Default.EmojiEvents,"Z-A Royale")
                                JourneyCountPill(Icons.Default.AutoAwesome,"Rogue Megas")
                                JourneyCountPill(Icons.Default.Explore,"Mega Dimension")
                            }
                            "Pokémon Scarlet / Violet" -> {
                                JourneyCountPill(Icons.Default.EmojiEvents,"8 Ginásios")
                                JourneyCountPill(Icons.Default.Landscape,"5 Titãs")
                                JourneyCountPill(Icons.Default.Stars,"5 Team Star")
                            }
                            "Pokémon Sword / Shield" -> {
                                JourneyCountPill(Icons.Default.EmojiEvents,"Ginásios de Galar")
                                JourneyCountPill(Icons.Default.Stars,"Champion Cup")
                                JourneyCountPill(Icons.Default.Explore,"Expansion Pass")
                            }
                            "Pokémon Legends: Arceus" -> {
                                JourneyCountPill(Icons.Default.Explore,"Hisui")
                                JourneyCountPill(Icons.Default.AutoAwesome,"Nobres")
                                JourneyCountPill(Icons.Default.Stars,"Placas")
                            }
                            "Pokémon Let's Go Pikachu / Eevee" -> {
                                JourneyCountPill(Icons.Default.EmojiEvents,"8 Ginásios")
                                JourneyCountPill(Icons.Default.Stars,"Liga Pokémon")
                                JourneyCountPill(Icons.Default.Explore,"Kanto")
                            }
                            "Pokémon Brilliant Diamond / Shining Pearl" -> {
                                JourneyCountPill(Icons.Default.EmojiEvents,"8 Ginásios")
                                JourneyCountPill(Icons.Default.Stars,"Liga Pokémon")
                                JourneyCountPill(Icons.Default.Explore,"Sinnoh")
                            }
                            else -> {
                                JourneyCountPill(Icons.Default.EmojiEvents,"Campanha")
                                JourneyCountPill(Icons.Default.Stars,"Chefes")
                                JourneyCountPill(Icons.Default.Explore,"Pós-jogo")
                            }
                        }
                    }
                }
            }
        }

        val starterOptions=JourneyStarterCatalog.forGame(game.label)
        val hasChosenStarter=AppStatePreferences.journeyStarterForGame(game.label)!=null
        if(starterOptions.isNotEmpty() && !hasChosenStarter){
            item(key="starter_guide",contentType="guide"){
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

        item(key="current_objective",contentType="current_objective"){
            Crossfade(
                targetState=currentUi?.step?.id,
                animationSpec=tween(durationMillis=220),
                label="currentObjectiveTransition"
            ){currentId->
                val ui=currentUi?.takeIf{it.step.id==currentId}
                if(ui!=null){
                    val step=ui.step
                    Column{
                        Text(
                            "OBJETIVO ATUAL · "+(ui.chapter ?: JourneyTeamProgressCatalog.chapterFor(step.id)),
                            fontWeight=FontWeight.Black,
                            style=MaterialTheme.typography.labelMedium,
                            color=MaterialTheme.colorScheme.primary,
                            modifier=Modifier.padding(top=10.dp,bottom=8.dp,start=8.dp)
                        )
                        JourneyStepCard(
                            step=step,
                            visual=ui.visual,
                            detail=ui.detail,
                            opponentPokemonIds=ui.opponentPokemonIds,
                            done=false,
                            isNext=true,
                            journeyRecommendation=smart.recommendation,
                            displayTitle=journeyDisplayTitle(step),
                            onOpen={onOpenStep(step.id)},
                            onToggle={JourneyProgressStore.toggle(game.label,step.id)}
                        )
                    }
                }else if(steps.isNotEmpty()){
                    Card(
                        shape=RoundedCornerShape(20.dp),
                        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer),
                        modifier=Modifier.fillMaxWidth().padding(vertical=8.dp)
                    ){
                        Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
                            Icon(Icons.Default.TaskAlt,null,Modifier.size(34.dp))
                            Text("Jornada concluída!",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=8.dp))
                            Text("Todos os objetivos estão concluídos. Abra “Objetivos concluídos” para revisar a Jornada.",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=4.dp))
                        }
                    }
                }
            }
        }

        if(upcomingSteps.isNotEmpty()){
            item(key="upcoming_toggle",contentType="toggle"){
                FilledTonalButton(
                    onClick={showUpcoming=!showUpcoming},
                    modifier=Modifier.fillMaxWidth().padding(bottom=12.dp)
                ){
                    Icon(if(showUpcoming)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if(showUpcoming) "Ocultar próximos objetivos"
                        else "Próximos objetivos ("+upcomingSteps.size+")"
                    )
                }
            }
        }

        if(hiddenCompletedCount>0){
            item(key="completed_toggle",contentType="toggle"){
                FilledTonalButton(
                    onClick={showCompleted=!showCompleted},
                    modifier=Modifier.fillMaxWidth().padding(bottom=12.dp)
                ){
                    Icon(if(showCompleted)Icons.Default.VisibilityOff else Icons.Default.Visibility,null)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if(showCompleted) "Ocultar objetivos concluídos"
                        else "Objetivos concluídos ("+hiddenCompletedCount+")"
                    )
                }
            }
        }

        items(
            items=routeUi,
            key={it.step.id},
            contentType={"route_step"}
        ){ui->
            val step=ui.step
            val done=step.id in completed
            Column{
                ui.chapter?.let{
                    Text(
                        it,
                        fontWeight=FontWeight.Black,
                        style=MaterialTheme.typography.labelMedium,
                        color=MaterialTheme.colorScheme.primary,
                        modifier=Modifier.padding(top=10.dp,bottom=8.dp,start=8.dp)
                    )
                }
                JourneyStepCard(
                    step=step,
                    visual=ui.visual,
                    detail=ui.detail,
                    opponentPokemonIds=ui.opponentPokemonIds,
                    done=done,
                    isNext=false,
                    journeyRecommendation=null,
                    displayTitle=journeyDisplayTitle(step),
                    onOpen={onOpenStep(step.id)},
                    onToggle={JourneyProgressStore.toggle(game.label,step.id)}
                )
            }
        }

        item{
            TextButton(
                onClick={confirmReset=true},
                modifier=Modifier.fillMaxWidth().padding(top=10.dp)
            ){
                Icon(Icons.Default.RestartAlt,null)
                Spacer(Modifier.width(6.dp))
                Text("Reiniciar Jornada")
            }
        }
        item{Spacer(Modifier.height(28.dp))}
    }

    if(confirmReset){
        AlertDialog(
            onDismissRequest={confirmReset=false},
            title={Text("Reiniciar Jornada?")},
            text={Text("Todo o progresso deste jogo será apagado. Essa ação não pode ser desfeita.")},
            confirmButton={
                TextButton(onClick={
                    JourneyProgressStore.clear(game.label)
                    confirmReset=false
                }){Text("Reiniciar")}
            },
            dismissButton={
                TextButton(onClick={confirmReset=false}){Text("Cancelar")}
            }
        )
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

private fun journeyChapterHeader(step:JourneyStep):String? = when {
    step.id in setOf("sv-01","sv-pg-01","sv-pg-04","sv-pg-05","sv-dlc-01","sv-dlc-08","sv-epi-01") ->
        JourneyTeamProgressCatalog.chapterFor(step.id)
    step.id in setOf("za-01","za-06","za-10","za-15","za-20","za-25","za-31","za-36","za-38","za-dlc-00") ->
        JourneyTeamProgressCatalog.chapterFor(step.id)
    else -> null
}

private fun journeyDisplayTitle(step:JourneyStep):String = when {
    step.id.startsWith("sv-") && step.kind==JourneyChallengeKind.GYM -> step.subtitle+" · "+step.title
    step.id.startsWith("sv-") && step.kind==JourneyChallengeKind.TITAN -> step.subtitle+" · "+step.title
    step.id.startsWith("sv-") && step.kind==JourneyChallengeKind.STAR -> step.subtitle+" · "+step.title
    else -> step.title
}

@Composable
private fun JourneyStepCard(
    step:JourneyStep,
    visual:JourneyVisualAsset?,
    detail:JourneyObjectiveDetail?,
    opponentPokemonIds:List<Int?>,
    done:Boolean,
    isNext:Boolean,
    journeyRecommendation:String?,
    displayTitle:String,
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
                    displayTitle,
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

                journeyRecommendation?.takeIf{it.isNotBlank()}?.let{recommendation->
                    Surface(
                        shape=RoundedCornerShape(14.dp),
                        color=MaterialTheme.colorScheme.secondaryContainer,
                        modifier=Modifier.fillMaxWidth().padding(top=10.dp)
                    ){
                        Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                            Icon(Icons.Default.AutoAwesome,null,Modifier.size(18.dp))
                            Text(recommendation,style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(start=8.dp))
                        }
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
                    LazyRow(
                        Modifier.fillMaxWidth().padding(top=7.dp),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                    ){
                        items(
                            items=members.take(6).mapIndexed { index, member -> member to opponentPokemonIds.getOrNull(index) },
                            key={it.first.name+"_"+it.first.level},
                            contentType={"opponent"}
                        ){(member,pokemonId)->
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
                            isNext->"Em andamento"
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
):Int? = journeyOpponentPokemonId(
    rawName,
    national.associateBy { it.name.lowercase() }
)

private fun journeyOpponentPokemonId(
    rawName:String,
    nationalByName:Map<String,PokeApiService.DexIndexEntry>
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
        "Groudon" to 383, "Kyogre" to 382, "Rayquaza" to 384,
        "Lopunny" to 428, "Lucario" to 448, "Talonflame" to 663, "Aerodactyl" to 142, "Metagross" to 376
    )
    aliases[rawName]?.let{return it}
    val simple=rawName
        .substringBefore(" / ")
        .substringBefore(" & ")
        .substringBefore(" · ")
        .trim()
    return nationalByName[simple.lowercase()]?.id
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
                        JourneyVisualRole.PROMOTION->"Promotion Match · Z-A Royale"
                        JourneyVisualRole.ROGUE_MEGA->"Rogue Mega"
                        JourneyVisualRole.HYPERSPACE->"Hyperspace · Mega Dimension"
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
