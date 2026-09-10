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
import androidx.compose.material.icons.filled.Map
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
import com.otaviobarreto.pokedex.ui.GameDexScreen
import com.otaviobarreto.pokedex.ui.GamesHubScreen
import com.otaviobarreto.pokedex.ui.LivingDexScreen
import com.otaviobarreto.pokedex.ui.PokedexScreen
import com.otaviobarreto.pokedex.ui.PokemonCollectionActions
import com.otaviobarreto.pokedex.ui.PokemonDetailScreen
import com.otaviobarreto.pokedex.ui.PokemonLocationScreen
import com.otaviobarreto.pokedex.ui.PokemonRegionMapScreen
import com.otaviobarreto.pokedex.ui.RegionExplorerScreen
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
    MainDestination("games", "Jogos", Icons.Default.Map),
    MainDestination("teams", "Times", Icons.Default.Groups),
    MainDestination("boxes", "Boxes", Icons.Default.GridView)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isSecondaryScreen = currentRoute == "pokemon/{id}?source={source}" ||
        currentRoute == "gameDex?source={source}" ||
        currentRoute == "location/{id}?source={source}" ||
        currentRoute == "regionMap/{id}?source={source}" ||
        currentRoute == "regionExplorer?source={source}"

    fun openPokemon(id: Int, source: String? = null) {
        val route = if (source.isNullOrBlank()) {
            "pokemon/$id"
        } else {
            "pokemon/$id?source=${Uri.encode(source)}"
        }
        navController.navigate(route)
    }

    fun openGameDex(source: String) {
        navController.navigate("gameDex?source=${Uri.encode(source)}")
    }

    fun openLocation(id: Int, source: String? = null) {
        val route = if (source.isNullOrBlank()) {
            "location/$id"
        } else {
            "location/$id?source=${Uri.encode(source)}"
        }
        navController.navigate(route)
    }

    fun openRegionMap(id: Int, source: String) {
        navController.navigate("regionMap/$id?source=${Uri.encode(source)}")
    }

    fun openRegionExplorer(source: String) {
        navController.navigate("regionExplorer?source=${Uri.encode(source)}")
    }

    Scaffold(
        topBar = {
            if (!isSecondaryScreen) {
                TopAppBar(title = { Text("POKEDEX") })
            }
        },
        bottomBar = {
            if (!isSecondaryScreen) {
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
            composable("games") {
                GamesHubScreen(onOpenGame = ::openGameDex)
            }
            composable(
                route = "gameDex?source={source}",
                arguments = listOf(
                    navArgument("source") {
                        type = NavType.StringType
                        nullable = false
                    }
                )
            ) { entry ->
                val source = entry.arguments?.getString("source")?.let(Uri::decode).orEmpty()
                GameDexScreen(
                    source = source,
                    onBack = { navController.popBackStack() },
                    onPokemonClick = { id, gameSource -> openPokemon(id, gameSource) },
                    onLocationClick = { id, gameSource -> openLocation(id, gameSource) },
                    onOpenRegionExplorer = ::openRegionExplorer
                )
            }
            composable(
                route = "regionExplorer?source={source}",
                arguments = listOf(
                    navArgument("source") {
                        type = NavType.StringType
                        nullable = false
                    }
                )
            ) { entry ->
                val source = entry.arguments?.getString("source")?.let(Uri::decode).orEmpty()
                RegionExplorerScreen(
                    source = source,
                    onBack = { navController.popBackStack() },
                    onPokemonClick = { id, gameSource -> openPokemon(id, gameSource) }
                )
            }
            composable(
                route = "location/{id}?source={source}",
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
                PokemonLocationScreen(
                    pokemonId = pokemonId,
                    source = source,
                    onBack = { navController.popBackStack() },
                    onOpenMap = source?.let { gameSource ->
                        { openRegionMap(pokemonId, gameSource) }
                    }
                )
            }
            composable(
                route = "regionMap/{id}?source={source}",
                arguments = listOf(
                    navArgument("id") { type = NavType.IntType },
                    navArgument("source") {
                        type = NavType.StringType
                        nullable = false
                    }
                )
            ) { entry ->
                val pokemonId = entry.arguments?.getInt("id") ?: -1
                val source = entry.arguments?.getString("source")?.let(Uri::decode).orEmpty()
                PokemonRegionMapScreen(
                    pokemonId = pokemonId,
                    source = source,
                    onBack = { navController.popBackStack() }
                )
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
