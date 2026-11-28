package com.shenhan.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PinyinTile(
    val symbol: String,
    val thaiSound: String,
    val speakHanzi: String
)

private data class PinyinGroup(
    val groupTitle: String,
    val tiles: List<PinyinTile>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PinyinCompleteModule(
    onSpeak: (String) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) }
    var lastPlayedInfo by remember { mutableStateOf("แตะที่ตัวอักษรพินอินด้านล่างเพื่อฟังเสียงออกเสียงจริง") }

    val initialGroups = remember {
        listOf(
            PinyinGroup("1. กลุ่มริมฝีปาก (双唇音 / 唇齿音)", listOf(
                PinyinTile("b", "ปัว", "波"), PinyinTile("p", "พัว", "坡"),
                PinyinTile("m", "มัว", "摸"), PinyinTile("f", "ฟัว", "佛")
            )),
            PinyinGroup("2. กลุ่มปลายลิ้น-ปุ่มเหงือก (舌尖中音)", listOf(
                PinyinTile("d", "เตอ", "得"), PinyinTile("t", "เทอ", "特"),
                PinyinTile("n", "เนอ", "讷"), PinyinTile("l", "เลอ", "勒")
            )),
            PinyinGroup("3. กลุ่มโคนลิ้น (舌根音)", listOf(
                PinyinTile("g", "เกอ", "哥"), PinyinTile("k", "เคอ", "科"),
                PinyinTile("h", "เฮอ", "喝")
            )),
            PinyinGroup("4. กลุ่มเพดานแข็ง (舌面音)", listOf(
                PinyinTile("j", "จี", "基"), PinyinTile("q", "ชี", "欺"),
                PinyinTile("x", "ซี", "希")
            )),
            PinyinGroup("5. กลุ่มม้วนลิ้น (翘舌音)", listOf(
                PinyinTile("zh", "จือ(ม้วน)", "知"), PinyinTile("ch", "ชือ(ม้วน)", "吃"),
                PinyinTile("sh", "ซือ(ม้วน)", "诗"), PinyinTile("r", "ยรือ", "日")
            )),
            PinyinGroup("6. กลุ่มปลายลิ้น-ฟัน (平舌音)", listOf(
                PinyinTile("z", "จือ", "资"), PinyinTile("c", "ชือ", "雌"),
                PinyinTile("s", "ซือ", "思")
            )),
            PinyinGroup("7. พยัญชนะกึ่งสระ (零声母)", listOf(
                PinyinTile("y", "อี", "衣"), PinyinTile("w", "อู", "乌")
            ))
        )
    }

    val finalGroups = remember {
        listOf(
            PinyinGroup("1. สระเดี่ยว 6 ตัว (单韵母)", listOf(
                PinyinTile("a", "อา", "啊"), PinyinTile("o", "โอ", "喔"),
                PinyinTile("e", "เออ", "鹅"), PinyinTile("i", "อี", "衣"),
                PinyinTile("u", "อู", "乌"), PinyinTile("ü", "อวี", "迂")
            )),
            PinyinGroup("2. สระผสมหน้า-หลัง (复韵母)", listOf(
                PinyinTile("ai", "ไอ", "哀"), PinyinTile("ei", "เอย", "诶"),
                PinyinTile("ui", "อุย", "威"), PinyinTile("ao", "เอา", "熬"),
                PinyinTile("ou", "โอว", "欧"), PinyinTile("iu", "อิว", "优"),
                PinyinTile("ie", "เอีย", "耶"), PinyinTile("üe", "อเวีย", "约"),
                PinyinTile("er", "เออร์", "儿")
            )),
            PinyinGroup("3. สระนาสิกฐานหน้า (前鼻韵母)", listOf(
                PinyinTile("an", "อัน", "安"), PinyinTile("en", "เอิน", "恩"),
                PinyinTile("in", "อิน", "因"), PinyinTile("un", "อุน", "温"),
                PinyinTile("ün", "อวิน", "晕")
            )),
            PinyinGroup("4. สระนาสิกฐานหลัง & สระควบ (后鼻韵母)", listOf(
                PinyinTile("ang", "อัง", "昂"), PinyinTile("eng", "เอิง", "亨"),
                PinyinTile("ing", "อิง", "英"), PinyinTile("ong", "อง", "轰"),
                PinyinTile("ia", "เอีย(อา)", "呀"), PinyinTile("iao", "เอียว", "腰"),
                PinyinTile("ian", "เอียน", "烟"), PinyinTile("iang", "เอียง", "央"),
                PinyinTile("iong", "อียง", "雍"), PinyinTile("ua", "อวา", "蛙"),
                PinyinTile("uo", "อัว", "窝"), PinyinTile("uai", "อไว", "歪"),
                PinyinTile("uan", "อวาน", "弯"), PinyinTile("uang", "อวาง", "汪"),
                PinyinTile("ueng", "อเวิง", "翁"), PinyinTile("üan", "อวียน", "冤")
            ))
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF2563EB)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10192C))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "🔤 ระบบเสียงพินอินมาตรฐานครบชุด (Pinyin Chart)",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "พยัญชนะ 23 ตัว • สระ 36 ตัว • วรรณยุกต์ 4 เสียง + เสียงเบา",
                color = Color(0xFF7DD3FC),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // แถบสถานะตัวที่กดฟังล่าสุด
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF17253F),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔊 $lastPlayedInfo",
                    color = Color(0xFFFDE68A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ปุ่มสลับหมวด พยัญชนะ / สระ / วรรณยุกต์ & กฎผันเสียง
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    0 to "พยัญชนะ (23)",
                    1 to "สระ (36)",
                    2 to "วรรณยุกต์ & ผันเสียง"
                ).forEach { (idx, label) ->
                    val active = subTab == idx
                    Surface(
                        onClick = { subTab = idx },
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) Color(0xFF2563EB) else Color(0xFF1B263B),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
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

            Spacer(modifier = Modifier.height(12.dp))

            when (subTab) {
                0 -> {
                    initialGroups.forEach { grp ->
                        Text(
                            text = grp.groupTitle,
                            color = Color(0xFF93C5FD),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            grp.tiles.forEach { tile ->
                                Surface(
                                    onClick = {
                                        lastPlayedInfo = "พยัญชนะ '${tile.symbol}' (เทียบเสียงไทย: ${tile.thaiSound})"
                                        onSpeak(tile.speakHanzi)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF182236),
                                    border = BorderStroke(1.dp, Color(0xFF2D3E5E))
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = tile.symbol,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = tile.thaiSound,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    finalGroups.forEach { grp ->
                        Text(
                            text = grp.groupTitle,
                            color = Color(0xFF93C5FD),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            grp.tiles.forEach { tile ->
                                Surface(
                                    onClick = {
                                        lastPlayedInfo = "สระ '${tile.symbol}' (เทียบเสียงไทย: ${tile.thaiSound})"
                                        onSpeak(tile.speakHanzi)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF182236),
                                    border = BorderStroke(1.dp, Color(0xFF2D3E5E))
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = tile.symbol,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = tile.thaiSound,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    val tones = listOf(
                        Triple("เสียง 1 (ˉ) สามัญสูง", "mā • 妈 (แม่)", "妈"),
                        Triple("เสียง 2 (ˊ) จัตวาขึ้นสูง", "má • 麻 (ชา/ป่าน)", "麻"),
                        Triple("เสียง 3 (ˇ) เอกต่ำแล้วขึ้น", "mǎ • 马 (ม้า)", "马"),
                        Triple("เสียง 4 (ˋ) โทตกกระแทก", "mà • 骂 (ด่า)", "骂"),
                        Triple("เสียงเบา (ไม่มีรูป)", "ma • 吗 (ไหม)", "好吗")
                    )
                    tones.forEach { (title, sample, hanzi) ->
                        Surface(
                            onClick = {
                                lastPlayedInfo = "$title → $sample"
                                onSpeak(hanzi)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF182236),
                            border = BorderStroke(1.dp, Color(0xFF2D3E5E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(sample, color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚡ กฎการเปลี่ยนเสียงจริงในชีวิตประจำวัน (Tone Sandhi):",
                        color = Color(0xFFFBBF24),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf(
                        "1. เสียง 3 ชนเสียง 3: ตัวหน้าเปลี่ยนเป็นเสียง 2 เช่น 你好 (nǐ hǎo → ní hǎo)" to "你好",
                        "2. คำว่า 不 (bù): หน้าเสียง 4 เปลี่ยนเป็น bú เช่น 不是 (bú shì), 不要 (bú yào)" to "不是",
                        "3. คำว่า 一 (yī): หน้าเสียง 4 อ่าน yí (一个) / หน้าเสียง 1,2,3 อ่าน yì (一天)" to "一个"
                    ).forEach { (ruleText, speakSample) ->
                        Surface(
                            onClick = {
                                lastPlayedInfo = ruleText
                                onSpeak(speakSample)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF141D2F),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = ruleText,
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
