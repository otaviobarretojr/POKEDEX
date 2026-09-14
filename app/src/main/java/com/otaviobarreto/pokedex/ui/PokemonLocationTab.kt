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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun V2Locations(
    pokemonId:Int,
    encounters:List<PokeApiService.EncounterLocation>,
    context:GameContext?,
    source:String?
){
    if(context==null){
        LegacyLocationList(encounters)
        return
    }

    var loadError by remember(pokemonId,context){mutableStateOf(false)}
    var canonical by remember(pokemonId,context,encounters){mutableStateOf<CanonicalAvailability?>(null)}

    LaunchedEffect(pokemonId,context,encounters){
        loadError=false
        canonical=null
        val resolved=runCatching{
            withContext(Dispatchers.IO){
                val dex=GameDexService.cached(context) ?: GameDexService.loadGameDex(context)
                val species=PokedexDataStore.species(pokemonId)
                val chain=species.evolutionChainUrl?.let{PokedexDataStore.evolutions(it,context)}.orEmpty()
                CanonicalAvailabilityResolver.resolve(
                    pokemonId=pokemonId,
                    context=context,
                    encounters=encounters,
                    dex=dex,
                    evolutionChain=chain
                )
            }
        }
        loadError=resolved.isFailure
        canonical=resolved.getOrNull()
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(PokedexDesignTokens.Spacing.Lg),
        verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)
    ){
        item{
            Text("Localização",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            Text(
                context.label+" · "+context.regionLabel,
                style=MaterialTheme.typography.labelMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                modifier=Modifier.padding(top=2.dp)
            )
        }

        when{
            canonical!=null -> {
                canonical!!.version?.let{availability->item{VersionAvailabilityCard(availability)}}
                item{AcquisitionCard(canonical!!)}
                if(canonical!!.locations.isNotEmpty()){
                    item{
                        Text(
                            "Onde encontrar",
                            style=MaterialTheme.typography.titleMedium,
                            fontWeight=FontWeight.Black,
                            modifier=Modifier.padding(top=4.dp)
                        )
                        Text(
                            canonical!!.locations.size.toString()+" local"+if(canonical!!.locations.size==1)"" else "izações",
                            style=MaterialTheme.typography.labelMedium,
                            color=MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(canonical!!.locations,key={it}){location->LocationRow(location)}
                }else if(canonical!!.inRegionalDex){
                    item{
                        InfoSurface(
                            if(canonical!!.confidence==AvailabilityConfidence.PARTIAL)
                                "Não há encontro selvagem confirmado nesta fonte para esta espécie. O método de obtenção acima é a referência principal."
                            else
                                "Esta espécie não exige uma localização selvagem para o método de obtenção informado."
                        )
                    }
                }
            }
            loadError -> item{
                InfoSurface("Não foi possível consolidar os dados agora. Os dados locais de encontro continuam preservados e serão usados quando a fonte voltar a responder.")
            }
            else -> item{
                Row(
                    Modifier.fillMaxWidth().padding(vertical=18.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.Center
                ){
                    CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)
                    Text("Consolidando disponibilidade…",Modifier.padding(start=10.dp),style=MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item{Spacer(Modifier.height(18.dp))}
    }
}

@Composable
private fun AcquisitionCard(record:CanonicalAvailability){
    val unavailable=record.acquisitionKind==CanonicalAcquisitionKind.UNAVAILABLE
    Surface(
        Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
        color=if(unavailable)MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.72f)
    ){
        Column(Modifier.padding(16.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Icon(
                    acquisitionIcon(record.acquisitionKind),
                    contentDescription=null,
                    tint=if(unavailable)MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                    modifier=Modifier.size(22.dp)
                )
                Column(Modifier.padding(start=10.dp).weight(1f)){
                    Text("Como obter",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(record.acquisitionLabel,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
                }
                if(record.regionalNumber!=null){
                    Text("#"+record.regionalNumber.toString().padStart(3,'0'),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
                }
            }
            record.requirement?.takeIf{it.isNotBlank()}?.let{
                Text(it,Modifier.padding(top=8.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(record.confidence==AvailabilityConfidence.PARTIAL){
                AssistChip(
                    onClick={},
                    label={Text("Cobertura complementar")},
                    leadingIcon={Icon(Icons.Default.Info,null,Modifier.size(16.dp))},
                    modifier=Modifier.padding(top=8.dp)
                )
            }
        }
    }
}

@Composable
private fun VersionAvailabilityCard(a:VersionAvailability){
    if(a.kind==VersionAvailabilityKind.SHARED){
        Surface(shape=RoundedCornerShape(999.dp),color=MaterialTheme.colorScheme.surfaceVariant,modifier=Modifier.wrapContentWidth()){
            Row(Modifier.padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){
                Icon(Icons.Default.CheckCircle,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.primary)
                Text(a.versionA+" + "+a.versionB,Modifier.padding(start=6.dp),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
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

@Composable
private fun LocationRow(location:String){
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f)){
        Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){
            Icon(Icons.Default.LocationOn,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(20.dp))
            Text(location,modifier=Modifier.padding(start=10.dp).weight(1f),style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InfoSurface(message:String){
    DexGlassSurface(Modifier.fillMaxWidth()){
        Text(message,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegacyLocationList(encounters:List<PokeApiService.EncounterLocation>){
    val names=encounters.map{LocationIntelligence.displayName(it.location)}.distinct().sorted()
    LazyColumn(Modifier.fillMaxSize().padding(PokedexDesignTokens.Spacing.Lg),verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)){
        item{Text("Localização",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
        if(names.isEmpty()) item{InfoSurface("Nenhum encontro registrado.")} else items(names,key={it}){LocationRow(it)}
        item{Spacer(Modifier.height(18.dp))}
    }
}

private fun acquisitionIcon(kind:CanonicalAcquisitionKind)=when(kind){
    CanonicalAcquisitionKind.WILD -> Icons.Default.LocationOn
    CanonicalAcquisitionKind.EVOLUTION -> Icons.Default.AccountTree
    CanonicalAcquisitionKind.TRADE -> Icons.Default.SwapHoriz
    CanonicalAcquisitionKind.GIFT_STARTER -> Icons.Default.CardGiftcard
    CanonicalAcquisitionKind.RAID -> Icons.Default.Bolt
    CanonicalAcquisitionKind.EVENT_SPECIAL -> Icons.Default.AutoAwesome
    CanonicalAcquisitionKind.HOME_TRANSFER -> Icons.Default.Cloud
    CanonicalAcquisitionKind.UNAVAILABLE -> Icons.Default.Block
}
