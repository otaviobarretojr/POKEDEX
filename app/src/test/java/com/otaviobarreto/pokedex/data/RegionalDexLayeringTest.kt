package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class RegionalDexLayeringTest {

    private fun entry(id:Int,number:Int=id)=GameDexService.GameDexEntry(
        nationalId=id,
        gameNumber=number,
        name="Pokemon $id"
    )

    @Test
    fun secondRegionExcludesEverythingAlreadyShownInBaseGame() {
        val game=AppGame(
            "Test Game",
            listOf(
                AppRegion("Base","Test · Base","Jogo base"),
                AppRegion("DLC 1","Test · DLC 1","DLC")
            )
        )

        val entries=mapOf(
            "Test · Base" to listOf(entry(1),entry(2),entry(3)),
            "Test · DLC 1" to listOf(entry(2),entry(3),entry(4),entry(5))
        )

        assertEquals(
            setOf(4,5),
            RegionalDexLayering.exclusiveIdsForRegion(game,"Test · DLC 1",entries)
        )
    }

    @Test
    fun thirdRegionExcludesSpeciesSeenInBaseAndFirstDlc() {
        val game=AppGame(
            "Test Game",
            listOf(
                AppRegion("Base","Test · Base","Jogo base"),
                AppRegion("DLC 1","Test · DLC 1","DLC"),
                AppRegion("DLC 2","Test · DLC 2","DLC")
            )
        )

        val entries=mapOf(
            "Test · Base" to listOf(entry(1),entry(2),entry(3)),
            "Test · DLC 1" to listOf(entry(2),entry(4),entry(5)),
            "Test · DLC 2" to listOf(entry(1),entry(4),entry(5),entry(6),entry(7))
        )

        assertEquals(
            setOf(6,7),
            RegionalDexLayering.exclusiveIdsForRegion(game,"Test · DLC 2",entries)
        )
    }

    @Test
    fun baseRegionKeepsItsCompleteDex() {
        val game=AppGame(
            "Test Game",
            listOf(
                AppRegion("Base","Test · Base","Jogo base"),
                AppRegion("DLC","Test · DLC","DLC")
            )
        )

        val entries=mapOf(
            "Test · Base" to listOf(entry(10),entry(20),entry(30)),
            "Test · DLC" to listOf(entry(20),entry(40))
        )

        assertEquals(
            setOf(10,20,30),
            RegionalDexLayering.exclusiveIdsForRegion(game,"Test · Base",entries)
        )
    }

    @Test
    fun exclusiveEntriesPreserveRegionalDexOrdering() {
        val game=AppGame(
            "Test Game",
            listOf(
                AppRegion("Base","Test · Base","Jogo base"),
                AppRegion("DLC","Test · DLC","DLC")
            )
        )

        val entries=mapOf(
            "Test · Base" to listOf(entry(1),entry(2)),
            "Test · DLC" to listOf(entry(2,1),entry(5,2),entry(4,3))
        )

        assertEquals(
            listOf(5,4),
            RegionalDexLayering.exclusiveEntriesForRegion(game,"Test · DLC",entries).map{it.nationalId}
        )
    }
}
