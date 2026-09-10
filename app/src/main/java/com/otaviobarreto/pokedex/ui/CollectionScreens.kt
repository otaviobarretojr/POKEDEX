package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val boxNames = CollectionStore.boxNames
    val boxes = CollectionStore.boxes
    var selectedBox by remember { mutableStateOf(boxNames.firstOrNull().orEmpty()) }
    var idText by remember { mutableStateOf("") }
    var createOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var movePokemonId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(boxNames) {
        if (selectedBox !in boxNames) selectedBox = boxNames.firstOrNull().orEmpty()
    }

    val ids = boxes[selectedBox].orEmpty().sorted()
    val slots = List(30) { index -> ids.getOrNull(index) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Boxes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Organize sua coleção como no Pokémon HOME", style = MaterialTheme.typography.bodyMedium)
            }
            FilledTonalIconButton(onClick = { createOpen = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nova Box")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            boxNames.forEach { box ->
                val count = boxes[box].orEmpty().size
                AssistChip(
                    onClick = { selectedBox = box },
                    label = { Text("$box · $count") },
                    leadingIcon = if (selectedBox == box) ({ Text("✓") }) else null
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedBox, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${ids.size}/30 slots ocupados", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { CollectionStore.moveBox(selectedBox, -1) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Mover Box para esquerda")
                    }
                    IconButton(onClick = { CollectionStore.moveBox(selectedBox, 1) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Mover Box para direita")
                    }
                    IconButton(onClick = { renameOpen = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Renomear Box")
                    }
                    IconButton(
                        enabled = boxNames.size > 1,
                        onClick = { deleteOpen = true }
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir Box")
                    }
                }
                LinearProgressIndicator(
                    progress = { (ids.size / 30f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
            FilledTonalIconButton(
                enabled = ids.size < 30,
                onClick = {
                    idText.toIntOrNull()?.takeIf { it in 1..1025 }?.let { id ->
                        if (CollectionStore.addToBox(selectedBox, id)) idText = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(slots) { index, pokemonId ->
                BoxSlot(
                    index = index,
                    pokemonId = pokemonId,
                    onPokemonClick = onPokemonClick,
                    onMove = { movePokemonId = it },
                    onRemove = { id -> CollectionStore.removeFromBox(selectedBox, id) }
                )
            }
        }
    }

    if (createOpen) {
        BoxNameDialog(
            title = "Nova Box",
            initialValue = "",
            confirmLabel = "Criar",
            onDismiss = { createOpen = false },
            onConfirm = { name ->
                if (CollectionStore.createBox(name)) {
                    selectedBox = CollectionStore.boxNames.last()
                    createOpen = false
                }
            }
        )
    }

    if (renameOpen) {
        BoxNameDialog(
            title = "Renomear Box",
            initialValue = selectedBox,
            confirmLabel = "Salvar",
            onDismiss = { renameOpen = false },
            onConfirm = { name ->
                val oldName = selectedBox
                if (CollectionStore.renameBox(oldName, name)) {
                    selectedBox = CollectionStore.boxNames.firstOrNull { it.equals(name.trim(), true) }
                        ?: CollectionStore.boxNames.firstOrNull().orEmpty()
                    renameOpen = false
                }
            }
        )
    }

    if (deleteOpen) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("Excluir $selectedBox?") },
            text = { Text("Os Pokémon sairão desta Box, mas continuarão marcados como capturados no Living Dex.") },
            confirmButton = {
                TextButton(onClick = {
                    CollectionStore.deleteBox(selectedBox)
                    selectedBox = CollectionStore.boxNames.firstOrNull().orEmpty()
                    deleteOpen = false
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deleteOpen = false }) { Text("Cancelar") } }
        )
    }

    movePokemonId?.let { pokemonId ->
        AlertDialog(
            onDismissRequest = { movePokemonId = null },
            title = { Text("Mover Pokémon #${pokemonId.toString().padStart(4, '0')}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    boxNames.filter { it != selectedBox }.forEach { target ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = boxes[target].orEmpty().size < 30,
                            onClick = {
                                CollectionStore.movePokemon(selectedBox, target, pokemonId)
                                movePokemonId = null
                            }
                        ) { Text(target) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { movePokemonId = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun BoxNameDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(32) },
                singleLine = true,
                label = { Text("Nome da Box") }
            )
        },
        confirmButton = {
            TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun BoxSlot(
    index: Int,
    pokemonId: Int?,
    onPokemonClick: (Int) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .then(if (pokemonId != null) Modifier.clickable { onPokemonClick(pokemonId) } else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = if (pokemonId == null) 0.dp else 1.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.Center) {
            if (pokemonId == null) {
                Text((index + 1).toString(), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AsyncImage(
                        model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$pokemonId.png",
                        contentDescription = "Pokémon #$pokemonId",
                        modifier = Modifier.size(50.dp)
                    )
                    Text("#${pokemonId.toString().padStart(4, '0')}", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Mover",
                    modifier = Modifier.align(Alignment.BottomStart).size(18.dp).clickable { onMove(pokemonId) }
                )
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remover",
                    modifier = Modifier.align(Alignment.TopEnd).size(18.dp).clickable { onRemove(pokemonId) }
                )
            }
        }
    }
}
