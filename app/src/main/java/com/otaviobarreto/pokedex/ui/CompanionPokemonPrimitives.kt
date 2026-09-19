package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Stateless Pokemon-first primitives. Artwork remains caller supplied. */
@Composable
fun PokemonGridCard(number: Int, name: String, owned: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, types: List<String> = emptyList(),
    accent: Color = MaterialTheme.colorScheme.primary, shiny: Boolean = false,
    artwork: @Composable BoxScope.() -> Unit) {
    Card(onClick = onClick, modifier = modifier,
        shape = RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
        colors = CardDefaults.cardColors(containerColor = if (owned) accent.copy(alpha=.08f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.46f))) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1.08f), contentAlignment=Alignment.Center) {
                artwork()
                Surface(Modifier.align(Alignment.TopStart).padding(PokedexDesignTokens.Spacing.Sm),
                    shape=RoundedCornerShape(PokedexDesignTokens.Radius.Pill),
                    color=MaterialTheme.colorScheme.surface.copy(alpha=.82f)) {
                    Text("#" + number.toString().padStart(4,'0'),
                        Modifier.padding(horizontal=8.dp,vertical=4.dp),
                        style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
                }
                if(shiny) Text("✦",Modifier.align(Alignment.TopEnd).padding(PokedexDesignTokens.Spacing.Sm),
                    color=accent,style=MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.padding(horizontal=PokedexDesignTokens.Spacing.Md,vertical=PokedexDesignTokens.Spacing.Sm)) {
                Text(name,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(types.isNotEmpty()) Text(types.take(2).joinToString(" · "),style=MaterialTheme.typography.labelSmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
                Text(if(owned)"Registrado" else "Não registrado",style=MaterialTheme.typography.labelSmall,
                    color=if(owned)accent else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CompanionContextHeader(title:String, modifier:Modifier=Modifier, eyebrow:String?=null,
    subtitle:String?=null, accent:Color=MaterialTheme.colorScheme.primary,
    progress:(@Composable (() -> Unit))?=null, artwork:(@Composable BoxScope.() -> Unit)?=null) {
    Surface(modifier.fillMaxWidth(),shape=RoundedCornerShape(PokedexDesignTokens.Radius.Xl),color=accent.copy(alpha=.09f)) {
        Box(Modifier.fillMaxWidth().heightIn(min=142.dp)) {
            artwork?.invoke(this)
            Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg).padding(end=if(artwork!=null)92.dp else 0.dp)) {
                eyebrow?.let { Text(it.uppercase(),style=MaterialTheme.typography.labelSmall,color=accent,fontWeight=FontWeight.Black) }
                Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
                subtitle?.let { Text(it,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis) }
                progress?.let { Spacer(Modifier.height(PokedexDesignTokens.Spacing.Md)); it() }
            }
        }
    }
}

@Composable
fun PokemonHeroHeader(number:Int,name:String,types:List<String>,modifier:Modifier=Modifier,
    accent:Color=MaterialTheme.colorScheme.primary,status:String?=null,
    artwork:@Composable BoxScope.()->Unit) {
    Surface(modifier.fillMaxWidth(),shape=RoundedCornerShape(PokedexDesignTokens.Radius.Xl),color=accent.copy(alpha=.10f)) {
        Column {
            Box(Modifier.fillMaxWidth().heightIn(min=220.dp),contentAlignment=Alignment.Center) {
                artwork()
                Text("#"+number.toString().padStart(4,'0'),Modifier.align(Alignment.TopStart).padding(PokedexDesignTokens.Spacing.Lg),
                    style=MaterialTheme.typography.labelLarge,color=accent,fontWeight=FontWeight.Black)
            }
            Column(Modifier.padding(PokedexDesignTokens.Spacing.Lg)) {
                Text(name,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
                if(types.isNotEmpty()) Text(types.take(2).joinToString(" · "),style=MaterialTheme.typography.labelLarge,color=accent)
                status?.let { Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
fun EvolutionPathCard(condition:String,modifier:Modifier=Modifier,accent:Color=MaterialTheme.colorScheme.primary,
    from: @Composable () -> Unit, to: @Composable () -> Unit) {
    Surface(modifier.fillMaxWidth(),shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
        color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.42f)) {
        Row(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Md),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.weight(1f),contentAlignment=Alignment.Center){from()}
            Column(Modifier.widthIn(min=90.dp).padding(horizontal=PokedexDesignTokens.Spacing.Sm),horizontalAlignment=Alignment.CenterHorizontally) {
                Text("→",style=MaterialTheme.typography.headlineSmall,color=accent)
                Text(condition,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=3,overflow=TextOverflow.Ellipsis)
            }
            Box(Modifier.weight(1f),contentAlignment=Alignment.Center){to()}
        }
    }
}
