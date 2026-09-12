package com.otaviobarreto.pokedex.ui

import android.util.Base64

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun JourneyGamePicker(onSelect:(String)->Unit){
    val captured=CollectionStore.contextualCapturedIds
    val dexIdsByGame by rememberJourneyDexIdsByGame()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        item{
            Text("JORNADA",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
            Text("Escolha um jogo para abrir sua central de rota, time e guias.",style=MaterialTheme.typography.bodyMedium)
        }
        items(AppGameCatalog.adventureGames,key={it.label}){game->
            val progress = rememberJourneyCollectionProgress(
                game = game,
                capturedBySource = captured,
                ids = dexIdsByGame[game.label].orEmpty()
            )
            JourneyGameReferenceCard(
                game = game,
                progress = progress,
                onClick = { onSelect(game.label) }
            )
        }
        item{Spacer(Modifier.height(20.dp))}
    }
}


@Composable
private fun JourneyGameReferenceCard(
    game: AppGame,
    progress: JourneyCollectionProgress,
    onClick: () -> Unit
) {
    val heroIds = JourneyGameVisualCatalog.forGame(game.label).heroPokemonIds
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        val cardHeight = if (compact) PokedexDesignTokens.Journey.CardHeightCompact else PokedexDesignTokens.Journey.CardHeight
        val coverWidth = if (compact) PokedexDesignTokens.Journey.CoverWidthCompact else PokedexDesignTokens.Journey.CoverWidth
        val heroWidth = if (compact) PokedexDesignTokens.Journey.HeroWidthCompact else PokedexDesignTokens.Journey.HeroWidth
        val fadeWidth = if (compact) PokedexDesignTokens.Journey.FadeWidthCompact else PokedexDesignTokens.Journey.FadeWidth
        val maxRegionChips = if (compact) 2 else 3

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(PokedexDesignTokens.Journey.CardRadius),
            colors = CardDefaults.cardColors(containerColor = PokedexDesignTokens.Journey.CardSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = PokedexDesignTokens.Elevation.Low)
        ) {
            Box(Modifier.fillMaxSize()) {
            JourneyHeroArtwork(
                ids = heroIds,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(heroWidth)
            )

            Box(
                Modifier
                    .fillMaxHeight()
                    .width(fadeWidth)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                PokedexDesignTokens.Journey.CardSurface,
                                PokedexDesignTokens.Journey.CardSurface.copy(alpha = .74f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = PokedexDesignTokens.Journey.CardHorizontalPadding,
                        vertical = PokedexDesignTokens.Journey.CardVerticalPadding
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                JourneyGameCover(
                    gameLabel = game.label,
                    modifier = Modifier
                        .width(coverWidth)
                        .fillMaxHeight()
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, end = 10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = game.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = game.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { progress.ratio },
                            modifier = Modifier
                                .weight(1f)
                                .height(7.dp),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = progress.captured.toString() + "/" + progress.total,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .84f)
                        ) {
                            Text(
                                text = (progress.ratio * 100).toInt().toString() + "%",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (game.regions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            game.regions.take(maxRegionChips).forEach { region ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .92f)
                                ) {
                                    Text(
                                        text = compactJourneyRegionLabel(region.label),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }
    }
    }
}



private object JourneyLocalArtworkCache {
    @Volatile
    private var scarletBytes: ByteArray? = null

    fun scarlet(context: android.content.Context): ByteArray? {
        scarletBytes?.let { return it }
        return synchronized(this) {
            scarletBytes ?: runCatching {
                val encoded = (1..3).joinToString(separator = "") { part ->
                    context.assets.open("journey/scarlet_user_art_" + part + ".b64")
                        .bufferedReader()
                        .use { it.readText() }
                }
                Base64.decode(encoded, Base64.DEFAULT).also { scarletBytes = it }
            }.getOrNull()
        }
    }
}

private fun compactJourneyRegionLabel(label: String): String = when (label) {
    "Isle of Armor" -> "ARMOR"
    "Crown Tundra" -> "TUNDRA"
    "Hyperspace" -> "HYPERSPACE"
    else -> label.uppercase()
}

@Composable
private fun JourneyHeroArtwork(
    ids: List<Int>,
    modifier: Modifier = Modifier
) {
    if (ids.isEmpty()) return
    Box(modifier) {
        ids.take(2).forEachIndexed { index, id ->
            AsyncImage(
                model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" + id + ".png",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (ids.size > 1) .72f else 1f)
                    .align(if (index == 0) Alignment.CenterEnd else Alignment.CenterStart)
                    .alpha(if (ids.size > 1) .34f else .28f),
                contentScale = ContentScale.Fit
            )
        }
    }
}


@Composable
private fun JourneyGameCover(
    gameLabel: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userScarletArtwork = remember(gameLabel, context.applicationContext) {
        if (gameLabel == "Scarlet / Violet") {
            JourneyLocalArtworkCache.scarlet(context.applicationContext)
        } else null
    }
    val covers = GameCoverCatalog.coversFor(gameLabel)
    if (covers.isEmpty()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(PokedexDesignTokens.Journey.ArtworkRadius))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SportsEsports, contentDescription = null)
        }
        return
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PokedexDesignTokens.Journey.ArtworkRadius))
            .background(PokedexDesignTokens.Journey.ArtworkBackdrop)
    ) {
        if (userScarletArtwork != null) {
            AsyncImage(
                model = userScarletArtwork,
                contentDescription = "Arte enviada pelo usuário para Scarlet / Violet",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Row(Modifier.fillMaxSize()) {
                covers.take(2).forEach { cover ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        AsyncImage(
                            model = cover,
                            contentDescription = "Arte oficial de $gameLabel",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = .04f),
                            Color.Black.copy(alpha = .16f)
                        )
                    )
                )
        )
    }
}

