package com.shenhan.app.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ProEntitlementManager {
    private const val PREFS_NAME = "shenhan_pro_entitlements_v1"
    private const val KEY_IS_PRO = "is_pro_member"
    private const val KEY_CHAT_DATE = "daily_chat_date"
    private const val KEY_CHAT_COUNT = "daily_chat_count"

    const val FREE_DAILY_CHAT_LIMIT = 8
    const val FREE_MAX_CUSTOM_PACKS = 2
    const val FREE_UNLOCKED_ZONES = 2

    fun isPro(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_PRO, false)
    }

    fun setPro(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_PRO, enabled).apply()
    }

    fun getUsedChatTurnsToday(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayKey()
        val savedDate = prefs.getString(KEY_CHAT_DATE, "")
        return if (savedDate == today) prefs.getInt(KEY_CHAT_COUNT, 0) else 0
    }

    fun getRemainingFreeTurns(context: Context): Int {
        if (isPro(context)) return 999
        return (FREE_DAILY_CHAT_LIMIT - getUsedChatTurnsToday(context)).coerceAtLeast(0)
    }

    fun canSendChatTurn(context: Context): Boolean {
        return isPro(context) || getRemainingFreeTurns(context) > 0
    }

    fun recordChatTurn(context: Context) {
        if (isPro(context)) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayKey()
        val current = getUsedChatTurnsToday(context)
        prefs.edit()
            .putString(KEY_CHAT_DATE, today)
            .putInt(KEY_CHAT_COUNT, current + 1)
            .apply()
    }

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
