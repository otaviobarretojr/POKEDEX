package com.otaviobarreto.pokedex.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CompanionCenterScreen(
    onPokemonClick:(Int)->Unit,
    onOpenBoxes:(String?,String?)->Unit
){
    var query by remember { mutableStateOf("") }
    var restoreOpen by remember { mutableStateOf(false) }
    var restoreText by remember { mutableStateOf("") }
    var restoreStatus by remember { mutableStateOf<String?>(null) }

    val clipboard=LocalClipboardManager.current
    val context=LocalContext.current
    val scope=rememberCoroutineScope()

    val insights=remember(
        CollectionStore.capturedIds,
        CollectionStore.contextualCapturedIds,
        CollectionStore.boxes,
        VariantCollectionStore.ownedVariants
    ){ CollectionInsightsService.current() }
    val livingPlan=remember(CollectionStore.capturedIds,VariantCollectionStore.ownedVariants){
        LivingDexPlanner.current(limit=18)
    }

    val normalizedQuery=query.trim()
    val pokemonResults=remember(normalizedQuery){
        val q=normalizedQuery.removePrefix("#")
        if(q.isBlank()) emptyList() else PokemonRepository.all().filter{
            it.name.contains(q,true) ||
                it.id.toString()==q ||
                it.types.any{type->type.contains(q,true)}
        }.take(30)
    }
    val gameResults=remember(normalizedQuery){
        if(normalizedQuery.isBlank()) emptyList() else AppGameCatalog.games.filter{
            it.label.contains(normalizedQuery,true) ||
                it.subtitle.contains(normalizedQuery,true) ||
                it.regions.any{region->region.label.contains(normalizedQuery,true)}
        }.take(8)
    }
    val boxResults=remember(normalizedQuery,CollectionStore.boxNames){
        if(normalizedQuery.isBlank()) emptyList() else CollectionStore.boxNames.filter{
            it.contains(normalizedQuery,true)
        }.take(8)
    }

    val createBackup=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ){uri->
        if(uri!=null){
            scope.launch{
                val ok=withContext(Dispatchers.IO){
                    runCatching{
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{
                            it.write(AppBackupManager.exportJson())
                        } ?: error("Não foi possível abrir o arquivo.")
                    }.isSuccess
                }
                restoreStatus=if(ok)"Backup salvo em arquivo." else "Não foi possível salvar o backup."
            }
        }
    }

    val openBackup=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ){uri->
        if(uri!=null){
            scope.launch{
                val raw=withContext(Dispatchers.IO){
                    runCatching{
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}.orEmpty()
                    }.getOrDefault("")
                }
                val ok=raw.isNotBlank() && AppBackupManager.importJson(raw)
                restoreStatus=if(ok)"Backup restaurado com sucesso." else "Arquivo de backup inválido ou incompatível."
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal=16.dp),
        contentPadding=PaddingValues(top=18.dp,bottom=26.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        item{
            Text("Central",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black)
            Text(
                "Busca global, coleção, atividade recente e backup.",
                color=MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item{
            OutlinedTextField(
                value=query,
                onValueChange={query=it},
                modifier=Modifier.fillMaxWidth(),
                singleLine=true,
                leadingIcon={Icon(Icons.Default.Search,null)},
                label={Text("Pokémon, número, tipo, jogo ou Box")}
            )
        }

        if(normalizedQuery.isNotBlank()){
            if(pokemonResults.isEmpty() && gameResults.isEmpty() && boxResults.isEmpty()){
                item{Text("Nenhum resultado encontrado.")}
            }

            if(gameResults.isNotEmpty()){
                item{SectionTitle("Jogos")}
                items(gameResults,key={it.label}){game->
                    ResultCard(
                        icon={Icon(Icons.Default.Map,null)},
                        title=game.label,
                        subtitle=game.subtitle.ifBlank{"Abrir jogo"},
                        onClick={onOpenBoxes(game.label,game.regions.firstOrNull()?.source)}
                    )
                }
            }

            if(boxResults.isNotEmpty()){
                item{SectionTitle("Boxes")}
                items(boxResults,key={it}){box->
                    val count=CollectionStore.boxes[box].orEmpty().size
                    ResultCard(
                        icon={Icon(Icons.Default.GridView,null)},
                        title=box,
                        subtitle=count.toString()+" Pokémon registrados",
                        onClick={onOpenBoxes(null,null)}
                    )
                }
            }

            if(pokemonResults.isNotEmpty()){
                item{SectionTitle("Pokémon")}
                items(pokemonResults,key={it.id}){pk->
                    ResultCard(
                        icon={Icon(Icons.Default.CatchingPokemon,null)},
                        title="#"+pk.id.toString().padStart(4,'0')+" · "+pk.name,
                        subtitle=pk.types.joinToString(" / ")+" · Gen "+pk.generation,
                        onClick={onPokemonClick(pk.id)}
                    )
                }
            }
        }

        if(normalizedQuery.isBlank() && RecentActivityStore.recentPokemon.isNotEmpty()){
            item{SectionTitle("Vistos recentemente")}
            items(RecentActivityStore.recentPokemon.take(6),key={it}){id->
                val pk=PokemonRepository.byId(id)
                ResultCard(
                    icon={Icon(Icons.Default.History,null)},
                    title=pk?.let{"#"+id.toString().padStart(4,'0')+" · "+it.name} ?: "#"+id,
                    subtitle="Abrir ficha novamente",
                    onClick={onPokemonClick(id)}
                )
            }
        }

        item{
            SectionTitle("Living Dex")
            Card(shape=RoundedCornerShape(20.dp)){
                Column(Modifier.fillMaxWidth().padding(16.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(
                                insights.totalCaptured.toString()+" / "+insights.nationalDexTotal,
                                style=MaterialTheme.typography.headlineSmall,
                                fontWeight=FontWeight.Black
                            )
                            Text("Pokémon únicos registrados",color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            (insights.livingDexRatio*100).toInt().toString()+"%",
                            style=MaterialTheme.typography.titleLarge,
                            fontWeight=FontWeight.Black,
                            color=MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress={insights.livingDexRatio},
                        modifier=Modifier.fillMaxWidth().padding(top=10.dp).height(8.dp),
                        strokeCap=androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        InsightCard("Formas",insights.ownedForms.toString(),Modifier.weight(1f))
                        InsightCard("Shiny",insights.shinyVariants.toString(),Modifier.weight(1f))
                        InsightCard("Espécies c/ variantes",insights.speciesWithVariants.toString(),Modifier.weight(1f))
                    }
                }
            }
        }

        if(normalizedQuery.isBlank()){
            item{
                SectionTitle("O que falta")
                Card(shape=RoundedCornerShape(20.dp)){
                    Column(Modifier.fillMaxWidth().padding(16.dp)){
                        Text(
                            if(livingPlan.missingSpecies.isEmpty()) "Species Dex concluída!"
                            else "Próximas espécies ausentes",
                            fontWeight=FontWeight.Bold
                        )
                        if(livingPlan.missingSpecies.isEmpty()){
                            Text(
                                "Agora você pode focar em formas alternativas e Shiny.",
                                style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }else{
                            LazyRow(
                                modifier=Modifier.padding(top=10.dp),
                                horizontalArrangement=Arrangement.spacedBy(8.dp)
                            ){
                                items(livingPlan.missingSpecies,key={it}){id->
                                    val pk=PokemonRepository.byId(id)
                                    AssistChip(
                                        onClick={onPokemonClick(id)},
                                        label={
                                            Text(
                                                "#"+id.toString().padStart(4,'0')+
                                                    (pk?.name?.let{" · "+it} ?: "")
                                            )
                                        },
                                        leadingIcon={Icon(Icons.Default.CatchingPokemon,null,Modifier.size(16.dp))}
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            InsightCard("Shiny espécies",livingPlan.shinySpecies.toString(),Modifier.weight(1f))
                            InsightCard("Registros de forma",livingPlan.formRegistrations.toString(),Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item{
            SectionTitle("Sua coleção")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                InsightCard("Capturados",insights.totalCaptured.toString(),Modifier.weight(1f))
                InsightCard("Jogos",insights.gamesWithProgress.toString()+"/"+insights.totalGames,Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                InsightCard("Registros",insights.contextualRegistrations.toString(),Modifier.weight(1f))
                InsightCard("Duplicados",insights.duplicates.toString(),Modifier.weight(1f))
                InsightCard("Sem Box",insights.unboxed.toString(),Modifier.weight(1f))
            }
        }

        item{
            SectionTitle("Progresso por jogo")
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                insights.byGame.forEach{progress->
                    Card(
                        onClick={
                            val game=AppGameCatalog.adventureGames.firstOrNull{it.label==progress.game}
                            onOpenBoxes(progress.game,game?.regions?.firstOrNull()?.source)
                        },
                        shape=RoundedCornerShape(18.dp)
                    ){
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Column(Modifier.weight(1f)){
                                Text(progress.game,fontWeight=FontWeight.Bold)
                                Text(
                                    progress.regionsWithProgress.toString()+"/"+progress.totalRegions+" regiões com progresso",
                                    style=MaterialTheme.typography.bodySmall,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                progress.captured.toString(),
                                style=MaterialTheme.typography.titleMedium,
                                fontWeight=FontWeight.Black,
                                color=MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item{
            SectionTitle("Offline e desempenho")
            val cache=PokedexDataStore.cacheStats()
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                AppGameCatalog.adventureGames.forEach{game->
                    val status=OfflineGamePackManager.status(game.label)
                    Card(shape=RoundedCornerShape(18.dp)){
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Icon(
                                if(status.verified) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                                null,
                                tint=if(status.verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(Modifier.weight(1f).padding(start=10.dp)){
                                Text(game.label,fontWeight=FontWeight.Bold)
                                Text(
                                    if(status.verified) "Pacote offline pronto · "+status.pokemonCount+" Pokémon"
                                    else "Pacote offline não verificado",
                                    style=MaterialTheme.typography.bodySmall,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Card(shape=RoundedCornerShape(18.dp)){
                    Column(Modifier.fillMaxWidth().padding(14.dp)){
                        Text("Cache desta sessão",fontWeight=FontWeight.Bold)
                        Text(
                            cache.total.toString()+" entradas · Pokémon "+cache.pokemon+
                                " · espécies "+cache.species+
                                " · evoluções "+cache.evolutions,
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item{
            SectionTitle("Backup")
            Text(
                "Salve toda a coleção, Jornada, contexto atual e atividade recente. Backups antigos continuam compatíveis. A v12 preserva coleção, Jornada, times, formas e Shiny.",
                style=MaterialTheme.typography.bodyMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                Modifier.fillMaxWidth().padding(top=8.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp)
            ){
                Button(
                    onClick={
                        clipboard.setText(AnnotatedString(AppBackupManager.exportJson()))
                        restoreStatus="Backup copiado."
                    },
                    modifier=Modifier.weight(1f)
                ){
                    Icon(Icons.Default.ContentCopy,null)
                    Spacer(Modifier.width(6.dp))
                    Text("Copiar")
                }
                FilledTonalButton(
                    onClick={createBackup.launch("pokedex-backup.json")},
                    modifier=Modifier.weight(1f)
                ){
                    Icon(Icons.Default.Save,null)
                    Spacer(Modifier.width(6.dp))
                    Text("Arquivo")
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top=8.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp)
            ){
                FilledTonalButton(
                    onClick={restoreOpen=true},
                    modifier=Modifier.weight(1f)
                ){
                    Icon(Icons.Default.Restore,null)
                    Spacer(Modifier.width(6.dp))
                    Text("Colar backup")
                }
                FilledTonalButton(
                    onClick={openBackup.launch(arrayOf("application/json","text/plain"))},
                    modifier=Modifier.weight(1f)
                ){
                    Icon(Icons.Default.FolderOpen,null)
                    Spacer(Modifier.width(6.dp))
                    Text("Importar")
                }
            }

            restoreStatus?.let{
                Text(
                    it,
                    Modifier.padding(top=8.dp),
                    color=MaterialTheme.colorScheme.primary,
                    fontWeight=FontWeight.SemiBold
                )
            }
        }
    }

    if(restoreOpen){
        AlertDialog(
            onDismissRequest={restoreOpen=false},
            title={Text("Restaurar backup")},
            text={
                Column{
                    Text("Cole abaixo o backup exportado anteriormente.")
                    OutlinedTextField(
                        restoreText,
                        {restoreText=it},
                        Modifier.fillMaxWidth().heightIn(min=140.dp).padding(top=8.dp),
                        label={Text("Backup JSON")}
                    )
                }
            },
            confirmButton={
                TextButton(onClick={
                    val ok=AppBackupManager.importJson(restoreText)
                    restoreStatus=if(ok)"Backup restaurado com sucesso." else "Backup inválido ou incompatível."
                    if(ok){restoreText="";restoreOpen=false}
                }){Text("Restaurar")}
            },
            dismissButton={TextButton(onClick={restoreOpen=false}){Text("Cancelar")}}
        )
    }
}

@Composable
private fun SectionTitle(text:String){
    Text(
        text,
        style=MaterialTheme.typography.titleLarge,
        fontWeight=FontWeight.Bold,
        modifier=Modifier.padding(top=4.dp)
    )
}

@Composable
private fun ResultCard(
    icon:@Composable ()->Unit,
    title:String,
    subtitle:String,
    onClick:()->Unit
){
    Card(onClick=onClick,shape=RoundedCornerShape(18.dp)){
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Box(Modifier.size(34.dp),contentAlignment=Alignment.Center){icon()}
            Column(Modifier.weight(1f).padding(start=8.dp)){
                Text(title,fontWeight=FontWeight.Bold)
                Text(
                    subtitle,
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight,null)
        }
    }
}

@Composable
private fun InsightCard(label:String,value:String,modifier:Modifier=Modifier){
    Card(modifier,shape=RoundedCornerShape(18.dp)){
        Column(Modifier.padding(13.dp)){
            Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
            Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
