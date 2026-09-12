package com.otaviobarreto.pokedex.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*
import com.otaviobarreto.pokedex.audio.HomeAudioManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CompanionCenterScreen(
    onPokemonClick:(Int)->Unit,
    onOpenBoxes:(String?,String?)->Unit
){
    var restoreOpen by remember{mutableStateOf(false)}
    var restoreText by remember{mutableStateOf("")}
    var statusText by remember{mutableStateOf<String?>(null)}
    var activeDownload by remember{mutableStateOf<String?>(null)}
    var progress by remember{mutableStateOf<OfflineGamePackManager.Progress?>(null)}
    var audioEnabled by remember{mutableStateOf(HomeAudioManager.enabled)}
    var audioVolume by remember{mutableFloatStateOf(HomeAudioManager.volume)}

    val clipboard=LocalClipboardManager.current
    val context=LocalContext.current
    val scope=rememberCoroutineScope()

    val createBackup=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ){uri->
        if(uri!=null){
            scope.launch{
                val ok=withContext(Dispatchers.IO){
                    runCatching{
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{
                            it.write(AppBackupManager.exportJson())
                        } ?: error("Não foi possível abrir o arquivo.")
                    }.isSuccess
                }
                statusText=if(ok)"Backup salvo em arquivo." else "Não foi possível salvar o backup."
            }
        }
    }

    val openBackup=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ){uri->
        if(uri!=null){
            scope.launch{
                val raw=withContext(Dispatchers.IO){
                    runCatching{
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}.orEmpty()
                    }.getOrDefault("")
                }
                val ok=raw.isNotBlank() && AppBackupManager.importJson(raw)
                statusText=if(ok)"Backup restaurado com sucesso." else "Arquivo inválido ou incompatível."
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal=16.dp),
        contentPadding=PaddingValues(top=18.dp,bottom=28.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ){
        item{
            Row(verticalAlignment=Alignment.CenterVertically){
                Icon(Icons.Default.Settings,null,Modifier.size(34.dp))
                Column(Modifier.padding(start=10.dp)){
                    Text("Configurações",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black)
                    Text(
                        "Downloads, armazenamento, backup e manutenção do aplicativo.",
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item{
            SettingsSectionTitle("Downloads dos jogos")
            Text(
                "Baixe dados, formas, artes e informações necessárias para usar cada jogo com menos dependência da internet.",
                style=MaterialTheme.typography.bodyMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AppGameCatalog.adventureGames.forEach{game->
            item(key=game.label){
                val pack=OfflineGamePackManager.status(game.label)
                val audit=OfflineGamePackManager.audit(game.label)
                Card(shape=RoundedCornerShape(20.dp)){
                    Column(Modifier.fillMaxWidth().padding(14.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Icon(
                                if(audit.valid) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                                null,
                                tint=if(audit.valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(Modifier.weight(1f).padding(start=10.dp)){
                                Text(game.label,fontWeight=FontWeight.Bold)
                                Text(
                                    when{
                                        activeDownload==game.label -> progress?.label ?: "Preparando download…"
                                        audit.valid -> "Offline pronto · "+pack.pokemonCount+" Pokémon"
                                        pack.downloaded -> audit.summary
                                        else -> "Ainda não baixado"
                                    },
                                    style=MaterialTheme.typography.bodySmall,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if(activeDownload==game.label){
                            LinearProgressIndicator(
                                progress={progress?.fraction ?: 0f},
                                modifier=Modifier.fillMaxWidth().padding(top=10.dp)
                            )
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(top=10.dp),
                            horizontalArrangement=Arrangement.spacedBy(8.dp)
                        ){
                            Button(
                                enabled=activeDownload==null,
                                onClick={
                                    activeDownload=game.label
                                    progress=null
                                    scope.launch{
                                        val result=runCatching{
                                            OfflineGamePackManager.download(game){p->progress=p}
                                        }
                                        statusText=if(result.isSuccess) game.label+": pacote offline atualizado."
                                        else game.label+": falha no download."
                                        activeDownload=null
                                        progress=null
                                    }
                                },
                                modifier=Modifier.weight(1f)
                            ){
                                Icon(Icons.Default.Download,null)
                                Spacer(Modifier.width(6.dp))
                                Text(if(pack.downloaded)"Atualizar" else "Baixar")
                            }
                            FilledTonalButton(
                                enabled=activeDownload==null && pack.downloaded,
                                onClick={
                                    OfflineGamePackManager.remove(game.label)
                                    statusText=game.label+": pacote offline removido."
                                },
                                modifier=Modifier.weight(1f)
                            ){
                                Icon(Icons.Default.DeleteOutline,null)
                                Spacer(Modifier.width(6.dp))
                                Text("Remover")
                            }
                        }
                    }
                }
            }
        }

        item{
            SettingsSectionTitle("Armazenamento e desempenho")
            val cache=PokedexDataStore.cacheStats()
            Card(shape=RoundedCornerShape(20.dp)){
                Column(Modifier.fillMaxWidth().padding(16.dp)){
                    Text("Cache da sessão",fontWeight=FontWeight.Bold)
                    Text(
                        cache.total.toString()+" entradas · Pokémon "+cache.pokemon+
                            " · espécies "+cache.species+
                            " · evoluções "+cache.evolutions,
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick={
                            PokedexDataStore.clearSessionCache()
                            statusText="Cache da sessão limpo. Pacotes offline foram preservados."
                        },
                        modifier=Modifier.fillMaxWidth().padding(top=10.dp)
                    ){
                        Icon(Icons.Default.CleaningServices,null)
                        Spacer(Modifier.width(6.dp))
                        Text("Limpar cache da sessão")
                    }
                }
            }
        }

        item{
            SettingsSectionTitle("Áudio")
            Card(shape=RoundedCornerShape(20.dp)){
                Column(Modifier.fillMaxWidth().padding(16.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.VolumeUp,null)
                        Text("Música e sons",Modifier.weight(1f).padding(start=10.dp),fontWeight=FontWeight.Bold)
                        Switch(
                            checked=audioEnabled,
                            onCheckedChange={
                                audioEnabled=it
                                HomeAudioManager.setEnabled(it)
                            }
                        )
                    }
                    Text(
                        "Mantém o sistema de áudio atual do aplicativo.",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value=audioVolume,
                        onValueChange={
                            audioVolume=it
                            HomeAudioManager.volume=it
                        },
                        enabled=audioEnabled,
                        valueRange=0f..1f
                    )
                }
            }
        }

        item{
            SettingsSectionTitle("Informações da versão")
            val versionName=remember(context){
                runCatching{
                    context.packageManager.getPackageInfo(context.packageName,0).versionName ?: "—"
                }.getOrDefault("—")
            }
            Card(shape=RoundedCornerShape(20.dp)){
                Column(Modifier.fillMaxWidth().padding(16.dp)){
                    Text("POKEDEX v"+versionName,fontWeight=FontWeight.Bold)
                    Text(
                        "Pacote: "+context.packageName,
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item{
            SettingsSectionTitle("Backup e restauração")
            Text(
                "O backup preserva coleção, Jornada, times, formas, Shiny e preferências compatíveis.",
                style=MaterialTheme.typography.bodyMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                Modifier.fillMaxWidth().padding(top=8.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp)
            ){
                Button(
                    onClick={
                        clipboard.setText(AnnotatedString(AppBackupManager.exportJson()))
                        statusText="Backup copiado."
                    },
                    modifier=Modifier.weight(1f)
                ){
                    Icon(Icons.Default.ContentCopy,null)
                    Spacer(Modifier.width(6.dp))
                    Text("Copiar")
                }
                FilledTonalButton(
                    onClick={createBackup.launch("pokedex-backup.json")},
                    modifier=Modifier.weight(1f)
                ){
                    Icon(Icons.Default.Save,null)
                    Spacer(Modifier.width(6.dp))
                    Text("Arquivo")
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top=8.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp)
            ){
                FilledTonalButton(
                    onClick={restoreOpen=true},
                    modifier=Modifier.weight(1f)
                ){
                    Icon(Icons.Default.Restore,null)
                    Spacer(Modifier.width(6.dp))
                    Text("Colar")
                }
                FilledTonalButton(
                    onClick={openBackup.launch(arrayOf("application/json","text/plain"))},
                    modifier=Modifier.weight(1f)
                ){
                    Icon(Icons.Default.FolderOpen,null)
                    Spacer(Modifier.width(6.dp))
                    Text("Importar")
                }
            }
        }

        statusText?.let{message->
            item{
                Card(shape=RoundedCornerShape(16.dp)){
                    Text(
                        message,
                        Modifier.fillMaxWidth().padding(12.dp),
                        color=MaterialTheme.colorScheme.primary,
                        fontWeight=FontWeight.SemiBold
                    )
                }
            }
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
                        value=restoreText,
                        onValueChange={restoreText=it},
                        modifier=Modifier.fillMaxWidth().heightIn(min=140.dp).padding(top=8.dp),
                        label={Text("Backup JSON")}
                    )
                }
            },
            confirmButton={
                TextButton(onClick={
                    val ok=AppBackupManager.importJson(restoreText)
                    statusText=if(ok)"Backup restaurado com sucesso." else "Backup inválido ou incompatível."
                    if(ok){
                        restoreText=""
                        restoreOpen=false
                    }
                }){Text("Restaurar")}
            },
            dismissButton={
                TextButton(onClick={restoreOpen=false}){Text("Cancelar")}
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(text:String){
    Text(
        text,
        style=MaterialTheme.typography.titleLarge,
        fontWeight=FontWeight.Bold,
        modifier=Modifier.padding(top=4.dp)
    )
}
