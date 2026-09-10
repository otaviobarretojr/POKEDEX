package com.otaviobarreto.pokedex.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

/** Single source of truth for collection and Box placement. */
object CollectionStore {
    private const val PREFS = "pokedex_collection"
    private const val KEY_CAPTURED = "captured_ids"
    private const val KEY_BOX_PREFIX = "box_"
    private const val KEY_BOX_ORDER = "box_order_v2"
    private const val MAX_BOX_SIZE = 30

    val defaultBoxes = listOf(
        "Pokémon HOME",
        "Scarlet / Violet",
        "Let's Go Pikachu / Eevee",
        "Sword / Shield",
        "Legends Arceus"
    )

    private var context: Context? = null
    var capturedIds by mutableStateOf<Set<Int>>(emptySet()); private set
    var boxNames by mutableStateOf(defaultBoxes); private set
    var boxes by mutableStateOf<Map<String, Set<Int>>>(defaultBoxes.associateWith { emptySet() }); private set

    fun initialize(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
        val prefs = this.context!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        capturedIds = prefs.getStringSet(KEY_CAPTURED, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        val storedOrder = prefs.getString(KEY_BOX_ORDER, null)?.let(::decodeBoxOrder).orEmpty()
        boxNames = if (storedOrder.isEmpty()) defaultBoxes else storedOrder
        boxes = boxNames.associateWith { box -> prefs.getStringSet(KEY_BOX_PREFIX + box, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet() }
        if (storedOrder.isEmpty()) persistBoxOrder()
        val boxed = boxes.values.flatten().toSet()
        if (!capturedIds.containsAll(boxed)) { capturedIds = capturedIds + boxed; persistCaptured() }
    }

    fun isCaptured(id: Int): Boolean = id in capturedIds
    fun toggleCaptured(id: Int) { capturedIds = if (id in capturedIds) capturedIds - id else capturedIds + id; persistCaptured() }
    fun markCaptured(id: Int) { if (id !in capturedIds) { capturedIds = capturedIds + id; persistCaptured() } }

    fun ensureBox(name: String): String? {
        val clean = sanitizeBoxName(name)
        if (clean.isBlank()) return null
        val existing = boxNames.firstOrNull { it.equals(clean, true) }
        if (existing != null) return existing
        if (!createBox(clean)) return null
        return clean
    }

    fun createBox(name: String): Boolean {
        val cleanName = sanitizeBoxName(name); if (cleanName.isBlank() || boxNames.any { it.equals(cleanName, true) }) return false
        boxNames = boxNames + cleanName; boxes = boxes + (cleanName to emptySet()); persistBoxOrder(); persistBox(cleanName); return true
    }

    fun renameBox(oldName: String, newName: String): Boolean {
        val cleanName = sanitizeBoxName(newName)
        if (oldName !in boxNames || cleanName.isBlank() || boxNames.any { it != oldName && it.equals(cleanName, true) }) return false
        if (oldName == cleanName) return true
        val contents = boxes[oldName].orEmpty(); boxNames = boxNames.map { if (it == oldName) cleanName else it }; boxes = boxes - oldName + (cleanName to contents)
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.remove(KEY_BOX_PREFIX + oldName)?.apply(); persistBox(cleanName); persistBoxOrder(); return true
    }

    fun deleteBox(name: String): Boolean {
        if (name !in boxNames || boxNames.size <= 1) return false
        val removed = boxes[name].orEmpty(); boxNames = boxNames - name; boxes = boxes - name
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.remove(KEY_BOX_PREFIX + name)?.apply(); persistBoxOrder()
        removed.forEach(::syncCapturedFromBoxes); return true
    }

    fun moveBox(name: String, direction: Int): Boolean {
        val from = boxNames.indexOf(name); if (from == -1) return false; val to = (from + direction).coerceIn(0, boxNames.lastIndex); if (to == from) return false
        val mutable = boxNames.toMutableList(); val item = mutable.removeAt(from); mutable.add(to, item); boxNames = mutable.toList(); persistBoxOrder(); return true
    }

    fun addToBox(box: String, id: Int): Boolean {
        val resolved = ensureBox(box) ?: return false
        val current = boxes[resolved].orEmpty(); if (id in current) return true; if (current.size >= MAX_BOX_SIZE) return false
        boxes = boxes + (resolved to (current + id)); markCaptured(id); persistBox(resolved); return true
    }

    /** Used only to repair old versions where a Pokémon was marked captured but had no Box placement. */
    fun migrateCapturedToBox(box: String, ids: Collection<Int>) {
        val resolved = ensureBox(box) ?: return
        val current = boxes[resolved].orEmpty().toMutableSet()
        ids.filter { it in capturedIds && boxesForPokemon(it).isEmpty() }
            .take((MAX_BOX_SIZE - current.size).coerceAtLeast(0))
            .forEach(current::add)
        boxes = boxes + (resolved to current.toSet())
        persistBox(resolved)
    }

    fun removeFromBox(box: String, id: Int) {
        val current = boxes[box].orEmpty(); boxes = boxes + (box to (current - id)); persistBox(box); syncCapturedFromBoxes(id)
    }

    fun movePokemon(fromBox: String, toBox: String, id: Int): Boolean {
        if (fromBox == toBox) return true; if (id !in boxes[fromBox].orEmpty()) return false; if (!addToBox(toBox, id)) return false; removeFromBox(fromBox, id); return true
    }

    fun boxesForPokemon(id: Int): List<String> = boxNames.filter { id in boxes[it].orEmpty() }

    private fun syncCapturedFromBoxes(id: Int) {
        if (boxes.values.none { id in it } && id in capturedIds) { capturedIds = capturedIds - id; persistCaptured() }
        else if (boxes.values.any { id in it } && id !in capturedIds) { capturedIds = capturedIds + id; persistCaptured() }
    }
    private fun sanitizeBoxName(name: String) = name.trim().replace(Regex("\\s+"), " ").take(48)
    private fun persistCaptured() { context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putStringSet(KEY_CAPTURED, capturedIds.map(Int::toString).toSet())?.apply() }
    private fun persistBox(box: String) { context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putStringSet(KEY_BOX_PREFIX + box, boxes[box].orEmpty().map(Int::toString).toSet())?.apply() }
    private fun persistBoxOrder() { context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString(KEY_BOX_ORDER, JSONArray(boxNames).toString())?.apply() }
    private fun decodeBoxOrder(raw: String): List<String> = runCatching { val a = JSONArray(raw); buildList { for (i in 0 until a.length()) a.optString(i).takeIf { it.isNotBlank() }?.let(::add) } }.getOrDefault(emptyList())
}
