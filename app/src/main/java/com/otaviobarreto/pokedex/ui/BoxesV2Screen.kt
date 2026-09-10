package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class BoxV2Game(val label: String, val source: String)

private val boxV2Games = listOf(
    BoxV2Game("Scarlet / Violet", "Scarlet / Violet · Paldea"),
    BoxV2Game("Kitakami", "Scarlet / Violet · Kitakami"),
    BoxV2Game("Blueberry", "Scarlet / Violet · Blueberry"),
    BoxV2Game("Sword / Shield", "Sword / Shield · Galar"),
    BoxV2Game("Let's Go", "Let's Go Pikachu / Eevee · Kanto"),
    BoxV2Game("Legends Arceus", "Legends Arceus · Hisui")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxesV2Screen(onPokemonClick: (Int, String?) -> Unit) {
    var game by remember { mutableStateOf(boxV2Games.first()) }
    var dex by remember { mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var page by remember { mutableIntStateOf(0) }
    var gameMenu by remember { mutableStateOf(false) }
    var allBoxesOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    val captured = CollectionStore.capturedIds

    LaunchedEffect(game.source) {
        loading = true
        page = 0
        val context = GameContext.fromSource(game.source)
        dex = if (context == null) emptyList() else runCatching {
            withContext(Dispatchers.IO) { GameDexService.loadGameDex(context) }
        }.getOrElse { emptyList() }
        loading = false
    }

    val pageCount = ((dex.size + 29) / 30).coerceAtLeast(1)
    val safePage = page.coerceIn(0, pageCount - 1)
    val pageEntries = dex.drop(safePage * 30).take(30)
    val caughtCount = dex.count { it.nationalId in captured }
    val remaining = (dex.size - caughtCount).coerceAtLeast(0)
    val progress = if (dex.isEmpty()) 0f else caughtCount.toFloat() / dex.size
    val firstNumber = pageEntries.firstOrNull()?.gameNumber ?: (safePage * 30 + 1)
    val lastNumber = pageEntries.lastOrNull()?.gameNumber ?: ((safePage + 1) * 30).coerceAtMost(dex.size)

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        ExposedDropdownMenuBox(
            expanded = gameMenu,
            onExpandedChange = { gameMenu = !gameMenu },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        ) {
            OutlinedTextField(
                value = game.label,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("Pokédex / jogo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(gameMenu) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = gameMenu, onDismissRequest = { gameMenu = false }) {
                boxV2Games.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = { game = option; gameMenu = false }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Progresso", style = MaterialTheme.typography.labelMedium)
                    }
                    BoxStat("${dex.size}", "Total", Modifier.weight(1f))
                    BoxStat("$caughtCount", "Capturados", Modifier.weight(1f))
                    BoxStat("$remaining", "Restantes", Modifier.weight(1f))
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).clip(RoundedCornerShape(99.dp))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledTonalIconButton(
                enabled = safePage > 0,
                onClick = { page = (safePage - 1).coerceAtLeast(0) }
            ) { Icon(Icons.Default.ChevronLeft, "Box anterior") }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Box ${safePage + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (dex.isEmpty()) "—" else "Pokémon ${firstNumber.toString().padStart(3, '0')} – ${lastNumber.toString().padStart(3, '0')}",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            FilledTonalIconButton(
                enabled = safePage < pageCount - 1,
                onClick = { page = (safePage + 1).coerceAtMost(pageCount - 1) }
            ) { Icon(Icons.Default.ChevronRight, "Próxima Box") }
        }

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            dex.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Não foi possível carregar esta Pokédex regional.") }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(pageEntries, key = { it.gameNumber }) { pokemon ->
                    BoxV2Slot(
                        pokemon = pokemon,
                        captured = pokemon.nationalId in captured,
                        onClick = { onPokemonClick(pokemon.nationalId, game.source) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(onClick = { allBoxesOpen = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.GridView, null)
                Spacer(Modifier.width(7.dp))
                Text("Todas as Boxes")
            }
            FilledTonalButton(onClick = { searchOpen = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Search, null)
                Spacer(Modifier.width(7.dp))
                Text("Pesquisar")
            }
        }
    }

    if (allBoxesOpen) {
        ModalBottomSheet(onDismissRequest = { allBoxesOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Todas as Boxes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${game.label} · $pageCount Boxes", modifier = Modifier.padding(bottom = 10.dp))
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items((0 until pageCount).toList()) { p ->
                        val entries = dex.drop(p * 30).take(30)
                        val caught = entries.count { it.nationalId in captured }
                        Card(Modifier.fillMaxWidth().clickable { page = p; allBoxesOpen = false }) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Box ${p + 1}", fontWeight = FontWeight.SemiBold)
                                Text("$caught/${entries.size} capturados")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (searchOpen) {
        BoxSearchDialog(
            dex = dex,
            captured = captured,
            onDismiss = { searchOpen = false },
            onSelect = { pokemon ->
                val index = dex.indexOfFirst { it.nationalId == pokemon.nationalId }
                if (index >= 0) page = index / 30
                searchOpen = false
            },
            onOpenPokemon = { pokemon ->
                searchOpen = false
                onPokemonClick(pokemon.nationalId, game.source)
            }
        )
    }
}

@Composable
private fun BoxStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BoxV2Slot(
    pokemon: GameDexService.GameDexEntry,
    captured: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(.82f).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (captured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(Modifier.fillMaxSize().padding(1.dp)) {
            AsyncImage(
                model = pokemon.spriteUrl,
                contentDescription = pokemon.name,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(.94f).aspectRatio(1f).alpha(if (captured) 1f else .18f),
                colorFilter = if (captured) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            )
            Text(
                pokemon.gameNumber.toString().padStart(3, '0'),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = if (captured) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BoxSearchDialog(
    dex: List<GameDexService.GameDexEntry>,
    captured: Set<Int>,
    onDismiss: () -> Unit,
    onSelect: (GameDexService.GameDexEntry) -> Unit,
    onOpenPokemon: (GameDexService.GameDexEntry) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().removePrefix("#")
    val results = remember(dex, query) {
        if (q.isBlank()) emptyList() else dex.filter {
            it.name.contains(q, true) || it.gameNumber.toString() == q || it.nationalId.toString() == q
        }.take(12)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pesquisar Pokémon") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Nome ou número") }
                )
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp).padding(top = 8.dp)) {
                    items(results, key = { it.nationalId }) { p ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onSelect(p) }.padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(p.spriteUrl, p.name, Modifier.size(48.dp))
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(p.name, fontWeight = FontWeight.SemiBold)
                                Text("#${p.gameNumber.toString().padStart(3, '0')} · ${if (p.nationalId in captured) "Capturado" else "Faltando"}", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { onOpenPokemon(p) }) { Text("Ficha") }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}
