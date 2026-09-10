package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.RegionMapCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonLocationScreen(
    pokemonId: Int,
    source: String? = null,
    onBack: () -> Unit,
    onOpenMap: (() -> Unit)? = null
) {
    val gameContext = remember(source) { GameContext.fromSource(source) }
    val hasVisualMap = remember(gameContext) { RegionMapCatalog.zones(gameContext).isNotEmpty() }
    var pokemon by remember(pokemonId) { mutableStateOf<PokeApiService.RemotePokemonDetail?>(null) }
    var encounters by remember(pokemonId) { mutableStateOf<List<PokeApiService.EncounterLocation>>(emptyList()) }
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
            encounters = loadedEncounters
            loading = false
        }.onFailure {
            error = "Não foi possível carregar as localizações deste Pokémon."
            loading = false
        }
    }

    val filtered = remember(encounters, gameContext) {
        if (gameContext == null) encounters else encounters.mapNotNull { encounter ->
            val versions = encounter.versions.filter(gameContext::matchesVersion)
            val details = encounter.details.filter { gameContext.matchesVersion(it.version) }
            if (versions.isEmpty() && details.isEmpty()) null
            else encounter.copy(versions = versions, details = details)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Localizações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (hasVisualMap && onOpenMap != null) {
                        IconButton(onClick = onOpenMap) {
                            Icon(Icons.Default.Map, contentDescription = "Abrir mapa visual")
                        }
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
                Text("Carregando áreas de encontro…")
            }

            error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(error!!)
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFFF8F8FC)),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = pokemon?.spriteUrl,
                                contentDescription = pokemon?.name,
                                modifier = Modifier.size(96.dp).padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(
                                    pokemon?.name ?: "Pokémon #$pokemonId",
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF151426)
                                )
                                Text("National Dex #${pokemonId.toString().padStart(4, '0')}")
                                gameContext?.let {
                                    Text(
                                        "${it.label} · ${it.regionLabel}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (filtered.isEmpty()) {
                                if (gameContext == null) "Nenhum encontro selvagem registrado na fonte atual."
                                else "Sem spawn detalhado registrado para ${gameContext.regionLabel}. O Pokémon pode existir nessa Pokédex por evolução, troca, evento ou outro método não descrito pelo endpoint de encontros."
                            } else {
                                "${filtered.size} área${if (filtered.size == 1) "" else "s"} de encontro encontrada${if (filtered.size == 1) "" else "s"}."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (hasVisualMap && onOpenMap != null) {
                            Text(
                                "Use o ícone de mapa no topo para visualizar os encontros por zona.",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                items(filtered, key = { it.location }) { location ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                                Text(
                                    location.location,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            if (location.versions.isNotEmpty()) {
                                Text(
                                    location.versions.joinToString(" • "),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            if (location.details.isEmpty()) {
                                Text(
                                    "Método detalhado não informado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            } else {
                                location.details.forEach { detail ->
                                    val levelText = when {
                                        detail.minLevel > 0 && detail.maxLevel > 0 && detail.minLevel != detail.maxLevel -> "Nv. ${detail.minLevel}–${detail.maxLevel}"
                                        detail.minLevel > 0 -> "Nv. ${detail.minLevel}"
                                        else -> null
                                    }
                                    val chanceText = detail.chance.takeIf { it > 0 }?.let { "$it%" }
                                    val extras = listOfNotNull(levelText, chanceText).joinToString(" · ")
                                    Text(
                                        buildString {
                                            append("• ${detail.method}")
                                            if (extras.isNotBlank()) append(" · $extras")
                                            if (detail.conditions.isNotEmpty()) append(" · ${detail.conditions.joinToString()}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
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
