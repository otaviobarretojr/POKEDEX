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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun JourneyGamePicker(onSelect:(String)->Unit){
    val captured=CollectionStore.contextualCapturedIds
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
            val progress by rememberJourneyCollectionProgress(game,captured)
            Card(
                Modifier.fillMaxWidth().clickable{onSelect(game.label)},
                shape=RoundedCornerShape(22.dp)
            ){
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                    JourneyGameCover(
                        gameLabel = game.label,
                        modifier = Modifier.width(88.dp).height(74.dp)
                    )
                    Column(Modifier.weight(1f).padding(horizontal=12.dp)){
                        Text(game.label,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                        Text(game.subtitle,style=MaterialTheme.typography.bodySmall)
                        if(progress.total>0){
                            Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){
                                LinearProgressIndicator(
                                    progress={progress.ratio},
                                    modifier=Modifier.weight(1f).height(7.dp),
                                    strokeCap=androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                Text(
                                    progress.captured.toString()+"/"+progress.total+" · "+(progress.ratio*100).toInt()+"%",
                                    style=MaterialTheme.typography.labelSmall,
                                    fontWeight=FontWeight.Bold,
                                    modifier=Modifier.padding(start=8.dp)
                                )
                            }
                        }else{
                            Text("Box · calculando progresso…",style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=6.dp))
                        }
                    }
                    Icon(Icons.Default.ChevronRight,null)
                }
            }
        }
        item{Spacer(Modifier.height(20.dp))}
    }
}


@Composable
private fun JourneyGameCover(
    gameLabel: String,
    modifier: Modifier = Modifier
) {
    val covers = GameCoverCatalog.coversFor(gameLabel)
    if (covers.isEmpty()) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SportsEsports, contentDescription = null)
            }
        }
        return
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            Modifier.fillMaxSize().padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            covers.take(2).forEach { cover ->
                AsyncImage(
                    model = cover,
                    contentDescription = "Capa oficial de $gameLabel",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(11.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
internal fun JourneyGameMenu(
    game:AppGame,
    onBack:()->Unit,
    onRoute:()->Unit,
    onMap:()->Unit,
    onTeam:()->Unit,
    onBoxes:()->Unit,
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
                icon=Icons.Default.Map,
                title="Mapa da Jornada",
                subtitle=if(JourneyMapCatalog.points(game.label).isNotEmpty()) "Veja concluídos, objetivo atual e próximos desafios distribuídos no mapa." else "Mapa desta campanha ainda não está disponível.",
                enabled=JourneyMapCatalog.points(game.label).isNotEmpty(),
                onClick=onMap
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
                colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)
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


private data class JourneyCollectionProgress(val captured:Int=0,val total:Int=0){
    val ratio:Float get()=if(total<=0)0f else captured.toFloat()/total
}

@Composable
private fun rememberJourneyCollectionProgress(
    game:AppGame,
    capturedBySource:Map<String,Set<Int>>
):State<JourneyCollectionProgress>{
    val ids by produceState<Set<Int>>(initialValue=emptySet(),game.label){
        value=withContext(Dispatchers.IO){
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
        mutableStateOf(JourneyCollectionProgress(ids.count{it in registered},ids.size))
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
