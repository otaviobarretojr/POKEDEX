package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.RegionMapCatalog
import com.otaviobarreto.pokedex.data.RegionMapZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonRegionMapScreen(
    pokemonId: Int,
    source: String,
    onBack: () -> Unit
) {
    val context = remember(source) { GameContext.fromSource(source) }
    val zones = remember(context) { RegionMapCatalog.zones(context) }
    var pokemon by remember(pokemonId) { mutableStateOf<PokeApiService.RemotePokemonDetail?>(null) }
    var encounters by remember(pokemonId) { mutableStateOf<List<PokeApiService.EncounterLocation>>(emptyList()) }
    var selectedZone by remember(source) { mutableStateOf<RegionMapZone?>(null) }
    var loading by remember(pokemonId, source) { mutableStateOf(true) }
    var error by remember(pokemonId, source) { mutableStateOf<String?>(null) }

    LaunchedEffect(pokemonId, source) {
        loading = true
        error = null
        runCatching {
            withContext(Dispatchers.IO) {
                PokeApiService.loadPokemon(pokemonId) to PokeApiService.loadEncounters(pokemonId)
            }
        }.onSuccess { (loadedPokemon, loadedEncounters) ->
            pokemon = loadedPokemon
            encounters = if (context == null) loadedEncounters else loadedEncounters.mapNotNull { encounter ->
                val versions = encounter.versions.filter(context::matchesVersion)
                val details = encounter.details.filter { context.matchesVersion(it.version) }
                if (versions.isEmpty() && details.isEmpty()) null
                else encounter.copy(versions = versions, details = details)
            }
            loading = false
        }.onFailure {
            error = "Não foi possível carregar os dados do mapa para este Pokémon."
            loading = false
        }
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
            loading -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Montando mapa de encontros…")
            }

            error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(error!!)
            }

            zones.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("O mapa visual ainda está disponível somente para Paldea, Kitakami e Blueberry.")
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = pokemon?.spriteUrl,
                                contentDescription = pokemon?.name,
                                modifier = Modifier.size(88.dp)
                            )
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(
                                    pokemon?.name ?: "Pokémon #$pokemonId",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${context?.label ?: source} · ${context?.regionLabel ?: ""}")
                                Text(
                                    if (encounters.isEmpty()) "Sem áreas detalhadas na fonte atual"
                                    else "${encounters.size} área${if (encounters.size == 1) "" else "s"} de encontro na fonte",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Mapa esquemático original. As zonas destacadas possuem encontro compatível com os dados disponíveis; a posição dos blocos representa setores da região, não coordenadas exatas do jogo.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                item {
                    RegionZoneGrid(
                        zones = zones,
                        encounters = encounters,
                        selectedZone = selectedZone,
                        onSelect = { selectedZone = it }
                    )
                }

                selectedZone?.let { zone ->
                    val matching = encounters.filter { zone.matches(it.location) }
                    item {
                        Text(
                            zone.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    if (matching.isEmpty()) {
                        item {
                            Text(
                                "Nenhum encontro deste Pokémon foi associado a esta zona pela fonte atual.",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        items(matching, key = { it.location }) { encounter ->
                            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null)
                                        Text(
                                            encounter.location,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    encounter.details.forEach { detail ->
                                        val level = when {
                                            detail.minLevel > 0 && detail.maxLevel > 0 && detail.minLevel != detail.maxLevel -> "Nv. ${detail.minLevel}–${detail.maxLevel}"
                                            detail.minLevel > 0 -> "Nv. ${detail.minLevel}"
                                            else -> null
                                        }
                                        val chance = detail.chance.takeIf { it > 0 }?.let { "$it%" }
                                        Text(
                                            listOfNotNull(detail.method, level, chance).joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun RegionZoneGrid(
    zones: List<RegionMapZone>,
    encounters: List<PokeApiService.EncounterLocation>,
    selectedZone: RegionMapZone?,
    onSelect: (RegionMapZone) -> Unit
) {
    val ordered = zones.sortedWith(compareBy<RegionMapZone> { it.row }.thenBy { it.column })
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(390.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(ordered, key = { it.id }) { zone ->
            val hasEncounter = encounters.any { zone.matches(it.location) }
            val selected = selectedZone?.id == zone.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clickable { onSelect(zone) },
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        selected -> MaterialTheme.colorScheme.primaryContainer
                        hasEncounter -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (hasEncounter) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        zone.label,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (hasEncounter) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
