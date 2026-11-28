package com.shenhan.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenhan.app.data.CustomVocabPack
import com.shenhan.app.data.CustomVocabPackManager
import com.shenhan.app.model.HskLevel
import com.shenhan.app.model.MandarinTone
import com.shenhan.app.model.PitchAnalysisResult
import com.shenhan.app.model.ScriptMode
import com.shenhan.app.model.TonePracticeSyllable
import com.shenhan.app.model.VocabWord
import com.shenhan.app.ui.components.HandwritingCanvas
import com.shenhan.app.ui.components.SrsFlashcardCard
import com.shenhan.app.ui.components.TonePitchVisualizer
import com.shenhan.app.ui.theme.FrostedGlassBorder
import com.shenhan.app.ui.theme.FrostedGlassWhite60
import com.shenhan.app.ui.theme.FrostedGlassWhite80
import com.shenhan.app.ui.theme.FrostedMatchGreenDark
import com.shenhan.app.ui.theme.FrostedPurpleContainer
import com.shenhan.app.ui.theme.FrostedPurplePrimary
import com.shenhan.app.ui.theme.FrostedSlateDark
import com.shenhan.app.ui.theme.FrostedSlateMuted

private sealed interface VocabFilter {
    data object All : VocabFilter
    data object AllCustomPacks : VocabFilter
    data class SpecificPack(val packId: String, val packName: String) : VocabFilter
    data class Hsk(val level: HskLevel) : VocabFilter
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VocabLabScreen(
    toneSyllables: List<TonePracticeSyllable>,
    vocabList: List<VocabWord>,
    scriptMode: ScriptMode,
    audioSpeed: Float,
    isRecording: Boolean,
    liveFrequencyHz: Float,
    liveSpectrum: List<Float>,
    recordedPitchPoints: List<Float>,
    lastPitchResult: PitchAnalysisResult?,
    onPlayNativeAudio: (String) -> Unit,
    onStartPitchRecording: (MandarinTone) -> Unit,
    onStopPitchRecording: () -> Unit,
    onSrsRateWord: (String, Int) -> Unit,
    onToggleBookmark: (String) -> Unit,
    modifier: Modifier = Modifier,
    customPacks: List<CustomVocabPack> = emptyList(),
    onImportVocabFile: (Uri, String?) -> Result<CustomVocabPack> = { _, _ ->
        Result.failure(UnsupportedOperationException("ยังไม่รองรับการนำเข้าไฟล์"))
    },
    onImportVocabText: (String, String) -> Result<CustomVocabPack> = { _, _ ->
        Result.failure(UnsupportedOperationException("ยังไม่รองรับการนำเข้าข้อความ"))
    },
    onDeleteCustomPack: (String) -> Unit = {},
    onLaunchCinemaWithPack: (String, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    var selectedLabTab by remember { mutableIntStateOf(0) }
    val labTabs = listOf(
        Triple("วิเคราะห์วรรณยุกต์ (Tone Pitch)", Icons.Default.GraphicEq, 0),
        Triple("คลังศัพท์ SRS (HSK 1-6)", Icons.Default.Style, 1),
        Triple("ฝึกคัดลายมือ (Stroke)", Icons.Default.Create, 2),
        Triple(
            if (customPacks.isEmpty()) "นำเข้าไฟล์ศัพท์ (Pack)" else "ไฟล์ของฉัน (${customPacks.size})",
            Icons.Default.FolderOpen,
            3
        )
    )

    // Tone Lab state
    var selectedSyllableIndex by remember { mutableIntStateOf(0) }
    val currentSyllable = toneSyllables.getOrElse(selectedSyllableIndex) { toneSyllables.first() }

    // Vocab & Handwriting filter state
    var activeFilter by remember { mutableStateOf<VocabFilter>(VocabFilter.All) }
    var currentFlashcardIndex by remember { mutableIntStateOf(0) }

    // Import Paste Dialog state
    var showPasteDialog by remember { mutableStateOf(false) }
    var pastePackName by remember { mutableStateOf("") }
    var pasteRawText by remember { mutableStateOf("") }

    // Zero-permission Storage Access Framework file picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportVocabFile(uri, null)
                .onSuccess { pack ->
                    activeFilter = VocabFilter.SpecificPack(pack.id, pack.name)
                    currentFlashcardIndex = 0
                    Toast.makeText(
                        context,
                        "นำเข้า \"${pack.name}\" สำเร็จ (${pack.entries.size} คำ) พร้อมใช้คัดลายมือและเกมแล้ว!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .onFailure { err ->
                    Toast.makeText(
                        context,
                        err.localizedMessage ?: "ไม่สามารถอ่านไฟล์คำศัพท์ได้",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    val launchDeviceFilePicker = {
        filePickerLauncher.launch(
            arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "text/tab-separated-values",
                "text/plain",
                "application/json",
                "text/*",
                "*/*"
            )
        )
    }

    val filteredVocab = remember(vocabList, activeFilter) {
        when (val f = activeFilter) {
            VocabFilter.All -> vocabList
            VocabFilter.AllCustomPacks -> vocabList.filter {
                it.id.startsWith(CustomVocabPackManager.CUSTOM_WORD_PREFIX)
            }
            is VocabFilter.SpecificPack -> vocabList.filter {
                CustomVocabPackManager.packIdFromWordId(it.id) == f.packId
            }
            is VocabFilter.Hsk -> vocabList.filter {
                !it.id.startsWith(CustomVocabPackManager.CUSTOM_WORD_PREFIX) && it.hskLevel == f.level
            }
        }
    }
    val safeFiltered = filteredVocab.ifEmpty { vocabList }
    val currentVocabWord = safeFiltered[currentFlashcardIndex % safeFiltered.size.coerceAtLeast(1)]

    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = FrostedPurplePrimary
                    )
                    Text("วางข้อความ / สร้างไฟล์คำศัพท์", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "รองรับ 1 คำต่อบรรทัด เช่น:\nคำจีน, พินอิน, ความหมาย, ประโยคตัวอย่าง, คำแปลประโยค\n(คั่นด้วย , หรือ Tab หรือ | หรือ - และรองรับ JSON)",
                        fontSize = 12.sp,
                        color = FrostedSlateMuted
                    )
                    OutlinedTextField(
                        value = pastePackName,
                        onValueChange = { pastePackName = it },
                        label = { Text("ชื่อชุดคำศัพท์ (Pack Name)") },
                        placeholder = { Text("เช่น ศัพท์ร้านอาหาร, HSK2 บทที่ 1") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_pack_name_input")
                    )
                    OutlinedTextField(
                        value = pasteRawText,
                        onValueChange = { pasteRawText = it },
                        label = { Text("รายการคำศัพท์") },
                        placeholder = { Text("你好,Nǐ hǎo,สวัสดี\n谢谢,Xièxie,ขอบคุณ") },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 130.dp)
                            .testTag("custom_pack_text_input")
                    )
                    TextButton(
                        onClick = {
                            if (pastePackName.isBlank()) pastePackName = "ชุดคำศัพท์ตัวอย่าง"
                            pasteRawText = CustomVocabPackManager.SAMPLE_IMPORT_TEXT
                        },
                        modifier = Modifier.testTag("fill_sample_vocab_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ใส่ตัวอย่างคำศัพท์เพื่อทดลองทันที", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportVocabText(pastePackName, pasteRawText)
                            .onSuccess { pack ->
                                showPasteDialog = false
                                pastePackName = ""
                                pasteRawText = ""
                                activeFilter = VocabFilter.SpecificPack(pack.id, pack.name)
                                currentFlashcardIndex = 0
                                Toast.makeText(
                                    context,
                                    "สร้างชุด \"${pack.name}\" (${pack.entries.size} คำ) สำเร็จ!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .onFailure { err ->
                                Toast.makeText(
                                    context,
                                    err.localizedMessage ?: "รูปแบบคำศัพท์ไม่ถูกต้อง",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FrostedPurplePrimary),
                    modifier = Modifier.testTag("confirm_paste_vocab_button")
                ) {
                    Text("นำเข้าคำศัพท์", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("vocab_lab_screen")
    ) {
        // Prominent Top Vocab File Import & Quick Action Banner
        Surface(
            color = FrostedGlassWhite80,
            border = BorderStroke(1.5.dp, FrostedPurplePrimary.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = FrostedPurplePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "ระบบนำเข้าแพ็กไฟล์คำศัพท์ (Vocab Pack)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = FrostedSlateDark
                            )
                            Text(
                                text = if (customPacks.isEmpty()) "ยังไม่มีไฟล์นำเข้า • กดเลือกไฟล์หรือวางข้อความเพื่อเริ่ม" else "มีไฟล์ของฉันแล้ว ${customPacks.size} ชุด",
                                fontSize = 11.sp,
                                color = FrostedSlateMuted
                            )
                        }
                    }

                    if (customPacks.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = FrostedPurpleContainer
                        ) {
                            Text(
                                text = "${customPacks.sumOf { it.entries.size }} คำ",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FrostedPurplePrimary
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = launchDeviceFilePicker,
                        colors = ButtonDefaults.buttonColors(containerColor = FrostedPurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("top_banner_pick_file_btn")
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("เลือกไฟล์ (.csv/.txt/.json)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { showPasteDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, FrostedPurplePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("top_banner_paste_text_btn")
                    ) {
                        Icon(imageVector = Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(15.dp), tint = FrostedPurplePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("วางข้อความศัพท์", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FrostedPurplePrimary)
                    }
                }
            }
        }

        // Frosted Tab Row
        Surface(
            color = FrostedGlassWhite80,
            modifier = Modifier.fillMaxWidth()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedLabTab,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                contentColor = FrostedPurplePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedLabTab]),
                        color = FrostedPurplePrimary,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                labTabs.forEachIndexed { index, (title, icon, _) ->
                    val isSelected = selectedLabTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedLabTab = index },
                        modifier = Modifier.testTag("lab_tab_$index"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) FrostedPurplePrimary else FrostedSlateMuted
                                )
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) FrostedPurplePrimary else FrostedSlateMuted
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedLabTab) {
            0 -> {
                // SUB-TAB 1: REAL-TIME TONE PITCH ANALYZER
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "เลือกพยางค์ฝึกวรรณยุกต์ 4 เสียง:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FrostedSlateDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(toneSyllables) { index, item ->
                                val isSelected = selectedSyllableIndex == index
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { selectedSyllableIndex = index }
                                        .testTag("tone_chip_${item.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) FrostedPurplePrimary else FrostedGlassWhite60,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) FrostedPurplePrimary else FrostedGlassBorder
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (scriptMode == ScriptMode.SIMPLIFIED) item.hanziSimplified else item.hanziTraditional,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else FrostedPurplePrimary
                                        )
                                        Text(
                                            text = item.syllablePinyin,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else FrostedSlateMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        TonePitchVisualizer(
                            syllable = currentSyllable,
                            scriptMode = scriptMode,
                            isRecording = isRecording,
                            liveFrequencyHz = liveFrequencyHz,
                            liveSpectrum = liveSpectrum,
                            recordedPitchPoints = recordedPitchPoints,
                            lastResult = lastPitchResult,
                            onPlayNativeAudio = {
                                onPlayNativeAudio(if (scriptMode == ScriptMode.SIMPLIFIED) currentSyllable.hanziSimplified else currentSyllable.hanziTraditional)
                            },
                            onStartRecording = {
                                onStartPitchRecording(currentSyllable.tone)
                            },
                            onStopRecording = onStopPitchRecording
                        )
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FrostedGlassWhite60),
                            border = BorderStroke(1.dp, FrostedGlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "ตารางอ้างอิงวรรณยุกต์ภาษาจีนมาตรฐาน (Chao 5-Level Pitch)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = FrostedPurplePrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• เสียง 1 (阴平 55): เสียงสูงคงที่ ลากยาวไม่ตก (เช่น mā 妈)", fontSize = 12.sp, color = Color(0xFFDC2626))
                                Text("• เสียง 2 (阳平 35): เสียงไต่ระดับจากกลางขึ้นสูง (เช่น má 麻)", fontSize = 12.sp, color = Color(0xFF16A34A))
                                Text("• เสียง 3 (上声 214): เสียงกดต่ำลึกก่อนแล้วตวัดยก (เช่น mǎ 马)", fontSize = 12.sp, color = Color(0xFFD97706))
                                Text("• เสียง 4 (去声 51): เสียงทิ้งดิ่งลงเร็วและหนักแน่น (เช่น mà 骂)", fontSize = 12.sp, color = FrostedPurplePrimary)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            1 -> {
                // SUB-TAB 2: SRS FLASHCARDS (INCLUDING IMPORTED FILE PACKS)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        VocabFilterAndImportHeader(
                            totalCount = filteredVocab.size,
                            currentIndex = currentFlashcardIndex,
                            activeFilter = activeFilter,
                            customPacks = customPacks,
                            onSelectFilter = {
                                activeFilter = it
                                currentFlashcardIndex = 0
                            },
                            onOpenFilePicker = launchDeviceFilePicker,
                            onOpenManagePacks = { selectedLabTab = 3 }
                        )
                    }

                    item {
                        SrsFlashcardCard(
                            vocabWord = currentVocabWord,
                            scriptMode = scriptMode,
                            audioSpeed = audioSpeed,
                            onSpeak = { text -> onPlayNativeAudio(text) },
                            onSrsRate = { ease ->
                                onSrsRateWord(currentVocabWord.id, ease)
                                currentFlashcardIndex++
                            },
                            onToggleBookmark = { onToggleBookmark(currentVocabWord.id) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            2 -> {
                // SUB-TAB 3: CHINESE STROKE & PINYIN HANDWRITING CANVAS (SUPPORTS IMPORTED PACKS)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        VocabFilterAndImportHeader(
                            totalCount = filteredVocab.size,
                            currentIndex = currentFlashcardIndex,
                            activeFilter = activeFilter,
                            customPacks = customPacks,
                            onSelectFilter = {
                                activeFilter = it
                                currentFlashcardIndex = 0
                            },
                            onOpenFilePicker = launchDeviceFilePicker,
                            onOpenManagePacks = { selectedLabTab = 3 }
                        )
                    }

                    item {
                        Text(
                            text = "เลือกคำศัพท์เพื่อฝึกคัดลายมือ (${safeFiltered.size} คำ):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FrostedSlateDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(safeFiltered) { idx, word ->
                                val isSelected = currentVocabWord.id == word.id
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { currentFlashcardIndex = idx },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) FrostedPurplePrimary else FrostedGlassWhite60,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) FrostedPurplePrimary else FrostedGlassBorder
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (scriptMode == ScriptMode.SIMPLIFIED) word.simplified else word.traditional,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else FrostedSlateDark
                                        )
                                        if (word.pinyin.isNotBlank() && word.pinyin != word.simplified) {
                                            Text(
                                                text = word.pinyin,
                                                fontSize = 10.sp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.85f) else FrostedSlateMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        HandwritingCanvas(
                            vocabWord = currentVocabWord,
                            scriptMode = scriptMode
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            3 -> {
                // SUB-TAB 4: CUSTOM VOCAB FILE / PACK IMPORTER & QUICK GAME LAUNCHER
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FrostedGlassWhite80),
                            border = BorderStroke(1.dp, FrostedGlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = FrostedPurplePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "นำเข้าไฟล์คำศัพท์ของคุณเอง",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FrostedSlateDark
                                    )
                                }
                                Text(
                                    text = "รองรับไฟล์ .csv, .tsv, .txt และ .json — เมื่อนำเข้าแล้วสามารถใช้ฝึกคัดลายมือ ทบทวนการ์ด SRS เล่นเกมจับคู่ เรียงประโยค และสนทนากับ AI Gemini ได้ครบทุกโหมดทันที",
                                    fontSize = 12.sp,
                                    color = FrostedSlateMuted
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = launchDeviceFilePicker,
                                        colors = ButtonDefaults.buttonColors(containerColor = FrostedPurplePrimary),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("pick_vocab_file_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("เลือกไฟล์จากเครื่อง", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    OutlinedButton(
                                        onClick = { showPasteDialog = true },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, FrostedPurplePrimary),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("paste_vocab_text_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = FrostedPurplePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("วางข้อความ / สร้างชุด", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FrostedPurplePrimary)
                                    }
                                }
                            }
                        }
                    }

                    if (customPacks.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = FrostedGlassWhite60),
                                border = BorderStroke(1.dp, FrostedGlassBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = FrostedPurplePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "ตัวอย่างรูปแบบไฟล์ที่รองรับ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = FrostedSlateDark
                                        )
                                    }
                                    Text(
                                        text = "• CSV / TXT: คำจีน, พินอิน, ความหมาย, ประโยคตัวอย่าง, คำแปล\n• JSON: [{\"zh\":\"你好\",\"pinyin\":\"Nǐ hǎo\",\"th\":\"สวัสดี\"}]\n• หรือใส่เฉพาะคำจีนบรรทัดละ 1 คำเพื่อฝึกคัดลายมือได้ทันที",
                                        fontSize = 12.sp,
                                        color = FrostedSlateMuted
                                    )
                                    Button(
                                        onClick = {
                                            onImportVocabText("ชุดคำศัพท์เริ่มต้น (ตัวอย่าง)", CustomVocabPackManager.SAMPLE_IMPORT_TEXT)
                                                .onSuccess { pack ->
                                                    activeFilter = VocabFilter.SpecificPack(pack.id, pack.name)
                                                    currentFlashcardIndex = 0
                                                    Toast.makeText(context, "โหลดชุดตัวอย่างแล้ว (${pack.entries.size} คำ)", Toast.LENGTH_SHORT).show()
                                                }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = FrostedMatchGreenDark),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("load_sample_pack_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("สร้างชุดคำศัพท์ตัวอย่างเพื่อลองใช้งานทุกฟีเจอร์", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    } else {
                        items(customPacks, key = { it.id }) { pack ->
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = FrostedGlassWhite80),
                                border = BorderStroke(1.dp, FrostedGlassBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_pack_card_${pack.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pack.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FrostedSlateDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${pack.entries.size} คำ • ${pack.entries.take(4).joinToString(" · ") { if (scriptMode == ScriptMode.SIMPLIFIED) it.simplified else it.traditional }}",
                                                fontSize = 12.sp,
                                                color = FrostedSlateMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (activeFilter is VocabFilter.SpecificPack && (activeFilter as VocabFilter.SpecificPack).packId == pack.id) {
                                                    activeFilter = VocabFilter.All
                                                }
                                                onDeleteCustomPack(pack.id)
                                                Toast.makeText(context, "ลบชุด \"${pack.name}\" แล้ว", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("delete_pack_${pack.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "ลบชุดคำศัพท์",
                                                tint = Color(0xFFDC2626)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "เลือกโหมดที่ต้องการฝึกจากชุดคำศัพท์นี้:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FrostedPurplePrimary
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        PackActionChip(
                                            label = "คัดลายมือ",
                                            icon = Icons.Default.Create,
                                            containerColor = FrostedPurplePrimary,
                                            contentColor = Color.White,
                                            testTag = "pack_handwriting_${pack.id}",
                                            onClick = {
                                                activeFilter = VocabFilter.SpecificPack(pack.id, pack.name)
                                                currentFlashcardIndex = 0
                                                selectedLabTab = 2
                                            }
                                        )
                                        PackActionChip(
                                            label = "การ์ดคำ SRS",
                                            icon = Icons.Default.Style,
                                            containerColor = FrostedPurpleContainer,
                                            contentColor = FrostedPurplePrimary,
                                            testTag = "pack_srs_${pack.id}",
                                            onClick = {
                                                activeFilter = VocabFilter.SpecificPack(pack.id, pack.name)
                                                currentFlashcardIndex = 0
                                                selectedLabTab = 1
                                            }
                                        )
                                        PackActionChip(
                                            label = "เกมจับคู่การ์ด",
                                            icon = Icons.Default.Extension,
                                            containerColor = Color(0xFF0284C7),
                                            contentColor = Color.White,
                                            testTag = "pack_match_${pack.id}",
                                            onClick = { onLaunchCinemaWithPack(pack.id, 1) }
                                        )
                                        PackActionChip(
                                            label = "การ์ดจำเป็น",
                                            icon = Icons.Default.Layers,
                                            containerColor = Color(0xFF0D9488),
                                            contentColor = Color.White,
                                            testTag = "pack_cards_${pack.id}",
                                            onClick = { onLaunchCinemaWithPack(pack.id, 2) }
                                        )
                                        PackActionChip(
                                            label = "เรียงประโยค",
                                            icon = Icons.Default.ViewModule,
                                            containerColor = Color(0xFFD97706),
                                            contentColor = Color.White,
                                            testTag = "pack_sentence_${pack.id}",
                                            onClick = { onLaunchCinemaWithPack(pack.id, 3) }
                                        )
                                        PackActionChip(
                                            label = "สนทนา AI",
                                            icon = Icons.Default.AutoAwesome,
                                            containerColor = Color(0xFF1E293B),
                                            contentColor = Color.White,
                                            testTag = "pack_ai_chat_${pack.id}",
                                            onClick = { onLaunchCinemaWithPack(pack.id, 0) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun VocabFilterAndImportHeader(
    totalCount: Int,
    currentIndex: Int,
    activeFilter: VocabFilter,
    customPacks: List<CustomVocabPack>,
    onSelectFilter: (VocabFilter) -> Unit,
    onOpenFilePicker: () -> Unit,
    onOpenManagePacks: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (activeFilter) {
                        VocabFilter.All -> "คำศัพท์ทั้งหมด ($totalCount คำ)"
                        VocabFilter.AllCustomPacks -> "คำศัพท์จากไฟล์ของฉัน ($totalCount คำ)"
                        is VocabFilter.SpecificPack -> "ไฟล์: ${activeFilter.packName} ($totalCount คำ)"
                        is VocabFilter.Hsk -> "${activeFilter.level.title} ($totalCount คำ)"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FrostedSlateDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (totalCount > 0) {
                    Text(
                        text = "คำที่ ${(currentIndex % totalCount.coerceAtLeast(1)) + 1} จาก $totalCount",
                        fontSize = 12.sp,
                        color = FrostedSlateMuted
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FrostedPurpleContainer,
                    border = BorderStroke(1.dp, FrostedPurplePrimary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenFilePicker() }
                        .testTag("quick_import_file_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "นำเข้าไฟล์",
                            tint = FrostedPurplePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "นำเข้าไฟล์",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FrostedPurplePrimary
                        )
                    }
                }

                if (customPacks.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FrostedGlassWhite80,
                        border = BorderStroke(1.dp, FrostedGlassBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenManagePacks() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "จัดการไฟล์",
                                tint = FrostedSlateDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "จัดการ (${customPacks.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FrostedSlateDark
                            )
                        }
                    }
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                val isSelected = activeFilter == VocabFilter.All
                FilterPill(
                    text = "ทั้งหมด",
                    isSelected = isSelected,
                    onClick = { onSelectFilter(VocabFilter.All) }
                )
            }
            if (customPacks.isNotEmpty()) {
                item {
                    val isSelected = activeFilter == VocabFilter.AllCustomPacks
                    FilterPill(
                        text = "ไฟล์ของฉันทั้งหมด",
                        isSelected = isSelected,
                        onClick = { onSelectFilter(VocabFilter.AllCustomPacks) }
                    )
                }
                items(customPacks, key = { it.id }) { pack ->
                    val isSelected = activeFilter is VocabFilter.SpecificPack && activeFilter.packId == pack.id
                    FilterPill(
                        text = "ไฟล์: ${pack.name}",
                        isSelected = isSelected,
                        onClick = { onSelectFilter(VocabFilter.SpecificPack(pack.id, pack.name)) }
                    )
                }
            }
            items(HskLevel.values()) { level ->
                val isSelected = activeFilter is VocabFilter.Hsk && activeFilter.level == level
                FilterPill(
                    text = level.title,
                    isSelected = isSelected,
                    onClick = { onSelectFilter(VocabFilter.Hsk(level)) }
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) FrostedPurplePrimary else FrostedGlassWhite60,
        border = BorderStroke(1.dp, if (isSelected) FrostedPurplePrimary else FrostedGlassBorder)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else FrostedSlateDark
        )
    }
}

@Composable
private fun PackActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
