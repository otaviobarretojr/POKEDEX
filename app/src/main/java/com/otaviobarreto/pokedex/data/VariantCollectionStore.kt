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
    val shiny:Boolean,
    val formKey:String=formName.lowercase(),
    val normalArtworkUrl:String?=null,
    val shinyArtworkUrl:String?=null
) {
    val key:String get() = listOf(source,speciesId,formPokemonId,formKey.lowercase(),shiny).joinToString("|")
    val artworkUrl:String
        get() = (if(shiny) shinyArtworkUrl else normalArtworkUrl)
            ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" +
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

    fun isOwned(source:String,speciesId:Int,formPokemonId:Int,formName:String?=null,shiny:Boolean):Boolean =
        ownedVariants.any{
            it.source==source && it.speciesId==speciesId &&
                it.formPokemonId==formPokemonId &&
                (formName==null || it.formName.equals(formName,true)) &&
                it.shiny==shiny
        }

    fun setOwned(
        source:String,
        speciesId:Int,
        formPokemonId:Int,
        formName:String,
        shiny:Boolean,
        owned:Boolean,
        formKey:String=formName.lowercase(),
        normalArtworkUrl:String?=null,
        shinyArtworkUrl:String?=null
    ){
        if(source.isBlank() || speciesId<=0 || formPokemonId<=0) return
        val entry=OwnedPokemonVariant(
            source,speciesId,formPokemonId,formName,shiny,
            formKey=formKey,
            normalArtworkUrl=normalArtworkUrl,
            shinyArtworkUrl=shinyArtworkUrl
        )
        fun matchesExisting(value:OwnedPokemonVariant):Boolean =
            value.key==entry.key || (
                value.source==entry.source &&
                    value.speciesId==entry.speciesId &&
                    value.formPokemonId==entry.formPokemonId &&
                    value.formName.equals(entry.formName,true) &&
                    value.shiny==entry.shiny
            )
        val exists=ownedVariants.any(::matchesExisting)
        ownedVariants=when{
            owned && !exists -> ownedVariants + entry
            owned && exists -> ownedVariants.map{if(matchesExisting(it)) entry else it}.distinctBy{it.key}
            !owned && exists -> ownedVariants.filterNot(::matchesExisting)
            else -> ownedVariants
        }
        if(owned) {
            CollectionStore.setCapturedIn(source,speciesId,true)
        } else if(ownedVariants.none { it.source==source && it.speciesId==speciesId }) {
            CollectionStore.setCapturedIn(source,speciesId,false)
        }
        persist()
    }

    fun toggle(
        source:String,
        speciesId:Int,
        formPokemonId:Int,
        formName:String,
        shiny:Boolean,
        formKey:String=formName.lowercase(),
        normalArtworkUrl:String?=null,
        shinyArtworkUrl:String?=null
    ) = setOwned(
        source,speciesId,formPokemonId,formName,shiny,
        !isOwned(source,speciesId,formPokemonId,formName,shiny),
        formKey=formKey,
        normalArtworkUrl=normalArtworkUrl,
        shinyArtworkUrl=shinyArtworkUrl
    )

    fun preferred(source:String,speciesId:Int):OwnedPokemonVariant? =
        variantsFor(source,speciesId)
            .sortedWith(compareBy<OwnedPokemonVariant>{it.shiny}.thenBy{it.formPokemonId!=speciesId})
            .firstOrNull()

    fun removeAll(source:String,speciesId:Int){
        val before=ownedVariants.size
        ownedVariants=ownedVariants.filterNot{it.source==source && it.speciesId==speciesId}
        if(ownedVariants.size!=before) persist()
        CollectionStore.setCapturedIn(source,speciesId,false)
    }

    fun shinyCount():Int = ownedVariants.count{it.shiny}
    fun formCount():Int = ownedVariants.map{listOf(it.source,it.speciesId.toString(),it.formPokemonId.toString(),it.formName.lowercase())}.distinct().size
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
                        .put("formKey",v.formKey)
                        .put("normalArtworkUrl",v.normalArtworkUrl)
                        .put("shinyArtworkUrl",v.shinyArtworkUrl)
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
                        shiny=o.optBoolean("shiny",false),
                        formKey=o.optString("formKey").takeIf{it.isNotBlank()}
                            ?: o.optString("formName","Forma").lowercase(),
                        normalArtworkUrl=o.optString("normalArtworkUrl").takeIf{it.isNotBlank() && it!="null"},
                        shinyArtworkUrl=o.optString("shinyArtworkUrl").takeIf{it.isNotBlank() && it!="null"}
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
                        shiny=o.optBoolean("shiny",false),
                        formKey=o.optString("formKey").takeIf{it.isNotBlank()}
                            ?: o.optString("formName","Forma").lowercase(),
                        normalArtworkUrl=o.optString("normalArtworkUrl").takeIf{it.isNotBlank() && it!="null"},
                        shinyArtworkUrl=o.optString("shinyArtworkUrl").takeIf{it.isNotBlank() && it!="null"}
                    )
                )
            }
        }.distinctBy{it.key}
    }.getOrDefault(emptyList())
}
