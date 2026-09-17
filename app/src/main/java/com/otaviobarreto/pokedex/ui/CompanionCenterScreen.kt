package com.otaviobarreto.pokedex.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.audio.HomeAudioManager
import com.otaviobarreto.pokedex.data.*
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
    var serverProgress by remember{mutableStateOf<ServerOfflinePackageInstaller.Progress?>(null)}
    var audioEnabled by remember{mutableStateOf(HomeAudioManager.enabled)}
    var audioVolume by remember{mutableFloatStateOf(HomeAudioManager.volume)}
    var storageRevision by remember{mutableIntStateOf(0)}
    var remoteManifestRevision by remember{mutableIntStateOf(0)}
    var installStateRevision by remember{mutableIntStateOf(0)}
    var performanceSnapshot by remember{mutableStateOf<SettingsPerformanceSnapshot?>(null)}

    val clipboard=LocalClipboardManager.current
    val context=LocalContext.current
    val scope=rememberCoroutineScope()

    LaunchedEffect(Unit){
        performanceSnapshot=withContext(Dispatchers.IO){loadSettingsPerformanceSnapshot()}
        withContext(Dispatchers.IO){runCatching{RemoteOfflinePackageCatalog.refresh()}}
        remoteManifestRevision++
        val remote=RemoteOfflinePackageCatalog.general()
        val state=OfflinePackageInstallState.read(context)
        val zipRecoverable=remote?.ready==true && OfflinePackageInstallState.generalZip(context,remote.version).let{zip->
            zip.exists() && zip.length()==(remote.sizeBytes ?: -1L)
        }
        if(remote?.ready==true && (state.active || state.stage==OfflinePackageInstallState.Stage.FAILED || zipRecoverable)){
            activeDownload="__general__"
            statusText="Retomando instalação da biblioteca…"
            val recovered=withContext(Dispatchers.IO){
                ServerOfflinePackageInstaller.recoverGeneralIfNeeded(context,remote){p->serverProgress=p}
            }
            statusText=if(recovered) "Biblioteca geral recuperada e instalada." else
                "Instalação não concluída · "+OfflinePackageInstallState.diagnose(context,remote.version,remote.sizeBytes ?: 0L)
            activeDownload=null
            serverProgress=null
            installStateRevision++
        }
    }

    val createBackup=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null) scope.launch{
            val ok=withContext(Dispatchers.IO){
                runCatching{
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(AppBackupManager.exportJson())}
                        ?: error("Não foi possível abrir o arquivo.")
                }.isSuccess
            }
            statusText=if(ok) "Backup salvo em arquivo." else "Não foi possível salvar o backup."
        }
    }

    val openBackup=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null) scope.launch{
            val raw=withContext(Dispatchers.IO){
                runCatching{context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}.orEmpty()}.getOrDefault("")
            }
            val offlineLabels=if(raw.isNotBlank()) AppBackupManager.downloadedPackLabels(raw) else emptyList()
            val ok=raw.isNotBlank() && withContext(Dispatchers.IO){AppBackupManager.importJson(raw)}
            statusText=when{
                !ok -> "Arquivo inválido, corrompido ou incompatível."
                offlineLabels.isNotEmpty() -> "Backup restaurado. "+offlineLabels.size+" pacote(s) offline precisam ser baixados novamente."
                else -> "Backup restaurado com sucesso."
            }
        }
    }

    DexAppBackground{
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal=PokedexDesignTokens.Spacing.Lg),
            contentPadding=PaddingValues(top=PokedexDesignTokens.Spacing.Lg,bottom=PokedexDesignTokens.Spacing.Xxl),
            verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Lg)
        ){
            item{
                CompanionContextHeader(
                    title="Configurações",
                    eyebrow="Trainer Companion",
                    subtitle="Seu conteúdo, preferências e dados em um só lugar.",
                    progress={Icon(Icons.Default.Settings,"Configurações",tint=MaterialTheme.colorScheme.primary)}
                )
            }

            item(key="offline_content_summary"){
                val generalValid=remember(storageRevision,activeDownload){OfflineGamePackManager.generalAudit()}
                val remoteGeneral=remember(remoteManifestRevision){RemoteOfflinePackageCatalog.general()}
                val installedServerVersion=remember(storageRevision,installStateRevision){OfflineGamePackManager.generalServerVersion()}
                val installedGames=remember(storageRevision){AppGameCatalog.adventureGames.count{OfflineGamePackManager.status(it.label).downloaded}}
                val updateAvailable=remoteGeneral?.ready==true && remoteGeneral.version>installedServerVersion && installedServerVersion>0
                CompanionSectionHeader(title="Conteúdo offline")
                CompanionSettingsGroup{
                    CompanionSettingsRow(
                        title=if(generalValid) "Biblioteca instalada" else "Biblioteca requer atenção",
                        supporting=if(generalValid) "v$installedServerVersion · $installedGames complementos de jogos" else "Repare a biblioteca para restaurar o conteúdo necessário.",
                        icon=if(generalValid) Icons.Default.CloudDone else Icons.Default.Warning
                    )
                    CompanionSettingsDivider()
                    if(updateAvailable){
                        CompanionSettingsRow(
                            title="Atualizar conteúdo",
                            supporting="Há uma versão mais recente da biblioteca disponível.",
                            icon=Icons.Default.SystemUpdate,
                            trailing={TextButton(enabled=activeDownload==null,onClick={
                                val remote=remoteGeneral ?: return@TextButton
                                activeDownload="__general__"
                                scope.launch{
                                    val result=runCatching{ServerOfflinePackageInstaller.installOrUpdateGeneral(context,remote){p->serverProgress=p}}
                                    statusText=if(result.isSuccess) "Biblioteca atualizada." else "Atualização falhou · "+(result.exceptionOrNull()?.message ?: "tente novamente")
                                    activeDownload=null; serverProgress=null; storageRevision++; installStateRevision++
                                }
                            }){Text("Atualizar")}}
                        )
                        CompanionSettingsDivider()
                    }
                    CompanionSettingsRow(
                        title="Reparar biblioteca",
                        supporting="Verifica os arquivos e recupera conteúdo ausente.",
                        icon=Icons.Default.Build,
                        trailing={TextButton(enabled=activeDownload==null,onClick={
                            activeDownload="__repair__"
                            scope.launch{
                                statusText="Verificando biblioteca…"
                                val result=runCatching{
                                    withContext(Dispatchers.IO){
                                        val remote=RemoteOfflinePackageCatalog.general() ?: error("Catálogo indisponível")
                                        check(ServerOfflinePackageInstaller.installOrUpdateGeneral(context,remote){p->serverProgress=p})
                                    }
                                }
                                statusText=if(result.isSuccess) "Biblioteca verificada e reparada." else "Reparo falhou · "+(result.exceptionOrNull()?.message ?: "tente novamente")
                                activeDownload=null; serverProgress=null; storageRevision++; installStateRevision++
                            }
                        }){Text("Reparar")}}
                    )
                }
            }

            item{
                CompanionSectionHeader(title="Armazenamento e desempenho")
                LaunchedEffect(storageRevision){performanceSnapshot=withContext(Dispatchers.IO){loadSettingsPerformanceSnapshot()}}
                val snapshot=performanceSnapshot
                val cache=snapshot?.cache ?: PokedexDataStore.cacheStats()
                val apiCacheMb=(snapshot?.apiCacheBytes ?: 0L)/1024f/1024f
                val downloadedPacks=snapshot?.downloadedPacks ?: 0
                val startupMs=snapshot?.startupMs ?: StartupPreloader.lastWarmDurationMs
                CompanionSettingsGroup{
                    CompanionSettingsRow(
                        title="Uso local",
                        supporting=cache.total.toString()+" entradas · API "+String.format("%.1f MB",apiCacheMb)+" · "+downloadedPacks+" pacotes offline"+(if(startupMs>0) " · inicialização ${startupMs} ms" else ""),
                        icon=Icons.Default.Storage
                    )
                    CompanionSettingsDivider()
                    CompanionSettingsRow(
                        title="Limpar cache da sessão",
                        supporting="Preserva seus pacotes offline e dados da coleção.",
                        icon=Icons.Default.CleaningServices,
                        trailing={TextButton(onClick={
                            PokedexDataStore.clearSessionCache(); PersistentApiCache.pruneUnpinned(); storageRevision++
                            statusText="Cache da sessão limpo. Pacotes offline foram preservados."
                        }){Text("Limpar")}}
                    )
                    CompanionSettingsDivider()
                    CompanionSettingsRow(
                        title="Verificar integridade",
                        supporting="Confere coleção e pacotes instalados.",
                        icon=Icons.Default.VerifiedUser,
                        trailing={TextButton(onClick={
                            val downloaded=AppGameCatalog.adventureGames.filter{OfflineGamePackManager.status(it.label).downloaded}
                            val invalid=downloaded.filterNot{OfflineGamePackManager.audit(it.label).valid}
                            val collection=CollectionIntegrityService.repair(); storageRevision++
                            statusText=when{
                                invalid.isNotEmpty() -> "Integridade: "+invalid.size+" pacote(s) precisam de reparo."
                                !collection.clean -> "Coleção reparada; verifique novamente."
                                else -> "Integridade verificada: coleção e pacotes offline estão consistentes."
                            }
                        }){Text("Verificar")}}
                    )
                }
            }

            item{
                CompanionSectionHeader(title="Áudio")
                CompanionSettingsGroup{
                    CompanionSettingsRow(
                        title="Música e sons",
                        supporting="Controle o áudio do Companion.",
                        icon=Icons.Default.VolumeUp,
                        trailing={Switch(checked=audioEnabled,onCheckedChange={audioEnabled=it; HomeAudioManager.setEnabled(it)})}
                    )
                    CompanionSettingsDivider()
                    Column(Modifier.fillMaxWidth().padding(horizontal=PokedexDesignTokens.Spacing.Lg,vertical=PokedexDesignTokens.Spacing.Sm)){
                        Text("Volume",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(value=audioVolume,onValueChange={audioVolume=it; HomeAudioManager.volume=it},enabled=audioEnabled,valueRange=0f..1f)
                    }
                }
            }

            item{
                CompanionSectionHeader(title="Backup e restauração")
                Text(
                    "Preserva coleção, Jornada, times, formas, Shiny e preferências. Pacotes offline precisam ser baixados novamente após uma restauração.",
                    style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant
                )
                CompanionSettingsGroup(Modifier.padding(top=PokedexDesignTokens.Spacing.Sm)){
                    CompanionSettingsRow("Copiar backup","Copia os dados em JSON para a área de transferência.",Icons.Default.ContentCopy,trailing={TextButton(onClick={clipboard.setText(AnnotatedString(AppBackupManager.exportJson()));statusText="Backup copiado."}){Text("Copiar")}})
                    CompanionSettingsDivider()
                    CompanionSettingsRow("Salvar arquivo","Cria um arquivo de backup no dispositivo.",Icons.Default.Save,trailing={TextButton(onClick={createBackup.launch("pokedex-backup.json")}){Text("Salvar")}})
                    CompanionSettingsDivider()
                    CompanionSettingsRow("Colar backup","Restaura um backup JSON copiado anteriormente.",Icons.Default.Restore,trailing={TextButton(onClick={restoreOpen=true}){Text("Colar")}})
                    CompanionSettingsDivider()
                    CompanionSettingsRow("Importar arquivo","Restaura um arquivo de backup salvo.",Icons.Default.FolderOpen,trailing={TextButton(onClick={openBackup.launch(arrayOf("application/json","text/plain"))}){Text("Importar")}})
                }
            }

            item{
                CompanionSectionHeader(title="Sobre")
                val versionName=remember(context){runCatching{context.packageManager.getPackageInfo(context.packageName,0).versionName ?: "—"}.getOrDefault("—")}
                CompanionSettingsGroup{
                    CompanionSettingsRow("POKEDEX v$versionName",context.packageName,Icons.Default.Info)
                }
            }

            statusText?.let{message->item{
                DexGlassSurface(Modifier.fillMaxWidth()){
                    Text(message,Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)
                }
            }}
        }
    }

    if(restoreOpen){
        AlertDialog(
            onDismissRequest={restoreOpen=false},
            title={Text("Restaurar backup")},
            text={Column{
                Text("Cole abaixo o backup exportado anteriormente.")
                OutlinedTextField(value=restoreText,onValueChange={restoreText=it},modifier=Modifier.fillMaxWidth().heightIn(min=140.dp).padding(top=PokedexDesignTokens.Spacing.Sm),label={Text("Backup JSON")})
            }},
            confirmButton={TextButton(onClick={
                val pasted=restoreText
                scope.launch{
                    val ok=withContext(Dispatchers.IO){AppBackupManager.importJson(pasted)}
                    statusText=if(ok) "Backup restaurado com sucesso." else "Backup inválido ou incompatível."
                    if(ok){restoreText="";restoreOpen=false}
                }
            }){Text("Restaurar")}},
            dismissButton={TextButton(onClick={restoreOpen=false}){Text("Cancelar")}}
        )
    }
}

private data class SettingsPerformanceSnapshot(
    val cache:PokedexDataStore.CacheStats,
    val apiCacheBytes:Long,
    val downloadedPacks:Int,
    val startupMs:Long
)

private fun loadSettingsPerformanceSnapshot()=SettingsPerformanceSnapshot(
    cache=PokedexDataStore.cacheStats(),
    apiCacheBytes=PersistentApiCache.sizeBytes(),
    downloadedPacks=AppGameCatalog.adventureGames.count{OfflineGamePackManager.status(it.label).downloaded},
    startupMs=StartupPreloader.lastWarmDurationMs
)
