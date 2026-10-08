package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*

@Composable
fun PokedexHomeScreen(
    onPokemonClick:(Int)->Unit,
    onOpenPokedex:()->Unit,
    onOpenCollection:()->Unit,
    onOpenBoxes:()->Unit,
    onOpenGameDex:()->Unit,
    onOpenSearch:()->Unit
){
    val captured=CollectionStore.capturedIds
    val total=PokeApiService.MAX_NATIONAL_DEX_ID
    val missing=(total-captured.size).coerceAtLeast(0)
    val ratio=(captured.size.toFloat()/total.coerceAtLeast(1)).coerceIn(0f,1f)

    DexAppBackground{
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal=PokedexDesignTokens.Spacing.Lg),
            contentPadding=PaddingValues(top=PokedexDesignTokens.Spacing.Lg,bottom=PokedexDesignTokens.Spacing.Xxl),
            verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Lg)
        ){
            item{
                CompanionContextHeader(
                    title="Minha Pokédex",
                    eyebrow="NATIONAL DEX",
                    subtitle="Capture, organize e complete sua coleção.",
                    progress={Icon(Icons.Default.CatchingPokemon,null,tint=MaterialTheme.colorScheme.primary)}
                )
            }
            item{
                Card(Modifier.fillMaxWidth()){
                    Column(Modifier.padding(18.dp)){
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text("Progresso nacional",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
                                Text(captured.size.toString()+" registrados · "+missing+" faltando",color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(((ratio*100).toInt()).toString()+"%",fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(progress={ratio},modifier=Modifier.fillMaxWidth().padding(top=14.dp))
                    }
                }
            }
            item{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    HomeDexAction("Pokédex",Icons.Default.MenuBook,Modifier.weight(1f),onOpenPokedex)
                    HomeDexAction("Por jogo",Icons.Default.SportsEsports,Modifier.weight(1f),onOpenGameDex)
                }
            }
            item{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    HomeDexAction("Coleção",Icons.Default.AutoAwesome,Modifier.weight(1f),onOpenCollection)
                    HomeDexAction("Boxes",Icons.Default.GridView,Modifier.weight(1f),onOpenBoxes)
                }
            }
            item{
                Button(onClick=onOpenSearch,modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Default.Search,null);Spacer(Modifier.width(8.dp));Text("Buscar Pokémon",fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HomeDexAction(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,onClick:()->Unit){
    ElevatedCard(onClick=onClick,modifier=modifier){
        Column(Modifier.fillMaxWidth().padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Icon(icon,null,tint=MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(label,fontWeight=FontWeight.Bold)
        }
    }
}
