package com.otaviobarreto.pokedex.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.otaviobarreto.pokedex.R

enum class AppSoundCue(val rawRes:Int) {
    CLICK(R.raw.home_ui_tap),
    DECIDE(R.raw.home_ui_decide),
    CANCEL(R.raw.home_ui_cancel),
    OPEN(R.raw.home_ui_window_open),
    CLOSE(R.raw.home_ui_window_close),
    MOVE(R.raw.home_ui_swipe),
    TOGGLE(R.raw.home_ui_select),
    LONG_PRESS(R.raw.home_ui_long_press),
    CHECK_BOX(R.raw.home_ui_check_box),
    SORT_DONE(R.raw.home_ui_sort),
    CATCH(R.raw.home_ui_catch),
    ESCAPE(R.raw.home_ui_escape),
    RL(R.raw.home_ui_rl)
}

object AppSoundManager {
    private const val PREFS="app_sound_preferences"
    private const val KEY_EFFECTS="effects_enabled"
    private const val KEY_MUSIC="music_enabled"
    private const val KEY_EFFECTS_VOLUME="effects_volume"
    private const val KEY_MUSIC_VOLUME="music_volume"

    private var appContext:Context?=null
    private var soundPool:SoundPool?=null
    private val soundIds=linkedMapOf<AppSoundCue,Int>()
    private val loaded=mutableSetOf<Int>()
    private var bootPlayer:MediaPlayer?=null

    fun initialize(context:Context){
        if(appContext!=null)return
        appContext=context.applicationContext
        val attributes=AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool=SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attributes)
            .build()
            .also{pool->
                pool.setOnLoadCompleteListener{_,sampleId,status->if(status==0)loaded+=sampleId}
                AppSoundCue.entries.forEach{cue->soundIds[cue]=pool.load(context,cue.rawRes,1)}
            }
    }

    fun play(cue:AppSoundCue,rate:Float=1f){
        val ctx=appContext?:return
        if(!effectsEnabled(ctx))return
        val id=soundIds[cue]?:return
        if(id !in loaded)return
        val volume=effectsVolume(ctx)
        soundPool?.play(id,volume,volume,1,0,rate.coerceIn(.5f,2f))
    }

    fun playBootMusic(){
        val ctx=appContext?:return
        if(!musicEnabled(ctx))return
        stopMusic()
        bootPlayer=MediaPlayer.create(ctx,R.raw.home_bgm_boot)?.apply{
            isLooping=false
            val volume=musicVolume(ctx)
            setVolume(volume,volume)
            start()
        }
    }

    fun stopMusic(){
        bootPlayer?.let{player->runCatching{if(player.isPlaying)player.stop()};runCatching{player.release()}}
        bootPlayer=null
    }

    fun effectsEnabled(context:Context=appContext?:return true):Boolean=
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(KEY_EFFECTS,true)

    fun musicEnabled(context:Context=appContext?:return true):Boolean=
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(KEY_MUSIC,true)

    fun effectsVolume(context:Context=appContext?:return 1f):Float=
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getFloat(KEY_EFFECTS_VOLUME,.85f).coerceIn(0f,1f)

    fun musicVolume(context:Context=appContext?:return 1f):Float=
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getFloat(KEY_MUSIC_VOLUME,.55f).coerceIn(0f,1f)

    fun setEffectsEnabled(enabled:Boolean){appContext?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_EFFECTS,enabled)?.apply()}
    fun setMusicEnabled(enabled:Boolean){appContext?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_MUSIC,enabled)?.apply()}
    fun setEffectsVolume(value:Float){appContext?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()?.putFloat(KEY_EFFECTS_VOLUME,value.coerceIn(0f,1f))?.apply()}
    fun setMusicVolume(value:Float){appContext?.getSharedPreferences(PREFS,Context.MODE_PRIVATE)?.edit()?.putFloat(KEY_MUSIC_VOLUME,value.coerceIn(0f,1f))?.apply()}
}
