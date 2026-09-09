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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.otaviobarreto.pokedex.data.PokemonFilter
import com.otaviobarreto.pokedex.data.PokemonRepository
import com.otaviobarreto.pokedex.data.PokemonSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexScreen(onPokemonClick: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    var generation by remember { mutableIntStateOf(0) }
    var selectedType by remember { mutableStateOf<String?>(null) }

    val filtered = remember(query, generation, selectedType) {
        PokemonRepository.search(
            PokemonFilter(
                query = query,
                generation = generation.takeIf { it != 0 },
                type = selectedType
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Nome ou número do Pokémon") },
            label = { Text("Pesquisar") }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilterList, contentDescription = null)
            GenerationChips(selected = generation, onSelect = { generation = it })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { selectedType = null },
                label = { Text("Todos os tipos") },
                leadingIcon = if (selectedType == null) ({ Text("✓") }) else null
            )
            PokemonRepository.types().forEach { type ->
                AssistChip(
                    onClick = { selectedType = type },
                    label = { Text(type) },
                    leadingIcon = if (selectedType == type) ({ Text("✓") }) else null
                )
            }
        }

        Text(
            text = "${filtered.size} Pokémon encontrados",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { pokemon ->
                PokemonRow(pokemon = pokemon, onClick = { onPokemonClick(pokemon.id) })
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
        (listOf(0) + PokemonRepository.generations()).forEach { gen ->
            AssistChip(
                onClick = { onSelect(gen) },
                label = { Text(if (gen == 0) "Todas" else "G$gen") },
                leadingIcon = if (selected == gen) ({ Text("✓") }) else null
            )
        }
    }
}

@Composable
private fun PokemonRow(pokemon: PokemonSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = pokemon.spriteUrl,
                contentDescription = pokemon.name,
                modifier = Modifier.size(82.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${pokemon.id.toString().padStart(4, '0')}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = pokemon.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = pokemon.types.joinToString("  •  "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text("›", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(id: Int, onBack: () -> Unit) {
    val pokemon = PokemonRepository.byId(id)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pokemon?.name ?: "Pokémon") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (pokemon == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { Text("Pokémon não encontrado") }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    AsyncImage(
                        model = pokemon.spriteUrl,
                        contentDescription = pokemon.name,
                        modifier = Modifier.size(240.dp)
                    )
                    Text(
                        "#${pokemon.id.toString().padStart(4, '0')} • Geração ${pokemon.generation}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        pokemon.types.joinToString("  •  "),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    SectionTitle("Base Stats")
                    StatLine("HP", pokemon.hp)
                    StatLine("Ataque", pokemon.attack)
                    StatLine("Defesa", pokemon.defense)
                    StatLine("Ataque Especial", pokemon.specialAttack)
                    StatLine("Defesa Especial", pokemon.specialDefense)
                    StatLine("Velocidade", pokemon.speed)
                    Spacer(Modifier.height(18.dp))
                    SectionTitle("Habilidades")
                    pokemon.abilities.forEach { ability ->
                        Text(
                            text = "• $ability",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    SectionTitle("Próximos módulos")
                    Text(
                        "Evolução, golpes, localização por jogo, habitat, formas e dados avançados serão ligados ao banco completo nas próximas etapas.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun StatLine(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value.toString(), fontWeight = FontWeight.SemiBold)
    }
}
