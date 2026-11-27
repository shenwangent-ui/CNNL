package com.shenhan.app.data

import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.firebase.auth.FirebaseAuth
import com.shenhan.app.model.DialogueTurn
import com.shenhan.app.model.HskLevel
import com.shenhan.app.model.QuestNode
import com.shenhan.app.model.QuestNodeType
import com.shenhan.app.model.QuestZone
import com.shenhan.app.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseSyncManager {
    companion object {
        const val SUPABASE_URL = "https://murqzekjjrvcabbzjxji.supabase.co"
        // นำค่า anon public key จาก Supabase > Project Settings > API มาใส่แทนค่าด้านล่างนี้
        const val SUPABASE_ANON_KEY = "Sb_publishable_bwP7kNtv1K8HJyR5BrV6Kg_H2v6_fsO"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun isConfigured(): Boolean {
        return (SUPABASE_ANON_KEY.startsWith("eyJ") || SUPABASE_ANON_KEY.startsWith("sb_publishable_", ignoreCase = true))
    }

    private suspend fun getFirebaseToken(): String? = suspendCancellableCoroutine { cont ->
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        user.getIdToken(false)
            .addOnSuccessListener { res -> cont.resume(res.token) }
            .addOnFailureListener { e ->
                Log.w("SupabaseSync", "Cannot get Firebase ID token: ${e.message}")
                cont.resume(null)
            }
    }

    private fun buildRequest(url: String, token: String?): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        return builder
    }

    suspend fun fetchZonesAndLessons(
        fallbackZones: List<QuestZone>,
        userId: String?
    ): Triple<List<QuestZone>, UserStatsSnapshot?, Boolean> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Triple(fallbackZones, null, false)
        }
        try {
            val token = getFirebaseToken()

            // 1. ดึง quest_zones
            val zonesReq = buildRequest("$SUPABASE_URL/rest/v1/quest_zones?select=*&order=order_index.asc", token).get().build()
            val zonesBody = client.newCall(zonesReq).execute().use { if (it.isSuccessful) it.body?.string() else null }

            // 2. ดึง dialogue_lessons
            val lessonsReq = buildRequest("$SUPABASE_URL/rest/v1/dialogue_lessons?select=*&order=order_index.asc", token).get().build()
            val lessonsBody = client.newCall(lessonsReq).execute().use { if (it.isSuccessful) it.body?.string() else null }

            // 3. ดึง lesson_progress และ user_profiles ของผู้ใช้ (ถ้าล็อกอินแล้ว)
            val progressMap = mutableMapOf<String, LessonProgressSnapshot>()
            var userStats: UserStatsSnapshot? = null

            if (!userId.isNullOrBlank() && !userId.startsWith("guest_")) {
                val progReq = buildRequest("$SUPABASE_URL/rest/v1/lesson_progress?user_id=eq.$userId&select=*", token).get().build()
                client.newCall(progReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val arr = JSONArray(resp.body?.string() ?: "[]")
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val lid = obj.optString("lesson_id")
                            if (lid.isNotEmpty()) {
                                progressMap[lid] = LessonProgressSnapshot(
                                    isUnlocked = obj.optBoolean("is_unlocked", false),
                                    isCompleted = obj.optBoolean("is_completed", false),
                                    starsEarned = obj.optInt("stars_earned", 0)
                                )
                            }
                        }
                    }
                }

                val profReq = buildRequest("$SUPABASE_URL/rest/v1/user_profiles?user_id=eq.$userId&select=*", token).get().build()
                client.newCall(profReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val arr = JSONArray(resp.body?.string() ?: "[]")
                        if (arr.length() > 0) {
                            val obj = arr.getJSONObject(0)
                            userStats = UserStatsSnapshot(
                                xp = obj.optInt("user_xp", 0),
                                level = obj.optInt("user_level", 1),
                                streakDays = obj.optInt("streak_days", 1),
                                recommendedZoneId = obj.optString("recommended_zone_id", "zone_1")
                            )
                        }
                    }
                }
            }

            if (zonesBody.isNullOrBlank() || lessonsBody.isNullOrBlank()) {
                return@withContext Triple(fallbackZones, userStats, false)
            }

            val zonesJson = JSONArray(zonesBody)
            val lessonsJson = JSONArray(lessonsBody)
            if (zonesJson.length() == 0) {
                return@withContext Triple(fallbackZones, userStats, false)
            }

            // จัดกลุ่มบทเรียนตาม zone_id
            val lessonsByZone = mutableMapOf<String, MutableList<QuestNode>>()
            for (i in 0 until lessonsJson.length()) {
                val lObj = lessonsJson.getJSONObject(i)
                val lessonId = lObj.optString("lesson_id")
                val zoneId = lObj.optString("zone_id")
                val orderIdx = lObj.optInt("order_index", 1)
                val scriptArr = lObj.optJSONArray("dialogue_script") ?: JSONArray()
                val turns = mutableListOf<DialogueTurn>()
                for (j in 0 until scriptArr.length()) {
                    val t = scriptArr.getJSONObject(j)
                    turns.add(
                        DialogueTurn(
                            turn = t.optInt("turn", j + 1),
                            speaker = t.optString("speaker", "npc"),
                            hant = t.optString("hant", ""),
                            hans = t.optString("hans", ""),
                            pinyin = t.optString("pinyin", ""),
                            th = t.optString("th", "")
                        )
                    )
                }

                val saved = progressMap[lessonId]
                val defaultUnlocked = orderIdx == 1
                val isUnlocked = saved?.isUnlocked ?: defaultUnlocked
                val stars = saved?.starsEarned ?: 0

                val node = QuestNode(
                    id = lessonId,
                    zoneId = zoneId,
                    titleTh = lObj.optString("title_th", "บทสนทนา"),
                    titleEn = lObj.optString("pinyin", ""),
                    titleZhSimp = lObj.optString("title_zh_hans", ""),
                    titleZhTrad = lObj.optString("title_zh_hant", ""),
                    nodeType = QuestNodeType.SENTENCE_BUILDER,
                    iconEmoji = "💬",
                    xpReward = 50,
                    starsEarned = stars,
                    isUnlocked = isUnlocked,
                    isCurrent = isUnlocked && stars == 0,
                    descriptionTh = lObj.optString("npc_role", "ฝึกสนทนาโต้ตอบ"),
                    hskLevel = HskLevel.HSK1,
                    dialogueTurns = turns
                )
                lessonsByZone.getOrPut(zoneId) { mutableListOf() }.add(node)
            }

            val mergedZones = mutableListOf<QuestZone>()
            for (i in 0 until zonesJson.length()) {
                val zObj = zonesJson.getJSONObject(i)
                val zoneId = zObj.optString("zone_id")
                val hskStr = zObj.optString("hsk_level", "HSK1")
                val hsk = runCatching { HskLevel.valueOf(hskStr) }.getOrDefault(HskLevel.HSK1)
                val dbNodes = lessonsByZone[zoneId].orEmpty()
                val fallbackNodes = fallbackZones.find { it.id == zoneId }?.nodes.orEmpty()
                val finalNodes = if (dbNodes.isNotEmpty()) dbNodes else fallbackNodes
                val unlocked = zObj.optBoolean("is_default_unlocked", i == 0) || finalNodes.any { it.isUnlocked }

                mergedZones.add(
                    QuestZone(
                        id = zoneId,
                        titleTh = zObj.optString("title_th", "บทเรียน"),
                        titleEn = hskStr,
                        titleZhSimp = zObj.optString("title_zh_hans", ""),
                        titleZhTrad = zObj.optString("title_zh_hant", ""),
                        hskLevel = hsk,
                        themeColorHex = 0xFF7C3AED,
                        isUnlocked = unlocked,
                        nodes = finalNodes
                    )
                )
            }

            Triple(mergedZones, userStats, true)
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Sync fetch failed: ${e.message}")
            Triple(fallbackZones, null, false)
        }
    }

    suspend fun upsertProfileAndProgress(
        profile: UserProfile?,
        xp: Int,
        level: Int,
        streakDays: Int,
        recommendedZoneId: String,
        completedNode: QuestNode?,
        unlockedNextNode: QuestNode?
    ) = withContext(Dispatchers.IO) {
        if (!isConfigured() || profile == null || profile.id.startsWith("guest_")) return@withContext
        try {
            val token = getFirebaseToken() ?: return@withContext

            // 1. Upsert user_profiles ก่อนเสมอ (เพื่อไม่ให้ติด Foreign Key ใน lesson_progress)
            val profileJson = JSONObject().apply {
                put("user_id", profile.id)
                if (!profile.customId.isNullOrBlank()) put("custom_id", profile.customId)
                put("display_name", profile.displayName)
                put("user_xp", xp)
                put("user_level", level)
                put("streak_days", streakDays)
                put("recommended_zone_id", recommendedZoneId)
            }
            val profReq = buildRequest("$SUPABASE_URL/rest/v1/user_profiles?on_conflict=user_id", token)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(profileJson.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(profReq).execute().close()

            // 2. บันทึกด่านที่เพิ่งผ่าน
            if (completedNode != null) {
                val progJson = JSONObject().apply {
                    put("user_id", profile.id)
                    put("zone_id", completedNode.zoneId)
                    put("lesson_id", completedNode.id)
                    put("status", "completed")
                    put("is_unlocked", true)
                    put("is_completed", true)
                    put("stars_earned", completedNode.starsEarned)
                    put("best_score", completedNode.starsEarned * 33 + 1)
                }
                val progReq = buildRequest("$SUPABASE_URL/rest/v1/lesson_progress?on_conflict=user_id,lesson_id", token)
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(progJson.toString().toRequestBody(jsonMediaType))
                    .build()
                client.newCall(progReq).execute().close()
            }

            // 3. บันทึกสถานะปลดล็อกด่านถัดไป
            if (unlockedNextNode != null) {
                val nextJson = JSONObject().apply {
                    put("user_id", profile.id)
                    put("zone_id", unlockedNextNode.zoneId)
                    put("lesson_id", unlockedNextNode.id)
                    put("status", if (unlockedNextNode.starsEarned > 0) "completed" else "unlocked")
                    put("is_unlocked", true)
                    put("is_completed", unlockedNextNode.starsEarned > 0)
                    put("stars_earned", unlockedNextNode.starsEarned)
                }
                val nextReq = buildRequest("$SUPABASE_URL/rest/v1/lesson_progress?on_conflict=user_id,lesson_id", token)
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(nextJson.toString().toRequestBody(jsonMediaType))
                    .build()
                client.newCall(nextReq).execute().close()
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Upsert failed: ${e.message}")
        }
    }
}

data class LessonProgressSnapshot(
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val starsEarned: Int
)

data class UserStatsSnapshot(
    val xp: Int,
    val level: Int,
    val streakDays: Int,
    val recommendedZoneId: String
)
