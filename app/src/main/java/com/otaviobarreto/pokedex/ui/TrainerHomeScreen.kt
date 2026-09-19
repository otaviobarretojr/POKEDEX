package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TrainerHomeScreen(
    onContinueJourney: (String) -> Unit,
    onOpenGames: () -> Unit,
    onPokemonClick: (Int) -> Unit,
    onOpenCollection: () -> Unit
) {
    val journeyRevision = JourneyProgressStore.revision
    val capturedIds = CollectionStore.capturedIds
    val recentCaptured = TrainerTodayStore.recentCapturedIds
    val recentlyViewed = RecentActivityStore.recentPokemon

    val totalJourneyCompleted = remember(journeyRevision) {
        AppGameCatalog.adventureGames.sumOf { JourneyProgressStore.completed(it.label).size }
    }
    LaunchedEffect(capturedIds.size, totalJourneyCompleted) {
        TrainerTodayStore.ensureCurrentDay(capturedIds.size, totalJourneyCompleted)
    }

    val configuredGame = AppStatePreferences.activeGame
    val activeGame = remember(configuredGame, journeyRevision) {
        AppGameCatalog.adventureGames
            .firstOrNull { it.label == configuredGame && JourneyProgressStore.isStarted(it.label) }
            ?: AppGameCatalog.adventureGames.firstOrNull { JourneyProgressStore.isStarted(it.label) }
    }
    val activeSteps = remember(activeGame?.label, journeyRevision) {
        activeGame?.let { JourneyCatalog.steps(it.label) }.orEmpty()
    }
    val activeCompleted = remember(activeGame?.label, journeyRevision) {
        activeGame?.let { JourneyProgressStore.completed(it.label) }.orEmpty()
    }
    val completedCount = remember(activeSteps, activeCompleted) {
        DataIntegrityRules.completedCount(activeSteps.map { it.id }, activeCompleted)
    }
    val progress = if (activeSteps.isEmpty()) 0f else completedCount.toFloat() / activeSteps.size
    val currentIndex = activeSteps.indexOfFirst { it.id !in activeCompleted }
    val nextStep = activeSteps.getOrNull(currentIndex.takeIf { it >= 0 } ?: activeSteps.size)
    val upcoming = if (currentIndex >= 0) activeSteps.drop(currentIndex).take(3) else emptyList()

    val today = remember { LocalDate.now() }
    val epochDay = today.toEpochDay()
    val dailyPokemonId = remember(today) {
        ((epochDay % PokeApiService.MAX_NATIONAL_DEX_ID) + 1L).toInt()
    }
    val dailyPokemon = remember(dailyPokemonId) { PokemonRepository.byId(dailyPokemonId) }
    val todayCaptured = TrainerTodayStore.todayCaptured(capturedIds.size)
    val todayJourney = TrainerTodayStore.todayJourneyCompleted(totalJourneyCompleted)
    val missionUsesJourney = activeGame != null && epochDay % 2L == 0L
    val missionTarget = if (missionUsesJourney) 1 else 3
    val missionProgress = if (missionUsesJourney) todayJourney else todayCaptured
    val missionRatio = (missionProgress.toFloat() / missionTarget).coerceIn(0f, 1f)
    val recentIds = remember(recentCaptured, recentlyViewed, capturedIds) {
        (recentCaptured.filter { it in capturedIds } + recentlyViewed).distinct().take(6)
    }

    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Bom dia"
            in 12..17 -> "Boa tarde"
            else -> "Boa noite"
        }
    }
    val dateText = remember(today) {
        val locale = Locale("pt", "BR")
        today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", locale))
            .replaceFirstChar { it.uppercase() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = .09f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "trainer_header") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text(dateText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "${TrainerTodayStore.visitStreak} ${if (TrainerTodayStore.visitStreak == 1) "dia" else "dias"}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item(key = "journey_hero") {
                if (activeGame != null) {
                    TrainerJourneyHero(
                        game = activeGame,
                        nextStep = nextStep,
                        completed = completedCount,
                        total = activeSteps.size,
                        progress = progress,
                        onContinue = { onContinueJourney(activeGame.label) }
                    )
                } else {
                    TrainerEmptyJourneyCard(onOpenGames)
                }
            }

            item(key = "today_header") {
                TrainerSectionHeader("Hoje", "Um resumo leve para continuar jogando.")
            }

            item(key = "today_cards") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DailyPokemonCard(
                        pokemonId = dailyPokemonId,
                        pokemonName = dailyPokemon?.name ?: "#${dailyPokemonId.toString().padStart(4, '0')}",
                        onClick = { onPokemonClick(dailyPokemonId) },
                        modifier = Modifier.weight(1f)
                    )
                    DailyMissionCard(
                        title = if (missionUsesJourney) "Avance na Jornada" else "Registre Pokémon",
                        supporting = if (missionUsesJourney) "Conclua 1 objetivo hoje." else "Adicione 3 registros hoje.",
                        progress = missionProgress,
                        target = missionTarget,
                        ratio = missionRatio,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "day_progress") {
                TrainerSectionHeader("Seu dia", "Progresso registrado neste aparelho.")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrainerMetricCard(Icons.Default.CatchingPokemon, "+$todayCaptured", "Registros", Modifier.weight(1f))
                    TrainerMetricCard(Icons.Default.Flag, "+$todayJourney", "Objetivos", Modifier.weight(1f))
                    TrainerMetricCard(Icons.Default.MenuBook, capturedIds.size.toString(), "Pokédex", Modifier.weight(1f), onOpenCollection)
                }
            }

            if (recentIds.isNotEmpty()) {
                item(key = "recent_header") {
                    TrainerSectionHeader("Recentes", "Capturas e Pokémon vistos recentemente.")
                }
                item(key = "recent_row") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(end = 4.dp)) {
                        items(recentIds, key = { it }) { id ->
                            val pokemon = PokemonRepository.byId(id)
                            RecentPokemonCard(
                                pokemonId = id,
                                pokemonName = pokemon?.name ?: "#${id.toString().padStart(4, '0')}",
                                captured = id in capturedIds,
                                onClick = { onPokemonClick(id) }
                            )
                        }
                    }
                }
            }

            if (activeGame != null && upcoming.isNotEmpty()) {
                item(key = "upcoming_header") {
                    TrainerSectionHeader("A seguir", GameCoverCatalog.displayNameFor(activeGame.label))
                }
                items(upcoming, key = { "upcoming_" + it.id }) { step ->
                    UpcomingJourneyCard(
                        step = step,
                        current = step.id == nextStep?.id,
                        onClick = { onContinueJourney(activeGame.label) }
                    )
                }
            }

            item(key = "bottom_space") { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun TrainerJourneyHero(
    game: AppGame,
    nextStep: JourneyStep?,
    completed: Int,
    total: Int,
    progress: Float,
    onContinue: () -> Unit
) {
    val accent = PokedexDesignTokens.Colors.game(game.label)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(272.dp).background(accent.copy(alpha = .22f))
        ) {
            AsyncImage(
                model = rememberOfflineArtworkModel(GameCoverCatalog.heroFor(game.label) ?: GameCoverCatalog.primaryCoverFor(game.label)),
                contentDescription = GameCoverCatalog.displayNameFor(game.label),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = .04f), Color.Black.copy(alpha = .20f), Color.Black.copy(alpha = .88f))
                    )
                )
            )
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = .68f)
            ) {
                Text(
                    "CONTINUAR JORNADA",
                    Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp)) {
                Text(
                    GameCoverCatalog.displayNameFor(game.label),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    nextStep?.let { "Próximo: ${it.title}" } ?: "Jornada principal concluída",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = .88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(999.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = .20f)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text("$completed/$total", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().padding(top = 11.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF17212B))
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Continuar", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun TrainerEmptyJourneyCard(onOpenGames: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .62f)
    ) {
        Column(Modifier.padding(18.dp)) {
            Icon(Icons.Default.Explore, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Sua próxima aventura começa aqui", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                "Escolha um jogo para iniciar uma Jornada e acompanhar cada objetivo.",
                Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onOpenGames, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Text("Escolher jogo", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DailyPokemonCard(
    pokemonId: Int,
    pokemonName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(194.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .58f)
    ) {
        Column(Modifier.padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(5.dp))
                Text("Pokémon do dia", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            PokemonArtwork(
                model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$pokemonId.png",
                contentDescription = pokemonName,
                pokemonId = pokemonId,
                modifier = Modifier.padding(top = 4.dp).size(100.dp),
                contentScale = ContentScale.Fit
            )
            Text(pokemonName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("#${pokemonId.toString().padStart(4, '0')}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DailyMissionCard(
    title: String,
    supporting: String,
    progress: Int,
    target: Int,
    ratio: Float,
    modifier: Modifier = Modifier
) {
    val done = progress >= target
    Surface(
        modifier = modifier.height(194.dp),
        shape = RoundedCornerShape(22.dp),
        color = if (done) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .70f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .68f)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .72f)) {
                    Icon(
                        if (done) Icons.Default.CheckCircle else Icons.Default.TrackChanges,
                        null,
                        Modifier.padding(9.dp).size(21.dp),
                        tint = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    if (done) "Missão concluída" else "Missão diária",
                    Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(title, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                Text(supporting, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column {
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))
                )
                Text("${progress.coerceAtMost(target)}/$target", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TrainerMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Surface(
        modifier = cardModifier.height(92.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .56f)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RecentPokemonCard(
    pokemonId: Int,
    pokemonName: String,
    captured: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.width(112.dp).height(142.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .56f)
    ) {
        Box {
            Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PokemonArtwork(
                    model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$pokemonId.png",
                    contentDescription = pokemonName,
                    pokemonId = pokemonId,
                    modifier = Modifier.size(82.dp),
                    contentScale = ContentScale.Fit
                )
                Text(pokemonName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("#${pokemonId.toString().padStart(4, '0')}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (captured) {
                Icon(
                    Icons.Default.CatchingPokemon,
                    contentDescription = "Registrado",
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun UpcomingJourneyCard(
    step: JourneyStep,
    current: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (current) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .62f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .46f)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .78f)) {
                Icon(
                    when (step.kind) {
                        JourneyChallengeKind.GYM -> Icons.Default.EmojiEvents
                        JourneyChallengeKind.TITAN -> Icons.Default.Landscape
                        JourneyChallengeKind.STAR -> Icons.Default.Groups
                        else -> Icons.Default.Flag
                    },
                    null,
                    Modifier.padding(10.dp).size(19.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                Text(if (current) "AGORA" else "DEPOIS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Text(step.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(step.location, step.levelLabel).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Abrir Jornada", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TrainerSectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
