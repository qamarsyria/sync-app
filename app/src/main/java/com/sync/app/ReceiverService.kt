package com.sync.app

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import okhttp3.*
import java.util.concurrent.TimeUnit

class ReceiverService : Service() {

    private var ws: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private lateinit var db: DBHelper

    override fun onCreate() {
        super.onCreate()
        startForeground(2, buildForegroundNotification())
        db = DBHelper(this)
        connect()
    }

    private fun connect() {
        val topic = Prefs.myTopic(this) ?: return
        val req = Request.Builder()
            .url("wss://ntfy.sh/$topic/ws")
            .build()

        ws = client.newWebSocket(req, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val title = extractJson(text, "title") ?: "Sync"
                val body = extractJson(text, "message") ?: text
                // تخزين بصمت، بدون أي إشعار
                db.insert("incoming", title, body)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Handler(Looper.getMainLooper()).postDelayed({ connect() }, 10_000)
            }
        })
    }

    private fun extractJson(json: String, key: String): String? {
        val pattern = "\"$key\":\"(.*?)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun buildForegroundNotification(): Notification {
        val channelId = "sync_receiver"
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(
                channelId,
                "Sync",
                NotificationManager.IMPORTANCE_MIN
            )
            ch.setShowBadge(false)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sync")
            .setContentText("running")
            .setSmallIcon(R.drawable.ic_rose)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        ws?.close(1000, null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
