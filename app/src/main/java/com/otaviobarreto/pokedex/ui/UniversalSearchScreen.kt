package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class UniversalResult(val kind:String,val title:String,val subtitle:String,val pokemonId:Int?=null,val rawName:String?=null)
private enum class SearchScope(val label:String){ ALL("Tudo"), POKEMON("Pokémon"), OWNED("Capturados"), MISSING("Faltando"), SHINY("Shiny"), FORMS("Formas") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSearchScreen(onBack:()->Unit,onPokemonClick:(Int)->Unit,onOpenReference:(String,String)->Unit){
    var query by rememberSaveable{mutableStateOf("")}
    var settledQuery by remember{mutableStateOf("")}
    var scopeName by rememberSaveable{mutableStateOf(SearchScope.ALL.name)}
    var generation by rememberSaveable{mutableStateOf<Int?>(null)}
    var type by rememberSaveable{mutableStateOf<String?>(null)}
    val scope=SearchScope.valueOf(scopeName)
    LaunchedEffect(query){ delay(180); settledQuery=query }

    val pokemon=remember{PokemonRepository.all()}
    val owned=CollectionStore.capturedIds
    val variants=VariantCollectionStore.ownedVariants
    val shinyIds=remember(variants){variants.filter{it.shiny}.mapTo(linkedSetOf()){it.speciesId}}
    val formIds=remember(variants){variants.filter{it.countsForFormDex()}.mapTo(linkedSetOf()){it.speciesId}}
    val types=remember{PokemonRepository.types()}
    var refs by remember{mutableStateOf<Map<String,List<ReferenceEntry>>>(emptyMap())}
    LaunchedEffect(Unit){
        refs=withContext(Dispatchers.IO){
            listOf("move","ability","item").associateWith{kind->
                runCatching{ReferenceCatalogService.load(kind)}.getOrDefault(emptyList())
            }
        }
    }

    val pokemonResults=remember(settledQuery,scope,generation,type,owned,shinyIds,formIds){
        val q=settledQuery.trim().removePrefix("#")
        pokemon.asSequence()
            .filter{pk->
                when(scope){
                    SearchScope.OWNED -> pk.id in owned
                    SearchScope.MISSING -> pk.id !in owned
                    SearchScope.SHINY -> pk.id in shinyIds
                    SearchScope.FORMS -> pk.id in formIds
                    else -> true
                }
            }
            .filter{generation==null || it.generation==generation}
            .filter{type==null || it.types.any{t->t.equals(type,true)}}
            .filter{q.isBlank() || pokemonMatch(it,q)}
            .take(80)
            .map{pk->
                UniversalResult(
                    "Pokémon",
                    pk.name,
                    "#"+pk.id.toString().padStart(4,'0')+" · "+pk.types.joinToString(" / "),
                    pk.id
                )
            }
            .toList()
    }
    val referenceResults=remember(settledQuery,refs,scope,generation,type){
        val q=settledQuery.trim()
        if(q.length<2 || scope!=SearchScope.ALL || generation!=null || type!=null) emptyList()
        else buildList{
            refs.forEach{(kind,list)->
                list.asSequence().filter{it.name.contains(q,true)}.take(12).forEach{
                    add(
                        UniversalResult(
                            kind=when(kind){"move"->"Golpe";"ability"->"Habilidade";else->"Item"},
                            title=it.name.replace("-"," ").replaceFirstChar{ch->ch.uppercase()},
                            subtitle=kind,
                            rawName=it.name
                        )
                    )
                }
            }
        }.take(40)
    }
    val results=remember(pokemonResults,referenceResults){(pokemonResults+referenceResults).take(100)}
    val hasActiveFilter=scope!=SearchScope.ALL || generation!=null || type!=null
    val shouldPrompt=settledQuery.trim().length<2 && !hasActiveFilter

