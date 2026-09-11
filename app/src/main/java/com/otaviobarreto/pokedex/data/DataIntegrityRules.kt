package com.otaviobarreto.pokedex.data

/** Pure collection/progress rules shared by UI and stores and covered by unit tests. */
object DataIntegrityRules {
    fun capturedForScope(
        global: Set<Int>,
        contextual: Map<String, Set<Int>>,
        source: String?
    ): Set<Int> = if (source.isNullOrBlank()) global else contextual[source].orEmpty()

    fun shouldEnsureOwned(
        alreadyOwned: Boolean,
        hasBoxReference: Boolean,
        hasContextualReference: Boolean
    ): Boolean = alreadyOwned || hasBoxReference || hasContextualReference

    fun completedCount(validStepIds: Collection<String>, completed: Set<String>): Int =
        validStepIds.count { it in completed }
}
