package com.otaviobarreto.pokedex.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class EvolutionFilterLoadResult(
    val displayDex:List<GameDexService.GameDexEntry>,
    val fullDex:List<GameDexService.GameDexEntry>,
    val routes:List<EvolutionRoute>,
    val exclusiveSpeciesIds:Set<Int>,
    val novelFormIdentities:Set<Pair<Int,String>>
)

@Composable
internal fun EvolutionFilterFullScreen(
    gameLabel:String,
    initialRegionSource:String,
    filterKey:String,
    onBack:()->Unit,
    onPokemonClick:(Int,String?)->Unit
){
    BackHandler(onBack=onBack)
    val game=remember(gameLabel){AppGameCatalog.games.first{it.label==gameLabel}}
    var selectedSource by rememberSaveable(gameLabel,filterKey){
        mutableStateOf(game.regions.firstOrNull{it.source==initialRegionSource}?.source ?: game.regions.first().source)
    }
    val selectedRegion=game.regions.first{it.source==selectedSource}
    var dex by remember(selectedSource){mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())}
    var fullDex by remember(selectedSource){mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())}
    var routes by remember(selectedSource){mutableStateOf<List<EvolutionRoute>>(emptyList())}
    var exclusiveSpeciesIds by remember(selectedSource){mutableStateOf<Set<Int>>(emptySet())}
    var novelFormIdentities by remember(selectedSource){mutableStateOf<Set<Pair<Int,String>>>(emptySet())}
    var loading by remember(selectedSource){mutableStateOf(true)}
    var failed by remember(selectedSource){mutableStateOf(false)}

    LaunchedEffect(selectedSource){
        loading=true
        failed=false
        val loaded=runCatching{
            withContext(Dispatchers.IO){
                val selectedIndex=game.regions.indexOfFirst{it.source==selectedSource}.coerceAtLeast(0)
                val requiredRegions=game.regions.take(selectedIndex+1)
                val entriesBySource=requiredRegions.associate{region->
                    val regionContext=requireNotNull(GameContext.fromSource(region.source))
                    val regionDex=GameDexService.cached(regionContext) ?: GameDexService.loadGameDex(regionContext)
                    region.source to regionDex
                }
                val routesBySource=requiredRegions.associate{region->
                    val regionDex=entriesBySource[region.source].orEmpty()
                    region.source to EvolutionFilterIndex.buildRoutes(region.source,regionDex)
                }
                val fullSelectedDex=entriesBySource[selectedSource].orEmpty()
                val selectedRoutes=routesBySource[selectedSource].orEmpty()
                val layer=RegionalDexLayering.layeredResult(
                    game=game,
                    regionSource=selectedSource,
                    entriesBySource=entriesBySource,
                    routesBySource=routesBySource
                )
                val visibleIds=layer.exclusiveSpeciesIds + layer.novelFormTargetIds
                EvolutionFilterLoadResult(
                    displayDex=fullSelectedDex.filter{it.nationalId in visibleIds},
                    fullDex=fullSelectedDex,
                    routes=selectedRoutes,
                    exclusiveSpeciesIds=layer.exclusiveSpeciesIds,
                    novelFormIdentities=layer.novelFormIdentities
                )
            }
        }
        failed=loaded.isFailure
        loaded.getOrNull()?.let{result->
            dex=result.displayDex
            fullDex=result.fullDex
            routes=result.routes
            exclusiveSpeciesIds=result.exclusiveSpeciesIds
            novelFormIdentities=result.novelFormIdentities
        }
        loading=false
    }

    val owned=CollectionStore.capturedForGame(selectedSource)
    val filteredRoutes=remember(routes,filterKey){
        routes.filter{route->
            when(filterKey){
                "ALL" -> EvolutionResolutionEngine.executable(route)
                "TRANSFER" -> route.availability==EvolutionAvailability.TRANSFER_ONLY
                "CONDITION" -> {
                    val legacy=ContextualEvolutionRule(route.sourcePokemonId,route.targetPokemonId,route.methods,route.summary,route.detail)
                    EvolutionRuleCatalog.filterBucket(legacy)=="CONDITION"
                }
                else -> {
                    val legacy=ContextualEvolutionRule(route.sourcePokemonId,route.targetPokemonId,route.methods,route.summary,route.detail)
                    EvolutionRuleCatalog.filterBucket(legacy)==filterKey || route.methods.any{it.name==filterKey}
                }
            }
        }
    }
    val visibleFilteredRoutes=remember(
        filteredRoutes,
        exclusiveSpeciesIds,
        novelFormIdentities
    ){
        filteredRoutes.filter{route->
            route.targetPokemonId in exclusiveSpeciesIds ||
                route.targetFormKey?.let{route.targetPokemonId to it} in novelFormIdentities
        }
    }
    val routesByTarget=remember(visibleFilteredRoutes){
        visibleFilteredRoutes.groupBy{it.targetPokemonId}
    }
    val pending=remember(
        dex,
        routesByTarget,
        owned,
        novelFormIdentities,
        selectedSource,
        VariantCollectionStore.ownedVariants
    ){
        dex.filter{entry->
            val targetRoutes=routesByTarget[entry.nationalId].orEmpty()
            if(targetRoutes.isEmpty()) return@filter false

            val novelFormRoutes=targetRoutes.filter{route->
                route.targetFormKey?.let{route.targetPokemonId to it} in novelFormIdentities
            }
            val hasPendingNovelForm=novelFormRoutes.any{route->
                val formKey=route.targetFormKey ?: return@any false
                !VariantCollectionStore.isFormOwnedForGame(
                    source=selectedSource,
                    speciesId=route.targetPokemonId,
                    formKey=formKey
                )
            }

            hasPendingNovelForm || (
                entry.nationalId in exclusiveSpeciesIds &&
                    entry.nationalId !in owned
            )
        }
    }
    val names=remember(fullDex){fullDex.associate{it.nationalId to it.name}}

    val filterLabel=when(filterKey){
        "ALL" -> "Todos que faltam"
        "LEVEL" -> "Evolução por nível"
        "CONDITION" -> "Evolução por condição"
        "TRANSFER" -> "Transferência"
        else -> PokeApiService.EvolutionMethod.entries.firstOrNull{it.name==filterKey}?.label?.let{"Evolução por "+it} ?: "Evolução"
    }

    Column(Modifier.fillMaxSize().padding(horizontal=12.dp)){
        Row(Modifier.fillMaxWidth().padding(top=8.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}
            Column(Modifier.weight(1f)){
                Text(filterLabel,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
                Text(
                    game.label+" · "+pending.size+" pendente"+if(pending.size==1)"" else "s",
                    style=MaterialTheme.typography.labelMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if(game.regions.size>1){
            ScrollableTabRow(
                selectedTabIndex=game.regions.indexOfFirst{it.source==selectedSource}.coerceAtLeast(0),
                edgePadding=0.dp,
                divider={}
            ){
                game.regions.forEach{region->
                    Tab(selected=region.source==selectedSource,onClick={selectedSource=region.source},text={Text(region.label,maxLines=1)})
                }
            }
        }else{
            Text(
                selectedRegion.label,
                style=MaterialTheme.typography.labelLarge,
                color=MaterialTheme.colorScheme.primary,
                modifier=Modifier.padding(horizontal=8.dp,vertical=8.dp)
            )
        }

        when{
            loading -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
            failed -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                Text("Não foi possível carregar as regras de evolução desta região.")
            }
            filteredRoutes.isEmpty() -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                Text(
                    "Nenhum Pokémon desta Pokédex usa este método no jogo atual.",
                    style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            visibleFilteredRoutes.isEmpty() -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                Text(
                    if(game.regions.first().source==selectedSource)
                        "Nenhum Pokémon desta Pokédex usa este método."
                    else
                        "Nenhum Pokémon novo desta expansão usa este método.",
                    style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            pending.isEmpty() -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                Text(
                    "Você já concluiu todas as pendências deste filtro em "+selectedRegion.label+".",
                    style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding=PaddingValues(vertical=8.dp),
                verticalArrangement=Arrangement.spacedBy(6.dp)
            ){
                items(pending.chunked(3),key={row->row.joinToString("-"){it.nationalId.toString()}}){row->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        repeat(3){index->
                            val entry=row.getOrNull(index)
                            if(entry==null){
                                Spacer(Modifier.weight(1f))
                            }else{
                                val targetRoutes=routesByTarget[entry.nationalId].orEmpty()
                                val route=EvolutionResolutionEngine.preferredRoute(targetRoutes)
                                Surface(
                                    modifier=Modifier.weight(1f).clickable{onPokemonClick(entry.nationalId,selectedSource)},
                                    shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),
                                    color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.62f)
                                ){
                                    Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                        AsyncImage(
                                            model=entry.spriteUrl,
                                            contentDescription=entry.name,
                                            modifier=Modifier.fillMaxWidth().aspectRatio(1f)
                                        )
                                        Text(
                                            entry.name,
                                            style=MaterialTheme.typography.labelLarge,
                                            fontWeight=FontWeight.Black,
                                            maxLines=1,
                                            overflow=TextOverflow.Ellipsis
                                        )
                                        route?.let{resolved->
                                            Text(
                                                (names[resolved.sourcePokemonId] ?: "Pokémon #"+resolved.sourcePokemonId)+" → "+entry.name,
                                                style=MaterialTheme.typography.labelSmall,
                                                color=MaterialTheme.colorScheme.primary,
                                                maxLines=1,
                                                overflow=TextOverflow.Ellipsis
                                            )
                                            resolved.targetFormKey?.takeIf{
                                                resolved.targetPokemonId to it in novelFormIdentities
                                            }?.let{formKey->
                                                Text(
                                                    formKey.replace('-',' ').replaceFirstChar{it.uppercase()},
                                                    style=MaterialTheme.typography.labelSmall,
                                                    color=MaterialTheme.colorScheme.primary,
                                                    maxLines=1,
                                                    overflow=TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                resolved.summary,
                                                style=MaterialTheme.typography.labelSmall,
                                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines=2,
                                                overflow=TextOverflow.Ellipsis
                                            )
                                            if(targetRoutes.size>1){
                                                Text(
                                                    "+"+(targetRoutes.size-1)+" alternativa"+if(targetRoutes.size-1==1)"" else "s",
                                                    style=MaterialTheme.typography.labelSmall,
                                                    color=MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item{Spacer(Modifier.height(10.dp))}
            }
        }
    }
}
