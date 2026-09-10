package com.otaviobarreto.pokedex.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Single source of truth for personal collection state.
 * Persisted with SharedPreferences so Living Dex and Boxes always stay in sync.
 */
object CollectionStore {
    private const val PREFS = "pokedex_collection"
    private const val KEY_CAPTURED = "captured_ids"
    private const val KEY_BOX_PREFIX = "box_"

    val defaultBoxes = listOf(
        "Pokémon HOME",
        "Scarlet / Violet",
        "Let's Go Pikachu / Eevee",
        "Sword / Shield",
        "Legends Arceus"
    )

    private var context: Context? = null

    var capturedIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    var boxes by mutableStateOf<Map<String, Set<Int>>>(defaultBoxes.associateWith { emptySet() })
        private set

    fun initialize(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
        val prefs = this.context!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        capturedIds = prefs.getStringSet(KEY_CAPTURED, emptySet())
            .orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

        boxes = defaultBoxes.associateWith { box ->
            prefs.getStringSet(KEY_BOX_PREFIX + box, emptySet())
                .orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        }
    }

    fun isCaptured(id: Int): Boolean = id in capturedIds

    fun toggleCaptured(id: Int) {
        capturedIds = if (id in capturedIds) capturedIds - id else capturedIds + id
        persistCaptured()
    }

    fun markCaptured(id: Int) {
        if (id !in capturedIds) {
            capturedIds = capturedIds + id
            persistCaptured()
        }
    }

    fun addToBox(box: String, id: Int) {
        val current = boxes[box].orEmpty()
        boxes = boxes + (box to (current + id))
        markCaptured(id)
        persistBox(box)
    }

    fun removeFromBox(box: String, id: Int) {
        val current = boxes[box].orEmpty()
        boxes = boxes + (box to (current - id))
        persistBox(box)
    }

    fun boxesForPokemon(id: Int): List<String> = boxes.filterValues { id in it }.keys.toList()

    private fun persistCaptured() {
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putStringSet(KEY_CAPTURED, capturedIds.map(Int::toString).toSet())
            ?.apply()
    }

    private fun persistBox(box: String) {
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putStringSet(KEY_BOX_PREFIX + box, boxes[box].orEmpty().map(Int::toString).toSet())
            ?.apply()
    }
}
