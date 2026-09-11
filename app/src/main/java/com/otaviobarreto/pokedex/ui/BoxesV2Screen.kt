package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import com.otaviobarreto.pokedex.data.PokedexDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val QBbg=Color(0xFFFBF8FF);private val QBsurface=Color(0xFFF1EEFA);private val QBink=Color(0xFF171522);private val QBmuted=Color(0xFF777286);private val QBmint=Color(0xFF67D7CB)
private data class QBRegion(val label:String,val source:String,val badge:String)
private data class QBGame(val label:String,val accent:Color,val regions:List<QBRegion>)
private fun qbAccent(game:String):Color=when(game){
 "Scarlet / Violet"->Color(0xFFB54C5D)
 "Sword / Shield"->Color(0xFF35A9C7)
 "Let's Go Pikachu / Eevee"->Color(0xFFE0A929)
 "Legends Arceus"->Color(0xFF527F7C)
 "Brilliant Diamond / Shining Pearl"->Color(0xFF5968C7)
 "Pokémon Legends: Z-A"->Color(0xFF2D7F8E)
 "FireRed / LeafGreen"->Color(0xFFCC5B43)
 "Pokémon Champions"->Color(0xFF7857D8)
 else->Color(0xFF5B55E7)
}
private val qbGames=AppGameCatalog.games.map{game->
 QBGame(game.label,qbAccent(game.label),game.regions.map{QBRegion(it.label,it.source,it.subtitle)})
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BoxesV2Screen(onPokemonClick:(Int,String?)->Unit){
 var gameLabel by rememberSaveable{mutableStateOf(qbGames.first().label)}
 val game=remember(gameLabel){qbGames.firstOrNull{it.label==gameLabel}?:qbGames.first()}
 var regionSource by rememberSaveable{mutableStateOf(game.regions.first().source)}
 val region=remember(game.label,regionSource){game.regions.firstOrNull{it.source==regionSource}?:game.regions.first()}
 var dex by remember{mutableStateOf<List<GameDexService.GameDexEntry>>(emptyList())};var loading by remember{mutableStateOf(true)};var page by rememberSaveable{mutableIntStateOf(0)};var gameMenu by remember{mutableStateOf(false)};var regionMenu by remember{mutableStateOf(false)};var search by remember{mutableStateOf(false)}
 LaunchedEffect(region.source,game.label){
  loading=true
  val ctx=GameContext.fromSource(region.source)
  dex=if(ctx==null)emptyList()else runCatching{withContext(Dispatchers.IO){GameDexService.loadGameDex(ctx)}}.getOrElse{emptyList()}
  val pageCount=((dex.size+29)/30).coerceAtLeast(1)
  if(page>=pageCount) page=pageCount-1
  loading=false
 }
 val pages=((dex.size+29)/30).coerceAtLeast(1);val current=page.coerceIn(0,pages-1);val entries=dex.drop(current*30).take(30);LaunchedEffect(entries){entries.take(12).forEach{PokedexDataStore.prefetchDetails(it.nationalId)};delay(350);entries.drop(12).forEach{PokedexDataStore.prefetchDetails(it.nationalId)}};val capturedIds=CollectionStore.capturedIds;val caught=dex.count{it.nationalId in capturedIds};val progress=if(dex.isEmpty())0f else caught.toFloat()/dex.size
 Column(Modifier.fillMaxSize().background(QBbg).padding(horizontal=6.dp)){
  Row(Modifier.fillMaxWidth().padding(top=2.dp,bottom=2.dp),verticalAlignment=Alignment.CenterVertically){
   Column(Modifier.weight(1f)){Text("BOX",fontSize=19.sp,lineHeight=19.sp,fontWeight=FontWeight.Black,color=QBink);Text(game.label,fontSize=9.sp,color=QBmuted,maxLines=1,overflow=TextOverflow.Ellipsis)}
   Surface(shape=RoundedCornerShape(10.dp),color=game.accent.copy(alpha=.10f)){Text(region.label,Modifier.padding(horizontal=9.dp,vertical=4.dp),fontSize=10.sp,fontWeight=FontWeight.Bold,color=game.accent)}
  }
  Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
   ExposedDropdownMenuBox(gameMenu,{gameMenu=!gameMenu},Modifier.weight(1.12f)){OutlinedTextField(game.label,{},Modifier.menuAnchor().fillMaxWidth().heightIn(min=46.dp),readOnly=true,singleLine=true,label={Text("Jogo",fontSize=10.sp)},textStyle=MaterialTheme.typography.bodySmall,trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(gameMenu)},shape=RoundedCornerShape(13.dp));ExposedDropdownMenu(gameMenu,{gameMenu=false}){qbGames.forEach{g->DropdownMenuItem({Text(g.label,fontWeight=FontWeight.SemiBold)},{gameLabel=g.label;regionSource=g.regions.first().source;page=0;gameMenu=false})}}}
   ExposedDropdownMenuBox(regionMenu,{regionMenu=!regionMenu},Modifier.weight(.88f)){OutlinedTextField(region.label,{},Modifier.menuAnchor().fillMaxWidth().heightIn(min=46.dp),readOnly=true,singleLine=true,label={Text(if(game.regions.size>1)"Região / DLC" else "Região",fontSize=10.sp)},textStyle=MaterialTheme.typography.bodySmall,trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(regionMenu)},shape=RoundedCornerShape(13.dp));ExposedDropdownMenu(regionMenu,{regionMenu=false}){game.regions.forEach{r->DropdownMenuItem({Column{Text(r.label,fontWeight=FontWeight.SemiBold);if(r.badge.isNotBlank())Text(r.badge,fontSize=10.sp,color=QBmuted)}},{regionSource=r.source;page=0;regionMenu=false})}}}
  }
  Row(Modifier.fillMaxWidth().height(40.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
   FilledTonalIconButton({page=current-1},enabled=current>0,modifier=Modifier.size(36.dp)){Icon(Icons.Default.ChevronLeft,"Anterior")}
   Column(horizontalAlignment=Alignment.CenterHorizontally){Text("Box "+(current+1)+" / "+pages,fontSize=14.sp,fontWeight=FontWeight.Black,color=QBink);Text(caught.toString()+" / "+dex.size+" capturados",fontSize=9.sp,color=game.accent)}
   FilledTonalIconButton({page=current+1},enabled=current<pages-1,modifier=Modifier.size(36.dp)){Icon(Icons.Default.ChevronRight,"Próxima")}
  }
  Box(Modifier.weight(1f).fillMaxWidth()){
   when{
    loading->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=game.accent)}
    dex.isEmpty()->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Não foi possível carregar esta Pokédex regional.")}
    else->QBGrid(entries,capturedIds){pk->onPokemonClick(pk.nationalId,region.source)}
   }
  }
  FilledTonalButton({search=true},Modifier.fillMaxWidth().height(40.dp).padding(bottom=1.dp),shape=RoundedCornerShape(13.dp)){Icon(Icons.Default.Search,null,Modifier.size(17.dp));Spacer(Modifier.width(6.dp));Text("Pesquisar Pokémon",fontWeight=FontWeight.Bold,fontSize=12.sp)}
 }
 if(search)QBSearch(dex,CollectionStore.capturedIds,{search=false},{pk->val i=dex.indexOfFirst{it.nationalId==pk.nationalId};if(i>=0)page=i/30;search=false},{pk->search=false;onPokemonClick(pk.nationalId,region.source)})
}

