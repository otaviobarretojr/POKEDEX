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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.ReferenceCatalogService
import com.otaviobarreto.pokedex.data.ReferenceDetail
import com.otaviobarreto.pokedex.data.ReferenceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ReferenceTab(val key:String,val label:String)
private val referenceTabs=listOf(ReferenceTab("move","Golpes"),ReferenceTab("ability","Habilidades"),ReferenceTab("item","Itens"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceHubScreen(
 onBack:()->Unit,
 initialKind:String?=null,
 initialName:String?=null,
 source:String?=null,
 onPokemonClick:((Int,String?)->Unit)?=null
){
 val initialIndex=remember(initialKind){referenceTabs.indexOfFirst{it.key==initialKind}.coerceAtLeast(0)}
 var selected by remember{mutableIntStateOf(initialIndex)}
 var query by remember{mutableStateOf(initialName.orEmpty())}
 var entries by remember{mutableStateOf(ReferenceCatalogService.cached(referenceTabs[initialIndex].key).orEmpty())}
 var loading by remember{mutableStateOf(entries.isEmpty())}
 var error by remember{mutableStateOf<String?>(null)}
 var chosen by remember{mutableStateOf<ReferenceEntry?>(null)}
 var detail by remember{mutableStateOf<ReferenceDetail?>(null)}
 var detailLoading by remember{mutableStateOf(false)}
 var detailError by remember{mutableStateOf<String?>(null)}
 var deepLinkConsumed by remember(initialKind,initialName){mutableStateOf(false)}
 val tab=referenceTabs[selected]
 val directDetailMode=!initialKind.isNullOrBlank()&&!initialName.isNullOrBlank()
 val detailContext=remember(source){GameContext.fromSource(source)}

 LaunchedEffect(tab.key){
  val cached=ReferenceCatalogService.cached(tab.key).orEmpty()
  if(cached.isNotEmpty()) entries=cached
  loading=entries.isEmpty();error=null
  val loaded=runCatching{withContext(Dispatchers.IO){ReferenceCatalogService.load(tab.key)}}
   .onFailure{if(entries.isEmpty()) error="Não foi possível carregar " + tab.label.lowercase() + "."}.getOrNull()
  if(loaded!=null) entries=loaded
  loading=false
 }
 LaunchedEffect(entries,initialName,tab.key){
  if(deepLinkConsumed||initialName.isNullOrBlank()||entries.isEmpty()||tab.key!=initialKind)return@LaunchedEffect
  val wanted=normalizeReferenceName(initialName)
  chosen=entries.firstOrNull{normalizeReferenceName(it.name)==wanted||normalizeReferenceName(pretty(it.name))==wanted}
  deepLinkConsumed=true
 }
 LaunchedEffect(chosen,tab.key){
  val entry=chosen?:return@LaunchedEffect
  detail=ReferenceCatalogService.cachedDetail(tab.key,entry,detailContext)
  detailLoading=detail==null;detailError=null
  val loaded=runCatching{withContext(Dispatchers.IO){ReferenceCatalogService.loadDetail(tab.key,entry,detailContext)}}
   .onFailure{if(detail==null) detailError="Não foi possível carregar os detalhes."}.getOrNull()
  if(loaded!=null) detail=loaded
  detailLoading=false
 }

 val filtered=remember(entries,query){val q=query.trim();if(q.isBlank())entries else entries.filter{pretty(it.name).contains(q,true)||it.name.contains(q,true)}}

 if(directDetailMode){
  ReferenceDetailFullScreen(
   title=when(initialKind){"move"->"Detalhes do golpe";"ability"->"Detalhes da habilidade";"item"->"Detalhes do item";else->"Detalhes"},
   detail=detail,
   loading=detailLoading || chosen==null,
   error=detailError,
   onBack=onBack,
   source=source,
   onPokemonClick=onPokemonClick
  )
  return
 }
 Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={TopAppBar(title={Text("Dados Pokémon")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}})}){inner->
  Column(Modifier.fillMaxSize().padding(inner)){
   DexGlassSurface(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp)){
    DexSectionEyebrow("Biblioteca")
    Text("Dados Pokémon",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
    Text("Consulte golpes, habilidades e itens sem sair da Pokédex.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   Row(
    Modifier.fillMaxWidth().padding(horizontal=PokedexDesignTokens.Spacing.Lg,vertical=PokedexDesignTokens.Spacing.Xs),
    horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)
   ){
    referenceTabs.forEachIndexed{i,t->
     FilterChip(
      selected=i==selected,
      onClick={selected=i;query="";chosen=null;deepLinkConsumed=true},
      label={Text(t.label)},
      modifier=Modifier.weight(1f)
     )
    }
   }
   OutlinedTextField(
    query,
    {query=it},
    Modifier.fillMaxWidth().padding(horizontal=PokedexDesignTokens.Spacing.Lg,vertical=PokedexDesignTokens.Spacing.Sm),
    singleLine=true,
    shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),
    leadingIcon={Icon(Icons.Default.Search,null)},
    label={Text("Buscar em ${tab.label.lowercase()}")}
   )
   when{
    loading->DexStatusPane(
     "Preparando ${tab.label.lowercase()}",
     "Organizando os dados para uma abertura rápida e consistente.",
     Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg),
     loading=true
    )
    error!=null->DexStatusPane(
     "Não foi possível carregar",
     error!!,
     Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)
    )
    filtered.isEmpty()->DexStatusPane(
     "Nenhum resultado",
     "Tente outro nome ou termo de busca.",
     Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)
    )
    else->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
     item{Text("${filtered.size} resultados",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
     items(filtered,key={it.url}){entry->Card(
      Modifier.fillMaxWidth().clickable{chosen=entry},
      shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),
      colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),
      elevation=CardDefaults.cardElevation(defaultElevation=PokedexDesignTokens.Elevation.Low)
     ){Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(pretty(entry.name),fontWeight=FontWeight.SemiBold);Text(entry.name,style=MaterialTheme.typography.labelSmall)};Icon(Icons.Default.ChevronRight,null)}}}
     item{Spacer(Modifier.height(16.dp))}
    }
   }
  }
 }

 if(chosen!=null)ModalBottomSheet(onDismissRequest={chosen=null;detail=null},dragHandle={BottomSheetDefaults.DragHandle()}){
  when{
   detail!=null->ReferenceDetailSheet(detail!!,source,onPokemonClick)
   detailError!=null->DexStatusPane(
    "Detalhes indisponíveis",
    detailError!!,
    Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)
   )
   else->DexStatusPane(
    "Carregando ${pretty(chosen!!.name)}",
    "Buscando os detalhes completos.",
    Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg),
    loading=true
   )
  }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceDetailFullScreen(
 title:String,
 detail:ReferenceDetail?,
 loading:Boolean,
 error:String?,
 onBack:()->Unit,
 source:String?,
 onPokemonClick:((Int,String?)->Unit)?
){
 Scaffold(
  containerColor=MaterialTheme.colorScheme.background,
  topBar={
   TopAppBar(
    title={Text(title.ifBlank{"Detalhes"})},
    navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}}
   )
  }
 ){inner->
  when{
   detail!=null->ReferenceDetailPage(
    detail=detail,
    source=source,
    onPokemonClick=onPokemonClick,
    modifier=Modifier.fillMaxSize().padding(inner)
   )
   error!=null->DexStatusPane(
    "Detalhes indisponíveis",
    error,
    Modifier.fillMaxSize().padding(inner).padding(PokedexDesignTokens.Spacing.Lg)
   )
   else->DexStatusPane(
    "Carregando "+title,
    "Buscando os detalhes completos.",
    Modifier.fillMaxSize().padding(inner).padding(PokedexDesignTokens.Spacing.Lg),
    loading=loading
   )
  }
 }
}

