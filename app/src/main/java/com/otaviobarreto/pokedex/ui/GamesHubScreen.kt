package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    Column(Modifier.fillMaxSize()){
        Column(Modifier.padding(16.dp)){
            Text("Jogos",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text("Cada jogo reúne sua Pokédex, regiões, mapas e conteúdos adicionais.",style=MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(gameHubEntries,key={it.title}){game->
                Card(Modifier.fillMaxWidth().padding(horizontal=16.dp).clickable{
                    if(game.regions.size==1) onOpenGame(game.regions.first().source) else regionPicker=game
                }){
                    Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
                        Icon(Icons.Default.Map,null)
                        Column(Modifier.weight(1f)){
                            Text(game.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                            Text(game.subtitle,style=MaterialTheme.typography.bodySmall)
                            if(game.regions.size>1) Text("${game.regions.size} regiões / conteúdos",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                        }
                        Text("›",style=MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
    regionPicker?.let{game-> ModalBottomSheet(onDismissRequest={regionPicker=null}){
        Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp)){
            Text(game.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text("Escolha a região ou DLC",modifier=Modifier.padding(bottom=12.dp))
            game.regions.forEach{region-> Card(Modifier.fillMaxWidth().padding(vertical=5.dp).clickable{regionPicker=null;onOpenGame(region.source)}){
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Default.Map,null);Column(Modifier.weight(1f).padding(start=12.dp)){Text(region.title,fontWeight=FontWeight.Bold);Text(region.subtitle,style=MaterialTheme.typography.bodySmall)};Text("›")
                }
            }}
            Spacer(Modifier.height(20.dp))
        }
    }}
}
