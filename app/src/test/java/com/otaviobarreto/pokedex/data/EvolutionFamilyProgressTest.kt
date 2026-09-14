package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class EvolutionFamilyProgressTest {

    private fun entry(id:Int,number:Int)=GameDexService.GameDexEntry(
        nationalId=id,
        gameNumber=number,
        name="Pokemon $id"
    )

    private fun route(source:Int,target:Int)=EvolutionRoute(
        sourcePokemonId=source,
        targetPokemonId=target,
        methods=setOf(PokeApiService.EvolutionMethod.LEVEL),
        summary="Nível",
        detail="Subir de nível",
        availability=EvolutionAvailability.AVAILABLE,
        contextLabel="Game",
        regionLabel="Region"
    )

    @Test
    fun missingFirstAndSecondStageRemainVisibleWhenFinalStageIsOwned() {
        val dex=listOf(
            entry(906,1), // Sprigatito
            entry(907,2), // Floragato
            entry(908,3)  // Meowscarada
        )
        val routes=listOf(
            route(906,907),
            route(907,908)
        )

        val missing=EvolutionFamilyProgress.missingEntries(
            dex=dex,
            routes=routes,
            owned=setOf(908)
        )

        assertEquals(listOf(906,907),missing.map{it.nationalId})
    }

    @Test
    fun missingStagesStayInEvolutionOrderEvenIfRegionalDexOrderDiffers() {
        val dex=listOf(
            entry(100,30),
            entry(101,10),
            entry(102,20)
        )
        val routes=listOf(
            route(100,101),
            route(101,102)
        )

        assertEquals(
            listOf(100,101,102),
            EvolutionFamilyProgress.orderEntries(dex,routes).map{it.nationalId}
        )
    }

    @Test
    fun ownedMiddleStageDoesNotHideMissingEarlierOrLaterStages() {
        val dex=listOf(
            entry(10,1),
            entry(11,2),
            entry(12,3)
        )
        val routes=listOf(
            route(10,11),
            route(11,12)
        )

        val missing=EvolutionFamilyProgress.missingEntries(
            dex=dex,
            routes=routes,
            owned=setOf(11)
        )

        assertEquals(listOf(10,12),missing.map{it.nationalId})
    }

    @Test
    fun standalonePokemonStillAppearsAsMissing() {
        val dex=listOf(entry(777,1))
        val missing=EvolutionFamilyProgress.missingEntries(
            dex=dex,
            routes=emptyList(),
            owned=emptySet()
        )
        assertEquals(listOf(777),missing.map{it.nationalId})
    }

    @Test
    fun fullyOwnedFamilyDisappearsFromMissingList() {
        val dex=listOf(entry(1,1),entry(2,2),entry(3,3))
        val routes=listOf(route(1,2),route(2,3))
        val missing=EvolutionFamilyProgress.missingEntries(
            dex=dex,
            routes=routes,
            owned=setOf(1,2,3)
        )
        assertTrue(missing.isEmpty())
    }
}
