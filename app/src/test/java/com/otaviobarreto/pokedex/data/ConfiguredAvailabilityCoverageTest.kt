package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class ConfiguredAvailabilityCoverageTest {

    @Test
    fun everyConfiguredRegionHasTotalResolverCoverageForNationalDexRange() {
        AppGameCatalog.games.forEach { game ->
            game.regions.forEach { region ->
                val context = requireNotNull(GameContext.fromSource(region.source)) {
                    "Contexto ausente: ${game.label} / ${region.label}"
                }

                for (id in 1..PokeApiService.MAX_NATIONAL_DEX_ID) {
                    val dex = listOf(
                        GameDexService.GameDexEntry(
                            nationalId = id,
                            gameNumber = id,
                            name = "Pokemon $id"
                        )
                    )
                    val record = CanonicalAvailabilityResolver.resolve(
                        pokemonId = id,
                        context = context,
                        encounters = emptyList(),
                        dex = dex,
                        evolutionChain = emptyList()
                    )

                    assertTrue("${region.source} #$id deve permanecer membro da dex sintética", record.inRegionalDex)
                    assertNotEquals(
                        "${region.source} #$id não pode cair em indisponível quando é membro regional",
                        CanonicalAcquisitionKind.UNAVAILABLE,
                        record.acquisitionKind
                    )
                    if(record.confidence==AvailabilityConfidence.CONFIRMED){
                        assertTrue("${region.source} #$id confirmado precisa de rótulo explícito", record.acquisitionLabel.isNotBlank())
                    }else{
                        assertTrue("${region.source} #$id parcial deve permanecer visualmente silencioso", record.acquisitionLabel.isBlank())
                    }
                    assertTrue("${region.source} #$id precisa de proveniência", record.provenance.isNotEmpty())
                }
            }
        }
    }

    @Test
    fun everyConfiguredRegionRejectsSpeciesOutsideItsRegionalDex() {
        AppGameCatalog.games.forEach { game ->
            game.regions.forEach { region ->
                val context = requireNotNull(GameContext.fromSource(region.source))
                val record = CanonicalAvailabilityResolver.resolve(
                    pokemonId = 25,
                    context = context,
                    encounters = emptyList(),
                    dex = listOf(GameDexService.GameDexEntry(1, 1, "Bulbasaur")),
                    evolutionChain = emptyList()
                )
                assertFalse(record.inRegionalDex)
                assertEquals(CanonicalAcquisitionKind.UNAVAILABLE, record.acquisitionKind)
            }
        }
    }

    @Test
    fun everyConfiguredRegionHasUniqueStableSourceAndSlug() {
        val sources = mutableSetOf<String>()
        val slugs = mutableListOf<String>()

        AppGameCatalog.games.forEach { game ->
            game.regions.forEach { region ->
                assertTrue("Fonte duplicada: ${region.source}", sources.add(region.source))
                val context = requireNotNull(GameContext.fromSource(region.source))
                assertTrue(context.pokedexSlug.isNotBlank())
                slugs += context.pokedexSlug
            }
        }

        assertTrue(slugs.contains("paldea"))
        assertTrue(slugs.contains("galar"))
        assertTrue(slugs.contains("lumiose-city"))
        assertTrue(slugs.contains("hyperspace"))
    }

    @Test
    fun supportedGameStartersUseConfirmedCuratedAcquisition() {
        val cases = listOf(
            "Pokémon Legends: Z-A · Lumiose" to 152,
            "Pokémon Legends: Z-A · Hyperspace" to 498,
            "Scarlet / Violet · Paldea" to 906,
            "Sword / Shield · Galar" to 810,
            "Let's Go Pikachu / Eevee · Kanto" to 25,
            "Legends Arceus · Hisui" to 722,
            "Brilliant Diamond / Shining Pearl · Sinnoh" to 387,
            "FireRed / LeafGreen · Kanto" to 1
        )

        cases.forEach { (source, id) ->
            val context = requireNotNull(GameContext.fromSource(source))
            val record = CanonicalAvailabilityResolver.resolve(
                pokemonId = id,
                context = context,
                encounters = emptyList(),
                dex = listOf(GameDexService.GameDexEntry(id, 1, "Starter")),
                evolutionChain = emptyList()
            )
            assertEquals(source, CanonicalAcquisitionKind.GIFT_STARTER, record.acquisitionKind)
            assertEquals(source, AvailabilityConfidence.CONFIRMED, record.confidence)
        }
    }

    @Test
    fun unresolvedMethodsAreHonestInsteadOfBeingMisclassifiedAsEvents() {
        val context = requireNotNull(GameContext.fromSource("Scarlet / Violet · Paldea"))
        val record = CanonicalAvailabilityResolver.resolve(
            pokemonId = 25,
            context = context,
            encounters = emptyList(),
            dex = listOf(GameDexService.GameDexEntry(25, 74, "Pikachu")),
            evolutionChain = emptyList()
        )
        assertEquals(CanonicalAcquisitionKind.OTHER_METHOD, record.acquisitionKind)
        assertEquals(AvailabilityConfidence.PARTIAL, record.confidence)
        assertTrue(record.acquisitionLabel.isBlank())
        assertNull(record.requirement)
    }
}
