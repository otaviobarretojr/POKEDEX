package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.PokedexDataStore
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.PokemonStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class DetailV2Bundle(
    val pokemon:PokeApiService.RemotePokemonDetail,
    val species:PokeApiService.SpeciesInfo,
    val evolutions:List<PokeApiService.EvolutionStage>,
    val encounters:List<PokeApiService.EncounterLocation>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailV2Screen(id:Int,source:String?=null,onBack:()->Unit,onOpenLocation:(()->Unit)?=null){
 var bundle by remember(id){mutableStateOf<DetailV2Bundle?>(null)}
 var error by remember(id){mutableStateOf<String?>(null)}
 var retry by remember{mutableIntStateOf(0)}
 var tab by remember(id){mutableIntStateOf(0)}
 val context=remember(source){GameContext.fromSource(source)}
 LaunchedEffect(id,retry){error=null;runCatching{withContext(Dispatchers.IO){val p=PokedexDataStore.pokemon(id);val s=PokedexDataStore.species(id);val e=s.evolutionChainUrl?.let{PokedexDataStore.evolutions(it)}?:emptyList();val locations=PokedexDataStore.encounters(id);DetailV2Bundle(p,s,e,locations)}}.onSuccess{bundle=it}.onFailure{error="Não foi possível carregar os dados deste Pokémon."}}
 Scaffold(topBar={TopAppBar(title={Column{Text(bundle?.pokemon?.name?:"Pokémon #$id",fontWeight=FontWeight.Bold);context?.let{Text("${it.label} · ${it.regionLabel}",style=MaterialTheme.typography.labelSmall)}}},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}})}){pad->
  when{bundle!=null->DetailV2Content(bundle!!,tab,{tab=it},context,onOpenLocation,Modifier.padding(pad));error!=null->Box(Modifier.fillMaxSize().padding(pad),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(error!!);Button({retry++},Modifier.padding(top=12.dp)){Text("Tentar novamente")}}};else->Box(Modifier.fillMaxSize().padding(pad),contentAlignment=Alignment.Center){CircularProgressIndicator()}}
 }
}

@Composable private fun DetailV2Content(b:DetailV2Bundle,tab:Int,setTab:(Int)->Unit,context:GameContext?,openLocation:(()->Unit)?,modifier:Modifier){
 val tabs=listOf("Info","Stats","Evolução","Golpes","Localização")
 Column(modifier.fillMaxSize()){
  Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(b.pokemon.spriteUrl,b.pokemon.name,Modifier.size(128.dp));Column(Modifier.weight(1f)){Text("#${b.pokemon.id.toString().padStart(4,'0')}",style=MaterialTheme.typography.labelLarge);Text(b.pokemon.types.joinToString(" / "),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text("${String.format("%.1f",b.pokemon.heightDecimeters/10.0)} m · ${String.format("%.1f",b.pokemon.weightHectograms/10.0)} kg",style=MaterialTheme.typography.bodyMedium);context?.let{AssistChip({}, {Text(it.regionLabel)})}}}
  ScrollableTabRow(selectedTabIndex=tab,edgePadding=8.dp){tabs.forEachIndexed{i,t->Tab(tab==i,{setTab(i)},text={Text(t)})}}
  when(tab){0->V2Info(b);1->V2Stats(b.pokemon.stats);2->V2Evolution(b.evolutions);3->V2Moves(b.pokemon.moves);else->V2Locations(b.encounters,context,openLocation)}
 }
}

@Composable private fun V2Info(b:DetailV2Bundle){LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Text("Informações",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=14.dp));V2InfoLine("Taxa de captura",b.species.captureRate.toString());V2InfoLine("Felicidade base",b.species.baseHappiness.toString());V2InfoLine("Habitat",b.species.habitat?:"—");V2InfoLine("Crescimento",b.species.growthRate?:"—");V2InfoLine("Grupos de ovo",b.species.eggGroups.joinToString().ifBlank{"—"});Text("Habilidades",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp));b.pokemon.abilities.forEach{Text("• $it")};b.species.flavorText?.let{Text("Descrição",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp));Text(it)};Spacer(Modifier.height(20.dp))}}}
@Composable private fun V2InfoLine(label:String,value:String){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,fontWeight=FontWeight.SemiBold)}}
@Composable private fun V2Stats(s:PokemonStats){val rows=listOf("HP" to s.hp,"Ataque" to s.attack,"Defesa" to s.defense,"Ataque Esp." to s.specialAttack,"Defesa Esp." to s.specialDefense,"Velocidade" to s.speed);LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(rows){(name,v)->Column{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name,fontWeight=FontWeight.SemiBold);Text(v.toString())};LinearProgressIndicator(progress={ (v/200f).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(top=4.dp))}};item{HorizontalDivider();V2InfoLine("Total",rows.sumOf{it.second}.toString())}}}
@Composable private fun V2Evolution(e:List<PokeApiService.EvolutionStage>){LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){if(e.isEmpty())item{Text("Nenhuma evolução encontrada.")}else items(e,key={it.pokemonId}){stage->Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${stage.pokemonId}.png",stage.name,Modifier.size(70.dp));Column(Modifier.padding(start=10.dp)){Text(stage.name,fontWeight=FontWeight.Bold);Text(stage.requirement?:"Forma inicial",style=MaterialTheme.typography.bodySmall)}}}}}}
@Composable private fun V2Moves(moves:List<PokeApiService.RemoteMove>){LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){items(moves,key={it.name}){m->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp)){Text(m.name,fontWeight=FontWeight.SemiBold);Text(m.methods.joinToString(" · ").ifBlank{"Método não informado"},style=MaterialTheme.typography.bodySmall)}}}}}
@Composable private fun V2Locations(encounters:List<PokeApiService.EncounterLocation>,context:GameContext?,openLocation:(()->Unit)?){val visible=if(context==null)encounters else encounters.mapNotNull{e->val versions=e.versions.filter(context::matchesVersion);val details=e.details.filter{context.matchesVersion(it.version)};if(versions.isEmpty()&&details.isEmpty())null else e.copy(versions=versions,details=details)};LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{if(openLocation!=null)Button(openLocation,Modifier.fillMaxWidth()){Icon(Icons.Default.LocationOn,null);Spacer(Modifier.width(8.dp));Text("Abrir localização / mapa")};Text(if(context==null)"Todas as versões" else "${context.label} · ${context.regionLabel}",style=MaterialTheme.typography.labelLarge,modifier=Modifier.padding(top=8.dp))};if(visible.isEmpty())item{Text("A PokéAPI não possui encontros detalhados para este contexto. O mapa regional pode conter dados complementares quando disponíveis.")}else items(visible,key={it.location}){e->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp)){Text(e.location,fontWeight=FontWeight.Bold);e.details.take(4).forEach{d->val lv=when{d.minLevel>0&&d.maxLevel>d.minLevel->"Nv. ${d.minLevel}–${d.maxLevel}";d.minLevel>0->"Nv. ${d.minLevel}";else->null};Text(listOfNotNull(d.method,lv).joinToString(" · "),style=MaterialTheme.typography.bodySmall)}}}}}}
