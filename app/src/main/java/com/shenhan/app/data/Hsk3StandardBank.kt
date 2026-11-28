package com.shenhan.app.data

import android.content.Context
import com.shenhan.app.model.HskLevel
import com.shenhan.app.model.VocabItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object Hsk3StandardBank {
    private const val CACHE_FILE_NAME = "hsk3_vocab_synced.json"
    private const val PREFS_NAME = "shenhan_vocab_sync"
    private const val KEY_VERSION = "vocab_version"

    // ใส่ URL ของไฟล์ vocab_manifest.json บนหลังบ้าน (GitHub Raw / Firebase Storage / Cloudflare R2)
    var remoteManifestUrl: String = ""

    @Volatile
    private var cachedList: List<VocabItem>? = null

    fun loadAll(context: Context): List<VocabItem> {
        cachedList?.let { return it }

        val syncedFile = File(context.filesDir, CACHE_FILE_NAME)
        val rawJson = if (syncedFile.exists() && syncedFile.length() > 100) {
            syncedFile.readText(Charsets.UTF_8)
        } else {
            context.assets.open("hsk3_vocab.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }

        val parsed = parseJson(rawJson)
        cachedList = parsed
        return parsed
    }

    /**
     * เรียกตอนเปิดแอป: เช็กเวอร์ชันไฟล์คำศัพท์บนหลังบ้าน ถ้ามีเวอร์ชันใหม่จะโหลดมาแคชลงเครื่องทันที
     */
    suspend fun syncFromBackend(context: Context, manifestUrl: String = remoteManifestUrl): Boolean =
        withContext(Dispatchers.IO) {
            if (manifestUrl.isBlank()) return@withContext false
            try {
                val manifestText = fetchUrl(manifestUrl) ?: return@withContext false
                val manifest = JSONObject(manifestText)
                val remoteVersion = manifest.optInt("version", 1)
                val vocabUrl = manifest.optString("vocabUrl", "")
                if (vocabUrl.isBlank()) return@withContext false

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val localVersion = prefs.getInt(KEY_VERSION, 1)
                val syncedFile = File(context.filesDir, CACHE_FILE_NAME)

                if (remoteVersion > localVersion || !syncedFile.exists()) {
                    val newVocabJson = fetchUrl(vocabUrl) ?: return@withContext false
                    val parsed = parseJson(newVocabJson)
                    if (parsed.isNotEmpty()) {
                        syncedFile.writeText(newVocabJson, Charsets.UTF_8)
                        prefs.edit().putInt(KEY_VERSION, remoteVersion).apply()
                        cachedList = parsed
                        return@withContext true
                    }
                }
                false
            } catch (_: Exception) {
                false
            }
        }

    private fun fetchUrl(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 15000
            requestMethod = "GET"
        }
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun parseJson(rawJson: String): List<VocabItem> {
        val arr = JSONArray(rawJson)
        val result = ArrayList<VocabItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val lvl = when (obj.optInt("level", 1)) {
                1 -> HskLevel.HSK1
                2 -> HskLevel.HSK2
                3 -> HskLevel.HSK3
                4 -> HskLevel.HSK4
                5 -> HskLevel.HSK5
                else -> HskLevel.HSK6
            }
            result.add(
                VocabItem(
                    id = obj.optString("id", "std_$i"),
                    hskLevel = lvl,
                    simplified = obj.optString("simplified", ""),
                    traditional = obj.optString("traditional", obj.optString("simplified", "")),
                    pinyin = obj.optString("pinyin", ""),
                    partOfSpeech = obj.optString("pos", ""),
                    meaningTh = obj.optString("meaningTh", ""),
                    meaningEn = obj.optString("meaningEn", ""),
                    exampleSimplified = obj.optString("exampleSimp", ""),
                    exampleTraditional = obj.optString("exampleTrad", ""),
                    examplePinyin = obj.optString("examplePinyin", ""),
                    exampleMeaningTh = obj.optString("exampleTh", "")
                )
            )
        }
        return result
    }
}
