package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.TypeMatchup

@Composable
fun TypeMatchupCard(types: List<String>) {
    val result = TypeMatchup.defensiveFor(types)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Defesa por tipo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Multiplicadores recebidos considerando ${types.joinToString(" / ")}.", style = MaterialTheme.typography.bodySmall)
            MatchupRow("Fraqueza 4×", result.quadrupleWeak, "4×")
            MatchupRow("Fraqueza 2×", result.doubleWeak, "2×")
            MatchupRow("Resiste ½×", result.halfResist, "½×")
            MatchupRow("Resiste ¼×", result.quarterResist, "¼×")
            MatchupRow("Imune", result.immune, "0×")
        }
    }
}

@Composable
private fun MatchupRow(title: String, values: List<String>, multiplier: String) {
    if (values.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(values, key = { it }) { type ->
                AssistChip(onClick = {}, label = { Text("$type $multiplier") })
            }
        }
    }
}
