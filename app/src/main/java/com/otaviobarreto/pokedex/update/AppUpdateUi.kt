package com.otaviobarreto.pokedex.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AutomaticUpdatePrompt(content: @Composable () -> Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var update by remember{mutableStateOf<AppUpdate?>(null)}
    var downloading by remember{mutableStateOf(false)}
    var downloadedApk by remember{mutableStateOf<File?>(null)}
    var error by remember{mutableStateOf<String?>(null)}
    val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){
        downloadedApk?.let{apk->if(AppUpdateManager.canInstallPackages(context))AppUpdateManager.install(context,apk)}
    }
    LaunchedEffect(Unit){update=AppUpdateManager.check()}
    content()
    update?.let{available->
        AlertDialog(
            onDismissRequest={if(!available.mandatory&&!downloading)update=null},
            title={Text("Nova atualização disponível")},
            text={Text("POKEDEX v"+available.versionName+"\n\n"+available.changelog+(if(downloading)"\n\nBaixando atualização…" else error?.let{"\n\n"+it}.orEmpty()))},
            confirmButton={TextButton(enabled=!downloading,onClick={
                downloading=true;error=null
                scope.launch{
                    runCatching{AppUpdateManager.download(context,available)}.onSuccess{apk->
                        downloadedApk=apk;downloading=false
                        if(AppUpdateManager.canInstallPackages(context))AppUpdateManager.install(context,apk)
                        else permissionLauncher.launch(AppUpdateManager.unknownSourcesIntent(context))
                    }.onFailure{downloading=false;error="Não foi possível baixar a atualização. Tente novamente."}
                }
            }){Text(if(downloading)"Baixando…" else "Atualizar agora")}},
            dismissButton={if(!available.mandatory){TextButton(enabled=!downloading,onClick={update=null}){Text("Depois")}}}
        )
    }
}

@Composable
fun ManualUpdateButton(onStatus:(String)->Unit){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var working by remember{mutableStateOf(false)}
    var downloadedApk by remember{mutableStateOf<File?>(null)}
    val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){
        downloadedApk?.let{apk->
            if(AppUpdateManager.canInstallPackages(context)) AppUpdateManager.install(context,apk)
            else onStatus("Autorize a instalação de apps desta fonte para concluir a atualização.")
        }
    }
    TextButton(enabled=!working,onClick={
        working=true
        onStatus("Verificando atualização…")
        scope.launch{
            val available=AppUpdateManager.check()
            if(available==null){
                working=false
                onStatus("Você já está na versão mais recente.")
                return@launch
            }
            onStatus("Baixando POKEDEX v"+available.versionName+"…")
            runCatching{AppUpdateManager.download(context,available)}
                .onSuccess{apk->
                    downloadedApk=apk
                    working=false
                    onStatus("Atualização pronta para instalar.")
                    if(AppUpdateManager.canInstallPackages(context)) AppUpdateManager.install(context,apk)
                    else permissionLauncher.launch(AppUpdateManager.unknownSourcesIntent(context))
                }
                .onFailure{error->
                    working=false
                    onStatus("Falha na atualização: "+(error.message?:"tente novamente."))
                }
        }
    }){Text(if(working)"Atualizando…" else "Verificar atualização")}
}
