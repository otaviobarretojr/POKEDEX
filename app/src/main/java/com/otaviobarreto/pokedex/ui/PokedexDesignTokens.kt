package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PokedexDesignTokens {
    object Colors {
        val Primary = Color(0xFF5C50E6)
        val OnPrimary = Color.White
        val PrimaryContainer = Color(0xFFEAE8FF)
        val OnPrimaryContainer = Color(0xFF211A66)
        val Secondary = Color(0xFF2E4B92)
        val OnSecondary = Color.White
        val SecondaryContainer = Color(0xFFE5ECFF)
        val OnSecondaryContainer = Color(0xFF162651)
        val Tertiary = Color(0xFF0B9A92)
        val OnTertiary = Color.White
        val Background = Color(0xFFF6F7FB)
        val OnBackground = Color(0xFF141522)
        val Surface = Color(0xFFFFFFFF)
        val OnSurface = Color(0xFF141522)
        val SurfaceVariant = Color(0xFFEEF0F6)
        val OnSurfaceVariant = Color(0xFF555C70)
        val Outline = Color(0xFF7C8294)
        val OutlineVariant = Color(0xFFD7DBE5)
        val Error = Color(0xFFB3261E)

        val DarkPrimary = Color(0xFFC6C0FF)
        val DarkPrimaryContainer = Color(0xFF373079)
        val DarkBackground = Color(0xFF0E1119)
        val DarkSurface = Color(0xFF171B25)
        val DarkSurfaceVariant = Color(0xFF222735)
        val DarkOnSurface = Color(0xFFF2F4FA)
        val DarkOnSurfaceVariant = Color(0xFFB9C0D2)
        val DarkOutline = Color(0xFF8990A4)
        val DarkOutlineVariant = Color(0xFF343A49)

        val Fire = Color(0xFFEA694D)
        val Water = Color(0xFF4B86E8)
        val Grass = Color(0xFF57AD65)
        val Electric = Color(0xFFF2C94C)
        val Psychic = Color(0xFFE45D9F)
        val Ice = Color(0xFF65C7D9)
        val Dragon = Color(0xFF725CE7)
        val Dark = Color(0xFF56505D)
        val Fairy = Color(0xFFE88FC6)
        val Normal = Color(0xFF98969B)
        val Fighting = Color(0xFFC65443)
        val Poison = Color(0xFF9B5BC6)
        val Ground = Color(0xFFC9A45D)
        val Rock = Color(0xFFAA9554)
        val Bug = Color(0xFF8AAE2D)
        val Ghost = Color(0xFF665F9A)
        val Steel = Color(0xFF7F9AA7)
        val Flying = Color(0xFF7E9AD8)

        val ShinyGold = Color(0xFFB78900)
        val SpecialGold = Color(0xFF7A5A00)
        val FireRedLeafGreen = Color(0xFFCC5B43)
        val Champions = Color(0xFF7857D8)

        fun game(label:String?):Color=when{
            label?.contains("Scarlet",true)==true || label?.contains("Violet",true)==true -> Color(0xFFB54C5D)
            label?.contains("Sword",true)==true || label?.contains("Shield",true)==true -> Color(0xFF35A9C7)
            label?.contains("Arceus",true)==true -> Color(0xFF527F7C)
            label?.contains("Brilliant",true)==true || label?.contains("Diamond",true)==true || label?.contains("Shining",true)==true -> Color(0xFF5968C7)
            label?.contains("Let's Go",true)==true -> Color(0xFFE0A929)
            label?.contains("Z-A",true)==true -> Color(0xFF2D7F8E)
            label?.contains("FireRed",true)==true || label?.contains("LeafGreen",true)==true -> FireRedLeafGreen
            label?.contains("Champions",true)==true -> Champions
            else -> Primary
        }

        fun type(type:String?):Color=when(type?.lowercase()){
            "fire"->Fire; "water"->Water; "grass"->Grass; "electric"->Electric
            "psychic"->Psychic; "ice"->Ice; "dragon"->Dragon; "dark"->Dark
            "fairy"->Fairy; "normal"->Normal; "fighting"->Fighting; "poison"->Poison
            "ground"->Ground; "rock"->Rock; "bug"->Bug; "ghost"->Ghost
            "steel"->Steel; "flying"->Flying; else->Primary
        }
    }

    object Spacing { val Xs=4.dp; val Sm=8.dp; val Md=12.dp; val Lg=16.dp; val Xl=24.dp; val Xxl=32.dp }
    object Radius { val Xs=10.dp; val Sm=14.dp; val Md=18.dp; val Lg=24.dp; val Xl=30.dp; val Pill=999.dp }
    object Elevation { val Flat=0.dp; val Low=2.dp; val Medium=6.dp; val High=12.dp }
    object Motion { const val Fast=140; const val Standard=220; const val Emphasis=320 }

    object Journey {
        val CardSurface = Color(0xFFFDFDFE)
        val ArtworkBackdrop = Color(0xFF101820)
        val CardHeight = 142.dp
        val CardHeightCompact = 138.dp
        val CoverWidth = 120.dp
        val CoverWidthCompact = 104.dp
        val HeroWidth = 150.dp
        val HeroWidthCompact = 132.dp
        val FadeWidth = 190.dp
        val FadeWidthCompact = 165.dp
        val CardRadius = 24.dp
        val ArtworkRadius = 16.dp
        val CardHorizontalPadding = 12.dp
        val CardVerticalPadding = 11.dp
    }

    val Shapes = Shapes(
        extraSmall = RoundedCornerShape(Radius.Xs),
        small = RoundedCornerShape(Radius.Sm),
        medium = RoundedCornerShape(Radius.Md),
        large = RoundedCornerShape(Radius.Lg),
        extraLarge = RoundedCornerShape(Radius.Xl)
    )

    val AppTypography = Typography(
        headlineLarge = TextStyle(fontWeight=FontWeight.Black,fontSize=32.sp,lineHeight=36.sp,letterSpacing=(-.6).sp),
        headlineMedium = TextStyle(fontWeight=FontWeight.Black,fontSize=27.sp,lineHeight=31.sp,letterSpacing=(-.35).sp),
        headlineSmall = TextStyle(fontWeight=FontWeight.ExtraBold,fontSize=23.sp,lineHeight=27.sp),
        titleLarge = TextStyle(fontWeight=FontWeight.ExtraBold,fontSize=21.sp,lineHeight=25.sp),
        titleMedium = TextStyle(fontWeight=FontWeight.Bold,fontSize=17.sp,lineHeight=21.sp),
        titleSmall = TextStyle(fontWeight=FontWeight.Bold,fontSize=14.sp,lineHeight=18.sp),
        bodyLarge = TextStyle(fontWeight=FontWeight.Medium,fontSize=16.sp,lineHeight=22.sp),
        bodyMedium = TextStyle(fontWeight=FontWeight.Medium,fontSize=14.sp,lineHeight=20.sp),
        bodySmall = TextStyle(fontWeight=FontWeight.Medium,fontSize=12.sp,lineHeight=17.sp),
        labelLarge = TextStyle(fontWeight=FontWeight.Bold,fontSize=14.sp,lineHeight=18.sp),
        labelMedium = TextStyle(fontWeight=FontWeight.Bold,fontSize=12.sp,lineHeight=16.sp),
        labelSmall = TextStyle(fontWeight=FontWeight.Bold,fontSize=10.sp,lineHeight=14.sp,letterSpacing=.35.sp)
    )
}
