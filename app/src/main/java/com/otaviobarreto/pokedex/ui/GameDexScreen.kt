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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDexScreen(
    source: String,
    onBack: () -> Unit,
    onPokemonClick: (Int, String) -> Unit,
    onLocationClick: (Int, String) -> Unit
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
    val relatedBox = context?.label
    val boxPokemon = relatedBox?.let { CollectionStore.boxes[it].orEmpty() }.orEmpty()
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

            else -> Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        context?.regionLabel ?: "Pokédex regional",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(context?.label ?: source, style = MaterialTheme.typography.bodyMedium)
                    Text("$capturedInGame de ${entries.size} capturados nesta Pokédex")
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        singleLine = true,
                        label = { Text("Nome, nº regional ou National Dex") }
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
                        val isCaptured = pokemon.nationalId in captured
                        val isInBox = pokemon.nationalId in boxPokemon
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { onPokemonClick(pokemon.nationalId, source) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = pokemon.spriteUrl,
                                    contentDescription = pokemon.name,
                                    modifier = Modifier.size(64.dp)
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
                                            isInBox -> "Na Box · ${if (isCaptured) "Capturado" else "Não marcado"}"
                                            isCaptured -> "Capturado"
                                            else -> "Faltando"
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    FilledTonalIconButton(onClick = { onLocationClick(pokemon.nationalId, source) }) {
                                        Icon(Icons.Default.LocationOn, contentDescription = "Localizações")
                                    }
                                    Button(onClick = { CollectionStore.toggleCaptured(pokemon.nationalId) }) {
                                        Text(if (isCaptured) "✓" else "+")
                                    }
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
