package com.otaviobarreto.pokedex.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

data class DexNavItem(val route:String,val label:String,val icon:ImageVector)

@Composable
fun DexAppBackground(content:@Composable BoxScope.()->Unit){
    val scheme=MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    scheme.background,
                    scheme.primaryContainer.copy(alpha=.11f),
                    scheme.background
                )
            )
        ),
        content=content
    )
}

@Composable
fun DexBottomBar(
    items:List<DexNavItem>,
    currentRoute:String?,
    onSelect:(DexNavItem)->Unit
){
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start=PokedexDesignTokens.Spacing.Sm,
                end=PokedexDesignTokens.Spacing.Sm,
                top=PokedexDesignTokens.Spacing.Xs,
                bottom=PokedexDesignTokens.Spacing.Sm
            )
    ){
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
        color=MaterialTheme.colorScheme.surface.copy(alpha=.97f),
        shadowElevation=PokedexDesignTokens.Elevation.High,
        tonalElevation=PokedexDesignTokens.Elevation.Low
    ){
        Row(
            Modifier.fillMaxWidth().padding(horizontal=PokedexDesignTokens.Spacing.Xs,vertical=PokedexDesignTokens.Spacing.Xs),
            horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Xs)
        ){
            items.forEach{item->
                val selected=currentRoute==item.route
                val interaction=remember{MutableInteractionSource()}
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(if(pressed).95f else 1f,tween(PokedexDesignTokens.Motion.Fast),label="navScale")
                val background by animateColorAsState(
                    if(selected)MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    tween(PokedexDesignTokens.Motion.Standard),
                    label="navBackground"
                )
                val tint by animateColorAsState(
                    if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(PokedexDesignTokens.Motion.Standard),
                    label="navTint"
                )
                val haptic=LocalHapticFeedback.current
                Column(
                    Modifier.weight(1f).scale(scale)
                        .background(background,RoundedCornerShape(PokedexDesignTokens.Radius.Md))
                        .clickable(interactionSource=interaction,indication=null){
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(item)
                        }
                        .padding(vertical=PokedexDesignTokens.Spacing.Sm),
                    horizontalAlignment=Alignment.CenterHorizontally
                ){
                    Icon(item.icon,item.label,Modifier.size(22.dp),tint=tint)
                    Spacer(Modifier.height(2.dp))
                    Text(item.label,style=MaterialTheme.typography.labelSmall,color=tint,maxLines=1)
                }
            }
        }
    }
    }
}

@Composable
fun DexSectionEyebrow(text:String){
    Text(
        text.uppercase(),
        style=MaterialTheme.typography.labelSmall,
        color=MaterialTheme.colorScheme.primary
    )
}

@Composable
fun DexGlassSurface(
    modifier:Modifier=Modifier,
    content:@Composable ColumnScope.()->Unit
){
    Surface(
        modifier=modifier,
        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
        color=MaterialTheme.colorScheme.surface.copy(alpha=.94f),
        tonalElevation=PokedexDesignTokens.Elevation.Low,
        shadowElevation=PokedexDesignTokens.Elevation.Low
    ){
        Column(Modifier.padding(PokedexDesignTokens.Spacing.Lg),content=content)
    }
}


@Composable
fun DexStatusPane(
    title:String,
    message:String,
    modifier:Modifier=Modifier,
    loading:Boolean=false
){
    DexGlassSurface(modifier){
        Column(
            Modifier.fillMaxWidth().padding(vertical=PokedexDesignTokens.Spacing.Lg),
            horizontalAlignment=Alignment.CenterHorizontally
        ){
            Text(title,style=MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(PokedexDesignTokens.Spacing.Xs))
            Text(
                message,
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant
            )
            if(loading){
                Spacer(Modifier.height(PokedexDesignTokens.Spacing.Md))
                LinearProgressIndicator(Modifier.fillMaxWidth(.55f))
            }
        }
    }
}


