package com.otaviobarreto.pokedex.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener
import kotlinx.coroutines.delay

internal object Companion3DContract {
    const val MODEL_ASSET = "models/pikachu_companion.glb"
    const val IDLE = "Idle"
    const val PET = "Pet"
    const val CALL = "Call"
    const val BERRY = "EatBerry"
    const val PLAY = "Play"
    const val TAP = "Happy"

    val requiredAnimationNames = listOf(IDLE, PET, CALL, BERRY, PLAY, TAP)
}

private enum class CompanionReaction(
    val preferredClips: List<String>,
    val status: String,
    val mood: String,
    val settleMillis: Long
) {
    IDLE(listOf("Idle", "idle", "Breath", "breath"), "Observando você.", "Curioso", Long.MAX_VALUE),
    PET(listOf("Pet", "pet", "Happy", "happy"), "Gostou do carinho.", "Feliz", 1500),
    CALL(listOf("Call", "call", "Look", "look"), "Olhou para você.", "Atento", 1350),
    BERRY(listOf("EatBerry", "eat_berry", "Eat", "eat"), "Adorou a Berry.", "Feliz", 2100),
    PLAY(listOf("Play", "play", "Jump", "jump"), "Quer brincar mais.", "Empolgado", 1800),
    TAP(listOf("Happy", "happy", "Tap", "tap"), "Reagiu ao seu toque.", "Feliz", 1350)
}

private fun Context.hasAsset(path: String): Boolean =
    runCatching {
        assets.open(path).use { }
        true
    }.getOrDefault(false)

@Composable
internal fun PokemonLivingCompanionCard(
    gameLabel: String,
    pokemonId: Int,
    pokemonName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val modelAvailable = remember { context.hasAsset(Companion3DContract.MODEL_ASSET) }
    val accent = PokedexDesignTokens.Colors.game(gameLabel)
    var reaction by rememberSaveable { mutableStateOf(CompanionReaction.IDLE.name) }
    var affinity by rememberSaveable { mutableIntStateOf(55) }
    val activeReaction = remember(reaction) {
        runCatching { CompanionReaction.valueOf(reaction) }.getOrDefault(CompanionReaction.IDLE)
    }

    LaunchedEffect(activeReaction) {
        if (activeReaction != CompanionReaction.IDLE) {
            delay(activeReaction.settleMillis)
            reaction = CompanionReaction.IDLE.name
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(PokedexDesignTokens.Radius.Xl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(390.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = .22f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = .42f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Box(
                Modifier
                    .size(240.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 72.dp, y = (-72).dp)
                    .background(accent.copy(alpha = .10f), CircleShape)
            )
            Box(
                Modifier
                    .size(180.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-80).dp, y = 34.dp)
                    .background(accent.copy(alpha = .08f), CircleShape)
            )

            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp)
                    .widthIn(max = 190.dp)
            ) {
                Text(
                    "COMPANHEIRO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = accent
                )
                Text(
                    pokemonName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    activeReaction.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (modelAvailable) {
                PokemonCompanionScene(
                    reaction = activeReaction,
                    onPokemonTap = {
                        reaction = CompanionReaction.TAP.name
                        affinity = (affinity + 2).coerceAtMost(100)
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(top = 18.dp)
                )
            } else {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(top = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PokemonArtwork(
                        model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$pokemonId.png",
                        contentDescription = pokemonName,
                        modifier = Modifier.size(215.dp),
                        pokemonId = pokemonId
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp, top = 22.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .86f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CatchingPokemon, null, Modifier.size(15.dp), tint = accent)
                        Spacer(Modifier.width(5.dp))
                        Text("3D pronto para receber o GLB", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompanionActionChip(
                        label = "Carinho",
                        icon = Icons.Default.Pets,
                        selected = activeReaction == CompanionReaction.PET,
                        onClick = {
                            reaction = CompanionReaction.PET.name
                            affinity = (affinity + 3).coerceAtMost(100)
                        }
                    )
                    CompanionActionChip(
                        label = "Chamar",
                        icon = Icons.Default.WavingHand,
                        selected = activeReaction == CompanionReaction.CALL,
                        onClick = {
                            reaction = CompanionReaction.CALL.name
                            affinity = (affinity + 1).coerceAtMost(100)
                        }
                    )
                    CompanionActionChip(
                        label = "Dar Berry",
                        icon = Icons.Default.Restaurant,
                        selected = activeReaction == CompanionReaction.BERRY,
                        onClick = {
                            reaction = CompanionReaction.BERRY.name
                            affinity = (affinity + 4).coerceAtMost(100)
                        }
                    )
                    CompanionActionChip(
                        label = "Brincar",
                        icon = Icons.Default.Favorite,
                        selected = activeReaction == CompanionReaction.PLAY,
                        onClick = {
                            reaction = CompanionReaction.PLAY.name
                            affinity = (affinity + 2).coerceAtMost(100)
                        }
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Humor · " + activeReaction.mood,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { affinity / 100f },
                            modifier = Modifier.fillMaxWidth().padding(top = 5.dp).height(5.dp),
                            color = accent
                        )
                    }
                    Text(
                        "$affinity%",
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun PokemonCompanionScene(
    reaction: CompanionReaction,
    onPokemonTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(y = .72f, z = 3.15f)
    }
    val modelNode = rememberNode {
        ModelNode(
            modelInstance = modelLoader.createModelInstance(
                assetFileLocation = Companion3DContract.MODEL_ASSET
            ),
            autoAnimate = false,
            scaleToUnits = 1.65f,
            centerOrigin = Position(0f, -1f, 0f)
        )
    }
    val animationNames = remember(modelNode) {
        (0 until modelNode.animationCount).map { modelNode.animator.getAnimationName(it) }
    }
    val animationName = remember(reaction, animationNames) {
        reaction.preferredClips.firstNotNullOfOrNull { candidate ->
            animationNames.firstOrNull { it.equals(candidate, ignoreCase = true) }
        } ?: animationNames.firstOrNull()
    }

    LaunchedEffect(modelNode, animationName, reaction) {
        modelNode.playingAnimations.keys.toList().forEach(modelNode::stopAnimation)
        animationName?.let {
            modelNode.playAnimation(
                animationName = it,
                loop = reaction == CompanionReaction.IDLE
            )
        }
    }

    Scene(
        modifier = modifier.clip(RoundedCornerShape(28.dp)),
        engine = engine,
        modelLoader = modelLoader,
        isOpaque = false,
        cameraNode = cameraNode,
        cameraManipulator = null,
        childNodes = listOf(modelNode),
        onGestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { _, node ->
                if (node != null) onPokemonTap()
            }
        )
    )
}

@Composable
private fun CompanionActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.Bold) },
        leadingIcon = { Icon(icon, null, Modifier.size(17.dp)) }
    )
}
