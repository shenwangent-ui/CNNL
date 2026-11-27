package com.shenhan.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenhan.app.data.AppRepository
import com.shenhan.app.model.QuestNode
import com.shenhan.app.model.ScriptMode
import com.shenhan.app.ui.theme.*

private data class CinemaChatBubble(
    val isUser: Boolean,
    val zhText: String,
    val pinyin: String,
    val thText: String,
    val coachFeedback: String? = null
)

@Composable
fun AICinemaScreen(
    scriptMode: ScriptMode,
    isTtsSpeaking: Boolean,
    isAudioRecording: Boolean,
    onSpeak: (String) -> Unit,
    onChangeAudioSpeed: (Float) -> Unit,
    onSimulateUserSpeech: (String) -> Unit,
    onAddXp: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val questZones by AppRepository.questZones.collectAsState()
    val activeCinemaNodeId by AppRepository.activeCinemaNodeId.collectAsState()

    var selectedZoneIndex by remember { mutableIntStateOf(0) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }

    // ถ้ามีการกดส่งด่านมาจากหน้าแผนที่เควสต์ ให้กระโดดเปิดด่านนั้นทันที
    LaunchedEffect(activeCinemaNodeId, questZones) {
        val targetId = activeCinemaNodeId
        if (targetId != null) {
            val zIdx = questZones.indexOfFirst { z -> z.nodes.any { it.id == targetId } }
            if (zIdx != -1) {
                selectedZoneIndex = zIdx
                selectedNodeId = targetId
            }
            AppRepository.clearActiveCinemaNode()
        }
    }

    if (questZones.isEmpty()) return
    val safeZoneIdx = selectedZoneIndex.coerceIn(0, questZones.lastIndex)
    val currentZone = questZones[safeZoneIdx]
    val unlockedOrFirstNode = currentZone.nodes.find { it.id == selectedNodeId }
        ?: currentZone.nodes.firstOrNull { it.isCurrent && it.isUnlocked }
        ?: currentZone.nodes.firstOrNull()

    val activeNode = unlockedOrFirstNode ?: return

    // สร้างสคริปต์ภารกิจจากบทเรียนจริง (รองรับทั้งด่านที่มีบทสนทนาจาก Supabase และด่านพื้นฐาน)
    val scriptSteps = remember(activeNode.id, scriptMode) {
        val rawTurns = activeNode.dialogueTurns
        if (rawTurns.isNotEmpty()) {
            rawTurns.map { t ->
                val zh = if (scriptMode == ScriptMode.SIMPLIFIED) t.hans else t.hant
                Triple(t.speaker.equals("user", ignoreCase = true), zh, Pair(t.pinyin, t.th))
            }
        } else {
            listOf(
                Triple(false, "你好！欢迎来到《${activeNode.titleZhSimp}》实战考核。", Pair("Nǐ hǎo! Huānyíng láidào shízhàn kǎohé.", "สวัสดี! ยินดีต้อนรับสู่สนามสอบปฏิบัติจริง: ${activeNode.titleTh}")),
                Triple(true, "你好，我已经准备好了，请开始吧！", Pair("Nǐ hǎo, wǒ yǐjīng zhǔnbèi hǎo le, qǐng kāishǐ ba!", "สวัสดี ฉันพร้อมแล้ว เริ่มได้เลย!")),
                Triple(false, "很好！请用本课的语法和词汇回答我的问题。", Pair("Hěn hǎo! Qǐng yòng běnkè de yǔfǎ hé cíhuì huídá wǒ de wèntí.", "เยี่ยมมาก! ลองใช้ไวยากรณ์และคำศัพท์ของบทนี้ตอบคำถามดูนะ")),
                Triple(true, "没问题，我现在用中文跟你交流。", Pair("Méi wèntí, wǒ xiànzài yòng Zhōngwén gēn nǐ jiāoliú.", "ไม่มีปัญหา ตอนนี้ฉันสื่อสารภาษาจีนกับคุณได้แล้ว"))
            )
        }
    }

    // State สำหรับการสอบโต้ตอบทีละรอบในด่านปัจจุบัน
    var currentStepIdx by remember(activeNode.id) { mutableIntStateOf(0) }
    var userInput by remember(activeNode.id) { mutableStateOf("") }
    var usedHintCount by remember(activeNode.id) { mutableIntStateOf(0) }
    var showHintBox by remember(activeNode.id) { mutableStateOf(false) }
    var isLessonCompleted by remember(activeNode.id) { mutableStateOf(false) }
    var awardedStars by remember(activeNode.id) { mutableIntStateOf(0) }

    val chatHistory = remember(activeNode.id) { mutableStateListOf<CinemaChatBubble>() }

    // เริ่มต้นเปิดฉากให้ AI NPC พูดประโยคเปิดก่อนเสมอ
    LaunchedEffect(activeNode.id) {
        chatHistory.clear()
        var idx = 0
        while (idx < scriptSteps.size && !scriptSteps[idx].first) {
            val step = scriptSteps[idx]
            chatHistory.add(
                CinemaChatBubble(
                    isUser = false,
                    zhText = step.second,
                    pinyin = step.third.first,
                    thText = step.third.second
                )
            )
            idx++
        }
        currentStepIdx = idx
        if (idx >= scriptSteps.size) {
            isLessonCompleted = true
            awardedStars = 3
        }
    }

    val expectedUserTurn = scriptSteps.getOrNull(currentStepIdx)
    val totalUserTasks = scriptSteps.count { it.first }.coerceAtLeast(1)
    val completedUserTasks = scriptSteps.take(currentStepIdx).count { it.first }

    fun submitUserResponse(answerText: String, usedAutoFill: Boolean = false) {
        val cleaned = answerText.trim()
        if (cleaned.isEmpty() || isLessonCompleted) return

        val expectedZh = expectedUserTurn?.second ?: ""
        val expectedPinyin = expectedUserTurn?.third?.first ?: ""
        val expectedTh = expectedUserTurn?.third?.second ?: ""

        val hasChineseChars = cleaned.any { it.code in 0x4E00..0x9FFF }
        val matchedKeyChar = expectedZh.any { ch -> ch.code in 0x4E00..0x9FFF && cleaned.contains(ch) }
        val feedbackMsg = when {
            usedAutoFill -> "💡 ฝึกพูดตามต้นแบบได้ดี (ใช้ตัวช่วย)"
            hasChineseChars && matchedKeyChar -> "✅ ยอดเยี่ยม! ใช้คำศัพท์และรูปประโยคตรงตามสถานการณ์"
            hasChineseChars -> "✅ สื่อสารภาษาจีนได้เข้าใจ! (ต้นแบบแนะนำ: $expectedZh)"
            else -> "👍 ตอบโต้ได้ต่อเนื่อง (ลองพิมพ์/พูดเป็นตัวอักษรจีน: $expectedZh)"
        }

        chatHistory.add(
            CinemaChatBubble(
                isUser = true,
                zhText = cleaned,
                pinyin = if (usedAutoFill) expectedPinyin else "การตอบโต้ของคุณ",
                thText = if (usedAutoFill) expectedTh else "ส่งคำตอบเข้าประเมินผลแล้ว",
                coachFeedback = feedbackMsg
            )
        )
        userInput = ""
        showHintBox = false

        // เลื่อนไปยังรอบถัดไปของ AI NPC
        var nextIdx = currentStepIdx + 1
        while (nextIdx < scriptSteps.size && !scriptSteps[nextIdx].first) {
            val npcTurn = scriptSteps[nextIdx]
            chatHistory.add(
                CinemaChatBubble(
                    isUser = false,
                    zhText = npcTurn.second,
                    pinyin = npcTurn.third.first,
                    thText = npcTurn.third.second
                )
            )
            onSpeak(npcTurn.second)
            nextIdx++
        }
        currentStepIdx = nextIdx

        // ถ้าโต้ตอบครบทุกภารกิจในด่านนี้แล้ว -> ตัดเกรดให้ดาวและปลดล็อกด่านถัดไปทันที
        if (nextIdx >= scriptSteps.size) {
            isLessonCompleted = true
            val stars = when {
                usedHintCount == 0 -> 3
                usedHintCount == 1 -> 2
                else -> 1
            }
            awardedStars = stars
            AppRepository.completeQuestNode(activeNode.id, stars, activeNode.xpReward)
            onAddXp(activeNode.xpReward)
            Toast.makeText(context, "🎉 ผ่านด่าน! รับ $stars ดาว และ +${activeNode.xpReward} XP", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E1A))
            .testTag("ai_cinema_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "🎬 AI Cinema สนามสอบปฏิบัติ",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "สนทนาโต้ตอบจริงทีละด่านเพื่อวัดผลให้ดาว",
                            fontSize = 12.sp,
                            color = FrostedSlateLight
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x33F59E0B),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = "ภารกิจ $completedUserTasks / $totalUserTasks",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // แถบเลือกหมวดหมู่: ⚡ ทันใจ + HSK 1 - HSK 6
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(questZones) { idx, zone ->
                        val isSel = idx == safeZoneIdx
                        val isFav = zone.id == "zone_favorite"
                        val label = if (isFav) "⚡ ทันใจ" else zone.hskLevel.title
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isSel && isFav -> Color(0xFFF59E0B)
                                isSel -> FrostedPurplePrimary
                                isFav -> Color(0x33F59E0B)
                                else -> Color(0xFF151E32)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isFav) Color(0xFFF59E0B) else Color(0xFF26334D)
                            ),
                            modifier = Modifier.clickable {
                                selectedZoneIndex = idx
                                selectedNodeId = zone.nodes.firstOrNull()?.id
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel && isFav) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // แถบเลือกด่านย่อยในโซนปัจจุบัน
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentZone.nodes) { node ->
                        val isCur = node.id == activeNode.id
                        val canPlay = node.isUnlocked || node.starsEarned > 0 || currentZone.id == "zone_favorite"
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isCur -> Color(0xFF1E293B)
                                canPlay -> Color(0xFF111827)
                                else -> Color(0xFF0C111D)
                            },
                            border = BorderStroke(
                                1.5.dp,
                                when {
                                    isCur -> Color(0xFF38BDF8)
                                    node.starsEarned > 0 -> Color(0xFF10B981)
                                    else -> Color(0xFF1F2937)
                                }
                            ),
                            modifier = Modifier
                                .width(205.dp)
                                .clickable {
                                    if (canPlay) {
                                        selectedNodeId = node.id
                                    } else {
                                        Toast.makeText(context, "กรุณาสอบผ่านด่านก่อนหน้าเพื่อปลดล็อก", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (node.starsEarned > 0) "★".repeat(node.starsEarned) + "☆".repeat(3 - node.starsEarned) else if (canPlay) "☆☆☆ พร้อมสอบ" else "🔒 ล็อกอยู่",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (node.starsEarned > 0) Color(0xFFFBBF24) else if (canPlay) Color(0xFF38BDF8) else FrostedSlateMuted
                                    )
                                    Text(
                                        text = "+${node.xpReward} XP",
                                        fontSize = 10.sp,
                                        color = Color(0xFF34D399)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = node.titleTh,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (canPlay) Color.White else FrostedSlateMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // การ์ดโจทย์ประจำด่าน + ไวยากรณ์สำคัญ (ตงหัว)
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                    ),
                    border = BorderStroke(1.dp, Color(0xFF4F46E5).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeNode.titleTh,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val introZh = chatHistory.lastOrNull { !it.isUser }?.zhText ?: activeNode.titleZhSimp
                                    onSpeak(introZh)
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x3338BDF8))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "ฟังเสียง",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = activeNode.descriptionTh,
                            fontSize = 12.sp,
                            color = FrostedSlateLight
                        )

                        if (activeNode.grammarNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x267C3AED),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "📘 ไวยากรณ์บังคับใช้ในด่านนี้:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC4B5FD)
                                    )
                                    activeNode.grammarNotes.forEach { g ->
                                        Text(
                                            text = "• ${g.pattern} : ${g.explain}",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // บทสนทนาโต้ตอบระหว่าง AI NPC และผู้เรียน
            items(chatHistory) { msg ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (msg.isUser) 16.dp else 4.dp,
                            bottomEnd = if (msg.isUser) 4.dp else 16.dp
                        ),
                        color = if (msg.isUser) Color(0xFF1D4ED8) else Color(0xFF162033),
                        border = BorderStroke(
                            1.dp,
                            if (msg.isUser) Color(0xFF60A5FA) else Color(0xFF2A3954)
                        ),
                        modifier = Modifier.widthIn(max = 310.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (msg.isUser) "คุณ (ผู้เข้าสอบ)" else "🤖 AI คู่สนทนา",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (msg.isUser) Color(0xFFBFDBFE) else Color(0xFF38BDF8)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "ฟังเสียง",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { onSpeak(msg.zhText) }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.zhText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = msg.pinyin,
                                fontSize = 12.sp,
                                color = Color(0xFF93C5FD)
                            )
                            Text(
                                text = msg.thText,
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            if (msg.coachFeedback != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x3310B981)
                                ) {
                                    Text(
                                        text = msg.coachFeedback,
                                        fontSize = 11.sp,
                                        color = Color(0xFF6EE7B7),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // การ์ดสรุปผลเมื่อสอบผ่านด่านนี้ครบแล้ว
            if (isLessonCompleted) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x2610B981),
                        border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "🏆 ผ่านการวัดผลด่านนี้แล้ว!",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ได้รับ ${"★".repeat(awardedStars)}${ "☆".repeat(3 - awardedStars) } • +${activeNode.xpReward} XP",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            val nextNode = currentZone.nodes.getOrNull(
                                currentZone.nodes.indexOfFirst { it.id == activeNode.id } + 1
                            )
                            Button(
                                onClick = {
                                    if (nextNode != null) {
                                        selectedNodeId = nextNode.id
                                    } else if (safeZoneIdx + 1 < questZones.size) {
                                        selectedZoneIndex = safeZoneIdx + 1
                                        selectedNodeId = questZones[safeZoneIdx + 1].nodes.firstOrNull()?.id
                                    } else {
                                        Toast.makeText(context, "คุณพิชิตครบทุกด่านในหมวดนี้แล้ว!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (nextNode != null) "ลุยด่านถัดไป: ${nextNode.titleTh} ▶" else "ไปยังระดับถัดไป ▶",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // แถบควบคุมการตอบโต้ด้านล่าง (แสดงระหว่างกำลังสอบในด่าน)
        if (!isLessonCompleted && expectedUserTurn != null) {
            Surface(
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎯 เป้าหมาย: ${expectedUserTurn.third.second}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.weight(1f),
                            maxLines = 2
                        )
                        TextButton(
                            onClick = {
                                if (!showHintBox) usedHintCount++
                                showHintBox = !showHintBox
                            }
                        ) {
                            Text(
                                text = if (showHintBox) "ซ่อนเฉลย" else "💡 ดูประโยคช่วยตอบ",
                                fontSize = 12.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    if (showHintBox) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable {
                                    onSpeak(expectedUserTurn.second)
                                    submitUserResponse(expectedUserTurn.second, usedAutoFill = true)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = expectedUserTurn.second,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = expectedUserTurn.third.first,
                                        fontSize = 11.sp,
                                        color = Color(0xFF93C5FD)
                                    )
                                }
                                Text(
                                    text = "แตะเพื่อพูดส่ง ▶",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                onSimulateUserSpeech(expectedUserTurn.second)
                                onSpeak(expectedUserTurn.second)
                                submitUserResponse(expectedUserTurn.second, usedAutoFill = false)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C3AED))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "พูดตอบ",
                                tint = Color.White
                            )
                        }

                        OutlinedTextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            placeholder = {
                                Text("พิมพ์ภาษาจีนตอบโต้...", fontSize = 13.sp, color = FrostedSlateMuted)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { submitUserResponse(userInput, usedAutoFill = false) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "ส่งคำตอบ",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
