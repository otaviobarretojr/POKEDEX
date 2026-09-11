package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeDashboardScreen(
    onPokemonClick:(Int)->Unit,
    onOpenLivingDex:()->Unit,
    onOpenGames:()->Unit,
    onOpenTeams:()->Unit,
    onOpenBoxes:()->Unit,
    onOpenCompanion:()->Unit,
    onOpenGame:(String)->Unit,
    onContinue:(String)->Unit
){
    val dex=PokedexDataStore.cachedNationalDex().orEmpty()
    val captured=CollectionStore.capturedIds
    val recent=RecentActivityStore.recentPokemon
    val activeGame=CompanionPreferences.activeGame
    var plan by remember(activeGame,captured){mutableStateOf<CapturePlan?>(null)}
    var loadingPlan by remember(activeGame,captured){mutableStateOf(true)}
    LaunchedEffect(activeGame,captured){
        loadingPlan=true
        plan=runCatching{withContext(Dispatchers.IO){CapturePlannerService.build(activeGame)}}.getOrNull()
        loadingPlan=false
    }
    val next=plan?.obtainableMissing?.firstOrNull()
    val activeRegions=AppGameCatalog.games.firstOrNull{it.label==activeGame}?.regions.orEmpty()
    val offline=runCatching{OfflineGamePackManager.status(activeGame)}.getOrNull()
    val nationalTotal=dex.size.coerceAtLeast(PokeApiService.MAX_NATIONAL_DEX_ID)
    val progress=if(nationalTotal==0)0f else captured.size.toFloat()/nationalTotal

    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{
            Text("POKEDEX",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
            Text("Sua jornada, organizada.",style=MaterialTheme.typography.bodyMedium)
        }
        item{
            Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){
                Column(Modifier.fillMaxWidth().padding(16.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.SportsEsports,null)
                        Column(Modifier.weight(1f).padding(start=10.dp)){
                            Text("Jogo ativo",style=MaterialTheme.typography.labelMedium)
                            Text(activeGame,fontWeight=FontWeight.Bold)
                        }
                        AssistChip(onClick=onOpenCompanion,label={Text("Alterar")})
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth())
                    Text(captured.size.toString()+" / "+nationalTotal+" na Living Dex",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=6.dp))
                }
            }
        }
        item{
            Card(Modifier.fillMaxWidth().clickable{onContinue(RecentActivityStore.lastRoute)},shape=RoundedCornerShape(20.dp)){
                Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Default.History,null)
                    Column(Modifier.weight(1f).padding(start=10.dp)){
                        Text("Continuar de onde parei",fontWeight=FontWeight.Bold)
                        Text(RecentActivityStore.lastRoute.replaceFirstChar{it.uppercase()},style=MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight,null)
                }
            }
        }
        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                HomeQuick(Icons.Default.ListAlt,"Living Dex",onOpenLivingDex,Modifier.weight(1f))
                HomeQuick(Icons.Default.GridView,"Boxes",onOpenBoxes,Modifier.weight(1f))
                HomeQuick(Icons.Default.Groups,"Times",onOpenTeams,Modifier.weight(1f))
            }
        }
        item{
            Card(Modifier.fillMaxWidth().clickable{next?.let{onPokemonClick(it.id)}},shape=RoundedCornerShape(20.dp)){
                Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Default.Flag,null)
                    Column(Modifier.weight(1f).padding(start=10.dp)){
                        Text("Próximo alvo",fontWeight=FontWeight.Bold)
                        when{
                            loadingPlan->Text("Calculando rota…",style=MaterialTheme.typography.bodySmall)
                            next!=null->Text("#"+next.id.toString().padStart(4,'0')+" · "+next.name,style=MaterialTheme.typography.bodySmall)
                            else->Text("Nenhum alvo disponível neste jogo.",style=MaterialTheme.typography.bodySmall)
                        }
                    }
                    if(next!=null)Icon(Icons.Default.ChevronRight,null)
                }
            }
        }
        item{
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){
                Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                    Icon(if(offline?.verified==true)Icons.Default.OfflinePin else Icons.Default.CloudDownload,null)
                    Column(Modifier.weight(1f).padding(start=10.dp)){
                        Text("Offline",fontWeight=FontWeight.Bold)
                        Text(if(offline?.verified==true)"Pacote de "+activeGame+" pronto" else "Pacote offline ainda não está completo",style=MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick=onOpenGames){Text("Gerenciar")}
                }
            }
        }
        if(activeRegions.isNotEmpty()){
            item{Text("Regiões do jogo ativo",fontWeight=FontWeight.Bold)}
            items(activeRegions,key={it.source}){region->
                Card(Modifier.fillMaxWidth().clickable{onOpenGame(region.source)}){
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.Map,null)
                        Column(Modifier.weight(1f).padding(start=10.dp)){Text(region.label,fontWeight=FontWeight.SemiBold);Text(region.subtitle,style=MaterialTheme.typography.bodySmall)}
                        Icon(Icons.Default.ChevronRight,null)
                    }
                }
            }
        }
        if(recent.isNotEmpty()){
            item{Text("Vistos recentemente",fontWeight=FontWeight.Bold)}
            item{
                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    items(recent.take(10),key={it}){id->
                        val p=dex.firstOrNull{it.id==id}
                        Card(Modifier.width(112.dp).clickable{onPokemonClick(id)},shape=RoundedCornerShape(18.dp)){
                            Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                AsyncImage(p?.spriteUrl ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png",p?.name,Modifier.size(72.dp))
                                Text(p?.name ?: "#"+id,maxLines=1,style=MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
        item{Spacer(Modifier.height(20.dp))}
    }
}

@Composable
private fun HomeQuick(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,onClick:()->Unit,modifier:Modifier=Modifier){
    Card(modifier.clickable(onClick=onClick),shape=RoundedCornerShape(18.dp)){
        Column(Modifier.fillMaxWidth().padding(vertical=14.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Icon(icon,null);Spacer(Modifier.height(4.dp));Text(label,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold)
        }
    }
}
