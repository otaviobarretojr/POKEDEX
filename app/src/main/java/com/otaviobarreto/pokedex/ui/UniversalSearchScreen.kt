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
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class UniversalResult(val kind:String,val title:String,val subtitle:String,val pokemonId:Int?=null,val rawName:String?=null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun UniversalSearchScreen(onBack:()->Unit,onPokemonClick:(Int)->Unit,onOpenReference:(String,String)->Unit){
 var query by remember{mutableStateOf("")}
 var settledQuery by remember{mutableStateOf("")}
 LaunchedEffect(query){ delay(180); settledQuery=query }
 val pokemon=remember{PokemonRepository.all()}
 var refs by remember{mutableStateOf<Map<String,List<ReferenceEntry>>>(emptyMap())}
 LaunchedEffect(Unit){
  refs=withContext(Dispatchers.IO){
   listOf("move","ability","item").associateWith{kind->runCatching{ReferenceCatalogService.load(kind)}.getOrDefault(emptyList())}
  }
 }
 val results=remember(settledQuery,refs){
  val q=settledQuery.trim()
  if(q.length<2) emptyList() else buildList{
   pokemon.asSequence().filter{it.name.contains(q,true)||it.id.toString()==q||it.types.any{t->t.contains(q,true)}}.take(20).forEach{
    add(UniversalResult("Pokémon",it.name,"#"+it.id.toString().padStart(4,'0')+" · "+it.types.joinToString(" / "),it.id))
   }
   refs.forEach{(kind,list)->list.asSequence().filter{it.name.contains(q,true)}.take(12).forEach{
    add(UniversalResult(kind=when(kind){"move"->"Golpe";"ability"->"Habilidade";else->"Item"},title=it.name.replace("-"," ").replaceFirstChar{ch->ch.uppercase()},subtitle=kind,rawName=it.rawNameOrSelf()))
   }}
  }.take(50)
 }
 Scaffold(topBar={TopAppBar(title={Text("Busca universal")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}})}){pad->
  Column(Modifier.fillMaxSize().padding(pad)){
   OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(16.dp),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Pokémon, golpe, habilidade ou item")})
   if(query.trim().length<2) DexStatusPane("Encontre qualquer coisa","Digite pelo menos 2 caracteres para pesquisar toda a biblioteca.",Modifier.fillMaxWidth().padding(16.dp))
   else if(results.isEmpty()) DexStatusPane("Nenhum resultado","Tente outro nome, número ou tipo.",Modifier.fillMaxWidth().padding(16.dp))
   else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    items(results,key={it.kind+":"+it.title+":"+it.pokemonId}){r->
     Card(Modifier.fillMaxWidth().clickable{r.pokemonId?.let(onPokemonClick)?:r.rawName?.let{onOpenReference(when(r.kind){"Golpe"->"move";"Habilidade"->"ability";else->"item"},it)}}){
      Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
       r.pokemonId?.let{PokemonArtwork(model=PokemonRepository.byId(it)?.spriteUrl,contentDescription=r.title,pokemonId=it,modifier=Modifier.size(54.dp))}
       Column(Modifier.weight(1f).padding(start=if(r.pokemonId!=null)10.dp else 0.dp)){Text(r.title,fontWeight=FontWeight.Bold);Text(r.kind+" · "+r.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
      }
     }
    }
   }
  }
 }
}
private fun ReferenceEntry.rawNameOrSelf():String=name
