package com.sync.app

import android.content.Context
import okhttp3.*
import java.io.IOException

object Sender {

    private val client = OkHttpClient()

    /**
     * إرسال إشعار إلى موضوع الطرف الآخر عبر ntfy.sh
     */
    fun send(ctx: Context, title: String, body: String) {
        val topic = Prefs.partnerTopic(ctx) ?: return
        val url = "https://ntfy.sh/$topic"

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(body.toByteArray(), MediaType.parse("text/plain")))
            .addHeader("Title", title)
            .addHeader("Priority", "default")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // فشل الإرسال: لا نفعل شيئاً حالياً
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    /**
     * إرسال حدث شبكة (اتصال/انقطاع)
     */
    fun sendNetwork(ctx: Context, connected: Boolean) {
        val title = if (connected) "📶 Internet" else "📵 Internet"
        val body = if (connected) "الاتصال رجع" else "الاتصال انقطع"
        send(ctx, title, body)
    }

    /**
     * إرسال حدث بطارية
     */
    fun sendBattery(ctx: Context, level: Int, charging: Boolean) {
        val title = "🔋 Battery"
        val body = "المستوى: $level% - " + if (charging) "يشحن" else "لا يشحن"
        send(ctx, title, body)
    }
}
