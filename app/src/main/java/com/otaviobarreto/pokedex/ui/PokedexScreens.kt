package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.PokeApiService
import com.otaviobarreto.pokedex.data.PokemonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val pokemonTypes = listOf(
    "Normal", "Fire", "Water", "Electric", "Grass", "Ice", "Fighting", "Poison", "Ground",
    "Flying", "Psychic", "Bug", "Rock", "Ghost", "Dragon", "Dark", "Steel", "Fairy"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexScreen(onPokemonClick: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    var generation by remember { mutableIntStateOf(0) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var dex by remember { mutableStateOf<List<PokeApiService.DexIndexEntry>>(emptyList()) }
    var typeIds by remember { mutableStateOf<Set<Int>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingType by remember { mutableStateOf(false) }
    var sourceMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        dex = runCatching {
            withContext(Dispatchers.IO) { PokeApiService.loadNationalDex() }
        }.getOrElse {
            sourceMessage = "Sem conexão: exibindo catálogo local disponível."
            PokemonRepository.all().map {
                PokeApiService.DexIndexEntry(it.id, it.name, it.generation)
            }
        }
        loading = false
    }

    LaunchedEffect(selectedType) {
        val type = selectedType
        if (type == null) {
            typeIds = null
            loadingType = false
        } else {
            loadingType = true
            typeIds = runCatching {
                withContext(Dispatchers.IO) { PokeApiService.loadPokemonIdsForType(type) }
            }.getOrElse {
                PokemonRepository.all().filter { pokemon ->
                    pokemon.types.any { it.equals(type, ignoreCase = true) }
                }.map { it.id }.toSet()
            }
            loadingType = false
        }
    }

    val filtered = remember(dex, query, generation, typeIds, selectedType) {
        val normalized = query.trim().removePrefix("#")
        dex.filter { pokemon ->
            val queryOk = normalized.isBlank() ||
                pokemon.name.contains(normalized, ignoreCase = true) ||
                pokemon.id.toString() == normalized
            val generationOk = generation == 0 || pokemon.generation == generation
            val typeOk = selectedType == null || pokemon.id in (typeIds ?: emptySet())
            queryOk && generationOk && typeOk
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Nome ou número do Pokémon") },
            label = { Text("Pesquisar") }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilterList, contentDescription = null)
            GenerationChips(selected = generation, onSelect = { generation = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { selectedType = null },
                label = { Text("Todos os tipos") },
                leadingIcon = if (selectedType == null) ({ Text("✓") }) else null
            )
            pokemonTypes.forEach { type ->
                AssistChip(
                    onClick = { selectedType = type },
                    label = { Text(type) },
                    leadingIcon = if (selectedType == type) ({ Text("✓") }) else null
                )
            }
        }

        if (loading || loadingType) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        sourceMessage?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        Text(
            text = if (loading) "Carregando Pokédex Nacional…" else "${filtered.size} Pokémon encontrados",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered, key = { it.id }) { pokemon ->
                PokemonIndexRow(pokemon = pokemon, onClick = { onPokemonClick(pokemon.id) })
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun GenerationChips(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (0..9).forEach { gen ->
            AssistChip(
                onClick = { onSelect(gen) },
                label = { Text(if (gen == 0) "Todas" else "G$gen") },
                leadingIcon = if (selected == gen) ({ Text("✓") }) else null
            )
        }
    }
}

@Composable
private fun PokemonIndexRow(pokemon: PokeApiService.DexIndexEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = pokemon.spriteUrl,
                contentDescription = pokemon.name,
                modifier = Modifier.size(82.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("#${pokemon.id.toString().padStart(4, '0')}", style = MaterialTheme.typography.labelMedium)
                Text(pokemon.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Geração ${pokemon.generation}", style = MaterialTheme.typography.bodyMedium)
            }
            Text("›", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

private data class DetailBundle(
    val pokemon: PokeApiService.RemotePokemonDetail,
    val species: PokeApiService.SpeciesInfo,
    val evolutions: List<PokeApiService.EvolutionStage>,
    val encounters: List<PokeApiService.EncounterLocation>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(id: Int, source: String? = null, onBack: () -> Unit) {
    var bundle by remember(id) { mutableStateOf<DetailBundle?>(null) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var selectedTab by remember(id) { mutableIntStateOf(0) }
    val gameContext = remember(source) { GameContext.fromSource(source) }

    LaunchedEffect(id, reload) {
        error = null
        bundle = null
        runCatching {
            withContext(Dispatchers.IO) {
                val pokemon = PokeApiService.loadPokemon(id)
                val species = PokeApiService.loadSpecies(id)
                val evolution = species.evolutionChainUrl?.let { PokeApiService.loadEvolutionChain(it) } ?: emptyList()
                val encounters = PokeApiService.loadEncounters(id)
                DetailBundle(pokemon, species, evolution, encounters)
            }
        }.onSuccess { bundle = it }
            .onFailure { error = "Não foi possível carregar os dados completos deste Pokémon." }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bundle?.pokemon?.name ?: "Pokémon #$id") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            bundle != null -> PokemonDetailContent(
                bundle = bundle!!,
                selectedTab = selectedTab,
                gameContext = gameContext,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.padding(innerPadding)
            )
            error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(error!!, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { reload++ }) { Text("Tentar novamente") }
            }
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Carregando dados completos…")
            }
        }
    }
}

@Composable
private fun PokemonDetailContent(
    bundle: DetailBundle,
    selectedTab: Int,
    gameContext: GameContext?,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("Info", "Stats", "Evolução", "Golpes", "Localização")
    Column(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = bundle.pokemon.spriteUrl,
            contentDescription = bundle.pokemon.name,
            modifier = Modifier.size(190.dp).align(Alignment.CenterHorizontally)
        )
        Text(
            "#${bundle.pokemon.id.toString().padStart(4, '0')}  •  ${bundle.pokemon.types.joinToString(" / ")}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp)
        )
        gameContext?.let {
            Text(
                "Contexto: ${it.label}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
            )
        }
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { onTabSelected(index) }, text = { Text(title) })
            }
        }
        when (selectedTab) {
            0 -> InfoTab(bundle)
            1 -> StatsTab(bundle.pokemon.stats)
            2 -> EvolutionTab(bundle.evolutions)
            3 -> MovesTab(bundle.pokemon.moves)
            else -> LocationTab(bundle.encounters, gameContext)
        }
    }
}

@Composable
private fun InfoTab(bundle: DetailBundle) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            SectionTitle("Informações")
            InfoLine("Altura", String.format("%.1f m", bundle.pokemon.heightDecimeters / 10.0))
            InfoLine("Peso", String.format("%.1f kg", bundle.pokemon.weightHectograms / 10.0))
            InfoLine("Taxa de captura", bundle.species.captureRate.toString())
            InfoLine("Felicidade base", bundle.species.baseHappiness.toString())
            InfoLine("Habitat", bundle.species.habitat ?: "—")
            InfoLine("Crescimento", bundle.species.growthRate ?: "—")
            InfoLine("Grupos de ovo", bundle.species.eggGroups.joinToString().ifBlank { "—" })
            Spacer(Modifier.height(14.dp))
            SectionTitle("Habilidades")
            bundle.pokemon.abilities.forEach { Text("• $it", modifier = Modifier.padding(vertical = 4.dp)) }
            bundle.species.flavorText?.let {
                Spacer(Modifier.height(14.dp))
                SectionTitle("Descrição")
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun StatsTab(stats: com.otaviobarreto.pokedex.data.PokemonStats) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            SectionTitle("Base Stats")
            StatLine("HP", stats.hp)
            StatLine("Ataque", stats.attack)
            StatLine("Defesa", stats.defense)
            StatLine("Ataque Especial", stats.specialAttack)
            StatLine("Defesa Especial", stats.specialDefense)
            StatLine("Velocidade", stats.speed)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            StatLine("Total", stats.hp + stats.attack + stats.defense + stats.specialAttack + stats.specialDefense + stats.speed)
        }
    }
}

