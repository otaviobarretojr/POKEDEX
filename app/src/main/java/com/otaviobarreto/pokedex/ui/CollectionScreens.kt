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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.otaviobarreto.pokedex.data.PokeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LivingDexScreen(onPokemonClick: (Int) -> Unit) {
    var dex by remember { mutableStateOf<List<PokeApiService.DexIndexEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var onlyMissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dex = runCatching { withContext(Dispatchers.IO) { PokeApiService.loadNationalDex() } }
            .getOrElse { emptyList() }
        loading = false
    }

    val captured = CollectionStore.capturedIds
    val total = if (dex.isNotEmpty()) dex.size else 1025
    val progress = if (total == 0) 0f else captured.size.coerceAtMost(total).toFloat() / total
    val filtered = dex.filter { p ->
        val q = query.trim().removePrefix("#")
        val queryOk = q.isBlank() || p.name.contains(q, true) || p.id.toString() == q
        val statusOk = !onlyMissing || p.id !in captured
        queryOk && statusOk
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("Living Dex", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${captured.size} de $total capturados")
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                singleLine = true,
                label = { Text("Pesquisar Pokémon") }
            )

            AssistChip(
                onClick = { onlyMissing = !onlyMissing },
                label = { Text(if (onlyMissing) "Mostrando faltantes" else "Mostrar só faltantes") },
                leadingIcon = if (onlyMissing) ({ Text("✓") }) else null,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (loading) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { pokemon ->
                    val isCaptured = pokemon.id in captured
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onPokemonClick(pokemon.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(pokemon.spriteUrl, pokemon.name, Modifier.size(68.dp))
                            Column(Modifier.weight(1f)) {
                                Text("#${pokemon.id.toString().padStart(4, '0')}", style = MaterialTheme.typography.labelMedium)
                                Text(pokemon.name, fontWeight = FontWeight.SemiBold)
                                Text(if (isCaptured) "Capturado" else "Faltando")
                            }
                            Button(onClick = { CollectionStore.toggleCaptured(pokemon.id) }) {
                                Text(if (isCaptured) "✓" else "+")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun BoxesScreen(onPokemonClick: (Int) -> Unit) {
    var selectedBox by remember { mutableStateOf(CollectionStore.defaultBoxes.first()) }
    var idText by remember { mutableStateOf("") }
    val boxes = CollectionStore.boxes
    val ids = boxes[selectedBox].orEmpty().sorted()

    Column(Modifier.fillMaxSize()) {
        Text(
            "Boxes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CollectionStore.defaultBoxes.forEach { box ->
                AssistChip(
                    onClick = { selectedBox = box },
                    label = { Text(box) },
                    leadingIcon = if (selectedBox == box) ({ Text("✓") }) else null
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = idText,
                onValueChange = { idText = it.filter(Char::isDigit).take(4) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Nº National Dex") }
            )
            Button(
                onClick = {
                    idText.toIntOrNull()?.takeIf { it in 1..1025 }?.let { id ->
                        CollectionStore.addToBox(selectedBox, id)
                        idText = ""
                    }
                }
            ) { Text("Adicionar") }
        }

        Text(
            "$selectedBox • ${ids.size} Pokémon",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (ids.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Essa Box ainda está vazia.")
                Text("Adicione pelo número da National Dex.", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ids, key = { it }) { id ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png",
                                contentDescription = "Pokémon #$id",
                                modifier = Modifier.size(64.dp).clickable { onPokemonClick(id) }
                            )
                            Column(Modifier.weight(1f).clickable { onPokemonClick(id) }) {
                                Text("#${id.toString().padStart(4, '0')}", fontWeight = FontWeight.SemiBold)
                                Text("Marcado como capturado no Living Dex", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { CollectionStore.removeFromBox(selectedBox, id) }) {
                                Text("Remover")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
