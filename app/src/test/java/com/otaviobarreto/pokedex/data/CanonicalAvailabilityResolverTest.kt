package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class CanonicalAvailabilityResolverTest {

    private val paldea=GameContext.fromSource("Scarlet / Violet · Paldea")!!

    @Test fun outsideRegionalDexIsUnavailable() {
        val record=CanonicalAvailabilityResolver.resolve(
            pokemonId=25,
            context=paldea,
            encounters=emptyList(),
            dex=listOf(GameDexService.GameDexEntry(1,1,"Bulbasaur"))
        )
        assertFalse(record.inRegionalDex)
        assertEquals(CanonicalAcquisitionKind.UNAVAILABLE,record.acquisitionKind)
        assertEquals(VersionAvailabilityKind.UNAVAILABLE,record.version?.kind)
    }

    @Test fun encounterWinsAsWildAcquisition() {
        val record=CanonicalAvailabilityResolver.resolve(
            pokemonId=25,
            context=paldea,
            encounters=listOf(
                PokeApiService.EncounterLocation(
                    location="South Province Area Two",
                    versions=listOf("Scarlet","Violet"),
                    details=listOf(PokeApiService.EncounterDetail("Scarlet","Walk",10,12,30,emptyList()))
                )
            ),
            dex=listOf(GameDexService.GameDexEntry(25,74,"Pikachu"))
        )
        assertEquals(CanonicalAcquisitionKind.WILD,record.acquisitionKind)
        assertEquals(listOf("South Province (Area Two)"),record.locations)
    }

    @Test fun evolutionIsUsedWhenNoWildEncounterExists() {
        val dex=listOf(
            GameDexService.GameDexEntry(172,73,"Pichu"),
            GameDexService.GameDexEntry(25,74,"Pikachu")
        )
        val chain=listOf(
            PokeApiService.EvolutionStage(172,"Pichu",null),
            PokeApiService.EvolutionStage(25,"Pikachu","Amizade")
        )
        val record=CanonicalAvailabilityResolver.resolve(25,paldea,emptyList(),dex,chain)
        assertEquals(CanonicalAcquisitionKind.EVOLUTION,record.acquisitionKind)
        assertEquals("Amizade",record.requirement)
    }

    @Test fun starterCatalogProvidesConfirmedMethod() {
        val record=CanonicalAvailabilityResolver.resolve(
            pokemonId=906,
            context=paldea,
            encounters=emptyList(),
            dex=listOf(GameDexService.GameDexEntry(906,1,"Sprigatito"))
        )
        assertEquals(CanonicalAcquisitionKind.GIFT_STARTER,record.acquisitionKind)
        assertEquals(AvailabilityConfidence.CONFIRMED,record.confidence)
    }

    @Test fun unknownRegionalAcquisitionStaysBlank() {
        for(id in listOf(1,25,133,1025)){
            val record=CanonicalAvailabilityResolver.resolve(
                pokemonId=id,
                context=paldea,
                encounters=emptyList(),
                dex=listOf(GameDexService.GameDexEntry(id,id,"Pokemon $id"))
            )
            assertTrue(record.inRegionalDex)
            assertEquals(CanonicalAcquisitionKind.OTHER_METHOD,record.acquisitionKind)
            assertTrue(record.acquisitionLabel.isBlank())
            assertNull(record.requirement)
            assertEquals(AvailabilityConfidence.PARTIAL,record.confidence)
        }
    }
}
