package com.otaviobarreto.pokedex.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object ContentBootstrapManager {
    data class Progress(val fraction:Float,val label:String,val downloadedBytes:Long=0,val totalBytes:Long=0)
    data class Result(val ready:Boolean,val error:String?=null)
    private const val PREFS="content_bootstrap_v2"
    private const val KEY_READY="ready_signature"
    private const val LEGACY_CLEANED="legacy_cleaned"

    suspend fun ensureReady(context:Context,onProgress:(Progress)->Unit):Result=withContext(Dispatchers.IO){
        runCatching{
            cleanupLegacyOnce(context)
            onProgress(Progress(.02f,"Verificando biblioteca POKEDEX"))
            val packages=RemoteOfflinePackageCatalog.refresh(force=true).filter{it.ready}
            check(packages.isNotEmpty()){"Servidor de conteúdo indisponível"}
            val general=packages.firstOrNull{it.packageKey=="general"} ?: error("Biblioteca principal indisponível")
            val gamePackages=packages.filter{it.packageKey!="general"}.distinctBy{it.packageKey}
            val signature=packages.sortedBy{it.packageKey}.joinToString("|"){p->p.packageKey+":"+p.version+":"+p.sha256}
            val generalIds=OfflineGamePackManager.generalManifestIds()
            val generalReady=OfflineLibraryManager.auditGeneral(context,general.version,generalIds)
            fun localGameFor(p:RemoteOfflinePackageCatalog.RemotePackage)=
                AppGameCatalog.games.firstOrNull{g->RemoteOfflinePackageCatalog.packageKeyForGame(g.label)==p.packageKey}
            fun gameReady(p:RemoteOfflinePackageCatalog.RemotePackage):Boolean {
                val game=localGameFor(p) ?: return false
                val status=OfflineGamePackManager.status(game.label)
                if(!status.downloaded || OfflineGamePackManager.gameServerVersion(game.label)!=p.version) return false
                val resources=OfflineGamePackManager.gameResourceUrls(game.label)
                val visuals=OfflineGamePackManager.gameVisualUrls(game.label)
                return OfflineLibraryManager.auditGame(context,p.packageKey,resources,visuals)
            }
            val installedGames=gamePackages.count(::gameReady)
            if(generalReady && installedGames==gamePackages.size){
                context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_READY,signature).commit()
                onProgress(Progress(1f,"Biblioteca pronta"))
                return@runCatching Result(true)
            }
            val total=packages.sumOf{p->p.sizeBytes ?: 0L}.coerceAtLeast(1L)
            var completed=0L
            if(!generalReady){
                val ok=ServerOfflinePackageInstaller.installOrUpdateGeneral(context,general){p->
                    val label=p.label
                    val stageFraction=when{
                        label.startsWith("Baixando") -> .70f*p.fraction
                        label.startsWith("Validando integridade") -> .70f
                        label.startsWith("Extraindo") -> .78f
                        label.startsWith("Instalando") || label.startsWith("Importando") -> .80f+(.15f*p.fraction)
                        label.startsWith("Validado") || label.startsWith("Biblioteca geral pronta") -> .95f
                        else -> (.70f*p.fraction).coerceAtLeast(.02f)
                    }
                    onProgress(Progress(stageFraction.coerceIn(.02f,.95f),"Biblioteca principal · "+label,p.downloadedBytes,p.totalBytes))
                }
                if(!ok){
                    val state=OfflinePackageInstallState.read(context)
                    error(state.error?.takeIf{it.isNotBlank()} ?: "Biblioteca principal não passou na auditoria")
                }
            }
            completed=general.sizeBytes ?: 0L
            val gameBase=completed
            val perGame=java.util.concurrent.ConcurrentHashMap<String,Long>()
            val semaphore=Semaphore(3)
            coroutineScope{
                gamePackages.map{pkg->
                    async{
                        semaphore.withPermit{
                            if(!gameReady(pkg)){
                                val game=localGameFor(pkg)
                                    ?: error("Jogo sem mapeamento local: "+pkg.packageKey)
                                ServerOfflinePackageInstaller.installGame(context,game,pkg){p->
                                    perGame[pkg.packageKey]=p.downloadedBytes.coerceAtMost(p.totalBytes.coerceAtLeast(0L))
                                    val current=gameBase+perGame.values.sum()
                                    onProgress(Progress((current.toDouble()/total).toFloat().coerceIn(0f,.99f),pkg.displayName+" · "+p.label,current,total))
                                }
                            }else{
                                perGame[pkg.packageKey]=pkg.sizeBytes ?: 0L
                            }
                        }
                    }
                }.awaitAll()
            }
            completed=gameBase+gamePackages.sumOf{it.sizeBytes ?: 0L}
            onProgress(Progress((completed.toDouble()/total).toFloat().coerceIn(0f,.99f),"Validando biblioteca completa",completed,total))
            val finalGeneralAudit=OfflineLibraryManager.auditGeneralDetailed(context,general.version)
            check(finalGeneralAudit.ok){"Auditoria final: "+finalGeneralAudit.message}
            val missing=gamePackages.filterNot(::gameReady)
            check(missing.isEmpty()){
                "Complementos pendentes: "+missing.joinToString(", "){p->
                    localGameFor(p)?.label ?: p.packageKey
                }
            }
            context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_READY,signature).commit()
            onProgress(Progress(1f,"POKEDEX pronto",total,total))
            Result(true)
        }.getOrElse{e->Result(false,e.message ?: e.javaClass.simpleName)}
    }

    private fun cleanupLegacyOnce(context:Context){
        val p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        if(p.getBoolean(LEGACY_CLEANED,false)) return
        OfflinePackageInstallState.packageRoot(context).listFiles()?.forEach{file->
            if(file.name.contains("extract") || file.name.endsWith(".part")) runCatching{file.deleteRecursively()}
        }
        check(p.edit().putBoolean(LEGACY_CLEANED,true).commit()){"Falha ao concluir migração do conteúdo"}
    }
}
