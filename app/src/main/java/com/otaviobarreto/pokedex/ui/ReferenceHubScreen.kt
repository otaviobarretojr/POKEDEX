package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.ReferenceCatalogService
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
 val tab=referenceTabs[selected]
 LaunchedEffect(tab.key){loading=true;error=null;entries=runCatching{withContext(Dispatchers.IO){ReferenceCatalogService.load(tab.key)}}.onFailure{error="Não foi possível carregar ${tab.label.lowercase()}."}.getOrDefault(emptyList());loading=false}
 val filtered=remember(entries,query){val q=query.trim();if(q.isBlank())entries else entries.filter{pretty(it.name).contains(q,true)||it.name.contains(q,true)}}
 Scaffold(topBar={TopAppBar(title={Text("Dados Pokémon")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}})}){inner->
  Column(Modifier.fillMaxSize().padding(inner)){
   Text("Biblioteca de referência",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=16.dp,vertical=8.dp))
   Text("Consulte golpes, habilidades e itens sem sair da Pokédex.",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(horizontal=16.dp))
   TabRow(selectedTabIndex=selected,modifier=Modifier.padding(top=8.dp)){referenceTabs.forEachIndexed{i,t->Tab(selected=i==selected,onClick={selected=i;query=""},text={Text(t.label)})}}
   OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(12.dp),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},label={Text("Buscar em ${tab.label.lowercase()}")})
   when{loading->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()};error!=null->Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Text(error!!)};else->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){item{Text("${filtered.size} resultados",style=MaterialTheme.typography.labelMedium)};items(filtered,key={it.url}){entry->Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(pretty(entry.name),fontWeight=FontWeight.SemiBold);Text(entry.name,style=MaterialTheme.typography.labelSmall)}}}};item{Spacer(Modifier.height(16.dp))}}}
  }
 }
}

private fun pretty(value:String)=value.split('-').joinToString(" "){part->part.replaceFirstChar{it.uppercase()}}
