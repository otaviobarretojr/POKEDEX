package com.otaviobarreto.pokedex.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

data class OwnedPokemonVariant(
    val source:String,
    val speciesId:Int,
    val formPokemonId:Int,
    val formName:String,
    val shiny:Boolean
) {
    val key:String get() = listOf(source,speciesId,formPokemonId,shiny).joinToString("|")
    val artworkUrl:String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" +
            (if(shiny)"shiny/" else "") + formPokemonId + ".png"
}

object VariantCollectionStore {
    private const val PREFS = "pokedex_variant_collection"
    private const val KEY_VARIANTS = "owned_variants_v1"
    private var context:Context?=null

    var ownedVariants by mutableStateOf<List<OwnedPokemonVariant>>(emptyList())
        private set

    fun initialize(context:Context){
        if(this.context!=null) return
        this.context=context.applicationContext
        val raw=this.context!!.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .getString(KEY_VARIANTS,null)
        ownedVariants=decode(raw)
    }

    fun variantsFor(source:String,speciesId:Int):List<OwnedPokemonVariant> =
        ownedVariants.filter{it.source==source && it.speciesId==speciesId}

    fun isOwned(source:String,speciesId:Int,formPokemonId:Int,shiny:Boolean):Boolean =
        ownedVariants.any{
            it.source==source && it.speciesId==speciesId &&
                it.formPokemonId==formPokemonId && it.shiny==shiny
        }

    fun setOwned(
        source:String,
        speciesId:Int,
        formPokemonId:Int,
        formName:String,
        shiny:Boolean,
        owned:Boolean
    ){
        if(source.isBlank() || speciesId<=0 || formPokemonId<=0) return
        val entry=OwnedPokemonVariant(source,speciesId,formPokemonId,formName,shiny)
        val exists=ownedVariants.any{it.key==entry.key}
        ownedVariants=when{
            owned && !exists -> ownedVariants + entry
            !owned && exists -> ownedVariants.filterNot{it.key==entry.key}
            else -> ownedVariants
        }
        if(owned) CollectionStore.setCapturedIn(source,speciesId,true)
        persist()
    }

    fun toggle(
        source:String,
        speciesId:Int,
        formPokemonId:Int,
        formName:String,
        shiny:Boolean
    ) = setOwned(
        source,speciesId,formPokemonId,formName,shiny,
        !isOwned(source,speciesId,formPokemonId,shiny)
    )

    fun preferred(source:String,speciesId:Int):OwnedPokemonVariant? =
        variantsFor(source,speciesId)
            .sortedWith(compareBy<OwnedPokemonVariant>{it.shiny}.thenBy{it.formPokemonId!=speciesId})
            .firstOrNull()

    fun shinyCount():Int = ownedVariants.count{it.shiny}
    fun formCount():Int = ownedVariants.map{Triple(it.source,it.speciesId,it.formPokemonId)}.distinct().size
    fun speciesWithVariants():Int = ownedVariants.map{it.speciesId}.distinct().size

    fun exportSnapshot():JSONArray = JSONArray().also{array->
        ownedVariants.sortedWith(compareBy<OwnedPokemonVariant>{it.source}.thenBy{it.speciesId}.thenBy{it.formPokemonId}.thenBy{it.shiny})
            .forEach{v->
                array.put(
                    JSONObject()
                        .put("source",v.source)
                        .put("speciesId",v.speciesId)
                        .put("formPokemonId",v.formPokemonId)
                        .put("formName",v.formName)
                        .put("shiny",v.shiny)
                )
            }
    }

    fun importSnapshot(array:JSONArray):Boolean = runCatching{
        val restored=buildList{
            for(i in 0 until array.length()){
                val o=array.optJSONObject(i)?:continue
                val source=o.optString("source")
                val species=o.optInt("speciesId")
                val form=o.optInt("formPokemonId")
                if(source.isBlank() || species !in 1..PokeApiService.MAX_NATIONAL_DEX_ID || form<=0) continue
                add(
                    OwnedPokemonVariant(
                        source=source,
                        speciesId=species,
                        formPokemonId=form,
                        formName=o.optString("formName","Forma"),
                        shiny=o.optBoolean("shiny",false)
                    )
                )
            }
        }.distinctBy{it.key}
        ownedVariants=restored
        ownedVariants.forEach{CollectionStore.setCapturedIn(it.source,it.speciesId,true)}
        persist()
        true
    }.getOrDefault(false)

    private fun persist(){
        context?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()
            ?.putString(KEY_VARIANTS,exportSnapshot().toString())?.apply()
    }

    private fun decode(raw:String?):List<OwnedPokemonVariant> = runCatching{
        if(raw.isNullOrBlank()) return@runCatching emptyList()
        val array=JSONArray(raw)
        buildList{
            for(i in 0 until array.length()){
                val o=array.optJSONObject(i)?:continue
                val source=o.optString("source")
                val species=o.optInt("speciesId")
                val form=o.optInt("formPokemonId")
                if(source.isBlank() || species !in 1..PokeApiService.MAX_NATIONAL_DEX_ID || form<=0) continue
                add(
                    OwnedPokemonVariant(
                        source=source,
                        speciesId=species,
                        formPokemonId=form,
                        formName=o.optString("formName","Forma"),
                        shiny=o.optBoolean("shiny",false)
                    )
                )
            }
        }.distinctBy{it.key}
    }.getOrDefault(emptyList())
}
