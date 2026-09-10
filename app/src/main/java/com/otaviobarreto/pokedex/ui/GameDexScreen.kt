package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import com.otaviobarreto.pokedex.data.PokedexDataStore
import com.otaviobarreto.pokedex.data.RegionMapCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDexScreen(
    source: String,
    onBack: () -> Unit,
    onPokemonClick: (Int, String) -> Unit,
    onLocationClick: (Int, String) -> Unit,
    onOpenRegionExplorer: (String) -> Unit
) {
    val context = remember(source) { GameContext.fromSource(source) }
    var entries by remember(source) { mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList()) }
    var loading by remember(source) { mutableStateOf(true) }
    var error by remember(source) { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var onlyMissing by remember { mutableStateOf(false) }

    LaunchedEffect(source) {
        val game = context
        if (game == null) {
            error = "Este contexto não está vinculado a um jogo reconhecido."
            loading = false
        } else {
            runCatching {
                withContext(Dispatchers.IO) { GameDexService.loadGameDex(game) }
            }.onSuccess {
                entries = it
                loading = false
            }.onFailure {
                error = "Não foi possível carregar a Pokédex desta região."
                loading = false
            }
        }
    }

    val captured = CollectionStore.capturedIds
    val capturedInGame = entries.count { it.nationalId in captured }
    val progress = if (entries.isEmpty()) 0f else capturedInGame.toFloat() / entries.size
    val normalized = query.trim().removePrefix("#")
    val filtered = entries.filter { entry ->
        val queryOk = normalized.isBlank() ||
            entry.name.contains(normalized, ignoreCase = true) ||
            entry.nationalId.toString() == normalized ||
            entry.gameNumber.toString() == normalized
        val statusOk = !onlyMissing || entry.nationalId !in captured
        queryOk && statusOk
    }
    val hasRegionalMap = RegionMapCatalog.zones(context).isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context?.regionLabel ?: "Pokédex do jogo") },
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
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Carregando Pokédex regional…")
            }

            error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(error!!, style = MaterialTheme.typography.bodyLarge)
            }

            else -> Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFFF8F8FC))) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                context?.regionLabel ?: "Pokédex regional",
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF151426)
                            )
                            Text((context?.label ?: source).uppercase(), fontSize=8.sp, color=Color(0xFF72778B))
                        }
                        if (hasRegionalMap) {
                            FilledTonalButton(onClick = { onOpenRegionExplorer(source) }) {
                                Icon(Icons.Default.Map, contentDescription = null)
                                Text("Mapa", modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                    Text("$capturedInGame de ${entries.size} capturados nesta Pokédex")
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = Color(0xFF5B55E7),
                        trackColor = Color(0xFF5B55E7).copy(alpha=.12f)
                    )
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        singleLine = true,
                        placeholder = { Text("Nome, nº regional ou National Dex") },
                        shape = RoundedCornerShape(18.dp)
                    )
                    AssistChip(
                        onClick = { onlyMissing = !onlyMissing },
                        label = { Text(if (onlyMissing) "Mostrando faltantes" else "Mostrar só faltantes") },
                        leadingIcon = if (onlyMissing) ({ Text("✓") }) else null,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Text(
                    "${filtered.size} Pokémon",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.gameNumber }) { pokemon ->
                        LaunchedEffect(pokemon.nationalId) { PokedexDataStore.prefetchDetails(pokemon.nationalId) }
                        val isCaptured = pokemon.nationalId in captured
                        val pokemonBoxes = CollectionStore.boxesForPokemon(pokemon.nationalId)
                        val isInBox = pokemonBoxes.isNotEmpty()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { onPokemonClick(pokemon.nationalId, source) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = if(isCaptured) Color(0xFFEAE8FB) else Color(0xFFF1F0F8))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = pokemon.spriteUrl,
                                    contentDescription = pokemon.name,
                                    modifier = Modifier.size(64.dp).padding(3.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                    Text(
                                        "#${pokemon.gameNumber.toString().padStart(3, '0')} · National #${pokemon.nationalId.toString().padStart(4, '0')}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(pokemon.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Disponível em ${context?.regionLabel ?: "esta Pokédex"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        when {
                                            isInBox -> pokemonBoxes.joinToString(" · ") { it.substringAfterLast("· ").trim() }
                                            else -> "Faltando"
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { onLocationClick(pokemon.nationalId, source) }) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Localizações")
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
