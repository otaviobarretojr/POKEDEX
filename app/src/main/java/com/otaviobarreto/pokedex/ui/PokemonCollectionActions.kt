package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.CollectionStore

/** Read-only collection placement. Collection changes are intentionally done from Box long-press. */
@Composable
fun PokemonCollectionActions(
    pokemonId: Int,
    modifier: Modifier = Modifier
) {
    val boxes = CollectionStore.boxes
    val boxNames = CollectionStore.boxNames
    val pokemonBoxes = boxNames.filter { pokemonId in boxes[it].orEmpty() }
    val sprite = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$pokemonId.png"

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Minha coleção", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (pokemonBoxes.isEmpty()) {
                Text("Ainda não está em nenhuma Box", style = MaterialTheme.typography.bodySmall)
            } else {
                Text(
                    "Adicionado em ${pokemonBoxes.size} Box${if (pokemonBoxes.size == 1) "" else "es"}",
                    style = MaterialTheme.typography.bodySmall
                )
                pokemonBoxes.forEach { box ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(sprite, null, Modifier.size(48.dp))
                            Column(Modifier.weight(1f).padding(start = 9.dp)) {
                                Text(box, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Pokémon registrado nesta Box", style = MaterialTheme.typography.labelSmall)
                            }
                            Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
