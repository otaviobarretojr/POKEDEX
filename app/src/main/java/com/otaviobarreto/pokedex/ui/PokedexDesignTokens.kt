package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Central visual tokens for the redesign phase.
 *
 * Values intentionally mirror the current UI so v6.21.0 is structural only.
 * The next visual phase can evolve the app from this single source of truth.
 */
object PokedexDesignTokens {
    object Colors {
        val Primary = Color(0xFF5B55E7)
        val OnPrimary = Color.White
        val PrimaryContainer = Color(0xFFEAE8FB)
        val OnPrimaryContainer = Color(0xFF241F66)
        val Secondary = Color(0xFF263C8C)
        val OnSecondary = Color.White
        val SecondaryContainer = Color(0xFFE6EAFA)
        val OnSecondaryContainer = Color(0xFF18224F)
        val Tertiary = Color(0xFF159D9B)
        val OnTertiary = Color.White
        val Background = Color(0xFFF8F8FC)
        val OnBackground = Color(0xFF151426)
        val Surface = Color.White
        val OnSurface = Color(0xFF151426)
        val SurfaceVariant = Color(0xFFF1F0F8)
        val OnSurfaceVariant = Color(0xFF50566A)
        val Outline = Color(0xFF797A8D)
        val OutlineVariant = Color(0xFFD8D7E2)
        val Error = Color(0xFFB3261E)
    }

    object Spacing {
        val Xs = 4.dp
        val Sm = 8.dp
        val Md = 12.dp
        val Lg = 16.dp
        val Xl = 24.dp
        val Xxl = 32.dp
    }

    object Radius {
        val Xs = 10.dp
        val Sm = 14.dp
        val Md = 18.dp
        val Lg = 24.dp
        val Xl = 30.dp
    }

    object Elevation {
        val Flat = 0.dp
        val Low = 2.dp
        val Medium = 6.dp
        val High = 10.dp
    }

    val Shapes = Shapes(
        extraSmall = RoundedCornerShape(Radius.Xs),
        small = RoundedCornerShape(Radius.Sm),
        medium = RoundedCornerShape(Radius.Md),
        large = RoundedCornerShape(Radius.Lg),
        extraLarge = RoundedCornerShape(Radius.Xl)
    )

    val Typography = Typography()
}
