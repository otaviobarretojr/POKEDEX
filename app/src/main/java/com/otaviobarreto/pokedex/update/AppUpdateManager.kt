package com.otaviobarreto.pokedex.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class AppUpdate(val versionCode:Int,val versionName:String,val apkUrl:String,val changelog:String,val mandatory:Boolean=false)

object AppUpdateManager {
    private const val MANIFEST_URL="https://raw.githubusercontent.com/otaviobarretojr/POKEDEX/main/update.json"
    private val client=OkHttpClient()
    suspend fun check():AppUpdate?=withContext(Dispatchers.IO){
        runCatching{
            val request=Request.Builder().url(MANIFEST_URL).cacheControl(okhttp3.CacheControl.FORCE_NETWORK).build()
            val body=client.newCall(request).execute().use{response->
                check(response.isSuccessful){"HTTP "+response.code}
                response.body?.string()?:error("Manifesto vazio")
            }
            val json=JSONObject(body)
            AppUpdate(json.getInt("versionCode"),json.getString("versionName"),json.getString("apkUrl"),json.optString("changelog","Melhorias e correções."),json.optBoolean("mandatory",false))
                .takeIf{it.versionCode>installedVersionCode()}
        }.getOrNull()
    }
    private fun installedVersionCode():Int = com.otaviobarreto.pokedex.BuildConfig.VERSION_CODE
    suspend fun download(context:Context,update:AppUpdate):File=withContext(Dispatchers.IO){
        val request=Request.Builder().url(update.apkUrl).build()
        val targetDir=File(context.cacheDir,"updates").apply{mkdirs()}
        val target=File(targetDir,"pokedex-"+update.versionName+".apk")
        client.newCall(request).execute().use{response->
            check(response.isSuccessful){"Falha no download: HTTP "+response.code}
            val body=response.body?:error("APK vazio")
            target.outputStream().use{output->body.byteStream().use{it.copyTo(output)}}
        }
        check(target.length()>0L){"APK inválido"};target
    }
    fun canInstallPackages(context:Context):Boolean=android.os.Build.VERSION.SDK_INT<android.os.Build.VERSION_CODES.O||context.packageManager.canRequestPackageInstalls()
    fun unknownSourcesIntent(context:Context)=Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+context.packageName))
    fun install(context:Context,apk:File){
        val uri=FileProvider.getUriForFile(context,context.packageName+".updates",apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply{setDataAndType(uri,"application/vnd.android.package-archive");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)})
    }
}