@Composable
private fun ReferenceDetailPage(
 detail:ReferenceDetail,
 source:String?,
 onPokemonClick:((Int,String?)->Unit)?,
 modifier:Modifier=Modifier
){
 if(detail.kind=="move"){
  MoveReferenceDetailContent(detail,source,onPokemonClick,modifier)
 }else{
  GenericReferenceDetailContent(detail,source,onPokemonClick,modifier)
 }
}

@Composable
private fun GenericReferenceDetailContent(
 detail:ReferenceDetail,
 source:String?,
 onPokemonClick:((Int,String?)->Unit)?,
 modifier:Modifier=Modifier
){
 LazyColumn(
  modifier,
  contentPadding=PaddingValues(horizontal=20.dp,vertical=16.dp),
  verticalArrangement=Arrangement.spacedBy(12.dp)
 ){
  item{
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    detail.spriteUrl?.let{url->AsyncImage(rememberOfflineArtworkModel(url),detail.name,Modifier.size(72.dp).padding(end=12.dp))}
    Column(Modifier.weight(1f)){
     Text(detail.name,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
     Text(if(detail.kind=="ability")"Habilidade" else "Item",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
  if(detail.kind=="item"&&detail.category!=null)item{
   Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md)){
    ReferenceLine("Categoria",detail.category,Modifier.padding(14.dp))
   }
  }
  detail.description?.takeIf{it.isNotBlank()}?.let{description->item{
   Text("Efeito",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
   Text(description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }}
  if(detail.pokemonIds.isNotEmpty()){
   item{
    Text("Pokémon com esta habilidade",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
    Text("${detail.pokemonIds.size}${if(detail.pokemonIds.size>=80)"+" else ""} listados",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   items(detail.pokemonIds.zip(detail.pokemonNames),key={it.first}){(id,name)->
    ReferencePokemonRow(id,name,source,onPokemonClick)
   }
  }
  item{Spacer(Modifier.height(24.dp))}
 }
}

@Composable
private fun ReferenceDetailSheet(detail:ReferenceDetail,source:String?,onPokemonClick:((Int,String?)->Unit)?){
 if(detail.kind=="move"){
  MoveReferenceDetailContent(detail,source,onPokemonClick,Modifier.fillMaxWidth().heightIn(max=650.dp))
  return
 }
 LazyColumn(Modifier.fillMaxWidth().heightIn(max=650.dp),contentPadding=PaddingValues(horizontal=20.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){detail.spriteUrl?.let{url->AsyncImage(rememberOfflineArtworkModel(url),detail.name,Modifier.size(72.dp).padding(end=12.dp))};Column(Modifier.weight(1f)){Text(detail.name,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(if(detail.kind=="ability")"Habilidade" else "Item",style=MaterialTheme.typography.labelLarge)}}}
  if(detail.kind=="item"&&detail.category!=null)item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md)){ReferenceLine("Categoria",detail.category,Modifier.padding(14.dp))}}
  detail.description?.takeIf{it.isNotBlank()}?.let{description->item{Text("Efeito",fontWeight=FontWeight.Bold);Text(description,style=MaterialTheme.typography.bodyMedium)}}
  if(detail.pokemonIds.isNotEmpty()){
   item{Text("Pokémon com esta habilidade",fontWeight=FontWeight.Bold);Text("${detail.pokemonIds.size}${if(detail.pokemonIds.size>=80)"+" else ""} listados",style=MaterialTheme.typography.labelSmall)}
   items(detail.pokemonIds.zip(detail.pokemonNames),key={it.first}){(id,name)->Card(Modifier.fillMaxWidth().then(if(onPokemonClick!=null)Modifier.clickable{onPokemonClick(id,source)}else Modifier)){Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){PokemonArtwork("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png",name,Modifier.size(48.dp),pokemonId=id);Column(Modifier.weight(1f).padding(start=8.dp)){Text(name,fontWeight=FontWeight.SemiBold);Text("#${id.toString().padStart(4,'0')}",style=MaterialTheme.typography.labelSmall)};if(onPokemonClick!=null)Icon(Icons.Default.ChevronRight,null)}}}
  }
  item{Spacer(Modifier.height(26.dp))}
 }
}

@Composable
private fun MoveReferenceDetailContent(detail:ReferenceDetail,source:String?,onPokemonClick:((Int,String?)->Unit)?,modifier:Modifier=Modifier){
 LazyColumn(
  modifier,
  contentPadding=PaddingValues(horizontal=20.dp,vertical=8.dp),
  verticalArrangement=Arrangement.spacedBy(14.dp)
 ){
  item{
   Column{
    Text(detail.name,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
    Row(Modifier.padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){
     detail.type?.let{MoveTypeBadge(it)}
     detail.category?.let{MoveClassBadge(it)}
    }
   }
  }
  item{
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    MoveStatTile("Poder",detail.power?.toString()?:"—",Modifier.weight(1f))
    MoveStatTile("Precisão",detail.accuracy?.let{"$it%"}?:"—",Modifier.weight(1f))
    MoveStatTile("PP",detail.pp?.toString()?:"—",Modifier.weight(1f))
   }
  }
  detail.priority?.takeIf{it!=0}?.let{priority->
   item{MoveStatTile("Prioridade",priority.toString(),Modifier.fillMaxWidth())}
  }
  detail.description?.takeIf{it.isNotBlank()}?.let{description->
   item{
    Text("Efeito",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
    Text(localizeMoveEffect(description),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))
   }
  }
  if(detail.pokemonIds.isNotEmpty()){
   item{
    Text("Pokémon que podem aprender",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
    Text("${detail.pokemonIds.size}${if(detail.pokemonIds.size>=80)"+" else ""} listados",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   items(detail.pokemonIds.zip(detail.pokemonNames),key={it.first}){(id,name)->
    Card(
     Modifier.fillMaxWidth().then(if(onPokemonClick!=null)Modifier.clickable{onPokemonClick(id,source)}else Modifier),
     shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md)
    ){
     Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){
      PokemonArtwork("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png",name,Modifier.size(48.dp),pokemonId=id)
      Column(Modifier.weight(1f).padding(start=8.dp)){
       Text(name,fontWeight=FontWeight.SemiBold)
       Text("#${id.toString().padStart(4,'0')}",style=MaterialTheme.typography.labelSmall)
      }
      if(onPokemonClick!=null) Icon(Icons.Default.ChevronRight,null)
     }
    }
   }
  }
  item{Spacer(Modifier.height(26.dp))}
 }
}

@Composable
private fun MoveTypeBadge(type:String){
 val color=PokedexDesignTokens.Colors.type(type)
 Surface(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Pill),color=color){
  Text(type.uppercase(),modifier=Modifier.padding(horizontal=10.dp,vertical=5.dp),style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Black,color=Color.White)
 }
}

@Composable
private fun MoveClassBadge(category:String){
 val label=when(category.lowercase()){
  "physical"->"Físico"
  "special"->"Especial"
  "status"->"Status"
  else->category
 }
 Surface(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Pill),color=MaterialTheme.colorScheme.surfaceVariant){
  Text(label,modifier=Modifier.padding(horizontal=10.dp,vertical=5.dp),style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold)
 }
}

@Composable
private fun MoveStatTile(label:String,value:String,modifier:Modifier=Modifier){
 Surface(modifier,shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.72f)){
  Column(Modifier.padding(horizontal=10.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Text(value,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
   Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}

private fun localizeMoveEffect(value:String):String = when(value.trim()){
 "Power is doubled if the target has already received damage this turn." -> "O poder é dobrado se o alvo já tiver sofrido dano neste turno."
 "Inflicts regular damage." -> "Causa dano normal."
 else -> value
}

@Composable
private fun ReferencePokemonRow(id:Int,name:String,source:String?,onPokemonClick:((Int,String?)->Unit)?){
 Card(
  Modifier.fillMaxWidth().then(if(onPokemonClick!=null)Modifier.clickable{onPokemonClick(id,source)}else Modifier),
  shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md)
 ){
  Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
   PokemonArtwork("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png",name,Modifier.size(48.dp),pokemonId=id)
   Column(Modifier.weight(1f).padding(start=8.dp)){
    Text(name,fontWeight=FontWeight.SemiBold)
    Text("#${id.toString().padStart(4,'0')}",style=MaterialTheme.typography.labelSmall)
   }
   if(onPokemonClick!=null) Icon(Icons.Default.ChevronRight,null)
  }
 }
}

@Composable private fun ReferenceLine(label:String,value:String,modifier:Modifier=Modifier){Row(modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,style=MaterialTheme.typography.bodyMedium);Text(value,fontWeight=FontWeight.SemiBold)}}
private fun pretty(value:String)=value.split('-').joinToString(" "){part->part.replaceFirstChar{it.uppercase()}}
private fun normalizeReferenceName(value:String)=value.lowercase().replace("(oculta)","").replace(Regex("[^a-z0-9]+"),"").trim()
