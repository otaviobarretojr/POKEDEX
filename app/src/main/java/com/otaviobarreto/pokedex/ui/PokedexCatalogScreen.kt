package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val kind:PokemonFormKind,
    val hasBattleDataChanges:Boolean,
    val normalImageUrl:String?=null,
    val shinyImageUrl:String?=null
){
    val imageUrl:String
        get()=(if(shiny) shinyImageUrl else normalImageUrl)
            ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+
                (if(shiny)"shiny/" else "")+formId+".png"
    val fallbackImageUrl:String
        get()=normalImageUrl
            ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+formId+".png"
}

private fun prettyFormLabel(baseName:String,rawName:String,shiny:Boolean):String =
    PokemonFormPresentation.label(baseName,rawName,shiny)

@Composable
private fun ArtworkWithFallback(
    model:String,
    fallbackModel:String?=null,
    contentDescription:String?,
    modifier:Modifier
){
    var current by remember(model,fallbackModel){mutableStateOf(model)}
    var failed by remember(model,fallbackModel){mutableStateOf(false)}
    Surface(
        modifier=modifier,
        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm),
        color=MaterialTheme.colorScheme.surfaceContainerLow
    ){
        Box(Modifier.fillMaxSize().padding(6.dp),contentAlignment=Alignment.Center){
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
}

@Composable
fun PokedexCatalogScreen(
    onPokemonClick:(Int)->Unit,
    onOpenFormDetail:(Int,String,Boolean)->Unit,
    onOpenSearch:()->Unit={},
    onOpenEvolutionCenter:()->Unit={},
    onOpenGameDex:()->Unit={}
){
    val haptic=LocalHapticFeedback.current
    var query by remember{mutableStateOf("")}
    var selectedId by remember{mutableStateOf<Int?>(null)}
    val all=remember{PokemonRepository.all()}
    val ownedIds=CollectionStore.capturedIds
    val filtered=remember(query){
        val q=query.trim().removePrefix("#")
        if(q.isBlank()) all
        else all.filter{
            it.name.contains(q,true) || it.id.toString()==q ||
                it.types.any{type->type.contains(q,true)}
        }
    }

    DexAppBackground {
    Column(Modifier.fillMaxSize()){
        CompanionContextHeader(
            title="Pokédex",
            eyebrow="National Dex",
            subtitle="#0001–#1025 · formas e Shiny",
            modifier=Modifier.padding(horizontal=PokedexDesignTokens.Spacing.Lg,vertical=PokedexDesignTokens.Spacing.Sm),
            artwork={
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription=null,
                    modifier=Modifier.align(Alignment.CenterEnd).padding(end=PokedexDesignTokens.Spacing.Xl).size(54.dp),
                    tint=MaterialTheme.colorScheme.primary.copy(alpha=.28f)
                )
            }
        )
        OutlinedTextField(
            value=query,
            onValueChange={value:String->query=value},
            modifier=Modifier.fillMaxWidth().padding(horizontal=PokedexDesignTokens.Spacing.Lg),
            singleLine=true,
            shape=RoundedCornerShape(PokedexDesignTokens.Radius.Pill),
            leadingIcon={Icon(Icons.Default.Search,null)},
            trailingIcon={
                if(query.isNotEmpty()){
                    IconButton(onClick={query=""}){
                        Icon(Icons.Default.Close,contentDescription="Limpar busca")
                    }
                }
            },
            placeholder={Text("Nome, número ou tipo")}
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal=PokedexDesignTokens.Spacing.Lg,vertical=PokedexDesignTokens.Spacing.Sm),
            horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)
        ){
            AssistChip(
                onClick=onOpenSearch,
                label={Text("Busca universal")},
                leadingIcon={Icon(Icons.Default.Search,null,Modifier.size(18.dp))}
            )
            AssistChip(
                onClick=onOpenEvolutionCenter,
                label={Text("Evoluções")},
                leadingIcon={Icon(Icons.Default.AutoAwesome,null,Modifier.size(18.dp))}
            )
        }
        TextButton(
            onClick=onOpenGameDex,
            modifier=Modifier.padding(horizontal=PokedexDesignTokens.Spacing.Lg)
        ){
            Icon(Icons.Default.Map,null,Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Pokédex do jogo · "+AppStatePreferences.activeGame)
        }
        LazyVerticalGrid(
            columns=GridCells.Adaptive(112.dp),
            modifier=Modifier.fillMaxSize().padding(top=PokedexDesignTokens.Spacing.Sm),
            contentPadding=PaddingValues(start=PokedexDesignTokens.Spacing.Lg,end=PokedexDesignTokens.Spacing.Lg,bottom=PokedexDesignTokens.Spacing.Xl),
            horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm),
            verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)
        ){
            items(filtered,key={it.id}){pk->
                val accent=PokedexDesignTokens.Colors.type(pk.types.firstOrNull())
                PokemonGridCard(
                    number=pk.id,
                    name=pk.name,
                    owned=pk.id in ownedIds,
                    onClick={haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove);selectedId=pk.id},
                    types=pk.types.map{it.uppercase()},
                    accent=accent,
                    artwork={
                        PokemonArtwork(
                            model=pk.spriteUrl,
                            contentDescription=pk.name,
                            pokemonId=pk.id,
                            modifier=Modifier.fillMaxSize().padding(PokedexDesignTokens.Spacing.Sm)
                        )
                    }
                )
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
    val haptic=LocalHapticFeedback.current
    val base=PokemonRepository.byId(pokemonId)
    var forms by remember(pokemonId){mutableStateOf<List<PokemonFormVariant>?>(PokemonFormsService.cached(pokemonId))}
    var selectedPreview by remember{mutableStateOf<FormPreview?>(null)}
    var selectedKind by remember(pokemonId){mutableStateOf<PokemonFormKind?>(null)}
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
                add(FormPreview(normalLabel,prefix+" — "+normalLabel,id,false,form.kind,form.hasBattleDataChanges,form.spriteUrl,form.shinySpriteUrl))
                add(FormPreview(shinyLabel,prefix+" — "+shinyLabel,id,true,form.kind,form.hasBattleDataChanges,form.spriteUrl,form.shinySpriteUrl))
            }
        }
    }

    val visiblePreviews=remember(previews,selectedKind){
        selectedKind?.let{k->previews.filter{it.kind==k}} ?: previews
    }

    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Surface(
            Modifier.fillMaxWidth(.94f).heightIn(max=720.dp),
            shape=RoundedCornerShape(PokedexDesignTokens.Radius.Xl)
        ){
            Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(base?.name ?: "Pokémon",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
                        Text("Formas e variantes",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onDismiss){Icon(Icons.Default.Close,"Fechar")}
                }
                TextButton(onClick=onOpenBase){Text("Abrir ficha principal")}
                if(forms!=null){
                    val kinds=previews.map{it.kind}.distinct()
                    if(kinds.size>1){
                        LazyRow(
                            modifier=Modifier.fillMaxWidth(),
                            horizontalArrangement=Arrangement.spacedBy(6.dp),
                            contentPadding=PaddingValues(vertical=4.dp)
                        ){
                            item{
                                FilterChip(
                                    selected=selectedKind==null,
                                    onClick={selectedKind=null},
                                    label={Text("Todas")}
                                )
                            }
                            items(kinds,key={it.name}){kind->
                                FilterChip(
                                    selected=selectedKind==kind,
                                    onClick={selectedKind=kind},
                                    label={Text(
                                        when(kind){
                                            PokemonFormKind.DEFAULT -> "Padrão"
                                            PokemonFormKind.REGIONAL -> "Regionais"
                                            PokemonFormKind.GENDER -> "Gênero"
                                            PokemonFormKind.BATTLE -> "Batalha"
                                            PokemonFormKind.SPECIAL -> "Especiais"
                                            PokemonFormKind.COSMETIC -> "Cosméticas"
                                            PokemonFormKind.OTHER -> "Outras"
                                        }
                                    )}
                                )
                            }
                        }
                    }
                }
                if(forms==null){
                    DexLoadingPane(
                        title="Preparando formas",
                        message="Carregando variantes e artes disponíveis.",
                        modifier=Modifier.fillMaxWidth().padding(vertical=PokedexDesignTokens.Spacing.Md)
                    )
                }else{
                    LazyVerticalGrid(
                        columns=GridCells.Fixed(2),
                        modifier=Modifier.weight(1f,fill=false).heightIn(max=520.dp),
                        horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm),
                        verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)
                    ){
                        items(visiblePreviews,key={it.label+"-"+it.formId+"-"+it.shiny}){preview->
                            val previewInteraction=remember(preview.formId,preview.shiny){MutableInteractionSource()}
                            Card(
                                modifier=Modifier.dexInteractiveSurface(interactionSource=previewInteraction,pressedScale=.97f),
                                onClick={haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove);selectedPreview=preview},
                                interactionSource=previewInteraction,
                                shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md)
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
                                        PokemonFormPresentation.behaviorLabel(preview.kind),
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
                    if(preview.hasBattleDataChanges){
                        Text(
                            "Esta forma possui dados próprios de batalha: atributos, tipos ou habilidades diferentes da forma padrão.",
                            style=MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton={
                if(preview.hasBattleDataChanges){
                    Button(onClick={
                        selectedPreview=null
                        onOpenFormDetail(preview.formId,preview.detailName,preview.shiny)
                    }){Text("Ver ficha completa")}
                }else{
                    TextButton(onClick={selectedPreview=null}){Text("Fechar")}
                }
            },
            dismissButton={
                if(preview.hasBattleDataChanges){
                    TextButton(onClick={selectedPreview=null}){Text("Fechar")}
                }
            }
        )
    }
}
