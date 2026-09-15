package com.otaviobarreto.pokedex.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            val installedGames=gamePackages.count{p->OfflineGamePackManager.status(p.displayName).downloaded}
            if(generalReady && installedGames==gamePackages.size){
                context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_READY,signature).commit()
                onProgress(Progress(1f,"Biblioteca pronta"))
                return@runCatching Result(true)
            }
            val total=packages.sumOf{p->p.sizeBytes ?: 0L}.coerceAtLeast(1L)
            var completed=0L
            if(!generalReady){
                val ok=ServerOfflinePackageInstaller.cleanRepairGeneral(context,general){p->
                    val current=p.done.coerceAtMost(p.total.coerceAtLeast(0L))
                    onProgress(Progress((current.toDouble()/total).toFloat().coerceIn(0f,.99f),"Biblioteca principal · "+p.label,current,total))
                }
                check(ok){"Biblioteca principal não passou na auditoria"}
            }
            completed=general.sizeBytes ?: 0L
            gamePackages.forEachIndexed{index,pkg->
                if(!OfflineGamePackManager.status(pkg.displayName).downloaded){
                    ServerOfflinePackageInstaller.installGame(context,pkg){p->
                        val current=completed+p.done.coerceAtMost(p.total.coerceAtLeast(0L))
                        onProgress(Progress((current.toDouble()/total).toFloat().coerceIn(0f,.99f),pkg.displayName+" · "+p.label,current,total))
                    }
                }
                completed+=(pkg.sizeBytes ?: 0L)
                onProgress(Progress((completed.toDouble()/total).toFloat().coerceIn(0f,.99f),"Validando jogos "+(index+1)+"/"+gamePackages.size,completed,total))
            }
            check(OfflineLibraryManager.auditGeneral(context,general.version,OfflineGamePackManager.generalManifestIds())){"Auditoria final da biblioteca principal falhou"}
            val missing=gamePackages.filterNot{p->OfflineGamePackManager.status(p.displayName).downloaded}
            check(missing.isEmpty()){"Pacotes de jogos ainda pendentes"}
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
