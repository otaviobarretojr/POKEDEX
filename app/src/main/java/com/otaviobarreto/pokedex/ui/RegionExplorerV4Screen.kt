package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.CommunityAreaEncounter
import com.otaviobarreto.pokedex.data.CommunityEncounterIndex
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.RegionMapCatalog
import com.otaviobarreto.pokedex.data.RegionMapZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

private enum class V4Filter { ALL, CAPTURED, MISSING }

private data class V4ZonePokemon(
    val pokemon: GameDexService.GameDexEntry,
    val areas: List<CommunityAreaEncounter>
)

@Composable
fun RegionExplorerV4Screen(
    source: String,
    onBack: () -> Unit,
    onPokemonClick: (Int, String) -> Unit
) {
    val context = remember(source) { GameContext.fromSource(source) }
    if (context?.regionLabel == "Paldea") {
        RegionExplorerV3Screen(source, onBack, onPokemonClick)
        return
    }
    if (context?.regionLabel !in setOf("Kitakami", "Blueberry")) {
        RegionExplorerV2Screen(source, onBack, onPokemonClick)
        return
    }
    RecreatedRegionExplorer(source, context!!, onBack, onPokemonClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecreatedRegionExplorer(
    source: String,
    context: GameContext,
    onBack: () -> Unit,
    onPokemonClick: (Int, String) -> Unit
) {
    val zones = remember(context) { RegionMapCatalog.zones(context) }
    var dex by remember(source) { mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList()) }
    var selectedPokemon by remember { mutableStateOf<GameDexService.GameDexEntry?>(null) }
    var encounters by remember { mutableStateOf<List<PokeApiService.EncounterLocation>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(V4Filter.ALL) }
    var loading by remember(source) { mutableStateOf(true) }
    var loadingEncounters by remember { mutableStateOf(false) }
    var error by remember(source) { mutableStateOf<String?>(null) }
    var selectedZone by remember(source) { mutableStateOf<RegionMapZone?>(null) }

    LaunchedEffect(source) {
        runCatching { withContext(Dispatchers.IO) { GameDexService.loadGameDex(context) } }
            .onSuccess { dex = it; loading = false }
            .onFailure { error = "Não foi possível carregar a Pokédex de ${context.regionLabel}."; loading = false }
    }

    LaunchedEffect(selectedPokemon?.nationalId, source) {
        val pokemon = selectedPokemon ?: run { encounters = emptyList(); return@LaunchedEffect }
        loadingEncounters = true
        encounters = runCatching {
            withContext(Dispatchers.IO) { PokeApiService.loadEncounters(pokemon.nationalId) }
        }.getOrElse { emptyList() }.mapNotNull { encounter ->
            val versions = encounter.versions.filter(context::matchesVersion)
            val details = encounter.details.filter { context.matchesVersion(it.version) }
            if (versions.isEmpty() && details.isEmpty()) null else encounter.copy(versions = versions, details = details)
        }
        selectedZone = null
        loadingEncounters = false
    }

    val captured = CollectionStore.capturedIds
    val regionCaptured = dex.count { it.nationalId in captured }
    val results = remember(dex, query, filter, captured) {
        val q = query.trim().removePrefix("#")
        if (q.isBlank()) emptyList() else dex.filter { p ->
            val matches = p.name.contains(q, true) || p.nationalId.toString() == q || p.gameNumber.toString() == q
            matches && when (filter) {
                V4Filter.ALL -> true
                V4Filter.CAPTURED -> p.nationalId in captured
                V4Filter.MISSING -> p.nationalId !in captured
            }
        }.take(8)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${context.regionLabel} · mapa") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { inner ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Box(Modifier.fillMaxSize().padding(inner).padding(24.dp), contentAlignment = Alignment.Center) { Text(error!!) }
            else -> LazyColumn(Modifier.fillMaxSize().padding(inner).background(Color(0xFFF8F8FC)), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            if (context.regionLabel == "Blueberry") "Terarium · Blueberry Academy" else "Kitakami",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF151426)
                        )
                        Text("$regionCaptured de ${dex.size} capturados", color = Color(0xFF72778B), style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Base cartográfica recriada a partir da geografia do jogo. Não é um screenshot nem um asset oficial.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip({ filter = V4Filter.ALL }, { Text("Todos") }, leadingIcon = if (filter == V4Filter.ALL) ({ Text("✓") }) else null)
                            AssistChip({ filter = V4Filter.CAPTURED }, { Text("Capturados") }, leadingIcon = if (filter == V4Filter.CAPTURED) ({ Text("✓") }) else null)
                            AssistChip({ filter = V4Filter.MISSING }, { Text("Faltantes") }, leadingIcon = if (filter == V4Filter.MISSING) ({ Text("✓") }) else null)
                        }
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it; if (selectedPokemon?.name?.equals(it, true) != true) selectedPokemon = null },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            placeholder = { Text("Buscar Pokémon") },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                    }
                }

                if (query.isNotBlank() && selectedPokemon == null) {
                    items(results, key = { it.nationalId }) { pokemon ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { selectedPokemon = pokemon; query = pokemon.name }, shape = RoundedCornerShape(18.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFFF1F0F8))) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model=pokemon.spriteUrl, contentDescription=pokemon.name, contentScale=ContentScale.Fit, modifier=Modifier.size(48.dp).padding(2.dp))
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(pokemon.name, fontWeight = FontWeight.SemiBold)
                                    Text("#${pokemon.gameNumber.toString().padStart(3, '0')} regional · #${pokemon.nationalId.toString().padStart(4, '0')} nacional", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(if (pokemon.nationalId in captured) "✓" else "○")
                            }
                        }
                    }
                }

                selectedPokemon?.let { pokemon ->
                    item {
                        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFFF1F0F8))) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model=pokemon.spriteUrl, contentDescription=pokemon.name, contentScale=ContentScale.Fit, modifier=Modifier.size(64.dp).padding(3.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(pokemon.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(if (pokemon.nationalId in captured) "Capturado" else "Faltando")
                                    Text(if (loadingEncounters) "Carregando encontros…" else if (encounters.isEmpty()) "Sem spawn detalhado na PokéAPI" else "${encounters.size} áreas registradas", style = MaterialTheme.typography.bodySmall)
                                }
                                FilledTonalButton(onClick = { onPokemonClick(pokemon.nationalId, source) }) { Text("Ficha") }
                            }
                        }
                    }
                }

                item {
                    RecreatedGameMap(
                        regionLabel = context.regionLabel,
                        zones = zones,
                        encounters = encounters,
                        onZoneClick = { selectedZone = it }
                    )
                }
                item {
                    Text(
                        "Geografia reconstruída visualmente com referências do mapa do jogo; coordenadas dos marcadores são aproximadas e podem ser refinadas.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                item { Spacer(Modifier.size(14.dp)) }
            }
        }
    }

    selectedZone?.let { zone ->
        ModalBottomSheet(onDismissRequest = { selectedZone = null }) {
            val matching = encounters.filter { zone.matches(it.location) }
            val areas = CommunityEncounterIndex.entriesForZone(context.regionLabel, zone)
            val zonePokemon = resolveV4ZonePokemon(dex, areas).filter { item ->
                when (filter) {
                    V4Filter.ALL -> true
                    V4Filter.CAPTURED -> item.pokemon.nationalId in captured
                    V4Filter.MISSING -> item.pokemon.nationalId !in captured
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null)
                    Text(zone.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.size(8.dp))
                if (selectedPokemon == null) {
                    Text("Pokémon da área", fontWeight = FontWeight.SemiBold)
                    if (zonePokemon.isEmpty()) Text("Ainda não há espécies indexadas para esta zona.")
                    else LazyColumn(Modifier.fillMaxWidth().heightIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(zonePokemon, key = { it.pokemon.nationalId }) { item ->
                            Card(Modifier.fillMaxWidth().clickable { selectedZone = null; onPokemonClick(item.pokemon.nationalId, source) }) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(item.pokemon.spriteUrl, item.pokemon.name, Modifier.size(50.dp))
                                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                        Text(item.pokemon.name, fontWeight = FontWeight.SemiBold)
                                        Text(item.areas.joinToString(" • ") { a -> a.area + (a.levels?.let { " · Nv. $it" } ?: "") }, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(if (item.pokemon.nationalId in captured) "✓" else "○")
                                }
                            }
                        }
                    }
                } else if (matching.isEmpty()) {
                    Text("Nenhum encontro detalhado deste Pokémon foi associado a esta zona pela PokéAPI.")
                    if (zonePokemon.any { it.pokemon.nationalId == selectedPokemon?.nationalId }) {
                        Text("O índice comunitário registra esta espécie nesta zona.", modifier = Modifier.padding(top = 8.dp))
                    }
                } else {
                    Text("Encontros detalhados · PokéAPI", fontWeight = FontWeight.SemiBold)
                    matching.forEach { encounter ->
                        Text(encounter.location, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        encounter.details.take(4).forEach { detail ->
                            val level = when {
                                detail.minLevel > 0 && detail.maxLevel > 0 && detail.minLevel != detail.maxLevel -> "Nv. ${detail.minLevel}–${detail.maxLevel}"
                                detail.minLevel > 0 -> "Nv. ${detail.minLevel}"
                                else -> null
                            }
                            Text(listOfNotNull(detail.method, level).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecreatedGameMap(
    regionLabel: String,
    zones: List<RegionMapZone>,
    encounters: List<PokeApiService.EncounterLocation>,
    onZoneClick: (RegionMapZone) -> Unit
) {
    var scale by remember(regionLabel) { mutableFloatStateOf(1f) }
    var translation by remember(regionLabel) { mutableStateOf(Offset.Zero) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = { scale = (scale - .25f).coerceAtLeast(1f) }) { Text("−") }
            FilledTonalButton(onClick = { scale = (scale + .25f).coerceAtMost(4f) }) { Text("+") }
            FilledTonalButton(onClick = { scale = 1f; translation = Offset.Zero }) { Text("Centralizar") }
            Text("${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.size(8.dp))
        Surface(Modifier.fillMaxWidth().aspectRatio(1f), shape = RoundedCornerShape(28.dp), color = Color(0xFFF1F0F8), tonalElevation = 0.dp, shadowElevation = 2.dp) {
            Box(Modifier.fillMaxSize().pointerInput(regionLabel) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    translation += pan
                }
            }) {
                BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = translation.x
                    translationY = translation.y
                }) {
                    val primary = MaterialTheme.colorScheme.primary
                    val secondary = MaterialTheme.colorScheme.secondary
                    val tertiaryTone = MaterialTheme.colorScheme.tertiaryContainer
                    val surface = MaterialTheme.colorScheme.surfaceContainer
                    val outline = MaterialTheme.colorScheme.outlineVariant
                    Canvas(Modifier.fillMaxSize()) {
                        if (regionLabel == "Kitakami") {
                            drawKitakamiMap(size.width, size.height, surface, primary, secondary, tertiaryTone, outline)
                        } else {
                            drawTerariumMap(size.width, size.height, surface, primary, secondary, tertiaryTone, outline)
                        }
                    }
                    val mapWidth = maxWidth
                    val mapHeight = maxHeight
                    zones.forEach { zone ->
                        val highlighted = encounters.any { zone.matches(it.location) }
                        val marker = if (highlighted) 34.dp else 28.dp
                        val x = mapWidth * zone.x - marker / 2
                        val y = mapHeight * zone.y - marker / 2
                        Surface(
                            modifier = Modifier.offset(x, y).size(marker).clickable { onZoneClick(zone) },
                            shape = CircleShape,
                            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            shadowElevation = 6.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (highlighted) Icons.Default.CatchingPokemon else Icons.Default.LocationOn,
                                    contentDescription = zone.label,
                                    modifier = Modifier.size(if (highlighted) 18.dp else 14.dp),
                                    tint = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
        Text("Pinça para zoom · arraste para explorar · toque nos marcadores.", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = Color(0xFF72778B))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawKitakamiMap(
    w: Float,
    h: Float,
    land: androidx.compose.ui.graphics.Color,
    mountain: androidx.compose.ui.graphics.Color,
    water: androidx.compose.ui.graphics.Color,
    biome: androidx.compose.ui.graphics.Color,
    outline: androidx.compose.ui.graphics.Color
) {
    drawRect(land)

    val barrens = Path().apply {
        moveTo(0f, 0f); lineTo(w * .46f, 0f); lineTo(w * .39f, h * .31f); lineTo(w * .17f, h * .39f); lineTo(0f, h * .31f); close()
    }
    drawPath(barrens, mountain.copy(alpha = .22f))

    val woods = Path().apply {
        moveTo(w * .62f, 0f); lineTo(w, 0f); lineTo(w, h * .43f); lineTo(w * .76f, h * .38f); lineTo(w * .62f, h * .18f); close()
    }
    drawPath(woods, biome.copy(alpha = .24f))

    val river = Path().apply {
        moveTo(w * .56f, 0f)
        cubicTo(w * .61f, h * .18f, w * .58f, h * .30f, w * .64f, h * .43f)
        cubicTo(w * .72f, h * .57f, w * .68f, h * .70f, w * .77f, h)
    }
    drawPath(river, water.copy(alpha = .55f), style = Stroke(width = w * .025f))
    drawCircle(water.copy(alpha = .45f), radius = w * .055f, center = Offset(w * .58f, h * .12f))
    drawCircle(water.copy(alpha = .45f), radius = w * .055f, center = Offset(w * .83f, h * .48f))

    drawCircle(mountain.copy(alpha = .18f), radius = w * .31f, center = Offset(w * .50f, h * .47f))
    drawCircle(land, radius = w * .18f, center = Offset(w * .50f, h * .47f))
    drawCircle(mountain.copy(alpha = .32f), radius = w * .09f, center = Offset(w * .50f, h * .47f))
    drawCircle(water.copy(alpha = .60f), radius = w * .026f, center = Offset(w * .50f, h * .47f))

    val southRoad = Path().apply {
        moveTo(w * .24f, h)
        cubicTo(w * .30f, h * .80f, w * .45f, h * .79f, w * .50f, h * .73f)
        cubicTo(w * .58f, h * .69f, w * .61f, h * .78f, w * .72f, h)
    }
    drawPath(southRoad, outline, style = Stroke(width = w * .012f))
    drawLine(outline, Offset(w * .39f, h * .76f), Offset(w * .61f, h * .76f), strokeWidth = w * .009f)
    drawCircle(outline.copy(alpha = .65f), radius = w * .018f, center = Offset(w * .50f, h * .79f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTerariumMap(
    w: Float,
    h: Float,
    land: androidx.compose.ui.graphics.Color,
    polar: androidx.compose.ui.graphics.Color,
    canyon: androidx.compose.ui.graphics.Color,
    coastal: androidx.compose.ui.graphics.Color,
    outline: androidx.compose.ui.graphics.Color
) {
    drawRect(land)
    val margin = w * .035f
    val center = Offset(w * .50f, h * .50f)

    val polarPath = Path().apply {
        moveTo(margin, margin); lineTo(w - margin, margin); lineTo(w * .62f, h * .43f); lineTo(w * .38f, h * .43f); close()
    }
    val canyonPath = Path().apply {
        moveTo(margin, margin); lineTo(w * .38f, h * .43f); lineTo(w * .38f, h * .58f); lineTo(margin, h - margin); close()
    }
    val coastalPath = Path().apply {
        moveTo(w - margin, margin); lineTo(w * .62f, h * .43f); lineTo(w * .62f, h * .58f); lineTo(w - margin, h - margin); close()
    }
    val savannaPath = Path().apply {
        moveTo(margin, h - margin); lineTo(w * .38f, h * .58f); lineTo(w * .62f, h * .58f); lineTo(w - margin, h - margin); close()
    }

    drawPath(polarPath, polar.copy(alpha = .24f))
    drawPath(canyonPath, canyon.copy(alpha = .23f))
    drawPath(coastalPath, coastal.copy(alpha = .24f))
    drawPath(savannaPath, polar.copy(alpha = .12f))

    drawLine(outline, Offset(w * .50f, margin), Offset(w * .50f, h - margin), strokeWidth = w * .006f)
    drawLine(outline, Offset(margin, h * .50f), Offset(w - margin, h * .50f), strokeWidth = w * .006f)

    drawCircle(land, radius = w * .105f, center = center)
    drawCircle(outline.copy(alpha = .30f), radius = w * .105f, center = center, style = Stroke(width = w * .012f))
    drawCircle(coastal.copy(alpha = .55f), radius = w * .035f, center = center)

    drawCircle(coastal.copy(alpha = .35f), radius = w * .055f, center = Offset(w * .79f, h * .47f))
    drawCircle(coastal.copy(alpha = .35f), radius = w * .035f, center = Offset(w * .73f, h * .60f))
    drawCircle(coastal.copy(alpha = .35f), radius = w * .028f, center = Offset(w * .83f, h * .63f))

    drawLine(outline.copy(alpha = .5f), Offset(w * .50f, h * .39f), Offset(w * .50f, h * .61f), strokeWidth = w * .012f)
    drawLine(outline.copy(alpha = .5f), Offset(w * .39f, h * .50f), Offset(w * .61f, h * .50f), strokeWidth = w * .012f)
}

private fun resolveV4ZonePokemon(
    dex: List<GameDexService.GameDexEntry>,
    areas: List<CommunityAreaEncounter>
): List<V4ZonePokemon> {
    val byName = dex.associateBy { normalizeV4PokemonName(it.name) }
    val grouped = linkedMapOf<Int, Pair<GameDexService.GameDexEntry, MutableList<CommunityAreaEncounter>>>()
    areas.forEach { area ->
        area.pokemonNames.forEach inner@{ rawName ->
            val pokemon = byName[normalizeV4PokemonName(rawName)] ?: return@inner
            grouped.getOrPut(pokemon.nationalId) { pokemon to mutableListOf() }.second += area
        }
    }
    return grouped.values.map { (pokemon, pokemonAreas) -> V4ZonePokemon(pokemon, pokemonAreas) }.sortedBy { it.pokemon.gameNumber }
}

private fun normalizeV4PokemonName(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .replace("’", "'")
    .lowercase()
    .replace("paldean", "")
    .replace(Regex("\\([^)]*\\)"), "")
    .replace(Regex("[^a-z0-9]+"), "")
