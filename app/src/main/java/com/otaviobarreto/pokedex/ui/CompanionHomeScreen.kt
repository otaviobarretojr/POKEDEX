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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.*

@Composable
fun CompanionHomeScreen(
    onContinueJourney: () -> Unit,
    onOpenGames: () -> Unit
) {
    val revision = JourneyProgressStore.revision
    val configuredGame = AppStatePreferences.activeGame
    val game = remember(revision, configuredGame) {
        AppGameCatalog.adventureGames.firstOrNull { g -> g.label == configuredGame && JourneyProgressStore.isStarted(g.label) }
            ?: AppGameCatalog.adventureGames.firstOrNull { g -> JourneyProgressStore.isStarted(g.label) }
            ?: AppGameCatalog.adventureGames.firstOrNull { g -> g.label == configuredGame }
            ?: AppGameCatalog.adventureGames.firstOrNull()
    }
    val steps = remember(game?.label, revision) { game?.let { g -> JourneyCatalog.steps(g.label) }.orEmpty() }
    val completed = remember(game?.label, revision) { game?.let { g -> JourneyProgressStore.completed(g.label) }.orEmpty() }
    val next = remember(steps, completed) { steps.firstOrNull { s -> s.id !in completed } }
    val started = JourneyProgressStore.isStarted(game?.label.orEmpty())
    val accent = PokedexDesignTokens.Colors.game(game?.label.orEmpty())
    val hero = remember(game?.label) { game?.let { g -> GameCoverCatalog.heroFor(g.label) } }

    DexAppBackground {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = PokedexDesignTokens.Spacing.Lg,
                end = PokedexDesignTokens.Spacing.Lg,
                top = PokedexDesignTokens.Spacing.Lg,
                bottom = PokedexDesignTokens.Spacing.Xxl
            ),
            verticalArrangement = Arrangement.spacedBy(PokedexDesignTokens.Spacing.Xxl)
        ) {
            item {
                CompanionContextHeader(
                    title = game?.label ?: "Sua Jornada Pokémon",
                    eyebrow = "Trainer Companion",
                    subtitle = when {
                        !started -> "Sua aventura está pronta para começar."
                        steps.isNotEmpty() && next == null -> "Campanha concluída"
                        else -> "Jornada em andamento"
                    },
                    accent = accent,
                    progress = {
                        CompanionProgress(
                            current = completed.size.coerceAtMost(steps.size),
                            total = steps.size,
                            label = "Progresso da campanha",
                            accent = accent
                        )
                    },
                    artwork = hero?.let { url ->
                        {
                            AsyncImage(
                                model = rememberOfflineArtworkModel(url),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .widthIn(max = 136.dp),
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center,
                                alpha = PokedexDesignTokens.Companion.ArtworkFadeAlpha
                            )
                        }
                    }
                )
            }

            item {
                Column {
                    CompanionSectionHeader(
                        title = if (started) "Continue de onde parou" else "Comece sua aventura",
                        supporting = next?.title ?: if (started) "Acompanhe os próximos passos da sua Jornada." else "Escolha seu inicial e prepare sua Jornada."
                    )
                    Spacer(Modifier.height(PokedexDesignTokens.Spacing.Lg))
                    PrimaryCompanionAction(
                        label = if (started) "Continuar Jornada" else "Abrir Jornada",
                        onClick = onContinueJourney,
                        leading = { Icon(Icons.Default.Explore, contentDescription = null) }
                    )
                }
            }

            item {
                Column {
                    CompanionSectionHeader(
                        title = "Jogos",
                        supporting = "Escolha outra aventura, inicie uma nova jornada ou revise um jogo."
                    )
                    Spacer(Modifier.height(PokedexDesignTokens.Spacing.Md))
                    FilledTonalButton(
                        onClick = onOpenGames,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = PokedexDesignTokens.Companion.MinimumTouchTarget)
                    ) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null)
                        Spacer(Modifier.width(PokedexDesignTokens.Spacing.Sm))
                        Text("Biblioteca de jogos", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
