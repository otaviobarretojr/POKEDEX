package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class JourneyEvolutionProgressAuditTest {

    private fun step(game:String,id:String)=
        JourneyCatalog.steps(game).first{it.id==id}

    private fun normalizedTeam(game:String,starterId:Int,phase:CampaignPhase,stepId:String):List<Int>{
        val focus=step(game,stepId)
        return TeamCampaignCatalog.preset(game,starterId,phase)!!.slots
            .map{JourneyTeamProgressCatalog.progressMemberFor(it.pokemonId,focus)}
    }

    @Test
    fun scarletVioletStarterTracksRealLevels(){
        assertEquals(909,JourneyTeamProgressCatalog.starterMemberForProgress(909,step("Scarlet / Violet","sv-01")))
        assertEquals(910,JourneyTeamProgressCatalog.starterMemberForProgress(909,step("Scarlet / Violet","sv-06")))
        assertEquals(911,JourneyTeamProgressCatalog.starterMemberForProgress(909,step("Scarlet / Violet","sv-15")))
    }

    @Test
    fun swordShieldStarterTracksRealLevels(){
        assertEquals(814,JourneyTeamProgressCatalog.starterMemberForProgress(813,step("Sword / Shield","swsh-g1")))
        assertEquals(814,JourneyTeamProgressCatalog.starterMemberForProgress(813,step("Sword / Shield","swsh-g3")))
        assertEquals(815,JourneyTeamProgressCatalog.starterMemberForProgress(813,step("Sword / Shield","swsh-16")))
    }

    @Test
    fun legendsArceusStarterTracksRealLevels(){
        assertEquals(155,JourneyTeamProgressCatalog.starterMemberForProgress(155,step("Legends Arceus","la-02")))
        assertEquals(156,JourneyTeamProgressCatalog.starterMemberForProgress(155,step("Legends Arceus","la-07")))
        assertEquals(157,JourneyTeamProgressCatalog.starterMemberForProgress(155,step("Legends Arceus","la-10")))
    }

    @Test
    fun bdspStarterTracksRealLevels(){
        assertEquals(387,JourneyTeamProgressCatalog.starterMemberForProgress(387,step("Brilliant Diamond / Shining Pearl","bdsp-g1")))
        assertEquals(388,JourneyTeamProgressCatalog.starterMemberForProgress(387,step("Brilliant Diamond / Shining Pearl","bdsp-g2")))
        assertEquals(389,JourneyTeamProgressCatalog.starterMemberForProgress(387,step("Brilliant Diamond / Shining Pearl","bdsp-g6")))
    }

    @Test
    fun letsGoPartnersNeverEvolve(){
        assertEquals(25,JourneyTeamProgressCatalog.starterMemberForProgress(25,step("Let's Go Pikachu / Eevee","lgpe-g8")))
        assertEquals(133,JourneyTeamProgressCatalog.starterMemberForProgress(133,step("Let's Go Pikachu / Eevee","lgpe-22")))
    }

    @Test
    fun fireRedLeafGreenStartersTrackRealLevels(){
        assertEquals(4,JourneyTeamProgressCatalog.starterMemberForProgress(4,step("FireRed / LeafGreen","frlg-g1")))
        assertEquals(5,JourneyTeamProgressCatalog.starterMemberForProgress(4,step("FireRed / LeafGreen","frlg-g2")))
        assertEquals(6,JourneyTeamProgressCatalog.starterMemberForProgress(4,step("FireRed / LeafGreen","frlg-g7")))
    }

    @Test
    fun legendsZaStartersTrackRealLevels(){
        assertEquals(152,JourneyTeamProgressCatalog.starterMemberForProgress(152,step("Pokémon Legends: Z-A","za-05")))
        assertEquals(153,JourneyTeamProgressCatalog.starterMemberForProgress(152,step("Pokémon Legends: Z-A","za-06")))
        assertEquals(154,JourneyTeamProgressCatalog.starterMemberForProgress(152,step("Pokémon Legends: Z-A","za-35")))
    }

    @Test
    fun everyMainStoryStepKeepsOneStarterFamilyAndNoDuplicateFamilies(){
        TeamCampaignCatalog.switchGames.forEach{game->
            val main=JourneyCatalog.steps(game).takeWhile{
                it.kind !in setOf(JourneyChallengeKind.POSTGAME,JourneyChallengeKind.DLC,JourneyChallengeKind.EPILOGUE)
            }
            TeamCampaignCatalog.starters(game).forEach{(_,starterId)->
                main.forEachIndexed{index,focus->
                    val ratio=if(main.size<=1)1f else index.toFloat()/(main.size-1)
                    val phase=when{
                        ratio < .34f -> CampaignPhase.EARLY
                        ratio < .72f -> CampaignPhase.MID
                        else -> CampaignPhase.LATE
                    }
                    val preset=TeamCampaignCatalog.preset(game,starterId,phase)!!
                    val team=preset.slots.map{JourneyTeamProgressCatalog.progressMemberFor(it.pokemonId,focus)}
                    assertFalse(game+" "+focus.id+" duplicate family: "+team,JourneyTeamProgressCatalog.hasFamilyDuplicate(team))
                    val expectedStarter=JourneyTeamProgressCatalog.starterMemberForProgress(starterId,focus)
                    assertEquals(
                        game+" "+focus.id+" starter family must appear exactly once",
                        1,
                        team.count{JourneyTeamProgressCatalog.sameEvolutionFamily(it,expectedStarter)}
                    )
                }
            }
        }
    }

    @Test
    fun deterministicNonStarterFamiliesAlsoTrackObjectiveLevel(){
        assertEquals(822,JourneyTeamProgressCatalog.progressMemberFor(821,step("Scarlet / Violet","sv-06")))
        assertEquals(823,JourneyTeamProgressCatalog.progressMemberFor(821,step("Scarlet / Violet","sv-15")))
        assertEquals(130,JourneyTeamProgressCatalog.progressMemberFor(129,step("Brilliant Diamond / Shining Pearl","bdsp-g4")))
        assertEquals(398,JourneyTeamProgressCatalog.progressMemberFor(396,step("Brilliant Diamond / Shining Pearl","bdsp-g8")))
        assertEquals(836,JourneyTeamProgressCatalog.progressMemberFor(835,step("Sword / Shield","swsh-g3")))
        assertEquals(405,JourneyTeamProgressCatalog.progressMemberFor(403,step("Legends Arceus","la-10")))
        assertEquals(20,JourneyTeamProgressCatalog.progressMemberFor(19,step("FireRed / LeafGreen","frlg-g4")))
    }

    @Test
    fun representativeTeamsHaveNoEvolutionFamilyDuplicates(){
        val samples=listOf(
            arrayOf("Scarlet / Violet",909,CampaignPhase.LATE,"sv-15"),
            arrayOf("Sword / Shield",813,CampaignPhase.LATE,"swsh-16"),
            arrayOf("Legends Arceus",155,CampaignPhase.LATE,"la-17"),
            arrayOf("Brilliant Diamond / Shining Pearl",390,CampaignPhase.LATE,"bdsp-24"),
            arrayOf("Let's Go Pikachu / Eevee",25,CampaignPhase.LATE,"lgpe-22"),
            arrayOf("FireRed / LeafGreen",4,CampaignPhase.LATE,"frlg-23"),
            arrayOf("Pokémon Legends: Z-A",152,CampaignPhase.LATE,"za-35")
        )
        samples.forEach{sample->
            val game=sample[0] as String
            val starter=sample[1] as Int
            val phase=sample[2] as CampaignPhase
            val stepId=sample[3] as String
            val team=normalizedTeam(game,starter,phase,stepId)
            assertFalse(game+" has duplicate evolution families: "+team,JourneyTeamProgressCatalog.hasFamilyDuplicate(team))
        }
    }
}
