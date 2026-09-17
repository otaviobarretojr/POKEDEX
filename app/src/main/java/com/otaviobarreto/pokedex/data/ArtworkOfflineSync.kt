package com.otaviobarreto.pokedex.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keeps artwork in durable app storage instead of relying only on Coil's
 * evictable disk cache. Startup audits every declared artwork URL, repairs
 * missing files and tracks the upstream PokeAPI/sprites artwork revision so
 * changed official artwork is refreshed without waiting for a screen to open.
 */
object ArtworkOfflineSync {
    data class Progress(
        val fraction:Float,
        val label:String,
        val done:Int=0,
        val total:Int=0
    )

    data class Status(
        val total:Int=0,
        val missing:Int=0,
        val changed:Int=0,
        val downloaded:Int=0,
        val failed:Int=0,
        val running:Boolean=false,
        val revision:String?=null,
        val updatedAt:Long=0L
    )

    private const val PREFS="artwork_offline_sync_v1"
    private const val KEY_REVISION="sprites_revision"
    private const val KEY_INVENTORY="inventory_signature"
    private const val RAW_ART="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork"
    private const val SPRITES_PATH="sprites/pokemon/other/official-artwork/"
    private const val REVISION_URL="https://api.github.com/repos/PokeAPI/sprites/commits?path=sprites/pokemon/other/official-artwork&per_page=1"
    private const val DOWNLOAD_CONCURRENCY=6

    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val running=AtomicBoolean(false)
    @Volatile var lastStatus=Status()
        private set

    fun launch(context:Context){
        val appContext=context.applicationContext
        scope.launch{sync(appContext)}
    }

    suspend fun sync(
        context:Context,
        onProgress:suspend (Progress)->Unit = {}
    ):Status=withContext(Dispatchers.IO){
        if(!running.compareAndSet(false,true)) return@withContext lastStatus
        try{
        val appContext=context.applicationContext
        onProgress(Progress(0f,"Verificando artworks"))
        val prefs=appContext.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val inventory=artworkInventory(appContext)
        val previousRevision=prefs.getString(KEY_REVISION,null)
        val remoteRevision=runCatching{fetchLatestRevision()}.getOrNull()
        val online=remoteRevision!=null || canReachArtwork()

        val missing=inventory.filter{OfflineLibraryManager.resolveAny(appContext,it)==null}.toSet()
        val changed=when{
            remoteRevision.isNullOrBlank() || previousRevision.isNullOrBlank() || remoteRevision==previousRevision -> emptySet()
            else -> runCatching{fetchChangedOfficialArtwork(previousRevision,remoteRevision,inventory)}
                .getOrElse{officialArtworkUrls().filterTo(linkedSetOf()){it in inventory}}
        }

        val queue=(missing+changed)
            .distinct()
            .sortedWith(compareBy<String>(
                {if("/official-artwork/shiny/" in it)0 else 1},
                {if("/official-artwork/" in it)0 else 1},
                {it}
            ))

        lastStatus=Status(
            total=inventory.size,
            missing=missing.size,
            changed=changed.size,
            running=true,
            revision=remoteRevision ?: previousRevision
        )
        persistStatus(appContext,lastStatus)
        onProgress(
            Progress(
                fraction=if(queue.isEmpty())1f else 0f,
                label=when{
                    queue.isEmpty()->"Artworks prontas"
                    !online->"Offline · "+queue.size+" artworks pendentes"
                    else->"Baixando artworks 0 / "+queue.size
                },
                done=0,
                total=queue.size
            )
        )

        if(!online || queue.isEmpty()){
            val done=lastStatus.copy(running=false,updatedAt=System.currentTimeMillis())
            if(queue.isEmpty() && !remoteRevision.isNullOrBlank()){
                prefs.edit()
                    .putString(KEY_REVISION,remoteRevision)
                    .putString(KEY_INVENTORY,inventorySignature(inventory))
                    .apply()
            }
            lastStatus=done
            persistStatus(appContext,done)
            onProgress(
                Progress(
                    1f,
                    if(queue.isEmpty())"Artworks prontas" else "Offline · atualização de artworks adiada",
                    queue.size,
                    queue.size
                )
            )
            return@withContext done
        }

        val changedSet=changed.toSet()
        val semaphore=Semaphore(DOWNLOAD_CONCURRENCY)
        var downloaded=0
        var failed=0
        val lock=Any()

        queue.chunked(36).forEach{chunk->
            coroutineScope{
                chunk.map{artworkUrl->
                    async{
                        semaphore.withPermit{
                            val ok=runCatching{
                                val bytes=downloadArtwork(artworkUrl,force=artworkUrl in changedSet)
                                OfflineLibraryManager.installSupplementalArtwork(
                                    appContext,
                                    artworkUrl,
                                    bytes,
                                    cacheKeysFor(artworkUrl)
                                )
                            }.isSuccess
                            val processed=synchronized(lock){
                                if(ok) downloaded++ else failed++
                                lastStatus=lastStatus.copy(
                                    downloaded=downloaded,
                                    failed=failed,
                                    running=true
                                )
                                downloaded+failed
                            }
                            if(processed==queue.size || processed%8==0){
                                onProgress(
                                    Progress(
                                        fraction=processed.toFloat()/queue.size.coerceAtLeast(1),
                                        label="Baixando artworks "+processed+" / "+queue.size,
                                        done=processed,
                                        total=queue.size
                                    )
                                )
                            }
                        }
                    }
                }.awaitAll()
            }
            persistStatus(appContext,lastStatus)
        }

        if(failed==0){
            prefs.edit()
                .putString(KEY_INVENTORY,inventorySignature(inventory))
                .apply()
            if(!remoteRevision.isNullOrBlank()) prefs.edit().putString(KEY_REVISION,remoteRevision).apply()
        }

        val result=lastStatus.copy(running=false,updatedAt=System.currentTimeMillis())
        lastStatus=result
        persistStatus(appContext,result)
        onProgress(
            Progress(
                1f,
                if(failed==0)"Artworks prontas" else "Artworks atualizadas · "+failed+" pendentes",
                queue.size,
                queue.size
            )
        )
        result
        }finally{
            running.set(false)
            lastStatus=lastStatus.copy(running=false)
        }
    }

