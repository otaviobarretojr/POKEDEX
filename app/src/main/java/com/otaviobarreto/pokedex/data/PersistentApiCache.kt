package com.otaviobarreto.pokedex.data

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Persistent raw-response cache shared by all remote data services.
 * Fresh entries are used without network access; stale entries are used as
 * offline fallback when refresh fails.
 */
object PersistentApiCache {
    private const val DEFAULT_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L
    private var directory: File? = null
    private val memory = ConcurrentHashMap<String, String>()
    private var pinsFile: File? = null
    @Volatile private var pinnedUrls: Set<String> = emptySet()

    fun initialize(context: Context) {
        if (directory != null) return
        directory = File(context.filesDir, "api-cache-v1").apply { mkdirs() }
        pinsFile = File(context.filesDir, "api-cache-pins.txt")
        pinnedUrls = pinsFile?.takeIf { it.exists() }?.readLines()?.filter { it.isNotBlank() }?.toSet().orEmpty()
    }

    fun getOrFetch(
        url: String,
        maxAgeMs: Long = DEFAULT_MAX_AGE_MS,
        fetch: () -> String
    ): String {
        memory[url]?.let { return it }
        val file = fileFor(url)
        val now = System.currentTimeMillis()
        if (file.exists() && (url in pinnedUrls || now - file.lastModified() <= maxAgeMs)) {
            read(file)?.let { value -> memory[url] = value; return value }
        }

        return runCatching { fetch() }
            .onSuccess { memory[url] = it; write(file, it) }
            .getOrElse { error ->
                read(file)?.also { memory[url] = it } ?: throw error
            }
    }

    fun has(url: String): Boolean = fileFor(url).exists()

    fun peek(url: String): String? {
        memory[url]?.let { return it }
        return read(fileFor(url))?.also { memory[url] = it }
    }

    @Synchronized fun pin(url: String) {
        if (!has(url)) return
        pinnedUrls = pinnedUrls + url
        persistPins()
    }

    @Synchronized fun pinAll(urls: Collection<String>) {
        val existing = urls.filter(::has)
        if (existing.isEmpty()) return
        pinnedUrls = pinnedUrls + existing
        persistPins()
    }

    @Synchronized fun unpinAll(urls: Collection<String>, deleteFiles: Boolean = false) {
        pinnedUrls = pinnedUrls - urls.toSet()
        urls.forEach { memory.remove(it); if (deleteFiles) fileFor(it).delete() }
        persistPins()
    }

    fun isPinned(url: String): Boolean = url in pinnedUrls

    fun clear() {
        memory.clear()
        directory?.listFiles()?.forEach { it.delete() }
        pinnedUrls = emptySet()
        persistPins()
    }

    fun sizeBytes(): Long = directory?.listFiles()?.sumOf { it.length() } ?: 0L

    private fun fileFor(url: String): File {
        val dir = directory ?: error("PersistentApiCache not initialized")
        return File(dir, sha256(url) + ".json.gz")
    }

    private fun read(file: File): String? = runCatching {
        GZIPInputStream(file.inputStream().buffered()).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()

    private fun write(file: File, text: String) {
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            GZIPOutputStream(tmp.outputStream().buffered()).bufferedWriter(Charsets.UTF_8).use { it.write(text) }
            if (file.exists()) file.delete()
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
            file.setLastModified(System.currentTimeMillis())
        }
    }

    private fun persistPins() {
        runCatching { pinsFile?.writeText(pinnedUrls.sorted().joinToString("\n")) }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
