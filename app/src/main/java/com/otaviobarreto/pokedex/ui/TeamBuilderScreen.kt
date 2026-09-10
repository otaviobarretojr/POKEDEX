package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class TeamScope(val label: String, val source: String?)
private val teamScopes = listOf(TeamScope("Pokédex Nacional", null)) + AppGameCatalog.games.flatMap { game -> game.regions.map { region -> TeamScope("${game.label} · ${region.label}", region.source) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamBuilderScreen(onPokemonClick: (Int) -> Unit) {
    val teams = TeamStore.teams
    var selectedTeamId by remember { mutableStateOf(teams.firstOrNull()?.id) }
    var query by remember { mutableStateOf("") }
    var national by remember { mutableStateOf<List<PokeApiService.DexIndexEntry>>(emptyList()) }
    var regionalIds by remember { mutableStateOf<Set<Int>?>(null) }
    var scope by remember { mutableStateOf(teamScopes.first()) }
    var scopeMenu by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var replacePokemonId by remember { mutableStateOf<Int?>(null) }
    var memberTypes by remember { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }
    var loadingDex by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { loadingDex=true; national=runCatching{withContext(Dispatchers.IO){PokedexDataStore.nationalDex()}}.getOrElse{emptyList()}; loadingDex=false }
    LaunchedEffect(scope.source) { val source=scope.source; regionalIds=if(source==null)null else GameContext.fromSource(source)?.let{ctx->runCatching{withContext(Dispatchers.IO){GameDexService.loadGameDex(ctx).map{it.nationalId}.toSet()}}.getOrElse{emptySet()}}?:emptySet() }
    LaunchedEffect(teams) { if(teams.none{it.id==selectedTeamId}) selectedTeamId=teams.firstOrNull()?.id }
    val selectedTeam=teams.firstOrNull{it.id==selectedTeamId}?:teams.firstOrNull()
    LaunchedEffect(selectedTeam?.members) { val ids=selectedTeam?.members.orEmpty(); memberTypes=withContext(Dispatchers.IO){ids.associateWith{id->runCatching{PokedexDataStore.pokemon(id).types}.getOrDefault(emptyList())}} }

    val normalized=query.trim().removePrefix("#"); val allowed=regionalIds
    val suggestions=remember(national,normalized,allowed){if(normalized.isBlank())emptyList() else national.asSequence().filter{p->(allowed==null||p.id in allowed)&&(p.name.contains(normalized,true)||p.id.toString()==normalized)}.take(8).toList()}
    val coverage=memberTypes.values.flatten().map{it.lowercase()}.distinct().sorted()

    Column(Modifier.fillMaxSize().background(Color(0xFFF8F8FC))) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("TIMES",fontSize=29.sp,fontWeight=FontWeight.Black,color=Color(0xFF151426));Text("M O N T E  ·  A N A L I S E  ·  A J U S T E",fontSize=7.sp,color=Color(0xFF72778B))};FilledTonalIconButton({showCreate=true}){Icon(Icons.Default.Add,"Novo time")}}
        ExposedDropdownMenuBox(expanded=scopeMenu,onExpandedChange={scopeMenu=!scopeMenu},modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp)){OutlinedTextField(scope.label,{},Modifier.menuAnchor().fillMaxWidth(),readOnly=true,singleLine=true,label={Text("Jogo / região")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(scopeMenu)},shape=RoundedCornerShape(18.dp));ExposedDropdownMenu(scopeMenu,{scopeMenu=false}){teamScopes.forEach{item->DropdownMenuItem({Text(item.label)},{scope=item;scopeMenu=false;query=""})}}}
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){teams.forEach{team->AssistChip({selectedTeamId=team.id},{Text("${team.name} · ${team.members.size}/6")},leadingIcon=if(team.id==selectedTeamId)({Text("✓")})else null)}}

        selectedTeam?.let { team ->
            Card(Modifier.fillMaxWidth().padding(horizontal=16.dp),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFF1F0F8))){Column(Modifier.fillMaxWidth().padding(14.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(team.name,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("${team.members.size} de 6 integrantes",style=MaterialTheme.typography.bodySmall)};IconButton({showRename=true}){Icon(Icons.Default.Edit,"Renomear")};IconButton({showDelete=true},enabled=teams.size>1){Icon(Icons.Default.DeleteOutline,"Excluir")}}
                if(team.members.isNotEmpty()){HorizontalDivider(Modifier.padding(vertical=10.dp));Text("Tipos do time",fontWeight=FontWeight.SemiBold);if(coverage.isEmpty())LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=6.dp)) else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=5.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){coverage.forEach{type->SuggestionChip({}, {Text(type.replaceFirstChar{it.uppercase()})})}};TeamDefenseSummary(memberTypes.values.toList())}
            }}
            OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},label={Text(if(replacePokemonId==null)"Adicionar Pokémon" else "Escolher substituto")},supportingText={Text(if(scope.source==null)"Busca na Pokédex Nacional" else scope.label)},shape=RoundedCornerShape(18.dp))
            if(loadingDex)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(suggestions.isNotEmpty())LazyColumn(Modifier.fillMaxWidth().heightIn(max=250.dp).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){items(suggestions,key={it.id}){pokemon->Card(Modifier.fillMaxWidth().clickable{val old=replacePokemonId;if(old==null)TeamStore.addPokemon(team.id,pokemon.id)else TeamStore.replacePokemon(team.id,old,pokemon.id);replacePokemonId=null;query=""}){Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(pokemon.spriteUrl,pokemon.name,Modifier.size(46.dp));Column(Modifier.weight(1f).padding(start=8.dp)){Text(pokemon.name,fontWeight=FontWeight.SemiBold);Text("#${pokemon.id.toString().padStart(4,'0')}",style=MaterialTheme.typography.bodySmall)};Icon(Icons.Default.AddCircle,null)}}}}
            val slots=List(6){team.members.getOrNull(it)}
            LazyVerticalGrid(columns=GridCells.Fixed(3),modifier=Modifier.fillMaxSize().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){itemsIndexed(slots){index,pokemonId->TeamSlotV2(index,pokemonId,onPokemonClick,{TeamStore.removePokemon(team.id,it)},{replacePokemonId=it;query=""},{TeamStore.moveMember(team.id,it,-1)},{TeamStore.moveMember(team.id,it,1)},memberTypes[pokemonId].orEmpty())}}
        }
    }
    if(showCreate)TeamNameDialogV2("Novo time","","Criar",{showCreate=false}){name->TeamStore.createTeam(name)?.let{selectedTeamId=it};showCreate=false}
    if(showRename&&selectedTeam!=null)TeamNameDialogV2("Renomear time",selectedTeam.name,"Salvar",{showRename=false}){name->TeamStore.renameTeam(selectedTeam.id,name);showRename=false}
    if(showDelete&&selectedTeam!=null)AlertDialog({showDelete=false},title={Text("Excluir ${selectedTeam.name}?")},text={Text("O time será apagado, mas seus Pokémon continuam nas Boxes e no Living Dex.")},confirmButton={TextButton({TeamStore.deleteTeam(selectedTeam.id);showDelete=false}){Text("Excluir")}},dismissButton={TextButton({showDelete=false}){Text("Cancelar")}})
}

