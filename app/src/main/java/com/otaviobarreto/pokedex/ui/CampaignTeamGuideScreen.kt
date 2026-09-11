package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignTeamGuideScreen(
    onBackToMyTeams:()->Unit,
    onPokemonClick:(Int)->Unit,
    initialGame:String?=null
){
    val initial=initialGame?.takeIf{it in TeamCampaignCatalog.switchGames} ?: CompanionPreferences.activeGame.takeIf{it in TeamCampaignCatalog.switchGames} ?: TeamCampaignCatalog.switchGames.first()
    var game by remember(initial) { mutableStateOf(initial) }
    var starterId by remember(game) { mutableIntStateOf(TeamCampaignCatalog.starters(game).first().second) }
    var phase by remember { mutableStateOf(CampaignPhase.EARLY) }
    var gameMenu by remember { mutableStateOf(false) }
    var createdMessage by remember { mutableStateOf<String?>(null) }
    val preset=remember(game,starterId,phase){TeamCampaignCatalog.preset(game,starterId,phase)}
    val national=PokedexDataStore.cachedNationalDex().orEmpty()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        item{
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text("MEU TIME",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
                    Text("GUIA DE CAMPANHA · SWITCH",style=MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick=onBackToMyTeams){Icon(Icons.Default.Groups,null);Spacer(Modifier.width(4.dp));Text("Meus times")}
            }
        }
        item{
            Card(shape=RoundedCornerShape(22.dp)){
                Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    ExposedDropdownMenuBox(expanded=gameMenu,onExpandedChange={gameMenu=!gameMenu}){
                        OutlinedTextField(
                            value=game,onValueChange={},readOnly=true,singleLine=true,
                            modifier=Modifier.menuAnchor().fillMaxWidth(),
                            label={Text("Jogo")},
                            trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(gameMenu)}
                        )
                        ExposedDropdownMenu(expanded=gameMenu,onDismissRequest={gameMenu=false}){
                            TeamCampaignCatalog.switchGames.forEach{g->
                                DropdownMenuItem(text={Text(g)},onClick={
                                    game=g
                                    starterId=TeamCampaignCatalog.starters(g).first().second
                                    CompanionPreferences.activeGame=g.takeIf{candidate->AppGameCatalog.games.any{it.label==candidate}} ?: CompanionPreferences.activeGame
                                    gameMenu=false
                                })
                            }
                        }
                    }
                    Text("Inicial",fontWeight=FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        TeamCampaignCatalog.starters(game).forEach{(name,id)->
                            FilterChip(
                                selected=starterId==id,
                                onClick={starterId=id},
                                label={Text(name)},
                                leadingIcon=if(starterId==id){{Icon(Icons.Default.Check,null)}}else null
                            )
                        }
                    }
                    Text("Fase da história",fontWeight=FontWeight.SemiBold)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){
                        CampaignPhase.values().forEachIndexed{index,item->
                            SegmentedButton(
                                selected=phase==item,
                                onClick={phase=item},
                                shape=SegmentedButtonDefaults.itemShape(index,CampaignPhase.values().size)
                            ){Text(item.label)}
                        }
                    }
                }
            }
        }
        preset?.let{team->
            item{
                Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){
                    Column(Modifier.fillMaxWidth().padding(14.dp)){
                        Text(team.starter+" · "+team.phase.label,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                        Text(team.rationale,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=4.dp))
                        Text("Base de pesquisa: "+team.sourceLabel,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=6.dp))
                    }
                }
            }
            itemsIndexed(team.slots,key={index,slot->index.toString()+"-"+slot.pokemonId}){index,slot->
                val entry=national.firstOrNull{it.id==slot.pokemonId}
                val build=TeamCampaignCatalog.buildFor(slot.pokemonId,game)
                Card(Modifier.fillMaxWidth().clickable{onPokemonClick(slot.pokemonId)},shape=RoundedCornerShape(20.dp)){
                    Column(Modifier.fillMaxWidth().padding(12.dp)){
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            AsyncImage(
                                model=entry?.spriteUrl ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+slot.pokemonId+".png",
                                contentDescription=entry?.name,
                                modifier=Modifier.size(76.dp)
                            )
                            Column(Modifier.weight(1f).padding(start=10.dp)){
                                Text((index+1).toString()+". "+(entry?.name ?: "#"+slot.pokemonId),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                                Text(build.role,style=MaterialTheme.typography.bodySmall)
                                if(slot.alternatives.isNotEmpty()){
                                    val names=slot.alternatives.map{id->national.firstOrNull{it.id==id}?.name ?: "#"+id}
                                    Text("Alternativas: "+names.joinToString(" / "),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                                }
                            }
                            Icon(Icons.Default.ChevronRight,null)
                        }
                        HorizontalDivider(Modifier.padding(vertical=8.dp))
                        Text("Build de campanha",fontWeight=FontWeight.SemiBold)
                        Text("Moves: "+build.moves.joinToString(" · "),style=MaterialTheme.typography.bodySmall)
                        Text("Nature: "+build.nature,style=MaterialTheme.typography.bodySmall)
                        Text("Item: "+build.item,style=MaterialTheme.typography.bodySmall)
                        Text(build.notes,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=4.dp))
                    }
                }
            }
            item{
                Button(
                    onClick={
                        val name=game.substringBefore(" / ")+" · "+team.starter+" · "+team.phase.label
                        TeamStore.createTeam(name,team.slots.map{it.pokemonId})
                        createdMessage="Time criado em Meus Times."
                    },
                    modifier=Modifier.fillMaxWidth().height(52.dp)
                ){
                    Icon(Icons.Default.AddCircle,null)
                    Spacer(Modifier.width(8.dp))
                    Text("Usar este time")
                }
                createdMessage?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=6.dp))}
            }
        }
        item{
            Card(shape=RoundedCornerShape(18.dp)){
                Column(Modifier.fillMaxWidth().padding(12.dp)){
                    Text("Como usar",fontWeight=FontWeight.Bold)
                    Text("Os presets são para zerar a história com pouco grind. No early/mid, os golpes mostrados são alvos de build: use o equivalente disponível até desbloquear o golpe indicado. Trocas e exclusividades têm alternativas quando relevante.",style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        item{Spacer(Modifier.height(24.dp))}
    }
}
