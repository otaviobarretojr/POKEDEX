package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.MoveMetadata
import com.otaviobarreto.pokedex.data.MoveMetadataService
import com.otaviobarreto.pokedex.data.PokeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class MoveView(
    val move:PokeApiService.RemoteMove,
    val details:List<PokeApiService.MoveLearnDetail>
)

@Composable
internal fun PokemonMovesTab(
    moves:List<PokeApiService.RemoteMove>,
    context:GameContext?,
    openRef:((String,String)->Unit)?
){
    var query by remember(moves,context){mutableStateOf("")}
    var methodFilter by remember(moves,context){mutableStateOf("Todos")}
    val base=remember(moves,context){
        if(context==null) moves.map{MoveView(it,it.learnDetails)}
        else moves.mapNotNull{move->
            move.learnDetails
                .filter{context.matchesVersionGroup(it.versionGroup)}
                .takeIf{it.isNotEmpty()}
                ?.let{MoveView(move,it)}
        }
    }
    val methods=remember(base){
        val preferred=listOf("Nível","TM","Ovo","Tutor")
        val found=base.flatMap{it.details}.map{moveMethodLabel(it.method)}.distinct()
        preferred.filter{it in found}+found.filterNot{it in preferred}.sorted()
    }
    val visible=remember(base,query,methodFilter){
        base.filter{view->
            (query.isBlank()||view.move.name.contains(query,true)) &&
                (methodFilter=="Todos"||view.details.any{moveMethodLabel(it.method)==methodFilter})
        }.sortedWith(
            compareBy<MoveView>{
                if(methodFilter=="Nível") it.details.filter{d->moveMethodLabel(d.method)=="Nível"}.map{d->d.level}.filter{l->l>0}.minOrNull() ?: Int.MAX_VALUE
                else 0
            }.thenBy{it.move.name}
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal=16.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp)
    ){
        item{
            Text(
                "Golpes",
                style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.Bold,
                modifier=Modifier.padding(top=14.dp)
            )
            Text(
                "Veja primeiro como o golpe é aprendido, seu tipo e sua categoria.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                modifier=Modifier.padding(top=2.dp,bottom=8.dp)
            )
            OutlinedTextField(
                value=query,
                onValueChange={query=it},
                modifier=Modifier.fillMaxWidth(),
                singleLine=true,
                leadingIcon={Icon(Icons.Default.Search,null)},
                label={Text("Buscar golpe")}
            )
            LazyRow(
                modifier=Modifier.padding(top=8.dp,bottom=2.dp),
                horizontalArrangement=Arrangement.spacedBy(6.dp)
            ){
                item{FilterChip(methodFilter=="Todos",{methodFilter="Todos"},{Text("Todos")})}
                items(methods,key={it}){label->
                    FilterChip(methodFilter==label,{methodFilter=label},{Text(label)})
                }
            }
        }

        items(visible,key={it.move.name}){view->
            MoveCompactRow(
                view=view,
                activeMethod=methodFilter,
                context=context,
                onClick=openRef?.let{{openRef("move",view.move.name)}}
            )
        }
        item{Spacer(Modifier.height(16.dp))}
    }
}

