package com.otaviobarreto.pokedex

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.CompanionPreferences
import com.otaviobarreto.pokedex.data.TeamStore
import com.otaviobarreto.pokedex.data.RecentActivityStore
import com.otaviobarreto.pokedex.data.JourneyProgressStore
import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.audio.HomeAudioManager
import com.otaviobarreto.pokedex.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CollectionStore.initialize(this)
        TeamStore.initialize(this)
        CompanionPreferences.initialize(this)
        val legacySource = CompanionPreferences.activeRegionSource
            ?: AppGameCatalog.games.firstOrNull { it.label == CompanionPreferences.activeGame }?.regions?.firstOrNull()?.source
        CollectionStore.migrateLegacyCapturedToSource(legacySource)
        RecentActivityStore.initialize(this)
        JourneyProgressStore.initialize(this)
        HomeAudioManager.initialize(this)
        HomeAudioManager.playBoot()
        setContent { PokedexTheme { PokedexRoot() } }
    }

    override fun onStart() {
        super.onStart()
        HomeAudioManager.onAppForegrounded()
    }

    override fun onStop() {
        HomeAudioManager.onAppBackgrounded()
        super.onStop()
    }

    override fun onDestroy() {
        HomeAudioManager.release()
        super.onDestroy()
    }
}

@Composable private fun PokedexRoot(){var bootReady by remember{mutableStateOf(false)};if(!bootReady)BootExperienceScreen{HomeAudioManager.playMainTrack();bootReady=true}else PokedexApp()}
data class MainDestination(val route:String,val label:String,val icon:ImageVector)
private val mainDestinations=listOf(MainDestination("home","Jornada",Icons.Default.Map),MainDestination("boxes","Boxes",Icons.Default.GridView))

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PokedexApp(){
 val navController=rememberNavController();val backStackEntry by navController.currentBackStackEntryAsState();val currentRoute=backStackEntry?.destination?.route
 val isSecondaryScreen=currentRoute=="pokemon/{id}?source={source}"||currentRoute=="gameDex?source={source}"||currentRoute=="location/{id}?source={source}"||currentRoute=="regionMap/{id}?source={source}"||currentRoute=="regionExplorer?source={source}"||currentRoute=="reference?kind={kind}&name={name}&source={source}"||currentRoute=="campaignGuide?game={game}&phase={phase}";val isMainDestination=currentRoute in mainDestinations.map{it.route};val useCompactOwnHeader=currentRoute=="boxes"
 fun openPokemon(id:Int,source:String?=null){RecentActivityStore.recordPokemon(id);navController.navigate(if(source.isNullOrBlank())"pokemon/$id" else "pokemon/$id?source=${Uri.encode(source)}")}
 fun openBoxes(game:String?=null,source:String?=null){
  val resolvedGame=game ?: CompanionPreferences.activeGame
  val resolvedSource=source ?: CompanionPreferences.activeRegionForGame(resolvedGame)
  game?.let{CompanionPreferences.activeGame=it}
  if(resolvedSource!=null) CompanionPreferences.setActiveRegionForGame(resolvedGame,resolvedSource)
  navController.navigate("boxes"){popUpTo("home"){saveState=true};launchSingleTop=true;restoreState=false}
 }
 fun openGameDex(source:String){navController.navigate("gameDex?source=${Uri.encode(source)}")};fun openCampaignGuide(game:String,phase:String?=null){navController.navigate("campaignGuide?game=${Uri.encode(game)}"+(phase?.let{"&phase=${Uri.encode(it)}"}?:""))};fun openLocation(id:Int,source:String?=null){navController.navigate(if(source.isNullOrBlank())"location/$id" else "location/$id?source=${Uri.encode(source)}")};fun openRegionMap(id:Int,source:String){navController.navigate("regionMap/$id?source=${Uri.encode(source)}")};fun openRegionExplorer(source:String){navController.navigate("regionExplorer?source=${Uri.encode(source)}")};fun openReference(kind:String?=null,name:String?=null,source:String?=null){navController.navigate(if(kind.isNullOrBlank()||name.isNullOrBlank())"reference" else "reference?kind=${Uri.encode(kind)}&name=${Uri.encode(name)}"+(source?.let{"&source=${Uri.encode(it)}"}?:""))}
 LaunchedEffect(currentRoute){
  currentRoute?.let(RecentActivityStore::recordRoute)
 }
 Scaffold(bottomBar={if(!isSecondaryScreen){NavigationBar{mainDestinations.forEach{d->NavigationBarItem(selected=currentRoute==d.route,onClick={navController.navigate(d.route){popUpTo("home"){saveState=true};launchSingleTop=true;restoreState=true}},icon={Icon(d.icon,d.label)},label={Text(d.label)},alwaysShowLabel=false)}}}},topBar={if(isMainDestination&&currentRoute!="home"&&!useCompactOwnHeader){TopAppBar(title={Text(mainDestinations.firstOrNull{it.route==currentRoute}?.label?:"POKEDEX")},navigationIcon={IconButton(onClick={navController.navigate("home"){popUpTo("home"){inclusive=false};launchSingleTop=true}}){Icon(Icons.Default.Home,"Voltar ao início")}})}}){innerPadding->
  NavHost(navController,"home",Modifier.padding(innerPadding)){
   composable("home"){JourneyScreen(onPokemonClick={id,source->openPokemon(id,source)},onOpenTeamGuide={game,phase->openCampaignGuide(game,phase)},onOpenBoxes=::openBoxes)}
   composable("campaignGuide?game={game}&phase={phase}",arguments=listOf(navArgument("game"){type=NavType.StringType;nullable=false},navArgument("phase"){type=NavType.StringType;nullable=true;defaultValue=null})){entry->val game=entry.arguments?.getString("game")?.let(Uri::decode);val phase=entry.arguments?.getString("phase")?.let(Uri::decode);CampaignTeamGuideScreen(onBackToMyTeams={navController.popBackStack()},onPokemonClick={id,source->openPokemon(id,source)},initialGame=game,initialPhase=phase)}
   composable("pokedex"){PokedexV2Screen(onPokemonClick={openPokemon(it)},onOpenReference={openReference()})}
   composable("reference?kind={kind}&name={name}&source={source}",arguments=listOf(navArgument("kind"){type=NavType.StringType;nullable=true;defaultValue=null},navArgument("name"){type=NavType.StringType;nullable=true;defaultValue=null},navArgument("source"){type=NavType.StringType;nullable=true;defaultValue=null})){entry->val kind=entry.arguments?.getString("kind")?.let(Uri::decode);val name=entry.arguments?.getString("name")?.let(Uri::decode);val source=entry.arguments?.getString("source")?.let(Uri::decode);ReferenceHubScreen(onBack={navController.popBackStack()},initialKind=kind,initialName=name,source=source,onPokemonClick={id,pokemonSource->openPokemon(id,pokemonSource)})}
   composable("pokemon/{id}?source={source}",arguments=listOf(navArgument("id"){type=NavType.IntType},navArgument("source"){type=NavType.StringType;nullable=true;defaultValue=null})){entry->val id=entry.arguments?.getInt("id")?:-1;val source=entry.arguments?.getString("source")?.let(Uri::decode);PokemonDetailV2Screen(id=id,source=source,onBack={navController.popBackStack()},onOpenLocation={openLocation(id,source)},onOpenReference={kind,name->openReference(kind,name,source)},onOpenPokemon={nextId->openPokemon(nextId,source)})}
   composable("livingdex"){LivingDexScreen(onPokemonClick={id,source->openPokemon(id,source)})};composable("games"){GamesHubScreen(onOpenGame=::openGameDex)};composable("companion"){CompanionHubScreen(onPokemonClick={openPokemon(it)},onReferenceClick={kind,name->openReference(kind,name)},onOpenGame=::openGameDex)}
   composable("gameDex?source={source}",arguments=listOf(navArgument("source"){type=NavType.StringType;nullable=false})){entry->val source=entry.arguments?.getString("source")?.let(Uri::decode).orEmpty();GameDexScreen(source,{navController.popBackStack()},{id,gameSource->openPokemon(id,gameSource)},{id,gameSource->openLocation(id,gameSource)},::openRegionExplorer)}
   composable("regionExplorer?source={source}",arguments=listOf(navArgument("source"){type=NavType.StringType;nullable=false})){entry->val source=entry.arguments?.getString("source")?.let(Uri::decode).orEmpty();UnifiedRegionExplorerScreen(source,{navController.popBackStack()},{id,gameSource->openPokemon(id,gameSource)})}
   composable("location/{id}?source={source}",arguments=listOf(navArgument("id"){type=NavType.IntType},navArgument("source"){type=NavType.StringType;nullable=true;defaultValue=null})){entry->val id=entry.arguments?.getInt("id")?:-1;val source=entry.arguments?.getString("source")?.let(Uri::decode);PokemonLocationScreen(id,source,{navController.popBackStack()},source?.let{{openRegionMap(id,it)}})}
   composable("regionMap/{id}?source={source}",arguments=listOf(navArgument("id"){type=NavType.IntType},navArgument("source"){type=NavType.StringType;nullable=false})){entry->val id=entry.arguments?.getInt("id")?:-1;val source=entry.arguments?.getString("source")?.let(Uri::decode).orEmpty();PokemonRegionMapScreen(id,source){navController.popBackStack()}}
   composable("teams"){TeamBuilderScreen(onPokemonClick={openPokemon(it)})};composable("boxes"){BoxesV2Screen(onPokemonClick={id,source->openPokemon(id,source)})}
  }
 }
}
