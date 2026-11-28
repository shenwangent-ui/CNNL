package com.shenhan.app.model

enum class HskLevel(val level: Int, val title: String, val wordCount: Int, val descriptionTh: String) {
    HSK1(1, "HSK 1", 500, "คำศัพท์มาตรฐานใหม่ 500 คำ (พื้นฐานการเอาตัวรอด & สนทนาทั่วไป)"),
    HSK2(2, "HSK 2", 1272, "คำศัพท์สะสม 1,272 คำ (สนทนาชีวิตประจำวัน & การเดินทาง)"),
    HSK3(3, "HSK 3", 2245, "คำศัพท์สะสม 2,245 คำ (สื่อสารคล่องแคล่ว & การทำงาน)"),
    HSK4(4, "HSK 4", 3245, "คำศัพท์สะสม 3,245 คำ (เจรจาธุรกิจ ข่าวสาร & วัฒนธรรม)"),
    HSK5(5, "HSK 5", 4316, "คำศัพท์สะสม 4,316 คำ (อ่านบทความเชิงลึก & สื่อสารระดับมืออาชีพ)"),
    HSK6(6, "HSK 6", 5456, "คำศัพท์สะสม 5,456 คำ (สำนวน สุภาษิต วิชาการ & ระดับเจ้าของภาษา)")
}

enum class SrsMastery(val labelTh: String, val labelEn: String) {
    NEW("คำใหม่", "New"),
    LEARNING("กำลังเรียน", "Learning"),
    REVIEW("ทบทวน", "Review"),
    MASTERED("แม่นยำแล้ว", "Mastered")
}

data class VocabWord(
    val id: String,
    val hskLevel: HskLevel,
    val simplified: String,
    val traditional: String,
    val pinyin: String,
    val toneNumbers: List<Int>,
    val partOfSpeech: String,
    val meaningTh: String,
    val meaningEn: String,
    val exampleSentenceSimp: String,
    val exampleSentenceTrad: String,
    val examplePinyin: String,
    val exampleMeaningTh: String,
    val exampleMeaningEn: String,
    val strokeCount: Int = 0,
    val strokeOrderTipTh: String = "",
    val srsStage: Int = 0, // 0: New, 1: 1 day, 2: 3 days, 3: 7 days, 4: 14 days, 5: 30 days
    val lastReviewedTimestamp: Long = 0L,
    val nextReviewTimestamp: Long = 0L,
    val mastery: SrsMastery = SrsMastery.NEW,
    val isBookmarked: Boolean = false
)