    fun status(context:Context):Status {
        if(lastStatus.running) return lastStatus
        val p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        return Status(
            total=p.getInt("total",0),
            missing=p.getInt("missing",0),
            changed=p.getInt("changed",0),
            downloaded=p.getInt("downloaded",0),
            failed=p.getInt("failed",0),
            running=false,
            revision=p.getString("revision",null),
            updatedAt=p.getLong("updated_at",0L)
        )
    }

    private fun artworkInventory(context:Context):LinkedHashSet<String> = linkedSetOf<String>().apply{
        addAll(officialArtworkUrls())
        addAll(JourneyTypeIconCatalog.allUrls())
        addAll(generalManifestArtworkUrls(context))
        AppGameCatalog.adventureGames.forEach{game->
            addAll(OfflineGamePackManager.gameVisualUrls(game.label))
            addAll(GameCoverCatalog.coversFor(game.label))
            GameCoverCatalog.heroFor(game.label)?.let(::add)
            JourneyCatalog.steps(game.label).forEach{step->
                JourneyVisualAssetCatalog.forStep(step.id)?.imageUrl?.let(::add)
            }
        }
    }

    private fun officialArtworkUrls():List<String> = buildList{
        for(id in 1..PokeApiService.MAX_NATIONAL_DEX_ID){
            add("$RAW_ART/$id.png")
            add("$RAW_ART/shiny/$id.png")
        }
    }

