package com.sync.app

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return

        // تجاهل تطبيقنا نفسه
        if (pkg == packageName) return

        // تجاهل تطبيقات النظام
        if (pkg == "android" || pkg.startsWith("com.android.systemui")) return

        // تجاهل الإشعارات الدائمة (ongoing)
        if (sbn.isOngoing) return

        val extras = sbn.notification.extras ?: return

        // استخراج العنوان
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""

        // استخراج النص
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // استخراج النص الطويل إذا وجد
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        val finalText = if (!bigText.isNullOrEmpty()) bigText else text

        // إذا لا يوجد عنوان ولا نص، تجاهل
        if (title.isEmpty() && finalText.isEmpty()) return

        // اسم التطبيق
        val appName = getAppName(pkg)

        // التنسيق النهائي
        val formattedTitle = "[$appName] $title"

        // إرسال
        Sender.send(this, formattedTitle, finalText)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // يمكن تسجيل حذف الإشعار لاحقاً
    }

    private fun getAppName(pkg: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkg
        }
    }
}
