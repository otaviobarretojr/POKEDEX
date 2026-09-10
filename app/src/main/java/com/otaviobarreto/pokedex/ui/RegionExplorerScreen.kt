package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

private enum class ExplorerCaptureFilter { ALL, CAPTURED, MISSING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionExplorerScreen(
    source: String,
    onBack: () -> Unit,
    onPokemonClick: (Int, String) -> Unit
) {
    val context = remember(source) { GameContext.fromSource(source) }
    val zones = remember(context) { RegionMapCatalog.zones(context) }
    var dex by remember(source) { mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selectedPokemon by remember { mutableStateOf<GameDexService.GameDexEntry?>(null) }
    var encounters by remember { mutableStateOf<List<PokeApiService.EncounterLocation>>(emptyList()) }
    var loadingDex by remember(source) { mutableStateOf(true) }
    var loadingEncounters by remember { mutableStateOf(false) }
    var error by remember(source) { mutableStateOf<String?>(null) }
    var captureFilter by remember { mutableStateOf(ExplorerCaptureFilter.ALL) }

    LaunchedEffect(source) {
        val game = context
        if (game == null) {
            error = "Este mapa não possui contexto de jogo reconhecido."
            loadingDex = false
        } else {
            runCatching { withContext(Dispatchers.IO) { GameDexService.loadGameDex(game) } }
                .onSuccess { dex = it; loadingDex = false }
                .onFailure { error = "Não foi possível carregar a Pokédex desta região."; loadingDex = false }
        }
    }

    LaunchedEffect(selectedPokemon?.nationalId, source) {
        val pokemon = selectedPokemon ?: run {
            encounters = emptyList()
            return@LaunchedEffect
        }
        loadingEncounters = true
        encounters = runCatching {
            withContext(Dispatchers.IO) { PokeApiService.loadEncounters(pokemon.nationalId) }
        }.getOrElse { emptyList() }
            .mapNotNull { encounter ->
                val game = context ?: return@mapNotNull encounter
                val versions = encounter.versions.filter(game::matchesVersion)
                val details = encounter.details.filter { game.matchesVersion(it.version) }
                if (versions.isEmpty() && details.isEmpty()) null
                else encounter.copy(versions = versions, details = details)
            }
        loadingEncounters = false
    }

    val captured = CollectionStore.capturedIds
    val regionCaptured = dex.count { it.nationalId in captured }
    val filteredDex = remember(dex, query, captureFilter, captured) {
        val q = query.trim().removePrefix("#")
        if (q.isBlank()) emptyList()
        else dex.filter { pokemon ->
            val queryOk = pokemon.name.contains(q, ignoreCase = true) ||
                pokemon.nationalId.toString() == q ||
                pokemon.gameNumber.toString() == q
            val captureOk = when (captureFilter) {
                ExplorerCaptureFilter.ALL -> true
                ExplorerCaptureFilter.CAPTURED -> pokemon.nationalId in captured
                ExplorerCaptureFilter.MISSING -> pokemon.nationalId !in captured
            }
            queryOk && captureOk
        }.take(8)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa · ${context?.regionLabel ?: "Região"}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            loadingDex -> Column(
                Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Carregando explorador regional…")
            }
            error != null -> Column(
                Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { Text(error!!) }
            zones.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("O explorador regional está disponível para Paldea, Kitakami e Blueberry.")
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            context?.regionLabel ?: "Região",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("$regionCaptured de ${dex.size} capturados nesta Pokédex")
                        Text(
                            "Pesquise um Pokémon e o mapa destaca as zonas de encontro disponíveis na fonte.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { captureFilter = ExplorerCaptureFilter.ALL },
                                label = { Text("Todos") },
                                leadingIcon = if (captureFilter == ExplorerCaptureFilter.ALL) ({ Text("✓") }) else null
                            )
                            AssistChip(
                                onClick = { captureFilter = ExplorerCaptureFilter.CAPTURED },
                                label = { Text("Capturados") },
                                leadingIcon = if (captureFilter == ExplorerCaptureFilter.CAPTURED) ({ Text("✓") }) else null
                            )
                            AssistChip(
                                onClick = { captureFilter = ExplorerCaptureFilter.MISSING },
                                label = { Text("Faltantes") },
                                leadingIcon = if (captureFilter == ExplorerCaptureFilter.MISSING) ({ Text("✓") }) else null
                            )
                        }
                        OutlinedTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                if (selectedPokemon?.name?.equals(it, ignoreCase = true) != true) selectedPokemon = null
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            label = { Text("Pokémon, nº regional ou National Dex") }
                        )
                    }
                }

