package com.shenhan.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.shenhan.app.model.GrammarNote
import com.shenhan.app.model.HskLevel
import com.shenhan.app.model.LessonDialogueLine
import com.shenhan.app.model.QuestNode
import com.shenhan.app.model.QuestNodeType
import com.shenhan.app.model.QuestZone
import com.shenhan.app.model.SrsMastery
import com.shenhan.app.model.VocabWord
import org.json.JSONArray
import org.json.JSONObject

data class CustomVocabEntry(
    val simplified: String,
    val traditional: String,
    val pinyin: String,
    val meaningTh: String,
    val exampleZhSimp: String,
    val exampleZhTrad: String,
    val examplePinyin: String,
    val exampleMeaningTh: String,
    val partOfSpeech: String = "ศัพท์นำเข้า"
)

data class CustomVocabPack(
    val id: String,
    val name: String,
    val entries: List<CustomVocabEntry>,
    val createdAt: Long
)

object CustomVocabPackManager {
    private const val PREFS_NAME = "shenhan_custom_vocab_prefs"
    private const val KEY_PACKS_JSON = "custom_vocab_packs_v1"
    const val CUSTOM_ZONE_ID = "zone_custom_pack"
    const val CUSTOM_NODE_PREFIX = "custom_node_"
    const val CUSTOM_WORD_PREFIX = "custom_"

    val SAMPLE_IMPORT_TEXT = """
        你好,Nǐ hǎo,สวัสดี,你好，请问这个多少钱？,สวัสดี ขอถามหน่อยว่าอันนี้ราคาเท่าไหร่?
        谢谢,Xièxie,ขอบคุณ,非常谢谢你的帮忙！,ขอบคุณมากสำหรับความช่วยเหลือของคุณ!
        喝水,Hē shuǐ,ดื่มน้ำ,每天要多喝水。,ทุกวันต้องดื่มน้ำให้มากๆ
        好吃,Hǎochī,อร่อย,这家店的牛肉面很好吃。,ก๋วยเตี๋ยวเนื้อร้านนี้อร่อยมาก
        开心,Kāixīn,มีความสุข,今天学习中文很开心。,วันนี้เรียนภาษาจีนแล้วมีความสุขมาก
        朋友,Péngyou,เพื่อน,他是我最好的朋友。,เขาเป็นเพื่อนสนิทที่สุดของฉัน
    """.trimIndent()

