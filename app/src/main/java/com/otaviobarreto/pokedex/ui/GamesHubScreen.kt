package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class GameShortcut(
    val source: String,
    val title: String,
    val subtitle: String
)

private val gameShortcuts = listOf(
    GameShortcut("Scarlet / Violet · Paldea", "Paldea", "Scarlet / Violet · Pokédex principal"),
    GameShortcut("Scarlet / Violet · Kitakami", "Kitakami", "Scarlet / Violet · The Teal Mask"),
    GameShortcut("Scarlet / Violet · Blueberry", "Blueberry", "Scarlet / Violet · The Indigo Disk"),
    GameShortcut("Sword / Shield · Galar", "Galar", "Sword / Shield · Pokédex regional"),
    GameShortcut("Legends Arceus · Hisui", "Hisui", "Legends Arceus · Pokédex regional"),
    GameShortcut("Let's Go Pikachu / Eevee · Kanto", "Kanto", "Let's Go Pikachu / Eevee")
)

@Composable
fun GamesHubScreen(onOpenGame: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("Jogos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Pokédex, progresso e localização separados por região e jogo",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(gameShortcuts, key = { it.source }) { game ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onOpenGame(game.source) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Column(Modifier.weight(1f)) {
                            Text(game.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(game.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("›", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}
