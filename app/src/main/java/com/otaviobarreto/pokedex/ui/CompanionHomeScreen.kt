package com.otaviobarreto.pokedex.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.*

@Composable fun CompanionHomeScreen(onContinueJourney:()->Unit,onOpenGames:()->Unit){
 val revision=JourneyProgressStore.revision
 val configuredGame=AppStatePreferences.activeGame
 val game=remember(revision,configuredGame){
  AppGameCatalog.adventureGames.firstOrNull{g->g.label==configuredGame&&JourneyProgressStore.isStarted(g.label)}
   ?:AppGameCatalog.adventureGames.firstOrNull{g->JourneyProgressStore.isStarted(g.label)}
   ?:AppGameCatalog.adventureGames.firstOrNull{g->g.label==configuredGame}
   ?:AppGameCatalog.adventureGames.firstOrNull()
 }
 val steps=remember(game?.label,revision){game?.let{g->JourneyCatalog.steps(g.label)}.orEmpty()}
 val completed=remember(game?.label,revision){game?.let{g->JourneyProgressStore.completed(g.label)}.orEmpty()}
 val next=remember(steps,completed){steps.firstOrNull{s->s.id !in completed}}
 val accent=PokedexDesignTokens.Colors.game(game?.label.orEmpty())
 val hero=remember(game?.label){game?.let{g->GameCoverCatalog.heroFor(g.label)}}
 DexAppBackground{
  LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(PokedexDesignTokens.Spacing.Lg),verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Xl)){
   item{
    CompanionContextHeader(title=game?.label?:"Sua Jornada Pokemon",eyebrow="Trainer Companion",
     subtitle=next?.let{s->"Proximo objetivo · "+s.title} ?: if(steps.isNotEmpty())"Campanha concluida" else "Escolha uma aventura para comecar.",
     accent=accent,
     progress={CompanionProgress(completed.size.coerceAtMost(steps.size),steps.size,"Progresso da campanha",accent=accent)},
     artwork=hero?.let{url->{AsyncImage(model=url,contentDescription=null,modifier=Modifier.align(Alignment.CenterEnd).fillMaxHeight().widthIn(max=128.dp),contentScale=ContentScale.Crop,alpha=PokedexDesignTokens.Companion.ArtworkFadeAlpha)}})
   }
   item{
    Column{
     CompanionSectionHeader(title=if(next!=null)"Continue de onde parou" else "Sua aventura",supporting=next?.title?:"Acesse sua Jornada para acompanhar a campanha.")
     Spacer(Modifier.height(PokedexDesignTokens.Spacing.Md))
     PrimaryCompanionAction(label=if(JourneyProgressStore.isStarted(game?.label.orEmpty()))"Continuar Jornada" else "Abrir Jornada",onClick=onContinueJourney,leading={Icon(Icons.Default.Explore,null)})
    }
   }
   item{
    CompanionSectionHeader(title="Jogos",supporting="Escolha outra aventura, inicie uma nova jornada ou revise um jogo.",actionLabel="Ver jogos",onAction=onOpenGames)
    FilledTonalButton(onClick=onOpenGames,modifier=Modifier.fillMaxWidth().heightIn(min=PokedexDesignTokens.Companion.MinimumTouchTarget)){
     Icon(Icons.Default.SportsEsports,null);Spacer(Modifier.width(PokedexDesignTokens.Spacing.Sm));Text("Biblioteca de jogos",fontWeight=FontWeight.Bold)
    }
   }
  }
 }
}
