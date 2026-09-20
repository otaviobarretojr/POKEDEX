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
    onPokemonClick:(Int,String?)->Unit,
    initialGame:String?=null,
    initialPhase:String?=null,
    initialStepId:String?=null
){
    val journeyContext=initialGame?.takeIf{it in TeamCampaignCatalog.switchGames}
    val initial=journeyContext
        ?: AppStatePreferences.activeGame.takeIf{it in TeamCampaignCatalog.switchGames}
        ?: TeamCampaignCatalog.switchGames.first()
    var game by remember(initial) { mutableStateOf(initial) }
    var starterId by remember(game) {
        mutableIntStateOf(
            AppStatePreferences.journeyStarterForGame(game)
                ?: JourneyStarterCatalog.bestForGame(game)?.pokemonId
                ?: TeamCampaignCatalog.starters(game).first().second
        )
    }
    val journeySmartContext=remember(game,initialStepId,JourneyProgressStore.revision){
        if(journeyContext!=null){
            initialStepId?.let{JourneySmartProgress.contextForStep(game,it)} ?: JourneySmartProgress.context(game)
        }else null
    }
    val automaticPhase=remember(game,journeySmartContext,initialPhase){
        journeySmartContext?.phase
            ?: CampaignPhase.values().firstOrNull{it.name==initialPhase}
            ?: CampaignPhase.EARLY
    }
    var phase by remember(game,automaticPhase) { mutableStateOf(automaticPhase) }
    var gameMenu by remember { mutableStateOf(false) }
    var editStarter by remember { mutableStateOf(false) }
    var starterUpdateMessage by remember { mutableStateOf<String?>(null) }
    var showJourneyAdjustments by remember { mutableStateOf(false) }
    var expandedBuilds by remember { mutableStateOf(emptySet<Int>()) }
    var showHelp by remember { mutableStateOf(false) }
    var createdMessage by remember { mutableStateOf<String?>(null) }
    val preset=remember(game,starterId,phase){TeamCampaignCatalog.preset(game,starterId,phase)}
    val dynamic=remember(game,starterId,journeyContext,initialStepId,JourneyProgressStore.revision){
        if(journeyContext!=null && JourneyCatalog.steps(game).isNotEmpty()){
            JourneyDynamicTeamCatalog.suggestion(game,starterId,initialStepId)
        }else null
    }
    val displaySlots=if(dynamic!=null && phase==dynamic.preset?.phase)dynamic.adjustedSlots else preset?.slots.orEmpty()
    val national=remember{PokedexDataStore.cachedNationalDex().orEmpty()}\n    val nationalById=remember(national){national.associateBy{it.id}}
    val source=remember(game){AppStatePreferences.activeRegionForGame(game) ?: AppGameCatalog.games.firstOrNull{it.label==game}?.regions?.firstOrNull()?.source}

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        item{
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text("MEU TIME",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
                    Text(if(journeyContext!=null)"PLANO DA JORNADA" else "GUIA DE CAMPANHA · SWITCH",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick=onBackToMyTeams){Icon(Icons.Default.Groups,null);Spacer(Modifier.width(4.dp));Text("Meus times")}
            }
        }
        item{
            Card(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg)){
                Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    if(journeyContext!=null){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text(game,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                                Text(
                                    "Jornada ativa · "+(journeySmartContext?.phaseLabel ?: phase.label)+" · inicial sincronizado",
                                    style=MaterialTheme.typography.bodySmall,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AssistChip(
                                onClick={},
                                enabled=false,
                                label={Text("Automático")},
                                leadingIcon={Icon(Icons.Default.AutoAwesome,null,Modifier.size(16.dp))}
                            )
                        }
                        Row(verticalAlignment=Alignment.CenterVertically){
                            val starterName=TeamCampaignCatalog.starters(game).firstOrNull{it.second==starterId}?.first ?: "Inicial"
                            Text("Inicial: "+starterName,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                            TextButton(onClick={editStarter=!editStarter}){Text(if(editStarter)"Concluir" else "Editar inicial")}
                        }
                        if(editStarter){
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement=Arrangement.spacedBy(8.dp)
                            ){
                                TeamCampaignCatalog.starters(game).forEach{(name,id)->
                                    FilterChip(
                                        selected=starterId==id,
                                        onClick={
                                            starterId=id
                                            AppStatePreferences.setJourneyStarterForGame(game,id)
                                            starterUpdateMessage="Inicial atualizado. As sugestões do time foram recalculadas para a sua Jornada."
                                        },
                                        label={Text(name)},
                                        leadingIcon=if(starterId==id){{Icon(Icons.Default.Check,null)}}else null
                                    )
                                }
                            }
                            starterUpdateMessage?.let{
                                Text(
                                    it,
                                    style=MaterialTheme.typography.labelSmall,
                                    color=MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }else{
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
                                        starterId=AppStatePreferences.journeyStarterForGame(g)
                                            ?: JourneyStarterCatalog.bestForGame(g)?.pokemonId
                                            ?: TeamCampaignCatalog.starters(g).first().second
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
        }
        preset?.let{team->
            dynamic?.takeIf{phase==it.preset?.phase}?.let{smart->
                item{
                    Card(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.tertiaryContainer)){
                        Column(Modifier.fillMaxWidth().padding(14.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Icon(Icons.Default.AutoAwesome,null)
                                Text("SUGESTÃO PARA SUA JORNADA",Modifier.weight(1f).padding(start=8.dp),fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelMedium)
                                if(smart.actions.isNotEmpty()){
                                    TextButton(onClick={showJourneyAdjustments=!showJourneyAdjustments}){
                                        Text(if(showJourneyAdjustments)"Ocultar" else "Ver ajustes")
                                    }
                                }
                            }
                            Text(smart.reason,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=6.dp))
                            val evolveCount=smart.actions.count{it.type==JourneyTeamActionType.EVOLVE}
                            val swapCount=smart.actions.count{it.type==JourneyTeamActionType.SWAP}
                            val catchCount=smart.actions.count{it.type==JourneyTeamActionType.CATCH}
                            if(evolveCount+swapCount+catchCount>0){
                                val parts=buildList{
                                    if(evolveCount>0)add(evolveCount.toString()+" evolução")
                                    if(swapCount>0)add(swapCount.toString()+" troca")
                                    if(catchCount>0)add(catchCount.toString()+" captura")
                                }
                                Text(parts.joinToString(" · ")+" recomendada(s)",style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=6.dp))
                            }
                            smart.focusStep?.let{step->
                                Text("Próximo foco: "+step.title+" · "+step.levelLabel,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=6.dp))
                            }
                            if(showJourneyAdjustments && smart.actions.isNotEmpty()){
                                HorizontalDivider(Modifier.padding(vertical=10.dp))
                                Text("RECOMENDAÇÕES",fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall)
                                smart.actions.forEach{action->
                                    val outName=action.fromPokemonId?.let{id->nationalById[id]?.name ?: "#"+id}
                                    val inName=nationalById[action.toPokemonId]?.name ?: "#"+action.toPokemonId
                                    val headline=when(action.type){
                                        JourneyTeamActionType.EVOLVE -> "Evolua: "+(outName ?: "Inicial")+" → "+inName
                                        JourneyTeamActionType.CATCH -> "Capture: "+inName
                                        JourneyTeamActionType.SWAP -> "Troque: "+(outName ?: "slot")+" → "+inName
                                        JourneyTeamActionType.KEEP -> "Mantenha: "+inName
                                    }
                                    Text(headline,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=6.dp))
                                    Text(action.reason,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=2.dp))
                                }
                            }
                        }
                    }
                }
            }
            item{
                Card(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){
                    Column(Modifier.fillMaxWidth().padding(14.dp)){
                        val expectedStarterId=JourneyTeamProgressCatalog.starterMemberForProgress(starterId,dynamic?.focusStep)
                        val expectedStarterName=nationalById[expectedStarterId]?.name ?: team.starter
                        val contextualPhase=dynamic?.focusStep?.let{"Antes de "+it.title} ?: team.phase.label
                        Text(expectedStarterName+" · "+contextualPhase,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                        Text(team.rationale,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=4.dp))
                        Text("Referência: "+team.sourceLabel,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=6.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            itemsIndexed(displaySlots,key={index,slot->index.toString()+"-"+slot.pokemonId}){index,slot->
                val entry=nationalById[slot.pokemonId]
                val build=TeamCampaignCatalog.buildFor(slot.pokemonId,game)
                Card(Modifier.fillMaxWidth().clickable{onPokemonClick(slot.pokemonId,source)},shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md)){
                    Column(Modifier.fillMaxWidth().padding(12.dp)){
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            PokemonArtwork(
                                model=entry?.spriteUrl ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+slot.pokemonId+".png",
                                contentDescription=entry?.name,
                                pokemonId=slot.pokemonId,
                                modifier=Modifier.size(76.dp)
                            )
                            Column(Modifier.weight(1f).padding(start=10.dp)){
                                Text((index+1).toString()+". "+(entry?.name ?: "#"+slot.pokemonId),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                                Text(build.role,style=MaterialTheme.typography.bodySmall)
                                if(slot.alternatives.isNotEmpty()){
                                    val names=slot.alternatives.map{id->nationalById[id]?.name ?: "#"+id}
                                    Text("Alternativas: "+names.joinToString(" / "),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                                }
                            }
                            IconButton(onClick={
                                expandedBuilds=if(slot.pokemonId in expandedBuilds) expandedBuilds-slot.pokemonId else expandedBuilds+slot.pokemonId
                            }){
                                Icon(if(slot.pokemonId in expandedBuilds)Icons.Default.ExpandLess else Icons.Default.ExpandMore,"Detalhes do Pokémon")
                            }
                        }
                        if(slot.pokemonId in expandedBuilds){
                            HorizontalDivider(Modifier.padding(vertical=8.dp))
                            Text("Detalhes para a Jornada",fontWeight=FontWeight.SemiBold)
                            Text("Golpes: "+build.moves.joinToString(" · "),style=MaterialTheme.typography.bodySmall)
                            Text("Nature: "+build.nature,style=MaterialTheme.typography.bodySmall)
                            Text("Item: "+build.item,style=MaterialTheme.typography.bodySmall)
                            Text(build.notes,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=4.dp))
                        }
                    }
                }
            }
            item{
                Button(
                    onClick={
                        val name=game.substringBefore(" / ")+" · "+team.starter+" · "+team.phase.label
                        TeamStore.createTeam(name,displaySlots.map{it.pokemonId})
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
            Card(
                modifier=Modifier.fillMaxWidth().clickable{showHelp=!showHelp},
                shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md)
            ){
                Column(Modifier.fillMaxWidth().padding(12.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("Dicas da Jornada",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                        Icon(if(showHelp)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
                    }
                    if(showHelp){
                        Text(
                            "As sugestões priorizam terminar a história com pouco grind. Use golpes equivalentes enquanto os indicados ainda não estiverem disponíveis; trocas e exclusivos recebem alternativas quando necessário.",
                            style=MaterialTheme.typography.bodySmall,
                            modifier=Modifier.padding(top=6.dp)
                        )
                    }
                }
            }
        }
        item{Spacer(Modifier.height(24.dp))}
    }
}
