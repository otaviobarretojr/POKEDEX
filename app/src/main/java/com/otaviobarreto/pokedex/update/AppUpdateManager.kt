package com.otaviobarreto.pokedex.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class AppUpdate(
    val versionCode:Int,
    val versionName:String,
    val apkUrl:String,
    val changelog:String,
    val mandatory:Boolean=false,
    val sha256:String?=null
)

object AppUpdateManager {
    private const val MANIFEST_URL="https://raw.githubusercontent.com/otaviobarretojr/POKEDEX/main/update.json"
    private val client=OkHttpClient()

    suspend fun check():AppUpdate?=withContext(Dispatchers.IO){
        runCatching{
            val request=Request.Builder()
                .url(MANIFEST_URL)
                .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                .build()
            val body=client.newCall(request).execute().use{response->
                check(response.isSuccessful){"HTTP "+response.code}
                response.body?.string()?:error("Manifesto vazio")
            }
            val json=JSONObject(body)
            val apkUrl=json.getString("apkUrl")
            check(Uri.parse(apkUrl).scheme.equals("https",true)){"URL de atualização insegura"}
            AppUpdate(
                versionCode=json.getInt("versionCode"),
                versionName=json.getString("versionName"),
                apkUrl=apkUrl,
                changelog=json.optString("changelog","Melhorias e correções."),
                mandatory=json.optBoolean("mandatory",false),
                sha256=json.optString("sha256").trim().takeIf{it.isNotBlank()}
            ).takeIf{it.versionCode>installedVersionCode()}
        }.getOrNull()
    }

    private fun installedVersionCode():Int = com.otaviobarreto.pokedex.BuildConfig.VERSION_CODE

    suspend fun download(context:Context,update:AppUpdate):File=withContext(Dispatchers.IO){
        val request=Request.Builder().url(update.apkUrl).build()
        val targetDir=File(context.cacheDir,"updates").apply{mkdirs()}
        val target=File(targetDir,"pokedex-"+update.versionName+".apk")
        runCatching{
            client.newCall(request).execute().use{response->
                check(response.isSuccessful){"Falha no download: HTTP "+response.code}
                val body=response.body?:error("APK vazio")
                target.outputStream().use{output->body.byteStream().use{it.copyTo(output)}}
            }
            check(target.length()>0L){"APK inválido"}
            verifyDownloadedApk(context,target,update)
            target
        }.getOrElse{error->
            runCatching{target.delete()}
            throw error
        }
    }

    private fun verifyDownloadedApk(context:Context,apk:File,update:AppUpdate){
        update.sha256?.let{expected->
            check(fileSha256(apk).equals(expected,true)){"SHA-256 do APK não confere"}
        }

        val pm=context.packageManager
        val candidate=archivePackageInfo(pm,apk) ?: error("APK de atualização inválido")
        check(candidate.packageName==context.packageName){"Pacote de atualização incorreto"}
        check(versionCode(candidate)==update.versionCode.toLong()){"Versão do APK não confere com o manifesto"}
        check(update.versionCode>installedVersionCode()){"A atualização não é mais recente"}

        val installed=installedPackageInfo(pm,context.packageName)
        val installedCerts=certDigests(installed)
        val candidateCerts=certDigests(candidate)
        check(installedCerts.isNotEmpty() && candidateCerts.isNotEmpty()){"Não foi possível validar a assinatura"}
        check(installedCerts.intersect(candidateCerts).isNotEmpty()){"Assinatura do APK não corresponde ao aplicativo instalado"}
    }

    @Suppress("DEPRECATION")
    private fun archivePackageInfo(pm:PackageManager,apk:File):PackageInfo? {
        val flags=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P)
            PackageManager.GET_SIGNING_CERTIFICATES
        else
            PackageManager.GET_SIGNATURES
        return pm.getPackageArchiveInfo(apk.absolutePath,flags)
    }

    @Suppress("DEPRECATION")
    private fun installedPackageInfo(pm:PackageManager,packageName:String):PackageInfo {
        val flags=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P)
            PackageManager.GET_SIGNING_CERTIFICATES
        else
            PackageManager.GET_SIGNATURES
        return pm.getPackageInfo(packageName,flags)
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info:PackageInfo):Long =
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun certDigests(info:PackageInfo):Set<String> {
        val signatures=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            info.signatures?.toList().orEmpty()
        }
        return signatures.mapTo(linkedSetOf()){signature->
            sha256(signature.toByteArray())
        }
    }

    private fun fileSha256(file:File):String {
        val digest=MessageDigest.getInstance("SHA-256")
        file.inputStream().use{input->
            val buffer=ByteArray(DEFAULT_BUFFER_SIZE)
            while(true){
                val read=input.read(buffer)
                if(read<=0)break
                digest.update(buffer,0,read)
            }
        }
        return digest.digest().joinToString(""){"%02x".format(it)}
    }

    private fun sha256(bytes:ByteArray):String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(""){"%02x".format(it)}

    fun canInstallPackages(context:Context):Boolean=
        Build.VERSION.SDK_INT<Build.VERSION_CODES.O||context.packageManager.canRequestPackageInstalls()

    fun unknownSourcesIntent(context:Context)=
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+context.packageName))

    fun install(context:Context,apk:File){
        val uri=FileProvider.getUriForFile(context,context.packageName+".updates",apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply{
                setDataAndType(uri,"application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