    private fun generalManifestArtworkUrls(context:Context):Set<String>{
        val manifest=File(OfflineLibraryManager.general(context),"manifest.json")
        if(!manifest.exists()) return emptySet()
        return runCatching{
            val root=JSONObject(manifest.readText())
            val pokemon=root.optJSONArray("pokemon") ?: JSONArray()
            buildSet{
                for(i in 0 until pokemon.length()){
                    val images=pokemon.optJSONObject(i)?.optJSONArray("images") ?: continue
                    for(j in 0 until images.length()){
                        images.optJSONObject(j)?.optString("source_url")
                            ?.takeIf{it.isNotBlank()}
                            ?.let(::add)
                    }
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun fetchLatestRevision():String {
        val body=readText(REVISION_URL)
        val commits=JSONArray(body)
        return commits.optJSONObject(0)?.optString("sha")?.takeIf{it.isNotBlank()}
            ?: error("Revisão de artwork indisponível")
    }

    private fun fetchChangedOfficialArtwork(
        oldRevision:String,
        newRevision:String,
        inventory:Set<String>
    ):Set<String>{
        val body=readText("https://api.github.com/repos/PokeAPI/sprites/compare/$oldRevision...$newRevision")
        val json=JSONObject(body)
        val files=json.optJSONArray("files") ?: error("Comparação de artwork sem arquivos")
        if(files.length()>=300) return officialArtworkUrls().filterTo(linkedSetOf()){it in inventory}
        return buildSet{
            for(i in 0 until files.length()){
                val file=files.optJSONObject(i) ?: continue
                if(file.optString("status")=="removed") continue
                val path=file.optString("filename")
                if(!path.startsWith(SPRITES_PATH)) continue
                val relative=path.removePrefix(SPRITES_PATH)
                val url="$RAW_ART/$relative"
                if(url in inventory) add(url)
            }
        }
    }

    private fun cacheKeysFor(url:String):Set<String>{
        val shiny=Regex("""/official-artwork/shiny/(\d+)\.png(?:\?.*)?$""").find(url)
        if(shiny!=null) return setOf("pokemon-shiny-offline-"+shiny.groupValues[1])
        val normal=Regex("""/official-artwork/(\d+)\.png(?:\?.*)?$""").find(url)
        return normal?.let{setOf("pokemon-offline-"+it.groupValues[1])}.orEmpty()
    }

    private fun canReachArtwork():Boolean = runCatching{
        val conn=(URL("$RAW_ART/25.png").openConnection() as HttpURLConnection).apply{
            requestMethod="HEAD"
            connectTimeout=4_000
            readTimeout=4_000
            setRequestProperty("User-Agent","POKEDEX-Android")
        }
        try{conn.responseCode in 200..399}finally{conn.disconnect()}
    }.getOrDefault(false)

    private fun downloadArtwork(url:String,force:Boolean):ByteArray {
        val conn=(URL(url).openConnection() as HttpURLConnection).apply{
            requestMethod="GET"
            connectTimeout=8_000
            readTimeout=20_000
            instanceFollowRedirects=true
            setRequestProperty("User-Agent","POKEDEX-Android")
            setRequestProperty("Accept","image/*")
            if(force) setRequestProperty("Cache-Control","no-cache")
        }
        return try{
            check(conn.responseCode in 200..299){"Artwork HTTP "+conn.responseCode}
            conn.inputStream.use{it.readBytes()}
        }finally{conn.disconnect()}
    }

    private fun readText(url:String):String {
        val conn=(URL(url).openConnection() as HttpURLConnection).apply{
            requestMethod="GET"
            connectTimeout=6_000
            readTimeout=10_000
            setRequestProperty("User-Agent","POKEDEX-Android")
            setRequestProperty("Accept","application/vnd.github+json")
        }
        return try{
            check(conn.responseCode in 200..299){"Manifest de artwork HTTP "+conn.responseCode}
            conn.inputStream.bufferedReader().use{it.readText()}
        }finally{conn.disconnect()}
    }

    private fun inventorySignature(urls:Set<String>):String {
        val digest=MessageDigest.getInstance("SHA-256")
        urls.sorted().forEach{url->
            digest.update(url.toByteArray(Charsets.UTF_8))
            digest.update(0.toByte())
        }
        return digest.digest().joinToString(""){"%02x".format(it)}
    }

    private fun persistStatus(context:Context,status:Status){
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putInt("total",status.total)
            .putInt("missing",status.missing)
            .putInt("changed",status.changed)
            .putInt("downloaded",status.downloaded)
            .putInt("failed",status.failed)
            .putString("revision",status.revision)
            .putLong("updated_at",status.updatedAt)
            .apply()
    }
}
