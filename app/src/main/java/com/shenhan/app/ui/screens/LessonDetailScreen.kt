package com.shenhan.app.ui.screens
import kotlinx.coroutines.launch
import android.widget.Toast
import com.shenhan.app.ui.components.PinyinCompleteModule
import com.shenhan.app.data.Hsk3StandardBank
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenhan.app.data.SampleData
import com.shenhan.app.model.*

private data class LessonQuizItem(
    val prompt: String,
    val subPrompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

@Composable
fun LessonDetailScreen(
    node: QuestNode,
    scriptMode: ScriptMode,
    onBack: () -> Unit,
    onSpeak: (String) -> Unit,
    onCompleteLesson: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var vocabRefreshTick by remember { mutableIntStateOf(0) }
    var isSyncingRemote by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showFullLevelBank by remember { mutableStateOf(false) }

    val allStandardVocab = remember(context, vocabRefreshTick) { Hsk3StandardBank.getAllVocab(context) }
    val cumulativeVocab = remember(context, node.hskLevel) {
        Hsk3StandardBank.getCumulativeVocabUpTo(context, node.hskLevel)
    }
    val survivalCombos = remember(node.hskLevel) {
        Hsk3StandardBank.getSurvivalComboSentences(node.hskLevel)
    }

    val lessonVocab = remember(node.id, node.hskLevel, allStandardVocab) {
        val matched = allStandardVocab.filter { it.hskLevel == node.hskLevel }
        val pool = matched.ifEmpty { allStandardVocab }
        val offset = (node.id.hashCode().let { if (it < 0) -it else it }) % (pool.size.coerceAtLeast(1))
        val rotated = pool.drop(offset) + pool.take(offset)
        if (showFullLevelBank) pool else rotated.take(15)
    }

    val quizItems = remember(node.id, lessonVocab, cumulativeVocab) {
        buildLessonQuizItems(node, lessonVocab, cumulativeVocab, scriptMode)
    }

    var quizIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var answerChecked by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var quizFinished by remember { mutableStateOf(false) }

    val nodeTitleZh = if (scriptMode == ScriptMode.SIMPLIFIED) node.titleZhSimp else node.titleZhTrad
    val typeLabel = when (node.nodeType) {
        QuestNodeType.TONE_DRILL -> "ฝึกออกเสียง & วรรณยุกต์"
        QuestNodeType.VOCAB_SRS -> "คลังคำศัพท์ประจำบท"
        QuestNodeType.SENTENCE_BUILDER -> "โครงสร้าง & สร้างประโยค"
        QuestNodeType.CINEMA_ROLEPLAY -> "บทสนทนาสถานการณ์จริง"
        QuestNodeType.BOSS_CHALLENGE -> "บททดสอบสรุประดับ"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090E1A))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // แถบปุ่มย้อนกลับสู่หน้าแผนที่เควสต์
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF162033),
                    border = BorderStroke(1.dp, Color(0xFF28354D))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "กลับ",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "กลับหน้าแผนที่",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x26F59E0B),
                    border = BorderStroke(1.dp, Color(0x66F59E0B))
                ) {
                    Text(
                        text = "+${node.xpReward} XP • ${node.hskLevel.title}",
                        color = Color(0xFFFBBF24),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // การ์ดรายละเอียดบทเรียนหลัก
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFF2E3C56)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11192B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x332563EB), Color(0xFF11192B))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = node.iconEmoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x3338BDF8)
                        ) {
                            Text(
                                text = typeLabel,
                                color = Color(0xFF7DD3FC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = nodeTitleZh,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onSpeak(nodeTitleZh) },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "ฟังเสียงหัวข้อ",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = node.titleTh,
                        color = Color(0xFFE2E8F0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (node.titleEn.isNotBlank()) {
                        Text(
                            text = node.titleEn,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "รายละเอียดบทเรียน: ${node.descriptionTh}",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "เกณฑ์ระดับชั้น: ${node.hskLevel.descriptionTh}",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // แถบเลือกโหมดการเรียนภายในบท (เนื้อหา & คำศัพท์ / ไวยากรณ์ / แบบทดสอบจบด่าน)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    Triple(0, "คำศัพท์ & บทสนทนา", Icons.Default.MenuBook),
                    Triple(1, "ไวยากรณ์", Icons.Default.School),
                    Triple(2, "ทดสอบจบด่าน", Icons.Default.Quiz)
                )
                tabs.forEach { (idx, label, icon) ->
                    val active = selectedTab == idx
                    Surface(
                        onClick = { selectedTab = idx },
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) Color(0xFF2563EB) else Color(0xFF151F32),
                        border = BorderStroke(1.dp, if (active) Color(0xFF60A5FA) else Color(0xFF28354D)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (active) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                color = if (active) Color.White else Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        when (selectedTab) {
            // แท็บ 1: พินอินครบชุด (ด่านแรก HSK 1) + บทสนทนา + คำศัพท์ประจำบท + ผสมคำศัพท์สะสมข้ามระดับ
            0 -> {
                if (node.id == "node_1_1" || node.nodeType == QuestNodeType.TONE_DRILL) {
                    item {
                        PinyinCompleteModule(onSpeak = onSpeak)
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2120))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            val cumulativeLevelsText = (1..node.hskLevel.level).joinToString(" + ") { "HSK $it" }
                            Text(
                                text = "🔗 คลังคำศัพท์สะสมรวมระดับ: $cumulativeLevelsText",
                                color = Color(0xFF6EE7B7),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "นำคำศัพท์ที่เรียนผ่านมาแล้วมารวมกับระดับปัจจุบัน เพื่อฝึกสร้างประโยคเอาตัวรอดจริง",
                                color = Color(0xFFA7F3D0),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            survivalCombos.forEach { combo ->
                                val comboZh = if (scriptMode == ScriptMode.SIMPLIFIED) combo.simplified else combo.traditional
                                Surface(
                                    onClick = { onSpeak(comboZh) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF132D2B),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "🔊 ${combo.sourceLevelsLabel}",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = comboZh,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = combo.pinyin,
                                            color = Color(0xFF7DD3FC),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = combo.meaningTh,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "โครงสร้างคำผสม: ${combo.vocabBreakdown}",
                                            color = Color(0xFF6EE7B7),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (node.dialogueTurns.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "💬 บทสนทนาประจำบทเรียน (${node.dialogueTurns.size} ประโยค)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = {
                                val fullText = node.dialogueTurns.joinToString("。") {
                                    if (scriptMode == ScriptMode.SIMPLIFIED) it.hans else it.hant
                                }
                                onSpeak(fullText)
                            }) {
                                Text("🔊 ฟังทั้งบท", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(node.dialogueTurns) { line ->
                        val zhLine = if (scriptMode == ScriptMode.SIMPLIFIED) line.hans else line.hant
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF243147)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (line.speaker.equals("user", true)) "ผู้เรียน (คุณ)" else "คู่สนทนา",
                                        color = if (line.speaker.equals("user", true)) Color(0xFF34D399) else Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = zhLine,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = line.pinyin,
                                        color = Color(0xFF93C5FD),
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = line.th,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 13.sp
                                    )
                                }
                                IconButton(
                                    onClick = { onSpeak(zhLine) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "ฟังเสียง",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    val levelTotalCount = allStandardVocab.count { it.hskLevel == node.hskLevel }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📖 คำศัพท์${node.hskLevel.title} (${lessonVocab.size}/$levelTotalCount คำ)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                enabled = !isSyncingRemote,
                                onClick = {
                                    isSyncingRemote = true
                                    scope.launch {
                                        val total = Hsk3StandardBank.syncRemoteCurriculum(context)
                                        vocabRefreshTick++
                                        isSyncingRemote = false
                                        Toast.makeText(
                                            context,
                                            "☁️ ซิงก์คลังศัพท์หลังบ้านสำเร็จ (รวมทั้งหมด $total คำ)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isSyncingRemote) "กำลังซิงก์..." else "☁️ ซิงก์",
                                    color = Color(0xFF34D399),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(onClick = { showFullLevelBank = !showFullLevelBank }) {
                                Text(
                                    text = if (showFullLevelBank) "ย่อลง" else "ดูทั้งหมด ($levelTotalCount)",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                items(lessonVocab) { word ->
                    val hanzi = if (scriptMode == ScriptMode.SIMPLIFIED) word.simplified else word.traditional
                    val exZh = if (scriptMode == ScriptMode.SIMPLIFIED) word.exampleSentenceSimp else word.exampleSentenceTrad
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF243147)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = hanzi,
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = word.pinyin,
                                        color = Color(0xFF38BDF8),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                IconButton(
                                    onClick = { onSpeak(hanzi) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "ฟังเสียงคำศัพท์",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = "ความหมาย: ${word.meaningTh}",
                                color = Color(0xFFFDE68A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (exZh.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0D1422),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSpeak(exZh) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "ตัวอย่าง: $exZh",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (word.examplePinyin.isNotBlank()) {
                                            Text(
                                                text = word.examplePinyin,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (word.exampleMeaningTh.isNotBlank()) {
                                            Text(
                                                text = word.exampleMeaningTh,
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { selectedTab = 2 },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "ไปทำแบบทดสอบจบด่านเพื่อรับดาว →",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // แท็บ 2: โครงสร้างไวยากรณ์
            1 -> {
                val notesToShow = node.grammarNotes.ifEmpty {
                    listOf(
                        GrammarNote(
                            pattern = "主语 + 动词 + 宾语 (ประธาน + กริยา + กรรม)",
                            explain = "โครงสร้างประโยคพื้นฐานในภาษาจีน เรียงลำดับคล้ายภาษาไทย เช่น 我学习汉语 (ฉันเรียนภาษาจีน)"
                        ),
                        GrammarNote(
                            pattern = "คำกริยา + 吗 (ma) ?",
                            explain = "เติม 吗 ท้ายประโยคบอกเล่าเพื่อเปลี่ยนเป็นประโยคคำถามใช่หรือไม่ เช่น 你好吗？(คุณสบายดีไหม)"
                        )
                    )
                }

                items(notesToShow) { note ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF28354D)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📌 โครงสร้าง: ${note.pattern}",
                                color = Color(0xFF38BDF8),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = note.explain,
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // แท็บ 3: แบบทดสอบจบด่านรับดาว
            2 -> {
                item {
                    if (quizItems.isEmpty()) {
                        Text("ไม่มีแบบทดสอบสำหรับบทนี้", color = Color.White)
                    } else if (quizFinished) {
                        val earnedStars = when {
                            correctCount >= quizItems.size -> 3
                            correctCount >= (quizItems.size + 1) / 2 -> 2
                            else -> 1
                        }
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF112224))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🎉 ผ่านบทเรียนเรียบร้อย!",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    repeat(3) { sIdx ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (sIdx < earnedStars) Color(0xFFFBBF24) else Color(0xFF475569),
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "ตอบถูก $correctCount / ${quizItems.size} ข้อ • รับ +${node.xpReward} XP",
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onCompleteLesson(earnedStars) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "บันทึกผลและกลับหน้าแผนที่เควสต์",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        val q = quizItems[quizIndex]
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF28354D)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ข้อที่ ${quizIndex + 1} / ${quizItems.size}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = q.prompt,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (q.subPrompt.isNotBlank()) {
                                    Text(
                                        text = q.subPrompt,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))

                                q.options.forEachIndexed { optIdx, optText ->
                                    val isSelected = selectedOption == optIdx
                                    val isCorrect = optIdx == q.correctIndex
                                    val bgColor = when {
                                        answerChecked && isCorrect -> Color(0x3310B981)
                                        answerChecked && isSelected && !isCorrect -> Color(0x33EF4444)
                                        isSelected -> Color(0x332563EB)
                                        else -> Color(0xFF1B263B)
                                    }
                                    val borderColor = when {
                                        answerChecked && isCorrect -> Color(0xFF10B981)
                                        answerChecked && isSelected && !isCorrect -> Color(0xFFEF4444)
                                        isSelected -> Color(0xFF3B82F6)
                                        else -> Color(0xFF2C3A52)
                                    }

                                    Surface(
                                        onClick = {
                                            if (!answerChecked) selectedOption = optIdx
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = bgColor,
                                        border = BorderStroke(1.dp, borderColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = optText,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (answerChecked && isCorrect) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (answerChecked) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = q.explanation,
                                        color = Color(0xFF93C5FD),
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    enabled = selectedOption != null,
                                    onClick = {
                                        if (!answerChecked) {
                                            answerChecked = true
                                            if (selectedOption == q.correctIndex) {
                                                correctCount += 1
                                            }
                                        } else {
                                            if (quizIndex + 1 < quizItems.size) {
                                                quizIndex += 1
                                                selectedOption = null
                                                answerChecked = false
                                            } else {
                                                quizFinished = true
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = when {
                                            !answerChecked -> "ตรวจคำตอบ"
                                            quizIndex + 1 < quizItems.size -> "ข้อถัดไป →"
                                            else -> "ดูผลคะแนนจบด่าน"
                                        },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildLessonQuizItems(
    node: QuestNode,
    vocab: List<VocabWord>,
    cumulativeVocab: List<VocabWord>,
    scriptMode: ScriptMode
): List<LessonQuizItem> {
    val result = mutableListOf<LessonQuizItem>()
    val allDistractors = cumulativeVocab.map { it.meaningTh }.distinct()

    val earlierWords = cumulativeVocab.filter { it.hskLevel.level < node.hskLevel.level }.shuffled().take(2)
    val quizPool = (vocab.take(3) + earlierWords).distinctBy { it.simplified }

    for (w in quizPool) {
        val hanzi = if (scriptMode == ScriptMode.SIMPLIFIED) w.simplified else w.traditional
        val wrongChoices = allDistractors.filter { it != w.meaningTh }.shuffled().take(3)
        val options = (wrongChoices + w.meaningTh).shuffled()
        val correctIdx = options.indexOf(w.meaningTh).coerceAtLeast(0)
        result.add(
            LessonQuizItem(
                prompt = "คำว่า \"$hanzi\" (${w.pinyin}) แปลว่าอะไร?",
                subPrompt = "เลือกความหมายภาษาไทยที่ถูกต้อง",
                options = options,
                correctIndex = correctIdx,
                explanation = "เฉลย: $hanzi (${w.pinyin}) แปลว่า \"${w.meaningTh}\""
            )
        )
    }

    if (result.isEmpty() && node.dialogueTurns.isNotEmpty()) {
        val line = node.dialogueTurns.first()
        val zh = if (scriptMode == ScriptMode.SIMPLIFIED) line.hans else line.hant
        result.add(
            LessonQuizItem(
                prompt = "ประโยค \"$zh\" ตรงกับความหมายใด?",
                subPrompt = line.pinyin,
                options = listOf(line.th, "ขอบคุณมากครับ", "ขอโทษครับ ฉันไม่เข้าใจ", "ราคาเท่าไหร่ครับ"),
                correctIndex = 0,
                explanation = "เฉลย: $zh (${line.pinyin}) = ${line.th}"
            )
        )
    }

    return result
}