@Composable private fun TeamDefenseSummary(memberTypes:List<List<String>>){
    val valid=memberTypes.filter{it.isNotEmpty()}; if(valid.isEmpty())return
    val results=valid.map(TypeMatchup::defensiveFor)
    val weakCounts=(results.flatMap{it.quadrupleWeak+it.doubleWeak}).groupingBy{it}.eachCount().toList().sortedWith(compareByDescending<Pair<String,Int>>{it.second}.thenBy{it.first})
    val resistCounts=(results.flatMap{it.halfResist+it.quarterResist+it.immune}).groupingBy{it}.eachCount()
    val exposed=weakCounts.filter{(type,count)->count>(resistCounts[type]?:0)}
    val stabTypes=valid.flatten().distinct()
    val offense=TypeMatchup.offensiveCoverageFor(stabTypes)
    val score=((offense.covered.size.toFloat()/TypeMatchup.allTypes().size)*100).toInt()
    Column(Modifier.fillMaxWidth().padding(top=10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
        HorizontalDivider();Text("Análise defensiva",fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=6.dp));Text("Fraquezas compartilhadas do time",style=MaterialTheme.typography.bodySmall)
        if(weakCounts.isEmpty())Text("Nenhuma fraqueza registrada.",style=MaterialTheme.typography.labelMedium) else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){weakCounts.take(8).forEach{(type,count)->AssistChip({}, {Text("$type · $count")})}}
        if(exposed.isNotEmpty()){Text("Pontos sem cobertura defensiva suficiente",style=MaterialTheme.typography.bodySmall);Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){exposed.take(8).forEach{(type,count)->val cover=resistCounts[type]?:0;SuggestionChip({}, {Text("$type $count× / cobre $cover")})}}}
        HorizontalDivider(Modifier.padding(top=5.dp));Text("Cobertura ofensiva por STAB",fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=4.dp));Text("$score% dos tipos podem ser atingidos com dano super efetivo usando os tipos naturais do time.",style=MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(progress={score/100f},modifier=Modifier.fillMaxWidth())
        if(offense.covered.isNotEmpty()){Text("Cobertos",style=MaterialTheme.typography.labelLarge);Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){offense.covered.forEach{type->AssistChip({}, {Text(type)})}}}
        if(offense.uncovered.isNotEmpty()){Text("Sem cobertura STAB",style=MaterialTheme.typography.labelLarge);Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){offense.uncovered.forEach{type->SuggestionChip({}, {Text(type)})}};Text("Esses tipos podem ainda ser cobertos por golpes de cobertura; esta análise considera somente STAB.",style=MaterialTheme.typography.bodySmall)}
    }
}