    Scaffold(topBar={TopAppBar(title={Text("Busca universal")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}})}){pad->
        Column(Modifier.fillMaxSize().padding(pad)){
            OutlinedTextField(
                query,
                {query=it},
                Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),
                singleLine=true,
                leadingIcon={Icon(Icons.Default.Search,null)},
                placeholder={Text("Pokémon, tipo, habilidade, golpe ou item")}
            )
            LazyRow(
                contentPadding=PaddingValues(horizontal=16.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp)
            ){
                items(SearchScope.entries,key={it.name}){item->
                    FilterChip(
                        selected=scope==item,
                        onClick={scopeName=item.name},
                        label={Text(item.label)}
                    )
                }
            }
            LazyRow(
                contentPadding=PaddingValues(horizontal=16.dp,vertical=6.dp),
                horizontalArrangement=Arrangement.spacedBy(6.dp)
            ){
                item{FilterChip(selected=generation==null,onClick={generation=null},label={Text("Todas gerações")})}
                items((1..9).toList()){gen->
                    FilterChip(selected=generation==gen,onClick={generation=gen},label={Text("G"+gen)})
                }
            }
            LazyRow(
                contentPadding=PaddingValues(horizontal=16.dp),
                horizontalArrangement=Arrangement.spacedBy(6.dp)
            ){
                item{FilterChip(selected=type==null,onClick={type=null},label={Text("Todos tipos")})}
                items(types,key={it}){value->
                    FilterChip(selected=type==value,onClick={type=value},label={Text(value)})
                }
            }
            when{
                shouldPrompt -> DexStatusPane("Encontre qualquer coisa","Digite pelo menos 2 caracteres ou use os filtros.",Modifier.fillMaxWidth().padding(16.dp))
                results.isEmpty() -> DexStatusPane("Nenhum resultado","Tente outro nome, número, tipo ou filtro.",Modifier.fillMaxWidth().padding(16.dp))
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding=PaddingValues(start=16.dp,end=16.dp,top=8.dp,bottom=24.dp),
                    verticalArrangement=Arrangement.spacedBy(6.dp)
                ){
                    items(results,key={it.kind+":"+it.title+":"+it.pokemonId+":"+it.rawName}){r->
                        Card(Modifier.fillMaxWidth().clickable{
                            r.pokemonId?.let(onPokemonClick)
                                ?: r.rawName?.let{
                                    onOpenReference(
                                        when(r.kind){"Golpe"->"move";"Habilidade"->"ability";else->"item"},
                                        it
                                    )
                                }
                        }){
                            Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                                r.pokemonId?.let{
                                    PokemonArtwork(
                                        model=PokemonRepository.byId(it)?.spriteUrl,
                                        contentDescription=r.title,
                                        pokemonId=it,
                                        modifier=Modifier.size(54.dp)
                                    )
                                }
                                Column(Modifier.weight(1f).padding(start=if(r.pokemonId!=null)10.dp else 0.dp)){
                                    Text(r.title,fontWeight=FontWeight.Bold)
                                    Text(r.kind+" · "+r.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun pokemonMatch(pokemon:PokemonSummary,query:String):Boolean {
    if(pokemon.id.toString()==query) return true
    if(pokemon.name.contains(query,true)) return true
    if(pokemon.types.any{it.contains(query,true)}) return true
    if(pokemon.abilities.any{it.contains(query,true)}) return true
    if(query.length<4) return false
    return levenshtein(pokemon.name.lowercase(),query.lowercase())<=2
}

private fun levenshtein(a:String,b:String):Int {
    if(a==b) return 0
    if(a.isEmpty()) return b.length
    if(b.isEmpty()) return a.length
    var previous=IntArray(b.length+1){it}
    for(i in a.indices){
        val current=IntArray(b.length+1)
        current[0]=i+1
        for(j in b.indices){
            val cost=if(a[i]==b[j])0 else 1
            current[j+1]=minOf(current[j]+1,previous[j+1]+1,previous[j]+cost)
        }
        previous=current
    }
    return previous[b.length]
}
