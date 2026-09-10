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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .9f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE9E8FF), modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Shield, null, tint = Color(0xFF4D4CD7), modifier = Modifier.size(21.dp)) }
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text("Defesa por tipo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Multiplicadores recebidos considerando ${types.joinToString(" / ")}.", style = MaterialTheme.typography.bodySmall)
                }
            }
            MatchupBand("Fraqueza (4×)", result.quadrupleWeak, "4×", Color(0xFFFFE3E5), Color(0xFFB4232F))
            MatchupBand("Fraqueza (2×)", result.doubleWeak, "2×", Color(0xFFFFE9EA), Color(0xFFB4232F))
            MatchupBand("Resistência (0.5×)", result.halfResist, "0.5×", Color(0xFFFFF4D8), Color(0xFF9A6A00))
            MatchupBand("Resistência (0.25×)", result.quarterResist, "0.25×", Color(0xFFFFF4D8), Color(0xFF9A6A00))
            MatchupBand("Imunidade (0×)", result.immune, "0×", Color(0xFFE8E8FF), Color(0xFF4D4CD7), showEmpty = true)
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
                if(values.isEmpty()) Surface(shape=RoundedCornerShape(50), color=Color.White.copy(alpha=.45f)){Text("—",Modifier.padding(horizontal=18.dp,vertical=5.dp),color=ink)}
                else values.forEach{type->Surface(shape=RoundedCornerShape(50),color=Color.White.copy(alpha=.55f),border=androidx.compose.foundation.BorderStroke(1.dp,ink.copy(alpha=.24f))){Text("$type $multiplier",Modifier.padding(horizontal=12.dp,vertical=5.dp),fontWeight=FontWeight.SemiBold,color=ink)}}
            }
        }
    }
}
