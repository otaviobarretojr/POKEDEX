package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadForOffline
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
import kotlinx.coroutines.launch

private data class GameHubRegion(val source:String,val title:String,val subtitle:String)
private data class GameHubEntry(val title:String,val subtitle:String,val regions:List<GameHubRegion>)

private val gameHubEntries=listOf(
    GameHubEntry("Scarlet / Violet","Jogo base + The Hidden Treasure of Area Zero",listOf(
        GameHubRegion("Scarlet / Violet · Paldea","Paldea","Jogo base"),
        GameHubRegion("Scarlet / Violet · Kitakami","Kitakami","DLC · The Teal Mask"),
        GameHubRegion("Scarlet / Violet · Blueberry","Blueberry","DLC · The Indigo Disk")
    )),
    GameHubEntry("Sword / Shield","Galar",listOf(GameHubRegion("Sword / Shield · Galar","Galar","Pokédex regional"))),
    GameHubEntry("Legends Arceus","Hisui",listOf(GameHubRegion("Legends Arceus · Hisui","Hisui","Pokédex regional"))),
    GameHubEntry("Let's Go Pikachu / Eevee","Kanto",listOf(GameHubRegion("Let's Go Pikachu / Eevee · Kanto","Kanto","Pokédex regional")))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesHubScreen(onOpenGame:(String)->Unit){
    var regionPicker by remember{mutableStateOf<GameHubEntry?>(null)}
    val scope=rememberCoroutineScope()
    val progress=remember{mutableStateMapOf<String,OfflineGamePackManager.Progress>()}
    val downloading=remember{mutableStateMapOf<String,Boolean>()}
    var refreshToken by remember{mutableIntStateOf(0)}
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
                val active=downloading[game.title]==true
                val current=progress[game.title]

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
                                Text(
                                    when {
                                        active -> current?.label ?: "Preparando download…"
                                        status.downloaded -> "Offline · " + status.pokemonCount + " Pokémon salvos"
                                        else -> if(game.regions.size>1) game.regions.size.toString() + " regiões / conteúdos" else "Disponível para download offline"
                                    },
                                    style=MaterialTheme.typography.labelMedium,
                                    color=if(status.downloaded) Color(0xFF2C8B65) else MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                enabled=!active && catalogGame!=null,
                                onClick={
                                    val target=catalogGame ?: return@IconButton
                                    downloading[game.title]=true
                                    progress[game.title]=OfflineGamePackManager.Progress(0,1,"Preparando…")
                                    scope.launch{
                                        runCatching{
                                            OfflineGamePackManager.download(target){p->progress[game.title]=p}
                                        }.onSuccess{
                                            message=game.title + " está disponível offline."
                                        }.onFailure{
                                            message="Não foi possível concluir o download de " + game.title + "."
                                        }
                                        downloading[game.title]=false
                                        refreshToken++
                                    }
                                }
                            ){
                                Icon(
                                    if(status.downloaded) Icons.Default.OfflinePin else Icons.Default.DownloadForOffline,
                                    contentDescription=if(status.downloaded)"Atualizar dados offline" else "Baixar dados offline",
                                    tint=if(status.downloaded) Color(0xFF2C8B65) else Color(0xFF5B55E7)
                                )
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
                                (current?.done?:0).toString() + " / " + (current?.total?:0),
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
            game.regions.forEach{region-> Card(Modifier.fillMaxWidth().padding(vertical=5.dp).clickable{regionPicker=null;onOpenGame(region.source)}){
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Default.Map,null)
                    Column(Modifier.weight(1f).padding(start=12.dp)){Text(region.title,fontWeight=FontWeight.Bold);Text(region.subtitle,style=MaterialTheme.typography.bodySmall)}
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
