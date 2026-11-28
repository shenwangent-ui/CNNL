package com.shenhan.app.ui.screens
import com.shenhan.app.data.ProEntitlementManager

import com.shenhan.app.data.CinemaGameBridge

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Train
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenhan.app.data.CustomVocabPackManager
import com.shenhan.app.data.SampleData
import com.shenhan.app.model.HskLevel
import com.shenhan.app.model.QuestNode
import com.shenhan.app.model.QuestZone
import com.shenhan.app.model.QuizQuestion
import com.shenhan.app.model.ScriptMode
import com.shenhan.app.ui.theme.FrostedAmberDark
import com.shenhan.app.ui.theme.FrostedAmberPill
import com.shenhan.app.ui.theme.FrostedGlassBorder
import com.shenhan.app.ui.theme.FrostedGlassWhite40
import com.shenhan.app.ui.theme.FrostedGlassWhite60
import com.shenhan.app.ui.theme.FrostedGlassWhite80
import com.shenhan.app.ui.theme.FrostedMatchGreen
import com.shenhan.app.ui.theme.FrostedMatchGreenContainer
import com.shenhan.app.ui.theme.FrostedMatchGreenDark
import com.shenhan.app.ui.theme.FrostedPitchTarget
import com.shenhan.app.ui.theme.FrostedPurpleContainer
import com.shenhan.app.ui.theme.FrostedPurplePrimary
import com.shenhan.app.ui.theme.FrostedSlateDark
import com.shenhan.app.ui.theme.FrostedSlateMuted
import com.shenhan.app.ui.theme.FrostedSlateLight

