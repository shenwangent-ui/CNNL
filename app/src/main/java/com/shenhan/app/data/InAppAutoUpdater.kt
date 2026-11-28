package com.shenhan.app.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import com.shenhan.app.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object InAppAutoUpdater {
    private const val PREFS_NAME = "shenhan_inapp_updater_prefs_v1"
    private const val KEY_AUTO_INSTALL_CONSENT = "auto_install_consent"
    private const val KEY_PENDING_AUTO_RELAUNCH = "pending_auto_relaunch"
    private const val KEY_JUST_UPDATED_TAG = "just_updated_version_tag"

    const val EXTRA_UPDATE_COMPLETED = "extra_update_completed"

    fun isAutoInstallConsented(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_INSTALL_CONSENT, true)
    }

    fun setAutoInstallConsented(context: Context, consented: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_INSTALL_CONSENT, consented).apply()
    }

    fun markPendingRelaunch(context: Context, versionTag: String, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_PENDING_AUTO_RELAUNCH, enabled)
            .putString(KEY_JUST_UPDATED_TAG, if (enabled) versionTag else "")
            .apply()
    }

    fun consumePendingRelaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pending = prefs.getBoolean(KEY_PENDING_AUTO_RELAUNCH, false)
        if (pending) {
            prefs.edit().putBoolean(KEY_PENDING_AUTO_RELAUNCH, false).apply()
        }
        return pending
    }

    fun consumeJustUpdatedTag(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tag = prefs.getString(KEY_JUST_UPDATED_TAG, null)?.takeIf { it.isNotBlank() }
        if (tag != null) {
            prefs.edit().remove(KEY_JUST_UPDATED_TAG).apply()
        }
        return tag
    }

    suspend fun downloadAndCommitInstall(
        context: Context,
        release: AppUpdateRelease,
        autoInstallConsent: Boolean,
        onProgress: (progress: Float, stageTitle: String, stageDetail: String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            setAutoInstallConsented(context, autoInstallConsent)
            val apkFile = File(context.cacheDir, "shenhan_auto_update.apk")
            if (apkFile.exists()) apkFile.delete()

            withContext(Dispatchers.Main) {
                onProgress(0.05f, "กำลังเชื่อมต่อเซิร์ฟเวอร์อัปเดต...", "เตรียมดาวน์โหลดเวอร์ชัน ${release.versionTag}")
            }

            val conn = (URL(release.apkDownloadUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 45000
                setRequestProperty("User-Agent", "ShenHan-AutoUpdater")
            }

            if (conn.responseCode !in 200..299) {
                throw Exception("ดาวน์โหลดไม่สำเร็จ (HTTP ${conn.responseCode})")
            }

            val totalBytes = conn.contentLengthLong
            var downloadedBytes = 0L
            var lastReportedPct = -1

            conn.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val pct = ((downloadedBytes * 85) / totalBytes).toInt().coerceIn(5, 85)
                            if (pct != lastReportedPct) {
                                lastReportedPct = pct
                                val mbDone = String.format("%.1f", downloadedBytes / 1048576.0)
                                val mbTotal = String.format("%.1f", totalBytes / 1048576.0)
                                withContext(Dispatchers.Main) {
                                    onProgress(
                                        pct / 100f,
                                        "กำลังดาวน์โหลดไฟล์อัปเดต ($pct%)",
                                        "ดาวน์โหลดแล้ว $mbDone / $mbTotal MB"
                                    )
                                }
                            }
                        }
                    }
                    output.flush()
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(
                    0.92f,
                    "กำลังเตรียมติดตั้งลงในเครื่อง...",
                    "ตรวจสอบความสมบูรณ์ของแพ็กเกจเรียบร้อย"
                )
            }

            // บันทึกสถานะให้เปิดแอปกลับอัตโนมัติทันทีที่ระบบติดตั้งเสร็จสิ้น
            markPendingRelaunch(context, release.versionTag, autoInstallConsent)

            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                if (autoInstallConsent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            FileInputStream(apkFile).use { apkIn ->
                session.openWrite("shenhan_base_apk", 0, apkFile.length()).use { sessionOut ->
                    apkIn.copyTo(sessionOut)
                    session.fsync(sessionOut)
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(
                    1.0f,
                    "⚙️ กำลังติดตั้งอัปเดตลงในเครื่อง...",
                    if (autoInstallConsent) {
                        "แอปจะปิดตัวลงชั่วคราวระหว่างติดตั้ง และจะเด้งเปิดกลับมาอัตโนมัติทันทีที่ติดตั้งเสร็จสมบูรณ์ครับ"
                    } else {
                        "กรุณากดยืนยันการติดตั้งบนหน้าต่างของระบบ"
                    }
                )
            }

            val statusIntent = Intent(context, UpdateInstallStatusReceiver::class.java)
            val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val statusPendingIntent = PendingIntent.getBroadcast(context, sessionId, statusIntent, piFlags)
            session.commit(statusPendingIntent.intentSender)
            session.close()

            Result.success(Unit)
        } catch (e: Exception) {
            markPendingRelaunch(context, "", false)
            Result.failure(e)
        }
    }
}

class UpdateInstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirmIntent)
                    } catch (_: Exception) {
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                // ระบบจะเรียก AppReplacedReceiver (MY_PACKAGE_REPLACED) ต่อทันที
            }
            else -> {
                InAppAutoUpdater.consumePendingRelaunch(context)
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "ยกเลิกหรือติดตั้งไม่สำเร็จ"
                Toast.makeText(context, "⚠️ อัปเดตไม่สำเร็จ: $msg", Toast.LENGTH_LONG).show()
            }
        }
    }
}

class AppReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            if (InAppAutoUpdater.consumePendingRelaunch(context)) {
                val relaunchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    putExtra(InAppAutoUpdater.EXTRA_UPDATE_COMPLETED, true)
                }
                try {
                    context.startActivity(relaunchIntent)
                } catch (_: Exception) {
                }
            }
        }
    }
}
