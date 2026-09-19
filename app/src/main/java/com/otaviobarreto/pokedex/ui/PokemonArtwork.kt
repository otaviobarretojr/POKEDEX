package com.otaviobarreto.pokedex.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.otaviobarreto.pokedex.data.OfflineLibraryManager
import kotlin.math.max
import kotlin.math.min

/**
 * Crops transparent borders from official artwork before Compose fits it into
 * the fixed hero safe-area. This keeps visually asymmetric Pokémon centered
 * without changing the card size or stretching the artwork.
 */
class TransparentBoundsCropTransformation(
    private val alphaThreshold: Int = 8,
    private val paddingRatio: Float = 0.035f
) : Transformation {
    override val cacheKey: String = "transparent-bounds-crop-v1-$alphaThreshold-$paddingRatio"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (input.width <= 1 || input.height <= 1) return input

        val source = if (input.config == Bitmap.Config.HARDWARE) {
            input.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            input
        }

        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (Color.alpha(pixels[row + x]) > alphaThreshold) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) return input

        val visibleW = maxX - minX + 1
        val visibleH = maxY - minY + 1
        val pad = max(2, (max(visibleW, visibleH) * paddingRatio).toInt())

        val left = max(0, minX - pad)
        val top = max(0, minY - pad)
        val right = min(width - 1, maxX + pad)
        val bottom = min(height - 1, maxY + pad)
        val cropW = right - left + 1
        val cropH = bottom - top + 1

        if (left == 0 && top == 0 && cropW == width && cropH == height) return input
        return Bitmap.createBitmap(source, left, top, cropW, cropH)
    }
}

@Composable
fun PokemonArtwork(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    pokemonId: Int? = null,
    contentScale: ContentScale = ContentScale.Fit,
    colorFilter: ColorFilter? = null,
    cropTransparentBounds: Boolean = false
) {
    val context = LocalContext.current
    val tuning = remember(pokemonId) { ArtworkTuningCatalog.forPokemon(pokemonId) }
    // Explicit variant artwork (notably Shiny) must win over the generic
    // offline species artwork. Otherwise pokemonId resolves the normal image
    // and silently replaces the Shiny URL supplied by collection screens.
    val explicitVariantArtwork = remember(model) {
        (model as? String)?.let { url ->
            "/shiny/" in url || "?shiny=" in url || "-shiny." in url
        } == true
    }
    val localModel = remember(model,pokemonId,explicitVariantArtwork) {
        if (explicitVariantArtwork) {
            (model as? String)?.let { OfflineLibraryManager.resolveAny(context,it) } ?: model
        } else {
            pokemonId?.let{OfflineLibraryManager.resolveAny(context,"pokemon-offline-$it")}
                ?: (model as? String)?.let{OfflineLibraryManager.resolveAny(context,it)}
                ?: model
        }
    }
    val request = remember(localModel,pokemonId,cropTransparentBounds,explicitVariantArtwork) {
        ImageRequest.Builder(context)
            .data(localModel)
            .apply {
                if (!explicitVariantArtwork) {
                    pokemonId?.let { id ->
                        diskCacheKey("pokemon-offline-$id")
                        memoryCacheKey("pokemon-offline-$id")
                    }
                }
            }
            .crossfade(false)
            .apply {
                if (cropTransparentBounds) transformations(TransparentBoundsCropTransformation())
            }
            .build()
    }

    BoxWithConstraints(modifier) {
        val dx = maxWidth * tuning.offsetX
        val dy = maxHeight * tuning.offsetY
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .offset(x = dx, y = dy)
                .graphicsLayer {
                    scaleX = tuning.scale
                    scaleY = tuning.scale
                },
            contentScale = contentScale,
            alignment = alignment,
            colorFilter = colorFilter
        )
    }
}