package com.shenhan.app.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import com.shenhan.app.MainActivity

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
                // ระบบจะเรียก AppReplacedReceiver (MY_PACKAGE_REPLACED) ต่ออัตโนมัติ
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
