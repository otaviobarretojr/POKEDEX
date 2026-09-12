package com.otaviobarreto.pokedex.data

data class CollectionIntegrityReport(
    val missingGlobalOwnership: Set<Int>,
    val missingContextualOwnership: List<Pair<String, Int>>
) {
    val clean: Boolean
        get() = missingGlobalOwnership.isEmpty() && missingContextualOwnership.isEmpty()
}

object CollectionIntegrityService {
    fun audit(): CollectionIntegrityReport {
        val boxIds = CollectionStore.boxes.values.flatten()
        val contextualIds = CollectionStore.contextualCapturedIds.values.flatten()
        val variantPairs = VariantCollectionStore.ownedVariants
            .map { it.source to it.speciesId }
            .distinct()

        return CollectionIntegrityReport(
            missingGlobalOwnership = DataIntegrityRules.missingGlobalOwnership(
                global = CollectionStore.capturedIds,
                boxIds = boxIds,
                contextualIds = contextualIds,
                variantIds = variantPairs.map { it.second }
            ),
            missingContextualOwnership = DataIntegrityRules.missingContextualVariantOwnership(
                contextual = CollectionStore.contextualCapturedIds,
                variantPairs = variantPairs
            )
        )
    }

    fun repair(): CollectionIntegrityReport {
        val before = audit()
        before.missingGlobalOwnership.forEach(CollectionStore::markCaptured)
        before.missingContextualOwnership.forEach { (source, id) ->
            CollectionStore.setCapturedIn(source, id, true)
        }
        return audit()
    }
}
