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

private data class DetailV2Bundle(
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

private data class DetailSaveLocation(
    val saved:Boolean,
    val gameLabel:String?,
    val boxLabel:String
)

private fun resolveSaveLocation(
    pokemonId:Int,
    context:GameContext?,
    source:String?,
    legacyBoxes:List<String>
):DetailSaveLocation{
    val registered = if(!source.isNullOrBlank()) CollectionStore.isCapturedIn(source,pokemonId) else CollectionStore.isCaptured(pokemonId)
    if(!registered){
        return DetailSaveLocation(false,null,"Segure na Box")
    }

    if(context!=null){
        val regionalDex=GameDexService.cached(context).orEmpty()
        val index=regionalDex.indexOfFirst{it.nationalId==pokemonId}
        if(index>=0){
            return DetailSaveLocation(
                saved=true,
                gameLabel=context.label,
                boxLabel="Box "+(index/30+1)
            )
        }
    }

    val legacy=legacyBoxes.firstOrNull()
    if(legacy!=null){
        return DetailSaveLocation(
            saved=true,
            gameLabel=gameFromBox(legacy),
            boxLabel=compactBoxName(legacy)
        )
    }

    return DetailSaveLocation(
        saved=true,
        gameLabel=context?.label ?: "Living Dex",
        boxLabel=if(context!=null)"Capturado" else "Living Dex"
    )
}

@Composable private fun HeroCard(b:DetailV2Bundle,context:GameContext?,source:String?,accent:Color,saveLocation:DetailSaveLocation,back:()->Unit){
    val inCollection=saveLocation.saved
    val preferredVariant=remember(b.pokemon.id,source,VariantCollectionStore.ownedVariants){
        if(source.isNullOrBlank()) null else VariantCollectionStore.preferred(source,b.pokemon.id)
    }
    val heroImage=preferredVariant?.artworkUrl ?: b.pokemon.spriteUrl
    val scheme=MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().height(350.dp).background(
            Brush.linearGradient(listOf(accent.copy(alpha=.30f),scheme.primaryContainer.copy(alpha=.55f),scheme.surface))
        )
    ){
        Box(Modifier.size(260.dp).align(Alignment.CenterEnd).offset(x=78.dp,y=(-42).dp).background(accent.copy(alpha=.08f),CircleShape))
        Box(Modifier.size(190.dp).align(Alignment.BottomStart).offset(x=(-75).dp,y=70.dp).background(accent.copy(alpha=.07f),CircleShape))
        Row(Modifier.align(Alignment.TopStart).padding(start=16.dp,top=14.dp),verticalAlignment=Alignment.CenterVertically){
            Surface(shape=CircleShape,color=scheme.surface.copy(alpha=.86f),shadowElevation=4.dp){
                IconButton(back,Modifier.size(46.dp)){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}
            }
            Surface(shape=RoundedCornerShape(999.dp),color=scheme.surface.copy(alpha=.78f),modifier=Modifier.padding(start=8.dp)){
                Text("#${b.pokemon.id.toString().padStart(4,'0')}",fontSize=15.sp,color=scheme.onSurfaceVariant,fontWeight=FontWeight.SemiBold)
            }
        }
        Surface(
            shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),
            color=scheme.surface.copy(alpha=.88f),
            modifier=Modifier.align(Alignment.TopEnd).padding(top=14.dp,end=16.dp).size(56.dp),
            shadowElevation=3.dp
        ){
            Box(contentAlignment=Alignment.Center){
                Icon(Icons.Default.CatchingPokemon,if(inCollection)"Capturado" else "Não capturado",tint=if(inCollection)MaterialTheme.colorScheme.error else scheme.onSurfaceVariant,modifier=Modifier.size(36.dp).alpha(if(inCollection)1f else .34f))
            }
        }
        Column(Modifier.align(Alignment.CenterStart).padding(start=24.dp,top=50.dp).width(220.dp)){
            DexSectionEyebrow(b.species.genus?:"Pokémon")
            Text(b.pokemon.name,style=MaterialTheme.typography.headlineLarge,color=scheme.onSurface,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)
            Row(Modifier.padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){b.pokemon.types.take(2).forEach{TypeBadge(it)}}
            Spacer(Modifier.height(15.dp))
            DetailMetric(Icons.Default.Height,String.format("%.1f",b.pokemon.heightDecimeters/10.0)+" m")
            DetailMetric(Icons.Default.MonitorWeight,String.format("%.1f",b.pokemon.weightHectograms/10.0)+" kg")
            DetailMetric(Icons.Default.LocationOn,context?.regionLabel?:"Nacional")
        }
        Surface(
            modifier=Modifier.align(Alignment.CenterEnd).padding(end=8.dp,top=58.dp).size(width=180.dp,height=226.dp),
            shape=RoundedCornerShape(PokedexDesignTokens.Radius.Xl),
            color=Color.White.copy(alpha=.18f)
        ){
            Box(contentAlignment=Alignment.Center){
                PokemonArtwork(model=heroImage,contentDescription=b.pokemon.name,modifier=Modifier.fillMaxSize().padding(horizontal=8.dp,vertical=10.dp),pokemonId=preferredVariant?.formPokemonId ?: b.pokemon.id)
            }
        }
    }
}
@Composable private fun TypeBadge(type:String){Surface(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm),color=PokedexDesignTokens.Colors.type(type),modifier=Modifier.wrapContentWidth()){Row(Modifier.padding(horizontal=10.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Eco,null,tint=Color.White,modifier=Modifier.size(15.dp));Spacer(Modifier.width(5.dp));Text(type.uppercase(),color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium,maxLines=1)}}}
private fun gameFromBox(box:String):String=when{box.contains("Scarlet / Violet",true)->"Scarlet / Violet";box.contains("Sword / Shield",true)->"Sword / Shield";box.contains("Let's Go",true)->"Let's Go Pikachu / Eevee";box.contains("Arceus",true)->"Legends Arceus";box.contains("HOME",true)->"Pokémon HOME";else->box.substringBefore(" · Box").substringBefore(" Box ").trim()}
private fun compactBoxName(box:String):String=when{box.contains("· Box",true)->box.substringAfter("· ").trim();Regex("Box \\d+",RegexOption.IGNORE_CASE).containsMatchIn(box)->Regex("Box \\d+",RegexOption.IGNORE_CASE).find(box)?.value?:box;else->box}
@Composable private fun DetailMetric(icon:androidx.compose.ui.graphics.vector.ImageVector,text:String){
    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(vertical=4.dp)){
        Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
@Composable
private fun DetailDexNavigator(currentId:Int,openPokemon:(Int)->Unit){
    val previous=(currentId-1).takeIf{it>=1}
    val next=(currentId+1).takeIf{it<=PokeApiService.MAX_NATIONAL_DEX_ID}
    Row(
        Modifier.fillMaxWidth().padding(horizontal=PokedexDesignTokens.Spacing.Lg,vertical=PokedexDesignTokens.Spacing.Sm),
        horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)
    ){
        OutlinedButton(
            onClick={previous?.let(openPokemon)},
            enabled=previous!=null,
            modifier=Modifier.weight(1f)
        ){
            Icon(Icons.AutoMirrored.Filled.ArrowBack,null,Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text(previous?.let{"#"+it.toString().padStart(4,'0')} ?: "Início")
        }
        OutlinedButton(
            onClick={next?.let(openPokemon)},
            enabled=next!=null,
            modifier=Modifier.weight(1f)
        ){
            Text(next?.let{"#"+it.toString().padStart(4,'0')} ?: "Fim")
            Spacer(Modifier.width(5.dp))
            Icon(Icons.Default.ArrowForward,null,Modifier.size(17.dp))
        }
    }
}

@Composable private fun DetailTabs(selected:Int,setSelected:(Int)->Unit,context:GameContext?){
    val haptics=LocalHapticFeedback.current
    val base=listOf(
        "Info" to Icons.Default.Info,
        "Stats" to Icons.Default.BarChart,
        "Evolução" to Icons.Default.AccountTree,
        "Golpes" to Icons.Default.AutoAwesome
    )
    val tabs=base+("Localização" to Icons.Default.LocationOn)
    LaunchedEffect(tabs.size,selected){
        if(selected>=tabs.size) setSelected(0)
    }
    Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=.98f),shadowElevation=PokedexDesignTokens.Elevation.Low){
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            tabs.forEachIndexed{i,(label,icon)->
                val active=i==selected
                val interaction=remember(i){MutableInteractionSource()}
                Surface(
                    Modifier
                        .weight(1f)
                        .dexInteractiveSurface(interactionSource=interaction,pressedScale=.96f)
                        .clickable(interactionSource=interaction,indication=null){
                            if(i!=selected){
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                setSelected(i)
                            }
                        },
                    shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),
                    color=if(active)MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ){
                    Column(Modifier.padding(vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally){
                        Icon(icon,null,tint=if(active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(18.dp))
                        Text(label,style=MaterialTheme.typography.labelSmall,color=if(active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
                    }
                }
            }
        }
    }
}

@Composable private fun InfoTab(b:DetailV2Bundle,accent:Color,context:GameContext?,source:String?,openRef:((String,String)->Unit)?){
    var advisorReady by remember { mutableStateOf(CollectionAdvisor.isWarm()) }
    LaunchedEffect(Unit){
        if(!advisorReady){
            withContext(Dispatchers.IO){runCatching{CollectionAdvisor.warmAllGames()}}
            advisorReady=CollectionAdvisor.isWarm()
        }
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal=PokedexDesignTokens.Spacing.Lg,vertical=PokedexDesignTokens.Spacing.Sm),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        item{
            SectionCard("Informações gerais",Icons.Default.Info){
                Column(verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)){
                    Row(horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)){
                        InfoMini("Taxa de captura",b.species.captureRate.toString(),Modifier.weight(1f))
                        InfoMini("Felicidade base",b.species.baseHappiness.toString(),Modifier.weight(1f))
                        InfoMini("Crescimento",b.species.growthRate?:"—",Modifier.weight(1f))
                    }
                    InfoMini("Grupos de ovo",b.species.eggGroups.joinToString().ifBlank{"—"},Modifier.fillMaxWidth())
                }
            }
        }
        item{TypeMatchupCard(b.pokemon.types)}
        item{PokemonFormsSummaryCard(b.pokemon.id,source,accent)}
        item{
            SectionCard("Habilidades",Icons.Default.Bolt){
                Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
                    b.pokemon.abilities.forEach{ability->
                        Surface(
                            Modifier.fillMaxWidth().then(if(openRef!=null)Modifier.clickable{openRef("ability",ability)}else Modifier),
                            shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm),
                            color=accent.copy(alpha=.08f)
                        ){
                            Row(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Md),verticalAlignment=Alignment.CenterVertically){
                                Text(ability,Modifier.weight(1f),fontWeight=FontWeight.SemiBold)
                                if(openRef!=null)Icon(Icons.Default.ChevronRight,null)
                            }
                        }
                    }
                }
            }
        }
        item{Spacer(Modifier.height(12.dp))}
    }
}
@Composable
private fun PokemonFormsSummaryCard(
    pokemonId:Int,
    source:String?,
    accent:Color
){
    val haptics=LocalHapticFeedback.current
    var forms by remember(pokemonId){ mutableStateOf<List<PokemonFormVariant>?>(PokemonFormsService.cached(pokemonId)) }
    LaunchedEffect(pokemonId){
        if(forms==null){
            forms=runCatching{
                withContext(Dispatchers.IO){PokemonFormsService.collectible(pokemonId)}
            }.getOrDefault(emptyList())
        }
    }
    val available=forms.orEmpty()
    if(available.size<=1 && source.isNullOrBlank()) return

    val owned=if(source.isNullOrBlank()) emptyList() else VariantCollectionStore.variantsFor(source,pokemonId)
    val contextLabel=source?.let{GameContext.fromSource(it)?.label}
        ?: AppStatePreferences.activeGame
    SectionCard("Coleção · Formas e Shiny",Icons.Default.AutoAwesome){
        Text(
            if(source.isNullOrBlank())
                "Abra este Pokémon por uma Box para registrar variantes."
            else
                contextLabel+" · "+owned.size+" variante(s) registrada(s)",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant
        )
        if(available.isEmpty()){
            Text("Carregando formas…",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }else{
            LazyRow(horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)){
                items(available,key={it.formKey}){form->
                    val formId=form.pokemonId ?: pokemonId
                    val normalOwned=owned.any{
                        it.formPokemonId==formId && it.formName.equals(form.name,true) && !it.shiny
                    }
                    val shinyOwned=owned.any{
                        it.formPokemonId==formId && it.formName.equals(form.name,true) && it.shiny
                    }
                    Surface(
                        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),
                        color=accent.copy(alpha=.08f)
                    ){
                        Column(
                            Modifier.width(132.dp).padding(10.dp),
                            horizontalAlignment=Alignment.CenterHorizontally
                        ){
                            PokemonArtwork(
                                model=form.spriteUrl ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+formId+".png",
                                contentDescription=form.name,
                                modifier=Modifier.size(72.dp),
                                pokemonId=formId
                            )
                            Text(
                                form.name,
                                fontSize=10.sp,
                                fontWeight=FontWeight.Bold,
                                maxLines=2,
                                overflow=TextOverflow.Ellipsis
                            )
                            if(!source.isNullOrBlank()){
                                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                    FilterChip(
                                        selected=normalOwned,
                                        onClick={
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            VariantCollectionStore.toggle(
                                                source,pokemonId,formId,form.name,false,
                                                formKey=form.formKey,
                                                normalArtworkUrl=form.spriteUrl,
                                                shinyArtworkUrl=form.shinySpriteUrl,
                                                isDefault=form.isDefault
                                            )
                                        },
                                        label={Text("Normal",style=MaterialTheme.typography.labelSmall)}
                                    )
                                    FilterChip(
                                        selected=shinyOwned,
                                        onClick={
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            VariantCollectionStore.toggle(
                                                source,pokemonId,formId,form.name,true,
                                                formKey=form.formKey,
                                                normalArtworkUrl=form.spriteUrl,
                                                shinyArtworkUrl=form.shinySpriteUrl,
                                                isDefault=form.isDefault
                                            )
                                        },
                                        label={Text("★",style=MaterialTheme.typography.labelSmall)}
                                    )
                                }
                            }else{
                                Text(
                                    listOfNotNull(
                                        if(normalOwned)"Normal" else null,
                                        if(shinyOwned)"★ Shiny" else null
                                    ).joinToString(" · ").ifBlank{"Não registrado"},
                                    fontSize=9.sp,
                                    color=if(normalOwned||shinyOwned)accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines=1
                                )
                            }
                        }
                    }
                }
            }
            if(!source.isNullOrBlank() && CollectionStore.isCapturedIn(source,pokemonId)){
                TextButton(
                    onClick={
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        VariantCollectionStore.removeAll(source,pokemonId)
                    },
                    modifier=Modifier.fillMaxWidth()
                ){
                    Icon(Icons.Default.DeleteOutline,null,Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Remover da Box")
                }
            }
        }
    }
}

