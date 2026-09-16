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
    var serverProgress by remember{mutableStateOf<ServerOfflinePackageInstaller.Progress?>(null)}
    var audioEnabled by remember{mutableStateOf(HomeAudioManager.enabled)}
    var audioVolume by remember{mutableFloatStateOf(HomeAudioManager.volume)}
    var storageRevision by remember{mutableIntStateOf(0)}
    var remoteManifestRevision by remember{mutableIntStateOf(0)}
    var installStateRevision by remember{mutableIntStateOf(0)}

    val clipboard=LocalClipboardManager.current
    val context=LocalContext.current
    val scope=rememberCoroutineScope()

    LaunchedEffect(Unit){
        withContext(Dispatchers.IO){runCatching{RemoteOfflinePackageCatalog.refresh()}}
        remoteManifestRevision++
        val remote=RemoteOfflinePackageCatalog.general()
        val state=OfflinePackageInstallState.read(context)
        val zipRecoverable=remote?.ready==true &&
            OfflinePackageInstallState.generalZip(context,remote.version).let{zip->
                zip.exists() && zip.length()==(remote.sizeBytes ?: -1L)
            }
        if(remote?.ready==true && (state.active || state.stage==OfflinePackageInstallState.Stage.FAILED || zipRecoverable)){
            activeDownload="__general__"
            statusText="Retomando instalação da biblioteca…"
            val recovered=withContext(Dispatchers.IO){
                ServerOfflinePackageInstaller.recoverGeneralIfNeeded(context,remote){p->serverProgress=p}
            }
            statusText=if(recovered) "Biblioteca geral recuperada e instalada." else
                "Instalação não concluída · "+OfflinePackageInstallState.diagnose(
                    context,remote.version,remote.sizeBytes ?: 0L
                )
            activeDownload=null
            serverProgress=null
            installStateRevision++
        }
    }

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
                val offlineLabels=if(raw.isNotBlank()) AppBackupManager.downloadedPackLabels(raw) else emptyList()
                val ok=raw.isNotBlank() && withContext(Dispatchers.IO){
                    AppBackupManager.importJson(raw)
                }
                statusText=when{
                    !ok -> "Arquivo inválido, corrompido ou incompatível."
                    offlineLabels.isNotEmpty() ->
                        "Backup restaurado. "+offlineLabels.size+" pacote(s) offline precisam ser baixados novamente."
                    else -> "Backup restaurado com sucesso."
                }
            }
        }
    }

    DexAppBackground {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal=PokedexDesignTokens.Spacing.Lg),
        contentPadding=PaddingValues(top=PokedexDesignTokens.Spacing.Lg,bottom=PokedexDesignTokens.Spacing.Xxl),
        verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Lg)
    ){
        item{
            CompanionContextHeader(title="Configurações",eyebrow="Trainer Companion",subtitle="Conteúdo, desempenho, áudio e segurança dos seus dados.",progress={Icon(Icons.Default.Settings,"Configurações",tint=MaterialTheme.colorScheme.primary)})
        }

        item(key="offline_content_summary"){
            val generalValid=remember(storageRevision,activeDownload){OfflineGamePackManager.generalAudit()}
            val remoteGeneral=remember(remoteManifestRevision){RemoteOfflinePackageCatalog.general()}
            val installedServerVersion=remember(storageRevision,installStateRevision){OfflineGamePackManager.generalServerVersion()}
            val installedGames=remember(storageRevision){
                AppGameCatalog.adventureGames.count{OfflineGamePackManager.status(it.label).downloaded}
            }
            val updateAvailable=remoteGeneral?.ready==true && remoteGeneral.version>installedServerVersion && installedServerVersion>0
            CompanionSectionHeader(title="Conteúdo offline")
            Card(
                shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
                colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.22f))
            ){
                Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Icon(
                            if(generalValid) Icons.Default.CloudDone else Icons.Default.Warning,
                            null,
                            tint=if(generalValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Column(Modifier.weight(1f).padding(start=PokedexDesignTokens.Spacing.Md)){
                            Text(if(generalValid)"Biblioteca instalada" else "Biblioteca requer atenção",fontWeight=FontWeight.Bold)
                            Text(
                                if(generalValid) "v"+installedServerVersion+" · "+installedGames+" complementos de jogos"
                                else "Use Reparar biblioteca para restaurar o conteúdo necessário.",
                                style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if(updateAvailable){
                        Button(
                            enabled=activeDownload==null,
                            onClick={
                                val remote=remoteGeneral ?: return@Button
                                activeDownload="__general__"
                                scope.launch{
                                    val result=runCatching{
                                        ServerOfflinePackageInstaller.installOrUpdateGeneral(context,remote){p->serverProgress=p}
                                    }
                                    statusText=if(result.isSuccess)"Biblioteca atualizada." else "Atualização falhou · "+(result.exceptionOrNull()?.message ?: "tente novamente")
                                    activeDownload=null
                                    serverProgress=null
                                    storageRevision++
                                    installStateRevision++
                                }
                            },
                            modifier=Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Md)
                        ){
                            Icon(Icons.Default.SystemUpdate,null)
                            Spacer(Modifier.width(6.dp))
                            Text("Atualizar conteúdo")
                        }
                    }
                    FilledTonalButton(
                        enabled=activeDownload==null,
                        onClick={
                            activeDownload="__repair__"
                            scope.launch{
                                statusText="Verificando biblioteca…"
                                val result=runCatching{
                                    withContext(Dispatchers.IO){
                                        val remote=RemoteOfflinePackageCatalog.general() ?: error("Catálogo indisponível")
                                        check(ServerOfflinePackageInstaller.installOrUpdateGeneral(context,remote){p->serverProgress=p})
                                    }
                                }
                                statusText=if(result.isSuccess)"Biblioteca verificada e reparada." else "Reparo falhou · "+(result.exceptionOrNull()?.message ?: "tente novamente")
                                activeDownload=null
                                serverProgress=null
                                storageRevision++
                                installStateRevision++
                            }
                        },
                        modifier=Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Sm)
                    ){
                        Icon(Icons.Default.Build,null)
                        Spacer(Modifier.width(6.dp))
                        Text("Reparar biblioteca")
                    }
                }
            }
        }

        item{
            CompanionSectionHeader(title="Armazenamento e desempenho")
            val cache=remember(storageRevision){PokedexDataStore.cacheStats()}
            val apiCacheBytes=remember(storageRevision){PersistentApiCache.sizeBytes()}
            val apiCacheMb=apiCacheBytes/1024f/1024f
            val downloadedPacks=remember(storageRevision){
                AppGameCatalog.adventureGames.count{OfflineGamePackManager.status(it.label).downloaded}
            }
            Card(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg)){
                Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){
                    Text("Uso local",fontWeight=FontWeight.Bold)
                    Text(
                        cache.total.toString()+" entradas · Pokémon "+cache.pokemon+
                            " · espécies "+cache.species+
                            " · evoluções "+cache.evolutions,
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "API persistente: "+String.format("%.1f MB",apiCacheMb)+
                            " · recursos fixados "+PersistentApiCache.pinnedCount()+
                            " · pacotes offline "+downloadedPacks,
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val startupMs=remember(storageRevision){StartupPreloader.lastWarmDurationMs}
                    if(startupMs>0){
                        Text(
                            "Última preparação inicial: "+startupMs+" ms",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick={
                            PokedexDataStore.clearSessionCache()
                            PersistentApiCache.pruneUnpinned()
                            storageRevision++
                            statusText="Cache da sessão limpo. Pacotes offline foram preservados."
                        },
                        modifier=Modifier.fillMaxWidth().padding(top=10.dp)
                    ){
                        Icon(Icons.Default.CleaningServices,null)
                        Spacer(Modifier.width(6.dp))
                        Text("Limpar cache da sessão")
                    }
                    FilledTonalButton(
                        onClick={
                            val downloaded=AppGameCatalog.adventureGames
                                .filter{OfflineGamePackManager.status(it.label).downloaded}
                            val invalid=downloaded.filterNot{OfflineGamePackManager.audit(it.label).valid}
                            val collection=CollectionIntegrityService.repair()
                            storageRevision++
                            statusText=when{
                                invalid.isNotEmpty() -> "Integridade: "+invalid.size+" pacote(s) precisam de reparo."
                                !collection.clean -> "Coleção reparada; verifique novamente."
                                else -> "Integridade verificada: coleção e pacotes offline estão consistentes."
                            }
                        },
                        modifier=Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Sm)
                    ){
                        Icon(Icons.Default.VerifiedUser,null)
                        Spacer(Modifier.width(6.dp))
                        Text("Verificar integridade")
                    }
                }
            }
        }

        item{
            SettingsSectionTitle("Áudio")
            Card(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg)){
                Column(Modifier.fillMaxWidth().padding(16.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.VolumeUp,null)
                        Text("Música e sons",Modifier.weight(1f).padding(start=PokedexDesignTokens.Spacing.Md),fontWeight=FontWeight.Bold)
                        Switch(
                            checked=audioEnabled,
                            onCheckedChange={
                                audioEnabled=it
                                HomeAudioManager.setEnabled(it)
                            }
                        )
                    }
                    Text(
                        "Controle a música e os sons do Companion.",
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
            Card(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg)){
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
            CompanionSectionHeader(title="Backup e restauração")
            Text(
                "O backup preserva coleção, Jornada, times, formas, Shiny e preferências. Pacotes offline são registrados no backup, mas precisam ser baixados novamente após restauração.",
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
                    Text("Copiar backup")
                }
                FilledTonalButton(
                    onClick={createBackup.launch("pokedex-backup.json")},
                    modifier=Modifier.weight(1f)
                ){
                    Icon(Icons.Default.Save,null)
                    Spacer(Modifier.width(6.dp))
                    Text("Salvar arquivo")
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
                    Text("Colar backup")
                }
                FilledTonalButton(
                    onClick={openBackup.launch(arrayOf("application/json","text/plain"))},
                    modifier=Modifier.weight(1f)
                ){
                    Icon(Icons.Default.FolderOpen,null)
                    Spacer(Modifier.width(6.dp))
                    Text("Importar arquivo")
                }
            }
        }

        statusText?.let{message->
            item{
                DexGlassSurface(Modifier.fillMaxWidth()){
                    Text(
                        message,
                        Modifier.fillMaxWidth(),
                        color=MaterialTheme.colorScheme.primary,
                        fontWeight=FontWeight.SemiBold
                    )
                }
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
                        modifier=Modifier.fillMaxWidth().heightIn(min=140.dp).padding(top=PokedexDesignTokens.Spacing.Sm),
                        label={Text("Backup JSON")}
                    )
                }
            },
            confirmButton={
                TextButton(onClick={
                    val pasted=restoreText
                    scope.launch{
                        val ok=withContext(Dispatchers.IO){
                            AppBackupManager.importJson(pasted)
                        }
                        statusText=if(ok)"Backup restaurado com sucesso." else "Backup inválido ou incompatível."
                        if(ok){
                            restoreText=""
                            restoreOpen=false
                        }
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
private fun SettingsSectionTitle(text:String)=CompanionSectionHeader(title=text)
