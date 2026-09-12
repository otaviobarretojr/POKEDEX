package com.otaviobarreto.pokedex.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.otaviobarreto.pokedex.R

private enum class HomeAudioTrack(
    val rawResId: Int,
    val looping: Boolean
) {
    BOOT(R.raw.pokehome_st_sys01, true),
    APP(R.raw.pokehome_st_sys02, true)
}

object HomeAudioManager {
    private const val PREFS = "home_audio_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_VOLUME = "volume"
    private const val DEFAULT_VOLUME = 0.62f

    private var appContext: Context? = null
    private var player: MediaPlayer? = null
    private var currentTrack: HomeAudioTrack? = null
    private var loadedTrack: HomeAudioTrack? = null
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
            currentTrack?.let { playTrack(it) }
        }
    }

    fun playBoot() {
        playTrack(HomeAudioTrack.BOOT, restart = true)
    }

    fun playMainTrack() {
        playTrack(HomeAudioTrack.APP)
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
        if (active != null && loadedTrack == currentTrack) {
            runCatching { active.start() }
        } else {
            currentTrack?.let { playTrack(it, restart = true) }
        }
    }

    fun release() {
        releasePlayer()
        appContext = null
    }

    private fun playTrack(track: HomeAudioTrack, restart: Boolean = false) {
        currentTrack = track
        if (!enabled || !appInForeground) return
        if (!restart && player != null && player?.isPlaying == true && loadedTrack == track) return

        releasePlayer(clearTrack = false)
        val context = appContext ?: return
        val descriptor = runCatching { context.resources.openRawResourceFd(track.rawResId) }.getOrNull() ?: return

        val candidate = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                isLooping = track.looping
                val level = volume
                setVolume(level, level)
            }
        }.getOrNull()
        descriptor.close()

        if (candidate == null) {
            loadedTrack = null
            return
        }

        player = candidate
        loadedTrack = track
        candidate.setOnPreparedListener { prepared ->
            if (player === prepared && loadedTrack == track && currentTrack == track && enabled && appInForeground) {
                runCatching { prepared.start() }
            }
        }
        candidate.setOnErrorListener { failed, _, _ ->
            if (player === failed) releasePlayer(clearTrack = false)
            true
        }
        runCatching { candidate.prepareAsync() }
            .onFailure {
                if (player === candidate) releasePlayer(clearTrack = false)
            }
    }

    private fun releasePlayer(clearTrack: Boolean = false) {
        val active = player
        player = null
        loadedTrack = null
        runCatching { active?.setOnPreparedListener(null) }
        runCatching { active?.setOnErrorListener(null) }
        runCatching { active?.stop() }
        runCatching { active?.reset() }
        runCatching { active?.release() }
        if (clearTrack) currentTrack = null
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
