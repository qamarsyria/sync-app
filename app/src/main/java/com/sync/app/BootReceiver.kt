package com.sync.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // إعادة تشغيل الخدمات
        val netIntent = Intent(context, NetService::class.java)
        val recvIntent = Intent(context, ReceiverService::class.java)

        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(netIntent)
            context.startForegroundService(recvIntent)
        } else {
            context.startService(netIntent)
            context.startService(recvIntent)
        }
    }
}
