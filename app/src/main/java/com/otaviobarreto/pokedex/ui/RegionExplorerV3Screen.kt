package com.otaviobarreto.pokedex.ui

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.CommunityAreaEncounter
import com.otaviobarreto.pokedex.data.CommunityEncounterIndex
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.PokedexDataStore
import com.otaviobarreto.pokedex.data.RegionMapCatalog
import com.otaviobarreto.pokedex.data.RegionMapVisualCatalog
import com.otaviobarreto.pokedex.data.RegionMapZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

private enum class HdMapFilter { ALL, CAPTURED, MISSING }

private data class HdZonePokemon(
    val pokemon: GameDexService.GameDexEntry,
    val areas: List<CommunityAreaEncounter>
)

@Composable
fun RegionExplorerV3Screen(
    source: String,
    onBack: () -> Unit,
    onPokemonClick: (Int, String) -> Unit
) {
    val context = remember(source) { GameContext.fromSource(source) }
    if (context?.regionLabel != "Paldea") {
        RegionExplorerV2Screen(source, onBack, onPokemonClick)
        return
    }
    PaldeaHdExplorer(source, context, onBack, onPokemonClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaldeaHdExplorer(
    source: String,
    context: GameContext,
    onBack: () -> Unit,
    onPokemonClick: (Int, String) -> Unit
) {
    val zones = remember(context) { RegionMapCatalog.zones(context) }
    val mapVisual = remember { RegionMapVisualCatalog.visualFor("Paldea") }
    var dex by remember(source) { mutableStateOf(GameDexService.cached(context).orEmpty()) }
    var selectedPokemon by remember { mutableStateOf<GameDexService.GameDexEntry?>(null) }
    var encounters by remember { mutableStateOf<List<PokeApiService.EncounterLocation>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(HdMapFilter.ALL) }
    var loading by remember(source) { mutableStateOf(dex.isEmpty()) }
    var loadingEncounters by remember { mutableStateOf(false) }
    var error by remember(source) { mutableStateOf<String?>(null) }
    var selectedZone by remember(source) { mutableStateOf<RegionMapZone?>(null) }

    LaunchedEffect(source) {
        runCatching { withContext(Dispatchers.IO) { GameDexService.loadGameDex(context) } }
            .onSuccess { dex = it; loading = false }
            .onFailure { error = "Não foi possível carregar a Pokédex de Paldea."; loading = false }
    }

    LaunchedEffect(selectedPokemon?.nationalId, source) {
        val pokemon = selectedPokemon ?: run { encounters = emptyList(); return@LaunchedEffect }
        val cachedEncounters = PokedexDataStore.cachedEncounters(pokemon.nationalId)
        loadingEncounters = cachedEncounters == null
        encounters = (cachedEncounters ?: runCatching {
            withContext(Dispatchers.IO) { PokedexDataStore.encounters(pokemon.nationalId) }
        }.getOrElse { emptyList() }).mapNotNull { encounter ->
            val versions = encounter.versions.filter(context::matchesVersion)
            val details = encounter.details.filter { context.matchesVersion(it.version) }
            if (versions.isEmpty() && details.isEmpty()) null else encounter.copy(versions = versions, details = details)
        }
        selectedZone = null
        loadingEncounters = false
    }

    val captured = CollectionStore.capturedIds
    val results = remember(dex, query, filter, captured) {
        val q = query.trim().removePrefix("#")
        if (q.isBlank()) emptyList() else dex.filter { p ->
            val matches = p.name.contains(q, true) || p.nationalId.toString() == q || p.gameNumber.toString() == q
            matches && when (filter) {
                HdMapFilter.ALL -> true
                HdMapFilter.CAPTURED -> p.nationalId in captured
                HdMapFilter.MISSING -> p.nationalId !in captured
            }
        }.take(8)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paldea · mapa HD") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { inner ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Box(Modifier.fillMaxSize().padding(inner).padding(24.dp), contentAlignment = Alignment.Center) { Text(error!!) }
            else -> LazyColumn(Modifier.fillMaxSize().padding(inner), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text("Mapa cartográfico de Paldea", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Base 2048×2048 licenciada em CC0. Os pontos interativos são sobrepostos pelo POKEDEX.", style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip({ filter = HdMapFilter.ALL }, { Text("Todos") }, leadingIcon = if (filter == HdMapFilter.ALL) ({ Text("✓") }) else null)
                            AssistChip({ filter = HdMapFilter.CAPTURED }, { Text("Capturados") }, leadingIcon = if (filter == HdMapFilter.CAPTURED) ({ Text("✓") }) else null)
                            AssistChip({ filter = HdMapFilter.MISSING }, { Text("Faltantes") }, leadingIcon = if (filter == HdMapFilter.MISSING) ({ Text("✓") }) else null)
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
                                Text(if (pokemon.nationalId in captured) "✓" else "○")
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
                                    Text(if (pokemon.nationalId in captured) "Capturado" else "Faltando")
                                    Text(if (loadingEncounters) "Carregando encontros…" else if (encounters.isEmpty()) "Sem spawn detalhado na PokéAPI" else "${encounters.size} áreas registradas", style = MaterialTheme.typography.bodySmall)
                                }
                                FilledTonalButton(onClick = { onPokemonClick(pokemon.nationalId, source) }) { Text("Ficha") }
                            }
                        }
                    }
                }

                item {
                    HdPaldeaMap(
                        imageUrl = mapVisual?.imageUrl.orEmpty(),
                        zones = zones,
                        encounters = encounters,
                        onZoneClick = { selectedZone = it }
                    )
                }

                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(mapVisual?.sourceLabel.orEmpty(), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        Text(mapVisual?.licenseLabel.orEmpty(), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        Text("Não é uma imagem oficial da Nintendo/The Pokémon Company; é uma recriação cartográfica licenciada para reutilização.", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
                item { Spacer(Modifier.size(12.dp)) }
            }
        }
    }

    selectedZone?.let { zone ->
        ModalBottomSheet(onDismissRequest = { selectedZone = null }) {
            val matching = encounters.filter { zone.matches(it.location) }
            val areas = CommunityEncounterIndex.entriesForZone("Paldea", zone)
            val zonePokemon = resolveHdZonePokemon(dex, areas).filter { item ->
                when (filter) {
                    HdMapFilter.ALL -> true
                    HdMapFilter.CAPTURED -> item.pokemon.nationalId in captured
                    HdMapFilter.MISSING -> item.pokemon.nationalId !in captured
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
                    val fallback = zonePokemon.firstOrNull { it.pokemon.nationalId == selectedPokemon?.nationalId }
                    if (fallback != null) Text("O índice comunitário registra esta espécie nesta zona.", modifier = Modifier.padding(top = 8.dp))
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
private fun HdPaldeaMap(
    imageUrl: String,
    zones: List<RegionMapZone>,
    encounters: List<PokeApiService.EncounterLocation>,
    onZoneClick: (RegionMapZone) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = { scale = (scale - .25f).coerceAtLeast(1f) }) { Text("−") }
            FilledTonalButton(onClick = { scale = (scale + .25f).coerceAtMost(4f) }) { Text("+") }
            FilledTonalButton(onClick = { scale = 1f; translation = Offset.Zero }) { Text("Centralizar") }
            Text("${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.size(8.dp))
        Surface(Modifier.fillMaxWidth().aspectRatio(1f), shape = RoundedCornerShape(24.dp), tonalElevation = 2.dp) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) {
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
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Mapa cartográfico HD de Paldea",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
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
        Text("Mapa HD com zoom de até 400%. Toque nos marcadores para abrir as áreas.", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
    }
}

private fun resolveHdZonePokemon(
    dex: List<GameDexService.GameDexEntry>,
    areas: List<CommunityAreaEncounter>
): List<HdZonePokemon> {
    val byName = dex.associateBy { normalizeHdPokemonName(it.name) }
    val grouped = linkedMapOf<Int, Pair<GameDexService.GameDexEntry, MutableList<CommunityAreaEncounter>>>()
    areas.forEach { area ->
        area.pokemonNames.forEach inner@{ rawName ->
            val pokemon = byName[normalizeHdPokemonName(rawName)] ?: return@inner
            grouped.getOrPut(pokemon.nationalId) { pokemon to mutableListOf() }.second += area
        }
    }
    return grouped.values.map { (pokemon, pokemonAreas) -> HdZonePokemon(pokemon, pokemonAreas) }.sortedBy { it.pokemon.gameNumber }
}

private fun normalizeHdPokemonName(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .replace("’", "'")
    .lowercase()
    .replace("paldean", "")
    .replace(Regex("\\([^)]*\\)"), "")
    .replace(Regex("[^a-z0-9]+"), "")
