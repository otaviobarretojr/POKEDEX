package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.CollectionStore

@Composable
fun PokemonCollectionActions(
    pokemonId: Int,
    modifier: Modifier = Modifier
) {
    val capturedIds by CollectionStore::capturedIds
    val boxes by CollectionStore::boxes
    val captured = pokemonId in capturedIds
    val pokemonBoxes = boxes.filterValues { pokemonId in it }.keys

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Minha coleção",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (pokemonBoxes.isEmpty()) {
                            "Ainda não está em nenhuma Box"
                        } else {
                            "Em ${pokemonBoxes.size} Box${if (pokemonBoxes.size == 1) "" else "es"}"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                AssistChip(
                    onClick = { CollectionStore.toggleCaptured(pokemonId) },
                    label = { Text(if (captured) "✓ Capturado" else "+ Capturar") }
                )
            }

            Text(
                text = "Adicionar / remover das Boxes",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CollectionStore.defaultBoxes.forEach { box ->
                    val selected = pokemonId in boxes[box].orEmpty()
                    AssistChip(
                        onClick = {
                            if (selected) CollectionStore.removeFromBox(box, pokemonId)
                            else CollectionStore.addToBox(box, pokemonId)
                        },
                        label = { Text(if (selected) "✓ $box" else "+ $box") }
                    )
                }
            }
        }
    }
}