@Composable
internal fun JourneyGameMenu(
    game:AppGame,
    onBack:()->Unit,
    onRoute:()->Unit,
    onMap:()->Unit,
    onTeam:()->Unit,
    onBoxes:()->Unit,
    onRegion:(String)->Unit
){
    val route=JourneyCatalog.steps(game.label)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        item{
            Row(verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Voltar")}
                Column{
                    Text(game.label,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall)
                    Text("Central da Jornada",style=MaterialTheme.typography.labelMedium)
                }
            }
        }
        item{
            JourneyActionCard(
                icon=Icons.Default.Route,
                title="Melhor rota",
                subtitle=if(route.isNotEmpty()) "Sequência recomendada por nível, com progresso salvo." else "Estrutura pronta; rota detalhada deste jogo entra na próxima curadoria.",
                enabled=route.isNotEmpty(),
                onClick=onRoute
            )
        }
        item{
            JourneyActionCard(
                icon=Icons.Default.Map,
                title="Mapa da Jornada",
                subtitle=if(JourneyMapCatalog.points(game.label).isNotEmpty()) "Veja concluídos, objetivo atual e próximos desafios distribuídos no mapa." else "Mapa desta campanha ainda não está disponível.",
                enabled=JourneyMapCatalog.points(game.label).isNotEmpty(),
                onClick=onMap
            )
        }
        item{
            JourneyActionCard(
                icon=Icons.Default.Groups,
                title="Time ideal",
                subtitle="Escolha o inicial e a fase da história. Veja trocas, golpes, item e função de cada Pokémon.",
                onClick=onTeam
            )
        }
        item{
            val captured=CollectionStore.contextualCapturedIds
            val boxProgress by rememberJourneyCollectionProgress(game,captured)
            Card(
                Modifier.fillMaxWidth().clickable(onClick=onBoxes),
                shape=RoundedCornerShape(22.dp),
                colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)
            ){
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                    Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.72f)){
                        Icon(Icons.Default.GridView,null,Modifier.padding(13.dp))
                    }
                    Column(Modifier.weight(1f).padding(horizontal=12.dp)){
                        Text("Boxes do jogo",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                        if(boxProgress.total>0){
                            Text(
                                boxProgress.captured.toString()+" de "+boxProgress.total+" Pokémon · "+(boxProgress.ratio*100).toInt()+"%",
                                style=MaterialTheme.typography.bodySmall
                            )
                            LinearProgressIndicator(
                                progress={boxProgress.ratio},
                                modifier=Modifier.fillMaxWidth().padding(top=8.dp).height(7.dp),
                                strokeCap=androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }else{
                            Text("Abra a coleção principal deste jogo.",style=MaterialTheme.typography.bodySmall)
                        }
                    }
                    Icon(Icons.Default.ChevronRight,null)
                }
            }
        }
        if(game.regions.isNotEmpty()){
            item{Text("Regiões e conteúdos",fontWeight=FontWeight.Bold)}
            items(game.regions,key={it.source}){region->
                Card(Modifier.fillMaxWidth().clickable{onRegion(region.source)},shape=RoundedCornerShape(18.dp)){
                    Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.Map,null)
                        Column(Modifier.weight(1f).padding(start=10.dp)){
                            Text(region.label,fontWeight=FontWeight.SemiBold)
                            Text(region.subtitle,style=MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ChevronRight,null)
                    }
                }
            }
        }
        item{Spacer(Modifier.height(20.dp))}
    }
}


