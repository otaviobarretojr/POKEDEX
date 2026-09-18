package com.otaviobarreto.pokedex.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import kotlinx.coroutines.delay

internal object Companion3DContract {
    const val MODEL_ASSET = "models/pikachu_companion/Pikachu_companion_normalized.gltf"
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
    val settleMillis: Long,
    val speech: String
) {
    IDLE(listOf("Idle", "idle", "Breath", "breath"), "Observando você.", "Curioso", Long.MAX_VALUE, "Pika!"),
    PET(listOf("Cute", "Cute 2", "Pet", "pet", "Happy", "happy"), "Gostou do carinho.", "Feliz", 1500, "Pika pika!"),
    CALL(listOf("Static-Cute", "Static", "Call", "call", "Look", "look"), "Olhou para você.", "Atento", 1350, "Pika?"),
    BERRY(listOf("CuteAction", "Cute", "EatBerry", "eat_berry", "Eat", "eat"), "Adorou a Berry.", "Feliz", 2100, "Pikachu!"),
    PLAY(listOf("Jump", "Run", "Play", "play", "jump"), "Quer brincar mais.", "Empolgado", 1800, "Pika!!"),
    TAP(listOf("Cute 2", "Cute", "Happy", "happy", "Tap", "tap"), "Reagiu ao seu toque.", "Feliz", 1350, "Pika! ♡")
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
    pokemonId: Int,
    pokemonName: String,
    modifier: Modifier = Modifier,
    immersive: Boolean = false
) {
    val context = LocalContext.current
    val hasModelAssets = remember {
        context.hasAsset(Companion3DContract.MODEL_ASSET) &&
            context.hasAsset(Companion3DContract.MODEL_BUFFER_ASSET)
    }
    val supports3D = remember { context.supportsCompanion3D() }
    val modelAvailable = hasModelAssets && supports3D
    val accent = MaterialTheme.colorScheme.primary

    var reaction by rememberSaveable { mutableStateOf(CompanionReaction.IDLE.name) }
    var affinity by rememberSaveable { mutableIntStateOf(72) }
    val activeReaction = remember(reaction) {
        runCatching { CompanionReaction.valueOf(reaction) }.getOrDefault(CompanionReaction.IDLE)
    }

    fun react(target: CompanionReaction, gain: Int) {
        reaction = target.name
        affinity = (affinity + gain).coerceAtMost(100)
    }

    LaunchedEffect(activeReaction) {
        if (activeReaction != CompanionReaction.IDLE) {
            delay(activeReaction.settleMillis)
            reaction = CompanionReaction.IDLE.name
        }
    }

    val cardShape = if (immersive) RoundedCornerShape(0.dp) else RoundedCornerShape(PokedexDesignTokens.Radius.Xl)
    Card(
        modifier = modifier,
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (immersive) 0.dp else PokedexDesignTokens.Elevation.Low
        )
    ) {
        val stageModifier = if (immersive) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxWidth().height(510.dp)
        }

        Box(stageModifier.clip(cardShape)) {
            CompanionStaticBackdrop(
                accent = accent,
                modifier = Modifier.fillMaxSize()
            )

            val sceneModifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(.72f)
                .fillMaxHeight(.70f)
                .padding(top = 58.dp, end = 4.dp, bottom = 78.dp)

            if (modelAvailable) {
                PokemonCompanionScene(
                    reaction = activeReaction,
                    modifier = sceneModifier
                )
            } else {
                Box(
                    modifier = sceneModifier,
                    contentAlignment = Alignment.Center
                ) {
                    PokemonArtwork(
                        model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$pokemonId.png",
                        contentDescription = pokemonName,
                        modifier = Modifier.size(208.dp),
                        pokemonId = pokemonId
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color(0x160D1720),
                                Color(0x4A0D1720)
                            )
                        )
                    )
            )

            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 14.dp)
                    .widthIn(max = 190.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xD924303A)
                ) {
                    Text(
                        "COMPANION",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD54A)
                    )
                }
                Text(
                    pokemonName,
                    modifier = Modifier.padding(top = 7.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF17212B)
                )
                Text(
                    activeReaction.status,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xC91D2A35)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 11.dp, top = 84.dp, bottom = 126.dp)
                    .width(106.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompanionActionButton(
                    label = "Carinho",
                    icon = Icons.Default.Pets,
                    selected = activeReaction == CompanionReaction.PET,
                    accent = accent
                ) { react(CompanionReaction.PET, 3) }
                CompanionActionButton(
                    label = "Chamar",
                    icon = Icons.Default.WavingHand,
                    selected = activeReaction == CompanionReaction.CALL,
                    accent = accent
                ) { react(CompanionReaction.CALL, 1) }
                CompanionActionButton(
                    label = "Berry",
                    icon = Icons.Default.Restaurant,
                    selected = activeReaction == CompanionReaction.BERRY,
                    accent = accent
                ) { react(CompanionReaction.BERRY, 4) }
                CompanionActionButton(
                    label = "Brincar",
                    icon = Icons.Default.Favorite,
                    selected = activeReaction == CompanionReaction.PLAY,
                    accent = accent
                ) { react(CompanionReaction.PLAY, 2) }
            }

            if (modelAvailable) {
                Box(
                    sceneModifier.clickable { react(CompanionReaction.TAP, 2) }
                )
            } else {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 18.dp, end = 14.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xD924303A)
                ) {
                    Row(
                        Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CatchingPokemon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFFFD54A)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (hasModelAssets) "Compatibilidade 2D" else "Modelo indisponível",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            CompanionSpeechBubble(
                pokemonName = pokemonName,
                speech = activeReaction.speech,
                accent = accent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 126.dp, end = 16.dp)
            )

            CompanionAffinityPanel(
                mood = activeReaction.mood,
                affinity = affinity,
                accent = accent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun CompanionStaticBackdrop(
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF76C8F5),
                        Color(0xFFBDE7FA),
                        Color(0xFF8EAAB5),
                        Color(0xFF35424B)
                    )
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = .26f),
                        Color.Transparent
                    ),
                    center = Offset(760f, 220f),
                    radius = 620f
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                color = Color.White.copy(alpha = .16f),
                radius = w * .24f,
                center = Offset(w * .79f, h * .13f)
            )
            drawCircle(
                color = accent.copy(alpha = .07f),
                radius = w * .34f,
                center = Offset(w * .79f, h * .13f),
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = Color.White.copy(alpha = .09f),
                radius = w * .44f,
                center = Offset(w * .79f, h * .13f),
                style = Stroke(width = 2f)
            )

            drawOval(
                color = Color(0xFF101820).copy(alpha = .17f),
                topLeft = Offset(w * .47f, h * .71f),
                size = Size(w * .39f, h * .055f)
            )
            drawOval(
                color = Color.White.copy(alpha = .08f),
                topLeft = Offset(w * .51f, h * .705f),
                size = Size(w * .31f, h * .035f)
            )

            val sparkles = listOf(
                Offset(w * .11f, h * .18f),
                Offset(w * .21f, h * .12f),
                Offset(w * .91f, h * .33f),
                Offset(w * .84f, h * .42f)
            )
            sparkles.forEachIndexed { index, point ->
                val r = if (index % 2 == 0) 3.5f else 2.2f
                drawCircle(Color.White.copy(alpha = .34f), radius = r, center = point)
            }
        }
    }
}

