package com.otaviobarreto.pokedex.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

internal data class DetailV2Bundle(
    val pokemon:PokeApiService.RemotePokemonDetail,
    val species:PokeApiService.SpeciesInfo,
    val evolutions:List<PokeApiService.EvolutionStage>,
    val evolutionRoutes:List<EvolutionRoute>,
    val encounters:List<PokeApiService.EncounterLocation>
)

private fun cachedDetailBundle(id:Int):DetailV2Bundle?{
    val p=PokedexDataStore.cachedPokemon(id)?:return null
    val s=PokedexDataStore.cachedSpecies(id)?:return null
    val e=s.evolutionChainUrl?.let{PokedexDataStore.cachedEvolutions(it)}?:emptyList()
    val l=PokedexDataStore.cachedEncounters(id).orEmpty()
    return DetailV2Bundle(p,s,e,emptyList(),l)
}

@Composable
fun PokemonDetailV2Screen(
    id:Int,
    source:String?=null,
    onBack:()->Unit,
    onOpenReference:((String,String)->Unit)?=null,
    onOpenPokemon:((Int)->Unit)?=null
){
    var bundle by remember(id){ mutableStateOf(cachedDetailBundle(id)) }
    var error by remember(id){ mutableStateOf<String?>(null) }
    var retry by remember{ mutableIntStateOf(0) }
    var tab by rememberSaveable(id){ mutableIntStateOf(0) }
    LaunchedEffect(source){ tab=0 }
    val context=remember(source){ GameContext.fromSource(source) }
    val collectionSource=remember(source,AppStatePreferences.activeGame){
        source ?: AppStatePreferences.activeRegionForGame(AppStatePreferences.activeGame)
    }
    LaunchedEffect(id){
        withContext(Dispatchers.IO){
            runCatching { PokedexDataStore.prefetchDetailWindow(id,radius=2) }
        }
    }

    LaunchedEffect(id,retry){
        error=null

        val core = runCatching {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val pJob=async { PokedexDataStore.pokemon(id) }
                    val sJob=async { PokedexDataStore.species(id) }
                    pJob.await() to sJob.await()
                }
            }
        }.getOrElse {
            if(bundle==null) error="Não foi possível carregar os dados deste Pokémon."
            return@LaunchedEffect
        }

        val (pokemon,species)=core
        // Render as soon as the two core payloads are ready. Evolution and encounter
        // data are secondary and must never block opening the detail screen.
        bundle=DetailV2Bundle(
            pokemon,
            species,
            PokedexDataStore.cachedEvolutions(species.evolutionChainUrl,context).orEmpty(),
            emptyList(),
            PokedexDataStore.cachedEncounters(id).orEmpty()
        )

        runCatching {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val eJob=async {
                        species.evolutionChainUrl?.let { chainUrl->
                            PokedexDataStore.evolutions(chainUrl,context) to EvolutionResolutionEngine.load(chainUrl,context)
                        } ?: (emptyList<PokeApiService.EvolutionStage>() to emptyList<EvolutionRoute>())
                    }
                    val lJob=async { PokedexDataStore.encounters(id) }
                    eJob.await() to lJob.await()
                }
            }
        }.onSuccess { (evolutionBundle,encounters) ->
            bundle=DetailV2Bundle(pokemon,species,evolutionBundle.first,evolutionBundle.second,encounters)
        }
    }

    when{
        bundle!=null -> DetailV2Content(
            bundle!!,tab,{tab=it},context,source,collectionSource,onBack,onOpenReference,onOpenPokemon
        )
        error!=null -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                Text(error!!)
                Button({retry++},Modifier.padding(top=12.dp)){Text("Tentar novamente")}
            }
        }
        else -> InstantDetailShell(id,onBack)
    }
}

@Composable private fun InstantDetailShell(id:Int,onBack:()->Unit){
    val entry=remember(id){PokedexDataStore.cachedIndexEntry(id)}
    val name=entry?.name ?: "Pokémon #$id"
    val image=entry?.spriteUrl ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
    val scheme=MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(scheme.background)){
        Box(Modifier.fillMaxWidth().height(338.dp).background(Brush.linearGradient(listOf(scheme.primaryContainer,scheme.surfaceVariant,scheme.surface)))){
            IconButton(onBack,Modifier.padding(PokedexDesignTokens.Spacing.Lg).size(46.dp).background(scheme.surface.copy(alpha=.86f),CircleShape)){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}
            Column(Modifier.align(Alignment.CenterStart).padding(start=28.dp,top=36.dp).width(185.dp)){
                Text("#${id.toString().padStart(4,'0')}",fontSize=15.sp,color=scheme.onSurfaceVariant,fontWeight=FontWeight.SemiBold)
                Text(name,fontSize=34.sp,lineHeight=36.sp,fontWeight=FontWeight.Black,color=scheme.onSurface,maxLines=2)
                Text("Abrindo ficha…",fontSize=14.sp,color=scheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp))
            }
            PokemonArtwork(model=image,contentDescription=name,modifier=Modifier.align(Alignment.CenterEnd).padding(end=20.dp,top=50.dp).size(width=190.dp,height=220.dp).padding(10.dp),pokemonId=id)
        }
        LinearProgressIndicator(modifier=Modifier.fillMaxWidth(),color=scheme.primary,trackColor=scheme.primaryContainer)
        Text("Dados complementares são carregados em segundo plano sem bloquear a navegação.",modifier=Modifier.padding(PokedexDesignTokens.Spacing.Lg),style=MaterialTheme.typography.bodyMedium,color=scheme.onSurfaceVariant)
    }
}

@Composable private fun DetailV2Content(
    b:DetailV2Bundle,
    tab:Int,
    setTab:(Int)->Unit,
    context:GameContext?,
    source:String?,
    collectionSource:String?,
    back:()->Unit,
    openRef:((String,String)->Unit)?,
    openPokemon:((Int)->Unit)?
){
    val primaryType=b.pokemon.types.firstOrNull().orEmpty()
    val accent=PokedexDesignTokens.Colors.type(primaryType)
    val legacyBoxes=CollectionStore.boxesForPokemon(b.pokemon.id)
    val saveLocation=remember(b.pokemon.id,context,source,CollectionStore.capturedIds,CollectionStore.contextualCapturedIds,legacyBoxes){
        resolveSaveLocation(b.pokemon.id,context,source,legacyBoxes)
    }
    DexAppBackground {
      Column(Modifier.fillMaxSize()){
        HeroCard(b,context,collectionSource,accent,saveLocation,back)
        DetailTabs(tab,setTab,context)
        if(openPokemon!=null){
            DetailDexNavigator(b.pokemon.id,openPokemon)
        }
        key(tab){
            when(tab){
                0->InfoTab(b,accent,context,collectionSource,openRef)
                1->V2Stats(b.pokemon.stats)
                2->V2Evolution(b.evolutions,b.evolutionRoutes,b.pokemon.id,openPokemon)
                3->PokemonMovesTab(b.pokemon.moves,context,openRef)
                else->V2Locations(b.pokemon.id,b.encounters,context,source)
            }
        }
      }
    }
}