private data class JourneyCollectionProgress(val captured:Int=0,val total:Int=0){
    val ratio:Float get()=if(total<=0)0f else captured.toFloat()/total
}

@Composable
private fun rememberJourneyDexIdsByGame(): State<Map<String, Set<Int>>> =
    produceState(initialValue = emptyMap()) {
        value = withContext(Dispatchers.IO) {
            AppGameCatalog.adventureGames.associate { game ->
                game.label to game.regions.flatMap { region ->
                    val ctx = GameContext.fromSource(region.source)
                    if (ctx == null) emptyList()
                    else runCatching { GameDexService.loadGameDex(ctx).map { it.nationalId } }
                        .getOrDefault(emptyList())
                }.toSet()
            }
        }
    }

@Composable
private fun rememberJourneyCollectionProgress(
    game: AppGame,
    capturedBySource: Map<String, Set<Int>>,
    ids: Set<Int>
): JourneyCollectionProgress {
    val registered = remember(game.label, capturedBySource) {
        game.regions.flatMap { capturedBySource[it.source].orEmpty() }.toSet()
    }
    return remember(ids, registered) {
        JourneyCollectionProgress(ids.count { it in registered }, ids.size)
    }
}

@Composable
private fun rememberJourneyCollectionProgress(
    game:AppGame,
    capturedBySource:Map<String,Set<Int>>
):State<JourneyCollectionProgress>{
    val ids by produceState<Set<Int>>(initialValue=emptySet(),game.label){
        value=withContext(Dispatchers.IO){
            game.regions.flatMap{region->
                val ctx=GameContext.fromSource(region.source)
                if(ctx==null) emptyList() else runCatching{GameDexService.loadGameDex(ctx).map{it.nationalId}}.getOrDefault(emptyList())
            }.toSet()
        }
    }
    val registered=remember(game.label,capturedBySource){
        game.regions.flatMap{capturedBySource[it.source].orEmpty()}.toSet()
    }
    return remember(ids,registered){
        mutableStateOf(JourneyCollectionProgress(ids.count{it in registered},ids.size))
    }
}

@Composable
private fun JourneyActionCard(
    icon:androidx.compose.ui.graphics.vector.ImageVector,
    title:String,
    subtitle:String,
    enabled:Boolean=true,
    onClick:()->Unit
){
    Card(
        Modifier.fillMaxWidth().clickable(enabled=enabled,onClick=onClick),
        shape=RoundedCornerShape(22.dp),
        colors=CardDefaults.cardColors(containerColor=if(enabled)MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceVariant)
    ){
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.primaryContainer){
                Icon(icon,null,Modifier.padding(13.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal=12.dp)){
                Text(title,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                Text(subtitle,style=MaterialTheme.typography.bodySmall)
            }
            if(enabled)Icon(Icons.Default.ChevronRight,null)
        }
    }
}
