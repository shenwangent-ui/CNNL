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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenhan.app.data.AppRepository
import com.shenhan.app.model.ScriptMode
import com.shenhan.app.ui.theme.*
import kotlinx.coroutines.delay

private enum class GameArenaMode(val label: String) {
    AI_CHAT("💬 แชทเกม AI"),
    CARD_MATCH("🃏 การ์ดจับคู่"),
    FLASHCARDS("🗂️ การ์ดจำเป็น"),
    SENTENCE_CARDS("🧩 เรียงประโยค")
}

private data class CinemaChatBubble(
    val id: String,
    val isUser: Boolean,
    val zhText: String,
    val pinyin: String,
    val thText: String,
    val coachFeedback: String? = null,
    val accuracyPct: Int? = null,
    val animateTyping: Boolean = false
)

private data class SmartReplyOption(
    val zhSimp: String,
    val zhTrad: String,
    val pinyin: String,
    val th: String,
    val tag: String
)

private data class EssentialCardItem(
    val id: String,
    val zhSimp: String,
    val zhTrad: String,
    val pinyin: String,
    val th: String,
    val category: String,
    val exampleZh: String,
    val exampleTh: String
)

private data class MatchCardTile(
    val uid: Int,
    val pairId: String,
    val isChineseSide: Boolean,
    val displayMain: String,
    val displaySub: String,
    val speakText: String
)

private data class SentencePuzzle(
    val thPrompt: String,
    val pinyinHint: String,
    val targetWords: List<String>,
    val category: String
)

private val ESSENTIAL_SURVIVAL_CARDS = listOf(
    EssentialCardItem("c1", "多少钱？", "多少錢？", "Duōshǎo qián?", "ราคาเท่าไหร่?", "💳 การเงิน", "请问这个多少钱？", "ขอถามหน่อยว่าอันนี้ราคาเท่าไหร่?"),
    EssentialCardItem("c2", "我扫支付宝", "我掃支付寶", "Wǒ sǎo Zhīfùbǎo", "ฉันสแกนจ่าย Alipay", "💳 การเงิน", "可以刷微信还是我扫支付宝？", "สแกน WeChat หรือให้ฉันสแกน Alipay?"),
    EssentialCardItem("c3", "不要放牛肉和花生", "不要放牛肉和花生", "Bù yào fàng niúròu hé huāshēng", "ไม่ใส่เนื้อวัวและถั่วลิสง", "🍜 อาหาร", "老板，炒饭不要放牛肉和花生。", "เถ้าแก่ ข้าวผัดไม่ใส่เนื้อวัวกับถั่วลิสง"),
    EssentialCardItem("c4", "珍珠奶茶半糖去冰", "珍珠奶茶半糖去冰", "Zhēnzhū nǎichá bàn táng qù bīng", "ชานมไข่มุกหวาน 50% ไม่ใส่น้ำแข็ง", "🧋 เครื่องดื่ม", "我要一杯珍珠奶茶，半糖去冰。", "เอาชานมไข่มุก 1 แก้ว หวานครึ่งเดียวไม่ใส่น้ำแข็ง"),
    EssentialCardItem("c5", "麻烦帮我拍张照", "麻煩幫我拍張照", "Máfan bāng wǒ pāi zhāng zhào", "รบกวนช่วยถ่ายรูปให้ฉันหน่อย", "🆘 ช่วยเหลือ", "你好，麻烦帮我们拍张合照可以吗？", "สวัสดี รบกวนช่วยถ่ายรูปหมู่ให้พวกเราหน่อยได้ไหม?"),
    EssentialCardItem("c6", "请帮我打110 / 120", "請幫我打110 / 120", "Qǐng bāng wǒ dǎ 110 / 120", "ช่วยโทรแจ้งตำรวจ 110 / รถพยาบาล 120", "🚨 ฉุกเฉิน", "发生意外了，请帮我打120叫救护车！", "เกิดอุบัติเหตุแล้ว ช่วยโทร 120 เรียกรถพยาบาลที!"),
    EssentialCardItem("c7", "手机尾号是...", "手機尾號是...", "Shǒujī wěihào shì...", "เลขท้ายเบอร์มือถือ 4 ตัวคือ...", "🚕 เดินทาง", "师傅你好，我的手机尾号是 8899。", "สวัสดีครับคนขับ เลขท้ายเบอร์ผมคือ 8899"),
    EssentialCardItem("c8", "我想办一张银行卡", "我想辦一張銀行卡", "Wǒ xiǎng bàn yī zhāng yínhángkǎ", "ฉันอยากเปิดบัตรธนาคาร 1 ใบ", "🏦 ธนาคาร", "我是泰国人，想办一张中国银行卡。", "ฉันเป็นคนไทย อยากเปิดบัตรธนาคารจีน 1 ใบ"),
    EssentialCardItem("c9", "我喜欢你，在一起吧", "我喜歡你，在一起吧", "Wǒ xǐhuān nǐ, zài yīqǐ ba", "ฉันชอบเธอ คบกันนะ", "❤️ ความรัก", "认识你很开心，我喜欢你，在一起吧！", "ดีใจที่ได้รู้จักเธอ ฉันชอบเธอ คบกันนะ!"),
    EssentialCardItem("c10", "我要办理飞往曼谷的登机", "我要辦理飛往曼谷的登機", "Wǒ yào bànlǐ fēiwǎng Màngǔ de dēngjī", "ฉันต้องการเช็กอินบินกลับกรุงเทพฯ", "🛫 กลับไท่กั๋ว", "你好，我要办理飞往泰国曼谷的登机手续。", "สวัสดี ฉันต้องการเช็กอินเที่ยวบินไปกรุงเทพฯ ประเทศไทย")
)