@Composable private fun TeamSlotV2(index:Int,pokemonId:Int?,open:(Int)->Unit,remove:(Int)->Unit,replace:(Int)->Unit,moveLeft:(Int)->Unit,moveRight:(Int)->Unit,types:List<String>){Card(Modifier.fillMaxWidth().aspectRatio(.82f),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFF1F0F8))){Box(Modifier.fillMaxSize().padding(7.dp),contentAlignment=Alignment.Center){if(pokemonId==null)Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.AddCircleOutline,null);Text("Slot ${index+1}",fontWeight=FontWeight.SemiBold);Text("Vazio",style=MaterialTheme.typography.bodySmall)}else Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally){AsyncImage(model="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$pokemonId.png",contentDescription="Pokémon #$pokemonId",contentScale=ContentScale.Fit,modifier=Modifier.weight(1f).fillMaxWidth(.82f).padding(4.dp).clickable{open(pokemonId)});Text("#${pokemonId.toString().padStart(4,'0')}",fontWeight=FontWeight.Bold);Text(types.joinToString(" / ").ifBlank{"carregando…"},style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis);Row{IconButton({moveLeft(pokemonId)},enabled=index>0){Icon(Icons.Default.ChevronLeft,null)};IconButton({replace(pokemonId)}){Icon(Icons.Default.SwapHoriz,null)};IconButton({moveRight(pokemonId)},enabled=index<5){Icon(Icons.Default.ChevronRight,null)}};TextButton({remove(pokemonId)}){Text("Remover")}}}}}
@Composable private fun TeamNameDialogV2(title:String,initial:String,confirmLabel:String,dismiss:()->Unit,confirm:(String)->Unit){var text by remember(initial){mutableStateOf(initial)};AlertDialog(dismiss,title={Text(title)},text={OutlinedTextField(text,{text=it.take(32)},singleLine=true,label={Text("Nome do time")})},confirmButton={Button({confirm(text)},enabled=text.trim().isNotBlank()){Text(confirmLabel)}},dismissButton={TextButton(dismiss){Text("Cancelar")}})}
