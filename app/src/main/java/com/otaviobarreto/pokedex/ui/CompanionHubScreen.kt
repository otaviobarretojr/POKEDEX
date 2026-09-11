package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

private data class UniversalResult(val kind:String,val title:String,val subtitle:String,val pokemonId:Int?=null,val refKind:String?=null,val refName:String?=null)

@Composable
fun CompanionHubScreen(
    onPokemonClick:(Int)->Unit,
    onReferenceClick:(String,String)->Unit,
    onOpenGame:(String)->Unit
){
    var query by remember { mutableStateOf("") }
    var refs by remember { mutableStateOf<List<UniversalResult>>(emptyList()) }
    var loadingRefs by remember { mutableStateOf(false) }
    var selectedPokemon by remember { mutableStateOf<Int?>(null) }
    var availability by remember { mutableStateOf<List<CaptureAvailability>>(emptyList()) }
    var loadingGuide by remember { mutableStateOf(false) }
    var backupText by remember { mutableStateOf("") }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val clipboard=LocalClipboardManager.current
    val dex=PokedexDataStore.cachedNationalDex().orEmpty()
    val captured=CollectionStore.capturedIds
    val pokemonResults=remember(query,dex){
        val q=query.trim().removePrefix("#")
        if(q.length<2) emptyList() else dex.asSequence().filter{it.name.contains(q,true)||it.id.toString()==q}.take(12)
            .map{UniversalResult("Pokémon",it.name,"#"+it.id.toString().padStart(4,'0')+" · G"+it.generation,pokemonId=it.id)}.toList()
    }

    LaunchedEffect(query){
        val q=query.trim()
        if(q.length<2){refs=emptyList();loadingRefs=false;return@LaunchedEffect}
        loadingRefs=true
        refs=withContext(Dispatchers.IO){
            coroutineScope {
                listOf("move","ability","item").map { kind ->
                    async {
                        runCatching { ReferenceCatalogService.load(kind) }.getOrDefault(emptyList())
                            .asSequence().filter{it.name.contains(q,true)}.take(5)
                            .map{UniversalResult(when(kind){"move"->"Golpe";"ability"->"Habilidade";else->"Item"},it.name.replace("-"," "),it.url,refKind=kind,refName=it.name)}.toList()
                    }
                }.flatMap { it.await() }
            }
        }
        loadingRefs=false
    }

    LaunchedEffect(selectedPokemon){
        val id=selectedPokemon ?: return@LaunchedEffect
        loadingGuide=true
        availability=CaptureGuideService.find(id)
        loadingGuide=false
    }

    val progress=if(dex.isEmpty())0f else captured.size.toFloat()/dex.size
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item {
            Text("COMPANION",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
            Text("Busca, captura, progresso e backup em um só lugar.",style=MaterialTheme.typography.bodyMedium)
        }
        item {
            Card(shape=RoundedCornerShape(22.dp)){
                Column(Modifier.fillMaxWidth().padding(16.dp)){
                    Text("Progresso Nacional",fontWeight=FontWeight.Bold)
                    Text(captured.size.toString()+" / "+dex.size.coerceAtLeast(PokeApiService.MAX_NATIONAL_DEX_ID)+" capturados",style=MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().padding(top=8.dp))
                    Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(CollectionStore.boxNames.size.toString()+" Boxes",style=MaterialTheme.typography.labelMedium)
                        Text(TeamStore.teams.size.toString()+" Times",style=MaterialTheme.typography.labelMedium)
                        Text(CollectionStore.boxes.values.sumOf{it.size}.toString()+" em Boxes",style=MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item {
            OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},label={Text("Busca universal")},placeholder={Text("Pokémon, golpe, habilidade ou item")})
        }
        if(query.trim().length>=2){
            item { Text("Resultados",fontWeight=FontWeight.Bold) }
            items(pokemonResults+refs,key={it.kind+it.title+it.subtitle}){result->
                Card(Modifier.fillMaxWidth().clickable{
                    result.pokemonId?.let{selectedPokemon=it;onPokemonClick(it)}
                    if(result.refKind!=null&&result.refName!=null)onReferenceClick(result.refKind,result.refName)
                }){
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                        Icon(if(result.pokemonId!=null)Icons.Default.CatchingPokemon else Icons.Default.MenuBook,null)
                        Column(Modifier.weight(1f).padding(start=10.dp)){Text(result.title,fontWeight=FontWeight.SemiBold);Text(result.kind+" · "+result.subtitle,style=MaterialTheme.typography.bodySmall)}
                        Icon(Icons.Default.ChevronRight,null)
                    }
                }
            }
            if(loadingRefs)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
        }
        item {
            HorizontalDivider()
            Text("Guia de captura",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            Text("Pesquise um Pokémon e toque nele para consultar disponibilidade nos jogos.",style=MaterialTheme.typography.bodySmall)
        }
        selectedPokemon?.let{id->
            if(loadingGuide)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
            if(!loadingGuide && availability.isEmpty()) item { Text("Nenhuma disponibilidade regional encontrada para #"+id+".") }
            items(availability,key={it.source}){entry->
                Card(Modifier.fillMaxWidth().clickable{onOpenGame(entry.source)}){
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.LocationOn,null)
                        Column(Modifier.weight(1f).padding(start=10.dp)){Text(entry.game,fontWeight=FontWeight.Bold);Text(entry.region+" · "+if(entry.encounterCount>0)entry.encounterCount.toString()+" áreas de encontro" else "Disponível nesta Pokédex",style=MaterialTheme.typography.bodySmall)}
                        Icon(Icons.Default.ChevronRight,null)
                    }
                }
            }
        }
        item {
            HorizontalDivider()
            Text("Backup e restauração",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            Text("O backup inclui capturados, Boxes e Times.",style=MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button(onClick={val data=BackupService.exportJson();backupText=data;clipboard.setText(AnnotatedString(data));backupMessage="Backup copiado para a área de transferência."},modifier=Modifier.weight(1f)){Icon(Icons.Default.Backup,null);Spacer(Modifier.width(6.dp));Text("Copiar backup")}
                OutlinedButton(onClick={backupText=clipboard.getText()?.text.orEmpty()},modifier=Modifier.weight(1f)){Icon(Icons.Default.ContentPaste,null);Spacer(Modifier.width(6.dp));Text("Colar")}
            }
            OutlinedTextField(backupText,{backupText=it},Modifier.fillMaxWidth().heightIn(min=120.dp).padding(top=8.dp),label={Text("Backup JSON")})
            Button(onClick={backupMessage=if(BackupService.importJson(backupText))"Backup restaurado com sucesso." else "Backup inválido ou incompatível."},modifier=Modifier.fillMaxWidth().padding(top=8.dp),enabled=backupText.isNotBlank()){Text("Restaurar backup")}
            backupMessage?.let{Text(it,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=6.dp))}
        }
        item{Spacer(Modifier.height(24.dp))}
    }
}
