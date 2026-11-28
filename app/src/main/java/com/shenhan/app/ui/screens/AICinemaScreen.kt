package com.shenhan.app.ui.screens
import com.shenhan.app.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenhan.app.data.CinemaGameBridge
import com.shenhan.app.data.CustomVocabPack
import com.shenhan.app.data.CustomVocabPackManager
import com.shenhan.app.data.GeminiSuggestion
import com.shenhan.app.data.GeminiTutorService
import com.shenhan.app.data.StoredChatMessage
import com.shenhan.app.data.CinemaChatHistoryStore
import com.shenhan.app.data.SmartNotificationManager
import com.shenhan.app.model.QuestZone
import com.shenhan.app.model.ScriptMode
import com.shenhan.app.ui.theme.FrostedSlateLight
import com.shenhan.app.ui.theme.FrostedSlateMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class GameArenaMode(val label: String, val icon: ImageVector) {
    AI_CHAT("แชทกับผู้ช่วยเฉิน", Icons.Default.AutoAwesome),
    CARD_MATCH("การ์ดจับคู่", Icons.Default.Style),
    FLASHCARDS("การ์ดจำเป็น", Icons.Default.Layers),
    SENTENCE_CARDS("เรียงประโยค", Icons.Default.Extension)
}

private fun categoryVectorIcon(category: String): ImageVector = when {
    category.contains("การเงิน") -> Icons.Default.CreditCard
    category.contains("อาหาร") -> Icons.Default.Restaurant
    category.contains("เครื่องดื่ม") -> Icons.Default.LocalCafe
    category.contains("ช่วยเหลือ") -> Icons.Default.PhotoCamera
    category.contains("ฉุกเฉิน") -> Icons.Default.Warning
    category.contains("เดินทาง") -> Icons.Default.DirectionsCar
    category.contains("ธนาคาร") -> Icons.Default.AccountBalance
    category.contains("ความรัก") -> Icons.Default.Favorite
    category.contains("กลับ") -> Icons.Default.FlightTakeoff
    category.contains("ซิม") -> Icons.Default.PhoneAndroid
    category.contains("ทันใจ") -> Icons.Default.Bolt
    else -> Icons.Default.SupportAgent
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

private data class EssentialCardItem(
    val id: String,
    val zhSimp: String,
    val zhTrad: String,
    val pinyin: String,
    val th: String,
    val category: String,
    val exampleZhSimp: String,
    val exampleZhTrad: String,
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
    val targetWordsSimp: List<String>,
    val targetWordsTrad: List<String>,
    val category: String
)

private val ESSENTIAL_SURVIVAL_CARDS = listOf(
    EssentialCardItem(
        id = "c1",
        zhSimp = "多少钱？",
        zhTrad = "多少錢？",
        pinyin = "Duōshǎo qián?",
        th = "ราคาเท่าไหร่?",
        category = "การเงิน",
        exampleZhSimp = "请问这个多少钱？",
        exampleZhTrad = "請問這個多少錢？",
        exampleTh = "ขอถามหน่อยว่าอันนี้ราคาเท่าไหร่?"
    ),
    EssentialCardItem(
        id = "c2",
        zhSimp = "我扫支付宝",
        zhTrad = "我掃支付寶",
        pinyin = "Wǒ sǎo Zhīfùbǎo",
        th = "ฉันสแกนจ่าย Alipay",
        category = "การเงิน",
        exampleZhSimp = "可以刷微信还是我扫支付宝？",
        exampleZhTrad = "可以刷微信還是我掃支付寶？",
        exampleTh = "สแกน WeChat หรือให้ฉันสแกน Alipay?"
    ),
    EssentialCardItem(
        id = "c3",
        zhSimp = "不要放牛肉和花生",
        zhTrad = "不要放牛肉和花生",
        pinyin = "Bù yào fàng niúròu hé huāshēng",
        th = "ไม่ใส่เนื้อวัวและถั่วลิสง",
        category = "อาหาร",
        exampleZhSimp = "老板，炒饭不要放牛肉和花生。",
        exampleZhTrad = "老闆，炒飯不要放牛肉和花生。",
        exampleTh = "เถ้าแก่ ข้าวผัดไม่ใส่เนื้อวัวกับถั่วลิสง"
    ),
    EssentialCardItem(
        id = "c4",
        zhSimp = "珍珠奶茶半糖去冰",
        zhTrad = "珍珠奶茶半糖去冰",
        pinyin = "Zhēnzhū nǎichá bàn táng qù bīng",
        th = "ชานมไข่มุกหวาน 50% ไม่ใส่น้ำแข็ง",
        category = "เครื่องดื่ม",
        exampleZhSimp = "我要一杯珍珠奶茶，半糖去冰。",
        exampleZhTrad = "我要一杯珍珠奶茶，半糖去冰。",
        exampleTh = "เอาชานมไข่มุก 1 แก้ว หวานครึ่งเดียวไม่ใส่น้ำแข็ง"
    ),
    EssentialCardItem(
        id = "c5",
        zhSimp = "麻烦帮我拍张照",
        zhTrad = "麻煩幫我拍張照",
        pinyin = "Máfan bāng wǒ pāi zhāng zhào",
        th = "รบกวนช่วยถ่ายรูปให้ฉันหน่อย",
        category = "ช่วยเหลือ",
        exampleZhSimp = "你好，麻烦帮我们拍张合照可以吗？",
        exampleZhTrad = "你好，麻煩幫我們拍張合照可以嗎？",
        exampleTh = "สวัสดี รบกวนช่วยถ่ายรูปหมู่ให้พวกเราหน่อยได้ไหม?"
    ),
    EssentialCardItem(
        id = "c6",
        zhSimp = "请帮我打110 / 120",
        zhTrad = "請幫我打110 / 120",
        pinyin = "Qǐng bāng wǒ dǎ 110 / 120",
        th = "ช่วยโทรแจ้งตำรวจ 110 / รถพยาบาล 120",
        category = "ฉุกเฉิน",
        exampleZhSimp = "发生意外了，请帮我打120叫救护车！",
        exampleZhTrad = "發生意外了，請幫我打120叫救護車！",
        exampleTh = "เกิดอุบัติเหตุแล้ว ช่วยโทร 120 เรียกรถพยาบาลที!"
    ),
    EssentialCardItem(
        id = "c7",
        zhSimp = "手机尾号是...",
        zhTrad = "手機尾號是...",
        pinyin = "Shǒujī wěihào shì...",
        th = "เลขท้ายเบอร์มือถือ 4 ตัวคือ...",
        category = "เดินทาง",
        exampleZhSimp = "师傅你好，我的手机尾号是 8899。",
        exampleZhTrad = "師傅你好，我的手機尾號是 8899。",
        exampleTh = "สวัสดีครับคนขับ เลขท้ายเบอร์ผมคือ 8899"
    ),
    EssentialCardItem(
        id = "c8",
        zhSimp = "我想办一张银行卡",
        zhTrad = "我想辦一張銀行卡",
        pinyin = "Wǒ xiǎng bàn yī zhāng yínhángkǎ",
        th = "ฉันอยากเปิดบัตรธนาคาร 1 ใบ",
        category = "ธนาคาร",
        exampleZhSimp = "我是泰国人，想办一张中国银行卡。",
        exampleZhTrad = "我是泰國人，想辦一張中國銀行卡。",
        exampleTh = "ฉันเป็นคนไทย อยากเปิดบัตรธนาคารจีน 1 ใบ"
    ),
    EssentialCardItem(
        id = "c9",
        zhSimp = "我喜欢你，在一起吧",
        zhTrad = "我喜歡你，在一起吧",
        pinyin = "Wǒ xǐhuān nǐ, zài yīqǐ ba",
        th = "ฉันชอบเธอ คบกันนะ",
        category = "ความรัก",
        exampleZhSimp = "认识你很开心，我喜欢你，在一起吧！",
        exampleZhTrad = "認識你很開心，我喜歡你，在一起吧！",
        exampleTh = "ดีใจที่ได้รู้จักเธอ ฉันชอบเธอ คบกันนะ!"
    ),
    EssentialCardItem(
        id = "c10",
        zhSimp = "我要办理飞往曼谷的登机",
        zhTrad = "我要辦理飛往曼谷的登機",
        pinyin = "Wǒ yào bànlǐ fēiwǎng Màngǔ de dēngjī",
        th = "ฉันต้องการเช็กอินบินกลับกรุงเทพฯ",
        category = "กลับไท่กั๋ว",
        exampleZhSimp = "你好，我要办理飞往泰国曼谷的登机手续。",
        exampleZhTrad = "你好，我要辦理飛往泰國曼谷的登機手續。",
        exampleTh = "สวัสดี ฉันต้องการเช็กอินเที่ยวบินไปกรุงเทพฯ ประเทศไทย"
    )
)

private val SENTENCE_PUZZLES = listOf(
    SentencePuzzle(
        thPrompt = "เอาชานมไข่มุก 1 แก้ว หวานครึ่งเดียวไม่ใส่น้ำแข็ง",
        pinyinHint = "Wǒ yào yī bēi zhēnzhū nǎichá bàn táng qù bīng",
        targetWordsSimp = listOf("我要", "一杯", "珍珠奶茶", "半糖", "去冰"),
        targetWordsTrad = listOf("我要", "一杯", "珍珠奶茶", "半糖", "去冰"),
        category = "สั่งเครื่องดื่ม"
    ),
    SentencePuzzle(
        thPrompt = "ไม่ใส่เนื้อวัวและถั่วลิสง เผ็ดนิดเดียวพอ",
        pinyinHint = "Bù yào fàng niúròu hé huāshēng wēi là jiù hǎo",
        targetWordsSimp = listOf("不要放", "牛肉", "和", "花生", "微辣", "就好"),
        targetWordsTrad = listOf("不要放", "牛肉", "和", "花生", "微辣", "就好"),
        category = "เอาตัวรอดร้านอาหาร"
    ),
    SentencePuzzle(
        thPrompt = "สวัสดี ฉันเป็นคนไทย อยากเปิดซิมมือถือจีน",
        pinyinHint = "Nǐ hǎo wǒ shì Tàiguórén xiǎng bàn shǒujīkǎ",
        targetWordsSimp = listOf("你好", "我是", "泰国人", "想办", "手机卡"),
        targetWordsTrad = listOf("你好", "我是", "泰國人", "想辦", "手機卡"),
        category = "เปิดซิมในจีน"
    ),
    SentencePuzzle(
        thPrompt = "รบกวนช่วยถ่ายรูปให้พวกเราหน่อยได้ไหม?",
        pinyinHint = "Máfan bāng wǒmen pāi zhāng zhào kěyǐ ma",
        targetWordsSimp = listOf("麻烦", "帮我们", "拍张照", "可以吗"),
        targetWordsTrad = listOf("麻煩", "幫我們", "拍張照", "可以嗎"),
        category = "ขอความช่วยเหลือ"
    ),
    SentencePuzzle(
        thPrompt = "ฉันต้องการเช็กอินเที่ยวบินกลับกรุงเทพฯ ประเทศไทย",
        pinyinHint = "Wǒ yào bànlǐ fēiwǎng Tàiguó Màngǔ de dēngjī",
        targetWordsSimp = listOf("我要", "办理", "飞往", "泰国曼谷", "的登机"),
        targetWordsTrad = listOf("我要", "辦理", "飛往", "泰國曼谷", "的登機"),
        category = "บินกลับไท่กั๋ว"
    )
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
    modifier: Modifier = Modifier,
    audioSpeed: Float = 1.0f,
    onCompleteNode: (String, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val questZones by CinemaGameBridge.questZones.collectAsState()
    val activeCinemaNodeId by CinemaGameBridge.activeCinemaNodeId.collectAsState()
    val customPacks by CinemaGameBridge.customPacks.collectAsState()
    val selectedCustomPackId by CinemaGameBridge.selectedCustomPackId.collectAsState()
    val requestedModeIndex by CinemaGameBridge.requestedModeIndex.collectAsState()

    var currentMode by remember { mutableStateOf(GameArenaMode.AI_CHAT) }
    var hearts by remember { mutableIntStateOf(5) }
    var comboCount by remember { mutableIntStateOf(0) }
    var chestCharge by remember { mutableIntStateOf(1) }

    // สถานะโมเดล AI ที่เชื่อมต่ออัตโนมัติ (ผู้ใช้ไม่ต้องกรอก API Key)
    var activeModelBadge by remember { mutableStateOf("ผู้ช่วยเฉิน") }

    LaunchedEffect(requestedModeIndex) {
        val reqIdx = requestedModeIndex
        if (reqIdx != null) {
            GameArenaMode.entries.getOrNull(reqIdx)?.let { currentMode = it }
            CinemaGameBridge.clearRequestedMode()
        }
    }

    LaunchedEffect(activeCinemaNodeId) {
        if (activeCinemaNodeId != null && requestedModeIndex == null) {
            currentMode = GameArenaMode.AI_CHAT
        }
    }

    fun rewardGameSuccess(baseXp: Int, reason: String) {
        comboCount = (comboCount + 1).coerceAtMost(5)
        chestCharge = (chestCharge + 1).coerceAtMost(3)
        val multiplier = when (comboCount) {
            5 -> 2.0
            4 -> 1.75
            3 -> 1.5
            2 -> 1.25
            else -> 1.0
        }
        val totalXp = (baseXp * multiplier).toInt()
        onAddXp(totalXp)
        if (comboCount == 5 && hearts < 5) {
            hearts += 1
            Toast.makeText(context, "COMBO x$comboCount! +$totalXp XP & ฟื้นหัวใจ +1", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "$reason (+$totalXp XP • COMBO x$comboCount)", Toast.LENGTH_SHORT).show()
        }
    }

    fun penalizeMistake(hint: String) {
        comboCount = 0
        hearts = (hearts - 1).coerceAtLeast(1)
        Toast.makeText(context, "ยังไม่ตรง ($hint) • เสีย 1 หัวใจ", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080D19))
            .testTag("ai_cinema_screen")
    ) {
        // แถบ HUD เกมด้านบน + สถานะโมเดล Gemini + โหมดทั้ง 4
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // หัวใจพลังชีวิต ❤️ 5/5
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x26EF4444),
                            border = BorderStroke(1.dp, Color(0x55EF4444))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Hearts",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$hearts/5",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }

                        // ตัวคูณคอมโบต่อเนื่อง COMBO x1..x5
                        val displayCombo = comboCount.coerceIn(1, 5)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (comboCount > 0) Color(0x33F59E0B) else Color(0xFF1E293B),
                            border = BorderStroke(
                                1.dp,
                                if (comboCount > 0) Color(0x88F59E0B) else Color(0xFF334155)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Combo",
                                    tint = if (comboCount > 0) Color(0xFFFBBF24) else FrostedSlateLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "COMBO x$displayCombo",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (comboCount > 0) Color(0xFFFBBF24) else FrostedSlateLight
                                )
                            }
                        }

                        // กล่องสมบัติโบนัส 3/3 (+100 XP & ฟื้นหัวใจเต็ม)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (chestCharge >= 3) Color(0xFFF59E0B) else Color(0xFF1E293B),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (chestCharge >= 3) {
                                        chestCharge = 0
                                        hearts = 5
                                        onAddXp(100)
                                        Toast.makeText(
                                            context,
                                            "เปิดกล่องสมบัติสำเร็จ! +100 XP & หัวใจเต็ม",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "สะสมอีก ${3 - chestCharge} แต้มเพื่อเปิดกล่องสมบัติ (+100 XP & ฟื้นหัวใจเต็ม)!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .testTag("cinema_bonus_chest_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = "Bonus Chest",
                                    tint = if (chestCharge >= 3) Color.Black else Color(0xFFFBBF24),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (chestCharge >= 3) "เปิด!" else "$chestCharge/3",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (chestCharge >= 3) Color.Black else Color(0xFFFBBF24)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // ปุ่มสลับความเร็ว TTS
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val nextSpeed = when {
                                        audioSpeed < 0.9f -> 1.0f
                                        audioSpeed < 1.15f -> 1.25f
                                        else -> 0.75f
                                    }
                                    onChangeAudioSpeed(nextSpeed)
                                    Toast.makeText(context, "ความเร็วเสียง ${nextSpeed}x", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "ความเร็วเสียง",
                                    tint = if (isTtsSpeaking) Color(0xFF34D399) else Color(0xFF93C5FD),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${audioSpeed}x",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // ป้ายแสดงโมเดล Gemini ที่ทำงานอัตโนมัติ
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x2638BDF8),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.testTag("gemini_model_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini AI",
                                    tint = Color(0xFF7DD3FC),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeModelBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7DD3FC),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GameArenaMode.entries) { mode ->
                        val isSelected = currentMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF162033),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF60A5FA) else Color(0xFF26334D)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { currentMode = mode }
                                .testTag("cinema_mode_${mode.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = mode.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        when (currentMode) {
            GameArenaMode.AI_CHAT -> GeminiInteractiveChatGame(
                questZones = questZones,
                activeCinemaNodeId = activeCinemaNodeId,
                scriptMode = scriptMode,
                isAudioRecording = isAudioRecording,
                onModelResolved = { model -> activeModelBadge = model },
                onSpeak = onSpeak,
                onSimulateUserSpeech = onSimulateUserSpeech,
                onReward = ::rewardGameSuccess,
                onCompleteNode = onCompleteNode,
                modifier = Modifier.weight(1f)
            )
            GameArenaMode.CARD_MATCH -> CardMatchBattleGame(
                questZones = questZones,
                customPacks = customPacks,
                selectedCustomPackId = selectedCustomPackId,
                onSelectPackId = { CinemaGameBridge.selectCustomPackFilter(it) },
                scriptMode = scriptMode,
                onSpeak = onSpeak,
                onReward = ::rewardGameSuccess,
                onMistake = ::penalizeMistake,
                modifier = Modifier.weight(1f)
            )
            GameArenaMode.FLASHCARDS -> SurvivalFlashcardsGame(
                questZones = questZones,
                customPacks = customPacks,
                selectedCustomPackId = selectedCustomPackId,
                onSelectPackId = { CinemaGameBridge.selectCustomPackFilter(it) },
                scriptMode = scriptMode,
                onSpeak = onSpeak,
                onReward = ::rewardGameSuccess,
                modifier = Modifier.weight(1f)
            )
            GameArenaMode.SENTENCE_CARDS -> SentenceBuilderCardGame(
                questZones = questZones,
                customPacks = customPacks,
                selectedCustomPackId = selectedCustomPackId,
                onSelectPackId = { CinemaGameBridge.selectCustomPackFilter(it) },
                scriptMode = scriptMode,
                onSpeak = onSpeak,
                onReward = ::rewardGameSuccess,
                onMistake = ::penalizeMistake,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =========================================================================
// โหมด 1: เกมแชทพิมพ์โต้ตอบสดกับ Google Gemini + ลิสต์ตัวช่วยคำตอบไดนามิก
// =========================================================================
@Composable
private fun GeminiInteractiveChatGame(
    questZones: List<QuestZone>,
    activeCinemaNodeId: String?,
    scriptMode: ScriptMode,
    isAudioRecording: Boolean,
    onModelResolved: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onSimulateUserSpeech: (String) -> Unit,
    onReward: (Int, String) -> Unit,
    onCompleteNode: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
            CinemaGameBridge.clearActiveCinemaNode()
        }
    }

    if (questZones.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "กำลังซิงก์ข้อมูลด่านจากแผนที่...", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "กรุณารอสักครู่ หรือกลับไปเลือกด่านจากแผนที่เควสต์", color = FrostedSlateLight, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        return
    }

    val safeZoneIdx = selectedZoneIndex.coerceIn(0, questZones.lastIndex)
    val currentZone = questZones[safeZoneIdx]
    val activeNode = currentZone.nodes.find { it.id == selectedNodeId }
        ?: currentZone.nodes.firstOrNull { it.isCurrent && it.isUnlocked }
        ?: currentZone.nodes.firstOrNull()
        ?: return

    var userInput by remember(activeNode.id) { mutableStateOf("") }
    var isGeminiTyping by remember(activeNode.id) { mutableStateOf(false) }
    var showSuggestionList by remember(activeNode.id) { mutableStateOf(true) }
    var passedTurns by remember(activeNode.id) { mutableIntStateOf(0) }
    var isLessonCompleted by remember(activeNode.id) { mutableStateOf(false) }
    var sessionSeed by remember(activeNode.id) { mutableIntStateOf(0) }

    val chatHistory = remember(activeNode.id, sessionSeed) { mutableStateListOf<CinemaChatBubble>() }
    val dynamicSuggestions = remember(activeNode.id, sessionSeed) { mutableStateListOf<GeminiSuggestion>() }

    fun loadFallbackSuggestions(turnOffset: Int = 0) {
        dynamicSuggestions.clear()
        val turns = activeNode.dialogueTurns
        val learnerTurns = turns.filter { it.speaker.equals("user", ignoreCase = true) || it.speaker.equals("learner", ignoreCase = true) }
        val scriptSource = if (learnerTurns.isNotEmpty()) learnerTurns else turns
        val picked = scriptSource.drop(turnOffset).take(3)
        if (picked.isNotEmpty()) {
            picked.forEachIndexed { idx, line ->
                val zh = if (scriptMode == ScriptMode.SIMPLIFIED) line.hans else line.hant
                dynamicSuggestions.add(
                    GeminiSuggestion(
                        zh = zh,
                        pinyin = line.pinyin,
                        th = line.th,
                        tag = if (idx == 0) "แนะนำตามบท" else "ประโยคฝึกพูด"
                    )
                )
            }
            return
        }
        if (scriptMode == ScriptMode.SIMPLIFIED) {
            dynamicSuggestions.add(GeminiSuggestion("你好！我想练习《${activeNode.titleZhSimp}》。", "Nǐ hǎo! Wǒ xiǎng liànxí zhège chǎngjǐng.", "สวัสดี! ฉันอยากฝึกบทสนทนาสถานการณ์นี้", "เริ่มภารกิจ"))
            dynamicSuggestions.add(GeminiSuggestion("请问这个怎么说？", "Qǐngwèn zhège zěnme shuō?", "ขอถามหน่อยว่าอันนี้พูดว่าอย่างไร?", "ถามต่อ"))
            dynamicSuggestions.add(GeminiSuggestion("好的，我明白了，谢谢！", "Hǎo de, wǒ míngbai le, xièxie!", "โอเค ฉันเข้าใจแล้ว ขอบคุณ!", "ตอบตกลง"))
        } else {
            dynamicSuggestions.add(GeminiSuggestion("你好！我想練習《${activeNode.titleZhTrad}》。", "Nǐ hǎo! Wǒ xiǎng liànxí zhège chǎngjǐng.", "สวัสดี! ฉันอยากฝึกบทสนทนาสถานการณ์นี้", "เริ่มภารกิจ"))
            dynamicSuggestions.add(GeminiSuggestion("請問這個怎麼說？", "Qǐngwèn zhège zěnme shuō?", "ขอถามหน่อยว่าอันนี้พูดว่าอย่างไร?", "ถามต่อ"))
            dynamicSuggestions.add(GeminiSuggestion("好的，我明白了，謝謝！", "Hǎo de, wǒ míngbai le, xièxie!", "โอเค ฉันเข้าใจแล้ว ขอบคุณ!", "ตอบตกลง"))
        }
    }

    // เริ่มต้นด่าน: เรียก Gemini เปิดฉากสนทนาตามสถานการณ์จริงอัตโนมัติ
    var lastResetSeed by remember(activeNode.id) { mutableStateOf(sessionSeed) }

    fun persistCurrentChat() {
        CinemaChatHistoryStore.saveSession(
            context = context,
            nodeId = activeNode.id,
            messages = chatHistory.map {
                StoredChatMessage(
                    id = it.id,
                    isUser = it.isUser,
                    zhText = it.zhText,
                    pinyin = it.pinyin,
                    thText = it.thText,
                    coachFeedback = it.coachFeedback,
                    accuracyPct = it.accuracyPct
                )
            },
            passedTurns = passedTurns,
            isLessonCompleted = isLessonCompleted
        )
    }

    LaunchedEffect(activeNode.id, scriptMode, sessionSeed) {
        val isManualReset = sessionSeed != lastResetSeed
        lastResetSeed = sessionSeed

        if (isManualReset) {
            CinemaChatHistoryStore.clearSession(context, activeNode.id)
        } else {
            val savedSession = CinemaChatHistoryStore.loadSession(context, activeNode.id)
            if (savedSession != null && savedSession.messages.isNotEmpty()) {
                chatHistory.clear()
                chatHistory.addAll(
                    savedSession.messages.map {
                        CinemaChatBubble(
                            id = (it.id ?: ""),
                            isUser = it.isUser,
                            zhText = (it.zhText ?: ""),
                            pinyin = (it.pinyin ?: ""),
                            thText = (it.thText ?: ""),
                            coachFeedback = it.coachFeedback,
                            accuracyPct = it.accuracyPct,
                            animateTyping = false
                        )
                    }
                )
                passedTurns = savedSession.passedTurns
                isLessonCompleted = savedSession.isLessonCompleted
                loadFallbackSuggestions(savedSession.passedTurns)
                return@LaunchedEffect
            }
        }
        chatHistory.clear()
        passedTurns = 0
        isLessonCompleted = false
        loadFallbackSuggestions(0)

        isGeminiTyping = true
        val res = GeminiTutorService.generateTurn(
            context = context,
            node = activeNode,
            scriptMode = scriptMode,
            historyPairs = emptyList(),
            latestUserMessage = null
        )
        isGeminiTyping = false
        res.onSuccess { turn ->
            onModelResolved(turn.usedModel)
            chatHistory.add(
                CinemaChatBubble(
                    id = "gem_open_${System.currentTimeMillis()}",
                    isUser = false,
                    zhText = turn.replyZh,
                    pinyin = turn.replyPinyin,
                    thText = turn.replyTh,
                    coachFeedback = turn.coachFeedbackTh,
                    animateTyping = true
                )
            )
            onSpeak(turn.replyZh)
            if (turn.suggestions.isNotEmpty()) {
                dynamicSuggestions.clear()
                dynamicSuggestions.addAll(turn.suggestions)
            }
        }.onFailure {
            val firstTurn = activeNode.dialogueTurns.firstOrNull()
            val openZh = if (firstTurn != null) {
                if (scriptMode == ScriptMode.SIMPLIFIED) firstTurn.hans else firstTurn.hant
            } else if (scriptMode == ScriptMode.SIMPLIFIED) {
                "你好！欢迎来到《${activeNode.titleZhSimp}》，我们开始对话吧！"
            } else {
                "你好！歡迎來到《${activeNode.titleZhTrad}》，我們開始對話吧！"
            }
            val openPinyin = firstTurn?.pinyin ?: "Nǐ hǎo! Wǒmen kāishǐ duìhuà ba!"
            val openTh = firstTurn?.th ?: "สวัสดี! ยินดีต้อนรับสู่ด่าน ${activeNode.titleTh} มาเริ่มสนทนากันเลย!"
            chatHistory.add(
                CinemaChatBubble(
                    id = "scene_open_${System.currentTimeMillis()}",
                    isUser = false,
                    zhText = openZh,
                    pinyin = openPinyin,
                    thText = openTh,
                    coachFeedback = "พร้อมฝึกสนทนาสถานการณ์: ${activeNode.titleTh}",
                    animateTyping = true
                )
            )
            onSpeak(openZh)
        }
    }

    fun sendMessageToGemini(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || isGeminiTyping) return

        val historyBefore = chatHistory.map { Pair(it.isUser, if (it.thText.isNotBlank() && !it.thText.startsWith("ส่งข้อความ") && !it.thText.startsWith("ตรวจโดย")) "${it.zhText} (${it.thText})" else it.zhText) }
        val userBubbleId = "usr_${System.currentTimeMillis()}"
        chatHistory.add(
            CinemaChatBubble(
                id = userBubbleId,
                isUser = true,
                zhText = text,
                pinyin = "กำลังวิเคราะห์คำตอบ...",
                thText = "ส่งให้ผู้ช่วยเฉินตรวจแล้ว"
            )
        )
        SmartNotificationManager.markStudiedToday(context)
        persistCurrentChat()
        userInput = ""
        isGeminiTyping = true

        scope.launch {
            val res = GeminiTutorService.generateTurn(
                context = context,
                node = activeNode,
                scriptMode = scriptMode,
                historyPairs = historyBefore,
                latestUserMessage = text
            )
            isGeminiTyping = false
            res.onSuccess { turn ->
                onModelResolved(turn.usedModel)
                val uIdx = chatHistory.indexOfFirst { it.id == userBubbleId }
                if (uIdx != -1) {
                    chatHistory[uIdx] = chatHistory[uIdx].copy(
                        pinyin = "ความแม่นยำ ${turn.accuracyPct}%",
                        thText = "ตรวจโดยผู้ช่วยเฉิน",
                        coachFeedback = turn.coachFeedbackTh,
                        accuracyPct = turn.accuracyPct
                    )
                }

                chatHistory.add(
                    CinemaChatBubble(
                        id = "gem_${System.currentTimeMillis()}",
                        isUser = false,
                        zhText = turn.replyZh,
                        pinyin = turn.replyPinyin,
                        thText = turn.replyTh,
                        animateTyping = true
                    )
                )
                persistCurrentChat()
                persistCurrentChat()
                onSpeak(turn.replyZh)

                if (turn.suggestions.isNotEmpty()) {
                    dynamicSuggestions.clear()
                    dynamicSuggestions.addAll(turn.suggestions)
                }

                onReward(25, "ผู้ช่วยเฉินประเมินผ่าน")
                if (turn.stepPassed) {
                    passedTurns += 1
                }
                if (passedTurns >= 3 && !isLessonCompleted) {
                    isLessonCompleted = true
                    CinemaGameBridge.completeQuestNode(activeNode.id, 3)
                    onCompleteNode(activeNode.id, 3)
                    onReward(activeNode.xpReward, "ผ่านด่านผู้ช่วยเฉิน 3 ดาว!")
                    SmartNotificationManager.notifyProgressMilestone(context, "🏆 ผ่านด่าน ${activeNode.titleTh} สำเร็จ!", "ผู้ช่วยเฉินมอบคะแนน +${activeNode.xpReward} XP ลุยด่านถัดไปกันเลยครับ")
                }
            }.onFailure { err ->
                val errMsg = err.message ?: "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้"
                val uIdx = chatHistory.indexOfFirst { it.id == userBubbleId }
                if (uIdx != -1) {
                    chatHistory[uIdx] = chatHistory[uIdx].copy(
                        pinyin = "เชื่อมต่อไม่สำเร็จ",
                        thText = "สัญญาณอินเทอร์เน็ตช้า ลองกดส่งอีกครั้งนะครับ",
                        coachFeedback = "⚠️ ระบบแจ้งเตือน: ${errMsg.take(140)}",
                        accuracyPct = 0
                    )
                    persistCurrentChat()
                }
            }
        }
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
                // แถบเลือกหมวดโซน (ไฟล์ของฉัน + ทันใจ + HSK 1-6)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(questZones) { idx, zone ->
                        val isSel = idx == safeZoneIdx
                        val isFav = zone.id == "zone_favorite"
                        val isCustomZone = zone.id == CustomVocabPackManager.CUSTOM_ZONE_ID
                        val selColor = when {
                            isCustomZone -> Color(0xFF8B5CF6)
                            isFav -> Color(0xFFF59E0B)
                            else -> Color(0xFF06B6D4)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) selColor else Color(0xFF151E32),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedZoneIndex = idx
                                    selectedNodeId = zone.nodes.firstOrNull()?.id
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                if (isFav || isCustomZone) {
                                    Icon(
                                        imageVector = if (isCustomZone) Icons.Default.FolderSpecial else Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = when {
                                            isSel && isFav -> Color.Black
                                            isCustomZone -> if (isSel) Color.White else Color(0xFFC4B5FD)
                                            else -> Color(0xFFFBBF24)
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = when {
                                        isCustomZone -> "ไฟล์ของฉัน"
                                        isFav -> "ทันใจ"
                                        else -> zone.hskLevel.title
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel && isFav) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // แถบสถานะการซิงก์ด่าน + รอบสนทนา + ปุ่มรีเซ็ตด่าน
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ซิงก์แล้ว ${questZones.sumOf { it.nodes.size }} ด่าน • รอบสนทนา ${passedTurns.coerceAtMost(3)} / 3",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.clickable { sessionSeed += 1 }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "เริ่มด่านใหม่",
                                tint = FrostedSlateLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "เริ่มด่านใหม่",
                                fontSize = 11.sp,
                                color = FrostedSlateLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentZone.nodes) { node ->
                        val isCur = node.id == activeNode.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCur) Color(0xFF1E293B) else Color(0xFF111827),
                            border = BorderStroke(1.dp, if (isCur) Color(0xFF38BDF8) else Color(0xFF1F2937)),
                            modifier = Modifier
                                .width(190.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedNodeId = node.id }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (node.starsEarned > 0) {
                                        repeat(node.starsEarned.coerceIn(1, 3)) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        Text(
                                            text = "ผ่านแล้ว",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "คุยกับผู้ช่วยเฉิน",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }
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

            // บอลลูนแชทโต้ตอบ (พร้อมเอฟเฟกต์พิมพ์ตัวอักษรไหลทีละตัว Typewriter)
            items(chatHistory, key = { it.id }) { msg ->
                var visibleChars by remember(msg.id) {
                    mutableIntStateOf(if (msg.animateTyping) 0 else msg.zhText.length)
                }
                LaunchedEffect(msg.id) {
                    if (msg.animateTyping) {
                        for (i in 1..msg.zhText.length) {
                            visibleChars = i
                            delay(24)
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
                        modifier = Modifier.widthIn(max = 315.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (!msg.isUser) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = if (msg.isUser) "คุณ • ${msg.accuracyPct ?: 90}%" else "ผู้ช่วยเฉิน",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (msg.isUser) Color(0xFFBFDBFE) else Color(0xFF38BDF8)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "ฟังเสียง",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(20.dp)
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

            if (isGeminiTyping) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "ผู้ช่วยเฉินกำลังพิมพ์...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ผ่านการวัดผลกับผู้ช่วยเฉินเรียบร้อย!",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(3) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
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
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ลุยด่านถัดไปทันที", fontWeight = FontWeight.Bold, color = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // ส่วนล่าง: ลิสต์ตัวเลือกคำตอบที่ Gemini สร้างให้สดใหม่ทุกเทิร์น + ช่องพิมพ์โต้ตอบอิสระ
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "คำตอบแนะนำจากผู้ช่วยเฉิน:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.clickable { showSuggestionList = !showSuggestionList }
                    ) {
                        Text(
                            text = if (showSuggestionList) "ซ่อนลิสต์" else "เปิดลิสต์",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8)
                        )
                        Icon(
                            imageVector = if (showSuggestionList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                if (showSuggestionList) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dynamicSuggestions) { opt ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .width(240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        userInput = opt.zh
                                        onSpeak(opt.zh)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = opt.tag,
                                            fontSize = 10.sp,
                                            color = Color(0xFF34D399),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            modifier = Modifier.clickable {
                                                onSpeak(opt.zh)
                                                sendMessageToGemini(opt.zh)
                                            }
                                        ) {
                                            Text(
                                                text = "ส่งเลย",
                                                fontSize = 10.sp,
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = opt.zh,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = opt.th,
                                        fontSize = 11.sp,
                                        color = FrostedSlateLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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
                            val quickPick = dynamicSuggestions.firstOrNull()?.zh ?: "你好"
                            onSimulateUserSpeech(quickPick)
                            onSpeak(quickPick)
                            sendMessageToGemini(quickPick)
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isAudioRecording) Color(0xFFEF4444) else Color(0xFF7C3AED))
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = "พูด", tint = Color.White)
                    }

                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        placeholder = {
                            Text(
                                text = "พิมพ์จีนหรือถามผู้ช่วยเฉินได้เลย...",
                                fontSize = 13.sp,
                                color = FrostedSlateMuted
                            )
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
                        onClick = { sendMessageToGemini(userInput) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
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

@Composable
private fun CustomPackFilterStrip(
    customPacks: List<CustomVocabPack>,
    selectedCustomPackId: String?,
    onSelectPackId: (String?) -> Unit
) {
    if (customPacks.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            val isAll = selectedCustomPackId == null
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isAll) Color(0xFF06B6D4) else Color(0xFF162033),
                border = BorderStroke(1.dp, if (isAll) Color(0xFF67E8F9) else Color(0xFF334155)),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectPackId(null) }
            ) {
                Text(
                    text = "ทั้งหมด (ด่าน + ไฟล์)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAll) Color.Black else Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        items(customPacks, key = { it.id }) { pack ->
            val isSel = selectedCustomPackId == pack.id
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSel) Color(0xFF8B5CF6) else Color(0xFF162033),
                border = BorderStroke(1.dp, if (isSel) Color(0xFFC4B5FD) else Color(0xFF334155)),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectPackId(pack.id) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = if (isSel) Color.White else Color(0xFFC4B5FD),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${pack.name} (${pack.entries.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun buildDynamicCardsFromZones(
    zones: List<QuestZone>,
    customPacks: List<CustomVocabPack>,
    selectedPackId: String?
): List<EssentialCardItem> {
    if (selectedPackId != null) {
        val targetPack = customPacks.firstOrNull { it.id == selectedPackId }
        if (targetPack != null && targetPack.entries.isNotEmpty()) {
            return targetPack.entries.mapIndexed { idx, e ->
                EssentialCardItem(
                    id = "pack_${targetPack.id}_$idx",
                    zhSimp = e.simplified,
                    zhTrad = e.traditional.ifBlank { e.simplified },
                    pinyin = e.pinyin,
                    th = e.meaningTh,
                    category = targetPack.name,
                    exampleZhSimp = e.exampleZhSimp.ifBlank { e.simplified },
                    exampleZhTrad = e.exampleZhTrad.ifBlank { e.traditional.ifBlank { e.simplified } },
                    exampleTh = e.exampleMeaningTh.ifBlank { e.meaningTh }
                )
            }
        }
    }

    val packCards = mutableListOf<EssentialCardItem>()
    customPacks.forEach { pack ->
        pack.entries.forEachIndexed { idx, e ->
            packCards.add(
                EssentialCardItem(
                    id = "pack_${pack.id}_$idx",
                    zhSimp = e.simplified,
                    zhTrad = e.traditional.ifBlank { e.simplified },
                    pinyin = e.pinyin,
                    th = e.meaningTh,
                    category = pack.name,
                    exampleZhSimp = e.exampleZhSimp.ifBlank { e.simplified },
                    exampleZhTrad = e.exampleZhTrad.ifBlank { e.traditional.ifBlank { e.simplified } },
                    exampleTh = e.exampleMeaningTh.ifBlank { e.meaningTh }
                )
            )
        }
    }

    val liveCards = mutableListOf<EssentialCardItem>()
    var idx = 1
    zones.filterNot { it.id == CustomVocabPackManager.CUSTOM_ZONE_ID }.forEach { zone ->
        val catLabel = if (zone.id == "zone_favorite") "ทันใจ" else zone.hskLevel.title
        zone.nodes.forEach { node ->
            node.dialogueTurns.forEach { t ->
                val simp = t.hans.trim()
                val trad = t.hant.trim()
                val pin = t.pinyin.trim()
                val th = t.th.trim()
                if (simp.isNotEmpty() && th.isNotEmpty()) {
                    val tradText = if (trad.isNotEmpty()) trad else simp
                    liveCards.add(
                        EssentialCardItem(
                            id = "live_${idx++}",
                            zhSimp = simp,
                            zhTrad = tradText,
                            pinyin = pin,
                            th = th,
                            category = catLabel,
                            exampleZhSimp = simp,
                            exampleZhTrad = tradText,
                            exampleTh = "${node.titleTh} — $th"
                        )
                    )
                }
            }
        }
    }
    val combined = packCards + liveCards + ESSENTIAL_SURVIVAL_CARDS
    return combined.distinctBy { it.zhSimp }
}

private fun splitChineseForPuzzle(text: String): List<String> {
    val clean = text.trim()
        .replace("，", " ")
        .replace("。", " ")
        .replace("？", " ")
        .replace("！", " ")
        .replace(",", " ")
        .replace(".", " ")
        .replace("?", " ")
        .replace("!", " ")
        .replace("、", " ")
        .trim()
    if (clean.isEmpty()) return emptyList()

    val segments = clean.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val rawTokens = mutableListOf<String>()
    segments.forEach { seg ->
        if (seg.length <= 2) {
            rawTokens.add(seg)
        } else if (seg.length <= 5) {
            // Split into individual characters or 2-char chunks so puzzle has >= 2 tiles
            var i = 0
            while (i < seg.length) {
                val end = if (seg.length >= 4 && i + 2 <= seg.length) i + 2 else i + 1
                rawTokens.add(seg.substring(i, end))
                i = end
            }
        } else {
            val step = if (seg.length > 10) 3 else 2
            var i = 0
            while (i < seg.length) {
                val end = (i + step).coerceAtMost(seg.length)
                rawTokens.add(seg.substring(i, end))
                i = end
            }
        }
    }
    return rawTokens.take(7)
}

private fun buildDynamicSentencePuzzles(
    zones: List<QuestZone>,
    customPacks: List<CustomVocabPack>,
    selectedPackId: String?
): List<SentencePuzzle> {
    val packPuzzles = mutableListOf<SentencePuzzle>()
    val targetPacks = if (selectedPackId != null) {
        customPacks.filter { it.id == selectedPackId }
    } else {
        customPacks
    }
    targetPacks.forEach { pack ->
        pack.entries.forEach { e ->
            val srcSimp = e.exampleZhSimp.ifBlank { e.simplified }
            val srcTrad = e.exampleZhTrad.ifBlank { e.traditional.ifBlank { srcSimp } }
            val tokensSimp = splitChineseForPuzzle(srcSimp)
            val tokensTrad = splitChineseForPuzzle(srcTrad).let {
                if (it.size == tokensSimp.size) it else tokensSimp
            }
            if (tokensSimp.size >= 2) {
                val prompt = if (e.exampleMeaningTh.isNotBlank() && e.exampleMeaningTh != e.meaningTh) {
                    "${e.exampleMeaningTh} (${e.meaningTh})"
                } else {
                    e.meaningTh
                }
                packPuzzles.add(
                    SentencePuzzle(
                        thPrompt = prompt,
                        pinyinHint = e.examplePinyin.ifBlank { e.pinyin },
                        targetWordsSimp = tokensSimp,
                        targetWordsTrad = tokensTrad,
                        category = "ไฟล์: ${pack.name}"
                    )
                )
            }
        }
    }

    if (selectedPackId != null && packPuzzles.isNotEmpty()) {
        return packPuzzles
    }

    val zonePuzzles = mutableListOf<SentencePuzzle>()
    zones.filterNot { it.id == CustomVocabPackManager.CUSTOM_ZONE_ID }.forEach { zone ->
        zone.nodes.forEach { node ->
            node.dialogueTurns.take(2).forEach { turn ->
                val tokensSimp = splitChineseForPuzzle(turn.hans)
                val tokensTrad = splitChineseForPuzzle(turn.hant).let {
                    if (it.size == tokensSimp.size) it else tokensSimp
                }
                if (tokensSimp.size in 3..6 && turn.th.isNotBlank()) {
                    zonePuzzles.add(
                        SentencePuzzle(
                            thPrompt = turn.th,
                            pinyinHint = turn.pinyin,
                            targetWordsSimp = tokensSimp,
                            targetWordsTrad = tokensTrad,
                            category = node.titleTh
                        )
                    )
                }
            }
        }
    }

    return (packPuzzles + SENTENCE_PUZZLES + zonePuzzles).distinctBy { it.thPrompt }
}

// =========================================================================
// โหมด 2: เกมการ์ดจับคู่สายฟ้าแลบ (Card Match Battle) - สูงสุด 12 ใบ (6 คู่)
// =========================================================================
@Composable
private fun CardMatchBattleGame(
    questZones: List<QuestZone>,
    customPacks: List<CustomVocabPack>,
    selectedCustomPackId: String?,
    onSelectPackId: (String?) -> Unit,
    scriptMode: ScriptMode,
    onSpeak: (String) -> Unit,
    onReward: (Int, String) -> Unit,
    onMistake: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var roundSeed by remember { mutableIntStateOf(0) }
    val allCards = remember(questZones, customPacks, selectedCustomPackId) {
        buildDynamicCardsFromZones(questZones, customPacks, selectedCustomPackId)
    }
    val pairGoal = minOf(6, allCards.size.coerceAtLeast(1))
    val tiles = remember(roundSeed, scriptMode, allCards) {
        val picked = allCards.shuffled().take(pairGoal)
        val deck = mutableListOf<MatchCardTile>()
        var uid = 1
        picked.forEach { item ->
            val zh = if (scriptMode == ScriptMode.SIMPLIFIED) item.zhSimp else item.zhTrad
            deck.add(MatchCardTile(uid++, item.id, true, zh, item.pinyin, zh))
            deck.add(MatchCardTile(uid++, item.id, false, item.th, "${item.category} • ${item.pinyin}", zh))
        }
        deck.shuffled()
    }

    var selectedTile by remember(roundSeed, selectedCustomPackId) { mutableStateOf<MatchCardTile?>(null) }
    val matchedPairIds = remember(roundSeed, selectedCustomPackId) { mutableStateListOf<String>() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (customPacks.isNotEmpty()) {
            item {
                CustomPackFilterStrip(
                    customPacks = customPacks,
                    selectedCustomPackId = selectedCustomPackId,
                    onSelectPackId = {
                        onSelectPackId(it)
                        roundSeed += 1
                    }
                )
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Text("เกมจับคู่การ์ด ${pairGoal * 2} ใบ ($pairGoal คู่)", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text("จับคู่ตัวอักษรจีน - คำแปลไทย/พินอิน (${matchedPairIds.size}/$pairGoal คู่)", fontSize = 12.sp, color = FrostedSlateLight)
                }
                IconButton(
                    onClick = { roundSeed += 1 },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
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
                            .height(86.dp)
                            .clip(RoundedCornerShape(16.dp))
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
                                    onReward(20, "จับคู่สำเร็จ!")
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isMatched) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = tile.displayMain,
                                    fontSize = if (tile.isChineseSide) 16.sp else 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMatched) Color(0xFF34D399) else Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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

        if (matchedPairIds.size >= pairGoal) {
            item {
                Button(
                    onClick = { roundSeed += 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("เคลียร์กระดานแล้ว! เล่นชุดการ์ดถัดไป", fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
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
    questZones: List<QuestZone>,
    customPacks: List<CustomVocabPack>,
    selectedCustomPackId: String?,
    onSelectPackId: (String?) -> Unit,
    scriptMode: ScriptMode,
    onSpeak: (String) -> Unit,
    onReward: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var cardIndex by remember(selectedCustomPackId) { mutableIntStateOf(0) }
    var isFlipped by remember(cardIndex, selectedCustomPackId) { mutableStateOf(false) }
    val allCards = remember(questZones, customPacks, selectedCustomPackId) {
        buildDynamicCardsFromZones(questZones, customPacks, selectedCustomPackId)
    }

    val card = allCards[cardIndex % allCards.size]
    val zhMain = if (scriptMode == ScriptMode.SIMPLIFIED) card.zhSimp else card.zhTrad
    val zhExample = if (scriptMode == ScriptMode.SIMPLIFIED) card.exampleZhSimp else card.exampleZhTrad

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (customPacks.isNotEmpty()) {
                CustomPackFilterStrip(
                    customPacks = customPacks,
                    selectedCustomPackId = selectedCustomPackId,
                    onSelectPackId = onSelectPackId
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "การ์ดคำศัพท์ & ประโยค (${(cardIndex % allCards.size) + 1}/${allCards.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0x3338BDF8)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = categoryVectorIcon(card.category),
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = card.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            border = BorderStroke(2.dp, if (isFlipped) Color(0xFF10B981) else Color(0xFF818CF8)),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        if (isFlipped) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        else listOf(Color(0xFF312E81), Color(0xFF1E1B4B))
                    )
                )
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
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
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
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF334155)))
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
                        text = "ตัวอย่าง: $zhExample",
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Color(0xFFC7D2FE),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "แตะที่การ์ดเพื่อพลิกดูคำแปล & ตัวอย่างใช้จริง",
                            fontSize = 12.sp,
                            color = Color(0xFFC7D2FE)
                        )
                    }
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
                    val next = allCards[cardIndex % allCards.size]
                    onSpeak(if (scriptMode == ScriptMode.SIMPLIFIED) next.zhSimp else next.zhTrad)
                },
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("ทบทวนอีกที", fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
            }

            Button(
                onClick = {
                    onReward(15, "จำการ์ดแม่นแล้ว!")
                    cardIndex += 1
                    val next = allCards[cardIndex % allCards.size]
                    onSpeak(if (scriptMode == ScriptMode.SIMPLIFIED) next.zhSimp else next.zhTrad)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("จำได้แม่น (+15 XP)", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

// =========================================================================
// โหมด 4: เกมการ์ดเรียงประโยค (Word-Card Sentence Builder)
// =========================================================================
@Composable
private fun SentenceBuilderCardGame(
    questZones: List<QuestZone>,
    customPacks: List<CustomVocabPack>,
    selectedCustomPackId: String?,
    onSelectPackId: (String?) -> Unit,
    scriptMode: ScriptMode,
    onSpeak: (String) -> Unit,
    onReward: (Int, String) -> Unit,
    onMistake: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var puzzleIdx by remember(selectedCustomPackId) { mutableIntStateOf(0) }
    val puzzles = remember(questZones, customPacks, selectedCustomPackId) {
        buildDynamicSentencePuzzles(questZones, customPacks, selectedCustomPackId)
    }
    val puzzle = puzzles[puzzleIdx % puzzles.size]
    val targetWords = if (scriptMode == ScriptMode.SIMPLIFIED) puzzle.targetWordsSimp else puzzle.targetWordsTrad

    val bankWords = remember(puzzleIdx, scriptMode, puzzles) { targetWords.shuffled() }
    val chosenIndices = remember(puzzleIdx, scriptMode, puzzles) { mutableStateListOf<Int>() }

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column {
            if (customPacks.isNotEmpty()) {
                CustomPackFilterStrip(
                    customPacks = customPacks,
                    selectedCustomPackId = selectedCustomPackId,
                    onSelectPackId = onSelectPackId
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF162033),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = categoryVectorIcon(puzzle.category),
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(puzzle.category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("จงเรียงการ์ดเป็นภาษาจีน (${(puzzleIdx % puzzles.size) + 1}/${puzzles.size}):", fontSize = 12.sp, color = FrostedSlateLight)
                    Text(puzzle.thPrompt, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (puzzle.pinyinHint.isNotBlank()) {
                        Text("พินอิน: ${puzzle.pinyinHint}", fontSize = 11.sp, color = Color(0xFF93C5FD))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { chosenIndices.remove(bankIdx) }
                                    ) {
                                        Text(
                                            text = bankWords[bankIdx],
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isUsed) {
                                    onSpeak(bankWords[idx])
                                    chosenIndices.add(idx)
                                }
                        ) {
                            Text(
                                text = bankWords[idx],
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUsed) Color(0xFF334155) else Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
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
                if (assembled == targetWords || fullSentence == targetWords.joinToString("")) {
                    onSpeak(fullSentence)
                    onReward(35, "เรียงประโยคถูกต้องเป๊ะ!")
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
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("ตรวจคำตอบประโยค", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 15.sp)
        }
    }
}
