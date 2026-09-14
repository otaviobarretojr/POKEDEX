package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*

@Composable
internal fun V2Locations(
    pokemonId:Int,
    encounters:List<PokeApiService.EncounterLocation>,
    context:GameContext?,
    source:String?
){
    val visible=remember(encounters,context){LocationIntelligence.filter(encounters,context)}
    val availability=remember(pokemonId,context){VersionAvailabilityCatalog.forPokemon(pokemonId,context)}
    LazyColumn(
        Modifier.fillMaxSize().padding(PokedexDesignTokens.Spacing.Lg),
        verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)
    ){
        item{
            Text("Localização",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            Text(
                context?.let{it.label+" · "+it.regionLabel} ?: "Encontros registrados",
                style=MaterialTheme.typography.labelMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                modifier=Modifier.padding(top=2.dp)
            )
        }
        availability?.let{item{VersionAvailabilityCard(it)}}
        if(visible.isEmpty()){
            item{
                DexGlassSurface(Modifier.fillMaxWidth()){
                    Text(
                        LocationIntelligence.emptyMessage(context,encounters.isNotEmpty()),
                        style=MaterialTheme.typography.bodyMedium,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }else{
            item{
                Text(
                    visible.size.toString()+" local"+if(visible.size==1)"" else "izações",
                    style=MaterialTheme.typography.labelMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(visible,key={it.location}){e->
                Surface(
                    Modifier.fillMaxWidth(),
                    shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),
                    color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f)
                ){
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=13.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        Icon(Icons.Default.LocationOn,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(20.dp))
                        Text(
                            LocationIntelligence.displayName(e.location),
                            modifier=Modifier.padding(start=10.dp).weight(1f),
                            style=MaterialTheme.typography.bodyLarge,
                            fontWeight=FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        item{Spacer(Modifier.height(18.dp))}
    }
}

@Composable
private fun VersionAvailabilityCard(a:VersionAvailability){
    if(a.kind==VersionAvailabilityKind.SHARED){
        Surface(
            shape=RoundedCornerShape(999.dp),
            color=MaterialTheme.colorScheme.surfaceVariant,
            modifier=Modifier.wrapContentWidth()
        ){
            Row(Modifier.padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){
                Icon(Icons.Default.CheckCircle,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.primary)
                Text(
                    a.versionA+" + "+a.versionB,
                    Modifier.padding(start=6.dp),
                    style=MaterialTheme.typography.labelLarge,
                    fontWeight=FontWeight.Bold
                )
            }
        }
        return
    }
    val special=a.kind==VersionAvailabilityKind.SPLIT_FORMS
    val unavailable=a.kind==VersionAvailabilityKind.UNAVAILABLE
    val container=when{special->MaterialTheme.colorScheme.tertiaryContainer;unavailable->MaterialTheme.colorScheme.errorContainer;else->MaterialTheme.colorScheme.primaryContainer}
    val content=when{special->MaterialTheme.colorScheme.onTertiaryContainer;unavailable->MaterialTheme.colorScheme.onErrorContainer;else->MaterialTheme.colorScheme.onPrimaryContainer}
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),color=container){
        Column(Modifier.padding(horizontal=16.dp,vertical=14.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Icon(if(special)Icons.Default.CompareArrows else if(unavailable)Icons.Default.Block else Icons.Default.Lock,null,tint=content,modifier=Modifier.size(20.dp))
                Text(a.title,Modifier.padding(start=8.dp),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black,color=content)
            }
            Text(a.subtitle,Modifier.padding(top=5.dp),style=MaterialTheme.typography.bodySmall,color=content.copy(alpha=.86f))
        }
    }
}
