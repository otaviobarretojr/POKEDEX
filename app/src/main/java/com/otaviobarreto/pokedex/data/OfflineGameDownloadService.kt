package com.otaviobarreto.pokedex.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.otaviobarreto.pokedex.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class OfflineGameDownloadService : Service() {
    companion object {
        const val EXTRA_GAME = "game"
        private const val CHANNEL_ID = "offline_game_downloads"
        private const val NOTIFICATION_BASE = 6400
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()

    override fun onCreate() {
        super.onCreate()
        OfflineGamePackManager.initialize(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads offline",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val gameLabel = intent?.getStringExtra(EXTRA_GAME) ?: return START_NOT_STICKY
        if (jobs[gameLabel]?.isActive == true) return START_NOT_STICKY

        val notificationId = NOTIFICATION_BASE + kotlin.math.abs(gameLabel.hashCode() % 500)
        startForeground(notificationId, notification(gameLabel, 0, 0, "Preparando download…", true))
        OfflineGamePackManager.setRuntimeProgress(
            gameLabel,
            OfflineGamePackManager.Progress(0, 1, "Preparando download…"),
            true
        )

        val job = scope.launch {
            val game = AppGameCatalog.games.firstOrNull { it.label == gameLabel }
            if (game == null) {
                finish(gameLabel, notificationId, false)
                return@launch
            }
            val ok = runCatching {
                OfflineGamePackManager.download(game) { p ->
                    OfflineGamePackManager.setRuntimeProgress(gameLabel, p, true)
                    getSystemService(NotificationManager::class.java)
                        .notify(notificationId, notification(gameLabel, p.done, p.total, p.label, true))
                }
            }.isSuccess
            finish(gameLabel, notificationId, ok)
        }
        jobs[gameLabel] = job
        return START_NOT_STICKY
    }

    private fun finish(gameLabel: String, notificationId: Int, success: Boolean) {
        OfflineGamePackManager.setRuntimeProgress(gameLabel, null, false)
        jobs.remove(gameLabel)
        val manager = getSystemService(NotificationManager::class.java)
        val status = OfflineGamePackManager.status(gameLabel)
        val text = if (success && status.verified) "Pacote offline concluído" else "Download interrompido. Toque novamente para continuar."
        manager.notify(notificationId, notification(gameLabel, status.completeCount, status.pokemonCount, text, false))
        if (jobs.isEmpty()) stopSelf()
    }

    private fun notification(gameLabel: String, done: Int, total: Int, label: String, ongoing: Boolean) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("POKEDEX · $gameLabel")
            .setContentText(label)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setProgress(total.coerceAtLeast(0), done.coerceAtLeast(0), total <= 1)
            .build()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
