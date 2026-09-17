package com.otaviobarreto.pokedex

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.otaviobarreto.pokedex.data.AppStatePreferences
import com.otaviobarreto.pokedex.data.RecentActivityStore
import com.otaviobarreto.pokedex.audio.HomeAudioManager
import com.otaviobarreto.pokedex.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HomeAudioManager.playBoot()
        setContent { PokedexTheme { PokedexRoot() } }
    }
    override fun onStart() { super.onStart(); HomeAudioManager.onAppForegrounded() }
    override fun onStop() { HomeAudioManager.onAppBackgrounded(); super.onStop() }
    override fun onDestroy() { HomeAudioManager.release(); super.onDestroy() }
}

@Composable private fun PokedexRoot(){var bootReady by remember{mutableStateOf(false)};if(!bootReady)BootExperienceScreen{HomeAudioManager.playMainTrack();bootReady=true}else PokedexApp()}
// Compatibility markers for legacy source guards; implementation uses PokedexRoutes.
// DexNavItem("home","Jornada"
// DexNavItem("pokedex","Pokédex"
// DexNavItem("boxes","Boxes"
// DexNavItem("central","Config."
private val mainDestinations=listOf(
 DexNavItem(PokedexRoutes.HOME,"Jornada",Icons.Default.Map),
 DexNavItem(PokedexRoutes.GAMES,"Jogos",Icons.Default.SportsEsports),
 DexNavItem(PokedexRoutes.POKEDEX,"Pokédex",Icons.Default.MenuBook),
 DexNavItem(PokedexRoutes.COLLECTION,"Coleção",Icons.Default.AutoAwesome),
 DexNavItem(PokedexRoutes.BOXES,"Box",Icons.Default.GridView),
 DexNavItem(PokedexRoutes.CENTRAL,"Config.",Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PokedexApp(){
 val haptics=LocalHapticFeedback.current
 val navController=rememberNavController();val backStackEntry by navController.currentBackStackEntryAsState();val currentRoute=backStackEntry?.destination?.route
 val isSecondaryScreen=PokedexRoutes.isSecondary(currentRoute)
 fun openPokemon(id:Int,source:String?=null){RecentActivityStore.recordPokemon(id);navController.navigate(if(source.isNullOrBlank())"pokemon/"+id else "pokemon/"+id+"?source="+Uri.encode(source))}
 fun replacePokemonDetail(id:Int,source:String?=null){RecentActivityStore.recordPokemon(id);val route=if(source.isNullOrBlank())"pokemon/"+id else "pokemon/"+id+"?source="+Uri.encode(source);navController.navigate(route){popUpTo("pokemon/{id}?source={source}"){inclusive=true}}}
 fun openFormDetail(id:Int,name:String,shiny:Boolean){navController.navigate("formDetail/"+id+"?name="+Uri.encode(name)+"&shiny="+shiny)}
 fun openBoxes(game:String?=null,source:String?=null){val resolvedGame=game?:AppStatePreferences.activeGame;val resolvedSource=source?:AppStatePreferences.activeRegionForGame(resolvedGame);game?.let{AppStatePreferences.activeGame=it};if(resolvedSource!=null)AppStatePreferences.setActiveRegionForGame(resolvedGame,resolvedSource);navController.navigate(PokedexRoutes.BOXES){popUpTo(PokedexRoutes.HOME){saveState=true};launchSingleTop=true;restoreState=true}}
 fun openCampaignGuide(game:String,phase:String?=null,step:String?=null){navController.navigate("campaignGuide?game=${Uri.encode(game)}"+(phase?.let{"&phase=${Uri.encode(it)}"}?:"")+(step?.let{"&step=${Uri.encode(it)}"}?:""))}
 fun openReference(kind:String?=null,name:String?=null,source:String?=null){navController.navigate(if(kind.isNullOrBlank()||name.isNullOrBlank())"reference" else "reference?kind=${Uri.encode(kind)}&name=${Uri.encode(name)}"+(source?.let{"&source=${Uri.encode(it)}"}?:""))}
 fun openUniversalSearch(){navController.navigate("search")}
 fun openEvolutionCenter(){navController.navigate("evolutionCenter")}
 fun openGameDex(){navController.navigate("gameDex")}
 LaunchedEffect(currentRoute){currentRoute?.let(RecentActivityStore::recordRoute)}
 Scaffold(containerColor=MaterialTheme.colorScheme.background,bottomBar={if(!isSecondaryScreen){DexBottomBar(mainDestinations,currentRoute){d->if(d.route!=currentRoute)haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove);navController.navigate(d.route){popUpTo(PokedexRoutes.HOME){saveState=true};launchSingleTop=true;restoreState=true}}}}){innerPadding->
  NavHost(navController,PokedexRoutes.HOME,Modifier.padding(innerPadding),enterTransition={fadeIn(tween(PokedexDesignTokens.Motion.Fast))+slideInHorizontally(tween(PokedexDesignTokens.Motion.Fast)){it/14}},exitTransition={fadeOut(tween(PokedexDesignTokens.Motion.Fast))+slideOutHorizontally(tween(PokedexDesignTokens.Motion.Fast)){-(it/18)}},popEnterTransition={fadeIn(tween(PokedexDesignTokens.Motion.Fast))+slideInHorizontally(tween(PokedexDesignTokens.Motion.Fast)){-(it/14)}},popExitTransition={fadeOut(tween(PokedexDesignTokens.Motion.Fast))+slideOutHorizontally(tween(PokedexDesignTokens.Motion.Fast)){it/18}}){
   composable(PokedexRoutes.HOME){JourneyScreen(startInGames=false,onPokemonClick={id,source->openPokemon(id,source)},onOpenTeamGuide={game,phase,step->openCampaignGuide(game,phase,step)},onOpenBoxes=::openBoxes,onOpenGameDex=::openGameDex,onOpenEvolutionCenter=::openEvolutionCenter,onOpenSearch=::openUniversalSearch)}
   composable(PokedexRoutes.GAMES){JourneyScreen(startInGames=true,onPokemonClick={id,source->openPokemon(id,source)},onOpenTeamGuide={game,phase,step->openCampaignGuide(game,phase,step)},onOpenBoxes=::openBoxes,onOpenGameDex=::openGameDex,onOpenEvolutionCenter=::openEvolutionCenter,onOpenSearch=::openUniversalSearch)}
   composable("campaignGuide?game={game}&phase={phase}&step={step}",arguments=listOf(navArgument("game"){type=NavType.StringType;nullable=false},navArgument("phase"){type=NavType.StringType;nullable=true;defaultValue=null},navArgument("step"){type=NavType.StringType;nullable=true;defaultValue=null})){entry->val game=entry.arguments?.getString("game")?.let(Uri::decode);val phase=entry.arguments?.getString("phase")?.let(Uri::decode);val step=entry.arguments?.getString("step")?.let(Uri::decode);CampaignTeamGuideScreen(onBackToMyTeams={navController.popBackStack()},onPokemonClick={id,source->openPokemon(id,source)},initialGame=game,initialPhase=phase,initialStepId=step)}
   composable("reference?kind={kind}&name={name}&source={source}",arguments=listOf(navArgument("kind"){type=NavType.StringType;nullable=true;defaultValue=null},navArgument("name"){type=NavType.StringType;nullable=true;defaultValue=null},navArgument("source"){type=NavType.StringType;nullable=true;defaultValue=null})){entry->val kind=entry.arguments?.getString("kind")?.let(Uri::decode);val name=entry.arguments?.getString("name")?.let(Uri::decode);val source=entry.arguments?.getString("source")?.let(Uri::decode);ReferenceHubScreen(onBack={navController.popBackStack()},initialKind=kind,initialName=name,source=source,onPokemonClick={id,pokemonSource->openPokemon(id,pokemonSource)})}
   composable("pokemon/{id}?source={source}",arguments=listOf(navArgument("id"){type=NavType.IntType},navArgument("source"){type=NavType.StringType;nullable=true;defaultValue=null})){entry->val id=entry.arguments?.getInt("id")?:-1;val source=entry.arguments?.getString("source")?.let(Uri::decode);PokemonDetailV2Screen(id=id,source=source,onBack={navController.popBackStack()},onOpenReference={kind,name->openReference(kind,name,source)},onOpenPokemon={nextId->replacePokemonDetail(nextId,source)})}
   composable("formDetail/{id}?name={name}&shiny={shiny}",arguments=listOf(navArgument("id"){type=NavType.IntType},navArgument("name"){type=NavType.StringType;nullable=false},navArgument("shiny"){type=NavType.BoolType;defaultValue=false})){entry->val id=entry.arguments?.getInt("id")?:-1;val name=entry.arguments?.getString("name")?.let(Uri::decode).orEmpty();val shiny=entry.arguments?.getBoolean("shiny")?:false;PokemonFormDetailScreen(id,name,shiny){navController.popBackStack()}}
   composable("search"){UniversalSearchScreen(onBack={navController.popBackStack()},onPokemonClick={id->openPokemon(id,null)},onOpenReference={kind,name->openReference(kind,name,AppStatePreferences.activeRegionForGame(AppStatePreferences.activeGame))})}
   composable("evolutionCenter"){EvolutionCenterScreen(onBack={navController.popBackStack()},onPokemonClick={id,source->openPokemon(id,source)})}
   composable("gameDex"){GameDexScreen(onBack={navController.popBackStack()},onPokemonClick={id,source->openPokemon(id,source)})}
   composable(PokedexRoutes.POKEDEX){PokedexCatalogScreen(onPokemonClick={id->openPokemon(id,null)},onOpenFormDetail=::openFormDetail,onOpenSearch=::openUniversalSearch,onOpenEvolutionCenter=::openEvolutionCenter,onOpenGameDex=::openGameDex)}
   composable(PokedexRoutes.COLLECTION){CollectionScreen(onPokemonClick={id->openPokemon(id,null)},onOpenBoxes=::openBoxes)}
   composable(PokedexRoutes.BOXES){BoxesV2Screen(onPokemonClick={id,source->openPokemon(id,source)})}
   composable(PokedexRoutes.CENTRAL){CompanionCenterScreen(onPokemonClick={id->openPokemon(id,null)},onOpenBoxes=::openBoxes)}
  }
 }
}