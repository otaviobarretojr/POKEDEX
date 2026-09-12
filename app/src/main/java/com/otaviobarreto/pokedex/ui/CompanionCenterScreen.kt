package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*

@Composable
fun CompanionCenterScreen(onPokemonClick:(Int)->Unit){
    var query by remember { mutableStateOf("") }
    var restoreOpen by remember { mutableStateOf(false) }
    var restoreText by remember { mutableStateOf("") }
    var restoreStatus by remember { mutableStateOf<String?>(null) }
    val clipboard=LocalClipboardManager.current
    val insights=remember(CollectionStore.capturedIds,CollectionStore.contextualCapturedIds,CollectionStore.boxes){
        CollectionInsightsService.current()
    }
    val results=remember(query){
        val q=query.trim().removePrefix("#")
        if(q.isBlank()) emptyList() else PokemonRepository.all().filter{
            it.name.contains(q,true) || it.id.toString()==q || it.types.any{type->type.contains(q,true)}
        }.take(40)
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal=16.dp),
        contentPadding=PaddingValues(top=18.dp,bottom=26.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        item{
            Text("Central",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black)
            Text("Busca rápida, visão da coleção e backup do seu progresso.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item{
            OutlinedTextField(
                value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),
                singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},
                label={Text("Buscar Pokémon, número ou tipo")}
            )
        }
        if(query.isNotBlank()){
            if(results.isEmpty()) item{Text("Nenhum resultado no índice local.")}
            else items(results,key={it.id}){pk->
                Card(onClick={onPokemonClick(pk.id)},shape=RoundedCornerShape(18.dp)){
                    Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text("#"+pk.id.toString().padStart(4,'0')+" · "+pk.name,fontWeight=FontWeight.Bold)
                            Text(pk.types.joinToString(" / "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Gen "+pk.generation,style=MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item{
            Text("Sua coleção",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=4.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                InsightCard("Capturados",insights.totalCaptured.toString(),Modifier.weight(1f))
                InsightCard("Jogos",insights.gamesWithProgress.toString()+"/"+insights.totalGames,Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                InsightCard("Registros",insights.contextualRegistrations.toString(),Modifier.weight(1f))
                InsightCard("Duplicados",insights.duplicates.toString(),Modifier.weight(1f))
                InsightCard("Sem Box",insights.unboxed.toString(),Modifier.weight(1f))
            }
        }
        item{
            Text("Backup",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=4.dp))
            Text("Copie o backup para guardar fora do aparelho. Para restaurar, cole o texto exportado.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button(
                    onClick={clipboard.setText(AnnotatedString(AppBackupManager.exportJson()));restoreStatus="Backup copiado."},
                    modifier=Modifier.weight(1f)
                ){Icon(Icons.Default.ContentCopy,null);Spacer(Modifier.width(6.dp));Text("Copiar")}
                FilledTonalButton(onClick={restoreOpen=true},modifier=Modifier.weight(1f)){
                    Icon(Icons.Default.Restore,null);Spacer(Modifier.width(6.dp));Text("Restaurar")
                }
            }
            restoreStatus?.let{Text(it,Modifier.padding(top=8.dp),color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)}
        }
    }

    if(restoreOpen){
        AlertDialog(
            onDismissRequest={restoreOpen=false},
            title={Text("Restaurar backup")},
            text={
                Column{
                    Text("Cole abaixo o backup exportado anteriormente.")
                    OutlinedTextField(
                        restoreText,{restoreText=it},Modifier.fillMaxWidth().heightIn(min=140.dp).padding(top=8.dp),
                        label={Text("Backup JSON")}
                    )
                }
            },
            confirmButton={
                TextButton(onClick={
                    val ok=AppBackupManager.importJson(restoreText)
                    restoreStatus=if(ok)"Backup restaurado com sucesso." else "Backup inválido ou incompatível."
                    if(ok){restoreText="";restoreOpen=false}
                }){Text("Restaurar")}
            },
            dismissButton={TextButton(onClick={restoreOpen=false}){Text("Cancelar")}}
        )
    }
}

@Composable
private fun InsightCard(label:String,value:String,modifier:Modifier=Modifier){
    Card(modifier,shape=RoundedCornerShape(18.dp)){
        Column(Modifier.padding(13.dp)){
            Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
            Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
