package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class FormPreview(
    val label:String,
    val detailName:String,
    val formId:Int,
    val shiny:Boolean,
    val kind:PokemonFormKind
){
    val imageUrl:String
        get()="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+
            (if(shiny)"shiny/" else "")+formId+".png"
    val fallbackImageUrl:String
        get()="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+formId+".png"
}

private fun prettyFormLabel(baseName:String,rawName:String,shiny:Boolean):String{
    val suffix=rawName.removePrefix(baseName).trim().ifBlank{"Padrão"}
    val pretty=when{
        suffix.equals("Alola",true) -> "Forma de Alola"
        suffix.equals("Galar",true) -> "Forma de Galar"
        suffix.equals("Hisui",true) -> "Forma de Hisui"
        suffix.startsWith("Paldea",true) -> "Forma de "+suffix
        else -> suffix
    }
    return pretty+(if(shiny)" · Shiny" else "")
}

@Composable
private fun ArtworkWithFallback(
    model:String,
    fallbackModel:String?=null,
    contentDescription:String?,
    modifier:Modifier
){
    var current by remember(model,fallbackModel){mutableStateOf(model)}
    var failed by remember(model,fallbackModel){mutableStateOf(false)}
    Box(modifier,contentAlignment=Alignment.Center){
        if(!failed){
            AsyncImage(
                model=current,
                contentDescription=contentDescription,
                modifier=Modifier.fillMaxSize(),
                contentScale=ContentScale.Fit,
                onError={
                    if(!fallbackModel.isNullOrBlank() && current!=fallbackModel) current=fallbackModel
                    else failed=true
                }
            )
        }else{
            Icon(
                Icons.Default.MenuBook,
                contentDescription="Arte indisponível",
                modifier=Modifier.fillMaxSize(.42f),
                tint=MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PokedexCatalogScreen(
    onPokemonClick:(Int)->Unit,
    onOpenFormDetail:(Int,String,Boolean)->Unit
){
    var query by remember{mutableStateOf("")}
    var selectedId by remember{mutableStateOf<Int?>(null)}
    val all=remember{PokemonRepository.all()}
    val filtered=remember(query){
        val q=query.trim().removePrefix("#")
        if(q.isBlank()) all
        else all.filter{
            it.name.contains(q,true) || it.id.toString()==q ||
                it.types.any{type->type.contains(q,true)}
        }
    }

    Column(Modifier.fillMaxSize()){
        Row(
            Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Icon(Icons.Default.MenuBook,null,Modifier.size(30.dp))
            Column(Modifier.padding(start=10.dp)){
                Text("Pokédex",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
                Text("Nacional #0001–#1025 · formas e Shiny",color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedTextField(
            value=query,
            onValueChange={query=it},
            modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp),
            singleLine=true,
            leadingIcon={Icon(Icons.Default.Search,null)},
            placeholder={Text("Nome ou número da National Dex")}
        )
        LazyVerticalGrid(
            columns=GridCells.Adaptive(112.dp),
            modifier=Modifier.fillMaxSize().padding(top=10.dp),
            contentPadding=PaddingValues(start=12.dp,end=12.dp,bottom=24.dp),
            horizontalArrangement=Arrangement.spacedBy(8.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            items(filtered,key={it.id}){pk->
                Card(
                    onClick={selectedId=pk.id},
                    shape=RoundedCornerShape(18.dp)
                ){
                    Column(
                        Modifier.fillMaxWidth().padding(8.dp),
                        horizontalAlignment=Alignment.CenterHorizontally
                    ){
                        ArtworkWithFallback(
                            model=pk.spriteUrl,
                            contentDescription=pk.name,
                            modifier=Modifier.size(88.dp)
                        )
                        Text(
                            "#"+pk.id.toString().padStart(4,'0'),
                            style=MaterialTheme.typography.labelSmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            pk.name,
                            fontWeight=FontWeight.Bold,
                            maxLines=1,
                            overflow=TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    selectedId?.let{id->
        PokedexFormsDialog(
            pokemonId=id,
            onDismiss={selectedId=null},
            onOpenBase={
                selectedId=null
                onPokemonClick(id)
            },
            onOpenFormDetail={formId,name,shiny->
                selectedId=null
                onOpenFormDetail(formId,name,shiny)
            }
        )
    }
}

@Composable
private fun PokedexFormsDialog(
    pokemonId:Int,
    onDismiss:()->Unit,
    onOpenBase:()->Unit,
    onOpenFormDetail:(Int,String,Boolean)->Unit
){
    val base=PokemonRepository.byId(pokemonId)
    var forms by remember(pokemonId){mutableStateOf<List<PokemonFormVariant>?>(PokemonFormsService.cached(pokemonId))}
    var selectedPreview by remember{mutableStateOf<FormPreview?>(null)}
    LaunchedEffect(pokemonId){
        if(forms==null){
            forms=withContext(Dispatchers.IO){
                runCatching{PokemonFormsService.collectible(pokemonId)}.getOrDefault(emptyList())
            }
        }
    }

    val previews=remember(forms,pokemonId){
        val source=(forms.orEmpty().ifEmpty{
            listOf(PokemonFormVariant(base?.name ?: "Forma padrão",pokemonId,true,PokemonFormKind.DEFAULT))
        }).filter{it.pokemonId!=null}
        buildList{
            source.forEach{form->
                val id=form.pokemonId ?: return@forEach
                val normalLabel=prettyFormLabel(base?.name ?: "",form.name,false)
                val shinyLabel=prettyFormLabel(base?.name ?: "",form.name,true)
                val prefix=base?.name ?: "Pokémon"
                add(FormPreview(normalLabel,prefix+" — "+normalLabel,id,false,form.kind))
                add(FormPreview(shinyLabel,prefix+" — "+shinyLabel,id,true,form.kind))
            }
        }
    }

    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Surface(
            Modifier.fillMaxWidth(.94f).heightIn(max=720.dp),
            shape=RoundedCornerShape(28.dp)
        ){
            Column(Modifier.fillMaxWidth().padding(16.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(base?.name ?: "Pokémon",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
                        Text("Formas e variantes",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onDismiss){Icon(Icons.Default.Close,"Fechar")}
                }
                TextButton(onClick=onOpenBase){Text("Abrir ficha principal")}
                if(forms==null){
                    Box(Modifier.fillMaxWidth().height(160.dp),contentAlignment=Alignment.Center){
                        CircularProgressIndicator()
                    }
                }else{
                    LazyVerticalGrid(
                        columns=GridCells.Fixed(2),
                        modifier=Modifier.weight(1f,fill=false).heightIn(max=520.dp),
                        horizontalArrangement=Arrangement.spacedBy(8.dp),
                        verticalArrangement=Arrangement.spacedBy(8.dp)
                    ){
                        items(previews,key={it.label+"-"+it.formId+"-"+it.shiny}){preview->
                            Card(
                                onClick={selectedPreview=preview},
                                shape=RoundedCornerShape(18.dp)
                            ){
                                Column(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalAlignment=Alignment.CenterHorizontally
                                ){
                                    ArtworkWithFallback(
                                        model=preview.imageUrl,
                                        fallbackModel=preview.fallbackImageUrl,
                                        contentDescription=preview.label,
                                        modifier=Modifier.size(96.dp)
                                    )
                                    Text(
                                        preview.label,
                                        fontWeight=FontWeight.Bold,
                                        maxLines=2,
                                        overflow=TextOverflow.Ellipsis
                                    )
                                    Text(
                                        when(preview.kind){
                                            PokemonFormKind.BATTLE -> "Forma de batalha"
                                            PokemonFormKind.REGIONAL -> "Forma regional"
                                            PokemonFormKind.COSMETIC -> "Forma cosmética"
                                            PokemonFormKind.GENDER -> "Diferença de gênero"
                                            PokemonFormKind.DEFAULT -> if(preview.shiny)"Shiny" else "Padrão"
                                            else -> "Variante"
                                        },
                                        style=MaterialTheme.typography.labelSmall,
                                        color=MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPreview?.let{preview->
        AlertDialog(
            onDismissRequest={selectedPreview=null},
            title={Text(preview.label)},
            text={
                Column(horizontalAlignment=Alignment.CenterHorizontally){
                    ArtworkWithFallback(
                        model=preview.imageUrl,
                        fallbackModel=preview.fallbackImageUrl,
                        contentDescription=preview.label,
                        modifier=Modifier.fillMaxWidth().height(260.dp)
                    )
                    if(preview.kind==PokemonFormKind.BATTLE){
                        Text(
                            "Esta forma pode alterar atributos, tipos ou habilidades.",
                            style=MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton={
                if(preview.kind==PokemonFormKind.BATTLE){
                    Button(onClick={
                        selectedPreview=null
                        onOpenFormDetail(preview.formId,preview.detailName,preview.shiny)
                    }){Text("Ver ficha completa")}
                }else{
                    TextButton(onClick={selectedPreview=null}){Text("Fechar")}
                }
            },
            dismissButton={
                if(preview.kind==PokemonFormKind.BATTLE){
                    TextButton(onClick={selectedPreview=null}){Text("Fechar")}
                }
            }
        )
    }
}
