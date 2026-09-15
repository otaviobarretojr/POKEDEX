package com.otaviobarreto.pokedex.data

import android.content.Context
import java.io.File

/**
 * Persistent state machine for the server general package.
 * It survives process death and lets the UI distinguish download, validation,
 * extraction, installation, audit and a recoverable failure.
 */
object OfflinePackageInstallState {
    private const val PREFS="offline_package_install_state_v1"
    private const val KEY_STAGE="general_stage"
    private const val KEY_VERSION="general_version"
    private const val KEY_DONE="general_done"
    private const val KEY_TOTAL="general_total"
    private const val KEY_ERROR="general_error"
    private const val KEY_UPDATED="general_updated"

    enum class Stage {
        IDLE, DOWNLOADING, DOWNLOADED, VALIDATING, EXTRACTING, INSTALLING, AUDITING, INSTALLED, FAILED
    }

    data class Snapshot(
        val stage:Stage,
        val version:Int,
        val done:Long,
        val total:Long,
        val error:String?,
        val updatedAt:Long
    ){
        val active:Boolean get()=stage in setOf(
            Stage.DOWNLOADING,Stage.DOWNLOADED,Stage.VALIDATING,
            Stage.EXTRACTING,Stage.INSTALLING,Stage.AUDITING
        )
    }

    fun read(context:Context):Snapshot {
        val p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        return Snapshot(
            stage=runCatching{Stage.valueOf(p.getString(KEY_STAGE,Stage.IDLE.name)!!)}.getOrDefault(Stage.IDLE),
            version=p.getInt(KEY_VERSION,0),
            done=p.getLong(KEY_DONE,0L),
            total=p.getLong(KEY_TOTAL,0L),
            error=p.getString(KEY_ERROR,null),
            updatedAt=p.getLong(KEY_UPDATED,0L)
        )
    }

    fun write(context:Context,stage:Stage,version:Int,done:Long=0L,total:Long=0L,error:String?=null){
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putString(KEY_STAGE,stage.name)
            .putInt(KEY_VERSION,version)
            .putLong(KEY_DONE,done)
            .putLong(KEY_TOTAL,total)
            .putString(KEY_ERROR,error)
            .putLong(KEY_UPDATED,System.currentTimeMillis())
            .commit()
    }

    fun packageRoot(context:Context)=File(context.filesDir,"offline-packages").apply{mkdirs()}
    fun generalZip(context:Context,version:Int)=File(packageRoot(context),"general-v$version.zip")
    fun generalExtract(context:Context,version:Int)=File(packageRoot(context),"general-v$version-extract")

    fun clear(context:Context){
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().clear().commit()
    }
}