@Composable
private fun CompanionActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) {
            accent.copy(alpha = .28f)
        } else {
            Color(0xDE202A34)
        },
        tonalElevation = if (selected) {
            PokedexDesignTokens.Elevation.Medium
        } else {
            PokedexDesignTokens.Elevation.Low
        }
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) Color.White else Color(0xFFD6DEE8)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CompanionSpeechBubble(
    pokemonName: String,
    speech: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(min = 108.dp, max = 142.dp),
        shape = RoundedCornerShape(19.dp),
        color = Color(0xE6212A34),
        tonalElevation = PokedexDesignTokens.Elevation.Medium
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    pokemonName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFBCC7D4)
                )
                Text(
                    speech,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = accent
            )
        }
    }
}

@Composable
private fun CompanionAffinityPanel(
    mood: String,
    affinity: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xEC151E28),
        tonalElevation = PokedexDesignTokens.Elevation.Medium
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = .18f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 9.dp, end = 10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Humor",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB7C2CF)
                        )
                        Text(
                            mood,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                    }
                    Text(
                        "$affinity%",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                LinearProgressIndicator(
                    progress = { affinity / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                        .height(5.dp),
                    color = accent,
                    trackColor = Color.White.copy(alpha = .08f)
                )
                Text(
                    "Afinidade",
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB7C2CF)
                )
            }
        }
    }
}

@Composable
private fun PokemonCompanionScene(
    reaction: CompanionReaction,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0f, y = .95f, z = 4.20f)
    }

    val modelNode = rememberNode {
        ModelNode(
            modelInstance = modelLoader.createModelInstance(
                assetFileLocation = Companion3DContract.MODEL_ASSET
            ),
            autoAnimate = false,
            scaleToUnits = 1.70f,
            centerOrigin = null
        ).apply {
            position = Position(x = -.10f, y = .64f, z = .04f)
            rotation = Rotation(y = 180f)
        }
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
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        isOpaque = false,
        cameraNode = cameraNode,
        cameraManipulator = null,
        childNodes = listOf(modelNode)
    )
}
