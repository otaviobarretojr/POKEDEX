package com.otaviobarreto.pokedex

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.TeamStore
import com.otaviobarreto.pokedex.ui.BoxesScreen
import com.otaviobarreto.pokedex.ui.LivingDexScreen
import com.otaviobarreto.pokedex.ui.PokedexScreen
import com.otaviobarreto.pokedex.ui.PokemonCollectionActions
import com.otaviobarreto.pokedex.ui.PokemonDetailScreen
import com.otaviobarreto.pokedex.ui.TeamBuilderScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CollectionStore.initialize(this)
        TeamStore.initialize(this)
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
    val isDetail = currentRoute == "pokemon/{id}?source={source}"

    fun openPokemon(id: Int, source: String? = null) {
        val route = if (source.isNullOrBlank()) {
            "pokemon/$id"
        } else {
            "pokemon/$id?source=${Uri.encode(source)}"
        }
        navController.navigate(route)
    }

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
                PokedexScreen(onPokemonClick = { id -> openPokemon(id) })
            }
            composable(
                route = "pokemon/{id}?source={source}",
                arguments = listOf(
                    navArgument("id") { type = NavType.IntType },
                    navArgument("source") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val pokemonId = entry.arguments?.getInt("id") ?: -1
                val source = entry.arguments?.getString("source")?.let(Uri::decode)
                Column(modifier = Modifier.fillMaxSize()) {
                    PokemonCollectionActions(pokemonId = pokemonId)
                    Box(modifier = Modifier.fillMaxSize()) {
                        PokemonDetailScreen(
                            id = pokemonId,
                            source = source,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
            composable("livingdex") {
                LivingDexScreen(onPokemonClick = { id -> openPokemon(id) })
            }
            composable("teams") {
                TeamBuilderScreen(onPokemonClick = { id -> openPokemon(id) })
            }
            composable("boxes") {
                BoxesScreen(onPokemonClick = { id, source -> openPokemon(id, source) })
            }
        }
    }
}
