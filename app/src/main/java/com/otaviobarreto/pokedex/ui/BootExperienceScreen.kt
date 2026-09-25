package com.otaviobarreto.pokedex.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class BootState(val progress: Float, val label: String)

@Composable
fun BootExperienceScreen(onReady: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
        }.getOrDefault("—")
    }
    var state by remember { mutableStateOf(BootState(.04f, "Preparando sua Pokédex")) }
    var finished by remember { mutableStateOf(false) }
    var bootstrapError by remember { mutableStateOf<String?>(null) }
    var retryToken by remember { mutableIntStateOf(0) }
    val animatedProgress by animateFloatAsState(state.progress, tween(420), label = "bootProgress")
    val infinite = rememberInfiniteTransition(label = "bootMotion")
    val rotation by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "orbit")
    val pulse by infinite.animateFloat(.96f, 1.04f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val glow by infinite.animateFloat(.18f, .42f, infiniteRepeatable(tween(1900), RepeatMode.Reverse), label = "glow")

    LaunchedEffect(retryToken) {
        bootstrapError=null
        state=BootState(.01f,"Preparando biblioteca POKEDEX")
        val result=ContentBootstrapManager.ensureReady(context){progress->
            state=BootState((progress.fraction*.72f).coerceIn(.01f,.72f),progress.label)
        }
        if(result.ready){
            state=BootState(1f,"Tudo pronto")
            onReady()
            ArtworkOfflineSync.launch(context)
            StartupPreloader.launchWarmInBackground(context)
        }else{
            bootstrapError=result.error ?: "Não foi possível preparar a biblioteca."
            state=BootState(state.progress,"Download interrompido")
        }
    }

    val scheme = MaterialTheme.colorScheme
    val mint = scheme.background
    val teal = scheme.tertiary
    val green = scheme.primary
    Box(Modifier.fillMaxSize().background(mint)) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * .5f, size.height * .43f)
            drawCircle(scheme.surface.copy(alpha=.58f), size.minDimension*.46f, center)
            drawCircle(teal.copy(alpha=.08f), size.minDimension*.34f, center)
            val radii = listOf(.31f,.43f,.57f)
            radii.forEachIndexed { index, r ->
                drawArc(
                    color = if(index%2==0) teal.copy(alpha=.22f) else green.copy(alpha=.18f),
                    startAngle = rotation + index*71f,
                    sweepAngle = 245f,
                    useCenter = false,
                    topLeft = Offset(center.x-size.minDimension*r, center.y-size.minDimension*r*.58f),
                    size = Size(size.minDimension*r*2, size.minDimension*r*1.16f),
                    style = Stroke(width=2.2f, cap=StrokeCap.Round)
                )
            }
            repeat(14) { i ->
                val x = size.width * (((i*37)%100)/100f)
                val y = size.height * (.10f + (((i*53)%78)/100f))
                val s = 5f + (i%4)*4f
                drawCircle(if(i%2==0) teal.copy(alpha=.13f) else green.copy(alpha=.16f), s, Offset(x,y))
            }
        }

        Column(Modifier.fillMaxSize().padding(horizontal=30.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(.22f))
            Box(Modifier.size(224.dp), contentAlignment=Alignment.Center) {
                Surface(Modifier.size(148.dp).scale(pulse), shape=CircleShape, color=scheme.surface.copy(alpha=.96f), shadowElevation=10.dp) {
                    Box(contentAlignment=Alignment.Center) { Icon(Icons.Default.CatchingPokemon, null, Modifier.size(94.dp), tint=teal) }
                }
                Surface(Modifier.align(Alignment.BottomCenter).offset(y=10.dp), shape=RoundedCornerShape(20.dp), color=teal.copy(alpha=.94f)) {
                    Text("POKEDEX", Modifier.padding(horizontal=20.dp,vertical=7.dp), color=Color.White, fontWeight=FontWeight.Black, letterSpacing=2.sp)
                }
            }
            Spacer(Modifier.height(42.dp))
            Text("SUA JORNADA, ORGANIZADA.", fontWeight=FontWeight.Bold, color=teal, letterSpacing=1.4.sp, fontSize=13.sp)
            Spacer(Modifier.weight(.28f))
            Text(state.label, style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.SemiBold, color=scheme.onBackground, textAlign=TextAlign.Center)
            Spacer(Modifier.height(13.dp))
            LinearProgressIndicator(progress={animatedProgress}, Modifier.fillMaxWidth().height(6.dp), color=teal, trackColor=teal.copy(alpha=.12f), strokeCap=StrokeCap.Round)
            Spacer(Modifier.height(9.dp))
            Text("${(animatedProgress*100).toInt().coerceIn(0,100)}%", style=MaterialTheme.typography.labelMedium, color=teal.copy(alpha=.75f))
            bootstrapError?.let{error->
                Spacer(Modifier.height(18.dp))
                Text(error,style=MaterialTheme.typography.bodySmall,color=scheme.error,textAlign=TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Button(onClick={retryToken++}){Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(8.dp));Text("Tentar novamente")}
            }
            Spacer(Modifier.height(38.dp))
            Text("POKEDEX  ·  v"+versionName, style=MaterialTheme.typography.labelSmall, color=scheme.onSurfaceVariant.copy(alpha=.72f), letterSpacing=1.sp)
            Spacer(Modifier.height(24.dp))
        }
        if(finished) Box(Modifier.fillMaxSize().background(scheme.surface.copy(alpha=glow*.15f)))
    }
}
