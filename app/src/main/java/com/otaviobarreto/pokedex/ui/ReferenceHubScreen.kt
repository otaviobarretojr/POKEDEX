package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.ReferenceCatalogService
import com.otaviobarreto.pokedex.data.ReferenceDetail
import com.otaviobarreto.pokedex.data.ReferenceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ReferenceTab(val key:String,val label:String)
private val referenceTabs=listOf(ReferenceTab("move","Golpes"),ReferenceTab("ability","Habilidades"),ReferenceTab("item","Itens"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceHubScreen(onBack:()->Unit){
 var selected by remember{mutableIntStateOf(0)}
 var query by remember{mutableStateOf("")}
 var entries by remember{mutableStateOf<List<ReferenceEntry>>(emptyList())}
 var loading by remember{mutableStateOf(true)}
 var error by remember{mutableStateOf<String?>(null)}
 var chosen by remember{mutableStateOf<ReferenceEntry?>(null)}
 var detail by remember{mutableStateOf<ReferenceDetail?>(null)}
 var detailLoading by remember{mutableStateOf(false)}
 var detailError by remember{mutableStateOf<String?>(null)}
 val tab=referenceTabs[selected]

 LaunchedEffect(tab.key){loading=true;error=null;entries=runCatching{withContext(Dispatchers.IO){ReferenceCatalogService.load(tab.key)}}.onFailure{error="Não foi possível carregar ${tab.label.lowercase()}."}.getOrDefault(emptyList());loading=false}
 LaunchedEffect(chosen,tab.key){
  val entry=chosen?:return@LaunchedEffect
  detailLoading=true;detail=null;detailError=null
  detail=runCatching{withContext(Dispatchers.IO){ReferenceCatalogService.loadDetail(tab.key,entry)}}.onFailure{detailError="Não foi possível carregar os detalhes."}.getOrNull()
  detailLoading=false
 }

 val filtered=remember(entries,query){val q=query.trim();if(q.isBlank())entries else entries.filter{pretty(it.name).contains(q,true)||it.name.contains(q,true)}}
 Scaffold(topBar={TopAppBar(title={Text("Dados Pokémon")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}})}){inner->
  Column(Modifier.fillMaxSize().padding(inner)){
   Text("Biblioteca de referência",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=16.dp,vertical=8.dp))
   Text("Consulte golpes, habilidades e itens sem sair da Pokédex.",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(horizontal=16.dp))
   TabRow(selectedTabIndex=selected,modifier=Modifier.padding(top=8.dp)){referenceTabs.forEachIndexed{i,t->Tab(selected=i==selected,onClick={selected=i;query="";chosen=null},text={Text(t.label)})}}
   OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(12.dp),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},label={Text("Buscar em ${tab.label.lowercase()}")})
   when{
    loading->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
    error!=null->Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Text(error!!)}
    else->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
     item{Text("${filtered.size} resultados",style=MaterialTheme.typography.labelMedium)}
     items(filtered,key={it.url}){entry->Card(Modifier.fillMaxWidth().clickable{chosen=entry},shape=RoundedCornerShape(14.dp)){Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(pretty(entry.name),fontWeight=FontWeight.SemiBold);Text(entry.name,style=MaterialTheme.typography.labelSmall)};Icon(Icons.Default.ChevronRight,null)}}}
     item{Spacer(Modifier.height(16.dp))}
    }
   }
  }
 }

 if(chosen!=null)ModalBottomSheet(onDismissRequest={chosen=null;detail=null},dragHandle={BottomSheetDefaults.DragHandle()}){
  when{
   detailLoading->Box(Modifier.fillMaxWidth().height(260.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()}
   detailError!=null->Box(Modifier.fillMaxWidth().height(220.dp).padding(24.dp),contentAlignment=Alignment.Center){Text(detailError!!)}
   detail!=null->ReferenceDetailSheet(detail!!)
  }
 }
}

@Composable
private fun ReferenceDetailSheet(detail:ReferenceDetail){
 LazyColumn(Modifier.fillMaxWidth().heightIn(max=650.dp),contentPadding=PaddingValues(horizontal=20.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    detail.spriteUrl?.let{AsyncImage(it,detail.name,Modifier.size(72.dp).padding(end=12.dp))}
    Column(Modifier.weight(1f)){Text(detail.name,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(when(detail.kind){"move"->"Golpe";"ability"->"Habilidade";else->"Item"},style=MaterialTheme.typography.labelLarge)}
   }
  }
  if(detail.kind=="move")item{
   Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text("Dados do golpe",fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp));ReferenceLine("Tipo",detail.type?:"—");ReferenceLine("Classe",detail.category?:"—");ReferenceLine("Poder",detail.power?.toString()?:"—");ReferenceLine("Precisão",detail.accuracy?.let{"$it%"}?:"—");ReferenceLine("PP",detail.pp?.toString()?:"—");ReferenceLine("Prioridade",detail.priority?.toString()?:"0")}}
  }
  if(detail.kind=="item"&&detail.category!=null)item{Card(Modifier.fillMaxWidth()){ReferenceLine("Categoria",detail.category,Modifier.padding(14.dp))}}
  detail.description?.takeIf{it.isNotBlank()}?.let{description->item{Text("Efeito",fontWeight=FontWeight.Bold);Text(description,style=MaterialTheme.typography.bodyMedium)}}
  if(detail.pokemonIds.isNotEmpty()){
   item{Text(if(detail.kind=="move")"Pokémon que podem aprender" else "Pokémon com esta habilidade",fontWeight=FontWeight.Bold);Text("${detail.pokemonIds.size}${if(detail.pokemonIds.size>=80)"+" else ""} listados",style=MaterialTheme.typography.labelSmall)}
   items(detail.pokemonIds.zip(detail.pokemonNames),key={it.first}){(id,name)->Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png",name,Modifier.size(48.dp));Column(Modifier.padding(start=8.dp)){Text(name,fontWeight=FontWeight.SemiBold);Text("#${id.toString().padStart(4,'0')}",style=MaterialTheme.typography.labelSmall)}}}}
  }
  item{Spacer(Modifier.height(26.dp))}
 }
}

@Composable
private fun ReferenceLine(label:String,value:String,modifier:Modifier=Modifier){Row(modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,style=MaterialTheme.typography.bodyMedium);Text(value,fontWeight=FontWeight.SemiBold)}}
private fun pretty(value:String)=value.split('-').joinToString(" "){part->part.replaceFirstChar{it.uppercase()}}
