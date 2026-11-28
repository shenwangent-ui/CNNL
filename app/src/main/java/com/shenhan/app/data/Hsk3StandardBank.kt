package com.shenhan.app.data

import android.content.Context
import com.shenhan.app.model.HskLevel
import com.shenhan.app.model.SrsMastery
import com.shenhan.app.model.VocabWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class SurvivalComboSentence(
    val sourceLevelsLabel: String,
    val simplified: String,
    val traditional: String,
    val pinyin: String,
    val meaningTh: String,
    val vocabBreakdown: String
)

object Hsk3StandardBank {
    private const val PREFS_NAME = "shenhan_remote_curriculum_prefs"
    private const val KEY_BACKEND_BASE_URL = "backend_curriculum_base_url"
    private const val KEY_LAST_SYNC_MS = "last_curriculum_sync_ms"

    const val DEFAULT_BACKEND_BASE_URL =
        "https://raw.githubusercontent.com/shenwangent-ui/CNNL/main/curriculum"

    @Volatile
    private var cachedMergedVocab: List<VocabWord>? = null

    fun invalidateCache() {
        cachedMergedVocab = null
    }

    fun getBackendBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BACKEND_BASE_URL, DEFAULT_BACKEND_BASE_URL)
            ?.trimEnd('/')
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_BACKEND_BASE_URL
    }

    fun setBackendBaseUrl(context: Context, url: String) {
        val cleaned = url.trim().trimEnd('/')
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_BACKEND_BASE_URL, cleaned).apply()
        invalidateCache()
    }

    suspend fun syncRemoteCurriculum(context: Context): Int = withContext(Dispatchers.IO) {
        val baseUrl = getBackendBaseUrl(context)
        val cacheDir = File(context.filesDir, "remote_curriculum").apply { mkdirs() }
        var downloadedFiles = 0

        for (lvl in 1..6) {
            try {
                val url = URL("$baseUrl/hsk$lvl.json")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "ShenHan-CurriculumSync")
                    connectTimeout = 8000
                    readTimeout = 12000
                }
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val arr = JSONArray(body)
                    if (arr.length() > 0) {
                        File(cacheDir, "hsk$lvl.json").writeText(body, Charsets.UTF_8)
                        downloadedFiles++
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (downloadedFiles > 0) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_SYNC_MS, System.currentTimeMillis()).apply()
            invalidateCache()
        }
        getAllVocab(context).size
    }

    private fun parseJsonVocab(jsonStr: String, fallbackLevel: Int = 1): List<VocabWord> {
        val list = mutableListOf<VocabWord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val lvlNum = obj.optInt("level", fallbackLevel)
                val hskLvl = when (lvlNum) {
                    1 -> HskLevel.HSK1
                    2 -> HskLevel.HSK2
                    3 -> HskLevel.HSK3
                    4 -> HskLevel.HSK4
                    5 -> HskLevel.HSK5
                    else -> HskLevel.HSK6
                }
                val simp = obj.optString("simplified", "").trim()
                if (simp.isBlank()) continue
                val trad = obj.optString("traditional", simp).ifBlank { simp }

                list.add(
                    VocabWord(
                        id = obj.optString("id", "hsk3_${lvlNum}_$i"),
                        hskLevel = hskLvl,
                        simplified = simp,
                        traditional = trad,
                        pinyin = obj.optString("pinyin", ""),
                        toneNumbers = listOf(1),
                        partOfSpeech = obj.optString("pos", "n"),
                        meaningTh = obj.optString("meaningTh", ""),
                        meaningEn = obj.optString("meaningEn", ""),
                        exampleSentenceSimp = obj.optString("exampleSimp", ""),
                        exampleSentenceTrad = obj.optString("exampleTrad", ""),
                        examplePinyin = obj.optString("examplePinyin", ""),
                        exampleMeaningTh = obj.optString("exampleTh", ""),
                        exampleMeaningEn = obj.optString("exampleEn", ""),
                        mastery = SrsMastery.NEW
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }

    fun getAllVocab(context: Context): List<VocabWord> {
        cachedMergedVocab?.let { return it }

        val bundledFromAssets = try {
            val jsonStr = context.assets.open("hsk3_vocab.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseJsonVocab(jsonStr, 1)
        } catch (_: Exception) {
            emptyList()
        }

        val remoteCached = mutableListOf<VocabWord>()
        val cacheDir = File(context.filesDir, "remote_curriculum")
        if (cacheDir.exists()) {
            for (lvl in 1..6) {
                val f = File(cacheDir, "hsk$lvl.json")
                if (f.exists()) {
                    remoteCached.addAll(parseJsonVocab(f.readText(Charsets.UTF_8), lvl))
                }
            }
        }

        val seenSimplified = HashSet<String>()
        val combined = mutableListOf<VocabWord>()
        for (w in SampleData.vocabList + bundledFromAssets + remoteCached) {
            if (w.simplified.isNotBlank() && seenSimplified.add(w.simplified)) {
                combined.add(w)
            }
        }
        cachedMergedVocab = combined
        return combined
    }

    fun getCumulativeVocabUpTo(context: Context, targetLevel: HskLevel): List<VocabWord> {
        return getAllVocab(context).filter { it.hskLevel.level <= targetLevel.level }
    }

    fun getSurvivalComboSentences(targetLevel: HskLevel): List<SurvivalComboSentence> {
        return when (targetLevel) {
            HskLevel.HSK1 -> listOf(
                SurvivalComboSentence(
                    sourceLevelsLabel = "พื้นฐาน HSK 1 เอาตัวรอดทันที",
                    simplified = "你好，请问去火车站怎么走？",
                    traditional = "你好，請問去火車站怎麼走？",
                    pinyin = "Nǐ hǎo, qǐngwèn qù huǒchēzhàn zěnme zǒu?",
                    meaningTh = "สวัสดีครับ/ค่ะ ขอถามหน่อยว่าไปสถานีรถไฟเดินไปทางไหน?",
                    vocabBreakdown = "你好 (สวัสดี) + 请问 (ขอถาม) + 去 (ไป) + 火车站 (สถานีรถไฟ) + 怎么走 (เดินไปอย่างไร)"
                ),
                SurvivalComboSentence(
                    sourceLevelsLabel = "พื้นฐาน HSK 1 ซื้อของ & ถามราคา",
                    simplified = "我要一碗米饭和一杯水，一共多少钱？",
                    traditional = "我要一碗米飯和一杯水，一共多少錢？",
                    pinyin = "Wǒ yào yì wǎn mǐfàn hé yì bēi shuǐ, yígòng duōshao qián?",
                    meaningTh = "ฉันขอข้าวสวย 1 ถ้วยกับน้ำเปล่า 1 แก้ว ทั้งหมดราคาเท่าไหร่?",
                    vocabBreakdown = "我 (ฉัน) + 要 (เอา/ต้องการ) + 米饭 (ข้าวสวย) + 水 (น้ำ) + 多少钱 (เท่าไหร่)"
                )
            )
            HskLevel.HSK2 -> listOf(
                SurvivalComboSentence(
                    sourceLevelsLabel = "ผสม HSK 1 + HSK 2 (เอาตัวรอดการเดินทาง)",
                    simplified = "因为今天下雨，所以我打算坐地铁去机场。",
                    traditional = "因為今天下雨，所以我打算坐地鐵去機場。",
                    pinyin = "Yīnwèi jīntiān xiàyǔ, suǒyǐ wǒ dǎsuàn zuò dìtiě qù jīchǎng.",
                    meaningTh = "เพราะวันนี้ฝนตก ดังนั้นฉันจึงวางแผนว่าจะนั่งรถไฟใต้ดินไปสนามบิน",
                    vocabBreakdown = "[HSK1] 今天 (วันนี้), 去 (ไป) + [HSK2] 因为…所以 (เพราะ…ดังนั้น), 地铁 (รถไฟใต้ดิน), 机场 (สนามบิน)"
                ),
                SurvivalComboSentence(
                    sourceLevelsLabel = "ผสม HSK 1 + HSK 2 (โรงพยาบาล & สุขภาพ)",
                    simplified = "我朋友生病了，请问医院在左边还是右边？",
                    traditional = "我朋友生病了，請問醫院在左邊還是右邊？",
                    pinyin = "Wǒ péngyou shēngbìng le, qǐngwèn yīyuàn zài zuǒbiān háishi yòubiān?",
                    meaningTh = "เพื่อนของฉันไม่สบาย ขอถามหน่อยว่าโรงพยาบาลอยู่ทางซ้ายหรือทางขวา?",
                    vocabBreakdown = "[HSK1] 朋友 (เพื่อน), 医院 (โรงพยาบาล), 在 (อยู่) + [HSK2] 生病 (ป่วย), 左边/右边 (ซ้าย/ขวา)"
                )
            )
            HskLevel.HSK3 -> listOf(
                SurvivalComboSentence(
                    sourceLevelsLabel = "ผสม HSK 1 + 2 + 3 (แก้ปัญหาต่างแดน)",
                    simplified = "别担心，虽然行李箱找不到了，但是我们有护照，经理会帮我们解决。",
                    traditional = "別擔心，雖然行李箱找不到了，但是我們有護照，經理會幫我們解決。",
                    pinyin = "Bié dānxīn, suīrán xínglǐxiāng zhǎo bú dào le, dànshì wǒmen yǒu hùzhào, jīnglǐ huì bāng wǒmen jiějué.",
                    meaningTh = "ไม่ต้องกังวล ถึงแม้กระเป๋าเดินทางจะหาไม่เจอ แต่พวกเรามีพาสปอร์ต ผู้จัดการจะช่วยพวกเราแก้ไขปัญหา",
                    vocabBreakdown = "[HSK1] 我们 (พวกเรา), 有 (มี) + [HSK2] 帮 (ช่วย) + [HSK3] 担心 (กังวล), 行李箱 (กระเป๋าเดินทาง), 护照 (พาสปอร์ต), 解决 (แก้ไข)"
                )
            )
            HskLevel.HSK4 -> listOf(
                SurvivalComboSentence(
                    sourceLevelsLabel = "ผสม HSK 1–3 + HSK 4 (เจรจาธุรกิจ)",
                    simplified = "虽然市场竞争很激烈，但是只要我们保证产品质量，客户一定会同意签合同。",
                    traditional = "雖然市場競爭很激烈，但是只要我們保證產品質量，客戶一定會同意簽合同。",
                    pinyin = "Suīrán shìchǎng jìngzhēng hěn jīliè, dànshì zhǐyào wǒmen bǎozhèng chǎnpǐn zhìliàng, kèhù yídìng huì tóngyì qiān hétong.",
                    meaningTh = "ถึงแม้การแข่งขันในตลาดจะรุนแรง แต่ขอเพียงพวกเรารับประกันคุณภาพสินค้า ลูกค้าย่อมตกลงเซ็นสัญญาแน่นอน",
                    vocabBreakdown = "[HSK1-3] 我们 (พวกเรา), 同意 (เห็นด้วย) + [HSK4] 竞争 (แข่งขัน), 质量 (คุณภาพ), 合同 (สัญญา)"
                )
            )
            HskLevel.HSK5 -> listOf(
                SurvivalComboSentence(
                    sourceLevelsLabel = "ผสม HSK 1–4 + HSK 5 (บริหาร & กลยุทธ์)",
                    simplified = "为了适应数字化趋势，公司决定投资新技术并调整营销策略，以提高整体效率。",
                    traditional = "為了適應數字化趨勢，公司決定投資新技術並調整營銷策略，以提高整體效率。",
                    pinyin = "Wèile shìyìng shùzìhuà qūshì, gōngsī juédìng tóuzī xīn jìshù bìng tiáozhěng yíngxiāo cèlüè, yǐ tígāo zhěngtǐ xiàolǜ.",
                    meaningTh = "เพื่อปรับตัวให้เข้ากับแนวโน้มดิจิทัล บริษัทจึงตัดสินใจลงทุนเทคโนโลยีใหม่และปรับกลยุทธ์การตลาดเพื่อยกระดับประสิทธิภาพโดยรวม",
                    vocabBreakdown = "[HSK1-4] 决定 (ตัดสินใจ), 适应 (ปรับตัว) + [HSK5] 趋势 (แนวโน้ม), 投资 (ลงทุน), 策略 (กลยุทธ์), 效率 (ประสิทธิภาพ)"
                )
            )
            HskLevel.HSK6 -> listOf(
                SurvivalComboSentence(
                    sourceLevelsLabel = "ผสม HSK 1–5 + HSK 6 (ระดับมืออาชีพ & สำนวน)",
                    simplified = "在复杂的商业环境中，只有洞察用户需求、权衡利弊并精益求精，才能脱颖而出。",
                    traditional = "在複雜的商業環境中，只有洞察用戶需求、權衡利弊並精益求精，才能脫穎而出。",
                    pinyin = "Zài fùzá de shāngyè huánjìng zhōng, zhǐyǒu dòngchá yònghù xūqiú, quánhéng lìbì bìng jīng yì qiú jīng, cái néng tuō yǐng ér chū.",
                    meaningTh = "ท่ามกลางสภาพแวดล้อมทางธุรกิจที่ซับซ้อน มีเพียงการเข้าใจความต้องการของผู้ใช้อย่างลึกซึ้ง ชั่งน้ำหนักข้อดีข้อเสีย และพัฒนาไม่หยุดยั้ง จึงจะโดดเด่นเหนือใครได้",
                    vocabBreakdown = "[HSK1-5] 环境 (สภาพแวดล้อม), 需求 (ความต้องการ) + [HSK6] 洞察 (หยั่งรู้), 权衡 (ชั่งน้ำหนัก), 精益求精 (พัฒนาไม่หยุดยั้ง), 脱颖而出 (โดดเด่น)"
                )
            )
        }
    }
}
