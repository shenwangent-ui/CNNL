package com.shenhan.app.data

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.shenhan.app.BuildConfig
import com.shenhan.app.model.QuestNode
import com.shenhan.app.model.ScriptMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
    private const val TAG = "GeminiTutorService"
    private const val PREFS_NAME = "shenhan_gemini_prefs"
    private const val KEY_API = "gemini_api_key"
    private val MODEL_CANDIDATES = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")

    private fun readBuildConfigApiKey(): String {
        val raw = runCatching {
            BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
        }.getOrNull()?.trim().orEmpty()
        if (raw.isBlank() || raw == "MY_GEMINI_API_KEY" || raw == "null") return ""
        return raw
    }

    private fun readStringResourceApiKey(context: Context): String {
        val resId = context.resources.getIdentifier("gemini_api_key", "string", context.packageName)
        if (resId == 0) return ""
        val raw = runCatching { context.getString(resId).trim() }.getOrNull().orEmpty()
        if (raw.isBlank() || raw == "MY_GEMINI_API_KEY") return ""
        return raw
    }

    fun getApiKey(context: Context): String {
        val buildKey = readBuildConfigApiKey()
        if (buildKey.isNotBlank()) return buildKey

        val resKey = readStringResourceApiKey(context)
        if (resKey.isNotBlank()) return resKey

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
        val scriptLabel = if (scriptMode == ScriptMode.SIMPLIFIED) {
            "Simplified Chinese (简体中文)"
        } else {
            "Traditional Chinese (繁體中文 / 台灣常用)"
        }
        val nodeTitleZh = if (scriptMode == ScriptMode.SIMPLIFIED) node.titleZhSimp else node.titleZhTrad

        val grammarSummary = if (node.grammarNotes.isNotEmpty()) {
            node.grammarNotes.joinToString("; ") { "${it.pattern} (${it.explain})" }
        } else {
            "Practical daily Mandarin conversation"
        }

        val customVocabInstruction = if (node.zoneId == CustomVocabPackManager.CUSTOM_ZONE_ID) {
            "IMPORTANT: This is a user-imported vocabulary pack. Focus your roleplay and suggested answers around practicing the imported words listed above, helping the learner naturally use them in real sentences."
        } else {
            ""
        }

        val systemPrompt = """
            You are an expert native Mandarin Chinese roleplay partner and language coach inside the ShenHan app.
            Current Scenario: "${node.titleTh}" ($nodeTitleZh)
            Scenario Description: "${node.descriptionTh}"
            Target Grammar/Vocabulary: "$grammarSummary"
            Script Requirement: ALWAYS write all Chinese text in $scriptLabel.
            $customVocabInstruction

            Your job:
            1. Stay in character for this scenario and reply naturally in Mandarin Chinese.
            2. If latestUserMessage is null, start the scene with an engaging opening line and 3 helpful initial replies for the learner.
            3. If latestUserMessage is provided, evaluate the learner's response (even if they wrote partial Chinese, Pinyin, or Thai), give encouraging Thai coaching feedback, score accuracy (60-100), and continue the roleplay.
            4. Always provide exactly 3 context-aware next reply suggestions for the learner to choose from.

            Respond ONLY with valid JSON matching this exact schema:
            {
              "reply_zh": "Chinese reply in $scriptLabel",
              "reply_pinyin": "Pinyin with tone marks",
              "reply_th": "Natural Thai translation of reply_zh",
              "coach_feedback_th": "Short, practical Thai feedback on the user's message (or welcome tip on turn 0)",
              "accuracy_pct": 92,
              "step_passed": true,
              "suggestions": [
                {"zh": "Option 1 in $scriptLabel", "pinyin": "Pinyin 1", "th": "Thai 1", "tag": "ตรงประเด็น"},
                {"zh": "Option 2 in $scriptLabel", "pinyin": "Pinyin 2", "th": "Thai 2", "tag": "ถามต่อ"},
                {"zh": "Option 3 in $scriptLabel", "pinyin": "Pinyin 3", "th": "Thai 3", "tag": "สุภาพ"}
              ]
            }
        """.trimIndent()

        val promptText = if (latestUserMessage.isNullOrBlank()) {
            "Start this roleplay scenario now and give 3 suggested opening responses for me."
        } else {
            latestUserMessage
        }

        val apiKey = getApiKey(context)
        if (apiKey.isNotBlank()) {
            val restResult = generateViaRestApi(
                apiKey = apiKey,
                systemPrompt = systemPrompt,
                historyPairs = historyPairs,
                promptText = promptText
            )
            if (restResult.isSuccess) return@withContext restResult
        }

        // Fallback / keyless mode via Firebase AI Logic (no user-entered API key required)
        generateViaFirebaseAiLogic(
            systemPrompt = systemPrompt,
            historyPairs = historyPairs,
            promptText = promptText
        )
    }

    private fun generateViaRestApi(
        apiKey: String,
        systemPrompt: String,
        historyPairs: List<Pair<Boolean, String>>,
        promptText: String
    ): Result<GeminiTurnResult> {
        val contentsArray = JSONArray()
        for ((isUser, text) in historyPairs.takeLast(8)) {
            val role = if (isUser) "user" else "model"
            val msgObj = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", text)))
            }
            contentsArray.put(msgObj)
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
            var conn: HttpURLConnection? = null
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
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

                return Result.success(parseTurnJson(rawText, modelName))
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "REST Gemini call failed on $modelName: ${e.message}")
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(lastError)
    }

    private suspend fun generateViaFirebaseAiLogic(
        systemPrompt: String,
        historyPairs: List<Pair<Boolean, String>>,
        promptText: String
    ): Result<GeminiTurnResult> {
        val suggestionSchema = Schema.obj(
            properties = mapOf(
                "zh" to Schema.string(),
                "pinyin" to Schema.string(),
                "th" to Schema.string(),
                "tag" to Schema.string()
            )
        )
        val responseSchema = Schema.obj(
            properties = mapOf(
                "reply_zh" to Schema.string(),
                "reply_pinyin" to Schema.string(),
                "reply_th" to Schema.string(),
                "coach_feedback_th" to Schema.string(),
                "accuracy_pct" to Schema.integer(),
                "step_passed" to Schema.boolean(),
                "suggestions" to Schema.array(items = suggestionSchema)
            )
        )
        val config = generationConfig {
            temperature = 0.7f
            responseMimeType = "application/json"
            this.responseSchema = responseSchema
        }

        val chatHistory = historyPairs.takeLast(8).map { (isUser, text) ->
            content(role = if (isUser) "user" else "model") { text(text) }
        }

        var lastError: Exception = Exception("Firebase AI Logic request failed")
        for (modelName in MODEL_CANDIDATES) {
            try {
                val model = Firebase.ai.generativeModel(
                    modelName = modelName,
                    generationConfig = config,
                    systemInstruction = content { text(systemPrompt) }
                )
                val chat = model.startChat(history = chatHistory)
                val response = withTimeout(45_000L) {
                    chat.sendMessage(promptText)
                }
                val rawText = response.text ?: throw IllegalStateException("Empty AI response")
                return Result.success(parseTurnJson(rawText, modelName))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Firebase AI Logic failed on $modelName: ${e.message}")
            }
        }
        return Result.failure(lastError)
    }

    private fun parseTurnJson(rawTextInput: String, modelName: String): GeminiTurnResult {
        val rawText = rawTextInput.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
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
            val item = sugArr.optJSONObject(i) ?: continue
            val zh = item.optString("zh", "").trim()
            if (zh.isNotEmpty()) {
                suggestions.add(
                    GeminiSuggestion(
                        zh = zh,
                        pinyin = item.optString("pinyin", ""),
                        th = item.optString("th", ""),
                        tag = item.optString("tag", "แนะนำ")
                    )
                )
            }
        }

        return GeminiTurnResult(
            replyZh = parsed.optString("reply_zh", "你好！我们开始吧。"),
            replyPinyin = parsed.optString("reply_pinyin", "Nǐ hǎo! Wǒmen kāishǐ ba."),
            replyTh = parsed.optString("reply_th", "สวัสดี! พวกเรามาเริ่มกันเลย"),
            coachFeedbackTh = parsed.optString("coach_feedback_th", "Gemini พร้อมสนทนาโต้ตอบแล้ว"),
            accuracyPct = parsed.optInt("accuracy_pct", 90).coerceIn(50, 100),
            stepPassed = parsed.optBoolean("step_passed", true),
            suggestions = suggestions.take(3),
            usedModel = modelName
        )
    }
}
