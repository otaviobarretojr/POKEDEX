package com.otaviobarreto.pokedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.otaviobarreto.pokedex.ui.PokedexScreen
import com.otaviobarreto.pokedex.ui.PokemonDetailScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PokedexApp()
            }
        }
    }
}

data class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val mainDestinations = listOf(
    MainDestination("pokedex", "Pokédex", Icons.Default.CatchingPokemon),
    MainDestination("livingdex", "Living Dex", Icons.Default.ListAlt),
    MainDestination("teams", "Times", Icons.Default.Groups),
    MainDestination("boxes", "Boxes", Icons.Default.GridView)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isDetail = currentRoute == "pokemon/{id}"

    Scaffold(
        topBar = {
            if (!isDetail) {
                TopAppBar(title = { Text("POKEDEX") })
            }
        },
        bottomBar = {
            if (!isDetail) {
                NavigationBar {
                    mainDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "pokedex",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("pokedex") {
                PokedexScreen(onPokemonClick = { id -> navController.navigate("pokemon/$id") })
            }
            composable(
                route = "pokemon/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entry ->
                PokemonDetailScreen(
                    id = entry.arguments?.getInt("id") ?: -1,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("livingdex") { PlaceholderScreen("Living Dex", "Progresso de captura e coleção.") }
            composable("teams") { PlaceholderScreen("Team Builder", "Criação e análise de times.") }
            composable("boxes") { PlaceholderScreen("Boxes", "Organização por jogo e coleção.") }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
