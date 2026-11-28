package com.shenhan.app.data

import android.Manifest
import android.app.AlarmManager
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.shenhan.app.MainActivity
import com.shenhan.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AppUpdateRelease(
    val releaseId: String,
    val versionTag: String,
    val title: String,
    val notes: String,
    val apkDownloadUrl: String,
    val publishedAt: String
)

object SmartNotificationManager {
    private const val PREFS_NAME = "shenhan_smart_notif_prefs_v1"

    const val CHANNEL_UPDATES = "shenhan_updates"
    const val CHANNEL_STUDY = "shenhan_study_reminder"
    const val CHANNEL_PROGRESS = "shenhan_progress"
    const val CHANNEL_PROMO = "shenhan_promotions"

    const val EXTRA_OPEN_UPDATE = "extra_open_update"
    const val EXTRA_TARGET_TAB = "extra_target_tab"
    const val ACTION_DAILY_REMINDER = "com.shenhan.app.ACTION_DAILY_REMINDER"

    private const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/shenwangent-ui/CNNL/releases/latest"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_UPDATES,
                    "อัปเดตแอปและฟีเจอร์ใหม่",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "แจ้งเตือนเมื่อมีเวอร์ชันใหม่หรือฟีเจอร์ใหม่พร้อมติดตั้ง" },
                NotificationChannel(
                    CHANNEL_STUDY,
                    "เตือนฝึกภาษาจีนประจำวัน",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "เตือนชวนฝึกคุยกับผู้ช่วยเฉินอย่างพอดีวันละ 1 ครั้ง" },
                NotificationChannel(
                    CHANNEL_PROGRESS,
                    "ความคืบหน้าและเป้าหมาย",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "แจ้งเตือนเมื่อเรียนจบด่านหรือทำสถิติครบเป้าหมาย" },
                NotificationChannel(
                    CHANNEL_PROMO,
                    "โปรโมชั่นและกิจกรรมพิเศษ",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "ข่าวสาร กิจกรรม และสิทธิพิเศษจาก ShenHan" }
            )
            channels.forEach { nm.createNotificationChannel(it) }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun markStudiedToday(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayKey()
        prefs.edit()
            .putString("last_studied_date", today)
            .putInt("ignored_reminder_days", 0)
            .apply()
    }

    fun scheduleSmartDailyReminder(context: Context) {
        try {
            createNotificationChannels(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SmartNotificationReceiver::class.java).apply {
                action = ACTION_DAILY_REMINDER
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, 4001, intent, flags)

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 19)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (_: Exception) {
        }
    }

    fun evaluateAndSendDailyReminder(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayKey()
        val lastStudied = prefs.getString("last_studied_date", "")
        val lastReminded = prefs.getString("last_reminded_date", "")

        // ถ้าวันนี้เรียนแล้ว หรือวันนี้แจ้งเตือนไปแล้ว จะไม่แจ้งเตือนซ้ำ
        if (lastStudied == today || lastReminded == today) return

        val ignoredDays = prefs.getInt("ignored_reminder_days", 0)
        // ถ้าผู้ใช้ไม่ได้เปิดแอปติดต่อกันหลายวัน ให้เว้นระยะเตือนทุก 2 วันเพื่อไม่ให้รบกวน
        if (ignoredDays >= 2 && ignoredDays % 2 != 0) {
            prefs.edit().putInt("ignored_reminder_days", ignoredDays + 1).apply()
            return
        }

        val messages = listOf(
            "ผู้ช่วยเฉินรอคุยด้วยอยู่นะครับ วันนี้แวะมาฝึกจีนสัก 3 นาทีไหม?",
            "ทบทวนศัพท์จีนวันละนิด กับผู้ช่วยเฉินใน AI Cinema กันครับ",
            "พร้อมลุยด่านภาษาจีนต่อหรือยังครับ? ผู้ช่วยเฉินเตรียมประโยคใหม่ไว้ให้แล้ว"
        )
        val body = messages[(System.currentTimeMillis() / 86400000L).toInt().mod(messages.size)]

        showTopBarNotification(
            context = context,
            channelId = CHANNEL_STUDY,
            notificationId = 1002,
            title = "📚 ได้เวลาฝึกจีนกับผู้ช่วยเฉินแล้วครับ",
            body = body,
            targetTab = 2
        )

        prefs.edit()
            .putString("last_reminded_date", today)
            .putInt("ignored_reminder_days", ignoredDays + 1)
            .apply()
    }

    fun notifyProgressMilestone(context: Context, title: String, detail: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastProgressTime = prefs.getLong("last_progress_notif_ms", 0L)
        val now = System.currentTimeMillis()
        // จำกัดไม่ให้แจ้งเตือนความคืบหน้าถี่เกิน 1 ครั้งต่อ 6 ชั่วโมง
        if (now - lastProgressTime < 6 * 60 * 60 * 1000L) return

        showTopBarNotification(
            context = context,
            channelId = CHANNEL_PROGRESS,
            notificationId = 1003,
            title = title,
            body = detail,
            targetTab = 0
        )
        prefs.edit().putLong("last_progress_notif_ms", now).apply()
    }

    suspend fun checkForAppUpdate(context: Context, notifyTopBar: Boolean = true): AppUpdateRelease? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(GITHUB_LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "ShenHan-Android-App")
                connectTimeout = 10000
                readTimeout = 12000
            }
            if (conn.responseCode !in 200..299) return@withContext null

            val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val assets = root.optJSONArray("assets") ?: JSONArray()
            var apkUrl = ""
            var updatedAt = root.optString("published_at", "")
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name", "").endsWith(".apk")) {
                    apkUrl = a.optString("browser_download_url", "")
                    val assetTime = a.optString("updated_at", "")
                    if (assetTime.isNotBlank()) updatedAt = assetTime
                    break
                }
            }
            if (apkUrl.isBlank()) return@withContext null

            val releaseSignature = "${root.optString("tag_name", "latest")}_$updatedAt"
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val appInstallTime = try {
                context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
            } catch (_: Exception) {
                0L
            }
            val savedInstallTime = prefs.getLong("tracked_apk_install_time", 0L)

            // บันทึกเวอร์ชันปัจจุบันอัตโนมัติหลังติดตั้ง APK ใหม่เสร็จ เพื่อไม่ให้แจ้งเตือนซ้ำเวอร์ชันเดิม
            if (appInstallTime > savedInstallTime) {
                prefs.edit()
                    .putLong("tracked_apk_install_time", appInstallTime)
                    .putString("installed_release_sig", releaseSignature)
                    .apply()
                return@withContext null
            }

            val installedSig = prefs.getString("installed_release_sig", "")
            if (installedSig == releaseSignature) return@withContext null

            val release = AppUpdateRelease(
                releaseId = releaseSignature,
                versionTag = root.optString("tag_name", "เวอร์ชันใหม่"),
                title = root.optString("name", "ShenHan อัปเดตเวอร์ชันใหม่"),
                notes = root.optString("body", "ปรับปรุงความเร็วผู้ช่วยเฉิน และเพิ่มฟีเจอร์ใหม่").take(220),
                apkDownloadUrl = apkUrl,
                publishedAt = updatedAt
            )

            val lastNotifiedSig = prefs.getString("notified_release_sig", "")
            if (notifyTopBar && lastNotifiedSig != releaseSignature) {
                showTopBarNotification(
                    context = context,
                    channelId = CHANNEL_UPDATES,
                    notificationId = 1001,
                    title = "🚀 อัปเดตใหม่พร้อมติดตั้ง (${release.versionTag})",
                    body = "แตะเพื่อเปิดแอปและดาวน์โหลดฟีเจอร์ใหม่ล่าสุดทันที",
                    openUpdate = true
                )
                prefs.edit().putString("notified_release_sig", releaseSignature).apply()
            }

            release
        } catch (_: Exception) {
            null
        }
    }

    suspend fun checkPromotionsAndNews(context: Context) = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseSyncManager.SUPABASE_URL}/rest/v1/app_notifications?is_active=eq.true&order=created_at.desc&limit=3"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SupabaseSyncManager.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseSyncManager.SUPABASE_ANON_KEY}")
                connectTimeout = 8000
                readTimeout = 10000
            }
            if (conn.responseCode !in 200..299) return@withContext

            val arr = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
            if (arr.length() == 0) return@withContext

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val seenIds = prefs.getStringSet("seen_promo_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val id = item.optString("id", "")
                if (id.isNotBlank() && !seenIds.contains(id)) {
                    val title = item.optString("title", "🎁 ข่าวสารและโปรโมชั่นใหม่จาก ShenHan")
                    val body = item.optString("body", "เปิดแอปเพื่อดูรายละเอียดสิทธิพิเศษล่าสุด")
                    val targetTab = item.optInt("target_tab", 0)
                    showTopBarNotification(
                        context = context,
                        channelId = CHANNEL_PROMO,
                        notificationId = 2000 + i,
                        title = title,
                        body = body,
                        targetTab = targetTab
                    )
                    seenIds.add(id)
                    prefs.edit().putStringSet("seen_promo_ids", seenIds).apply()
                    break // แจ้งเตือนทีละ 1 รายการ ไม่ยิงรัว
                }
            }
        } catch (_: Exception) {
        }
    }

    fun startApkUpdateDownload(context: Context, release: AppUpdateRelease) {
        try {
            val fileName = "ShenHan-${release.versionTag.replace(Regex("[^A-Za-z0-9._-]"), "_")}.apk"
            val request = DownloadManager.Request(Uri.parse(release.apkDownloadUrl)).apply {
                setTitle("กำลังดาวน์โหลดอัปเดต ShenHan")
                setDescription("แตะที่แถบแจ้งเตือนเมื่อโหลดเสร็จเพื่อติดตั้งทันที")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(
                context,
                "กำลังดาวน์โหลดอัปเดตบนแถบด้านบน โหลดเสร็จแล้วกดติดตั้งได้เลยครับ",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(release.apkDownloadUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {
            }
        }
    }

    fun showTopBarNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        openUpdate: Boolean = false,
        targetTab: Int = 0
    ) {
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_UPDATE, openUpdate)
            putExtra(EXTRA_TARGET_TAB, targetTab)
        }
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(context, notificationId, launchIntent, piFlags)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (openUpdate) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        if (openUpdate) {
            builder.addAction(
                R.mipmap.ic_launcher,
                "อัปเดตทันที",
                contentIntent
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

class SmartNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == SmartNotificationManager.ACTION_DAILY_REMINDER ||
            intent?.action == Intent.ACTION_BOOT_COMPLETED
        ) {
            SmartNotificationManager.scheduleSmartDailyReminder(context)
            SmartNotificationManager.evaluateAndSendDailyReminder(context)
        }
    }
}