                if (query.isNotBlank() && selectedPokemon == null) {
                    if (filteredDex.isEmpty()) {
                        item {
                            Text(
                                "Nenhum Pokémon corresponde à busca e ao filtro selecionado.",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        items(filteredDex, key = { it.nationalId }) { pokemon ->
                            val isCaptured = pokemon.nationalId in captured
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                    .clickable {
                                        selectedPokemon = pokemon
                                        query = pokemon.name
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(pokemon.spriteUrl, pokemon.name, Modifier.size(52.dp))
                                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                        Text(pokemon.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Regional #${pokemon.gameNumber.toString().padStart(3, '0')} · National #${pokemon.nationalId.toString().padStart(4, '0')}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(if (isCaptured) "✓ Capturado" else "Faltando", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                selectedPokemon?.let { pokemon ->
                    val isCaptured = pokemon.nationalId in captured
                    item {
                        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(pokemon.spriteUrl, pokemon.name, Modifier.size(72.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(pokemon.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(if (isCaptured) "Capturado" else "Ainda faltando", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        when {
                                            loadingEncounters -> "Carregando encontros…"
                                            encounters.isEmpty() -> "Sem spawn detalhado nesta fonte"
                                            else -> "${encounters.size} área${if (encounters.size == 1) "" else "s"} encontrada${if (encounters.size == 1) "" else "s"}"
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                FilledTonalButton(onClick = { onPokemonClick(pokemon.nationalId, source) }) {
                                    Text("Ficha")
                                }
                            }
                        }
                    }
                }

                item {
                    ExplorerMap(
                        regionLabel = context?.regionLabel ?: "Região",
                        zones = zones,
                        encounters = encounters
                    )
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun ExplorerMap(
    regionLabel: String,
    zones: List<RegionMapZone>,
    encounters: List<PokeApiService.EncounterLocation>
) {
    var scale by remember(regionLabel) { mutableFloatStateOf(1f) }
    var translation by remember(regionLabel) { mutableStateOf(Offset.Zero) }
    var selectedZone by remember(regionLabel) { mutableStateOf<RegionMapZone?>(null) }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val land = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(onClick = { scale = (scale - 0.25f).coerceAtLeast(1f) }) { Text("−") }
            FilledTonalButton(onClick = { scale = (scale + 0.25f).coerceAtMost(3f) }) { Text("+") }
            FilledTonalButton(onClick = { scale = 1f; translation = Offset.Zero }) { Text("Centralizar") }
            Text("${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(500.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp
        ) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(regionLabel) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 3f)
                            translation += pan
                        }
                    }
            ) {
                BoxWithConstraints(
                    Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = translation.x
                        translationY = translation.y
                    }
                ) {
                    val mapHeight = maxHeight
                    val mapWidth = maxWidth
                    Canvas(
                        modifier = Modifier.align(Alignment.Center)
                            .width(mapWidth * 0.84f)
                            .height(mapHeight * 0.86f)
                    ) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.50f, h * 0.03f)
                            quadraticBezierTo(w * 0.72f, h * 0.04f, w * 0.84f, h * 0.20f)
                            quadraticBezierTo(w * 0.98f, h * 0.34f, w * 0.88f, h * 0.51f)
                            quadraticBezierTo(w * 0.96f, h * 0.69f, w * 0.74f, h * 0.78f)
                            quadraticBezierTo(w * 0.65f, h * 0.96f, w * 0.48f, h * 0.91f)
                            quadraticBezierTo(w * 0.27f, h * 0.98f, w * 0.20f, h * 0.77f)
                            quadraticBezierTo(w * 0.03f, h * 0.64f, w * 0.14f, h * 0.45f)
                            quadraticBezierTo(w * 0.03f, h * 0.25f, w * 0.25f, h * 0.17f)
                            quadraticBezierTo(w * 0.34f, h * 0.02f, w * 0.50f, h * 0.03f)
                            close()
                        }
                        drawPath(path = path, color = land)
                        drawPath(path = path, color = outline, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                    }

                    Text(
                        regionLabel,
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    zones.forEach { zone ->
                        val highlighted = encounters.any { zone.matches(it.location) }
                        val selected = selectedZone?.id == zone.id
                        val markerSize = if (highlighted || selected) 38.dp else 30.dp
                        val x = (mapWidth * zone.x) - markerSize / 2
                        val y = (mapHeight * zone.y) - markerSize / 2
                        Surface(
                            modifier = Modifier.offset(x = x, y = y)
                                .size(markerSize)
                                .clickable { selectedZone = zone },
                            shape = CircleShape,
                            color = when {
                                selected -> primary
                                highlighted -> secondary
                                else -> MaterialTheme.colorScheme.surface
                            },
                            shadowElevation = if (highlighted || selected) 6.dp else 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (highlighted) Icons.Default.CatchingPokemon else Icons.Default.LocationOn,
                                    contentDescription = zone.label,
                                    modifier = Modifier.size(if (highlighted || selected) 20.dp else 16.dp),
                                    tint = when {
                                        selected -> MaterialTheme.colorScheme.onPrimary
                                        highlighted -> MaterialTheme.colorScheme.onSecondary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        selectedZone?.let { zone ->
            val matching = encounters.filter { zone.matches(it.location) }
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Text(
                            zone.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (matching.isEmpty()) {
                        Text(
                            "Nenhum encontro do Pokémon selecionado foi associado a esta zona.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    } else {
                        matching.forEach { encounter ->
                            Text(
                                encounter.location,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            encounter.details.take(3).forEach { detail ->
                                val level = when {
                                    detail.minLevel > 0 && detail.maxLevel > 0 && detail.minLevel != detail.maxLevel -> "Nv. ${detail.minLevel}–${detail.maxLevel}"
                                    detail.minLevel > 0 -> "Nv. ${detail.minLevel}"
                                    else -> null
                                }
                                val chance = detail.chance.takeIf { it > 0 }?.let { "$it%" }
                                Text(
                                    listOfNotNull(detail.method, level, chance).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            "Marcadores destacados indicam zonas associadas aos encontros da fonte. O desenho é uma representação esquemática e não uma coordenada oficial de spawn.",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}
