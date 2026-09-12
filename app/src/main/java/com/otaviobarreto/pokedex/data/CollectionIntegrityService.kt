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
        val boxIds = CollectionStore.boxes.values.flatten().toSet()
        val contextualIds = CollectionStore.contextualCapturedIds.values.flatten().toSet()
        val variantPairs = VariantCollectionStore.ownedVariants
            .map { it.source to it.speciesId }
            .distinct()

        val missingGlobal = (boxIds + contextualIds + variantPairs.map { it.second })
            .filterNot(CollectionStore::isCaptured)
            .toSet()

        val missingContextual = variantPairs.filterNot { (source, id) ->
            CollectionStore.isCapturedIn(source, id)
        }

        return CollectionIntegrityReport(missingGlobal, missingContextual)
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
