package com.shenhan.app.data

import android.content.Context
import android.net.Uri
import com.shenhan.app.model.AppLanguage
import com.shenhan.app.model.AuthProvider
import com.shenhan.app.model.HskLevel
import com.shenhan.app.model.HskReadinessScore
import com.shenhan.app.model.MandarinTone
import com.shenhan.app.model.PlacementTestResult
import com.shenhan.app.model.QuestNode
import com.shenhan.app.model.QuestZone
import com.shenhan.app.model.RoleplayFeedback
import com.shenhan.app.model.ScriptMode
import com.shenhan.app.model.SrsMastery
import com.shenhan.app.model.SubscriptionTier
import com.shenhan.app.model.TonePracticeSyllable
import com.shenhan.app.model.UserProfile
import com.shenhan.app.model.VocabWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppRepository(private val context: Context) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val supabaseSync = SupabaseSyncManager()

    
    private val _customPacks = MutableStateFlow(CustomVocabPackManager.loadPacks(context))
    val customPacks: StateFlow<List<CustomVocabPack>> = _customPacks.asStateFlow()

    private fun mergeCustomQuestZones(baseZones: List<QuestZone>, packs: List<CustomVocabPack> = _customPacks.value): List<QuestZone> {
        val withoutCustom = baseZones.filterNot { it.id == CustomVocabPackManager.CUSTOM_ZONE_ID }
        val customZone = CustomVocabPackManager.buildCustomQuestZone(packs)
        return if (customZone != null) {
            listOf(customZone) + withoutCustom
        } else {
            withoutCustom
        }
    }

    private fun mergeCustomVocabWords(
        baseWords: List<VocabWord>,
        packs: List<CustomVocabPack> = _customPacks.value,
        existingWords: List<VocabWord> = emptyList()
    ): List<VocabWord> {
        val existingCustomById = existingWords
            .filter { it.id.startsWith(CustomVocabPackManager.CUSTOM_WORD_PREFIX) }
            .associateBy { it.id }
        val customWords = CustomVocabPackManager.toVocabWords(packs).map { word ->
            val prev = existingCustomById[word.id]
            if (prev != null) {
                word.copy(
                    srsStage = prev.srsStage,
                    mastery = prev.mastery,
                    lastReviewedTimestamp = prev.lastReviewedTimestamp,
                    isBookmarked = prev.isBookmarked
                )
            } else {
                word
            }
        }
        val cleanBase = baseWords.filterNot { it.id.startsWith(CustomVocabPackManager.CUSTOM_WORD_PREFIX) }
        return customWords + cleanBase
    }

    private fun buildZeroQuestZones(): List<QuestZone> {
        val base = SampleData.questZones.mapIndexed { zIdx, zone ->
            zone.copy(
                isUnlocked = (zIdx == 0),
                nodes = zone.nodes.mapIndexed { nIdx, node ->
                    node.copy(
                        starsEarned = 0,
                        isUnlocked = (zone.id == "zone_favorite") || (zIdx == 0 && nIdx == 0),
                        isCurrent = (zIdx == 0 && nIdx == 0)
                    )
                }
            )
        }
        return mergeCustomQuestZones(base)
    }

    private fun buildZeroVocabList(): List<VocabWord> {
        val base = SampleData.vocabList.map { word ->
            word.copy(
                srsStage = 0,
                mastery = SrsMastery.NEW,
                isBookmarked = false
            )
        }
        return mergeCustomVocabWords(base)
    }

    fun resetProgressToZero() {
        _streakDays.value = 0
        _isCheckedInToday.value = false
        _userXp.value = 0
        _userLevel.value = 1
        _readinessScore.value = HskReadinessScore(
            overallScore = 0,
            listeningScore = 0,
            speakingToneScore = 0,
            readingScore = 0,
            vocabMasteryScore = 0,
            targetHskLevel = HskLevel.HSK1,
            readinessPercentage = 0
        )
        _questZones.value = buildZeroQuestZones()
        _vocabList.value = buildZeroVocabList()
    }

    fun syncFromSupabase() {
        repoScope.launch {
            val uid = _currentUser.value?.id
            val baseOnly = _questZones.value.filterNot { it.id == CustomVocabPackManager.CUSTOM_ZONE_ID }
            val (zones, stats, ok) = supabaseSync.fetchZonesAndLessons(baseOnly, uid)
            if (ok) {
                _questZones.value = mergeCustomQuestZones(zones)
            }
            if (stats != null) {
                _userXp.value = maxOf(_userXp.value, stats.xp)
                _userLevel.value = maxOf(_userLevel.value, stats.level)
                _streakDays.value = maxOf(_streakDays.value, stats.streakDays)
            }
        }
    }

    private val profilePrefs by lazy {
        context.getSharedPreferences("shenhan_profile_prefs", Context.MODE_PRIVATE)
    }

    private fun resolveProfileState(base: UserProfile, isFreshLogin: Boolean): UserProfile {
        val uid = base.id
        val savedCustomId = profilePrefs.getString("custom_id_$uid", null) ?: ""
        val savedIdChanged = profilePrefs.getBoolean("id_changed_$uid", false)
        val firebasePhoto = runCatching {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
        }.getOrNull()

        return if (isFreshLogin) {
            val latestPhoto = base.avatarUrl?.takeIf { it.isNotBlank() } ?: firebasePhoto
            profilePrefs.edit()
                .putString("custom_name_$uid", base.displayName)
                .apply {
                    if (!latestPhoto.isNullOrBlank()) putString("avatar_url_$uid", latestPhoto)
                }
                .apply()
            base.copy(
                avatarUrl = latestPhoto,
                customId = savedCustomId.ifBlank { base.customId },
                idChanged = savedIdChanged || base.idChanged
            )
        } else {
            val savedName = profilePrefs.getString("custom_name_$uid", null)?.takeIf { it.isNotBlank() }
            val savedPhoto = profilePrefs.getString("avatar_url_$uid", null)?.takeIf { it.isNotBlank() }
            base.copy(
                displayName = savedName ?: base.displayName,
                avatarUrl = base.avatarUrl?.takeIf { it.isNotBlank() } ?: firebasePhoto ?: savedPhoto,
                customId = savedCustomId.ifBlank { base.customId },
                idChanged = savedIdChanged || base.idChanged
            )
        }
    }

    // User Profile & Authentication State
    private val _currentUser = MutableStateFlow<UserProfile?>(
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.let { fbUser ->
                val isLine = fbUser.uid.startsWith("line_")
                UserProfile(
                    id = fbUser.uid,
                    displayName = fbUser.displayName ?: if (isLine) "LINE Scholar" else fbUser.email?.substringBefore("@") ?: "ผู้เรียน ShenHan",
                    email = fbUser.email ?: if (isLine) "${fbUser.uid}@line.shenhan.app" else "",
                    avatarEmoji = if (isLine) "🟢" else "🌐",
                    provider = if (isLine) AuthProvider.LINE else AuthProvider.GOOGLE,
                    linkedProviders = listOf(if (isLine) AuthProvider.LINE else AuthProvider.GOOGLE),
                    avatarUrl = fbUser.photoUrl?.toString(),
                    startingHskLevel = HskLevel.HSK1,
                    dailyGoalMinutes = 15,
                    isGuest = false
                ).let { resolveProfileState(it, isFreshLogin = false) }
            }
        } catch (_: Exception) {
            null
        }
    )
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // Placement Test State
    private val _hasCompletedPlacement = MutableStateFlow(true)
    val hasCompletedPlacement: StateFlow<Boolean> = _hasCompletedPlacement.asStateFlow()

    private val _lastPlacementResult = MutableStateFlow<PlacementTestResult?>(null)
    val lastPlacementResult: StateFlow<PlacementTestResult?> = _lastPlacementResult.asStateFlow()

    // Language & Script
    private val _currentLanguage = MutableStateFlow(AppLanguage.THAI)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _scriptMode = MutableStateFlow(ScriptMode.SIMPLIFIED)
    val scriptMode: StateFlow<ScriptMode> = _scriptMode.asStateFlow()

    // Subscription
    private val _subscriptionTier = MutableStateFlow(SubscriptionTier.FREE)
    val subscriptionTier: StateFlow<SubscriptionTier> = _subscriptionTier.asStateFlow()

    // User Progress & Gamification
    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    private val _isCheckedInToday = MutableStateFlow(false)
    val isCheckedInToday: StateFlow<Boolean> = _isCheckedInToday.asStateFlow()

    private val _userXp = MutableStateFlow(0)
    val userXp: StateFlow<Int> = _userXp.asStateFlow()

    private val _userLevel = MutableStateFlow(1)
    val userLevel: StateFlow<Int> = _userLevel.asStateFlow()

    // HSK Readiness Radar
    private val _readinessScore = MutableStateFlow(
        HskReadinessScore(
            overallScore = 0,
            listeningScore = 0,
            speakingToneScore = 0,
            readingScore = 0,
            vocabMasteryScore = 0,
            targetHskLevel = HskLevel.HSK3,
            readinessPercentage = 0
        )
    )
    val readinessScore: StateFlow<HskReadinessScore> = _readinessScore.asStateFlow()

    // Quest Zones
    private val _questZones = MutableStateFlow(buildZeroQuestZones())
    val questZones: StateFlow<List<QuestZone>> = _questZones.asStateFlow()

    // Vocab SRS Items
    private val _vocabList = MutableStateFlow(buildZeroVocabList())
    val vocabList: StateFlow<List<VocabWord>> = _vocabList.asStateFlow()

    init {
        if (ProEntitlementManager.isPro(context)) {
            val proTier = SubscriptionTier.values().firstOrNull { it.isPro } ?: SubscriptionTier.FREE
            _subscriptionTier.value = proTier
        }
        CinemaGameBridge.updateCustomPacks(_customPacks.value)
        CinemaGameBridge.updateQuestZones(_questZones.value)
    }

    fun importVocabPackFromUri(uri: Uri, customName: String? = null): Result<CustomVocabPack> {
        val readResult = CustomVocabPackManager.readFileTextAndName(context, uri)
        val (detectedName, rawText) = readResult.getOrElse { return Result.failure(it) }
        val finalName = customName?.takeIf { it.isNotBlank() } ?: detectedName
        return importVocabPackFromText(finalName, rawText)
    }

    fun importVocabPackFromText(packName: String, rawText: String): Result<CustomVocabPack> {
                val isProNow = _subscriptionTier.value.isPro || ProEntitlementManager.isPro(context)
        if (!isProNow && _customPacks.value.size >= ProEntitlementManager.FREE_MAX_CUSTOM_PACKS) {
            return Result.failure(
                IllegalStateException("แพ็กเกจฟรีสร้างชุดคำศัพท์ AI ได้สูงสุด ${ProEntitlementManager.FREE_MAX_CUSTOM_PACKS} ชุด — อัปเกรดเป็น ShenHan Pro เพื่อสร้างไม่จำกัดครับ")
            )
        }
        val parsed = CustomVocabPackManager.parsePackFromText(packName, rawText)
        return parsed.onSuccess { newPack ->
            val updatedPacks = listOf(newPack) + _customPacks.value
            _customPacks.value = updatedPacks
            CustomVocabPackManager.savePacks(context, updatedPacks)
            _vocabList.value = mergeCustomVocabWords(_vocabList.value, updatedPacks, _vocabList.value)
            _questZones.value = mergeCustomQuestZones(_questZones.value, updatedPacks)
            CinemaGameBridge.updateCustomPacks(updatedPacks)
            CinemaGameBridge.updateQuestZones(_questZones.value)
        }
    }

    fun deleteCustomVocabPack(packId: String) {
        val updatedPacks = _customPacks.value.filterNot { it.id == packId }
        _customPacks.value = updatedPacks
        CustomVocabPackManager.savePacks(context, updatedPacks)
        _vocabList.value = mergeCustomVocabWords(_vocabList.value, updatedPacks, _vocabList.value)
        _questZones.value = mergeCustomQuestZones(_questZones.value, updatedPacks)
        CinemaGameBridge.updateCustomPacks(updatedPacks)
        CinemaGameBridge.updateQuestZones(_questZones.value)
    }

    // Tone Practice Syllables
    private val _toneSyllables = MutableStateFlow(SampleData.toneSyllables)
    val toneSyllables: StateFlow<List<TonePracticeSyllable>> = _toneSyllables.asStateFlow()

    // Settings
    private val _audioSpeed = MutableStateFlow(1.0f)
    val audioSpeed: StateFlow<Float> = _audioSpeed.asStateFlow()

    // Methods to manipulate state
    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    fun toggleScriptMode() {
        _scriptMode.value = if (_scriptMode.value == ScriptMode.SIMPLIFIED) {
            ScriptMode.TRADITIONAL
        } else {
            ScriptMode.SIMPLIFIED
        }
    }

    fun setScriptMode(mode: ScriptMode) {
        _scriptMode.value = mode
    }

    fun setSubscriptionTier(tier: SubscriptionTier) {
        _subscriptionTier.value = tier
        ProEntitlementManager.setPro(context, tier.isPro)
    }

    fun setAudioSpeed(speed: Float) {
        _audioSpeed.value = speed.coerceIn(0.75f, 1.25f)
    }

    fun performDailyCheckIn() {
        if (!_isCheckedInToday.value) {
            _isCheckedInToday.value = true
            _streakDays.value += 1
            addXp(50)
        }
    }

    fun addXp(amount: Int) {
        val newXp = _userXp.value + amount
        _userXp.value = newXp
        _userLevel.value = (newXp / 350) + 1
    }

    fun completeQuestNode(nodeId: String, stars: Int) {
        var completedNodeRef: QuestNode? = null
        var unlockedNextNodeRef: QuestNode? = null
        var unlockNextZoneIndex = -1

        val currentZones = _questZones.value.mapIndexed { zIdx, zone ->
            val targetIdx = zone.nodes.indexOfFirst { it.id == nodeId }
            if (targetIdx == -1) {
                if (zIdx == unlockNextZoneIndex) {
                    val firstNode = zone.nodes.firstOrNull()?.copy(isUnlocked = true, isCurrent = true)
                    if (firstNode != null) unlockedNextNodeRef = firstNode
                    zone.copy(
                        isUnlocked = true,
                        nodes = zone.nodes.mapIndexed { nIdx, n -> if (nIdx == 0 && firstNode != null) firstNode else n }
                    )
                } else {
                    zone
                }
            } else {
                if (targetIdx == zone.nodes.lastIndex) {
                    unlockNextZoneIndex = zIdx + 1
                }
                val updatedNodes = zone.nodes.mapIndexed { nIdx, node ->
                    when (nIdx) {
                        targetIdx -> {
                            val updated = node.copy(
                                starsEarned = stars.coerceAtLeast(node.starsEarned),
                                isUnlocked = true,
                                isCurrent = false
                            )
                            completedNodeRef = updated
                            updated
                        }
                        targetIdx + 1 -> {
                            val nextUnlocked = node.copy(
                                isUnlocked = true,
                                isCurrent = node.starsEarned == 0
                            )
                            unlockedNextNodeRef = nextUnlocked
                            nextUnlocked
                        }
                        else -> node
                    }
                }
                zone.copy(isUnlocked = true, nodes = updatedNodes)
            }
        }
        _questZones.value = currentZones
        addXp(50 * stars)

        val currentZoneId = currentZones.firstOrNull { it.isUnlocked }?.id ?: "zone_1"
        repoScope.launch {
            supabaseSync.upsertProfileAndProgress(
                profile = _currentUser.value,
                xp = _userXp.value,
                level = _userLevel.value,
                streakDays = _streakDays.value,
                recommendedZoneId = currentZoneId,
                completedNode = completedNodeRef,
                unlockedNextNode = unlockedNextNodeRef
            )
        }
    }

    fun updateVocabSrs(wordId: String, easeQuality: Int) {
        // easeQuality: 0 = Again, 1 = Hard, 2 = Good, 3 = Easy
        val updated = _vocabList.value.map { word ->
            if (word.id == wordId) {
                val newStage = when (easeQuality) {
                    0 -> 0
                    1 -> (word.srsStage).coerceAtLeast(1)
                    2 -> (word.srsStage + 1).coerceAtMost(5)
                    3 -> (word.srsStage + 2).coerceAtMost(5)
                    else -> word.srsStage
                }
                val newMastery = when (newStage) {
                    0 -> SrsMastery.NEW
                    1, 2 -> SrsMastery.LEARNING
                    3, 4 -> SrsMastery.REVIEW
                    else -> SrsMastery.MASTERED
                }
                word.copy(
                    srsStage = newStage,
                    mastery = newMastery,
                    lastReviewedTimestamp = System.currentTimeMillis()
                )
            } else {
                word
            }
        }
        _vocabList.value = updated
        addXp(15)
    }

    fun toggleBookmarkVocab(wordId: String) {
        _vocabList.value = _vocabList.value.map {
            if (it.id == wordId) it.copy(isBookmarked = !it.isBookmarked) else it
        }
    }

    // Authentication & Profile Actions
    fun loginUser(profile: UserProfile) {
        resetProgressToZero()
        _currentUser.value = resolveProfileState(profile, isFreshLogin = true)
        _hasCompletedPlacement.value = true
        syncFromSupabase()
    }

    fun updateUserProfile(newDisplayName: String, newCustomId: String?): Result<UserProfile> {
        val current = _currentUser.value
            ?: return Result.failure(IllegalStateException("ไม่พบข้อมูลบัญชีผู้ใช้"))
        val cleanName = newDisplayName.trim()
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("กรุณากรอกชื่อที่ต้องการแสดง"))
        }

        var nextCustomId = current.customId
        var nextIdChanged = current.idChanged
        val requestedId = newCustomId?.trim().orEmpty()

        if (requestedId.isNotEmpty() && requestedId != current.effectiveId) {
            if (current.idChanged) {
                return Result.failure(IllegalStateException("คุณใช้สิทธิ์เปลี่ยนไอดีครบ 1 ครั้งแล้ว"))
            }
            if (!requestedId.matches(Regex("^[a-zA-Z0-9._]{4,20}$"))) {
                return Result.failure(IllegalArgumentException("ไอดีต้องมี 4-20 ตัวอักษร (a-z, 0-9, _ หรือ .)"))
            }
            nextCustomId = requestedId
            nextIdChanged = true
        }

        val editor = profilePrefs.edit()
            .putString("custom_name_${current.id}", cleanName)
        if (nextIdChanged) {
            editor.putString("custom_id_${current.id}", nextCustomId)
            editor.putBoolean("id_changed_${current.id}", true)
        }
        editor.apply()

        val updated = current.copy(
            displayName = cleanName,
            customId = nextCustomId,
            idChanged = nextIdChanged
        )
        _currentUser.value = updated

        runCatching {
            val req = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(cleanName)
                .build()
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.updateProfile(req)
        }
        return Result.success(updated)
    }

    fun continueAsGuest() {
        val guest = UserProfile(
            id = "guest_${System.currentTimeMillis()}",
            displayName = "ผู้เยี่ยมชม (Guest)",
            email = "guest@shenhan.local",
            avatarEmoji = "👤",
            provider = AuthProvider.GUEST,
            linkedProviders = listOf(AuthProvider.GUEST),
            startingHskLevel = HskLevel.HSK1,
            dailyGoalMinutes = 10,
            isGuest = true
        )
        _currentUser.value = guest
        _hasCompletedPlacement.value = true
    }

    fun logout() {
        resetProgressToZero()
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
        _userXp.value = 0
        _userLevel.value = 1
        _streakDays.value = 0
    }

    fun linkThirdPartyProvider(provider: AuthProvider) {
        val current = _currentUser.value ?: return
        if (!current.linkedProviders.contains(provider)) {
            val updatedProviders = current.linkedProviders + provider
            _currentUser.value = current.copy(linkedProviders = updatedProviders)
        }
    }

    fun savePlacementTestResult(result: PlacementTestResult) {
        _lastPlacementResult.value = result
        _hasCompletedPlacement.value = true

        // Update user starting HSK level
        val current = _currentUser.value
        if (current != null) {
            _currentUser.value = current.copy(
                startingHskLevel = result.recommendedHskLevel,
                dailyGoalMinutes = result.suggestedDailyGoalMinutes
            )
        }

        // Focus and unlock recommended Quest Zone
        val updatedZones = _questZones.value.map { zone ->
            if (zone.id == result.recommendedZoneId) {
                zone.copy(isUnlocked = true)
            } else {
                zone
            }
        }
        _questZones.value = updatedZones

        // Update readiness score
        _readinessScore.value = _readinessScore.value.copy(
            targetHskLevel = result.recommendedHskLevel,
            readinessPercentage = result.overallScorePercentage.coerceAtLeast(60)
        )
    }

    fun retakePlacementTest() {
        _hasCompletedPlacement.value = false
    }
}

