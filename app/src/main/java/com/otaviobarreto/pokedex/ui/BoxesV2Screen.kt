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

private data class BoxGame(val label: String, val regions: List<BoxRegion>)
private data class BoxRegion(val label: String, val source: String, val badge: String)

private val boxGames = listOf(
    BoxGame("Scarlet / Violet", listOf(
        BoxRegion("Paldea", "Scarlet / Violet · Paldea", "Jogo base"),
        BoxRegion("Kitakami", "Scarlet / Violet · Kitakami", "DLC · The Teal Mask"),
        BoxRegion("Blueberry", "Scarlet / Violet · Blueberry", "DLC · The Indigo Disk")
    )),
    BoxGame("Sword / Shield", listOf(BoxRegion("Galar", "Sword / Shield · Galar", "Jogo base"))),
    BoxGame("Let's Go Pikachu / Eevee", listOf(BoxRegion("Kanto", "Let's Go Pikachu / Eevee · Kanto", "Jogo base"))),
    BoxGame("Legends Arceus", listOf(BoxRegion("Hisui", "Legends Arceus · Hisui", "Jogo base")))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxesV2Screen(onPokemonClick: (Int, String?) -> Unit) {
    var game by remember { mutableStateOf(boxGames.first()) }
    var region by remember { mutableStateOf(game.regions.first()) }
    var dex by remember { mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var page by remember { mutableIntStateOf(0) }
    var gameMenu by remember { mutableStateOf(false) }
    var regionMenu by remember { mutableStateOf(false) }
    var allBoxesOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    val captured = CollectionStore.capturedIds

    LaunchedEffect(region.source) {
        loading = true; page = 0
        val context = GameContext.fromSource(region.source)
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
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(expanded = gameMenu, onExpandedChange = { gameMenu = !gameMenu }, modifier = Modifier.weight(1.15f)) {
                OutlinedTextField(value = game.label, onValueChange = {}, readOnly = true, singleLine = true,
                    label = { Text("Jogo") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(gameMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = gameMenu, onDismissRequest = { gameMenu = false }) {
                    boxGames.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = {
                        game = option; region = option.regions.first(); gameMenu = false
                    }) }
                }
            }
            ExposedDropdownMenuBox(expanded = regionMenu, onExpandedChange = { regionMenu = !regionMenu }, modifier = Modifier.weight(.85f)) {
                OutlinedTextField(value = region.label, onValueChange = {}, readOnly = true, singleLine = true,
                    label = { Text(if (game.regions.size > 1) "DLC / região" else "Região") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(regionMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = regionMenu, onDismissRequest = { regionMenu = false }) {
                    game.regions.forEach { option -> DropdownMenuItem(text = {
                        Column { Text(option.label, fontWeight = FontWeight.SemiBold); Text(option.badge, style = MaterialTheme.typography.labelSmall) }
                    }, onClick = { region = option; regionMenu = false }) }
                }
            }
        }
        Text("${region.label} · ${region.badge}", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))

        Card(Modifier.fillMaxWidth().padding(top = 6.dp), shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), strokeWidth = 6.dp)
                    Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
                Column(Modifier.weight(1.2f).padding(start = 10.dp)) {
                    Text("$caughtCount / ${dex.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("capturados", style = MaterialTheme.typography.labelMedium)
                }
                BoxStat("${dex.size}", "Total", Modifier.weight(.8f))
                BoxStat("$caughtCount", "Capt.", Modifier.weight(.8f))
                BoxStat("$remaining", "Rest.", Modifier.weight(.8f))
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            FilledTonalIconButton(enabled = safePage > 0, onClick = { page = safePage - 1 }) { Icon(Icons.Default.ChevronLeft, "Box anterior") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Box ${safePage + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (dex.isEmpty()) "—" else "Pokémon ${firstNumber.toString().padStart(3,'0')} – ${lastNumber.toString().padStart(3,'0')}", style = MaterialTheme.typography.labelMedium)
            }
            FilledTonalIconButton(enabled = safePage < pageCount - 1, onClick = { page = safePage + 1 }) { Icon(Icons.Default.ChevronRight, "Próxima Box") }
        }

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            dex.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Não foi possível carregar esta Pokédex regional.") }
            else -> LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.weight(1f).fillMaxWidth(), userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(pageEntries, key = { it.gameNumber }) { pokemon -> BoxV2Slot(pokemon, pokemon.nationalId in captured) {
                    onPokemonClick(pokemon.nationalId, region.source)
                } }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = { allBoxesOpen = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.GridView,null); Spacer(Modifier.width(7.dp)); Text("Todas as Boxes") }
            FilledTonalButton(onClick = { searchOpen = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Search,null); Spacer(Modifier.width(7.dp)); Text("Pesquisar") }
        }
    }

    if (allBoxesOpen) ModalBottomSheet(onDismissRequest = { allBoxesOpen = false }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Todas as Boxes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${game.label} · ${region.label} · $pageCount Boxes", modifier = Modifier.padding(bottom = 10.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items((0 until pageCount).toList()) { p ->
                    val entries = dex.drop(p*30).take(30); val caught = entries.count { it.nationalId in captured }
                    Card(Modifier.fillMaxWidth().clickable { page=p; allBoxesOpen=false }) { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Box ${p+1}", fontWeight = FontWeight.SemiBold); Text("$caught/${entries.size} capturados")
                    } }
                }
            }; Spacer(Modifier.height(20.dp))
        }
    }

    if (searchOpen) BoxSearchDialog(dex, captured, { searchOpen=false }, { pokemon ->
        val index=dex.indexOfFirst { it.nationalId==pokemon.nationalId }; if(index>=0) page=index/30; searchOpen=false
    }, { pokemon -> searchOpen=false; onPokemonClick(pokemon.nationalId, region.source) })
}

