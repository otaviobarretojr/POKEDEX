package com.otaviobarreto.pokedex.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.otaviobarreto.pokedex.data.OfflineLibraryManager

@Composable
internal fun rememberOfflineArtworkModel(url:String?):Any? {
    val context=LocalContext.current.applicationContext
    return remember(url,context){
        url?.let{OfflineLibraryManager.resolveAny(context,it)} ?: url
    }
}