@Composable
fun QuestMapScreen(
    questZones: List<QuestZone>,
    scriptMode: ScriptMode,
    streakDays: Int,
    isCheckedInToday: Boolean,
    userXp: Int,
    userLevel: Int,
    onDailyCheckIn: () -> Unit,
    onCompleteNode: (String, Int) -> Unit,
    onNavigateToLab: () -> Unit,
    onNavigateToCinema: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(questZones) {
        CinemaGameBridge.updateQuestZones(questZones)
    }

    var selectedZoneIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    var activeDialogueNode by remember { mutableStateOf<QuestNode?>(null) }
    var dialogueStepIndex by remember { mutableStateOf(0) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = engine?.setLanguage(Locale.TRADITIONAL_CHINESE)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine?.setLanguage(Locale.CHINESE)
                }
            }
        }
        ttsEngine = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    var activeQuizQuestion by remember { mutableStateOf<QuizQuestion?>(null) }
    var activeNodeId by remember { mutableStateOf<String?>(null) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }

    var isProMember by remember { mutableStateOf(ProEntitlementManager.isPro(context)) }
    var selectedLessonNode by remember { mutableStateOf<QuestNode?>(null) }
    var lockedHskTitle by remember { mutableStateOf<String?>(null) }
    val rawZone = questZones.getOrNull(selectedZoneIndex) ?: questZones.firstOrNull() ?: return
    val isZoneHsk4to6 = rawZone.hskLevel in setOf(HskLevel.HSK4, HskLevel.HSK5, HskLevel.HSK6)
    val currentZone = if (!isProMember && isZoneHsk4to6) {
        rawZone.copy(
            isUnlocked = false,
            nodes = rawZone.nodes.map { it.copy(isUnlocked = false) }
        )
    } else {
        rawZone.copy(
            isUnlocked = true,
            nodes = rawZone.nodes.map { it.copy(isUnlocked = true) }
        )
    }

    if (lockedHskTitle != null) {
        AlertDialog(
            onDismissRequest = { lockedHskTitle = null },
            title = {
                Text(
                    text = "👑 ปลดล็อกเนื้อหาระดับสูง (${lockedHskTitle})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "ระดับ HSK 1–3 เปิดให้เรียนฟรีครบทุกด่านแล้วครับ!\n\n" +
                        "สำหรับเนื้อหาระดับสูง HSK 4, HSK 5 และ HSK 6 สงวนสิทธิ์เฉพาะสมาชิก ShenHan Pro เพื่อรับสิทธิ์:\n" +
                        "• 🗺️ ปลดล็อกด่านระดับสูง HSK 4–6 ครบทุกบท\n" +
                        "• 💬 คุยกับผู้ช่วยเฉินไม่จำกัด 24 ชม.\n" +
                        "• 🧪 สร้างชุดคำศัพท์ AI ในห้องแล็บไม่จำกัด"
                )
            },
            confirmButton = {
                Button(onClick = {
                    ProEntitlementManager.setPro(context, true)
                    isProMember = true
                    lockedHskTitle = null
                    Toast.makeText(context, "👑 ปลดล็อก ShenHan Pro และด่าน HSK 4–6 เรียบร้อย!", Toast.LENGTH_LONG).show()
                }) {
                    Text("ปลดล็อก ShenHan Pro")
                }
            },
            dismissButton = {
                TextButton(onClick = { lockedHskTitle = null }) {
                    Text("กลับไปเรียน HSK 1–3")
                }
            }
        )
    }

    val activeLessonNode = selectedLessonNode
    if (activeLessonNode != null) {
        LessonDetailScreen(
            node = activeLessonNode,
            scriptMode = scriptMode,
            onBack = { selectedLessonNode = null },
            onSpeak = { textToSpeak ->
                ttsEngine?.speak(
                    textToSpeak,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "lesson_tts_${System.currentTimeMillis()}"
                )
            },
            onCompleteLesson = { starsEarned ->
                onCompleteNode(activeLessonNode.id, starsEarned)
                selectedLessonNode = null
            }
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("quest_map_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Level & Adventure Progression Hub Card (กล่องเลเวลและสเตตัสการผจญภัย)
            val xpPerLevel = 500
            val currentLevelXp = userXp % xpPerLevel
            val levelProgress = (currentLevelXp.toFloat() / xpPerLevel).coerceIn(0.05f, 1f)
            val levelTitle = when (userLevel) {
                1, 2 -> "ศิษย์สำนักหยกขาว (Novice Scholar)"
                3, 4, 5 -> "จอมยุทธ์คลังศัพท์ (Vocab Disciple)"
                6, 7, 8 -> "ผู้ชำนาญสำเนียงมังกร (Tone Master)"
                else -> "มหาปราชญ์ภาษาจีน (Grand Sage)"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, FrostedGlassBorder, RoundedCornerShape(24.dp))
                    .testTag("daily_streak_banner"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FrostedGlassWhite80),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Top: Level Avatar, Title & Streak Flame
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Level Circle Emblem
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(FrostedPurplePrimary, Color(0xFF6366F1))
                                        )
                                    )
                                    .border(2.dp, FrostedAmberDark, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "LV",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FrostedAmberPill
                                    )
                                    Text(
                                        text = "$userLevel",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = levelTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = FrostedPurpleContainer
                                    ) {
                                        Text(
                                            text = "$userXp XP รวม",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FrostedPurplePrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Daily Streak Flame Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = FrostedAmberPill,
                            border = BorderStroke(1.dp, FrostedAmberDark.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = FrostedAmberDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$streakDays วัน",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = FrostedAmberDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Middle: EXP Level Progress Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ระดับถัดไป (LV.${userLevel + 1})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FrostedSlateLight
                            )
                            Text(
                                text = "$currentLevelXp / $xpPerLevel XP (${(levelProgress * 100).toInt()}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FrostedPurplePrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(levelProgress)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(FrostedPurplePrimary, Color(0xFF818CF8), FrostedMatchGreen)
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom: Daily Check-in Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FrostedGlassWhite40)
                            .border(1.dp, FrostedGlassBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCheckedInToday) Icons.Default.CheckCircle else Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isCheckedInToday) FrostedMatchGreenDark else FrostedAmberDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCheckedInToday) "เช็กอินวันนี้เรียบร้อย (+50 XP)" else "เช็กอินประจำวัน (+50 XP)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = onDailyCheckIn,
                            enabled = !isCheckedInToday,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FrostedPurplePrimary,
                                disabledContainerColor = FrostedMatchGreenContainer,
                                disabledContentColor = FrostedMatchGreenDark
                            )
                        ) {
                            Text(
                                text = if (isCheckedInToday) "รับแล้ว ✓" else "รับโบนัส",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Zone Selector Tabs (HSK 1, HSK 2, HSK 3-4, etc.)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "เลือกโซนแผนที่ (HSK 3.0)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        onClick = {
                            if (isProMember) {
                                ProEntitlementManager.setPro(context, false)
                                isProMember = false
                                Toast.makeText(context, "สลับเป็นโหมดสายฟรี: ล็อก HSK 4–6 แล้ว", Toast.LENGTH_SHORT).show()
                            } else {
                                lockedHskTitle = "HSK 4–6"
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isProMember) Color(0x3310B981) else Color(0x33F59E0B),
                        border = BorderStroke(1.dp, if (isProMember) Color(0xFF10B981) else Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = if (isProMember) "👑 Pro (กดเพื่อเทสต์สายฟรี)" else "🔒 สายฟรี (HSK 4–6 ล็อก)",
                            color = if (isProMember) Color(0xFF6EE7B7) else Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(questZones.size) { index ->
                        val zone = questZones[index]
                        val isSelected = selectedZoneIndex == index
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedZoneIndex = index },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) FrostedPurplePrimary else FrostedGlassBorder
                            ),
                            color = if (isSelected) FrostedPurplePrimary else FrostedGlassWhite60
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isCustomZone = zone.id == CustomVocabPackManager.CUSTOM_ZONE_ID
                                if (zone.id == "zone_favorite" || isCustomZone) {
                                    Icon(
                                        imageVector = if (isCustomZone) Icons.Default.FolderSpecial else Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else if (isCustomZone) FrostedPurplePrimary else FrostedPitchTarget,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = when {
                                        isCustomZone -> "ไฟล์ของฉัน"
                                        zone.id == "zone_favorite" -> "ทันใจ"
                                        else -> zone.hskLevel.title
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else FrostedSlateDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Current Zone Hero Card
        item {
            val totalStarsInZone = currentZone.nodes.size * 3
            val earnedStarsInZone = currentZone.nodes.sumOf { it.starsEarned }
            val completedNodesCount = currentZone.nodes.count { it.starsEarned > 0 }
            val zoneProgress = if (currentZone.nodes.isNotEmpty()) completedNodesCount.toFloat() / currentZone.nodes.size else 0f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, FrostedGlassBorder, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FrostedGlassWhite80),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF312E81),
                                        FrostedPurplePrimary,
                                        Color(0xFF6366F1)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.align(Alignment.BottomStart)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    if (currentZone.id == "zone_favorite") {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = FrostedPitchTarget,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (currentZone.id == "zone_favorite") "หมวดทันใจ • เอาตัวรอดในจีน 100%" else currentZone.hskLevel.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (scriptMode == ScriptMode.SIMPLIFIED) currentZone.titleZhSimp else currentZone.titleZhTrad,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = FrostedPitchTarget,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$earnedStarsInZone / $totalStarsInZone ดาว",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentZone.titleTh,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Text(
                                text = "$completedNodesCount/${currentZone.nodes.size} ด่านสำเร็จ",
                                maxLines = 1,
                                softWrap = false,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FrostedMatchGreenDark
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(zoneProgress)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(FrostedMatchGreenDark)
                            )
                        }
                    }
                }
            }
        }

        // Gamified Node Path
        item {
            Text(
                text = "ด่านในโซนนี้ (Quest Path)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        items(currentZone.nodes) { rawNode ->
            val nodeLockedForFree = !isProMember && isZoneHsk4to6
            val displayNode = rawNode.copy(isUnlocked = !nodeLockedForFree)
            QuestNodeCard(
                node = displayNode,
                scriptMode = scriptMode,
                onNodeClick = {
                    if (nodeLockedForFree) {
                        lockedHskTitle = "${currentZone.hskLevel.title} (${currentZone.hskLevel.wordCount} คำ)"
                    } else {
                        selectedLessonNode = displayNode
                    }
                }
            )
        }
    }

    // Interactive Quiz Question Modal for Boss & Sentence Builder Nodes
    if (activeQuizQuestion != null) {
        val question = activeQuizQuestion!!
        AlertDialog(
            onDismissRequest = { activeQuizQuestion = null },
            modifier = Modifier.testTag("quest_quiz_dialog"),
            containerColor = FrostedGlassWhite80,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Quiz, contentDescription = null, tint = FrostedPurplePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "แบบทดสอบปลดล็อกโซน",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = question.promptTh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FrostedPurpleContainer,
                        border = BorderStroke(1.dp, FrostedPurplePrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (scriptMode == ScriptMode.SIMPLIFIED) question.chineseSimp else question.chineseTrad,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = FrostedPurplePrimary
                            )
                            Text(
                                text = question.pinyin,
                                fontSize = 12.sp,
                                color = FrostedSlateLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option Buttons
                    question.options.forEachIndexed { optIndex, optionText ->
                        val isSelected = selectedAnswerIndex == optIndex
                        val isCorrect = optIndex == question.correctIndex
                        val cardBg = when {
                            !isAnswerSubmitted && isSelected -> FrostedPurpleContainer
                            isAnswerSubmitted && isCorrect -> FrostedMatchGreenContainer
                            isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFFEE2E2)
                            else -> FrostedGlassWhite60
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !isAnswerSubmitted) {
                                    selectedAnswerIndex = optIndex
                                },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) FrostedPurplePrimary else FrostedGlassBorder
                            ),
                            color = cardBg
                        ) {
                            Text(
                                text = optionText,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White
                            )
                        }
                    }

                    // Explanation Feedback
                    if (isAnswerSubmitted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedAnswerIndex == question.correctIndex) FrostedMatchGreenContainer else Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, if (selectedAnswerIndex == question.correctIndex) FrostedMatchGreen.copy(alpha = 0.4f) else Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (selectedAnswerIndex == question.correctIndex) "ถูกต้อง! (+75 XP)" else "ยังไม่ถูกต้อง",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (selectedAnswerIndex == question.correctIndex) FrostedMatchGreenDark else Color(0xFFDC2626)
                                )
                                Text(
                                    text = question.explanationTh,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!isAnswerSubmitted) {
                    Button(
                        onClick = {
                            if (selectedAnswerIndex != null) {
                                isAnswerSubmitted = true
                                if (selectedAnswerIndex == question.correctIndex && activeNodeId != null) {
                                    onCompleteNode(activeNodeId!!, 3)
                                }
                            }
                        },
                        enabled = selectedAnswerIndex != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FrostedPurplePrimary)
                    ) {
                        Text("ตรวจคำตอบ")
                    }
                } else {
                    Button(
                        onClick = { activeQuizQuestion = null },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FrostedPurplePrimary)
                    ) {
                        Text("ปิด")
                    }
                }
            },
            dismissButton = {
                if (!isAnswerSubmitted) {
                    TextButton(onClick = { activeQuizQuestion = null }) {
                        Text("ข้าม", color = FrostedSlateLight)
                    }
                }
            }
        )
    }

    // Audio Dialogue Practice Modal
    if (activeDialogueNode != null) {
        val dNode = activeDialogueNode!!
        val turns = dNode.dialogueTurns
        val currentTurn = turns.getOrNull(dialogueStepIndex)
        if (currentTurn != null) {
            val chineseLine = if (scriptMode == ScriptMode.SIMPLIFIED) currentTurn.hans else currentTurn.hant
            val isNpc = currentTurn.speaker == "npc"
            AlertDialog(
                onDismissRequest = { activeDialogueNode = null },
                containerColor = FrostedGlassWhite80,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isNpc) Icons.Default.Headset else Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = FrostedPurplePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNpc) "ฟังคู่สนทนา (${dialogueStepIndex + 1}/${turns.size})" else "ตาคุณพูดตอบ (${dialogueStepIndex + 1}/${turns.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isNpc) FrostedPurpleContainer else FrostedMatchGreenContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = chineseLine, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = currentTurn.pinyin, fontSize = 13.sp, color = FrostedPurplePrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = currentTurn.th, fontSize = 13.sp, color = FrostedSlateLight)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { ttsEngine?.speak(chineseLine, TextToSpeech.QUEUE_FLUSH, null, "replay_turn") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FrostedGlassWhite80)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ฟังเสียงอ่านภาษาจีนอีกครั้ง", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                activeDialogueNode = null
                                CinemaGameBridge.openNodeInCinema(dNode.id)
                                onNavigateToCinema()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("dialogue_open_cinema_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, FrostedPurplePrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = FrostedPurplePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ส่งด่านนี้เข้าฝึกสดใน AI Cinema", color = FrostedPurplePrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (dialogueStepIndex < turns.lastIndex) {
                                val nextIdx = dialogueStepIndex + 1
                                dialogueStepIndex = nextIdx
                                val nextTurn = turns[nextIdx]
                                val nextText = if (scriptMode == ScriptMode.SIMPLIFIED) nextTurn.hans else nextTurn.hant
                                ttsEngine?.speak(nextText, TextToSpeech.QUEUE_FLUSH, null, "turn_$nextIdx")
                            } else {
                                onCompleteNode(dNode.id, 3)
                                activeDialogueNode = null
                                Toast.makeText(context, "ผ่านบทสนทนาแล้ว! +${dNode.xpReward} XP", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FrostedPurplePrimary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(if (dialogueStepIndex < turns.lastIndex) "ประโยคถัดไป" else "เรียนจบรับ 3 ดาว")
                            Icon(
                                imageVector = if (dialogueStepIndex < turns.lastIndex) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialogueNode = null }) {
                        Text("ปิด", color = FrostedSlateLight)
                    }
                }
            )
        }
    }

}


