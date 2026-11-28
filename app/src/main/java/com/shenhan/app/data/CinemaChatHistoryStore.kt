package com.shenhan.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class StoredChatMessage(
    val id: String,
    val isUser: Boolean,
    val zhText: String,
    val pinyin: String,
    val thText: String,
    val coachFeedback: String,
    val accuracyPct: Int?
)

data class StoredChatSession(
    val messages: List<StoredChatMessage>,
    val passedTurns: Int,
    val isLessonCompleted: Boolean
)

object CinemaChatHistoryStore {
    private const val PREFS_NAME = "shenhan_cinema_chat_history_v1"
    private const val MAX_MESSAGES_PER_NODE = 60

    fun loadSession(context: Context, nodeId: String): StoredChatSession? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString("node_$nodeId", null) ?: return null
            val root = JSONObject(raw)
            val arr = root.optJSONArray("messages") ?: return null
            val list = mutableListOf<StoredChatMessage>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                list.add(
                    StoredChatMessage(
                        id = item.optString("id", "msg_$i"),
                        isUser = item.optBoolean("isUser", false),
                        zhText = item.optString("zhText", ""),
                        pinyin = item.optString("pinyin", ""),
                        thText = item.optString("thText", ""),
                        coachFeedback = item.optString("coachFeedback", ""),
                        accuracyPct = if (item.has("accuracyPct") && !item.isNull("accuracyPct")) {
                            item.getInt("accuracyPct")
                        } else {
                            null
                        }
                    )
                )
            }
            if (list.isEmpty()) null else StoredChatSession(
                messages = list,
                passedTurns = root.optInt("passedTurns", 0),
                isLessonCompleted = root.optBoolean("isLessonCompleted", false)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveSession(
        context: Context,
        nodeId: String,
        messages: List<StoredChatMessage>,
        passedTurns: Int,
        isLessonCompleted: Boolean
    ) {
        try {
            val trimmed = messages.takeLast(MAX_MESSAGES_PER_NODE)
            val arr = JSONArray()
            for (m in trimmed) {
                arr.put(
                    JSONObject().apply {
                        put("id", m.id)
                        put("isUser", m.isUser)
                        put("zhText", m.zhText)
                        put("pinyin", m.pinyin)
                        put("thText", m.thText)
                        put("coachFeedback", m.coachFeedback)
                        if (m.accuracyPct != null) {
                            put("accuracyPct", m.accuracyPct)
                        }
                    }
                )
            }
            val root = JSONObject().apply {
                put("passedTurns", passedTurns)
                put("isLessonCompleted", isLessonCompleted)
                put("messages", arr)
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("node_$nodeId", root.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun clearSession(context: Context, nodeId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("node_$nodeId").apply()
    }
}
