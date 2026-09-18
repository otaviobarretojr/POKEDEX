package com.otaviobarreto.pokedex.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Build
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.GameCoverCatalog
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
    const val MODEL_ASSET = "models/pikachu_companion/Pikachu_resize.gltf"
    const val MODEL_BUFFER_ASSET = "models/pikachu_companion/Pikachu_resize.bin"
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
    PET(listOf("Cute", "Cute 2", "Pet", "pet", "Happy", "happy"), "Gostou do carinho.", "Feliz", 1500),
    CALL(listOf("Static-Cute", "Static", "Call", "call", "Look", "look"), "Olhou para você.", "Atento", 1350),
    BERRY(listOf("CuteAction", "Cute", "EatBerry", "eat_berry", "Eat", "eat"), "Adorou a Berry.", "Feliz", 2100),
    PLAY(listOf("Jump", "Run", "Play", "play", "jump"), "Quer brincar mais.", "Empolgado", 1800),
    TAP(listOf("Cute 2", "Cute", "Happy", "happy", "Tap", "tap"), "Reagiu ao seu toque.", "Feliz", 1350)
}

private fun Context.hasAsset(path: String): Boolean =
    runCatching {
        assets.open(path).use { }
        true
    }.getOrDefault(false)

private fun Context.supportsCompanion3D(): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ?: return false
    val config = activityManager.deviceConfigurationInfo
    val fingerprint = Build.FINGERPRINT.lowercase()
    val model = Build.MODEL.lowercase()
    val product = Build.PRODUCT.lowercase()
    val hardware = Build.HARDWARE.lowercase()

    val isEmulator =
        fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator") ||
            model.contains("emulator") ||
            model.contains("sdk_gphone") ||
            product.contains("sdk_gphone") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu")

    return !isEmulator &&
        !activityManager.isLowRamDevice &&
        config.reqGlEsVersion >= 0x00030000
}

@Composable
internal fun PokemonLivingCompanionCard(
    gameLabel: String,
    pokemonId: Int,
    pokemonName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasModelAssets = remember {
        context.hasAsset(Companion3DContract.MODEL_ASSET) &&
            context.hasAsset(Companion3DContract.MODEL_BUFFER_ASSET)
    }
    val supports3D = remember { context.supportsCompanion3D() }
    val modelAvailable = hasModelAssets && supports3D
    val accent = PokedexDesignTokens.Colors.game(gameLabel)
    val heroArtwork = remember(gameLabel) { GameCoverCatalog.heroFor(gameLabel) }
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
                .height(448.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            heroArtwork?.let { artwork ->
                AsyncImage(
                    model = rememberOfflineArtworkModel(artwork),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = .28f
                )
            }
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = .20f),
                                MaterialTheme.colorScheme.surface.copy(alpha = .16f),
                                MaterialTheme.colorScheme.surface.copy(alpha = .94f)
                            )
                        )
                    )
            )
            Box(
                Modifier
                    .size(210.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 68.dp, y = (-68).dp)
                    .background(accent.copy(alpha = .12f), CircleShape)
            )

            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 18.dp, top = 16.dp)
            ) {
                Text(
                    "COMPANHEIRO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = accent
                )
                Text(
                    pokemonName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    activeReaction.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .86f),
                tonalElevation = 2.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = accent
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "$affinity%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black
                    )
                }
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
                        .height(310.dp)
                        .padding(top = 24.dp, bottom = 18.dp)
                )
            } else {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(top = 24.dp, bottom = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PokemonArtwork(
                        model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$pokemonId.png",
                        contentDescription = pokemonName,
                        modifier = Modifier.size(238.dp),
                        pokemonId = pokemonId
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp, top = 58.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .84f)
                ) {
                    Text(
                        if (hasModelAssets) "Modo 2D" else "3D indisponível",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .90f),
                tonalElevation = 3.dp
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 9.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        CompanionQuickAction(
                            label = "Carinho",
                            icon = Icons.Default.Pets,
                            selected = activeReaction == CompanionReaction.PET,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                reaction = CompanionReaction.PET.name
                                affinity = (affinity + 3).coerceAtMost(100)
                            }
                        )
                        CompanionQuickAction(
                            label = "Chamar",
                            icon = Icons.Default.WavingHand,
                            selected = activeReaction == CompanionReaction.CALL,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                reaction = CompanionReaction.CALL.name
                                affinity = (affinity + 1).coerceAtMost(100)
                            }
                        )
                        CompanionQuickAction(
                            label = "Berry",
                            icon = Icons.Default.Restaurant,
                            selected = activeReaction == CompanionReaction.BERRY,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                reaction = CompanionReaction.BERRY.name
                                affinity = (affinity + 4).coerceAtMost(100)
                            }
                        )
                        CompanionQuickAction(
                            label = "Brincar",
                            icon = Icons.Default.Favorite,
                            selected = activeReaction == CompanionReaction.PLAY,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                reaction = CompanionReaction.PLAY.name
                                affinity = (affinity + 2).coerceAtMost(100)
                            }
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Humor · " + activeReaction.mood,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 92.dp)
                        )
                        LinearProgressIndicator(
                            progress = { affinity / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                                .height(4.dp),
                            color = accent
                        )
                    }
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
private fun CompanionQuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = .82f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = .36f)
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(19.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