private fun questNodeVectorIcon(node: QuestNode): ImageVector {
    if (!node.isUnlocked && node.starsEarned == 0) return Icons.Default.Lock
    return when (node.id) {
        "fav_01" -> Icons.Default.FlightTakeoff
        "fav_02" -> Icons.Default.Explore
        "fav_03" -> Icons.Default.MedicalServices
        "fav_04" -> Icons.Default.CreditCard
        "fav_05" -> Icons.Default.Train
        "fav_06" -> Icons.Default.Home
        "fav_07" -> Icons.Default.Favorite
        "fav_08" -> Icons.Default.ShoppingBag
        "fav_09" -> Icons.Default.LocalHospital
        "fav_10" -> Icons.Default.Flight
        else -> when (node.nodeType) {
            com.shenhan.app.model.QuestNodeType.TONE_DRILL -> Icons.Default.GraphicEq
            com.shenhan.app.model.QuestNodeType.VOCAB_SRS -> Icons.Default.Style
            com.shenhan.app.model.QuestNodeType.CINEMA_ROLEPLAY -> Icons.Default.Movie
            com.shenhan.app.model.QuestNodeType.SENTENCE_BUILDER -> Icons.Default.Extension
            com.shenhan.app.model.QuestNodeType.BOSS_CHALLENGE -> Icons.Default.EmojiEvents
        }
    }
}