@Composable
private fun MoveCompactRow(
    view:MoveView,
    activeMethod:String,
    context:GameContext?,
    onClick:(()->Unit)?
){
    val metadata by produceState<MoveMetadata?>(
        initialValue=MoveMetadataService.cached(view.move.resourceUrl,context),
        key1=view.move.resourceUrl,
        key2=context?.label
    ){
        if(value==null && view.move.resourceUrl.isNotBlank()){
            value=runCatching{
                withContext(Dispatchers.IO){MoveMetadataService.load(view.move.resourceUrl,context)}
            }.getOrNull()
        }
    }
    val relevant=remember(view.details,activeMethod){
        if(activeMethod=="Todos") view.details else view.details.filter{moveMethodLabel(it.method)==activeMethod}
    }
    val methodLabel=remember(relevant,metadata,activeMethod){primaryLearnLabel(relevant,metadata,activeMethod)}
    val modifier=Modifier.fillMaxWidth().then(if(onClick!=null)Modifier.clickable(onClick=onClick)else Modifier)

    Surface(
        modifier=modifier,
        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Sm),
        color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.58f)
    ){
        Row(
            Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=9.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            LearnMethodPill(methodLabel)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)){
                Text(
                    view.move.name,
                    style=MaterialTheme.typography.bodyMedium,
                    fontWeight=FontWeight.ExtraBold,
                    maxLines=1,
                    overflow=TextOverflow.Ellipsis
                )
                Row(
                    modifier=Modifier.padding(top=4.dp),
                    horizontalArrangement=Arrangement.spacedBy(5.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    metadata?.let{
                        TypePill(it.type)
                        DamageClassPill(it.category)
                    } ?: Text(
                        "Carregando dados…",
                        style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if(onClick!=null) Icon(
                Icons.Default.ChevronRight,
                contentDescription="Abrir detalhes do golpe",
                tint=MaterialTheme.colorScheme.onSurfaceVariant,
                modifier=Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LearnMethodPill(label:String){
    Surface(
        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Pill),
        color=MaterialTheme.colorScheme.primaryContainer
    ){
        Text(
            label,
            modifier=Modifier.padding(horizontal=8.dp,vertical=5.dp),
            style=MaterialTheme.typography.labelSmall,
            fontWeight=FontWeight.Black,
            color=MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines=1
        )
    }
}

@Composable
private fun TypePill(type:String){
    val color=PokedexDesignTokens.Colors.type(type)
    Surface(shape=RoundedCornerShape(PokedexDesignTokens.Radius.Pill),color=color){
        Text(
            type.uppercase(),
            modifier=Modifier.padding(horizontal=7.dp,vertical=3.dp),
            style=MaterialTheme.typography.labelSmall,
            fontWeight=FontWeight.Black,
            color=Color.White,
            maxLines=1
        )
    }
}

@Composable
private fun DamageClassPill(category:String){
    val normalized=category.lowercase()
    val label=when(normalized){
        "physical"->"Físico"
        "special"->"Especial"
        "status"->"Status"
        else->category
    }
    val icon=when(normalized){
        "physical"->Icons.Default.Bolt
        "special"->Icons.Default.AutoAwesome
        else->Icons.Default.Tune
    }
    Surface(
        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Pill),
        color=MaterialTheme.colorScheme.surface
    ){
        Row(
            Modifier.padding(horizontal=6.dp,vertical=3.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(3.dp)
        ){
            Icon(icon,null,Modifier.size(11.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun primaryLearnLabel(
    details:List<PokeApiService.MoveLearnDetail>,
    metadata:MoveMetadata?,
    activeMethod:String
):String{
    val selected=if(activeMethod=="Todos"){
        details.sortedWith(compareBy<PokeApiService.MoveLearnDetail>{
            when(moveMethodLabel(it.method)){"Nível"->0;"TM"->1;"Ovo"->2;"Tutor"->3;else->4}
        }.thenBy{it.level}).firstOrNull()
    }else details.firstOrNull()
    val method=selected?.let{moveMethodLabel(it.method)} ?: activeMethod
    return when(method){
        "Nível" -> selected?.level?.takeIf{it>0}?.let{"Nv. $it"} ?: "Nível"
        "TM" -> metadata?.machineLabel ?: "TM"
        "Ovo" -> "Ovo"
        "Tutor" -> "Tutor"
        else -> method.ifBlank{"Golpe"}
    }
}

private fun moveMethodLabel(method:String):String=when(method.lowercase()){
    "level up"->"Nível"
    "machine"->"TM"
    "egg"->"Ovo"
    "tutor"->"Tutor"
    else->method
}
