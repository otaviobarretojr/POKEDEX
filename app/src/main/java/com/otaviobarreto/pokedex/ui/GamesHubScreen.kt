package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.data.OfflineGamePackManager
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class GameHubRegion(val source:String,val title:String,val subtitle:String)
private data class GameHubEntry(val title:String,val subtitle:String,val regions:List<GameHubRegion>)

private val gameHubEntries=listOf(
    GameHubEntry("Scarlet / Violet","Jogo base + The Hidden Treasure of Area Zero",listOf(
        GameHubRegion("Scarlet / Violet · Paldea","Paldea","Jogo base"),
        GameHubRegion("Scarlet / Violet · Kitakami","Kitakami","DLC · The Teal Mask"),
        GameHubRegion("Scarlet / Violet · Blueberry","Blueberry","DLC · The Indigo Disk")
    )),
    GameHubEntry("Sword / Shield","Galar + Expansion Pass",listOf(
        GameHubRegion("Sword / Shield · Galar","Galar","Jogo base"),
        GameHubRegion("Sword / Shield · Isle of Armor","Isle of Armor","DLC · The Isle of Armor"),
        GameHubRegion("Sword / Shield · Crown Tundra","Crown Tundra","DLC · The Crown Tundra")
    )),
    GameHubEntry("Legends Arceus","Hisui",listOf(GameHubRegion("Legends Arceus · Hisui","Hisui","Pokédex regional"))),
    GameHubEntry("Let's Go Pikachu / Eevee","Kanto",listOf(GameHubRegion("Let's Go Pikachu / Eevee · Kanto","Kanto","Pokédex regional"))),
    GameHubEntry("Brilliant Diamond / Shining Pearl","Sinnoh",listOf(GameHubRegion("Brilliant Diamond / Shining Pearl · Sinnoh","Sinnoh","Pokédex regional"))),
    GameHubEntry("Black / White","Unova",listOf(GameHubRegion("Black / White · Unova","Unova","Pokédex regional"))),
    GameHubEntry("X / Y","Kalos",listOf(
        GameHubRegion("X / Y · Kalos Central","Central Kalos","Pokédex Central"),
        GameHubRegion("X / Y · Kalos Coastal","Coastal Kalos","Pokédex Costeira"),
        GameHubRegion("X / Y · Kalos Mountain","Mountain Kalos","Pokédex Montanhosa")
    )),
    GameHubEntry("Omega Ruby / Alpha Sapphire","Hoenn",listOf(GameHubRegion("Omega Ruby / Alpha Sapphire · Hoenn","Hoenn","Pokédex regional")))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesHubScreen(onOpenGame:(String)->Unit){
    var regionPicker by remember{mutableStateOf<GameHubEntry?>(null)}
    var refreshToken by remember{mutableIntStateOf(0)}
    LaunchedEffect(Unit){ while(true){ delay(1500); refreshToken++ } }
    var message by remember{mutableStateOf<String?>(null)}

    Column(Modifier.fillMaxSize().background(Color(0xFFF8F8FC))){
        Column(Modifier.padding(16.dp)){
            Text("JOGOS",fontSize=29.sp,fontWeight=FontWeight.Black,color=Color(0xFF151426))
            Text("S U A S  A V E N T U R A S  ·  R E G I Õ E S  E  D L C S",fontSize=7.sp,color=Color(0xFF72778B))
            Text("Abra uma região ou salve os dados do jogo para usar offline.",style=MaterialTheme.typography.bodySmall,color=Color(0xFF72778B),modifier=Modifier.padding(top=7.dp))
        }

        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(gameHubEntries,key={it.title}){game->
                val catalogGame=remember(game.title){AppGameCatalog.games.firstOrNull{it.label==game.title}}
                val status=remember(game.title,refreshToken){OfflineGamePackManager.status(game.title)}
                val current=remember(game.title,refreshToken){OfflineGamePackManager.runtimeProgress(game.title)}
                val audit=remember(game.title,refreshToken){OfflineGamePackManager.audit(game.title)}
                val active=current!=null
                val capturedIds=CollectionStore.capturedIds
                val gameIds=remember(game.title,refreshToken,capturedIds){
                    catalogGame?.regions.orEmpty().flatMap { region ->
                        GameContext.fromSource(region.source)?.let { GameDexService.cached(it).orEmpty() }.orEmpty()
                    }.map { it.nationalId }.distinct()
                }
                val gameCaptured=gameIds.count { it in capturedIds }

                Card(
                    Modifier.fillMaxWidth().padding(horizontal=16.dp).clickable{
                        if(game.regions.size==1) onOpenGame(game.regions.first().source) else regionPicker=game
                    },
                    shape=RoundedCornerShape(22.dp),
                    colors=CardDefaults.cardColors(containerColor=Color(0xFFF1F0F8))
                ){
                    Column(Modifier.fillMaxWidth().padding(16.dp)){
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
                            Surface(shape=RoundedCornerShape(16.dp),color=Color(0xFFEAE8FB)){
                                Box(Modifier.size(48.dp),contentAlignment=Alignment.Center){
                                    Icon(Icons.Default.Map,null,tint=Color(0xFF5B55E7))
                                }
                            }
                            Column(Modifier.weight(1f)){
                                Text(game.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                                Text(game.subtitle,style=MaterialTheme.typography.bodySmall)
                                if(gameIds.isNotEmpty()){
                                    Text("$gameCaptured / ${gameIds.size} na coleção",style=MaterialTheme.typography.labelMedium,color=Color(0xFF5B55E7))
                                }
                                Text(
                                    when {
                                        active -> current?.label ?: "Preparando download…"
                                        status.verified -> "Offline verificado · " + status.pokemonCount + " Pokémon + imagens"
                                        status.completeCount > 0 -> "Pacote parcial · " + status.completeCount + " / " + status.pokemonCount + " Pokémon"
                                        status.downloaded -> "Pacote incompleto · toque para atualizar"
                                        else -> if(game.regions.size>1) game.regions.size.toString() + " regiões / conteúdos" else "Disponível para download offline"
                                    },
                                    style=MaterialTheme.typography.labelMedium,
                                    color=if(status.verified) Color(0xFF2C8B65) else MaterialTheme.colorScheme.primary
                                )
                                if(status.downloadedAt>0L){
                                    Text(
                                        "Atualizado em " + SimpleDateFormat("dd/MM · HH:mm",Locale.getDefault()).format(Date(status.downloadedAt)),
                                        style=MaterialTheme.typography.labelSmall,
                                        color=Color(0xFF72778B)
                                    )
                                }
                            }
                            Column(horizontalAlignment=Alignment.CenterHorizontally){
                                IconButton(
                                    enabled=!active && catalogGame!=null,
                                    onClick={
                                        if(catalogGame==null) return@IconButton
                                        OfflineGamePackManager.enqueue(game.title)
                                        refreshToken++
                                    }
                                ){
                                    Icon(
                                        if(status.verified) Icons.Default.OfflinePin else Icons.Default.DownloadForOffline,
                                        contentDescription=if(status.verified)"Atualizar dados offline" else "Baixar dados offline",
                                        tint=if(status.verified) Color(0xFF2C8B65) else Color(0xFF5B55E7)
                                    )
                                }
                                if(status.pokemonCount>0 && !active){
                                    IconButton(onClick={
                                        OfflineGamePackManager.remove(game.title)
                                        refreshToken++
                                        message="Pacote offline de " + game.title + " removido."
                                    }){
                                        Icon(Icons.Default.DeleteForever,contentDescription="Remover pacote offline",tint=Color(0xFF9A4650))
                                    }
                                }
                            }
                        }
                        if(status.pokemonCount>0 && !active){
                            Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.End){
                                Text(audit.summary,style=MaterialTheme.typography.labelSmall,color=if(audit.valid)Color(0xFF2C8B65)else Color(0xFFB26A00),modifier=Modifier.align(Alignment.CenterVertically))
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick={OfflineGamePackManager.repair(game.title);refreshToken++}){Text(if(audit.valid)"Verificar/Atualizar" else "Reparar")}
                            }
                        }
                        if(active){
                            LinearProgressIndicator(
                                progress={current?.fraction?:0f},
                                modifier=Modifier.fillMaxWidth().padding(top=12.dp),
                                color=Color(0xFF5B55E7),
                                trackColor=Color(0xFF5B55E7).copy(alpha=.12f)
                            )
                            Text(
                                (current?.done?:0).toString() + " / " + (current?.total?:0) + " · " + (((current?.fraction?:0f)*100).toInt()) + "%",
                                style=MaterialTheme.typography.labelSmall,
                                color=Color(0xFF72778B),
                                modifier=Modifier.align(Alignment.End).padding(top=4.dp)
                            )
                        }
                    }
                }
            }
            item{Spacer(Modifier.height(10.dp))}
        }
    }

    regionPicker?.let{game-> ModalBottomSheet(onDismissRequest={regionPicker=null}){
        Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp)){
            Text(game.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text("Escolha a região ou DLC",modifier=Modifier.padding(bottom=12.dp))
            game.regions.forEach{region->
                val regionContext=remember(region.source){GameContext.fromSource(region.source)}
                val regionDex=remember(region.source){regionContext?.let{GameDexService.cached(it).orEmpty()}.orEmpty()}
                val regionCaptured=regionDex.count{it.nationalId in CollectionStore.capturedIds}
                Card(Modifier.fillMaxWidth().padding(vertical=5.dp).clickable{regionPicker=null;onOpenGame(region.source)}){
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Default.Map,null)
                    Column(Modifier.weight(1f).padding(start=12.dp)){Text(region.title,fontWeight=FontWeight.Bold);Text(region.subtitle,style=MaterialTheme.typography.bodySmall);if(regionDex.isNotEmpty())Text("$regionCaptured / ${regionDex.size} capturados",style=MaterialTheme.typography.labelSmall,color=Color(0xFF5B55E7))}
                    Text("›")
                }
            }}
            Spacer(Modifier.height(20.dp))
        }
    }}

    message?.let{ body ->
        AlertDialog(
            onDismissRequest={message=null},
            confirmButton={TextButton({message=null}){Text("OK")}},
            title={Text("Dados offline")},
            text={Text(body)}
        )
    }
}