@Composable
fun QuestNodeCard(
    node: QuestNode,
    scriptMode: ScriptMode,
    onNodeClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenInCinema: () -> Unit = {}
) {
    val (nodeTypeIcon, nodeTypeLabel) = when (node.nodeType) {
        com.shenhan.app.model.QuestNodeType.TONE_DRILL -> Icons.Default.GraphicEq to "วิเคราะห์วรรณยุกต์"
        com.shenhan.app.model.QuestNodeType.VOCAB_SRS -> Icons.Default.Style to "คลังศัพท์ SRS"
        com.shenhan.app.model.QuestNodeType.CINEMA_ROLEPLAY -> Icons.Default.Movie to "ซีนาริโอภาพยนตร์"
        com.shenhan.app.model.QuestNodeType.SENTENCE_BUILDER -> Icons.Default.Extension to "ไวยากรณ์ประโยค"
        com.shenhan.app.model.QuestNodeType.BOSS_CHALLENGE -> Icons.Default.EmojiEvents to "ประลองบอส HSK"
    }

    val isLocked = !node.isUnlocked && node.starsEarned == 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, FrostedGlassBorder, RoundedCornerShape(22.dp))
            .clickable { onNodeClick() }
            .testTag("quest_node_${node.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = FrostedGlassWhite80),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Node Vector Icon Emblem
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            if (node.starsEarned > 0) FrostedMatchGreenContainer else FrostedPurpleContainer
                        )
                        .border(
                            1.5.dp,
                            if (node.starsEarned > 0) FrostedMatchGreen.copy(alpha = 0.5f) else FrostedPurplePrimary.copy(alpha = 0.35f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = questNodeVectorIcon(node),
                        contentDescription = node.titleTh,
                        tint = when {
                            isLocked -> FrostedSlateLight
                            node.starsEarned > 0 -> Color(0xFF4ADE80)
                            else -> FrostedPurplePrimary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = nodeTypeIcon,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = nodeTypeLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (scriptMode == ScriptMode.SIMPLIFIED) node.titleZhSimp else node.titleZhTrad,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = node.titleTh,
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostedSlateLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "+${node.xpReward} XP • ${node.descriptionTh}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = FrostedPurplePrimary
                    )
                }
            }

            // Star Rating, Status & AI Cinema Launch Button
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Row {
                    for (i in 1..3) {
                        Icon(
                            imageVector = if (i <= node.starsEarned) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (i <= node.starsEarned) FrostedPitchTarget else Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (node.starsEarned > 0) FrostedMatchGreenContainer else FrostedPurpleContainer,
                    border = BorderStroke(1.dp, if (node.starsEarned == 3) FrostedMatchGreen.copy(alpha = 0.4f) else FrostedPurplePrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = when { isLocked -> "ล็อกอยู่"; node.starsEarned > 0 -> "ผ่านแล้ว ✓"; else -> "เริ่มลุย" },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when { isLocked -> FrostedSlateLight; node.starsEarned > 0 -> Color(0xFF4ADE80); else -> FrostedPurplePrimary }
                    )
                }
                if (!isLocked) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x2638BDF8),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenInCinema() }
                            .testTag("quest_open_cinema_${node.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color(0xFF7DD3FC),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI Cinema",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7DD3FC)
                            )
                        }
                    }
                }
            }
        }
    }
}

