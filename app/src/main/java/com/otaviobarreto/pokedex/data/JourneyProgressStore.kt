package com.otaviobarreto.pokedex.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

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

    fun setCompleted(game:String,stepId:String,completed:Boolean){
        val next=this.completed(game).toMutableSet().apply{
            if(completed)add(stepId) else remove(stepId)
        }
        context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()
            ?.putStringSet(key(game),next)?.apply()
        revision++
    }

    fun completeThrough(game:String,orderedStepIds:List<String>,stepId:String){
        val index=orderedStepIds.indexOf(stepId)
        if(index<0)return
        val next=completed(game).toMutableSet().apply{addAll(orderedStepIds.take(index+1))}
        context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()
            ?.putStringSet(key(game),next)?.apply()
        revision++
    }

    fun clear(game:String){
        context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()?.remove(key(game))?.apply()
        revision++
    }

    fun exportSnapshot():JSONObject{
        val games=JSONObject()
        AppGameCatalog.adventureGames.forEach { game ->
            games.put(game.label, JSONArray(completed(game.label).sorted()))
        }
        return JSONObject().put("games",games)
    }

    fun importSnapshot(snapshot:JSONObject):Boolean=runCatching{
        val games=snapshot.optJSONObject("games") ?: JSONObject()
        val prefs=context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE) ?: return@runCatching false
        val editor=prefs.edit()
        AppGameCatalog.adventureGames.forEach { game ->
            val array=games.optJSONArray(game.label) ?: JSONArray()
            val validSteps=JourneyCatalog.steps(game.label).map{it.id}.toSet()
            val restored=buildSet{
                for(i in 0 until array.length()){
                    array.optString(i).takeIf{it in validSteps}?.let(::add)
                }
            }
            editor.putStringSet(key(game.label),restored)
        }
        editor.apply()
        revision++
        true
    }.getOrDefault(false)

    private fun key(game:String)="completed_"+game.lowercase().replace(Regex("[^a-z0-9]+"),"_")
}
