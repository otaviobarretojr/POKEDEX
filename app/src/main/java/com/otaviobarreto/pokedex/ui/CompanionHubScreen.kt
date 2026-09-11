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
private data class GameProgress(val game:String,val region:String,val source:String,val captured:Int,val total:Int)
private fun smartSearchTerm(raw:String):String {
    val stop = setOf("onde","pego","pegar","capturo","capturar","como","consigo","achar","encontro","no","na","em","scarlet","violet","pokemon","pokémon")
    return raw.lowercase().replace("#"," ").split(Regex("\\s+")).filter { it.isNotBlank() && it !in stop }.joinToString(" ")
}

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
    var gameProgress by remember { mutableStateOf<List<GameProgress>>(emptyList()) }
    var loadingProgress by remember { mutableStateOf(false) }
    var plannerGame by remember { mutableStateOf(AppGameCatalog.games.first().label) }
    var plannerPlan by remember { mutableStateOf<CapturePlan?>(null) }
    var plannerLoading by remember { mutableStateOf(false) }
    var plannerMenu by remember { mutableStateOf(false) }
    val clipboard=LocalClipboardManager.current
    val dex=PokedexDataStore.cachedNationalDex().orEmpty()
    val captured=CollectionStore.capturedIds
    val pokemonResults=remember(query,dex){
        val q=smartSearchTerm(query).ifBlank { query.trim().removePrefix("#") }
        if(q.length<2) emptyList() else dex.asSequence().filter{it.name.contains(q,true)||it.id.toString()==q}.take(12)
            .map{UniversalResult("Pokémon",it.name,"#"+it.id.toString().padStart(4,'0')+" · G"+it.generation,pokemonId=it.id)}.toList()
    }

    LaunchedEffect(query){
        val q=smartSearchTerm(query).ifBlank { query.trim() }
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

    LaunchedEffect(Unit){
        loadingProgress=true
        gameProgress=withContext(Dispatchers.IO){
            coroutineScope {
                AppGameCatalog.games.flatMap { game ->
                    game.regions.map { region ->
                        async {
                            val context=GameContext.fromSource(region.source) ?: return@async null
                            val dexEntries=runCatching { GameDexService.loadGameDex(context) }.getOrDefault(emptyList())
                            GameProgress(game.label,region.label,region.source,dexEntries.count{it.nationalId in CollectionStore.capturedIds},dexEntries.size)
                        }
                    }
                }.map { it.await() }.filterNotNull()
            }
        }
        loadingProgress=false
    }

    LaunchedEffect(selectedPokemon){
        val id=selectedPokemon ?: return@LaunchedEffect
        loadingGuide=true
        availability=CaptureGuideService.find(id)
        loadingGuide=false
    }

    LaunchedEffect(plannerGame){
        plannerLoading=true
        plannerPlan=CapturePlannerService.build(plannerGame)
        plannerLoading=false
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
            Text("Progresso por jogo",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        }
        if(loadingProgress)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
        items(gameProgress,key={it.source}){gp->
            Card(Modifier.fillMaxWidth().clickable{onOpenGame(gp.source)}){
                Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Default.Map,null)
                    Column(Modifier.weight(1f).padding(start=10.dp)){
                        Text(gp.game+" · "+gp.region,fontWeight=FontWeight.SemiBold)
                        Text(gp.captured.toString()+" / "+gp.total.toString()+" capturados",style=MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress={if(gp.total==0)0f else gp.captured.toFloat()/gp.total},modifier=Modifier.fillMaxWidth().padding(top=5.dp))
                    }
                    Icon(Icons.Default.ChevronRight,null)
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
                    if(result.pokemonId!=null) selectedPokemon=result.pokemonId
                    else if(result.refKind!=null&&result.refName!=null)onReferenceClick(result.refKind,result.refName)
                }){
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                        Icon(if(result.pokemonId!=null)Icons.Default.CatchingPokemon else Icons.Default.MenuBook,null)
                        Column(Modifier.weight(1f).padding(start=10.dp)){Text(result.title,fontWeight=FontWeight.SemiBold);Text(result.kind+" · "+result.subtitle,style=MaterialTheme.typography.bodySmall)}
                        if(result.pokemonId!=null) TextButton(onClick={onPokemonClick(result.pokemonId)}){Text("Ficha")} else Icon(Icons.Default.ChevronRight,null)
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
            item {
                val owned=id in CollectionStore.capturedIds
                Surface(shape=RoundedCornerShape(16.dp),color=if(owned)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant){
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                        Icon(if(owned)Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,null)
                        Text(if(owned)"Você já tem este Pokémon na coleção." else "Este Pokémon ainda falta na sua coleção.",Modifier.weight(1f).padding(start=10.dp),fontWeight=FontWeight.SemiBold)
                    }
                }
            }
            if(loadingGuide)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
            if(!loadingGuide && availability.isEmpty()) item { Text("Nenhuma disponibilidade regional encontrada para #"+id+".") }
            items(availability,key={it.source}){entry->
                Card(Modifier.fillMaxWidth().clickable{onOpenGame(entry.source)}){
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.LocationOn,null)
                        Column(Modifier.weight(1f).padding(start=10.dp)){
                            Text(entry.game,fontWeight=FontWeight.Bold)
                            Text(entry.region+" · "+if(entry.encounterCount>0)entry.encounterCount.toString()+" áreas de encontro" else "Disponível nesta Pokédex",style=MaterialTheme.typography.bodySmall)
                            if(entry.sampleLocations.isNotEmpty()) Text(entry.sampleLocations.joinToString(" · "),style=MaterialTheme.typography.labelSmall)
                            val methodText=listOfNotNull(entry.methods.takeIf{it.isNotEmpty()}?.joinToString("/"),entry.minLevel?.let{"Nv. "+it+(entry.maxLevel?.takeIf{m->m!=it}?.let{m->"–"+m}?:"")}).joinToString(" · ")
                            if(methodText.isNotBlank()) Text(methodText,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Default.ChevronRight,null)
                    }
                }
            }
        }
        item {
            HorizontalDivider()
            Text("Planejador de captura",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            Text("Veja o que ainda falta e o que você consegue buscar no jogo escolhido.",style=MaterialTheme.typography.bodySmall)
            ExposedDropdownMenuBox(expanded=plannerMenu,onExpandedChange={plannerMenu=!plannerMenu},modifier=Modifier.fillMaxWidth().padding(top=8.dp)){
                OutlinedTextField(plannerGame,{},Modifier.menuAnchor().fillMaxWidth(),readOnly=true,singleLine=true,label={Text("Jogo")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(plannerMenu)})
                ExposedDropdownMenu(plannerMenu,{plannerMenu=false}){
                    AppGameCatalog.games.forEach{game->DropdownMenuItem({Text(game.label)},{plannerGame=game.label;plannerMenu=false})}
                }
            }
        }
        if(plannerLoading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
        plannerPlan?.let{plan->
            item {
                Card(Modifier.fillMaxWidth()){
                    Column(Modifier.fillMaxWidth().padding(14.dp)){
                        Text(plan.game,fontWeight=FontWeight.Bold)
                        Text(plan.obtainableMissing.size.toString()+" faltantes disponíveis neste jogo",color=MaterialTheme.colorScheme.primary)
                        Text(plan.externalMissing.size.toString()+" faltantes dependem de outros jogos/trocas/HOME",style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if(plan.obtainableMissing.isNotEmpty()) {
                item { Text("Próximos para capturar",fontWeight=FontWeight.Bold) }
                items(plan.obtainableMissing.take(12),key={"plan-"+it.id}){p->
                    Card(Modifier.fillMaxWidth().clickable{selectedPokemon=p.id}){
                        Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                            Icon(Icons.Default.CatchingPokemon,null)
                            Column(Modifier.weight(1f).padding(start=10.dp)){Text(p.name,fontWeight=FontWeight.SemiBold);Text("#"+p.id.toString().padStart(4,'0'),style=MaterialTheme.typography.bodySmall)}
                            TextButton({onPokemonClick(p.id)}){Text("Ficha")}
                        }
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