@Composable
private fun QBGrid(
    entries:List<GameDexService.GameDexEntry>,
    captured:Set<Int>,
    open:(GameDexService.GameDexEntry)->Unit
){
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(2.dp)){
        repeat(5){row->
            Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)){
                repeat(6){col->
                    val pk=entries.getOrNull(row*6+col)
                    if(pk==null){
                        Surface(Modifier.weight(1f).fillMaxHeight(),RoundedCornerShape(9.dp),color=QBsurface){}
                    }else{
                        QBSlot(
                            pk=pk,
                            captured=pk.nationalId in captured,
                            open={open(pk)},
                            modifier=Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QBSlot(
    pk:GameDexService.GameDexEntry,
    captured:Boolean,
    open:()->Unit,
    modifier:Modifier=Modifier
){
    Surface(
        modifier.clickable(onClick=open),
        shape=RoundedCornerShape(9.dp),
        color=if(captured)Color(0xFFEAE6FA)else QBsurface
    ){
        Box(Modifier.fillMaxSize()){
            AsyncImage(
                model=pk.spriteUrl,
                contentDescription=pk.name,
                modifier=Modifier.fillMaxSize().padding(horizontal=1.dp,vertical=8.dp).alpha(if(captured)1f else .22f),
                colorFilter=if(captured)null else ColorFilter.colorMatrix(ColorMatrix().apply{setToSaturation(0f)})
            )
            Surface(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color=Color.White.copy(alpha=.84f)
            ){
                Column(
                    Modifier.padding(vertical=2.dp,horizontal=1.dp),
                    horizontalAlignment=Alignment.CenterHorizontally
                ){
                    Text(
                        pretty(pk.name),
                        fontSize=7.5.sp,
                        lineHeight=8.sp,
                        maxLines=1,
                        overflow=TextOverflow.Ellipsis,
                        color=if(captured)QBink else QBmuted
                    )
                    Text(
                        "#"+pk.gameNumber.toString().padStart(3,'0'),
                        fontSize=7.sp,
                        lineHeight=7.sp,
                        color=QBmuted
                    )
                }
            }
        }
    }
}

@Composable
private fun QBSearch(
    dex:List<GameDexService.GameDexEntry>,
    captured:Set<Int>,
    dismiss:()->Unit,
    select:(GameDexService.GameDexEntry)->Unit,
    open:(GameDexService.GameDexEntry)->Unit
){
    var q by remember{mutableStateOf("")}
    val key=q.trim().removePrefix("#")
    val results=if(key.isBlank()) emptyList() else dex.filter{
        it.name.contains(key,true)||it.gameNumber.toString()==key||it.nationalId.toString()==key
    }.take(10)

    AlertDialog(
        onDismissRequest=dismiss,
        title={Text("Pesquisar Pokémon")},
        text={Column{
            OutlinedTextField(
                q,{q=it},Modifier.fillMaxWidth(),singleLine=true,
                leadingIcon={Icon(Icons.Default.Search,null)},
                placeholder={Text("Nome ou número")}
            )
            Spacer(Modifier.height(8.dp))
            results.forEach{pk->
                Row(
                    Modifier.fillMaxWidth().clickable{select(pk)}.padding(vertical=5.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    AsyncImage(pk.spriteUrl,pk.name,Modifier.size(48.dp))
                    Column(Modifier.weight(1f)){
                        Text(pretty(pk.name),fontWeight=FontWeight.SemiBold)
                        Text(if(pk.nationalId in captured)"Capturado" else "Faltando",fontSize=11.sp,color=QBmuted)
                    }
                    TextButton({open(pk)}){Text("Ficha")}
                }
            }
        }},
        confirmButton={},
        dismissButton={TextButton(dismiss){Text("Fechar")}}
    )
}

private fun pretty(name:String)=name.split("-"," ").joinToString(" "){
    it.replaceFirstChar{c->if(c.isLowerCase())c.titlecase()else c.toString()}
}
