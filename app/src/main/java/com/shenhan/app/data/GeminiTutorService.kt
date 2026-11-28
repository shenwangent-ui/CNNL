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
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        "gemini-2.0-flash"
    )

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API, "")?.trim().orEmpty()
    }

    fun saveApiKey(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_API, apiKey.trim()).apply()
    }

    fun hasApiKey(context: Context): Boolean = getApiKey(context).isNotBlank()

    suspend fun generateTurn(
        context: Context,
        node: QuestNode,
        scriptMode: ScriptMode,
        historyPairs: List<Pair<Boolean, String>>,
        latestUserMessage: String?
    ): Result<GeminiTurnResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("NO_API_KEY"))
        }

        val scriptLabel = if (scriptMode == ScriptMode.SIMPLIFIED) {
            "Simplified Chinese (简体中文)"
        } else {
            "Traditional Chinese (繁體中文 / 台灣國語常用字)"
        }

        val grammarSummary = if (node.grammarNotes.isNotEmpty()) {
            node.grammarNotes.joinToString("; ") { "${it.pattern} (${it.explain})" }
        } else {
            "Real-life Mandarin communication & general knowledge"
        }

        val systemPrompt = """
            You are Google Gemini — an unrestricted, super-intelligent 6-in-1 Mandarin Chinese AI Tutor & All-Round Companion inside the ShenHan app.
            Current Quest Context (optional backdrop): "${node.titleTh}" (${node.titleZhSimp}) — ${node.descriptionTh}
            Reference Grammar: "$grammarSummary"
            Script Mode: Write all Chinese characters strictly in $scriptLabel.

            CRITICAL RULES:
            - NEVER repeat canned or identical responses. Always respond directly, dynamically, and intelligently to the user's ACTUAL latest message!
            - UNRESTRICTED & OUT-OF-THE-BOX: If the user asks anything outside the lesson (general knowledge, daily life in China/Taiwan, slang, culture, tech, translation, casual chat, or questions in Thai/English/Chinese), answer their question thoroughly and naturally! Do NOT force them back into a rigid script.
            - Provide 6-in-1 value in every turn:
              1. Natural Mandarin reply directly answering what the user just said/asked.
              2. Accurate Pinyin with tone marks.
              3. Clear, helpful Thai explanation/answer in "reply_th" (if they asked a question, answer it completely here).
              4. Multi-skill coaching in "coach_feedback_th" combining:
                 • ✍️ แก้/เกลาประโยค (Native polish if user wrote Chinese)
                 • 📖 ศัพท์ & ไวยากรณ์เด่น (Key vocab/grammar breakdown)
                 • 🇹🇼🇨🇳 เทียบไต้หวัน vs จีนกลาง (Regional usage tip)
                 • 💡 ตัวอย่างเสริม (1 extra practical sentence)
              5. Score accuracy (65-100).
              6. Generate 4 diverse next-turn suggestions in "suggestions" (covering direct reply, deep follow-up, out-of-the-box question, and native slang).

            Respond ONLY with valid JSON matching this schema:
            {
              "reply_zh": "Dynamic Chinese response in $scriptLabel",
              "reply_pinyin": "Full Pinyin with tone marks",
              "reply_th": "Complete natural Thai translation and direct answer to the user's message",
              "coach_feedback_th": "✍️ เกลาประโยค: ... | 📖 ศัพท์/ไวยากรณ์: ... | 🇹🇼🇨🇳 ไต้หวัน-จีน: ... | 💡 ตัวอย่าง: ...",
              "accuracy_pct": 94,
              "step_passed": true,
              "suggestions": [
                {"zh": "Option 1 in $scriptLabel", "pinyin": "Pinyin 1", "th": "Thai 1", "tag": "🎯 ตอบตรงเรื่อง"},
                {"zh": "Option 2 in $scriptLabel", "pinyin": "Pinyin 2", "th": "Thai 2", "tag": "💬 ถามเจาะลึก"},
                {"zh": "Option 3 in $scriptLabel", "pinyin": "Pinyin 3", "th": "Thai 3", "tag": "🚀 ถามนอกกรอบ"},
                {"zh": "Option 4 in $scriptLabel", "pinyin": "Pinyin 4", "th": "Thai 4", "tag": "😎 สำนวนเจ้าถิ่น"}
              ]
            }
        """.trimIndent()

        val promptText = if (latestUserMessage.isNullOrBlank()) {
            "ทักทายเปิดฉากสถานการณ์ '${node.titleTh}' แบบเป็นธรรมชาติ พร้อมบอกผู้ใช้ว่าพิมพ์ถามได้ทุกเรื่องทั้งในบทและนอกกรอบ และสร้าง 4 ตัวเลือกเริ่มต้น"
        } else {
            latestUserMessage
        }

        // จัดเรียงลำดับ role ให้เริ่มด้วย "user" และสลับ "user" / "model" ตามข้อกำหนดของ Gemini REST API
        val rawTurns = mutableListOf<Pair<String, String>>()
        for ((isUser, text) in historyPairs.takeLast(10)) {
            val clean = text.trim()
            if (clean.isNotEmpty()) {
                rawTurns.add(Pair(if (isUser) "user" else "model", clean))
            }
        }
        rawTurns.add(Pair("user", promptText))

        if (rawTurns.first().first != "user") {
            rawTurns.add(0, Pair("user", "หัวข้อเริ่มต้น: ${node.titleTh}"))
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

        val requestBody = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.85)
                put("responseMimeType", "application/json")
            })
        }.toString()

        var lastError: Exception = Exception("Gemini API request failed")

        for (modelName in MODEL_CANDIDATES) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connectTimeout = 15000
                    readTimeout = 25000
                    doOutput = true
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(requestBody)
                }

                val code = conn.responseCode
                val rawResponse = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    val errText = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    throw Exception("HTTP $code ($modelName): $errText")
                }

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

                return@withContext Result.success(
                    GeminiTurnResult(
                        replyZh = parsed.optString("reply_zh", "你好！有什么我可以帮你的吗？"),
                        replyPinyin = parsed.optString("reply_pinyin", "Nǐ hǎo! Yǒu shénme wǒ kěyǐ bāng nǐ de ma?"),
                        replyTh = parsed.optString("reply_th", "สวัสดี! มีอะไรให้ฉันช่วยไหม?"),
                        coachFeedbackTh = parsed.optString("coach_feedback_th", "✨ Gemini 6-in-1 พร้อมตอบทุกคำถามทั้งในและนอกบทเรียน"),
                        accuracyPct = parsed.optInt("accuracy_pct", 92).coerceIn(50, 100),
                        stepPassed = parsed.optBoolean("step_passed", true),
                        suggestions = suggestions.take(4),
                        usedModel = modelName
                    )
                )
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(lastError)
    }
}
