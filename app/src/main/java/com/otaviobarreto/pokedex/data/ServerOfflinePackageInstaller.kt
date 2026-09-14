package com.otaviobarreto.pokedex.data

import android.content.Context
import coil.imageLoader
import coil.annotation.ExperimentalCoilApi
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ServerOfflinePackageInstaller {
    data class Progress(
        val downloadedBytes:Long,
        val totalBytes:Long,
        val label:String
    ){
        val fraction:Float
            get()=if(totalBytes<=0L) 0f else
                (downloadedBytes.toDouble()/totalBytes.toDouble()).toFloat().coerceIn(0f,1f)
    }

    suspend fun installGeneral(
        context:Context,
        remote:RemoteOfflinePackageCatalog.RemotePackage,
        onProgress:(Progress)->Unit
    ) = withContext(Dispatchers.IO){
        require(remote.packageKey=="general"){"Pacote remoto inválido"}
        require(remote.ready){"Pacote geral ainda não está pronto no servidor"}
        val url=requireNotNull(remote.downloadUrl)
        val expectedSha=requireNotNull(remote.sha256).lowercase()
        val total=remote.sizeBytes ?: 0L

        OfflineGamePackManager.beginServerGeneralInstall()
        val root=File(context.cacheDir,"server-offline-packages").apply{mkdirs()}
        val zipFile=File(root,"general-v${remote.version}.zip")
        downloadResumable(url,zipFile,total,"Baixando pacote geral",onProgress)

        onProgress(Progress(zipFile.length(),total,"Validando integridade"))
        val actualSha=sha256(zipFile)
        check(actualSha.equals(expectedSha,ignoreCase=true)){
            "SHA-256 do pacote geral não confere"
        }

        val extractDir=File(root,"general-v${remote.version}-extract")
        if(extractDir.exists()) extractDir.deleteRecursively()
        extractDir.mkdirs()
        onProgress(Progress(total,total,"Extraindo pacote"))
        unzipSafe(zipFile,extractDir)

        val manifestFile=File(extractDir,"manifest.json")
        check(manifestFile.exists()){"manifest.json ausente"}
        val manifest=JSONObject(manifestFile.readText())
        check(manifest.optString("package_key")=="general"){"Manifesto geral inválido"}
        check(manifest.optInt("schema",0)==1){"Versão de manifesto não suportada"}

        val pokemon=manifest.getJSONArray("pokemon")
        val ids=linkedSetOf<Int>()
        onProgress(Progress(total,total,"Instalando biblioteca · 0 / ${pokemon.length()}"))
        for(i in 0 until pokemon.length()){
            val item=pokemon.getJSONObject(i)
            val id=item.getInt("id")
            ids += id

            val resources=linkedSetOf<String>()
            val resourceArray=item.optJSONArray("resources")
            if(resourceArray!=null){
                for(j in 0 until resourceArray.length()){
                    val resource=resourceArray.getJSONObject(j)
                    val resourceUrl=resource.getString("url")
                    val path=resource.getString("path")
                    val file=resolveInside(extractDir,path)
                    check(file.exists()){"Recurso ausente: $path"}
                    PersistentApiCache.importRaw(resourceUrl,file.readText(),pin=true)
                    resources += resourceUrl
                }
            }

            val formKeys=linkedSetOf<String>()
            val images=item.optJSONArray("images")
            if(images!=null){
                for(j in 0 until images.length()){
                    val image=images.getJSONObject(j)
                    val cacheKey=image.getString("cache_key")
                    val sourceUrl=image.optString("source_url").takeIf{it.isNotBlank()}
                    val path=image.getString("path")
                    val file=resolveInside(extractDir,path)
                    check(file.exists()){"Imagem ausente: $path"}
                    val imported=runCatching{
                        importImageIntoDiskCache(context,cacheKey,file)
                        sourceUrl?.let{url->importImageIntoDiskCache(context,url,file)}
                    }.isSuccess
                    if(cacheKey=="pokemon-offline-$id"){
                        check(imported){"Falha ao importar arte principal #$id"}
                    }else if(imported && cacheKey.startsWith("pokemon-form-offline-")){
                        formKeys += cacheKey
                    }
                }
            }

            OfflineGamePackManager.registerImportedGeneralPokemon(
                id=id,
                resources=resources,
                formKeys=formKeys
            )
            onProgress(
                Progress(
                    downloadedBytes=total,
                    totalBytes=total,
                    label="Importando biblioteca · ${i+1} / ${pokemon.length()}"
                )
            )
        }

        val refs=manifest.optJSONArray("reference_resources")
        if(refs!=null){
            for(i in 0 until refs.length()){
                val resource=refs.getJSONObject(i)
                val resourceUrl=resource.getString("url")
                val path=resource.getString("path")
                val file=resolveInside(extractDir,path)
                check(file.exists()){"Referência ausente: $path"}
                PersistentApiCache.importRaw(resourceUrl,file.readText(),pin=true)
            }
        }

        OfflineGamePackManager.finalizeImportedGeneral(ids,remote.version)
        check(OfflineGamePackManager.generalAudit()){"Biblioteca importada falhou na auditoria"}
        onProgress(Progress(total,total,"Biblioteca geral pronta"))

        runCatching{extractDir.deleteRecursively()}
        runCatching{zipFile.delete()}
    }

    suspend fun installGame(
        context:Context,
        game:AppGame,
        remote:RemoteOfflinePackageCatalog.RemotePackage,
        onProgress:(Progress)->Unit
    ) = withContext(Dispatchers.IO){
        require(remote.packageType=="game"){"Pacote remoto de jogo inválido"}
        require(remote.ready){"Complemento ainda não está pronto no servidor"}
        require(OfflineGamePackManager.generalAudit()){
            "Baixe a biblioteca geral antes do complemento deste jogo"
        }

        val url=requireNotNull(remote.downloadUrl)
        val expectedSha=requireNotNull(remote.sha256).lowercase()
        val total=remote.sizeBytes ?: 0L
        val root=File(context.cacheDir,"server-offline-packages").apply{mkdirs()}
        val safeKey=remote.packageKey.replace(Regex("[^a-zA-Z0-9._-]"),"_")
        val zipFile=File(root,"$safeKey-v${remote.version}.zip")
        downloadResumable(url,zipFile,total,"Baixando complemento"){p->onProgress(p)}

        onProgress(Progress(zipFile.length(),total,"Validando complemento"))
        check(sha256(zipFile).equals(expectedSha,ignoreCase=true)){
            "SHA-256 do complemento não confere"
        }

        val extractDir=File(root,"$safeKey-v${remote.version}-extract")
        if(extractDir.exists()) extractDir.deleteRecursively()
        extractDir.mkdirs()
        unzipSafe(zipFile,extractDir)

        val manifestFile=File(extractDir,"manifest.json")
        check(manifestFile.exists()){"manifest.json ausente"}
        val manifest=JSONObject(manifestFile.readText())
        check(manifest.optInt("schema",0)==1){"Versão de manifesto não suportada"}
        check(manifest.optString("package_key")==remote.packageKey){"Chave do complemento inválida"}
        check(manifest.optString("game_label")==game.label){"Complemento pertence a outro jogo"}

        val ids=linkedSetOf<Int>()
        val idArray=manifest.getJSONArray("pokemon_ids")
        for(i in 0 until idArray.length()) ids += idArray.getInt(i)

        val regionSlugs=mutableListOf<String>()
        val resources=linkedSetOf<String>()
        val regions=manifest.getJSONArray("regions")
        for(i in 0 until regions.length()){
            val item=regions.getJSONObject(i)
            val slug=item.getString("slug")
            val resourceUrl=item.getString("url")
            val path=item.getString("path")
            val file=resolveInside(extractDir,path)
            check(file.exists()){"Pokédex regional ausente: $path"}
            PersistentApiCache.importRaw(resourceUrl,file.readText(),pin=true)
            regionSlugs += slug
            resources += resourceUrl
        }

        val visualUrls=linkedSetOf<String>()
        val visuals=manifest.optJSONArray("visuals")
        if(visuals!=null){
            for(i in 0 until visuals.length()){
                val item=visuals.getJSONObject(i)
                val visualUrl=item.getString("url")
                val path=item.getString("path")
                val file=resolveInside(extractDir,path)
                check(file.exists()){"Visual ausente: $path"}
                importImageIntoDiskCache(
                    context,
                    OfflineGamePackManager.journeyVisualCacheKey(visualUrl),
                    file
                )
                importImageIntoDiskCache(context,visualUrl,file)
                visualUrls += visualUrl
                onProgress(
                    Progress(
                        downloadedBytes=total,
                        totalBytes=total,
                        label="Importando complemento · ${i+1} / ${visuals.length()}"
                    )
                )
            }
        }

        OfflineGamePackManager.finalizeImportedGame(
            gameLabel=game.label,
            ids=ids,
            regions=regionSlugs,
            resourceUrls=resources,
            visualUrls=visualUrls,
            serverVersion=remote.version
        )
        check(OfflineGamePackManager.auditImportedGameFast(game.label)){
            "Complemento importado falhou na auditoria"
        }

        onProgress(Progress(total,total,"Complemento pronto"))
        runCatching{extractDir.deleteRecursively()}
        runCatching{zipFile.delete()}
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun importImageIntoDiskCache(context:Context,cacheKey:String,file:File){
        val disk=requireNotNull(context.imageLoader.diskCache){"Cache de imagens indisponível"}
        val editor=requireNotNull(disk.openEditor(cacheKey)){"Não foi possível abrir o cache $cacheKey"}
        try{
            val bytes=file.readBytes()
            disk.fileSystem.write(editor.data){
                write(bytes)
            }
            editor.commit()
        }catch(t:Throwable){
            editor.abort()
            throw t
        }
    }

    private fun humanBytes(bytes:Long):String =
        if(bytes>=1024L*1024L*1024L)
            String.format("%.2f GB",bytes/1024.0/1024.0/1024.0)
        else
            String.format("%.1f MB",bytes/1024.0/1024.0)

    private fun humanSpeed(bytesPerSecond:Long):String =
        humanBytes(bytesPerSecond)+"/s"

    private fun downloadResumable(
        url:String,
        destination:File,
        expectedBytes:Long,
        label:String,
        onProgress:(Progress)->Unit
    ){
        val existing=destination.takeIf{it.exists()}?.length() ?: 0L
        val connection=(URL(url).openConnection() as HttpURLConnection).apply{
            connectTimeout=15_000
            readTimeout=30_000
            requestMethod="GET"
            setRequestProperty("Accept","application/octet-stream")
            if(existing>0L) setRequestProperty("Range","bytes=$existing-")
        }

        connection.connect()
        val response=connection.responseCode
        val append=existing>0L && response==HttpURLConnection.HTTP_PARTIAL
        if(response !in 200..299){
            connection.disconnect()
            error("Falha HTTP $response ao baixar pacote")
        }

        if(!append && existing>0L) destination.delete()
        val start=if(append) existing else 0L
        val serverLength=connection.contentLengthLong.coerceAtLeast(0L)
        val total=when{
            expectedBytes>0L -> expectedBytes
            append -> start+serverLength
            else -> serverLength
        }

        val transferStartedAt=System.nanoTime()
        val transferStartBytes=start
        FileOutputStream(destination,append).buffered(1024*1024).use{out->
            connection.inputStream.buffered(1024*1024).use{input->
                val buffer=ByteArray(1024*1024)
                var downloaded=start
                var read:Int
                while(input.read(buffer).also{read=it}>=0){
                    if(read==0) continue
                    out.write(buffer,0,read)
                    downloaded += read
                    val elapsed=((System.nanoTime()-transferStartedAt)/1_000_000_000.0).coerceAtLeast(.05)
                    val speed=((downloaded-transferStartBytes)/elapsed).toLong().coerceAtLeast(0L)
                    val progressLabel=buildString{
                        append(label)
                        append(" · ")
                        append(humanBytes(downloaded))
                        if(total>0L){
                            append(" / ")
                            append(humanBytes(total))
                        }
                        if(speed>0L){
                            append(" · ")
                            append(humanSpeed(speed))
                        }
                    }
                    onProgress(Progress(downloaded,total,progressLabel))
                }
            }
        }
        connection.disconnect()
    }

    private fun unzipSafe(zipFile:File,destination:File){
        ZipInputStream(zipFile.inputStream().buffered()).use{zip->
            var entry=zip.nextEntry
            while(entry!=null){
                val out=resolveInside(destination,entry.name)
                if(entry.isDirectory){
                    out.mkdirs()
                }else{
                    out.parentFile?.mkdirs()
                    out.outputStream().buffered().use{output->zip.copyTo(output)}
                }
                zip.closeEntry()
                entry=zip.nextEntry
            }
        }
    }

    private fun resolveInside(root:File,relative:String):File{
        val rootPath=root.canonicalFile
        val file=File(root,relative).canonicalFile
        check(file.path.startsWith(rootPath.path+File.separator) || file==rootPath){
            "Caminho inválido no pacote"
        }
        return file
    }

    private fun sha256(file:File):String{
        val digest=MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(1024*1024).use{input->
            val buffer=ByteArray(1024*1024)
            var read:Int
            while(input.read(buffer).also{read=it}>=0){
                if(read>0) digest.update(buffer,0,read)
            }
        }
        return digest.digest().joinToString(""){"%02x".format(it)}
    }
}
