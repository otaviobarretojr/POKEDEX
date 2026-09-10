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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.TeamStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TeamBuilderScreen(onPokemonClick: (Int) -> Unit) {
    val teams = TeamStore.teams
    var selectedTeamId by remember { mutableStateOf(teams.firstOrNull()?.id) }
    var query by remember { mutableStateOf("") }
    var dex by remember { mutableStateOf<List<PokeApiService.DexIndexEntry>>(emptyList()) }
    var showCreate by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var replacePokemonId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        dex = runCatching { withContext(Dispatchers.IO) { PokeApiService.loadNationalDex() } }
            .getOrElse { emptyList() }
    }

    LaunchedEffect(teams) {
        if (teams.none { it.id == selectedTeamId }) selectedTeamId = teams.firstOrNull()?.id
    }

    val selectedTeam = teams.firstOrNull { it.id == selectedTeamId } ?: teams.firstOrNull()
    val normalized = query.trim().removePrefix("#")
    val suggestions = if (normalized.isBlank()) emptyList() else dex.filter { p ->
        p.name.contains(normalized, ignoreCase = true) || p.id.toString() == normalized
    }.take(6)

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Team Builder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Monte e salve seus times de até 6 Pokémon", style = MaterialTheme.typography.bodyMedium)
            }
            FilledTonalIconButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Novo time")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            teams.forEach { team ->
                AssistChip(
                    onClick = { selectedTeamId = team.id },
                    label = { Text("${team.name} · ${team.members.size}/6") },
                    leadingIcon = if (team.id == selectedTeamId) ({ Text("✓") }) else null
                )
            }
        }

        selectedTeam?.let { team ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(team.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${team.members.size} de 6 integrantes", style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { showRename = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Renomear time")
                        }
                        IconButton(onClick = { showDelete = true }, enabled = teams.size > 1) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir time")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                label = { Text(if (replacePokemonId == null) "Adicionar por nome ou número" else "Escolher substituto") }
            )

            if (suggestions.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    suggestions.forEach { pokemon ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable {
                                val replaced = replacePokemonId
                                if (replaced == null) TeamStore.addPokemon(team.id, pokemon.id)
                                else TeamStore.replacePokemon(team.id, replaced, pokemon.id)
                                replacePokemonId = null
                                query = ""
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(pokemon.spriteUrl, pokemon.name, Modifier.size(44.dp))
                                Column(Modifier.padding(start = 8.dp)) {
                                    Text(pokemon.name, fontWeight = FontWeight.SemiBold)
                                    Text("#${pokemon.id.toString().padStart(4, '0')}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            val slots = List(6) { index -> team.members.getOrNull(index) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(slots) { index, pokemonId ->
                    TeamSlot(
                        index = index,
                        pokemonId = pokemonId,
                        onOpen = onPokemonClick,
                        onRemove = { id -> TeamStore.removePokemon(team.id, id) },
                        onReplace = { id ->
                            replacePokemonId = id
                            query = ""
                        },
                        onMoveLeft = { id -> TeamStore.moveMember(team.id, id, -1) },
                        onMoveRight = { id -> TeamStore.moveMember(team.id, id, 1) }
                    )
                }
            }
        }
    }

    if (showCreate) {
        TeamNameDialog(
            title = "Novo time",
            initial = "",
            confirmLabel = "Criar",
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                TeamStore.createTeam(name)?.let { selectedTeamId = it }
                showCreate = false
            }
        )
    }

    if (showRename && selectedTeam != null) {
        TeamNameDialog(
            title = "Renomear time",
            initial = selectedTeam.name,
            confirmLabel = "Salvar",
            onDismiss = { showRename = false },
            onConfirm = { name ->
                TeamStore.renameTeam(selectedTeam.id, name)
                showRename = false
            }
        )
    }

    if (showDelete && selectedTeam != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Excluir ${selectedTeam.name}?") },
            text = { Text("O time será apagado, mas nenhum Pokémon será removido das suas Boxes ou do Living Dex.") },
            confirmButton = {
                TextButton(onClick = {
                    TeamStore.deleteTeam(selectedTeam.id)
                    showDelete = false
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TeamSlot(
    index: Int,
    pokemonId: Int?,
    onOpen: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onReplace: (Int) -> Unit,
    onMoveLeft: (Int) -> Unit,
    onMoveRight: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.86f),
        elevation = CardDefaults.cardElevation(defaultElevation = if (pokemonId == null) 0.dp else 2.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
            if (pokemonId == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Slot ${index + 1}", style = MaterialTheme.typography.labelMedium)
                    Text("Vazio", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AsyncImage(
                        model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$pokemonId.png",
                        contentDescription = "Pokémon #$pokemonId",
                        modifier = Modifier.size(92.dp).clickable { onOpen(pokemonId) }
                    )
                    Text("#${pokemonId.toString().padStart(4, '0')}", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.Center) {
                        IconButton(onClick = { onMoveLeft(pokemonId) }, enabled = index > 0) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Mover para esquerda")
                        }
                        IconButton(onClick = { onReplace(pokemonId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Trocar Pokémon")
                        }
                        IconButton(onClick = { onMoveRight(pokemonId) }, enabled = index < 5) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Mover para direita")
                        }
                    }
                    OutlinedButton(onClick = { onRemove(pokemonId) }) { Text("Remover") }
                }
            }
        }
    }
}

@Composable
private fun TeamNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(32) },
                singleLine = true,
                label = { Text("Nome do time") }
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }, enabled = text.trim().isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
