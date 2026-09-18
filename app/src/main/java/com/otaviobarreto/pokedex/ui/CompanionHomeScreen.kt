package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CompanionHomeScreen() {
    Box(Modifier.fillMaxSize()) {
        PokemonLivingCompanionCard(
            pokemonId = 25,
            pokemonName = "Pikachu",
            immersive = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}
