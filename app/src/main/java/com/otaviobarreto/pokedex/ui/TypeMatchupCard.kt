package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.TypeMatchup

@Composable
fun TypeMatchupCard(types: List<String>) {
    val result = TypeMatchup.defensiveFor(types)
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp)) }
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text("Defesa por tipo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Multiplicadores recebidos considerando ${types.joinToString(" / ")}.", style = MaterialTheme.typography.bodySmall)
                }
            }
            MatchupBand("Fraqueza (4×)", result.quadrupleWeak, "4×", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
            MatchupBand("Fraqueza (2×)", result.doubleWeak, "2×", MaterialTheme.colorScheme.errorContainer.copy(alpha=.72f), MaterialTheme.colorScheme.onErrorContainer)
            MatchupBand("Resistência (0.5×)", result.halfResist, "0.5×", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
            MatchupBand("Resistência (0.25×)", result.quarterResist, "0.25×", MaterialTheme.colorScheme.tertiaryContainer.copy(alpha=.72f), MaterialTheme.colorScheme.onTertiaryContainer)
            MatchupBand("Imunidade (0×)", result.immune, "0×", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, showEmpty = true)
        }
    }
}

@Composable
private fun MatchupBand(title:String, values:List<String>, multiplier:String, background:Color, ink:Color, showEmpty:Boolean=false){
    if(values.isEmpty() && !showEmpty) return
    Surface(Modifier.fillMaxWidth(), shape=RoundedCornerShape(15.dp), color=background){
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=9.dp), verticalAlignment=Alignment.CenterVertically){
            Text(title, fontWeight=FontWeight.Bold, color=ink, style=MaterialTheme.typography.labelLarge, modifier=Modifier.widthIn(min=128.dp))
            Spacer(Modifier.width(10.dp))
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(7.dp)){
                if(values.isEmpty()) Surface(shape=RoundedCornerShape(50), color=MaterialTheme.colorScheme.surface.copy(alpha=.62f)){Text("—",Modifier.padding(horizontal=18.dp,vertical=5.dp),color=ink)}
                else values.forEach{type->Surface(shape=RoundedCornerShape(50),color=MaterialTheme.colorScheme.surface.copy(alpha=.72f),border=androidx.compose.foundation.BorderStroke(1.dp,ink.copy(alpha=.24f))){Text("$type $multiplier",Modifier.padding(horizontal=12.dp,vertical=5.dp),fontWeight=FontWeight.SemiBold,color=ink)}}
            }
        }
    }
}
