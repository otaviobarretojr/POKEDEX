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

    data class AuditResult(
        val ok:Boolean,
        val message:String,
        val pokemonCount:Int=0,
        val resourcesChecked:Int=0,
        val imagesChecked:Int=0
    )

    fun auditStaging(staging:File,version:Int):AuditResult {
        if(!staging.isDirectory) return AuditResult(false,"staging ausente")
        val manifestFile=File(staging,"manifest.json")
        if(!manifestFile.exists()) return AuditResult(false,"manifest.json ausente")
        val manifest=runCatching{JSONObject(manifestFile.readText())}.getOrElse{
            return AuditResult(false,"manifest.json inválido: "+(it.message ?: "erro"))
        }
        if(manifest.optString("package_key")!="general") return AuditResult(false,"package_key inválido")
        val pokemon=manifest.optJSONArray("pokemon") ?: return AuditResult(false,"lista pokemon ausente")
        var resources=0
        var images=0
        for(i in 0 until pokemon.length()){
            val item=pokemon.optJSONObject(i) ?: return AuditResult(false,"pokemon["+i+"] inválido",i,resources,images)
            item.optJSONArray("resources")?.let{array->
                for(j in 0 until array.length()){
                    val entry=array.optJSONObject(j) ?: return AuditResult(false,"resource inválido em pokemon["+i+"]",i,resources,images)
                    val path=entry.optString("path")
                    if(path.isBlank() || !File(staging,path).exists()) return AuditResult(false,"recurso ausente: "+path,i,resources,images)
                    resources++
                }
            }
            item.optJSONArray("images")?.let{array->
                for(j in 0 until array.length()){
                    val entry=array.optJSONObject(j) ?: return AuditResult(false,"imagem inválida em pokemon["+i+"]",i,resources,images)
                    val path=entry.optString("path")
                    if(path.isBlank() || !File(staging,path).exists()) return AuditResult(false,"imagem ausente: "+path,i,resources,images)
                    images++
                }
            }
        }
        manifest.optJSONArray("reference_resources")?.let{array->
            for(i in 0 until array.length()){
                val entry=array.optJSONObject(i) ?: return AuditResult(false,"reference_resource["+i+"] inválido",pokemon.length(),resources,images)
                val path=entry.optString("path")
                if(path.isBlank() || !File(staging,path).exists()) return AuditResult(false,"referência ausente: "+path,pokemon.length(),resources,images)
                resources++
            }
        }
        return AuditResult(true,pokemon.length().toString()+" Pokémon · "+resources+" recursos · "+images+" imagens",pokemon.length(),resources,images)
    }

    fun auditGeneralDetailed(context:Context,version:Int):AuditResult {
        val active=general(context)
        val state=File(active,"library-state.json")
        if(!state.exists()) return AuditResult(false,"library-state.json ausente")
        val json=runCatching{JSONObject(state.readText())}.getOrElse{return AuditResult(false,"library-state.json inválido")}
        if(json.optInt("version",-1)!=version) return AuditResult(false,"versão local "+json.optInt("version",-1)+" != "+version)
        val content=auditStaging(active,version)
        if(!content.ok) return content
        if(json.optInt("pokemon_count",-1)!=content.pokemonCount) return AuditResult(false,"contagem gravada "+json.optInt("pokemon_count",-1)+" != "+content.pokemonCount)
        return content
    }

    fun auditGeneral(context:Context,version:Int,ids:Set<Int>):Boolean =
        auditGeneralDetailed(context,version).ok

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
