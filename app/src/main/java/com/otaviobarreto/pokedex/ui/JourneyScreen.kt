package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*

private enum class JourneyView { GAMES, GAME_MENU, ROUTE }

@Composable
fun JourneyScreen(
    onPokemonClick:(Int)->Unit,
    onOpenTeamGuide:(String)->Unit,
    onOpenGameDex:(String)->Unit
){
    var selectedGame by remember { mutableStateOf<String?>(null) }
    var view by remember { mutableStateOf(JourneyView.GAMES) }
    val game=AppGameCatalog.adventureGames.firstOrNull{it.label==selectedGame}

    when(view){
        JourneyView.GAMES -> JourneyGamePicker(
            onSelect={selectedGame=it;CompanionPreferences.activeGame=it;view=JourneyView.GAME_MENU}
        )
        JourneyView.GAME_MENU -> if(game!=null) JourneyGameMenu(
            game=game,
            onBack={view=JourneyView.GAMES},
            onRoute={view=JourneyView.ROUTE},
            onTeam={onOpenTeamGuide(game.label)},
            onDex={game.regions.firstOrNull()?.source?.let(onOpenGameDex)},
            onRegion={onOpenGameDex}
        ) else { view=JourneyView.GAMES }
        JourneyView.ROUTE -> if(game!=null) JourneyRoute(
            game=game,
            onBack={view=JourneyView.GAME_MENU},
            onTeam={onOpenTeamGuide(game.label)}
        ) else { view=JourneyView.GAMES }
    }
}

@Composable
private fun JourneyGamePicker(onSelect:(String)->Unit){
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        item{
            Text("JORNADA",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
            Text("Escolha um jogo para abrir sua central de rota, time e guias.",style=MaterialTheme.typography.bodyMedium)
        }
        items(AppGameCatalog.adventureGames,key={it.label}){game->
            Card(
                Modifier.fillMaxWidth().clickable{onSelect(game.label)},
                shape=RoundedCornerShape(22.dp)
            ){
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                    Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.primaryContainer){
                        Icon(Icons.Default.SportsEsports,null,Modifier.padding(14.dp))
                    }
                    Column(Modifier.weight(1f).padding(horizontal=12.dp)){
                        Text(game.label,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                        Text(game.subtitle,style=MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight,null)
                }
            }
        }
        item{Spacer(Modifier.height(20.dp))}
    }
}

@Composable
private fun JourneyGameMenu(
    game:AppGame,
    onBack:()->Unit,
    onRoute:()->Unit,
    onTeam:()->Unit,
    onDex:()->Unit,
    onRegion:(String)->Unit
){
    val route=JourneyCatalog.steps(game.label)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        item{
            Row(verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Voltar")}
                Column{
                    Text(game.label,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall)
                    Text("Central da Jornada",style=MaterialTheme.typography.labelMedium)
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
            JourneyActionCard(
                icon=Icons.Default.CatchingPokemon,
                title="Pokédex do jogo",
                subtitle="Abra a Pokédex regional vinculada ao jogo.",
                onClick=onDex
            )
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
                Icon(icon,null,Modifier.padding(13.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal=12.dp)){
                Text(title,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                Text(subtitle,style=MaterialTheme.typography.bodySmall)
            }
            if(enabled)Icon(Icons.Default.ChevronRight,null)
        }
    }
}

@Composable
private fun JourneyRoute(game:AppGame,onBack:()->Unit,onTeam:()->Unit){
    val revision=JourneyProgressStore.revision
    val steps=remember(game.label,revision){JourneyCatalog.steps(game.label)}
    val completed=remember(game.label,revision){JourneyProgressStore.completed(game.label)}
    val completedCount=completed.size.coerceAtMost(steps.size)
    val progress=if(steps.isEmpty())0f else completedCount.toFloat()/steps.size
    val nextStep=steps.firstOrNull{it.id !in completed}

    LazyColumn(
        Modifier.fillMaxSize(),
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
                    Text(game.label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
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

        nextStep?.let{step->
            item{
                Card(
                    shape=RoundedCornerShape(20.dp),
                    colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.tertiaryContainer),
                    modifier=Modifier.fillMaxWidth().padding(bottom=14.dp)
                ){
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        Icon(Icons.Default.NearMe,null)
                        Column(Modifier.weight(1f).padding(start=10.dp)){
                            Text("PRÓXIMO OBJETIVO",style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
                            Text(step.title+" · "+step.levelLabel,fontWeight=FontWeight.Bold)
                            Text(step.location,style=MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ChevronRight,null)
                    }
                }
            }
        }

        items(steps,key={it.id}){step->
            val done=step.id in completed
            val isNext=nextStep?.id==step.id
            JourneyStepCard(
                step=step,
                done=done,
                isNext=isNext,
                onToggle={JourneyProgressStore.toggle(game.label,step.id)}
            )
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
    done:Boolean,
    isNext:Boolean,
    onToggle:()->Unit
){
    val kindIcon=when(step.kind){
        JourneyChallengeKind.GYM->Icons.Default.EmojiEvents
        JourneyChallengeKind.TITAN->Icons.Default.Landscape
        JourneyChallengeKind.STAR->Icons.Default.Stars
        JourneyChallengeKind.STORY->Icons.Default.AutoStories
    }
    val kindColor=when(step.kind){
        JourneyChallengeKind.GYM->MaterialTheme.colorScheme.primaryContainer
        JourneyChallengeKind.TITAN->MaterialTheme.colorScheme.secondaryContainer
        JourneyChallengeKind.STAR->MaterialTheme.colorScheme.tertiaryContainer
        JourneyChallengeKind.STORY->MaterialTheme.colorScheme.surfaceVariant
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
            VerticalDivider(
                modifier=Modifier.height(118.dp).alpha(if(done).65f else .22f),
                thickness=2.dp,
                color=if(done)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
        }

        Card(
            Modifier.weight(1f).padding(bottom=10.dp).clickable(onClick=onToggle),
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
                    JourneyInfoChip(Icons.Default.Category,step.typeLabel)
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
                    Icon(
                        if(done)Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        if(done)"Concluído" else "Marcar como concluído"
                    )
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
