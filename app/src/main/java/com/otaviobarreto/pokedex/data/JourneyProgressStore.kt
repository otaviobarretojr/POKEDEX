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

    fun isStarted(game:String):Boolean{
        val prefs=context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE) ?: return false
        val completedAny=completed(game).isNotEmpty()
        val starterRequired=TeamCampaignCatalog.starters(game).isNotEmpty()
        val starterReady=!starterRequired || AppStatePreferences.journeyStarterForGame(game)!=null
        return completedAny || (prefs.getBoolean(startedKey(game),false) && starterReady)
    }

    fun isConfiguring(game:String):Boolean{
        val prefs=context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE) ?: return false
        if(isStarted(game)) return false
        val legacyIncomplete=prefs.getBoolean(startedKey(game),false) &&
            TeamCampaignCatalog.starters(game).isNotEmpty() &&
            AppStatePreferences.journeyStarterForGame(game)==null &&
            completed(game).isEmpty()
        return prefs.getBoolean(configuringKey(game),false) || legacyIncomplete
    }

    fun beginConfiguration(game:String,reset:Boolean=true){
        val prefs=context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE) ?: return
        val editor=prefs.edit()
            .putBoolean(configuringKey(game),true)
            .putBoolean(startedKey(game),false)
        if(reset) editor.remove(key(game))
        editor.apply()
        revision++
    }

    fun confirmStart(game:String){
        context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()
            ?.putBoolean(startedKey(game),true)
            ?.putBoolean(configuringKey(game),false)
            ?.apply()
        revision++
    }

    fun cancelConfiguration(game:String){
        context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()
            ?.putBoolean(configuringKey(game),false)
            ?.apply()
        revision++
    }

    fun start(game:String,reset:Boolean=true){
        beginConfiguration(game,reset)
        confirmStart(game)
    }

    fun toggle(game:String,stepId:String){
        val current=completed(game)
        if(stepId in current){
            setCompleted(game,stepId,false)
            return
        }
        val ordered=JourneyCatalog.steps(game).map{it.id}
        if(stepId in ordered){
            completeThrough(game,ordered,stepId)
        }else{
            setCompleted(game,stepId,true)
        }
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

    fun resetProgress(game:String){
        context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()
            ?.remove(key(game))
            ?.apply()
        revision++
    }

    fun endJourney(game:String){
        context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()
            ?.remove(key(game))
            ?.putBoolean(startedKey(game),false)
            ?.putBoolean(configuringKey(game),false)
            ?.apply()
        revision++
    }

    @Deprecated("Use resetProgress(game) when keeping the Journey active, or endJourney(game) to finish it.")
    fun clear(game:String)=resetProgress(game)

    fun exportSnapshot():JSONObject{
        val games=JSONObject()
        val started=JSONObject()
        val configuring=JSONObject()
        AppGameCatalog.adventureGames.forEach { game ->
            games.put(game.label, JSONArray(completed(game.label).sorted()))
            started.put(game.label,isStarted(game.label))
            configuring.put(game.label,isConfiguring(game.label))
        }
        return JSONObject().put("games",games).put("started",started).put("configuring",configuring)
    }

    fun importSnapshot(snapshot:JSONObject):Boolean=runCatching{
        val games=snapshot.optJSONObject("games") ?: JSONObject()
        val started=snapshot.optJSONObject("started")
        val configuring=snapshot.optJSONObject("configuring")
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
            val wasStarted=started?.optBoolean(game.label,restored.isNotEmpty()) ?: restored.isNotEmpty()
            editor.putBoolean(startedKey(game.label),wasStarted)
            editor.putBoolean(configuringKey(game.label),configuring?.optBoolean(game.label,false)==true && !wasStarted)
        }
        editor.apply()
        revision++
        true
    }.getOrDefault(false)

    private fun key(game:String)="completed_"+game.lowercase().replace(Regex("[^a-z0-9]+"),"_")
    private fun startedKey(game:String)="started_"+game.lowercase().replace(Regex("[^a-z0-9]+"),"_")
    private fun configuringKey(game:String)="configuring_"+game.lowercase().replace(Regex("[^a-z0-9]+"),"_")
}
