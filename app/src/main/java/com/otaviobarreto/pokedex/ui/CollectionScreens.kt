package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import com.otaviobarreto.pokedex.data.PokedexDataStore
import com.otaviobarreto.pokedex.data.PokeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private data class LivingDexScope(val label: String, val source: String?)
private enum class LivingCollectionFilter { ALL, OWNED, BOXED, MISSING, UNBOXED, DUPLICATES }
private val livingDexScopes = listOf(LivingDexScope("Nacional", null)) +
    AppGameCatalog.games.flatMap { game -> game.regions.map { LivingDexScope("${game.label} · ${it.label}", it.source) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivingDexScreen(onPokemonClick: (Int) -> Unit) {
    var national by remember { mutableStateOf(PokedexDataStore.cachedNationalDex().orEmpty()) }
    var regional by remember { mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(national.isEmpty()) }
    var query by remember { mutableStateOf("") }
    var collectionFilter by remember { mutableStateOf(LivingCollectionFilter.ALL) }
    var generation by remember { mutableIntStateOf(0) }
    var scope by remember { mutableStateOf(livingDexScopes.first()) }
    var scopeMenu by remember { mutableStateOf(false) }
    val captured = CollectionStore.capturedIds
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        if (national.isEmpty()) loading = true
        val loaded = runCatching { withContext(Dispatchers.IO) { PokedexDataStore.nationalDex() } }.getOrNull()
        if (loaded != null) national = loaded
        loading = false
    }

    LaunchedEffect(scope.source) {
        val source = scope.source ?: run { regional = emptyList(); return@LaunchedEffect }
        val context = GameContext.fromSource(source) ?: return@LaunchedEffect
        loading = true
        regional = runCatching { withContext(Dispatchers.IO) { GameDexService.loadGameDex(context) } }.getOrElse { emptyList() }
        loading = false
    }

    val regionalIds = remember(regional) { regional.map { it.nationalId }.toSet() }
    val regionalNumbers = remember(regional) { regional.associate { it.nationalId to it.gameNumber } }
    val scoped = remember(national, regionalIds, scope.source) {
        if (scope.source == null) national else national.filter { it.id in regionalIds }
    }
    val filtered = remember(scoped, query, collectionFilter, generation, captured, CollectionStore.boxes) {
        val q = query.trim().removePrefix("#")
        scoped.filter { p ->
            val queryOk = q.isBlank() || p.name.contains(q, true) || p.id.toString() == q || regionalNumbers[p.id]?.toString() == q
            val generationOk = generation == 0 || p.generation == generation
            val collectionOk = when (collectionFilter) {
                LivingCollectionFilter.ALL -> true
                LivingCollectionFilter.OWNED -> p.id in captured
                LivingCollectionFilter.BOXED -> p.id in captured && CollectionStore.boxesForPokemon(p.id).isNotEmpty()
                LivingCollectionFilter.MISSING -> p.id !in captured
                LivingCollectionFilter.UNBOXED -> p.id in captured && CollectionStore.boxesForPokemon(p.id).isEmpty()
                LivingCollectionFilter.DUPLICATES -> p.id in CollectionStore.duplicateIds()
            }
            queryOk && generationOk && collectionOk
        }
    }
    val caughtInScope = scoped.count { it.id in captured }
    val boxedInScope = scoped.count { CollectionStore.boxesForPokemon(it.id).isNotEmpty() }
    val unboxedInScope = (caughtInScope - boxedInScope).coerceAtLeast(0)
    val total = scoped.size
    val progress = if (total == 0) 0f else caughtInScope.toFloat() / total
    val nextMissing = remember(scoped, captured, generation) {
        scoped.firstOrNull { it.id !in captured && (generation == 0 || it.generation == generation) }
    }
    val generationCounts = remember(scoped, captured) {
        (1..9).associateWith { gen ->
            val genItems = scoped.filter { it.generation == gen }
            genItems.count { it.id in captured } to genItems.size
        }
    }

    LaunchedEffect(gridState, filtered) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.map { it.index } }
            .map { indices -> indices.mapNotNull { filtered.getOrNull(it)?.id }.take(12) }
            .distinctUntilChanged()
            .collect { ids -> ids.forEach { id -> PokedexDataStore.prefetchDetails(id) } }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8F8FC))) {
        if (loading && national.isNotEmpty()) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF5B55E7), trackColor = Color.Transparent)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("LIVING DEX", fontSize = 27.sp, fontWeight = FontWeight.Black, color = Color(0xFF151426))
                    Text("S U A  C O L E Ç Ã O  ·  P O R  J O G O  E  R E G I Ã O", fontSize = 7.sp, color = Color(0xFF72778B))
                }
            }

            Card(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F0F8))
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), strokeWidth = 6.dp, color = Color(0xFF5B55E7), trackColor = Color(0xFF5B55E7).copy(alpha=.12f))
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("$caughtInScope / $total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("$boxedInScope organizados em Box", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5B55E7))
                        Text(if (scope.source == null) "Pokédex Nacional" else scope.label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${(total - caughtInScope).coerceAtLeast(0)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("restantes", style = MaterialTheme.typography.labelSmall)
                        if (unboxedInScope > 0) Text("$unboxedInScope sem Box", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB26A00))
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = scopeMenu, onExpandedChange = { scopeMenu = !scopeMenu }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                OutlinedTextField(
                    value = scope.label,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Visualização") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(scopeMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )
                ExposedDropdownMenu(expanded = scopeMenu, onDismissRequest = { scopeMenu = false }) {
                    livingDexScopes.forEach { item ->
                        DropdownMenuItem(text = { Text(item.label) }, onClick = {
                            scope = item
                            scopeMenu = false
                            query = ""
                        })
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Nome, Nº Nacional ou Nº regional") },
                shape = RoundedCornerShape(18.dp)
            )

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FilterAlt, null, modifier = Modifier.size(20.dp))
                listOf(
                    LivingCollectionFilter.ALL to "Todos",
                    LivingCollectionFilter.OWNED to "Capturados",
                    LivingCollectionFilter.BOXED to "Na Box",
                    LivingCollectionFilter.MISSING to "Faltantes",
                    LivingCollectionFilter.UNBOXED to "Sem Box",
                    LivingCollectionFilter.DUPLICATES to "Duplicados"
                ).forEach { (filter, label) ->
                    AssistChip(onClick = { collectionFilter = filter }, label = { Text(label) }, leadingIcon = if (collectionFilter == filter) ({ Text("✓") }) else null)
                }
                (0..9).forEach { gen ->
                    val genLabel = if (gen == 0) "Todas Gerações" else {
                        val counts = generationCounts[gen] ?: (0 to 0)
                        "G$gen ${counts.first}/${counts.second}"
                    }
                    AssistChip(onClick = { generation = gen }, label = { Text(genLabel) }, leadingIcon = if (generation == gen) ({ Text("✓") }) else null)
                }
            }
            if (nextMissing != null) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onPokemonClick(nextMissing.id) }) {
                        Text("Próximo faltante · #${nextMissing.id.toString().padStart(4, '0')} ${nextMissing.name}")
                    }
                }
            }
        }

        if (loading && national.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Nenhum Pokémon corresponde aos filtros selecionados.", textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.id }, contentType = { "pokemon" }) { p ->
                    val caught = p.id in captured
                    Card(
                        Modifier.fillMaxWidth().aspectRatio(.78f).clickable { onPokemonClick(p.id) },
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = if (caught) Color(0xFFEAE8FB) else Color(0xFFF1F0F8))
                    ) {
                        Column(Modifier.fillMaxSize().padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = p.spriteUrl,
                                contentDescription = p.name,
                                modifier = Modifier.weight(1f).fillMaxWidth(.90f).padding(3.dp).alpha(if (caught) 1f else .24f),
                                contentScale = ContentScale.Fit,
                                colorFilter = if (caught) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                            )
                            Text(p.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (caught && CollectionStore.boxesForPokemon(p.id).isNotEmpty()) "${CollectionStore.boxesForPokemon(p.id).size} Box · #${p.id.toString().padStart(4, '0')}"
                                else regionalNumbers[p.id]?.let { "#${it.toString().padStart(3, '0')} regional · #${p.id.toString().padStart(4, '0')}" }
                                    ?: "#${p.id.toString().padStart(4, '0')}",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}
