package com.sync.app

import android.app.*
import android.content.*
import android.net.*
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class NetService : Service() {

    private lateinit var cm: ConnectivityManager
    private var lastNetState: Boolean? = null
    private var lastBatteryLevel: Int = -1
    private var lastCharging: Boolean = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

            if (level < 0) return

            val percent = (level * 100) / scale
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            // نرسل فقط عند التغيرات المهمة
            if (percent != lastBatteryLevel) {
                // تنبيهات على مستويات محددة
                if (percent == 20 || percent == 15 || percent == 10 || percent == 5) {
                    Sender.sendBattery(this@NetService, percent, charging)
                }
                // تنبيه عند اكتمال الشحن
                if (percent == 100 && charging && !lastCharging) {
                    Sender.sendBattery(this@NetService, percent, charging)
                }
                lastBatteryLevel = percent
            }

            if (charging != lastCharging) {
                Sender.sendBattery(this@NetService, percent, charging)
                lastCharging = charging
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildForegroundNotification())

        cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // مراقبة الشبكة
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (lastNetState != true) {
                    Sender.sendNetwork(this@NetService, true)
                    lastNetState = true
                }
            }

            override fun onLost(network: Network) {
                if (lastNetState != false) {
                    Sender.sendNetwork(this@NetService, false)
                    lastNetState = false
                }
            }
        })

        // مراقبة البطارية
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun buildForegroundNotification(): Notification {
        val channelId = "sync_service"
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
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // تجاهل
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
