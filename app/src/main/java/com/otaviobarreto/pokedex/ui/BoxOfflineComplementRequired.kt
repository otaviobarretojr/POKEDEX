package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BoxOfflineComplementRequired(gameLabel:String,accent:Color){
    Box(
        Modifier.fillMaxSize().padding(24.dp),
        contentAlignment=Alignment.Center
    ){
        Column(horizontalAlignment=Alignment.CenterHorizontally){
            Icon(
                Icons.Default.CloudDownload,
                contentDescription=null,
                tint=accent,
                modifier=Modifier.size(42.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text("Complemento do jogo necessário",fontWeight=FontWeight.Bold)
            Text(
                "A biblioteca geral está instalada. Baixe o complemento de $gameLabel em Configurações para liberar esta Box offline.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
