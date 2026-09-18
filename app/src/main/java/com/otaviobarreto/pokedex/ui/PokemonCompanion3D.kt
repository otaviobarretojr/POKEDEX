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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val sceneLabel = remember(gameLabel) { companionSceneLabel(gameLabel) }
    val isLumiose = remember(gameLabel) {
        gameLabel.contains("Z-A", ignoreCase = true) || gameLabel.contains("Z A", ignoreCase = true)
    }
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

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(PokedexDesignTokens.Radius.Xl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = PokedexDesignTokens.Elevation.Low)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(510.dp)
                .clip(RoundedCornerShape(PokedexDesignTokens.Radius.Xl))
        ) {
            CompanionWorldBackdrop(
                accent = accent,
                lumiose = isLumiose,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = .34f),
                                MaterialTheme.colorScheme.surface.copy(alpha = .76f)
                            ),
                            startY = 0f
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .80f)
                ) {
                    Text(
                        sceneLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                }
                Text(
                    pokemonName,
                    modifier = Modifier.padding(top = 7.dp),
                    style = MaterialTheme.typography.headlineSmall,
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
                    .padding(12.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .78f)
            ) {
                Text(
                    if (isLumiose) "☀  Lumiose" else "☀  Em viagem",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 11.dp, top = 72.dp, bottom = 96.dp)
                    .width(128.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
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
                    label = "Dar Berry",
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
                PokemonCompanionScene(
                    reaction = activeReaction,
                    onPokemonTap = { react(CompanionReaction.TAP, 2) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(270.dp)
                        .height(330.dp)
                        .padding(top = 64.dp, end = 2.dp, bottom = 54.dp)
                )
            } else {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(260.dp)
                        .height(300.dp)
                        .padding(top = 58.dp, end = 4.dp, bottom = 42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PokemonArtwork(
                        model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$pokemonId.png",
                        contentDescription = pokemonName,
                        modifier = Modifier.size(220.dp),
                        pokemonId = pokemonId
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp, bottom = 104.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .84f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CatchingPokemon, null, Modifier.size(14.dp), tint = accent)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (hasModelAssets) "Modo 2D neste dispositivo" else "Modelo 3D indisponível",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            CompanionSpeechBubble(
                pokemonName = pokemonName,
                speech = activeReaction.speech,
                accent = accent,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp, bottom = 188.dp)
            )

            CompanionAffinityPanel(
                mood = activeReaction.mood,
                affinity = affinity,
                accent = accent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
    }
}

private fun companionSceneLabel(gameLabel: String): String = when {
    gameLabel.contains("Z-A", ignoreCase = true) -> "LUMIOSE CITY"
    gameLabel.contains("Scarlet", ignoreCase = true) -> "PALDEA"
    gameLabel.contains("Sword", ignoreCase = true) -> "GALAR"
    gameLabel.contains("Arceus", ignoreCase = true) -> "HISUI"
    gameLabel.contains("Let's Go", ignoreCase = true) -> "KANTO"
    gameLabel.contains("Diamond", ignoreCase = true) -> "SINNOH"
    else -> "COMPANHEIRO"
}

@Composable
private fun CompanionWorldBackdrop(
    accent: Color,
    lumiose: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF64B5F6),
                    Color(0xFFAEDCFF),
                    Color(0xFFD8ECF5),
                    Color(0xFF7D8B92)
                )
            )
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizon = h * .57f

            drawCircle(
                color = Color.White.copy(alpha = .24f),
                radius = w * .28f,
                center = Offset(w * .78f, h * .11f)
            )

            val buildingColor = Color(0xFF44515D).copy(alpha = .52f)
            val buildingLight = Color(0xFF647482).copy(alpha = .55f)
            val widths = floatArrayOf(.12f, .11f, .14f, .10f, .13f)
            var x = 0f
            widths.forEachIndexed { index, fraction ->
                val bw = w * fraction
                val bh = h * (.20f + (index % 3) * .045f)
                drawRect(
                    color = if (index % 2 == 0) buildingColor else buildingLight,
                    topLeft = Offset(x, horizon - bh),
                    size = Size(bw, bh)
                )
                x += bw * 1.04f
            }
            x = w
            widths.reversedArray().forEachIndexed { index, fraction ->
                val bw = w * fraction
                val bh = h * (.19f + (index % 3) * .05f)
                x -= bw
                drawRect(
                    color = if (index % 2 == 0) buildingLight else buildingColor,
                    topLeft = Offset(x, horizon - bh),
                    size = Size(bw, bh)
                )
                x -= bw * .04f
            }

            if (lumiose) {
                val tower = Path().apply {
                    moveTo(w * .50f, h * .08f)
                    lineTo(w * .466f, horizon * .74f)
                    lineTo(w * .448f, horizon)
                    lineTo(w * .552f, horizon)
                    lineTo(w * .534f, horizon * .74f)
                    close()
                }
                drawPath(tower, Color(0xFF2D4051).copy(alpha = .78f))
                drawLine(
                    color = accent.copy(alpha = .92f),
                    start = Offset(w * .50f, h * .12f),
                    end = Offset(w * .50f, horizon * .93f),
                    strokeWidth = 5f
                )
                drawCircle(
                    color = accent.copy(alpha = .95f),
                    radius = 12f,
                    center = Offset(w * .50f, horizon * .72f),
                    style = Stroke(width = 3.5f)
                )
            }

            drawRect(
                color = Color(0xFF63727A).copy(alpha = .72f),
                topLeft = Offset(0f, horizon),
                size = Size(w, h - horizon)
            )
            drawLine(
                Color.White.copy(alpha = .26f),
                Offset(w * .50f, horizon),
                Offset(w * .17f, h),
                strokeWidth = 2f
            )
            drawLine(
                Color.White.copy(alpha = .26f),
                Offset(w * .50f, horizon),
                Offset(w * .83f, h),
                strokeWidth = 2f
            )
            drawLine(
                accent.copy(alpha = .20f),
                Offset(w * .50f, horizon),
                Offset(w * .50f, h),
                strokeWidth = 4f
            )

            listOf(
                Offset(w * .08f, horizon * .83f),
                Offset(w * .17f, horizon * .74f),
                Offset(w * .86f, horizon * .78f),
                Offset(w * .93f, horizon * .88f)
            ).forEachIndexed { index, center ->
                drawCircle(
                    color = if (index % 2 == 0) Color(0xFF4D7E58) else Color(0xFF65956B),
                    radius = w * .055f,
                    center = center
                )
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
            .heightIn(min = 46.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = .27f)
        else MaterialTheme.colorScheme.surface.copy(alpha = .82f),
        tonalElevation = if (selected) PokedexDesignTokens.Elevation.Medium else PokedexDesignTokens.Elevation.Low
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
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
        modifier = modifier.widthIn(min = 112.dp, max = 148.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
        tonalElevation = PokedexDesignTokens.Elevation.Medium
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    pokemonName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(speech, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            }
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
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
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
        tonalElevation = PokedexDesignTokens.Elevation.Medium
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = .18f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Favorite, null, tint = accent, modifier = Modifier.size(21.dp))
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Humor", style = MaterialTheme.typography.labelSmall)
                        Text(mood, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    }
                    Text("$affinity%", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                }
                LinearProgressIndicator(
                    progress = { affinity / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)
                )
                Text(
                    "Afinidade",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