@Composable
private fun EvolutionTab(evolutions: List<PokeApiService.EvolutionStage>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionTitle("Linha evolutiva") }
        if (evolutions.isEmpty()) {
            item { Text("Nenhuma evolução encontrada.") }
        } else {
            items(evolutions, key = { it.pokemonId }) { stage ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${stage.pokemonId}.png",
                            contentDescription = stage.name,
                            modifier = Modifier.size(72.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stage.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(stage.requirement ?: "Forma inicial", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun MovesTab(moves: List<PokeApiService.RemoteMove>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SectionTitle("Golpes")
            Text("${moves.size} golpes disponíveis", style = MaterialTheme.typography.labelLarge)
        }
        items(moves, key = { it.name }) { move ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(move.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(move.methods.joinToString(" • ").ifBlank { "Método não informado" }, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun LocationTab(
    encounters: List<PokeApiService.EncounterLocation>,
    gameContext: GameContext?
) {
    val filtered = remember(encounters, gameContext) {
        if (gameContext == null) {
            encounters
        } else {
            encounters.mapNotNull { encounter ->
                val versions = encounter.versions.filter(gameContext::matchesVersion)
                if (versions.isEmpty()) null else encounter.copy(versions = versions)
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SectionTitle(
                if (gameContext == null) "Onde encontrar"
                else "Onde encontrar em ${gameContext.label}"
            )
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    if (gameContext == null) {
                        "Não há encontros selvagens registrados para este Pokémon."
                    } else {
                        "A fonte atual não possui localização específica deste Pokémon para ${gameContext.label}."
                    }
                )
            }
        } else {
            items(filtered, key = { it.location }) { encounter ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(encounter.location, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(encounter.versions.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatLine(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value.toString(), fontWeight = FontWeight.SemiBold)
    }
}
