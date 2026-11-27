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
    private val MODEL_CANDIDATES = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")

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
            "Traditional Chinese (繁體中文 / 台灣常用)"
        }

        val grammarSummary = if (node.grammarNotes.isNotEmpty()) {
            node.grammarNotes.joinToString("; ") { "${it.pattern} (${it.explain})" }
        } else {
            "Practical daily Mandarin conversation"
        }

        val systemPrompt = """
            You are an expert native Mandarin Chinese roleplay partner and language coach inside the ShenHan app.
            Current Scenario: "${node.titleTh}" (${node.titleZhSimp})
            Scenario Description: "${node.descriptionTh}"
            Target Grammar/Vocabulary: "$grammarSummary"
            Script Requirement: ALWAYS write all Chinese text in $scriptLabel.

            Your job:
            1. Stay in character for this scenario and reply naturally in Mandarin Chinese.
            2. If latestUserMessage is null, start the scene with an engaging opening line and 3 helpful initial replies for the learner.
            3. If latestUserMessage is provided, evaluate the learner's response (even if they wrote partial Chinese, Pinyin, or Thai), give encouraging Thai coaching feedback, score accuracy (60-100), and continue the roleplay.
            4. Always provide 3 context-aware next reply suggestions for the learner to choose from.

            Respond ONLY with valid JSON matching this exact schema:
            {
              "reply_zh": "Chinese reply in $scriptLabel",
              "reply_pinyin": "Pinyin with tone marks",
              "reply_th": "Natural Thai translation of reply_zh",
              "coach_feedback_th": "Short, practical Thai feedback on the user's message (or welcome tip on turn 0)",
              "accuracy_pct": 92,
              "step_passed": true,
              "suggestions": [
                {"zh": "Option 1 in $scriptLabel", "pinyin": "Pinyin 1", "th": "Thai 1", "tag": "🎯 ตรงประเด็น"},
                {"zh": "Option 2 in $scriptLabel", "pinyin": "Pinyin 2", "th": "Thai 2", "tag": "💬 ถามต่อ"},
                {"zh": "Option 3 in $scriptLabel", "pinyin": "Pinyin 3", "th": "Thai 3", "tag": "👍 สุภาพ"}
              ]
            }
        """.trimIndent()

        val contentsArray = JSONArray()
        for ((isUser, text) in historyPairs.takeLast(8)) {
            val role = if (isUser) "user" else "model"
            val msgObj = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", text)))
            }
            contentsArray.put(msgObj)
        }

        val promptText = if (latestUserMessage.isNullOrBlank()) {
            "Start this roleplay scenario now and give 3 suggested opening responses for me."
        } else {
            latestUserMessage
        }
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", promptText)))
            }
        )

        val requestBody = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
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
                    readTimeout = 20000
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
                    throw Exception("HTTP $code ($modelName):$errText")
                }

                val root = JSONObject(rawResponse)
                val rawText = root.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val parsed = JSONObject(rawText)
                val sugArr = parsed.optJSONArray("suggestions") ?: JSONArray()
                val suggestions = mutableListOf<GeminiSuggestion>()
                for (i in 0 until sugArr.length()) {
                    val item = sugArr.getJSONObject(i)
                    suggestions.add(
                        GeminiSuggestion(
                            zh = item.optString("zh", ""),
                            pinyin = item.optString("pinyin", ""),
                            th = item.optString("th", ""),
                            tag = item.optString("tag", "💬 แนะนำ")
                        )
                    )
                }

                return@withContext Result.success(
                    GeminiTurnResult(
                        replyZh = parsed.optString("reply_zh", "你好！我们开始吧。"),
                        replyPinyin = parsed.optString("reply_pinyin", "Nǐ hǎo! Wǒmen kāishǐ ba."),
                        replyTh = parsed.optString("reply_th", "สวัสดี! พวกเรามาเริ่มกันเลย"),
                        coachFeedbackTh = parsed.optString("coach_feedback_th", "✨ Gemini พร้อมสนทนาโต้ตอบแล้ว"),
                        accuracyPct = parsed.optInt("accuracy_pct", 90).coerceIn(50, 100),
                        stepPassed = parsed.optBoolean("step_passed", true),
                        suggestions = suggestions,
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
