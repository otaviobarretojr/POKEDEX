package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class RegionalDexLayeringFormsTest {

    private fun entry(id:Int,number:Int=id)=GameDexService.GameDexEntry(
        nationalId=id,
        gameNumber=number,
        name="Pokemon $id"
    )

    private fun route(
        source:Int,
        target:Int,
        targetFormKey:String?
    )=EvolutionRoute(
        sourcePokemonId=source,
        targetPokemonId=target,
        methods=setOf(PokeApiService.EvolutionMethod.LEVEL),
        summary="Nível 20",
        detail="Subir ao nível 20",
        availability=EvolutionAvailability.AVAILABLE,
        contextLabel="Game",
        regionLabel="Region",
        targetFormKey=targetFormKey
    )

    @Test
    fun newFormCanAppearInDlcEvenWhenSpeciesWasAlreadyShownInBase() {
        val game=AppGame(
            "Game",
            listOf(
                AppRegion("Base","Game · Base","Base"),
                AppRegion("DLC","Game · DLC","DLC")
            )
        )
        val entries=mapOf(
            "Game · Base" to listOf(entry(1),entry(2)),
            "Game · DLC" to listOf(entry(2),entry(3))
        )
        val routes=mapOf(
            "Game · Base" to listOf(route(1,2,null)),
            "Game · DLC" to listOf(route(1,2,"pokemon-2-regional"),route(2,3,null))
        )

        val layer=RegionalDexLayering.layeredResult(game,"Game · DLC",entries,routes)

        assertEquals(setOf(3),layer.exclusiveSpeciesIds)
        assertTrue(2 to "pokemon-2-regional" in layer.novelFormIdentities)
        assertEquals(setOf(2,3),layer.exclusiveSpeciesIds + layer.novelFormTargetIds)
    }

    @Test
    fun formAlreadySeenEarlierIsNotNovelAgain() {
        val game=AppGame(
            "Game",
            listOf(
                AppRegion("Base","Game · Base","Base"),
                AppRegion("DLC","Game · DLC","DLC")
            )
        )
        val entries=mapOf(
            "Game · Base" to listOf(entry(1),entry(2)),
            "Game · DLC" to listOf(entry(2))
        )
        val routes=mapOf(
            "Game · Base" to listOf(route(1,2,"pokemon-2-regional")),
            "Game · DLC" to listOf(route(1,2,"pokemon-2-regional"))
        )

        val layer=RegionalDexLayering.layeredResult(game,"Game · DLC",entries,routes)
        assertTrue(layer.novelFormIdentities.isEmpty())
    }

    @Test
    fun exactFormOwnershipAcrossGameSourcesIsDetected() {
        val variants=listOf(
            OwnedPokemonVariant(
                source="Game · Base",
                speciesId=2,
                formPokemonId=2002,
                formName="Regional",
                shiny=false,
                formKey="pokemon-2-regional",
                isDefault=false
            )
        )

        assertTrue(
            isOwnedFormAcrossSources(
                variants=variants,
                sources=setOf("Game · Base","Game · DLC"),
                speciesId=2,
                formKey="pokemon-2-regional"
            )
        )
        assertFalse(
            isOwnedFormAcrossSources(
                variants=variants,
                sources=setOf("Game · Base","Game · DLC"),
                speciesId=2,
                formKey="pokemon-2-other"
            )
        )
    }
}
