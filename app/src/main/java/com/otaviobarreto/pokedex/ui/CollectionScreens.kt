package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
        dex = runCatching { withContext(Dispatchers.IO) { PokeApiService.loadNationalDex() } }.getOrElse { emptyList() }
        loading = false
    }
    val captured = CollectionStore.capturedIds
    val filtered = dex.filter { p ->
        val q = query.trim().removePrefix("#")
        (q.isBlank() || p.name.contains(q, true) || p.id.toString() == q) && (!onlyMissing || p.id !in captured)
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("Living Dex", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${captured.size} de ${dex.size.coerceAtLeast(1025)} capturados")
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true, label = { Text("Pesquisar Pokémon") })
            AssistChip({ onlyMissing = !onlyMissing }, { Text(if (onlyMissing) "Mostrando faltantes" else "Mostrar só faltantes") }, leadingIcon = if (onlyMissing) ({ Text("✓") }) else null, modifier = Modifier.padding(top = 8.dp))
        }
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxSize().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(filtered, key = { it.id }) { p ->
                val caught = p.id in captured
                Card(Modifier.aspectRatio(.85f).clickable { onPokemonClick(p.id) }) {
                    Column(Modifier.fillMaxSize().padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        AsyncImage(p.spriteUrl, p.name, Modifier.size(64.dp).alpha(if (caught) 1f else .24f), colorFilter = if (caught) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }))
                        Text("#${p.id.toString().padStart(4, '0')}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private data class BoxGame(val label: String, val source: String)
private val boxGames = listOf(
    BoxGame("Scarlet / Violet", "Scarlet / Violet · Paldea"),
    BoxGame("Kitakami", "Scarlet / Violet · Kitakami"),
    BoxGame("Blueberry", "Scarlet / Violet · Blueberry"),
    BoxGame("Sword / Shield", "Sword / Shield · Galar"),
    BoxGame("Let's Go", "Let's Go Pikachu / Eevee · Kanto"),
    BoxGame("Legends Arceus", "Legends Arceus · Hisui")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxesScreen(onPokemonClick: (Int, String?) -> Unit) {
    var game by remember { mutableStateOf(boxGames.first()) }
    var dex by remember { mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var gameMenu by remember { mutableStateOf(false) }
    var allBoxesOpen by remember { mutableStateOf(false) }
    val captured = CollectionStore.capturedIds

    LaunchedEffect(game.source) {
        loading = true
        val context = GameContext.fromSource(game.source)
        dex = if (context == null) emptyList() else runCatching { withContext(Dispatchers.IO) { GameDexService.loadGameDex(context) } }.getOrElse { emptyList() }
        loading = false
    }
    val q = query.trim().removePrefix("#")
    val visible = if (q.isBlank()) dex else dex.filter { it.name.contains(q, true) || it.gameNumber.toString() == q || it.nationalId.toString() == q }
    val pageCount = (dex.size + 29) / 30

    Column(Modifier.fillMaxSize()) {
        ExposedDropdownMenuBox(expanded = gameMenu, onExpandedChange = { gameMenu = !gameMenu }, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            OutlinedTextField(game.label, {}, readOnly = true, label = { Text("Pokédex / jogo") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(gameMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(gameMenu, { gameMenu = false }) { boxGames.forEach { option -> DropdownMenuItem({ Text(option.label) }, { game = option; query = ""; gameMenu = false }) } }
        }

        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(game.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${captured.count { id -> dex.any { it.nationalId == id } }} / ${dex.size} capturados · $pageCount Boxes", style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 8.dp), leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("Pesquisar Pokémon") }, singleLine = true)
            }
        }

        if (loading) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (dex.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Não foi possível carregar esta Pokédex regional.") }
        else LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(visible, key = { it.gameNumber }) { pokemon ->
                RegionalDexSlot(pokemon, pokemon.nationalId in captured) { onPokemonClick(pokemon.nationalId, game.source) }
            }
        }

        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton({ allBoxesOpen = true }, Modifier.weight(1f)) { Icon(Icons.Default.GridView, null); Spacer(Modifier.width(8.dp)); Text("Todas as Boxes") }
            FilledTonalButton({}, Modifier.weight(1f)) { Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("Pesquisar") }
        }
    }

    if (allBoxesOpen) ModalBottomSheet(onDismissRequest = { allBoxesOpen = false }) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Todas as Boxes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${game.label} · $pageCount páginas de até 30 posições", modifier = Modifier.padding(bottom = 12.dp))
            for (page in 0 until pageCount) {
                val start = page * 30
                val pageEntries = dex.drop(start).take(30)
                val caught = pageEntries.count { it.nationalId in captured }
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { allBoxesOpen = false }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Box ${page + 1}", fontWeight = FontWeight.SemiBold)
                        Text("${start + 1}–${(start + pageEntries.size)} · $caught/${pageEntries.size}")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RegionalDexSlot(pokemon: GameDexService.GameDexEntry, captured: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(.82f).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (captured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(Modifier.fillMaxSize().padding(2.dp)) {
            AsyncImage(
                model = pokemon.spriteUrl,
                contentDescription = pokemon.name,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(.92f).aspectRatio(1f).alpha(if (captured) 1f else .20f),
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