    fun loadPacks(context: Context): List<CustomVocabPack> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PACKS_JSON, null)?.takeIf { it.isNotBlank() } ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val result = mutableListOf<CustomVocabPack>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val name = obj.optString("name", "ชุดคำศัพท์").trim()
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val entriesArr = obj.optJSONArray("entries") ?: JSONArray()
                val entries = mutableListOf<CustomVocabEntry>()
                for (j in 0 until entriesArr.length()) {
                    val e = entriesArr.optJSONObject(j) ?: continue
                    val simp = e.optString("simplified", "").trim()
                    if (simp.isEmpty()) continue
                    val trad = e.optString("traditional", simp).ifBlank { simp }
                    entries.add(
                        CustomVocabEntry(
                            simplified = simp,
                            traditional = trad,
                            pinyin = e.optString("pinyin", ""),
                            meaningTh = e.optString("meaningTh", "ฝึกคำศัพท์"),
                            exampleZhSimp = e.optString("exampleZhSimp", simp),
                            exampleZhTrad = e.optString("exampleZhTrad", trad),
                            examplePinyin = e.optString("examplePinyin", ""),
                            exampleMeaningTh = e.optString("exampleMeaningTh", ""),
                            partOfSpeech = e.optString("partOfSpeech", "ศัพท์นำเข้า")
                        )
                    )
                }
                if (id.isNotEmpty() && entries.isNotEmpty()) {
                    result.add(CustomVocabPack(id = id, name = name, entries = entries, createdAt = createdAt))
                }
            }
            result
        }.getOrDefault(emptyList())
    }

    fun savePacks(context: Context, packs: List<CustomVocabPack>) {
        val arr = JSONArray()
        packs.forEach { pack ->
            val entriesArr = JSONArray()
            pack.entries.forEach { entry ->
                entriesArr.put(
                    JSONObject().apply {
                        put("simplified", entry.simplified)
                        put("traditional", entry.traditional)
                        put("pinyin", entry.pinyin)
                        put("meaningTh", entry.meaningTh)
                        put("exampleZhSimp", entry.exampleZhSimp)
                        put("exampleZhTrad", entry.exampleZhTrad)
                        put("examplePinyin", entry.examplePinyin)
                        put("exampleMeaningTh", entry.exampleMeaningTh)
                        put("partOfSpeech", entry.partOfSpeech)
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", pack.id)
                    put("name", pack.name)
                    put("createdAt", pack.createdAt)
                    put("entries", entriesArr)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PACKS_JSON, arr.toString())
            .apply()
    }

    fun readFileTextAndName(context: Context, uri: Uri): Result<Pair<String, String>> {
        return runCatching {
            var fileName = "คำศัพท์นำเข้า"
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val rawName = cursor.getString(idx)?.trim().orEmpty()
                        if (rawName.isNotEmpty()) {
                            fileName = rawName.substringBeforeLast('.').ifBlank { rawName }
                        }
                    }
                }
            }
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            } ?: throw IllegalStateException("ไม่สามารถเปิดอ่านไฟล์ที่เลือกได้")
            Pair(fileName, text)
        }
    }

    fun parsePackFromText(packName: String, rawInput: String): Result<CustomVocabPack> {
        val cleaned = rawInput
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()

        if (cleaned.isEmpty()) {
            return Result.failure(IllegalArgumentException("ไฟล์หรือข้อความว่างเปล่า กรุณาเพิ่มคำศัพท์อย่างน้อย 1 คำ"))
        }

        val parsedPair = if (cleaned.startsWith("{") || cleaned.startsWith("[")) {
            parseJsonVocab(cleaned)
        } else {
            Pair(null, parseDelimitedVocab(cleaned))
        }

        val resolvedName = packName.trim().ifBlank { parsedPair.first ?: "ไฟล์คำศัพท์ของฉัน" }
        val entries = parsedPair.second.take(500)
        if (entries.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    "ไม่พบคำศัพท์ที่อ่านได้ กรุณาใช้รูปแบบ เช่น: คำจีน, พินอิน, ความหมาย (รองรับ CSV, TSV, TXT, JSON)"
                )
            )
        }

        return Result.success(
            CustomVocabPack(
                id = "pack_${System.currentTimeMillis()}",
                name = resolvedName,
                entries = entries,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun parseJsonVocab(rawJson: String): Pair<String?, List<CustomVocabEntry>> {
        var detectedName: String? = null
        val jsonArray = runCatching {
            if (rawJson.startsWith("[")) {
                JSONArray(rawJson)
            } else {
                val root = JSONObject(rawJson)
                detectedName = root.optString("name", root.optString("title", "")).takeIf { it.isNotBlank() }
                root.optJSONArray("words")
                    ?: root.optJSONArray("entries")
                    ?: root.optJSONArray("items")
                    ?: root.optJSONArray("vocab")
                    ?: root.optJSONArray("data")
                    ?: JSONArray()
            }
        }.getOrNull() ?: return Pair(null, emptyList())

        val list = mutableListOf<CustomVocabEntry>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.opt(i)
            when (item) {
                is JSONObject -> {
                    val simp = firstNonBlank(
                        item, "simplified", "zh", "hanzi", "word", "term", "chinese", "hans", "front"
                    ) ?: continue
                    val trad = firstNonBlank(item, "traditional", "hant", "trad") ?: simp
                    val pinyin = firstNonBlank(item, "pinyin", "py", "pronunciation", "reading") ?: ""
                    val meaning = firstNonBlank(
                        item, "meaningTh", "meaning_th", "th", "meaning", "translation", "definition", "back", "en"
                    ) ?: "ฝึกคำศัพท์ภาษาจีน"
                    val exZh = firstNonBlank(
                        item, "exampleZhSimp", "example_zh", "exampleZh", "example", "sentence"
                    ) ?: simp
                    val exTrad = firstNonBlank(item, "exampleZhTrad", "example_trad") ?: exZh
                    val exPy = firstNonBlank(item, "examplePinyin", "example_pinyin") ?: pinyin
                    val exTh = firstNonBlank(
                        item, "exampleMeaningTh", "example_th", "exampleTh"
                    ) ?: meaning
                    val pos = firstNonBlank(item, "partOfSpeech", "pos", "category") ?: "ศัพท์นำเข้า"
                    list.add(
                        CustomVocabEntry(
                            simplified = simp,
                            traditional = trad,
                            pinyin = pinyin,
                            meaningTh = meaning,
                            exampleZhSimp = exZh,
                            exampleZhTrad = exTrad,
                            examplePinyin = exPy,
                            exampleMeaningTh = exTh,
                            partOfSpeech = pos
                        )
                    )
                }
                is String -> {
                    val word = item.trim()
                    if (word.isNotEmpty()) {
                        list.add(
                            CustomVocabEntry(
                                simplified = word,
                                traditional = word,
                                pinyin = "",
                                meaningTh = "ฝึกคัด & ทบทวนคำศัพท์",
                                exampleZhSimp = word,
                                exampleZhTrad = word,
                                examplePinyin = "",
                                exampleMeaningTh = "คำศัพท์จากไฟล์นำเข้า"
                            )
                        )
                    }
                }
            }
        }
        return Pair(detectedName, list)
    }

    private fun firstNonBlank(obj: JSONObject, vararg keys: String): String? {
        for (k in keys) {
            val v = obj.optString(k, "").trim()
            if (v.isNotEmpty() && v != "null") return v
        }
        return null
    }

    private fun parseDelimitedVocab(text: String): List<CustomVocabEntry> {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }
        if (lines.isEmpty()) return emptyList()

        val firstCells = splitVocabLine(lines.first())
        val hasHeader = isLikelyHeaderRow(firstCells)
        val headerCols = if (hasHeader) firstCells.map { it.lowercase().trim() } else emptyList()
        val dataLines = if (hasHeader) lines.drop(1) else lines

        val entries = mutableListOf<CustomVocabEntry>()
        for (line in dataLines) {
            val cells = splitVocabLine(line)
            if (cells.isEmpty()) continue
            val entry = if (headerCols.isNotEmpty()) {
                mapCellsWithHeader(headerCols, cells)
            } else {
                mapCellsAuto(cells)
            }
            if (entry != null && entry.simplified.isNotBlank()) {
                entries.add(entry)
            }
        }
        return entries
    }

    private fun splitVocabLine(line: String): List<String> {
        return when {
            line.contains('\t') -> line.split('\t').map { it.trim().trim('"') }
            line.contains(',') -> parseCsvCells(line)
            line.contains('|') -> line.split('|').map { it.trim() }
            line.contains(';') -> line.split(';').map { it.trim() }
            line.contains(" - ") -> line.split(" - ").map { it.trim() }
            line.contains(" : ") -> line.split(" : ").map { it.trim() }
            line.contains(":") && !line.contains("http") -> line.split(':').map { it.trim() }
            else -> listOf(line.trim())
        }
    }

    private fun parseCsvCells(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    private fun isLikelyHeaderRow(cells: List<String>): Boolean {
        val first = cells.firstOrNull()?.lowercase()?.trim().orEmpty()
        val knownHeaders = setOf(
            "hanzi", "simplified", "simp", "zh", "word", "chinese", "hans", "character",
            "term", "vocab", "คำจีน", "ตัวอักษร", "ศัพท์"
        )
        return first in knownHeaders
    }

    private fun containsCjk(text: String): Boolean {
        return text.any { ch ->
            val code = ch.code
            code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF
        }
    }

    private fun mapCellsWithHeader(headers: List<String>, cells: List<String>): CustomVocabEntry? {
        fun getByAliases(vararg aliases: String): String {
            for (alias in aliases) {
                val idx = headers.indexOfFirst { it == alias || it.contains(alias) }
                if (idx in cells.indices && cells[idx].isNotBlank()) {
                    return cells[idx].trim()
                }
            }
            return ""
        }

        val simp = getByAliases("simplified", "hanzi", "hans", "zh", "word", "chinese", "term", "คำจีน", "ตัวอักษร")
            .ifBlank { cells.firstOrNull()?.trim().orEmpty() }
        if (simp.isBlank()) return null

        val trad = getByAliases("traditional", "hant", "trad").ifBlank { simp }
        val pinyin = getByAliases("pinyin", "py", "pronunciation", "พินอิน")
        val meaning = getByAliases("meaning_th", "meaning", "th", "thai", "translation", "definition", "en", "ความหมาย", "คำแปล")
            .ifBlank { "ฝึกคำศัพท์ภาษาจีน" }
        val exZh = getByAliases("example_zh", "example", "sentence", "ตัวอย่าง").ifBlank { simp }
        val exTrad = getByAliases("example_trad").ifBlank { exZh }
        val exPy = getByAliases("example_pinyin").ifBlank { pinyin }
        val exTh = getByAliases("example_th", "example_meaning", "คำแปลตัวอย่าง").ifBlank { meaning }
        val pos = getByAliases("pos", "part", "category", "หมวด").ifBlank { "ศัพท์นำเข้า" }

        return CustomVocabEntry(
            simplified = simp,
            traditional = trad,
            pinyin = pinyin,
            meaningTh = meaning,
            exampleZhSimp = exZh,
            exampleZhTrad = exTrad,
            examplePinyin = exPy,
            exampleMeaningTh = exTh,
            partOfSpeech = pos
        )
    }

    private fun mapCellsAuto(cells: List<String>): CustomVocabEntry? {
        val clean = cells.map { it.trim() }.filter { it.isNotEmpty() }
        if (clean.isEmpty()) return null
        val c0 = clean[0]

        return when (clean.size) {
            1 -> CustomVocabEntry(
                simplified = c0,
                traditional = c0,
                pinyin = "",
                meaningTh = "ฝึกคัด & ทบทวนคำศัพท์",
                exampleZhSimp = c0,
                exampleZhTrad = c0,
                examplePinyin = "",
                exampleMeaningTh = "คำศัพท์จากไฟล์นำเข้า"
            )
            2 -> {
                val c1 = clean[1]
                CustomVocabEntry(
                    simplified = c0,
                    traditional = c0,
                    pinyin = "",
                    meaningTh = c1,
                    exampleZhSimp = c0,
                    exampleZhTrad = c0,
                    examplePinyin = "",
                    exampleMeaningTh = c1
                )
            }
            3 -> {
                val c1 = clean[1]
                val c2 = clean[2]
                if (containsCjk(c1)) {
                    CustomVocabEntry(
                        simplified = c0,
                        traditional = c1,
                        pinyin = "",
                        meaningTh = c2,
                        exampleZhSimp = c0,
                        exampleZhTrad = c1,
                        examplePinyin = "",
                        exampleMeaningTh = c2
                    )
                } else {
                    CustomVocabEntry(
                        simplified = c0,
                        traditional = c0,
                        pinyin = c1,
                        meaningTh = c2,
                        exampleZhSimp = c0,
                        exampleZhTrad = c0,
                        examplePinyin = c1,
                        exampleMeaningTh = c2
                    )
                }
            }
            4 -> {
                if (containsCjk(clean[1])) {
                    // [simp, trad, pinyin, meaning]
                    CustomVocabEntry(
                        simplified = c0,
                        traditional = clean[1],
                        pinyin = clean[2],
                        meaningTh = clean[3],
                        exampleZhSimp = c0,
                        exampleZhTrad = clean[1],
                        examplePinyin = clean[2],
                        exampleMeaningTh = clean[3]
                    )
                } else {
                    // [simp, pinyin, meaning, exampleZh]
                    CustomVocabEntry(
                        simplified = c0,
                        traditional = c0,
                        pinyin = clean[1],
                        meaningTh = clean[2],
                        exampleZhSimp = clean[3],
                        exampleZhTrad = clean[3],
                        examplePinyin = clean[1],
                        exampleMeaningTh = clean[2]
                    )
                }
            }
            else -> {
                if (containsCjk(clean[1]) && !containsCjk(clean[2])) {
                    // [simp, trad, pinyin, meaning, exampleZh, (exampleTh)]
                    val exZh = clean.getOrElse(4) { c0 }
                    val exTh = clean.getOrElse(5) { clean[3] }
                    CustomVocabEntry(
                        simplified = c0,
                        traditional = clean[1],
                        pinyin = clean[2],
                        meaningTh = clean[3],
                        exampleZhSimp = exZh,
                        exampleZhTrad = exZh,
                        examplePinyin = clean[2],
                        exampleMeaningTh = exTh
                    )
                } else {
                    // [simp, pinyin, meaning, exampleZh, exampleTh]
                    CustomVocabEntry(
                        simplified = c0,
                        traditional = c0,
                        pinyin = clean[1],
                        meaningTh = clean[2],
                        exampleZhSimp = clean[3],
                        exampleZhTrad = clean[3],
                        examplePinyin = clean[1],
                        exampleMeaningTh = clean.getOrElse(4) { clean[2] }
                    )
                }
            }
        }
    }

    fun toVocabWords(packs: List<CustomVocabPack>): List<VocabWord> {
        val list = mutableListOf<VocabWord>()
        packs.forEach { pack ->
            pack.entries.forEachIndexed { idx, entry ->
                val estimatedStrokes = (entry.simplified.count { containsCjk(it.toString()) }.coerceAtLeast(1) * 6).coerceIn(4, 28)
                list.add(
                    VocabWord(
                        id = "${CUSTOM_WORD_PREFIX}${pack.id}_$idx",
                        hskLevel = HskLevel.HSK1,
                        simplified = entry.simplified,
                        traditional = entry.traditional,
                        pinyin = entry.pinyin.ifBlank { entry.simplified },
                        toneNumbers = listOf(1),
                        partOfSpeech = "ไฟล์: ${pack.name}",
                        meaningTh = entry.meaningTh,
                        meaningEn = entry.meaningTh,
                        exampleSentenceSimp = entry.exampleZhSimp.ifBlank { entry.simplified },
                        exampleSentenceTrad = entry.exampleZhTrad.ifBlank { entry.traditional },
                        examplePinyin = entry.examplePinyin.ifBlank { entry.pinyin },
                        exampleMeaningTh = entry.exampleMeaningTh.ifBlank { entry.meaningTh },
                        exampleMeaningEn = entry.exampleMeaningTh.ifBlank { entry.meaningTh },
                        strokeCount = estimatedStrokes,
                        strokeOrderTipTh = "คัดจากบนลงล่าง ซ้ายไปขวา ทีละอักษร",
                        srsStage = 0,
                        mastery = SrsMastery.NEW,
                        isBookmarked = false
                    )
                )
            }
        }
        return list
    }

    fun buildCustomQuestZone(packs: List<CustomVocabPack>): QuestZone? {
        if (packs.isEmpty()) return null
        val nodes = packs.map { pack ->
            val previewZhSimp = pack.entries.take(3).joinToString(" · ") { it.simplified }
            val previewZhTrad = pack.entries.take(3).joinToString(" · ") { it.traditional }
            val vocabPromptSummary = pack.entries.take(15).joinToString(", ") { e ->
                if (e.pinyin.isNotBlank()) "${e.simplified} (${e.pinyin}) = ${e.meaningTh}"
                else "${e.simplified} = ${e.meaningTh}"
            }
            val dialogueTurns = pack.entries.mapIndexed { idx, entry ->
                val zhSimp = entry.exampleZhSimp.ifBlank { entry.simplified }
                val zhTrad = entry.exampleZhTrad.ifBlank { entry.traditional }
                LessonDialogueLine(
                    turn = idx + 1,
                    speaker = if (idx % 2 == 0) "npc" else "user",
                    hant = zhTrad,
                    hans = zhSimp,
                    pinyin = entry.examplePinyin.ifBlank { entry.pinyin },
                    th = if (entry.exampleMeaningTh.isNotBlank() && entry.exampleMeaningTh != entry.meaningTh) {
                        "${entry.meaningTh} • ${entry.exampleMeaningTh}"
                    } else {
                        entry.meaningTh
                    }
                )
            }
            val grammarNotes = pack.entries.take(12).map { entry ->
                GrammarNote(
                    pattern = if (entry.pinyin.isNotBlank()) "${entry.simplified} (${entry.pinyin})" else entry.simplified,
                    explain = entry.meaningTh
                )
            }

            QuestNode(
                id = "$CUSTOM_NODE_PREFIX${pack.id}",
                zoneId = CUSTOM_ZONE_ID,
                titleTh = "ไฟล์: ${pack.name} (${pack.entries.size} คำ)",
                titleEn = "Pack: ${pack.name}",
                titleZhSimp = previewZhSimp.ifBlank { pack.name },
                titleZhTrad = previewZhTrad.ifBlank { pack.name },
                nodeType = QuestNodeType.CINEMA_ROLEPLAY,
                iconEmoji = "",
                xpReward = 80,
                starsEarned = 0,
                isUnlocked = true,
                isCurrent = true,
                descriptionTh = "ฝึกสนทนาและเล่นเกมด้วยคำศัพท์จากไฟล์ \"${pack.name}\": $vocabPromptSummary",
                hskLevel = HskLevel.HSK1,
                dialogueTurns = dialogueTurns,
                grammarNotes = grammarNotes
            )
        }

        return QuestZone(
            id = CUSTOM_ZONE_ID,
            titleTh = "คลังศัพท์จากไฟล์ของฉัน",
            titleEn = "My Imported Vocab Packs",
            titleZhSimp = "自定词库",
            titleZhTrad = "自訂詞庫",
            hskLevel = HskLevel.HSK1,
            themeColorHex = 0xFF7C3AED,
            isUnlocked = true,
            nodes = nodes
        )
    }

    fun packIdFromWordId(wordId: String): String? {
        if (!wordId.startsWith(CUSTOM_WORD_PREFIX)) return null
        val rest = wordId.removePrefix(CUSTOM_WORD_PREFIX)
        val lastUnderscore = rest.lastIndexOf('_')
        return if (lastUnderscore > 0) rest.substring(0, lastUnderscore) else null
    }
}
