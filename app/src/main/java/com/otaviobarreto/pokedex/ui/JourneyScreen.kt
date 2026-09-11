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
    val progress=if(steps.isEmpty())0f else completed.size.coerceAtMost(steps.size).toFloat()/steps.size

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(16.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        item{
            Row(verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Voltar")}
                Column(Modifier.weight(1f)){
                    Text("Melhor rota",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall)
                    Text(game.label,style=MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick=onTeam){Text("Meu time")}
            }
        }
        item{
            Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){
                Column(Modifier.fillMaxWidth().padding(14.dp)){
                    Text(completed.size.coerceAtMost(steps.size).toString()+" de "+steps.size+" objetivos",fontWeight=FontWeight.Bold)
                    LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().padding(top=8.dp))
                    Text("A ordem prioriza progressão natural de nível para evitar grind e picos de dificuldade.",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=6.dp))
                }
            }
        }
        items(steps,key={it.id}){step->
            val done=step.id in completed
            Card(
                Modifier.fillMaxWidth().clickable{JourneyProgressStore.toggle(game.label,step.id)},
                shape=RoundedCornerShape(20.dp),
                colors=CardDefaults.cardColors(containerColor=if(done)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)
            ){
                Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                    Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer){
                        Box(Modifier.size(48.dp),contentAlignment=Alignment.Center){
                            Icon(
                                when(step.kind){
                                    JourneyChallengeKind.GYM->Icons.Default.EmojiEvents
                                    JourneyChallengeKind.TITAN->Icons.Default.Landscape
                                    JourneyChallengeKind.STAR->Icons.Default.Stars
                                    JourneyChallengeKind.STORY->Icons.Default.AutoStories
                                },
                                null
                            )
                        }
                    }
                    Column(Modifier.weight(1f).padding(horizontal=12.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Text(step.order.toString().padStart(2,'0')+" · "+step.title,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                            Text(step.levelLabel,style=MaterialTheme.typography.labelMedium)
                        }
                        Text(step.subtitle+" · "+step.kind.label,style=MaterialTheme.typography.bodySmall)
                        Text(step.typeLabel+" · "+step.location,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                        if(step.note.isNotBlank())Text(step.note,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=3.dp))
                    }
                    Icon(if(done)Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,if(done)"Concluído" else "Pendente")
                }
            }
        }
        item{
            TextButton(onClick={JourneyProgressStore.clear(game.label)},modifier=Modifier.fillMaxWidth()){
                Icon(Icons.Default.RestartAlt,null);Spacer(Modifier.width(6.dp));Text("Zerar progresso desta rota")
            }
        }
        item{Spacer(Modifier.height(24.dp))}
    }
}