@Composable private fun SectionCard(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,content:@Composable ColumnScope.()->Unit){
    DexGlassSurface(Modifier.fillMaxWidth()){
        Row(verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm),color=MaterialTheme.colorScheme.primaryContainer){
                Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(PokedexDesignTokens.Spacing.Sm).size(18.dp))
            }
            Spacer(Modifier.width(9.dp))
            Text(title,style=MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}
@Composable private fun InfoMini(label:String,value:String,modifier:Modifier){Surface(modifier,shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.72f)){Column(Modifier.padding(PokedexDesignTokens.Spacing.Md)){Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis)}}}
@Composable private fun V2Stats(s:PokemonStats){
    val rows=listOf("HP" to s.hp,"Ataque" to s.attack,"Defesa" to s.defense,"Ataque Esp." to s.specialAttack,"Defesa Esp." to s.specialDefense,"Velocidade" to s.speed)
    LazyColumn(Modifier.fillMaxSize().padding(PokedexDesignTokens.Spacing.Lg),verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Md)){
        item{DexSectionEyebrow("Atributos de batalha")}
        items(rows){(name,v)->
            val animated by animateFloatAsState((v/200f).coerceIn(0f,1f),tween(PokedexDesignTokens.Motion.Emphasis),label="stat")
            Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),color=MaterialTheme.colorScheme.surface){
                Column(Modifier.padding(14.dp)){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(name,style=MaterialTheme.typography.bodyMedium)
                        Text(v.toString(),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(progress={animated},modifier=Modifier.fillMaxWidth().padding(top=7.dp).height(7.dp),strokeCap=androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
        }
        item{InfoMini("Total",rows.sumOf{it.second}.toString(),Modifier.fillMaxWidth())}
    }
}
@Composable private fun V2Evolution(
    e:List<PokeApiService.EvolutionStage>,
    routes:List<EvolutionRoute>,
    currentId:Int,
    openPokemon:((Int)->Unit)?
){
    LazyColumn(Modifier.fillMaxSize().padding(PokedexDesignTokens.Spacing.Lg),verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)){
        item{DexSectionEyebrow("Família evolutiva")}
        if(e.isEmpty()) item{Text("Nenhuma evolução encontrada.")}
        else items(e,key={it.pokemonId}){stage->
            val active=stage.pokemonId==currentId
            Card(
                Modifier.fillMaxWidth().then(if(openPokemon!=null&&!active)Modifier.clickable{openPokemon(stage.pokemonId)}else Modifier),
                shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),
                colors=CardDefaults.cardColors(containerColor=if(active)MaterialTheme.colorScheme.primaryContainer.copy(alpha=.55f) else MaterialTheme.colorScheme.surface)
            ){
                Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                    PokemonArtwork(
                        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+stage.pokemonId+".png",
                        stage.name,
                        Modifier.size(70.dp).padding(4.dp),
                        pokemonId=stage.pokemonId
                    )
                    Column(Modifier.weight(1f).padding(start=10.dp)){
                        Text(stage.name,style=MaterialTheme.typography.titleSmall)
                        val targetRoutes=EvolutionResolutionEngine.routesForTarget(routes,stage.pokemonId)
                        val resolved=EvolutionResolutionEngine.preferredRoute(targetRoutes)
                        val summary=resolved?.summary ?: EvolutionRuleCatalog.simplify(stage.requirement)
                        val special=resolved?.availability!=EvolutionAvailability.AVAILABLE &&
                            (resolved!=null || PokeApiService.isSpecialEvolutionRequirement(stage.requirement))
                        Text(
                            summary,
                            style=MaterialTheme.typography.bodySmall,
                            fontWeight=if(special)FontWeight.Bold else FontWeight.Normal,
                            color=if(special)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines=2,
                            overflow=TextOverflow.Ellipsis
                        )
                        if(targetRoutes.size>1){
                            Text(
                                targetRoutes.drop(1).joinToString(" ou "){it.summary},
                                style=MaterialTheme.typography.labelSmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines=2,
                                overflow=TextOverflow.Ellipsis
                            )
                        }
                        if(resolved?.availability==EvolutionAvailability.TRANSFER_ONLY){
                            Text("Somente via transferência",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                        }
                        if(active)Text("Pokémon atual",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                    }
                    if(openPokemon!=null&&!active)Icon(Icons.Default.ChevronRight,null)
                }
            }
        }
    }
}
