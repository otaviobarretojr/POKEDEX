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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.RegionMapCatalog
import com.otaviobarreto.pokedex.data.RegionMapZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class MapCaptureFilter { ALL, CAPTURED, MISSING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionExplorerV2Screen(
    source: String,
    onBack: () -> Unit,
    onPokemonClick: (Int, String) -> Unit
) {
    val context = remember(source) { GameContext.fromSource(source) }
    val zones = remember(context) { RegionMapCatalog.zones(context) }
    var dex by remember(source) { mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList()) }
    var selectedPokemon by remember { mutableStateOf<GameDexService.GameDexEntry?>(null) }
    var encounters by remember { mutableStateOf<List<PokeApiService.EncounterLocation>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(MapCaptureFilter.ALL) }
    var loading by remember(source) { mutableStateOf(true) }
    var loadingEncounters by remember { mutableStateOf(false) }
    var error by remember(source) { mutableStateOf<String?>(null) }
    var selectedZone by remember(source) { mutableStateOf<RegionMapZone?>(null) }

    LaunchedEffect(source) {
        val game = context
        if (game == null) {
            error = "Este mapa não possui contexto reconhecido."
            loading = false
        } else {
            runCatching { withContext(Dispatchers.IO) { GameDexService.loadGameDex(game) } }
                .onSuccess { dex = it; loading = false }
                .onFailure { error = "Não foi possível carregar esta Pokédex regional."; loading = false }
        }
    }

    LaunchedEffect(selectedPokemon?.nationalId, source) {
        val pokemon = selectedPokemon ?: run { encounters = emptyList(); return@LaunchedEffect }
        loadingEncounters = true
        encounters = runCatching {
            withContext(Dispatchers.IO) { PokeApiService.loadEncounters(pokemon.nationalId) }
        }.getOrElse { emptyList() }.mapNotNull { encounter ->
            val game = context ?: return@mapNotNull encounter
            val versions = encounter.versions.filter(game::matchesVersion)
            val details = encounter.details.filter { game.matchesVersion(it.version) }
            if (versions.isEmpty() && details.isEmpty()) null else encounter.copy(versions = versions, details = details)
        }
        selectedZone = null
        loadingEncounters = false
    }

    val captured = CollectionStore.capturedIds
    val regionCaptured = dex.count { it.nationalId in captured }
    val results = remember(dex, query, filter, captured) {
        val q = query.trim().removePrefix("#")
        if (q.isBlank()) emptyList() else dex.filter { pokemon ->
            val matches = pokemon.name.contains(q, true) || pokemon.nationalId.toString() == q || pokemon.gameNumber.toString() == q
            val status = when (filter) {
                MapCaptureFilter.ALL -> true
                MapCaptureFilter.CAPTURED -> pokemon.nationalId in captured
                MapCaptureFilter.MISSING -> pokemon.nationalId !in captured
            }
            matches && status
        }.take(8)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explorar · ${context?.regionLabel ?: "Região"}") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { inner ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Box(Modifier.fillMaxSize().padding(inner).padding(24.dp), contentAlignment = Alignment.Center) { Text(error!!) }
            zones.isEmpty() -> Box(Modifier.fillMaxSize().padding(inner).padding(24.dp), contentAlignment = Alignment.Center) { Text("Mapa visual ainda indisponível para esta região.") }
            else -> LazyColumn(Modifier.fillMaxSize().padding(inner), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column(Modifier.padding(16.dp)) {
                        Text(context?.regionLabel ?: "Região", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("$regionCaptured de ${dex.size} capturados")
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip({ filter = MapCaptureFilter.ALL }, { Text("Todos") }, leadingIcon = if (filter == MapCaptureFilter.ALL) ({ Text("✓") }) else null)
                            AssistChip({ filter = MapCaptureFilter.CAPTURED }, { Text("Capturados") }, leadingIcon = if (filter == MapCaptureFilter.CAPTURED) ({ Text("✓") }) else null)
                            AssistChip({ filter = MapCaptureFilter.MISSING }, { Text("Faltantes") }, leadingIcon = if (filter == MapCaptureFilter.MISSING) ({ Text("✓") }) else null)
                        }
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it; if (selectedPokemon?.name?.equals(it, true) != true) selectedPokemon = null },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            label = { Text("Buscar Pokémon") },
                            singleLine = true
                        )
                    }
                }

                if (query.isNotBlank() && selectedPokemon == null) {
                    items(results, key = { it.nationalId }) { pokemon ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { selectedPokemon = pokemon; query = pokemon.name }) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(pokemon.spriteUrl, pokemon.name, Modifier.size(48.dp))
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(pokemon.name, fontWeight = FontWeight.SemiBold)
                                    Text("#${pokemon.gameNumber.toString().padStart(3, '0')} regional · #${pokemon.nationalId.toString().padStart(4, '0')} nacional", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(if (pokemon.nationalId in captured) "✓" else "○", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                selectedPokemon?.let { pokemon ->
                    item {
                        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(pokemon.spriteUrl, pokemon.name, Modifier.size(64.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(pokemon.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(if (pokemon.nationalId in captured) "Capturado" else "Faltando", style = MaterialTheme.typography.labelMedium)
                                    Text(if (loadingEncounters) "Carregando encontros…" else if (encounters.isEmpty()) "Sem spawn detalhado" else "${encounters.size} áreas registradas", style = MaterialTheme.typography.bodySmall)
                                }
                                FilledTonalButton(onClick = { onPokemonClick(pokemon.nationalId, source) }) { Text("Ficha") }
                            }
                        }
                    }
                }

                item {
                    RefinedRegionMap(
                        regionLabel = context?.regionLabel ?: "Região",
                        zones = zones,
                        encounters = encounters,
                        onZoneClick = { selectedZone = it }
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    selectedZone?.let { zone ->
        ModalBottomSheet(onDismissRequest = { selectedZone = null }) {
            val matching = encounters.filter { zone.matches(it.location) }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null)
                    Text(zone.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(8.dp))
                if (selectedPokemon == null) {
                    Text("Selecione um Pokémon para cruzar esta zona com os encontros registrados.")
                } else if (matching.isEmpty()) {
                    Text("Nenhum encontro do Pokémon selecionado foi associado a esta zona pela fonte atual.")
                } else {
                    matching.forEach { encounter ->
                        Text(encounter.location, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                        encounter.details.take(4).forEach { detail ->
                            val level = when {
                                detail.minLevel > 0 && detail.maxLevel > 0 && detail.minLevel != detail.maxLevel -> "Nv. ${detail.minLevel}–${detail.maxLevel}"
                                detail.minLevel > 0 -> "Nv. ${detail.minLevel}"
                                else -> null
                            }
                            val chance = detail.chance.takeIf { it > 0 }?.let { "$it%" }
                            Text(listOfNotNull(detail.method, level, chance).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RefinedRegionMap(
    regionLabel: String,
    zones: List<RegionMapZone>,
    encounters: List<PokeApiService.EncounterLocation>,
    onZoneClick: (RegionMapZone) -> Unit
) {
    var scale by remember(regionLabel) { mutableFloatStateOf(1f) }
    var translation by remember(regionLabel) { mutableStateOf(Offset.Zero) }
    val land = MaterialTheme.colorScheme.surfaceContainerHigh
    val outline = MaterialTheme.colorScheme.outlineVariant

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = { scale = (scale - .25f).coerceAtLeast(1f) }) { Text("−") }
            FilledTonalButton(onClick = { scale = (scale + .25f).coerceAtMost(3f) }) { Text("+") }
            FilledTonalButton(onClick = { scale = 1f; translation = Offset.Zero }) { Text("Centralizar") }
            Text("${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
        Surface(Modifier.fillMaxWidth().height(520.dp), shape = RoundedCornerShape(28.dp), tonalElevation = 2.dp) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant).pointerInput(regionLabel) {
                detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 3f); translation += pan }
            }) {
                BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = translation.x; translationY = translation.y }) {
                    val mapWidth = maxWidth
                    val mapHeight = maxHeight
                    Canvas(Modifier.align(Alignment.Center).width(mapWidth * .86f).height(mapHeight * .88f)) {
                        val w = size.width; val h = size.height
                        val path = regionSilhouette(regionLabel, w, h)
                        drawPath(path, land)
                        drawPath(path, outline, style = Stroke(width = 3f))
                    }
                    Text(regionLabel, Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    zones.forEach { zone ->
                        val highlighted = encounters.any { zone.matches(it.location) }
                        val marker = if (highlighted) 34.dp else 26.dp
                        val x = mapWidth * zone.x - marker / 2
                        val y = mapHeight * zone.y - marker / 2
                        Surface(
                            modifier = Modifier.offset(x, y).size(marker).clickable { onZoneClick(zone) },
                            shape = CircleShape,
                            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (highlighted) 6.dp else 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(if (highlighted) Icons.Default.CatchingPokemon else Icons.Default.LocationOn, zone.label, Modifier.size(if (highlighted) 18.dp else 14.dp), tint = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
        Text("Toque em um ponto para abrir os detalhes da zona.", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
    }
}

private fun regionSilhouette(region: String, w: Float, h: Float): Path = Path().apply {
    when (region) {
        "Kitakami" -> {
            moveTo(w * .50f, h * .04f); quadraticBezierTo(w * .82f, h * .12f, w * .82f, h * .38f)
            quadraticBezierTo(w * .95f, h * .58f, w * .70f, h * .76f); quadraticBezierTo(w * .56f, h * .98f, w * .35f, h * .84f)
            quadraticBezierTo(w * .06f, h * .76f, w * .18f, h * .46f); quadraticBezierTo(w * .12f, h * .18f, w * .50f, h * .04f); close()
        }
        "Blueberry" -> {
            moveTo(w * .50f, h * .05f); quadraticBezierTo(w * .92f, h * .15f, w * .92f, h * .50f)
            quadraticBezierTo(w * .92f, h * .85f, w * .50f, h * .95f); quadraticBezierTo(w * .08f, h * .85f, w * .08f, h * .50f)
            quadraticBezierTo(w * .08f, h * .15f, w * .50f, h * .05f); close()
        }
        else -> {
            moveTo(w * .50f, h * .03f); quadraticBezierTo(w * .72f, h * .04f, w * .84f, h * .20f)
            quadraticBezierTo(w * .98f, h * .34f, w * .88f, h * .51f); quadraticBezierTo(w * .96f, h * .69f, w * .74f, h * .78f)
            quadraticBezierTo(w * .65f, h * .96f, w * .48f, h * .91f); quadraticBezierTo(w * .27f, h * .98f, w * .20f, h * .77f)
            quadraticBezierTo(w * .03f, h * .64f, w * .14f, h * .45f); quadraticBezierTo(w * .03f, h * .25f, w * .25f, h * .17f)
            quadraticBezierTo(w * .34f, h * .02f, w * .50f, h * .03f); close()
        }
    }
}
