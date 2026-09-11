package com.otaviobarreto.pokedex.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Single source of truth for collection and Box placement. */
object CollectionStore {
    private const val PREFS = "pokedex_collection"
    private const val KEY_CAPTURED = "captured_ids"
    private const val KEY_BOX_PREFIX = "box_"
    private const val KEY_BOX_ORDER = "box_order_v2"
    private const val KEY_CONTEXTUAL_CAPTURED = "contextual_captured_v1"
    private const val KEY_CONTEXT_MIGRATED = "contextual_captured_migrated_v1"
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
    var contextualCapturedIds by mutableStateOf<Map<String, Set<Int>>>(emptyMap()); private set
    var boxNames by mutableStateOf(defaultBoxes); private set
    var boxes by mutableStateOf<Map<String, Set<Int>>>(defaultBoxes.associateWith { emptySet() }); private set

    fun initialize(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
        val prefs = this.context!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        capturedIds = prefs.getStringSet(KEY_CAPTURED, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        contextualCapturedIds = decodeContextualCaptured(prefs.getString(KEY_CONTEXTUAL_CAPTURED, null))
        val storedOrder = prefs.getString(KEY_BOX_ORDER, null)?.let(::decodeBoxOrder).orEmpty()
        boxNames = if (storedOrder.isEmpty()) defaultBoxes else storedOrder
        boxes = boxNames.associateWith { box -> prefs.getStringSet(KEY_BOX_PREFIX + box, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet() }
        if (storedOrder.isEmpty()) persistBoxOrder()
        val boxed = boxes.values.flatten().toSet()
        if (!capturedIds.containsAll(boxed)) { capturedIds = capturedIds + boxed; persistCaptured() }
    }

    fun isCaptured(id: Int): Boolean = id in capturedIds
    fun capturedIn(source: String): Set<Int> = contextualCapturedIds[source].orEmpty()
    fun isCapturedIn(source: String, id: Int): Boolean = id in contextualCapturedIds[source].orEmpty()
    fun toggleCapturedIn(source: String, id: Int) = setCapturedIn(source, id, !isCapturedIn(source, id))
    fun setCapturedIn(source: String, id: Int, captured: Boolean) {
        if (source.isBlank()) { setCaptured(id, captured); return }
        val current = contextualCapturedIds[source].orEmpty()
        val updated = if (captured) current + id else current - id
        if (updated == current) return
        contextualCapturedIds = if (updated.isEmpty()) contextualCapturedIds - source else contextualCapturedIds + (source to updated)
        persistContextualCaptured()
        if (captured) markCaptured(id)
    }

    fun migrateLegacyCapturedToSource(source: String?) {
        if (source.isNullOrBlank() || capturedIds.isEmpty()) return
        val prefs = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
        if (prefs.getBoolean(KEY_CONTEXT_MIGRATED, false)) return
        if (contextualCapturedIds.isEmpty()) {
            contextualCapturedIds = mapOf(source to capturedIds)
            persistContextualCaptured()
        }
        prefs.edit().putBoolean(KEY_CONTEXT_MIGRATED, true).apply()
    }

    fun toggleCaptured(id: Int) = setCaptured(id, id !in capturedIds)
    fun setCaptured(id: Int, captured: Boolean) {
        if (captured) {
            if (id !in capturedIds) { capturedIds = capturedIds + id; persistCaptured() }
            return
        }
        if (id in capturedIds) {
            capturedIds = capturedIds - id
            persistCaptured()
        }
        val contextualAffected = contextualCapturedIds.filterValues { id in it }.keys
        if (contextualAffected.isNotEmpty()) {
            contextualCapturedIds = contextualCapturedIds.mapValues { (_, ids) -> ids - id }.filterValues { it.isNotEmpty() }
            persistContextualCaptured()
        }
        val affected = boxes.filterValues { id in it }.keys
        if (affected.isNotEmpty()) {
            boxes = boxes.mapValues { (_, ids) -> ids - id }
            affected.forEach(::persistBox)
        }
    }
    fun markCaptured(id: Int) = setCaptured(id, true)

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

    /** Repairs old versions where entries were captured without a concrete numbered Box. */
    fun migrateCapturedToBox(box: String, ids: Collection<Int>) {
        val resolved = ensureBox(box) ?: return
        val current = boxes[resolved].orEmpty().toMutableSet()
        ids.filter { it in capturedIds && boxesForPokemon(it).isEmpty() }
            .take((MAX_BOX_SIZE - current.size).coerceAtLeast(0))
            .forEach(current::add)
        boxes = boxes + (resolved to current.toSet())
        persistBox(resolved)
    }

    /** Splits the old game-level bucket into the numbered Box matching the regional page. */
    fun migrateLegacyGameBox(legacyBox: String, numberedBox: String, pageIds: Collection<Int>) {
        if (legacyBox == numberedBox) return
        val legacy = boxes[legacyBox].orEmpty()
        val moving = legacy.intersect(pageIds.toSet())
        if (moving.isEmpty()) return
        val target = ensureBox(numberedBox) ?: return
        val targetSet = boxes[target].orEmpty().toMutableSet()
        moving.take((MAX_BOX_SIZE - targetSet.size).coerceAtLeast(0)).forEach(targetSet::add)
        val moved = targetSet.intersect(moving)
        boxes = boxes + (target to targetSet.toSet()) + (legacyBox to (legacy - moved))
        persistBox(target)
        persistBox(legacyBox)
    }

    fun removeFromBox(box: String, id: Int) {
        val current = boxes[box].orEmpty(); boxes = boxes + (box to (current - id)); persistBox(box); syncCapturedFromBoxes(id)
    }

    fun movePokemon(fromBox: String, toBox: String, id: Int): Boolean {
        if (fromBox == toBox) return true; if (id !in boxes[fromBox].orEmpty()) return false; if (!addToBox(toBox, id)) return false; removeFromBox(fromBox, id); return true
    }

    fun boxesForPokemon(id: Int): List<String> = boxNames.filter { id in boxes[it].orEmpty() }
    fun duplicateIds(): Set<Int> = boxes.values.flatten().groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    fun unboxedCapturedIds(): Set<Int> = capturedIds - boxes.values.flatten().toSet()

    fun moveMany(fromBox: String, toBox: String, ids: Collection<Int>): Int {
        if (fromBox == toBox) return 0
        val target = ensureBox(toBox) ?: return 0
        val sourceIds = boxes[fromBox].orEmpty()
        val candidates = ids.filter { it in sourceIds && it !in boxes[target].orEmpty() }
        val room = (MAX_BOX_SIZE - boxes[target].orEmpty().size).coerceAtLeast(0)
        val moving = candidates.take(room)
        if (moving.isEmpty()) return 0
        boxes = boxes + (target to (boxes[target].orEmpty() + moving)) + (fromBox to (sourceIds - moving.toSet()))
        moving.forEach(::markCaptured)
        persistBox(target); persistBox(fromBox)
        return moving.size
    }

    fun sortBox(box: String): Boolean {
        if (box !in boxes) return false
        boxes = boxes + (box to boxes[box].orEmpty().sorted().toCollection(linkedSetOf()))
        persistBox(box)
        return true
    }

    fun exportSnapshot(): JSONObject {
        val boxArray = JSONArray()
        boxNames.forEach { name ->
            boxArray.put(JSONObject().put("name", name).put("ids", JSONArray(boxes[name].orEmpty().sorted())))
        }
        val contextual = JSONObject()
        contextualCapturedIds.toSortedMap().forEach { (source, ids) ->
            contextual.put(source, JSONArray(ids.sorted()))
        }
        return JSONObject()
            .put("captured", JSONArray(capturedIds.sorted()))
            .put("contextualCaptured", contextual)
            .put("boxes", boxArray)
    }

    fun importSnapshot(snapshot: JSONObject): Boolean = runCatching {
        val capturedArray = snapshot.optJSONArray("captured") ?: JSONArray()
        val newCaptured = buildSet {
            for (i in 0 until capturedArray.length()) {
                capturedArray.optInt(i).takeIf { it in 1..PokeApiService.MAX_NATIONAL_DEX_ID }?.let(::add)
            }
        }
        val contextualObject = snapshot.optJSONObject("contextualCaptured")
        val restoredContextual = linkedMapOf<String, Set<Int>>()
        if (contextualObject != null) {
            val keys = contextualObject.keys()
            while (keys.hasNext()) {
                val source = keys.next()
                val idsArray = contextualObject.optJSONArray(source) ?: continue
                val ids = buildSet {
                    for (j in 0 until idsArray.length()) {
                        idsArray.optInt(j).takeIf { it in 1..PokeApiService.MAX_NATIONAL_DEX_ID }?.let(::add)
                    }
                }
                if (source.isNotBlank() && ids.isNotEmpty()) restoredContextual[source] = ids
            }
        }
        val boxesArray = snapshot.optJSONArray("boxes") ?: JSONArray()
        val names = mutableListOf<String>()
        val restored = linkedMapOf<String, Set<Int>>()
        for (i in 0 until boxesArray.length()) {
            val obj = boxesArray.optJSONObject(i) ?: continue
            val name = sanitizeBoxName(obj.optString("name"))
            if (name.isBlank() || name in names) continue
            val idsArray = obj.optJSONArray("ids") ?: JSONArray()
            val ids = buildSet {
                for (j in 0 until idsArray.length()) {
                    idsArray.optInt(j).takeIf { it in 1..PokeApiService.MAX_NATIONAL_DEX_ID }?.let(::add)
                }
            }.take(MAX_BOX_SIZE).toSet()
            names += name
            restored[name] = ids
        }
        boxNames = if (names.isEmpty()) defaultBoxes else names
        boxes = if (restored.isEmpty()) boxNames.associateWith { emptySet() } else boxNames.associateWith { restored[it].orEmpty() }
        contextualCapturedIds = restoredContextual
        capturedIds = newCaptured + boxes.values.flatten() + contextualCapturedIds.values.flatten()
        persistBoxOrder()
        boxNames.forEach(::persistBox)
        persistCaptured()
        persistContextualCaptured()
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putBoolean(KEY_CONTEXT_MIGRATED, contextualCapturedIds.isNotEmpty())?.apply()
        true
    }.getOrDefault(false)

    private fun syncCapturedFromBoxes(id: Int) {
        val boxed = boxes.values.any { id in it }
        val contextual = contextualCapturedIds.values.any { id in it }
        if (!boxed && !contextual && id in capturedIds) {
            capturedIds = capturedIds - id
            persistCaptured()
        } else if ((boxed || contextual) && id !in capturedIds) {
            capturedIds = capturedIds + id
            persistCaptured()
        }
    }
    private fun sanitizeBoxName(name: String) = name.trim().replace(Regex("\\s+"), " ").take(48)
    private fun persistCaptured() { context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putStringSet(KEY_CAPTURED, capturedIds.map(Int::toString).toSet())?.apply() }
    private fun persistContextualCaptured() {
        val root = JSONObject()
        contextualCapturedIds.toSortedMap().forEach { (source, ids) -> root.put(source, JSONArray(ids.sorted())) }
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString(KEY_CONTEXTUAL_CAPTURED, root.toString())?.apply()
    }
    private fun decodeContextualCaptured(raw: String?): Map<String, Set<Int>> = runCatching {
        if (raw.isNullOrBlank()) return@runCatching emptyMap()
        val root = JSONObject(raw)
        buildMap {
            val keys = root.keys()
            while (keys.hasNext()) {
                val source = keys.next()
                val array = root.optJSONArray(source) ?: continue
                val ids = buildSet {
                    for (i in 0 until array.length()) {
                        array.optInt(i).takeIf { it in 1..PokeApiService.MAX_NATIONAL_DEX_ID }?.let(::add)
                    }
                }
                if (ids.isNotEmpty()) put(source, ids)
            }
        }
    }.getOrDefault(emptyMap())
    private fun persistBox(box: String) { context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putStringSet(KEY_BOX_PREFIX + box, boxes[box].orEmpty().map(Int::toString).toSet())?.apply() }
    private fun persistBoxOrder() { context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString(KEY_BOX_ORDER, JSONArray(boxNames).toString())?.apply() }
    private fun decodeBoxOrder(raw: String): List<String> = runCatching { val a = JSONArray(raw); buildList { for (i in 0 until a.length()) a.optString(i).takeIf { it.isNotBlank() }?.let(::add) } }.getOrDefault(emptyList())
}
