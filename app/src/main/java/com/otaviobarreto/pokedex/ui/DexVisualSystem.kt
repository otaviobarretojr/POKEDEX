package com.otaviobarreto.pokedex.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object DexMotion {
    const val Quick = 160
    const val Standard = 240
    const val Emphasis = 360
}

object DexTypeColors {
    fun forType(type:String?):Color = when(type?.lowercase()){
        "fire"->Color(0xFFEF6C48); "water"->Color(0xFF4D8FDB); "grass"->Color(0xFF55A96A)
        "electric"->Color(0xFFE8B62F); "psychic"->Color(0xFFD85D91); "ice"->Color(0xFF58BFC5)
        "dragon"->Color(0xFF6556C8); "dark"->Color(0xFF4A4655); "fairy"->Color(0xFFE477A8)
        "fighting"->Color(0xFFC65B4D); "poison"->Color(0xFF9A5DB2); "ground"->Color(0xFFC99B54)
        "flying"->Color(0xFF7896CF); "bug"->Color(0xFF8DA63E); "rock"->Color(0xFF9C8756)
        "ghost"->Color(0xFF665D9E); "steel"->Color(0xFF728B99); else->PokedexDesignTokens.Colors.Primary
    }
}

@Composable
fun DexScreenBackground(accent:Color=MaterialTheme.colorScheme.primary,content:@Composable BoxScope.()->Unit){
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(
                MaterialTheme.colorScheme.background,
                accent.copy(alpha=.055f),
                MaterialTheme.colorScheme.background
            ))
        ), content=content
    )
}

@Composable
fun DexSectionLabel(text:String,modifier:Modifier=Modifier){
    Text(text.uppercase(),modifier,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Black,
        color=MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun DexBadge(text:String,accent:Color=MaterialTheme.colorScheme.primary,modifier:Modifier=Modifier){
    Surface(modifier,shape=RoundedCornerShape(999.dp),color=accent.copy(alpha=.12f)){
        Text(text,Modifier.padding(horizontal=9.dp,vertical=5.dp),style=MaterialTheme.typography.labelSmall,
            fontWeight=FontWeight.Black,color=accent)
    }
}

@Composable
fun DexActionCard(
    modifier:Modifier=Modifier,
    accent:Color=MaterialTheme.colorScheme.primary,
    onClick:(()->Unit)?=null,
    content:@Composable ColumnScope.()->Unit
){
    val source=remember{MutableInteractionSource()}
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed) .975f else 1f, spring(stiffness=650f),label="dexPress")
    val surface by animateColorAsState(if(pressed) accent.copy(alpha=.10f) else MaterialTheme.colorScheme.surface,label="dexSurface")
    Card(
        onClick=onClick?:{}, enabled=onClick!=null,
        interactionSource=source,
        modifier=modifier.scale(scale),
        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
        colors=CardDefaults.cardColors(containerColor=surface),
        elevation=CardDefaults.cardElevation(defaultElevation=PokedexDesignTokens.Elevation.Low)
    ){
        Column(Modifier.fillMaxWidth(),content=content)
    }
}

@Composable
fun DexIconTile(icon:ImageVector,accent:Color=MaterialTheme.colorScheme.primary,contentDescription:String?=null){
    Surface(shape=RoundedCornerShape(14.dp),color=accent.copy(alpha=.12f)){
        Icon(icon,contentDescription,Modifier.padding(9.dp).size(21.dp),tint=accent)
    }
}

@Composable
fun DexHeroHeader(
    eyebrow:String,
    title:String,
    subtitle:String?=null,
    accent:Color=MaterialTheme.colorScheme.primary,
    trailing:(@Composable RowScope.()->Unit)?=null
){
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.weight(1f)){
            DexBadge(eyebrow,accent)
            Spacer(Modifier.height(8.dp))
            Text(title,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black)
            subtitle?.let{Text(it,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
        trailing?.invoke(this)
    }
}
