package com.otaviobarreto.pokedex.data

import android.content.Context
import java.io.File
import java.security.MessageDigest
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

    fun initialize(context: Context) {
        if (directory != null) return
        directory = File(context.filesDir, "api-cache-v1").apply { mkdirs() }
    }

    fun getOrFetch(
        url: String,
        maxAgeMs: Long = DEFAULT_MAX_AGE_MS,
        fetch: () -> String
    ): String {
        val file = fileFor(url)
        val now = System.currentTimeMillis()
        if (file.exists() && now - file.lastModified() <= maxAgeMs) {
            read(file)?.let { return it }
        }

        return runCatching { fetch() }
            .onSuccess { write(file, it) }
            .getOrElse { error ->
                read(file) ?: throw error
            }
    }

    fun has(url: String): Boolean = fileFor(url).exists()

    fun clear() {
        directory?.listFiles()?.forEach { it.delete() }
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

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
