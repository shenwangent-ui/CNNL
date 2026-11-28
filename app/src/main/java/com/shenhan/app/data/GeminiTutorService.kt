package com.shenhan.app.data

import android.content.Context
import com.shenhan.app.model.QuestNode
import com.shenhan.app.model.ScriptMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class GeminiSuggestion(
    val zh: String,
    val pinyin: String,
    val th: String,
    val tag: String
)

data class GeminiTurnResult(
    val replyZh: String,
    val replyPinyin: String,
    val replyTh: String,
    val coachFeedbackTh: String,
    val accuracyPct: Int,
    val stepPassed: Boolean,
    val suggestions: List<GeminiSuggestion>,
    val usedModel: String
)

object GeminiTutorService {
    private const val PREFS_NAME = "shenhan_gemini_prefs"
    private const val KEY_API = "gemini_api_key"
    private val MODEL_CANDIDATES = listOf(
        "gemini-2.5-flash-lite",
        "gemini-2.5-flash"
    )

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API, "")?.trim().orEmpty()
    }

    fun saveApiKey(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_API, apiKey.trim()).apply()
    }

    fun hasApiKey(context: Context): Boolean = true

    suspend fun generateTurn(
        context: Context,
        node: QuestNode,
        scriptMode: ScriptMode,
        historyPairs: List<Pair<Boolean, String>>,
        latestUserMessage: String?
    ): Result<GeminiTurnResult> = withContext(Dispatchers.IO) {
        val scriptLabel = if (scriptMode == ScriptMode.SIMPLIFIED) {
            "Simplified Chinese (简体中文)"
        } else {
            "Traditional Chinese (繁體中文 / 台灣國語常用字)"
        }

        val grammarSummary = if (node.grammarNotes.isNotEmpty()) {
            node.grammarNotes.take(2).joinToString("; ") { "${it.pattern} (${it.explain})" }
        } else {
            "Practical Mandarin conversation"
        }

        val systemPrompt = """
            You are "ผู้ช่วยเฉิน" (Chen / 陈助理), a smart, friendly native Mandarin Chinese tutor inside the ShenHan (神漢) app.
            IDENTITY RULE: Your name is strictly "ผู้ช่วยเฉิน" (หรือ "เฉิน"). NEVER mention Google, Gemini, LLM, or any AI model name.
            Current Topic: "${node.titleTh}" (${node.titleZhSimp})
            Reference Grammar: "$grammarSummary"
            Script Mode: Write all Chinese characters in $scriptLabel.

            RULES:
            - Respond directly, naturally, and fast to the user's ACTUAL latest message (never repeat canned answers).
            - Answer ANY topic (in-lesson roleplay, general questions, slang, Taiwan vs Mainland usage, or Thai/Chinese chat).
            - Keep explanations concise, practical, and easy to read on mobile.

            Return ONLY valid JSON:
            {
              "reply_zh": "Natural Chinese reply in $scriptLabel",
              "reply_pinyin": "Pinyin with tone marks",
              "reply_th": "Clear Thai translation and direct answer",
              "coach_feedback_th": "✍️ เกลาประโยค: ... | 📖 ศัพท์/ไวยากรณ์: ... | 🇹🇼🇨🇳 ไต้หวัน-จีน: ...",
              "accuracy_pct": 94,
              "step_passed": true,
              "suggestions": [
                {"zh": "Option 1 in $scriptLabel", "pinyin": "Pinyin 1", "th": "Thai 1", "tag": "🎯 ตอบตรงเรื่อง"},
                {"zh": "Option 2 in $scriptLabel", "pinyin": "Pinyin 2", "th": "Thai 2", "tag": "💬 ถามต่อ"},
                {"zh": "Option 3 in $scriptLabel", "pinyin": "Pinyin 3", "th": "Thai 3", "tag": "🚀 ถามนอกกรอบ"},
                {"zh": "Option 4 in $scriptLabel", "pinyin": "Pinyin 4", "th": "Thai 4", "tag": "😎 สำนวนเจ้าถิ่น"}
              ]
            }
        """.trimIndent()

        val promptText = if (latestUserMessage.isNullOrBlank()) {
            "แนะนำตัวสั้นๆ ในฐานะ 'ผู้ช่วยเฉิน' เปิดฉากหัวข้อ '${node.titleTh}' และสร้าง 4 ตัวเลือกเริ่มต้น"
        } else {
            latestUserMessage
        }

        val rawTurns = mutableListOf<Pair<String, String>>()
        for ((isUser, text) in historyPairs.takeLast(6)) {
            val clean = text.trim()
            if (clean.isNotEmpty()) {
                rawTurns.add(Pair(if (isUser) "user" else "model", clean))
            }
        }
        rawTurns.add(Pair("user", promptText))

        if (rawTurns.first().first != "user") {
            rawTurns.add(0, Pair("user", "หัวข้อ: ${node.titleTh}"))
        }

        val normalizedTurns = mutableListOf<Pair<String, String>>()
        for ((role, text) in rawTurns) {
            if (normalizedTurns.isNotEmpty() && normalizedTurns.last().first == role) {
                val prev = normalizedTurns.removeAt(normalizedTurns.lastIndex)
                normalizedTurns.add(Pair(role, "${prev.second}\n$text"))
            } else {
                normalizedTurns.add(Pair(role, text))
            }
        }

        val contentsArray = JSONArray()
        for ((role, text) in normalizedTurns) {
            contentsArray.put(
                JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                }
            )
        }

        val geminiPayloadObj = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.75)
                put("maxOutputTokens", 750)
                put("responseMimeType", "application/json")
                put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
            })
        }

        fun parseResponse(rawResponse: String): GeminiTurnResult {
            val root = JSONObject(rawResponse)
            val rawText = root.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()

            val jsonStart = rawText.indexOf('{')
            val jsonEnd = rawText.lastIndexOf('}')
            val cleanJson = if (jsonStart != -1 && jsonEnd > jsonStart) {
                rawText.substring(jsonStart, jsonEnd + 1)
            } else {
                rawText
            }

            val parsed = JSONObject(cleanJson)
            val sugArr = parsed.optJSONArray("suggestions") ?: JSONArray()
            val suggestions = mutableListOf<GeminiSuggestion>()
            for (i in 0 until sugArr.length()) {
                val item = sugArr.getJSONObject(i)
                val zhItem = item.optString("zh", "").trim()
                if (zhItem.isNotEmpty()) {
                    suggestions.add(
                        GeminiSuggestion(
                            zh = zhItem,
                            pinyin = item.optString("pinyin", ""),
                            th = item.optString("th", ""),
                            tag = item.optString("tag", "💬 แนะนำ")
                        )
                    )
                }
            }

            return GeminiTurnResult(
                replyZh = parsed.optString("reply_zh", "你好！我是陈助理，有什么我可以帮你的吗？"),
                replyPinyin = parsed.optString("reply_pinyin", "Nǐ hǎo! Wǒ shì Chén zhùlǐ, yǒu shénme wǒ kěyǐ bāng nǐ de ma?"),
                replyTh = parsed.optString("reply_th", "สวัสดีครับ! ผมผู้ช่วยเฉิน มีอะไรให้ผมช่วยไหมครับ?"),
                coachFeedbackTh = parsed.optString("coach_feedback_th", "ผู้ช่วยเฉินพร้อมสนทนาและตอบคำถามทุกเรื่องครับ"),
                accuracyPct = parsed.optInt("accuracy_pct", 92).coerceIn(50, 100),
                stepPassed = parsed.optBoolean("step_passed", true),
                suggestions = suggestions.take(4),
                usedModel = "ผู้ช่วยเฉิน"
            )
        }

        var lastError: Exception = Exception("การเชื่อมต่อล่าช้า")

        try {
            val edgeUrl = "${SupabaseSyncManager.SUPABASE_URL}/functions/v1/gemini-tutor"
            val edgeBody = JSONObject().put("payload", geminiPayloadObj).toString()
            val conn = (URL(edgeUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("apikey", SupabaseSyncManager.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseSyncManager.SUPABASE_ANON_KEY}")
                connectTimeout = 15000
                readTimeout = 45000
                doOutput = true
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(edgeBody)
            }

            val code = conn.responseCode
            val rawResponse = if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val errText = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                throw Exception("HTTP $code: $errText")
            }

            return@withContext Result.success(parseResponse(rawResponse))
        } catch (e: Exception) {
            lastError = e
        }

        val localKey = getApiKey(context)
        if (localKey.isNotBlank()) {
            val directBody = geminiPayloadObj.toString()
            for (modelName in MODEL_CANDIDATES) {
                try {
                    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$localKey"
                    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        connectTimeout = 15000
                        readTimeout = 45000
                        doOutput = true
                    }

                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(directBody)
                    }

                    val code = conn.responseCode
                    val rawResponse = if (code in 200..299) {
                        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else {
                        throw Exception("HTTP $code")
                    }

                    return@withContext Result.success(parseResponse(rawResponse))
                } catch (e: Exception) {
                    lastError = e
                }
            }
        }

        Result.failure(lastError)
    }
}
