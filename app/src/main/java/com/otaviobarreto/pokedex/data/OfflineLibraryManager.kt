package com.otaviobarreto.pokedex.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Durable offline library. Downloaded resources live in filesDir, never in an
 * evictable image cache. Activation is atomic at directory level.
 */
object OfflineLibraryManager {
    private const val ROOT="offline-library"
    private const val GENERAL="general"

    fun root(context:Context)=File(context.filesDir,ROOT).apply{mkdirs()}
    fun general(context:Context)=File(root(context),GENERAL)
    private fun previous(context:Context)=File(root(context),"$GENERAL.previous")

    fun installImage(
        stagingRoot:File,
        relativePath:String,
        cacheKey:String,
        sourceUrl:String?
    ):File {
        val source=File(stagingRoot,relativePath).canonicalFile
        check(source.exists()){"Imagem ausente: $relativePath"}
        val indexDir=File(stagingRoot,"local-index").apply{mkdirs()}
        val keyFile=File(indexDir,safe(cacheKey)+".path")
        keyFile.writeText(relativePath)
        sourceUrl?.let{File(indexDir,safe(it)+".path").writeText(relativePath)}
        return source
    }

    fun activateGeneral(context:Context,staging:File,version:Int,ids:Set<Int>){
        val manifest=File(staging,"manifest.json")
        check(manifest.exists()){"manifest.json ausente antes da ativação"}
        val meta=JSONObject()
            .put("version",version)
            .put("pokemon_count",ids.size)
            .put("pokemon_ids",JSONArray(ids.toList()))
        File(staging,"library-state.json").writeText(meta.toString())

        val active=general(context)
        val old=previous(context)
        if(old.exists()) old.deleteRecursively()
        if(active.exists()) check(active.renameTo(old)){"Não foi possível preservar biblioteca anterior"}
        if(!staging.renameTo(active)){
            if(active.exists()) active.deleteRecursively()
            if(old.exists()) old.renameTo(active)
            error("Não foi possível ativar biblioteca offline")
        }
        if(old.exists()) old.deleteRecursively()
    }

    fun auditGeneral(context:Context,version:Int,ids:Set<Int>):Boolean {
        val active=general(context)
        val state=File(active,"library-state.json")
        val manifest=File(active,"manifest.json")
        if(!active.isDirectory || !state.exists() || !manifest.exists()) return false
        val json=runCatching{JSONObject(state.readText())}.getOrNull() ?: return false
        if(json.optInt("version",-1)!=version || json.optInt("pokemon_count",-1)!=ids.size) return false
        val index=File(active,"local-index")
        return index.isDirectory && ids.all{id->
            File(index,safe("pokemon-offline-$id")+".path").exists()
        }
    }

    fun resolve(context:Context,key:String):File? {
        val active=general(context)
        val pointer=File(File(active,"local-index"),safe(key)+".path")
        if(!pointer.exists()) return null
        val relative=runCatching{pointer.readText()}.getOrNull()?.takeIf{it.isNotBlank()} ?: return null
        val root=active.canonicalFile
        val target=File(active,relative).canonicalFile
        if(!(target.path.startsWith(root.path+File.separator) || target==root)) return null
        return target.takeIf{it.exists()}
    }

    fun installResource(stagingRoot:File,relativePath:String,url:String):File {
        val source=File(stagingRoot,relativePath).canonicalFile
        check(source.exists()){"Recurso ausente: $relativePath"}
        val indexDir=File(stagingRoot,"resource-index").apply{mkdirs()}
        File(indexDir,safe(url)+".path").writeText(relativePath)
        return source
    }

    fun resolveResource(context:Context,url:String):File? {
        val active=general(context)
        val pointer=File(File(active,"resource-index"),safe(url)+".path")
        if(!pointer.exists()) return null
        val relative=runCatching{pointer.readText()}.getOrNull()?.takeIf{it.isNotBlank()} ?: return null
        val root=active.canonicalFile
        val target=File(active,relative).canonicalFile
        if(!(target.path.startsWith(root.path+File.separator) || target==root)) return null
        return target.takeIf{it.exists()}
    }

    fun installGameResource(context:Context,gameKey:String,source:File,sourceUrl:String):File {
        val dir=File(root(context),"games/"+safe(gameKey)+"/resources").apply{mkdirs()}
        val target=File(dir,safe(sourceUrl)+".json")
        source.copyTo(target,overwrite=true)
        val index=File(root(context),"game-resource-index").apply{mkdirs()}
        File(index,safe(sourceUrl)+".path").writeText(target.absolutePath)
        return target
    }

    fun resolveAnyResource(context:Context,url:String):File? {
        resolveResource(context,url)?.let{return it}
        val pointer=File(File(root(context),"game-resource-index"),safe(url)+".path")
        return pointer.takeIf{it.exists()}?.readText()?.let{File(it)}?.takeIf{it.exists()}
    }

    fun auditGame(context:Context,gameKey:String,resourceUrls:Set<String>,visualUrls:Set<String>):Boolean {
        return resourceUrls.all{resolveAnyResource(context,it)?.exists()==true} &&
            visualUrls.all{resolveAny(context,it)?.exists()==true}
    }

    fun installGameVisual(context:Context,gameKey:String,source:File,sourceUrl:String,cacheKey:String):File {
        val dir=File(root(context),"games/"+safe(gameKey)).apply{mkdirs()}
        val target=File(dir,safe(sourceUrl)+".img")
        source.copyTo(target,overwrite=true)
        val index=File(root(context),"game-index").apply{mkdirs()}
        File(index,safe(sourceUrl)+".path").writeText(target.absolutePath)
        File(index,safe(cacheKey)+".path").writeText(target.absolutePath)
        return target
    }

    fun resolveAny(context:Context,key:String):File? {
        resolve(context,key)?.let{return it}
        val pointer=File(File(root(context),"game-index"),safe(key)+".path")
        return pointer.takeIf{it.exists()}?.readText()?.let{File(it)}?.takeIf{it.exists()}
    }

    fun removeGeneral(context:Context){
        runCatching{general(context).deleteRecursively()}
        runCatching{previous(context).deleteRecursively()}
    }

    private fun safe(value:String):String {
        val bytes=java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString(""){"%02x".format(it)}
    }
}