@Composable private fun BoxStat(value:String,label:String,modifier:Modifier=Modifier){ Column(modifier,horizontalAlignment=Alignment.CenterHorizontally){ Text(value,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium); Text(label,style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.Center) } }

@Composable private fun BoxV2Slot(pokemon:GameDexService.GameDexEntry,captured:Boolean,onClick:()->Unit){ Card(modifier=Modifier.fillMaxWidth().aspectRatio(.82f).clickable(onClick=onClick),shape=RoundedCornerShape(12.dp),colors=CardDefaults.cardColors(containerColor=if(captured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)){ Box(Modifier.fillMaxSize().padding(1.dp)){ AsyncImage(model=pokemon.spriteUrl,contentDescription=pokemon.name,modifier=Modifier.align(Alignment.Center).fillMaxWidth(.94f).aspectRatio(1f).alpha(if(captured)1f else .18f),colorFilter=if(captured)null else ColorFilter.colorMatrix(ColorMatrix().apply{setToSaturation(0f)})); Text(pokemon.gameNumber.toString().padStart(3,'0'),modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth(),textAlign=TextAlign.Center,style=MaterialTheme.typography.labelSmall,color=if(captured)MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun BoxSearchDialog(dex:List<GameDexService.GameDexEntry>,captured:Set<Int>,onDismiss:()->Unit,onSelect:(GameDexService.GameDexEntry)->Unit,onOpenPokemon:(GameDexService.GameDexEntry)->Unit){ var query by remember{mutableStateOf("")}; val q=query.trim().removePrefix("#"); val results=remember(dex,query){if(q.isBlank()) emptyList() else dex.filter{it.name.contains(q,true)||it.gameNumber.toString()==q||it.nationalId.toString()==q}.take(12)}; AlertDialog(onDismissRequest=onDismiss,title={Text("Pesquisar Pokémon")},text={Column{OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Nome ou número")}); LazyColumn(Modifier.fillMaxWidth().heightIn(max=360.dp).padding(top=8.dp)){items(results,key={it.nationalId}){p->Row(Modifier.fillMaxWidth().clickable{onSelect(p)}.padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(p.spriteUrl,p.name,Modifier.size(48.dp));Column(Modifier.weight(1f).padding(start=8.dp)){Text(p.name,fontWeight=FontWeight.SemiBold);Text("#${p.gameNumber.toString().padStart(3,'0')} · ${if(p.nationalId in captured)"Capturado" else "Faltando"}",style=MaterialTheme.typography.labelSmall)};TextButton(onClick={onOpenPokemon(p)}){Text("Ficha")}}}}}},confirmButton={},dismissButton={TextButton(onClick=onDismiss){Text("Fechar")}}) }
