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
    const val MODEL_ASSET = "models/pikachu_companion/Pikachu_companion_embedded.gltf"
    const val PALDEA_WORLD_ASSET = "models/companion_worlds/paldea_companion_world.gltf"
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
    modifier: Modifier = Modifier,
    regionSource: String? = null,
    immersive: Boolean = false
) {
    val context = LocalContext.current
    val hasModelAssets = remember { context.hasAsset(Companion3DContract.MODEL_ASSET) }
    val supports3D = remember { context.supportsCompanion3D() }
    val modelAvailable = hasModelAssets && supports3D
    val accent = PokedexDesignTokens.Colors.game(gameLabel)
    val worldKind = remember(gameLabel, regionSource) { companionWorldKind(gameLabel, regionSource) }
    val world3DAsset = remember(worldKind) { companionWorld3DAsset(worldKind) }
    val hasWorld3DAsset = remember(world3DAsset) { world3DAsset?.let(context::hasAsset) == true }
    val sceneLabel = remember(worldKind) { companionSceneLabel(worldKind) }
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
        elevation = CardDefaults.cardElevation(defaultElevation = if (immersive) 0.dp else PokedexDesignTokens.Elevation.Low)
    ) {
        val stageModifier = if (immersive) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(510.dp)
        Box(stageModifier.clip(cardShape)) {
            CompanionWorldBackdrop(
                worldKind = worldKind,
                accent = accent,
                modifier = Modifier.fillMaxSize()
            )

            if (modelAvailable) {
                PokemonCompanionScene(
                    reaction = activeReaction,
                    worldAsset = world3DAsset.takeIf { hasWorld3DAsset },
                    modifier = Modifier.fillMaxSize()
                )
            }

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
                    companionWorldBadge(worldKind),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 11.dp, top = 72.dp, bottom = 96.dp)
                    .width(112.dp),
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
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(.62f)
                        .fillMaxHeight(.68f)
                        .padding(top = 52.dp, bottom = 84.dp)
                        .clickable { react(CompanionReaction.TAP, 2) }
                )
            } else {
                Box(
                    if (immersive) {
                        Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxWidth(.62f)
                            .fillMaxHeight(.66f)
                            .padding(top = 46.dp, end = 4.dp, bottom = 68.dp)
                    } else {
                        Modifier
                            .align(Alignment.CenterEnd)
                            .width(260.dp)
                            .height(300.dp)
                            .padding(top = 58.dp, end = 4.dp, bottom = 42.dp)
                    },
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

private enum class CompanionWorldKind {
    LUMIOSE, HYPERSPACE, PALDEA, KITAKAMI, BLUEBERRY,
    GALAR, ISLE_ARMOR, CROWN_TUNDRA, HISUI, KANTO_LETS_GO,
    SINNOH, KANTO_GBA
}

private fun companionWorldKind(gameLabel: String, regionSource: String?): CompanionWorldKind {
    val game = gameLabel.lowercase()
    val region = regionSource.orEmpty().lowercase()
    return when {
        game.contains("z-a") && region.contains("hyperspace") -> CompanionWorldKind.HYPERSPACE
        game.contains("z-a") -> CompanionWorldKind.LUMIOSE
        game.contains("scarlet") && region.contains("kitakami") -> CompanionWorldKind.KITAKAMI
        game.contains("scarlet") && region.contains("blueberry") -> CompanionWorldKind.BLUEBERRY
        game.contains("scarlet") -> CompanionWorldKind.PALDEA
        game.contains("sword") && region.contains("armor") -> CompanionWorldKind.ISLE_ARMOR
        game.contains("sword") && region.contains("tundra") -> CompanionWorldKind.CROWN_TUNDRA
        game.contains("sword") -> CompanionWorldKind.GALAR
        game.contains("arceus") -> CompanionWorldKind.HISUI
        game.contains("let's go") || game.contains("lets go") -> CompanionWorldKind.KANTO_LETS_GO
        game.contains("diamond") || game.contains("pearl") -> CompanionWorldKind.SINNOH
        else -> CompanionWorldKind.KANTO_GBA
    }
}

private fun companionSceneLabel(kind: CompanionWorldKind): String = when (kind) {
    CompanionWorldKind.LUMIOSE -> "LUMIOSE CITY"
    CompanionWorldKind.HYPERSPACE -> "HYPERSPACE LUMIOSE"
    CompanionWorldKind.PALDEA -> "PALDEA"
    CompanionWorldKind.KITAKAMI -> "KITAKAMI"
    CompanionWorldKind.BLUEBERRY -> "BLUEBERRY ACADEMY"
    CompanionWorldKind.GALAR -> "WILD AREA · GALAR"
    CompanionWorldKind.ISLE_ARMOR -> "ISLE OF ARMOR"
    CompanionWorldKind.CROWN_TUNDRA -> "CROWN TUNDRA"
    CompanionWorldKind.HISUI -> "HISUI"
    CompanionWorldKind.KANTO_LETS_GO -> "KANTO"
    CompanionWorldKind.SINNOH -> "SINNOH"
    CompanionWorldKind.KANTO_GBA -> "KANTO · GBA"
}

private fun companionWorldBadge(kind: CompanionWorldKind): String = when (kind) {
    CompanionWorldKind.LUMIOSE -> "☀  Lumiose"
    CompanionWorldKind.HYPERSPACE -> "✦  Hyperspace"
    CompanionWorldKind.PALDEA -> "☀  Paldea"
    CompanionWorldKind.KITAKAMI -> "☀  Kitakami"
    CompanionWorldKind.BLUEBERRY -> "◌  Terarium"
    CompanionWorldKind.GALAR -> "☁  Galar"
    CompanionWorldKind.ISLE_ARMOR -> "☀  Isle of Armor"
    CompanionWorldKind.CROWN_TUNDRA -> "❄  Crown Tundra"
    CompanionWorldKind.HISUI -> "☀  Hisui"
    CompanionWorldKind.KANTO_LETS_GO -> "☀  Kanto"
    CompanionWorldKind.SINNOH -> "☀  Sinnoh"
    CompanionWorldKind.KANTO_GBA -> "▦  Kanto"
}

private fun companionWorld3DAsset(kind: CompanionWorldKind): String? = when (kind) {
    CompanionWorldKind.PALDEA -> Companion3DContract.PALDEA_WORLD_ASSET
    else -> null
}

private data class CompanionWorldPalette(
    val skyTop: Color,
    val skyBottom: Color,
    val ground: Color,
    val landmark: Color,
    val foliage: Color
)

private fun companionWorldPalette(kind: CompanionWorldKind): CompanionWorldPalette = when (kind) {
    CompanionWorldKind.LUMIOSE -> CompanionWorldPalette(Color(0xFF64B5F6), Color(0xFFD8ECF5), Color(0xFF697980), Color(0xFF334A5D), Color(0xFF5F946A))
    CompanionWorldKind.HYPERSPACE -> CompanionWorldPalette(Color(0xFF372B76), Color(0xFF8A6ED9), Color(0xFF34304C), Color(0xFF78E2FF), Color(0xFF7257A7))
    CompanionWorldKind.PALDEA -> CompanionWorldPalette(Color(0xFF72C8FF), Color(0xFFDDF3FF), Color(0xFF79A95B), Color(0xFFD6A55F), Color(0xFF4D8E4D))
    CompanionWorldKind.KITAKAMI -> CompanionWorldPalette(Color(0xFF8CC5D6), Color(0xFFE7E4C3), Color(0xFF78945B), Color(0xFF7B5541), Color(0xFF4F7C52))
    CompanionWorldKind.BLUEBERRY -> CompanionWorldPalette(Color(0xFF4F91BB), Color(0xFFBFE9E8), Color(0xFF5B927B), Color(0xFFDAF4F1), Color(0xFF3E7E6D))
    CompanionWorldKind.GALAR -> CompanionWorldPalette(Color(0xFF7397B2), Color(0xFFD8E1E3), Color(0xFF62835F), Color(0xFF536575), Color(0xFF466B49))
    CompanionWorldKind.ISLE_ARMOR -> CompanionWorldPalette(Color(0xFF58CBE4), Color(0xFFD6F4E8), Color(0xFFCAA96A), Color(0xFF7B6548), Color(0xFF3F8E62))
    CompanionWorldKind.CROWN_TUNDRA -> CompanionWorldPalette(Color(0xFF8BAFC8), Color(0xFFEAF4F8), Color(0xFFD7E4EA), Color(0xFF71869A), Color(0xFF708A89))
    CompanionWorldKind.HISUI -> CompanionWorldPalette(Color(0xFF7FAEC2), Color(0xFFE1D8B9), Color(0xFF75845A), Color(0xFF666356), Color(0xFF4F6E48))
    CompanionWorldKind.KANTO_LETS_GO -> CompanionWorldPalette(Color(0xFF7CCEFF), Color(0xFFF0F4CF), Color(0xFF82B765), Color(0xFFB27D57), Color(0xFF4D9851))
    CompanionWorldKind.SINNOH -> CompanionWorldPalette(Color(0xFF7CB9E8), Color(0xFFE5F0F6), Color(0xFF719B69), Color(0xFF7A8793), Color(0xFF4F7C56))
    CompanionWorldKind.KANTO_GBA -> CompanionWorldPalette(Color(0xFF86C8A5), Color(0xFFCFE4B3), Color(0xFF80A75A), Color(0xFF5D6D4B), Color(0xFF3F7745))
}

@Composable
private fun CompanionWorldBackdrop(
    worldKind: CompanionWorldKind,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = remember(worldKind) { companionWorldPalette(worldKind) }
    Box(
        modifier.background(
            Brush.verticalGradient(
                listOf(palette.skyTop, palette.skyBottom, palette.ground.copy(alpha = .28f))
            )
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = Color.White.copy(alpha = .18f),
                radius = w * .23f,
                center = Offset(w * .78f, h * .12f)
            )
            drawCircle(
                color = accent.copy(alpha = .06f),
                radius = w * .34f,
                center = Offset(w * .78f, h * .12f)
            )
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
    worldAsset: String?,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(y = .90f, z = 4.20f)
    }

    val worldNode = worldAsset?.let { asset ->
        rememberNode {
            ModelNode(
                modelInstance = modelLoader.createModelInstance(assetFileLocation = asset),
                autoAnimate = false
            )
        }
    }

    val modelNode = rememberNode {
        ModelNode(
            modelInstance = modelLoader.createModelInstance(
                assetFileLocation = Companion3DContract.MODEL_ASSET
            ),
            autoAnimate = false,
            scaleToUnits = 1.85f,
            centerOrigin = null,
            // The bundled source model has an off-centre authored pivot.
            // This translation is derived from its normalized AABB after removing
            // the broken SceneView 2.3 bottom-origin transform.
            position = Position(x = -.25f, y = .16f, z = .30f)
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
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        isOpaque = false,
        cameraNode = cameraNode,
        cameraManipulator = null,
        childNodes = listOfNotNull(worldNode, modelNode)
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
