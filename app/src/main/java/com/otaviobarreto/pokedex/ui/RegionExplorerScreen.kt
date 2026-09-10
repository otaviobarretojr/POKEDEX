package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.RegionMapCatalog
import com.otaviobarreto.pokedex.data.RegionMapZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    val filteredDex = remember(dex, query) {
        val q = query.trim().removePrefix("#")
        if (q.isBlank()) emptyList()
        else dex.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.nationalId.toString() == q ||
                it.gameNumber.toString() == q
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
                        Text("Pesquise um Pokémon desta Pokédex para destacar as zonas de encontro.")
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            label = { Text("Pokémon, nº regional ou National Dex") }
                        )
                    }
                }

                if (query.isNotBlank() && selectedPokemon == null) {
                    items(filteredDex, key = { it.nationalId }) { pokemon ->
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
                                Column(Modifier.padding(start = 10.dp)) {
                                    Text(pokemon.name, fontWeight = FontWeight.SemiBold)
                                    Text("Regional #${pokemon.gameNumber.toString().padStart(3, '0')} · National #${pokemon.nationalId.toString().padStart(4, '0')}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                selectedPokemon?.let { pokemon ->
                    item {
                        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(pokemon.spriteUrl, pokemon.name, Modifier.size(72.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(pokemon.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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

                if (selectedPokemon != null && !loadingEncounters) {
                    val matchedZones = zones.filter { zone -> encounters.any { zone.matches(it.location) } }
                    item {
                        Text(
                            if (matchedZones.isEmpty()) "Nenhuma zona destacada com os dados atuais."
                            else "Zonas encontradas: ${matchedZones.joinToString { it.label }}",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = { scale = (scale - 0.25f).coerceAtLeast(1f) }) { Text("−") }
            FilledTonalButton(onClick = { scale = (scale + 0.25f).coerceAtMost(3f) }) { Text("+") }
            FilledTonalButton(onClick = { scale = 1f; translation = Offset.Zero }) { Text("Centralizar") }
            Text("${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(480.dp),
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
                    Box(
                        Modifier.align(Alignment.Center)
                            .width(mapWidth * 0.80f)
                            .height(mapHeight * 0.84f)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(54.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(54.dp))
                    )
                    Text(
                        regionLabel,
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    zones.forEach { zone ->
                        val highlighted = encounters.any { zone.matches(it.location) }
                        val selected = selectedZone?.id == zone.id
                        val markerWidth = 110.dp
                        val markerHeight = 60.dp
                        val x = (mapWidth * zone.x) - markerWidth / 2
                        val y = (mapHeight * zone.y) - markerHeight / 2
                        Card(
                            modifier = Modifier.offset(x = x, y = y)
                                .width(markerWidth).height(markerHeight)
                                .clickable { selectedZone = zone },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().background(
                                    when {
                                        selected -> MaterialTheme.colorScheme.primaryContainer
                                        highlighted -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    }
                                ).padding(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (highlighted) Icon(Icons.Default.LocationOn, contentDescription = null, Modifier.size(16.dp))
                                Text(
                                    zone.label,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
        selectedZone?.let { zone ->
            val matching = encounters.filter { zone.matches(it.location) }
            Text(zone.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            Text(
                if (matching.isEmpty()) "Nenhum encontro associado a esta zona para o Pokémon selecionado."
                else matching.joinToString(" · ") { it.location },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
