package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    val contextual=CollectionStore.contextualCapturedIds
    val recent=RecentActivityStore.recentPokemon
    val total=PokeApiService.MAX_NATIONAL_DEX_ID
    val missing=(total-captured.size).coerceAtLeast(0)
    val ratio=(captured.size.toFloat()/total.coerceAtLeast(1)).coerceIn(0f,1f)
    val insights=remember(captured,contextual,CollectionStore.boxes){CollectionInsightsService.current()}
    val missingSuggestions=remember(captured){
        NationalDexCatalog.all.asSequence().filter{it.id !in captured}.take(6).toList()
    }
    val recentSpecies=remember(recent){
        recent.mapNotNull{PokemonRepository.byId(it)}.take(8)
    }
    var quickQuery by rememberSaveable{mutableStateOf("")}
    val quickResults=remember(quickQuery){
        if(quickQuery.trim().length<2) emptyList()
        else PokemonRepository.search(PokemonFilter(query=quickQuery)).take(6)
    }

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
                OutlinedTextField(
                    value=quickQuery,
                    onValueChange={quickQuery=it},
                    modifier=Modifier.fillMaxWidth(),
                    singleLine=true,
                    leadingIcon={Icon(Icons.Default.Search,"Buscar Pokémon")},
                    trailingIcon={
                        if(quickQuery.isNotBlank()) IconButton(onClick={quickQuery=""}){
                            Icon(Icons.Default.Close,"Limpar busca")
                        } else IconButton(onClick=onOpenSearch){
                            Icon(Icons.Default.Tune,"Busca avançada")
                        }
                    },
                    placeholder={Text("Buscar Pokémon, tipo ou habilidade")}
                )
            }
            if(quickResults.isNotEmpty()){
                item{
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        items(quickResults,key={it.id}){pk->
                            ElevatedCard(
                                onClick={onPokemonClick(pk.id)},
                                modifier=Modifier.width(132.dp)
                            ){
                                Column(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalAlignment=Alignment.CenterHorizontally
                                ){
                                    PokemonArtwork(pk.spriteUrl,pk.name,Modifier.size(72.dp),pokemonId=pk.id)
                                    Text(pk.name,fontWeight=FontWeight.Bold,maxLines=1)
                                    Text("#"+pk.id.toString().padStart(4,'0'),style=MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
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
            if(missingSuggestions.isNotEmpty()){
                item{
                    CompanionSectionHeader(
                        title="Próximos que faltam",
                        supporting="Atalhos rápidos para avançar a National Dex."
                    )
                    LazyRow(
                        modifier=Modifier.padding(top=8.dp),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                    ){
                        items(missingSuggestions,key={it.id}){pk->
                            Surface(
                                modifier=Modifier.width(112.dp).clickable{onPokemonClick(pk.id)},
                                shape=MaterialTheme.shapes.medium,
                                color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f)
                            ){
                                Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                    PokemonArtwork(PokemonRepository.byId(pk.id)?.spriteUrl,pk.displayName,Modifier.size(68.dp),pokemonId=pk.id)
                                    Text(pk.displayName,fontWeight=FontWeight.Bold,maxLines=1)
                                    Text("#"+pk.id.toString().padStart(4,'0'),style=MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            if(recentSpecies.isNotEmpty()){
                item{
                    CompanionSectionHeader(title="Vistos recentemente")
                    LazyRow(
                        modifier=Modifier.padding(top=8.dp),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                    ){
                        items(recentSpecies,key={it.id}){pk->
                            AssistChip(
                                onClick={onPokemonClick(pk.id)},
                                label={Text(pk.name)},
                                leadingIcon={PokemonArtwork(pk.spriteUrl,pk.name,Modifier.size(28.dp),pokemonId=pk.id)}
                            )
                        }
                    }
                }
            }
            val gamesWithProgress=insights.byGame.filter{it.captured>0}.take(4)
            if(gamesWithProgress.isNotEmpty()){
                item{
                    CompanionSectionHeader(title="Progresso por jogo",supporting="${insights.gamesWithProgress} jogo(s) com registros")
                    Column(Modifier.padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        gamesWithProgress.forEach{game->
                            Surface(
                                modifier=Modifier.fillMaxWidth().clickable(onClick=onOpenGameDex),
                                shape=MaterialTheme.shapes.medium,
                                color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.24f)
                            ){
                                Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                                    Icon(Icons.Default.SportsEsports,null,tint=MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f).padding(horizontal=10.dp)){
                                        Text(game.game,fontWeight=FontWeight.Bold,maxLines=1)
                                        Text("${game.captured} registrados · ${game.regionsWithProgress}/${game.totalRegions} região(ões)",style=MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.Default.ChevronRight,null)
                                }
                            }
                        }
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
                OutlinedButton(onClick=onOpenSearch,modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Default.ManageSearch,null);Spacer(Modifier.width(8.dp));Text("Abrir busca avançada",fontWeight=FontWeight.Bold)
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
