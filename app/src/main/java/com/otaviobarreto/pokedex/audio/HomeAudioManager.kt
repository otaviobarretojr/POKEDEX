package com.otaviobarreto.pokedex.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.otaviobarreto.pokedex.R

enum class HomeAudioScene(
    val rawResId: Int,
    val looping: Boolean
) {
    BOOT(R.raw.pokehome_st_sys01, false),
    JOURNEY(R.raw.pokehome_ps_01, true),
    BOXES(R.raw.pokehome_st_sys02, true),
    DETAIL(R.raw.pokehome_st_sys03, true)
}

object HomeAudioManager {
    private const val PREFS = "home_audio_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_VOLUME = "volume"
    private const val DEFAULT_VOLUME = 0.62f

    private var appContext: Context? = null
    private var player: MediaPlayer? = null
    private var currentScene: HomeAudioScene? = null
    private var appInForeground = true

    fun initialize(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    val enabled: Boolean
        get() = prefs()?.getBoolean(KEY_ENABLED, true) ?: true

    var volume: Float
        get() = prefs()?.getFloat(KEY_VOLUME, DEFAULT_VOLUME) ?: DEFAULT_VOLUME
        set(value) {
            val safe = value.coerceIn(0f, 1f)
            prefs()?.edit()?.putFloat(KEY_VOLUME, safe)?.apply()
            player?.setVolume(safe, safe)
        }

    fun setEnabled(value: Boolean) {
        prefs()?.edit()?.putBoolean(KEY_ENABLED, value)?.apply()
        if (!value) {
            releasePlayer()
        } else if (appInForeground) {
            currentScene?.let(::playScene)
        }
    }

    fun playBoot() {
        playScene(HomeAudioScene.BOOT, restart = true)
    }

    fun playForRoute(route: String?) {
        val scene = when (route) {
            "boxes" -> HomeAudioScene.BOXES
            "home", null -> HomeAudioScene.JOURNEY
            else -> HomeAudioScene.DETAIL
        }
        playScene(scene)
    }

    fun playScene(scene: HomeAudioScene, restart: Boolean = false) {
        val previousScene = currentScene
        currentScene = scene
        if (!enabled || !appInForeground) return
        if (!restart && player != null && player?.isPlaying == true && previousScene == scene) return

        releasePlayer(clearScene = false)
        val context = appContext ?: return
        player = MediaPlayer.create(context, scene.rawResId)?.apply {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(attrs)
            isLooping = scene.looping
            val level = volume
            setVolume(level, level)
            setOnCompletionListener {
                if (!scene.looping) {
                    it.release()
                    if (player === it) player = null
                }
            }
            start()
        }
    }

    fun onAppBackgrounded() {
        appInForeground = false
        runCatching {
            if (player?.isPlaying == true) player?.pause()
        }
    }

    fun onAppForegrounded() {
        appInForeground = true
        if (!enabled) return
        val active = player
        if (active != null) {
            runCatching { active.start() }
        } else {
            currentScene?.let(::playScene)
        }
    }

    fun release() {
        releasePlayer()
        appContext = null
    }

    private fun releasePlayer(clearScene: Boolean = false) {
        runCatching { player?.stop() }
        runCatching { player?.reset() }
        runCatching { player?.release() }
        player = null
        if (clearScene) currentScene = null
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
