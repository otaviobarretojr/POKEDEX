package com.otaviobarreto.pokedex.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object JourneyProgressStore {
    private const val PREFS="journey_progress"
    private var context:Context?=null
    var revision by mutableIntStateOf(0)
        private set

    fun initialize(context:Context){this.context=context.applicationContext}

    fun completed(game:String):Set<String>{
        return context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            ?.getStringSet(key(game),emptySet())?.toSet().orEmpty()
    }

    fun toggle(game:String,stepId:String){
        val next=completed(game).toMutableSet().apply{
            if(!add(stepId)) remove(stepId)
        }
        context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()
            ?.putStringSet(key(game),next)?.apply()
        revision++
    }

    fun clear(game:String){
        context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()?.remove(key(game))?.apply()
        revision++
    }

    private fun key(game:String)="completed_"+game.lowercase().replace(Regex("[^a-z0-9]+"),"_")
}
