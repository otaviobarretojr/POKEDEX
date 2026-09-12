package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.PokedexDataStore
import com.otaviobarreto.pokedex.data.PokeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonFormDetailScreen(
    formId:Int,
    formName:String,
    shiny:Boolean,
    onBack:()->Unit
){
    var data by remember(formId){mutableStateOf<PokeApiService.RemotePokemonDetail?>(null)}
    var error by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(formId){
        data=runCatching{
            withContext(Dispatchers.IO){PokedexDataStore.pokemon(formId)}
        }.onFailure{error=it.message}.getOrNull()
    }

    Scaffold(
        topBar={
            TopAppBar(
                title={Text(formName)},
                navigationIcon={
                    IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Voltar")}
                }
            )
        }
    ){padding->
        when{
            error!=null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment=Alignment.Center
            ){Text("Não foi possível carregar esta forma.")}
            data==null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment=Alignment.Center
            ){CircularProgressIndicator()}
            else -> {
                val p=data!!
                val imageUrl="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+
                    (if(shiny)"shiny/" else "")+formId+".png"
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding=PaddingValues(16.dp),
                    verticalArrangement=Arrangement.spacedBy(12.dp)
                ){
                    item{
                        Card(shape=RoundedCornerShape(24.dp)){
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment=Alignment.CenterHorizontally
                            ){
                                AsyncImage(
                                    model=imageUrl,
                                    contentDescription=formName,
                                    modifier=Modifier.fillMaxWidth().height(280.dp),
                                    contentScale=ContentScale.Fit
                                )
                                Text(
                                    formName,
                                    style=MaterialTheme.typography.headlineSmall,
                                    fontWeight=FontWeight.Black
                                )
                                Text(
                                    p.types.joinToString(" / "),
                                    color=MaterialTheme.colorScheme.primary,
                                    fontWeight=FontWeight.Bold
                                )
                            }
                        }
                    }
                    item{
                        Card(shape=RoundedCornerShape(20.dp)){
                            Column(Modifier.fillMaxWidth().padding(16.dp)){
                                Text("Status base",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                                StatRow("HP",p.stats.hp)
                                StatRow("Ataque",p.stats.attack)
                                StatRow("Defesa",p.stats.defense)
                                StatRow("Ataque Esp.",p.stats.specialAttack)
                                StatRow("Defesa Esp.",p.stats.specialDefense)
                                StatRow("Velocidade",p.stats.speed)
                                HorizontalDivider(Modifier.padding(vertical=8.dp))
                                Text(
                                    "Total: "+(
                                        p.stats.hp+p.stats.attack+p.stats.defense+
                                            p.stats.specialAttack+p.stats.specialDefense+p.stats.speed
                                    ),
                                    fontWeight=FontWeight.Black
                                )
                            }
                        }
                    }
                    item{
                        Card(shape=RoundedCornerShape(20.dp)){
                            Column(Modifier.fillMaxWidth().padding(16.dp)){
                                Text("Habilidades",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                                p.abilities.forEach{
                                    Text("• "+it,Modifier.padding(top=6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label:String,value:Int){
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp)){
        Text(label,Modifier.weight(1f))
        Text(value.toString(),fontWeight=FontWeight.Bold)
    }
}