private val SENTENCE_PUZZLES = listOf(
    SentencePuzzle("เอาชานมไข่มุก 1 แก้ว หวานครึ่งเดียวไม่ใส่น้ำแข็ง", "Wǒ yào yī bēi zhēnzhū nǎichá bàn táng qù bīng", listOf("我要", "一杯", "珍珠奶茶", "半糖", "去冰"), "🧋 สั่งเครื่องดื่ม"),
    SentencePuzzle("ไม่ใส่เนื้อวัวและถั่วลิสง เผ็ดนิดเดียวพอ", "Bù yào fàng niúròu hé huāshēng wēi là jiù hǎo", listOf("不要放", "牛肉", "和", "花生", "微辣", "就好"), "🍜 เอาตัวรอดร้านอาหาร"),
    SentencePuzzle("สวัสดี ฉันเป็นคนไทย อยากเปิดซิมมือถือจีน", "Nǐ hǎo wǒ shì Tàiguórén xiǎng bàn shǒujīkǎ", listOf("你好", "我是", "泰国人", "想办", "手机卡"), "📱 เปิดซิมในจีน"),
    SentencePuzzle("รบกวนช่วยถ่ายรูปให้พวกเราหน่อยได้ไหม?", "Máfan bāng wǒmen pāi zhāng zhào kěyǐ ma", listOf("麻烦", "帮我们", "拍张照", "可以吗"), "📸 ขอความช่วยเหลือ"),
    SentencePuzzle("ฉันต้องการเช็กอินเที่ยวบินกลับกรุงเทพฯ ประเทศไทย", "Wǒ yào bànlǐ fēiwǎng Tàiguó Màngǔ de dēngjī", listOf("我要", "办理", "飞往", "泰国曼谷", "的登机"), "🛫 บินกลับไท่กั๋ว")
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
    val userXp by AppRepository.userXp.collectAsState()

    var currentMode by remember { mutableStateOf(GameArenaMode.AI_CHAT) }
    var hearts by remember { mutableIntStateOf(5) }
    var comboCount by remember { mutableIntStateOf(0) }
    var chestCharge by remember { mutableIntStateOf(1) } // เต็ม 3 เปิดกล่องสมบัติได้

    LaunchedEffect(activeCinemaNodeId) {
        if (activeCinemaNodeId != null) {
            currentMode = GameArenaMode.AI_CHAT
        }
    }

    fun rewardGameSuccess(baseXp: Int, reason: String) {
        comboCount += 1
        chestCharge = (chestCharge + 1).coerceAtMost(3)
        val multiplier = when {
            comboCount >= 5 -> 2.0
            comboCount >= 3 -> 1.5
            else -> 1.0
        }
        val totalXp = (baseXp * multiplier).toInt()
        onAddXp(totalXp)
        if (comboCount % 4 == 0 && hearts < 5) {
            hearts += 1
            Toast.makeText(context, "🔥 COMBO x$comboCount! +$totalXp XP & ฟื้นหัวใจ +1 ❤️", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "$reason (+$totalXp XP • COMBO x$comboCount)", Toast.LENGTH_SHORT).show()
        }
    }

    fun penalizeMistake(hint: String) {
        comboCount = 0
        hearts = (hearts - 1).coerceAtLeast(1)
        Toast.makeText(context, "❌ ยังไม่ตรงเป๊ะ ($hint) • เสีย 1 ❤️", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080D19))
            .testTag("ai_cinema_screen")
    ) {
        // แถบสถานะเกมด้านบน (หัวใจ, คอมโบ, กล่องสมบัติ, XP)
        Surface(
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // หัวใจพลังชีวิต
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Hearts",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$hearts/5",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // คอมโบไฟลุก
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (comboCount > 0) Color(0x33F59E0B) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (comboCount > 0) Color(0xFFF59E0B) else Color(0xFF334155))
                        ) {
                            Text(
                                text = "🔥 COMBO x${comboCount.coerceAtLeast(1)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (comboCount > 0) Color(0xFFFBBF24) else FrostedSlateLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // ปุ่มกล่องสมบัติโบนัส
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (chestCharge >= 3) Color(0xFFF59E0B) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.clickable {
                            if (chestCharge >= 3) {
                                chestCharge = 0
                                hearts = 5
                                onAddXp(100)
                                Toast.makeText(context, "🎁 เปิดกล่องสมบัติสำเร็จ! รับ +100 XP & หัวใจเต็ม 5 ดวง ❤️", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "ตอบถูกอีก ${3 - chestCharge} ครั้งเพื่อเปิดกล่องสมบัติรับ +100 XP!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(
                            text = if (chestCharge >= 3) "🎁 เปิดกล่องโบนัส!" else "🎁 กล่อง $chestCharge/3",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (chestCharge >= 3) Color.Black else Color(0xFFFBBF24),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // แถบสลับโหมดเกม 4 รูปแบบ
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GameArenaMode.values()) { mode ->
                        val isSelected = currentMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF162033),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF60A5FA) else Color(0xFF26334D)),
                            modifier = Modifier.clickable { currentMode = mode }
                        ) {
                            Text(
                                text = mode.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // เนื้อหาหลักตามโหมดที่เลือก
        when (currentMode) {
            GameArenaMode.AI_CHAT -> AiInteractiveChatGame(
                questZones = questZones,
                activeCinemaNodeId = activeCinemaNodeId,
                scriptMode = scriptMode,
                onSpeak = onSpeak,
                onSimulateUserSpeech = onSimulateUserSpeech,
                onReward = ::rewardGameSuccess,
                onMistake = ::penalizeMistake,
                modifier = Modifier.weight(1f)
            )
            GameArenaMode.CARD_MATCH -> CardMatchBattleGame(
                scriptMode = scriptMode,
                onSpeak = onSpeak,
                onReward = ::rewardGameSuccess,
                onMistake = ::penalizeMistake,
                modifier = Modifier.weight(1f)
            )
            GameArenaMode.FLASHCARDS -> SurvivalFlashcardsGame(
                scriptMode = scriptMode,
                onSpeak = onSpeak,
                onReward = ::rewardGameSuccess,
                modifier = Modifier.weight(1f)
            )
            GameArenaMode.SENTENCE_CARDS -> SentenceBuilderCardGame(
                onSpeak = onSpeak,
                onReward = ::rewardGameSuccess,
                onMistake = ::penalizeMistake,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =========================================================================
// โหมด 1: เกมแชทพิมพ์โต้ตอบ AI + AI พิมพ์ตอบสด + ลิสต์ตัวช่วยคำตอบอัจฉริยะ
// =========================================================================
@Composable
private fun AiInteractiveChatGame(
    questZones: List<com.shenhan.app.model.QuestZone>,
    activeCinemaNodeId: String?,
    scriptMode: ScriptMode,
    onSpeak: (String) -> Unit,
    onSimulateUserSpeech: (String) -> Unit,
    onReward: (Int, String) -> Unit,
    onMistake: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedZoneIndex by remember { mutableIntStateOf(0) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }

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
    val activeNode = currentZone.nodes.find { it.id == selectedNodeId }
        ?: currentZone.nodes.firstOrNull { it.isCurrent && it.isUnlocked }
        ?: currentZone.nodes.firstOrNull() ?: return

    val scriptSteps = remember(activeNode.id, scriptMode) {
        val rawTurns = activeNode.dialogueTurns
        if (rawTurns.isNotEmpty()) {
            rawTurns.map { t ->
                val zh = if (scriptMode == ScriptMode.SIMPLIFIED) t.hans else t.hant
                Triple(t.speaker.equals("user", ignoreCase = true), zh, Pair(t.pinyin, t.th))
            }
        } else {
            listOf(
                Triple(false, "你好！欢迎来到《${activeNode.titleZhSimp}》实战对话。", Pair("Nǐ hǎo! Huānyíng láidào shízhàn duìhuà.", "สวัสดี! ยินดีต้อนรับสู่การสนทนาจริง: ${activeNode.titleTh}")),
                Triple(true, "你好，我想用中文练习这个场景。", Pair("Nǐ hǎo, wǒ xiǎng yòng Zhōngwén liànxí zhège chǎngjǐng.", "สวัสดี ฉันอยากฝึกใช้ภาษาจีนในสถานการณ์นี้")),
                Triple(false, "太棒了！请告诉我你现在需要什么帮助？", Pair("Tài bàng le! Qǐng gàosu wǒ nǐ xiànzài xūyào shénme bāngzhù?", "ยอดเยี่ยมมาก! บอกฉันได้เลยว่าตอนนี้คุณต้องการความช่วยเหลือเรื่องอะไร?")),
                Triple(true, "谢谢你的帮助，我现在完全明白了！", Pair("Xièxie nǐ de bāngzhù, wǒ xiànzài wánquán míngbai le!", "ขอบคุณสำหรับความช่วยเหลือ ตอนนี้ฉันเข้าใจทั้งหมดแล้ว!"))
            )
        }
    }

    var currentStepIdx by remember(activeNode.id) { mutableIntStateOf(0) }
    var userInput by remember(activeNode.id) { mutableStateOf("") }
    var isAiTyping by remember(activeNode.id) { mutableStateOf(false) }
    var showSuggestionList by remember(activeNode.id) { mutableStateOf(true) }
    var isLessonCompleted by remember(activeNode.id) { mutableStateOf(false) }
    var awardedStars by remember(activeNode.id) { mutableIntStateOf(0) }

    val chatHistory = remember(activeNode.id) { mutableStateListOf<CinemaChatBubble>() }

    // AI พิมพ์ข้อความเปิดฉาก
    LaunchedEffect(activeNode.id) {
        chatHistory.clear()
        isAiTyping = true
        delay(350)
        var idx = 0
        while (idx < scriptSteps.size && !scriptSteps[idx].first) {
            val step = scriptSteps[idx]
            chatHistory.add(
                CinemaChatBubble(
                    id = "init_$idx",
                    isUser = false,
                    zhText = step.second,
                    pinyin = step.third.first,
                    thText = step.third.second,
                    animateTyping = true
                )
            )
            idx++
        }
        isAiTyping = false
        currentStepIdx = idx
    }

    val expectedUserTurn = scriptSteps.getOrNull(currentStepIdx)

    // สร้างลิสต์ตัวช่วยคำตอบอัจฉริยะ (Smart Reply List) ที่เปลี่ยนตามรอบสนทนา
    val dynamicSuggestions = remember(activeNode.id, currentStepIdx, scriptMode) {
        val list = mutableListOf<SmartReplyOption>()
        if (expectedUserTurn != null) {
            list.add(
                SmartReplyOption(
                    zhSimp = expectedUserTurn.second,
                    zhTrad = expectedUserTurn.second,
                    pinyin = expectedUserTurn.third.first,
                    th = expectedUserTurn.third.second,
                    tag = "🎯 คำตอบตรงภารกิจ"
                )
            )
        }
        list.add(SmartReplyOption("你好，请问可以帮我一下吗？", "你好，請問可以幫我一下嗎？", "Nǐ hǎo, qǐngwèn kěyǐ bāng wǒ yīxià ma?", "สวัสดี ขอถามหน่อยว่าช่วยฉันสักครู่ได้ไหม?", "💬 ขอความช่วยเหลือ"))
        list.add(SmartReplyOption("好的，没问题，谢谢你！", "好的，沒問題，謝謝你！", "Hǎo de, méi wèntí, xièxie nǐ!", "โอเค ไม่มีปัญหา ขอบคุณมาก!", "👍 ตอบตกลง"))
        list.add(SmartReplyOption("不好意思，请再说一遍。", "不好意思，請再說一遍。", "Bù hǎoyìsi, qǐng zài shuō yī biàn.", "ขอโทษครับ กรุณาพูดอีกครั้ง", "🔄 ขอฟังซ้ำ"))
        list
    }

    // เมื่อผู้ใช้กดส่งคำตอบ -> AI จะขึ้นกำลังพิมพ์และพิมพ์ตอบโต้กลับมาทีละตัวอักษร
    var pendingReplyStepIdx by remember(activeNode.id) { mutableIntStateOf(-1) }

    LaunchedEffect(pendingReplyStepIdx) {
        if (pendingReplyStepIdx >= 0) {
            isAiTyping = true
            delay(650) // จำลองจังหวะ AI กำลังคิดและพิมพ์คำตอบ
            var nextIdx = pendingReplyStepIdx
            while (nextIdx < scriptSteps.size && !scriptSteps[nextIdx].first) {
                val npcTurn = scriptSteps[nextIdx]
                chatHistory.add(
                    CinemaChatBubble(
                        id = "npc_${nextIdx}_${System.currentTimeMillis()}",
                        isUser = false,
                        zhText = npcTurn.second,
                        pinyin = npcTurn.third.first,
                        thText = npcTurn.third.second,
                        animateTyping = true
                    )
                )
                onSpeak(npcTurn.second)
                nextIdx++
            }
            isAiTyping = false
            currentStepIdx = nextIdx
            pendingReplyStepIdx = -1

            if (nextIdx >= scriptSteps.size && !isLessonCompleted) {
                isLessonCompleted = true
                awardedStars = 3
                AppRepository.completeQuestNode(activeNode.id, 3, activeNode.xpReward)
                onReward(activeNode.xpReward, "🎉 ผ่านด่านสนทนา AI!")
            }
        }
    }

    fun handleSendAnswer(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || isAiTyping || isLessonCompleted) return

        val expectedZh = expectedUserTurn?.second ?: ""
        val hasChinese = text.any { it.code in 0x4E00..0x9FFF }
        val matchScore = when {
            text == expectedZh -> 100
            hasChinese && expectedZh.any { ch -> ch.code in 0x4E00..0x9FFF && text.contains(ch) } -> 92
            hasChinese -> 84
            else -> 70
        }

        val feedback = when {
            matchScore >= 95 -> "🌟 สมบูรณ์แบบ! รูปประโยคและคำศัพท์เป๊ะมาก"
            matchScore >= 85 -> "✅ สื่อสารเป็นธรรมชาติ! AI เข้าใจเจตนาครบถ้วน"
            else -> "💡 ตอบโต้ได้ต่อเนื่อง (แนะนำเพิ่มคำว่า: $expectedZh)"
        }

        chatHistory.add(
            CinemaChatBubble(
                id = "usr_${System.currentTimeMillis()}",
                isUser = true,
                zhText = text,
                pinyin = expectedUserTurn?.third?.first ?: "ข้อความของคุณ",
                thText = expectedUserTurn?.third?.second ?: "ส่งคำตอบโต้ตอบแล้ว",
                coachFeedback = feedback,
                accuracyPct = matchScore
            )
        )
        userInput = ""
        onReward(25, "💬 ตอบโต้สำเร็จ")
        pendingReplyStepIdx = currentStepIdx + 1
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // แถบเลือกหมวดโซน (⚡ ทันใจ + HSK 1-6)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(questZones) { idx, zone ->
                        val isSel = idx == safeZoneIdx
                        val isFav = zone.id == "zone_favorite"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) (if (isFav) Color(0xFFF59E0B) else FrostedPurplePrimary) else Color(0xFF151E32),
                            modifier = Modifier.clickable {
                                selectedZoneIndex = idx
                                selectedNodeId = zone.nodes.firstOrNull()?.id
                            }
                        ) {
                            Text(
                                text = if (isFav) "⚡ ทันใจ" else zone.hskLevel.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel && isFav) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // แถบเลือกด่านย่อย
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentZone.nodes) { node ->
                        val isCur = node.id == activeNode.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCur) Color(0xFF1E293B) else Color(0xFF111827),
                            border = BorderStroke(1.dp, if (isCur) Color(0xFF38BDF8) else Color(0xFF1F2937)),
                            modifier = Modifier
                                .width(190.dp)
                                .clickable { selectedNodeId = node.id }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = if (node.starsEarned > 0) "★".repeat(node.starsEarned) + " ผ่านแล้ว" else "🎯 ด่านสนทนา",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (node.starsEarned > 0) Color(0xFFFBBF24) else Color(0xFF38BDF8)
                                )
                                Text(
                                    text = node.titleTh,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // บอลลูนแชทโต้ตอบ (พร้อมเอฟเฟกต์ AI พิมพ์ตัวอักษรไหลทีละตัว)
            items(chatHistory, key = { it.id }) { msg ->
                var visibleChars by remember(msg.id) {
                    mutableIntStateOf(if (msg.animateTyping) 0 else msg.zhText.length)
                }
                LaunchedEffect(msg.id) {
                    if (msg.animateTyping) {
                        for (i in 1..msg.zhText.length) {
                            visibleChars = i
                            delay(28)
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (msg.isUser) Color(0xFF1D4ED8) else Color(0xFF162033),
                        border = BorderStroke(1.dp, if (msg.isUser) Color(0xFF60A5FA) else Color(0xFF2A3954)),
                        modifier = Modifier.widthIn(max = 310.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (msg.isUser) "คุณ • ความแม่นยำ ${msg.accuracyPct ?: 90}%" else "🤖 AI คู่สนทนา",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (msg.isUser) Color(0xFFBFDBFE) else Color(0xFF38BDF8)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "ฟังเสียง",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onSpeak(msg.zhText) }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.zhText.take(visibleChars),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (visibleChars >= msg.zhText.length) {
                                Text(text = msg.pinyin, fontSize = 12.sp, color = Color(0xFF93C5FD))
                                Text(text = msg.thText, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                            }
                            if (msg.coachFeedback != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = msg.coachFeedback,
                                    fontSize = 11.sp,
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }
                    }
                }
            }

            // แถบแจ้งเตือนขณะ AI กำลังพิมพ์คำตอบ
            if (isAiTyping) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "🤖 AI กำลังพิมพ์คำตอบ...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (isLessonCompleted) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0x2610B981),
                        border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "🏆 ผ่านด่านสนทนาเรียบร้อย! (★★★)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val nextNode = currentZone.nodes.getOrNull(
                                currentZone.nodes.indexOfFirst { it.id == activeNode.id } + 1
                            )
                            Button(
                                onClick = {
                                    if (nextNode != null) selectedNodeId = nextNode.id
                                    else if (safeZoneIdx + 1 < questZones.size) {
                                        selectedZoneIndex = safeZoneIdx + 1
                                        selectedNodeId = questZones[safeZoneIdx + 1].nodes.firstOrNull()?.id
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ลุยด่านถัดไปทันที ▶", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // ส่วนล่าง: ลิสต์คำตอบแนะนำ (Smart Suggestion List) + ช่องพิมพ์คำตอบโต้ตอบ
        if (!isLessonCompleted) {
            Surface(
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📋 ลิสต์ประโยคแนะนำสำหรับตอบโต้:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                        Text(
                            text = if (showSuggestionList) "ซ่อนลิสต์ ▲" else "เปิดลิสต์ ▼",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.clickable { showSuggestionList = !showSuggestionList }
                        )
                    }

                    if (showSuggestionList) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(dynamicSuggestions) { opt ->
                                val zhDisplay = if (scriptMode == ScriptMode.SIMPLIFIED) opt.zhSimp else opt.zhTrad
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier
                                        .width(235.dp)
                                        .clickable {
                                            userInput = zhDisplay
                                            onSpeak(zhDisplay)
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(text = opt.tag, fontSize = 10.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "ส่งเลย ▶",
                                                fontSize = 10.sp,
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable {
                                                    onSpeak(zhDisplay)
                                                    handleSendAnswer(zhDisplay)
                                                }
                                            )
                                        }
                                        Text(text = zhDisplay, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(text = opt.th, fontSize = 11.sp, color = FrostedSlateLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // แถบปุ่มคำศัพท์ด่วนกดเติมลงช่องพิมพ์
                    val quickWords = listOf("你好", "请问", "多少钱", "不要放辣", "可以吗", "谢谢")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickWords) { w ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF172554),
                                modifier = Modifier.clickable { userInput += w }
                            ) {
                                Text(
                                    text = "+ $w",
                                    fontSize = 11.sp,
                                    color = Color(0xFF93C5FD),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                val autoText = expectedUserTurn?.second ?: "你好"
                                onSimulateUserSpeech(autoText)
                                onSpeak(autoText)
                                handleSendAnswer(autoText)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C3AED))
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = "พูด", tint = Color.White)
                        }

                        OutlinedTextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            placeholder = { Text("พิมพ์คำตอบภาษาจีน...", fontSize = 13.sp, color = FrostedSlateMuted) },
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
                            onClick = { handleSendAnswer(userInput) },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "ส่ง", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// โหมด 2: เกมการ์ดจับคู่สายฟ้าแลบ (Card Match Battle)
// =========================================================================
@Composable
private fun CardMatchBattleGame(
    scriptMode: ScriptMode,
    onSpeak: (String) -> Unit,
    onReward: (Int, String) -> Unit,
    onMistake: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var roundSeed by remember { mutableIntStateOf(0) }
    val tiles = remember(roundSeed, scriptMode) {
        val picked = ESSENTIAL_SURVIVAL_CARDS.shuffled().take(6)
        val deck = mutableListOf<MatchCardTile>()
        var uid = 1
        picked.forEach { item ->
            val zh = if (scriptMode == ScriptMode.SIMPLIFIED) item.zhSimp else item.zhTrad
            deck.add(MatchCardTile(uid++, item.id, true, zh, item.pinyin, zh))
            deck.add(MatchCardTile(uid++, item.id, false, item.th, item.category, zh))
        }
        deck.shuffled()
    }

    var selectedTile by remember(roundSeed) { mutableStateOf<MatchCardTile?>(null) }
    val matchedPairIds = remember(roundSeed) { mutableStateListOf<String>() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("🃏 เกมจับคู่การ์ดจำเป็นในจีน", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("จับคู่ตัวอักษรจีน ↔ คำแปลไทย (${matchedPairIds.size}/6 คู่)", fontSize = 12.sp, color = FrostedSlateLight)
                }
                IconButton(
                    onClick = { roundSeed += 1 },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "สุ่มใหม่", tint = Color(0xFF38BDF8))
                }
            }
        }

        items(tiles.chunked(2)) { rowPair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowPair.forEach { tile ->
                    val isMatched = matchedPairIds.contains(tile.pairId)
                    val isSelected = selectedTile?.uid == tile.uid
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            isMatched -> Color(0x2610B981)
                            isSelected -> Color(0xFF1D4ED8)
                            tile.isChineseSide -> Color(0xFF1E1B4B)
                            else -> Color(0xFF162033)
                        },
                        border = BorderStroke(
                            1.5.dp,
                            when {
                                isMatched -> Color(0xFF10B981)
                                isSelected -> Color(0xFF60A5FA)
                                tile.isChineseSide -> Color(0xFF6366F1)
                                else -> Color(0xFF334155)
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clickable(enabled = !isMatched) {
                                onSpeak(tile.speakText)
                                val prev = selectedTile
                                if (prev == null) {
                                    selectedTile = tile
                                } else if (prev.uid == tile.uid) {
                                    selectedTile = null
                                } else if (prev.pairId == tile.pairId && prev.isChineseSide != tile.isChineseSide) {
                                    matchedPairIds.add(tile.pairId)
                                    selectedTile = null
                                    onReward(20, "⚡ จับคู่สำเร็จ!")
                                } else {
                                    selectedTile = null
                                    onMistake("จับคู่ไม่ตรงกัน")
                                }
                            }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = if (isMatched) "✅ ${tile.displayMain}" else tile.displayMain,
                                fontSize = if (tile.isChineseSide) 16.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMatched) Color(0xFF34D399) else Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tile.displaySub,
                                fontSize = 11.sp,
                                color = Color(0xFF93C5FD),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (matchedPairIds.size >= 6) {
            item {
                Button(
                    onClick = { roundSeed += 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🎉 เคลียร์กระดานแล้ว! เล่นชุดการ์ดถัดไป ▶", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

// =========================================================================
// โหมด 3: การ์ดคำศัพท์และประโยคจำเป็นต้องใช้ (Survival Flashcards)
// =========================================================================
@Composable
private fun SurvivalFlashcardsGame(
    scriptMode: ScriptMode,
    onSpeak: (String) -> Unit,
    onReward: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var cardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember(cardIndex) { mutableStateOf(false) }

    val card = ESSENTIAL_SURVIVAL_CARDS[cardIndex % ESSENTIAL_SURVIVAL_CARDS.size]
    val zhMain = if (scriptMode == ScriptMode.SIMPLIFIED) card.zhSimp else card.zhTrad

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🗂️ การ์ดประโยคจำเป็น (${(cardIndex % ESSENTIAL_SURVIVAL_CARDS.size) + 1}/${ESSENTIAL_SURVIVAL_CARDS.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0x3338BDF8)) {
                Text(card.category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }

        // ตัวการ์ดหลัก (แตะเพื่อพลิกดูคำแปล + ประโยคตัวอย่าง)
        Surface(
            shape = RoundedCornerShape(24.dp),
            brush = Brush.verticalGradient(
                if (isFlipped) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                else listOf(Color(0xFF312E81), Color(0xFF1E1B4B))
            ),
            border = BorderStroke(2.dp, if (isFlipped) Color(0xFF10B981) else Color(0xFF818CF8)),
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .clickable {
                    isFlipped = !isFlipped
                    onSpeak(zhMain)
                }
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {
                IconButton(
                    onClick = { onSpeak(zhMain) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "ฟังเสียง", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = zhMain,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = card.pinyin,
                    fontSize = 15.sp,
                    color = Color(0xFF93C5FD),
                    textAlign = TextAlign.Center
                )

                if (isFlipped) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = card.th,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ตัวอย่าง: ${card.exampleZh}",
                        fontSize = 13.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = card.exampleTh,
                        fontSize = 12.sp,
                        color = FrostedSlateLight,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = "👆 แตะที่การ์ดเพื่อพลิกดูคำแปล & ตัวอย่างใช้จริง",
                        fontSize = 12.sp,
                        color = Color(0xFFC7D2FE)
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = {
                    cardIndex += 1
                    onSpeak(ESSENTIAL_SURVIVAL_CARDS[cardIndex % ESSENTIAL_SURVIVAL_CARDS.size].zhSimp)
                },
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("🔄 ทบทวนอีกที", fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
            }

            Button(
                onClick = {
                    onReward(15, "✅ จำการ์ดแม่นแล้ว!")
                    cardIndex += 1
                    onSpeak(ESSENTIAL_SURVIVAL_CARDS[cardIndex % ESSENTIAL_SURVIVAL_CARDS.size].zhSimp)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("✅ จำได้แม่น (+15 XP)", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

// =========================================================================
// โหมด 4: เกมการ์ดเรียงประโยค (Word-Card Sentence Builder)
// =========================================================================
@Composable
private fun SentenceBuilderCardGame(
    onSpeak: (String) -> Unit,
    onReward: (Int, String) -> Unit,
    onMistake: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var puzzleIdx by remember { mutableIntStateOf(0) }
    val puzzle = SENTENCE_PUZZLES[puzzleIdx % SENTENCE_PUZZLES.size]

    val bankWords = remember(puzzleIdx) { puzzle.targetWords.shuffled() }
    val chosenIndices = remember(puzzleIdx) { mutableStateListOf<Int>() }

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF162033),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(puzzle.category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("จงเรียงการ์ดเป็นภาษาจีน:", fontSize = 12.sp, color = FrostedSlateLight)
                    Text(puzzle.thPrompt, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("พินอิน: ${puzzle.pinyinHint}", fontSize = 11.sp, color = Color(0xFF93C5FD))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ช่องวางการ์ดประโยคที่ผู้เล่นเรียง
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.5.dp, Color(0xFF4F46E5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 95.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ประโยคที่คุณเรียง (แตะเพื่อเอาออก):", fontSize = 11.sp, color = FrostedSlateMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (chosenIndices.isEmpty()) {
                        Text("แตะการ์ดคำศัพท์ด้านล่างเพื่อเริ่มเรียงประโยค...", fontSize = 12.sp, color = Color(0xFF475569))
                    } else {
                        chosenIndices.chunked(3).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                                rowItems.forEach { bankIdx ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.clickable { chosenIndices.remove(bankIdx) }
                                    ) {
                                        Text(
                                            text = bankWords[bankIdx],
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // คลังการ์ดคำศัพท์ให้เลือกแตะ
            Text("คลังการ์ดคำศัพท์:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            bankWords.indices.toList().chunked(3).forEach { rowIdxList ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    rowIdxList.forEach { idx ->
                        val isUsed = chosenIndices.contains(idx)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isUsed) Color(0xFF0F172A) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (isUsed) Color(0xFF1E293B) else Color(0xFF475569)),
                            modifier = Modifier.clickable(enabled = !isUsed) {
                                onSpeak(bankWords[idx])
                                chosenIndices.add(idx)
                            }
                        ) {
                            Text(
                                text = bankWords[idx],
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUsed) Color(0xFF334155) else Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                val assembled = chosenIndices.map { bankWords[it] }
                val fullSentence = assembled.joinToString("")
                if (assembled == puzzle.targetWords) {
                    onSpeak(fullSentence)
                    onReward(35, "🧩 เรียงประโยคถูกต้องเป๊ะ!")
                    puzzleIdx += 1
                } else {
                    onMistake("ลำดับคำยังไม่ถูก ลองเรียงใหม่นะ")
                    chosenIndices.clear()
                }
            },
            enabled = chosenIndices.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("✅ ตรวจคำตอบประโยค", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 15.sp)
        }
    }
}
