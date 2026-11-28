package com.shenhan.app
import com.shenhan.app.data.InAppAutoUpdater
import android.widget.Toast
import com.shenhan.app.data.SmartNotificationManager
import com.shenhan.app.data.AppUpdateRelease
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.core.app.ActivityCompat
import android.os.Build
import android.Manifest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import com.shenhan.app.auth.RealAuthManager
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenhan.app.audio.AudioEngine
import com.shenhan.app.data.AppRepository
import com.shenhan.app.data.CinemaGameBridge
import com.shenhan.app.model.AuthProvider
import com.shenhan.app.model.MandarinTone
import com.shenhan.app.model.PlacementTestResult
import com.shenhan.app.model.UserProfile
import com.shenhan.app.ui.components.FrostedCanvasContainer
import com.shenhan.app.ui.components.HumanGatesModal
import com.shenhan.app.ui.components.ShenHanTopAppBar
import com.shenhan.app.ui.components.SubscriptionDialog
import com.shenhan.app.ui.screens.AICinemaScreen
import com.shenhan.app.ui.screens.AuthScreen
import com.shenhan.app.ui.screens.PlacementTestScreen
import com.shenhan.app.ui.screens.ProfileScreen
import com.shenhan.app.ui.screens.QuestMapScreen
import com.shenhan.app.ui.screens.VocabLabScreen
import com.shenhan.app.ui.theme.FrostedGlassBorder
import com.shenhan.app.ui.theme.FrostedGlassWhite80
import com.shenhan.app.ui.theme.FrostedPurpleContainer
import com.shenhan.app.ui.theme.FrostedPurplePrimary
import com.shenhan.app.ui.theme.FrostedSlateMuted
import com.shenhan.app.ui.theme.MyApplicationTheme

enum class MainAppFlow {
    MAIN_NAV,
    PLACEMENT_TEST,
    AUTH
}

class MainActivity : ComponentActivity() {

    private lateinit var audioEngine: AudioEngine
    private lateinit var appRepository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        SmartNotificationManager.createNotificationChannels(this)
        SmartNotificationManager.markStudiedToday(this)
        SmartNotificationManager.scheduleSmartDailyReminder(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !SmartNotificationManager.hasNotificationPermission(this)
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 901)
        }
        RealAuthManager.ensureFirebaseInitialized(this)
        configureAppCheck(this, intent)
        intent?.data?.let { RealAuthManager.pendingDeepLinkUri = it }
        enableEdgeToEdge()

        audioEngine = AudioEngine(this)
        appRepository = AppRepository(this)

        setContent {
            var pendingUpdateRelease by remember { mutableStateOf<AppUpdateRelease?>(null) }
            var showUpdateDialog by remember {
                mutableStateOf(intent?.getBooleanExtra(SmartNotificationManager.EXTRA_OPEN_UPDATE, false) == true)
            }

            LaunchedEffect(Unit) {
                val updatedVer = InAppAutoUpdater.consumeJustUpdatedTag(this@MainActivity)
                if (updatedVer != null || intent?.getBooleanExtra(InAppAutoUpdater.EXTRA_UPDATE_COMPLETED, false) == true) {
                    Toast.makeText(
                        this@MainActivity,
                        "✅ อัปเดต ShenHan ${updatedVer ?: "เวอร์ชันใหม่"} เสร็จสมบูรณ์แล้ว!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                SmartNotificationManager.checkForAppUpdate(this@MainActivity, notifyTopBar = true)
                SmartNotificationManager.checkPromotionsAndNews(this@MainActivity)
            }

            com.shenhan.app.ui.components.InAppUpdateOverlay()

            MyApplicationTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = density.fontScale.coerceAtMost(1.15f))
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                        val d = LocalDensity.current
                        val autoScale = (maxWidth.value / 395f).coerceIn(0.85f, 1.08f)
                        CompositionLocalProvider(
                            LocalDensity provides Density(d.density * autoScale, fontScale = d.fontScale.coerceIn(0.90f, 1.08f))
                        ) {
                ShenHanMainApp(
                    repository = appRepository,
                    audioEngine = audioEngine
                )
                                }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { RealAuthManager.pendingDeepLinkUri = it }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioEngine.shutdown()
    }
}

@Composable
fun ShenHanMainApp(
    repository: AppRepository,
    audioEngine: AudioEngine
) {
    // Repository States
    val currentUser by repository.currentUser.collectAsState()
    val lastPlacementResult by repository.lastPlacementResult.collectAsState()
    val currentLanguage by repository.currentLanguage.collectAsState()
    val scriptMode by repository.scriptMode.collectAsState()
    val subscriptionTier by repository.subscriptionTier.collectAsState()
    val streakDays by repository.streakDays.collectAsState()
    val isCheckedInToday by repository.isCheckedInToday.collectAsState()
    val userXp by repository.userXp.collectAsState()
    val userLevel by repository.userLevel.collectAsState()
    val readinessScore by repository.readinessScore.collectAsState()
    val questZones by repository.questZones.collectAsState()
    val vocabList by repository.vocabList.collectAsState()
    val customPacks by repository.customPacks.collectAsState()
    val toneSyllables by repository.toneSyllables.collectAsState()
    val audioSpeed by repository.audioSpeed.collectAsState()

    // Audio Engine States
    val isPitchRecording by audioEngine.isRecording.collectAsState()
    val isTtsSpeaking by audioEngine.isSpeaking.collectAsState()
    val liveFrequencyHz by audioEngine.currentFrequencyHz.collectAsState()
    val liveSpectrum by audioEngine.audioSpectrum.collectAsState()
    val recordedPitchPoints by audioEngine.recordedPitchPoints.collectAsState()
    val lastPitchResult by audioEngine.lastAnalysisResult.collectAsState()

    LaunchedEffect(questZones) {
        CinemaGameBridge.updateQuestZones(questZones)
    }

    LaunchedEffect(customPacks) {
        CinemaGameBridge.updateCustomPacks(customPacks)
    }

    // App Flow & Dialog States
    var currentFlow by remember { mutableStateOf(MainAppFlow.MAIN_NAV) }
    var tempPlacementResult by remember { mutableStateOf<PlacementTestResult?>(null) }
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var showHumanGatesModal by remember { mutableStateOf(false) }
    var showSubscriptionModal by remember { mutableStateOf(false) }
    var lineAuthError by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(RealAuthManager.pendingDeepLinkUri) {
        val uri = RealAuthManager.pendingDeepLinkUri ?: return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            val activePlacement = tempPlacementResult ?: lastPlacementResult
            val customRes = RealAuthManager.completeFirebaseCustomTokenCallback(
                uri = uri,
                initialHskLevel = activePlacement?.recommendedHskLevel ?: com.shenhan.app.model.HskLevel.HSK1,
                dailyGoalMinutes = activePlacement?.suggestedDailyGoalMinutes ?: 15
            )
            RealAuthManager.pendingDeepLinkUri = null
            if (customRes != null) {
                customRes.onSuccess { user ->
                    repository.loginUser(user)
                    currentFlow = MainAppFlow.MAIN_NAV
                    selectedNavIndex = 3
                }.onFailure { err ->
                    lineAuthError = err.localizedMessage ?: err.toString()
                }
            } else {
                RealAuthManager.parseSupabaseCallbackUri(uri)?.let { user ->
                    repository.loginUser(user)
                    currentFlow = MainAppFlow.MAIN_NAV
                    selectedNavIndex = 3
                }
            }
        }
    }

    val navItems = listOf(
        Triple("แผนที่เควสต์", Icons.Default.Map, "nav_quest_map"),
        Triple("ห้องแล็บคำศัพท์", Icons.Default.Science, "nav_vocab_lab"),
        Triple("AI Cinema", Icons.Default.Movie, "nav_ai_cinema"),
        Triple("โปรไฟล์", Icons.Default.Person, "nav_profile")
    )

    if (lineAuthError != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { lineAuthError = null },
            title = { Text("แจ้งเตือน LINE Login") },
            text = { Text(lineAuthError ?: "") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { lineAuthError = null }) {
                    Text("ตกลง")
                }
            }
        )
    }

    FrostedCanvasContainer {
        when (currentFlow) {
            MainAppFlow.PLACEMENT_TEST -> {
                PlacementTestScreen(
                    scriptMode = scriptMode,
                    onSpeakAudio = { text -> audioEngine.speak(text, audioSpeed) },
                    onFinishAndNavigateToAuth = { result ->
                        tempPlacementResult = result
                        repository.savePlacementTestResult(result)
                        currentFlow = MainAppFlow.AUTH
                    },
                    onFinishAndStartApp = { result ->
                        repository.savePlacementTestResult(result)
                        currentFlow = MainAppFlow.MAIN_NAV
                    },
                    onBack = { currentFlow = MainAppFlow.MAIN_NAV }
                )
            }

            MainAppFlow.AUTH -> {
                
                AuthScreen(
                    placementResult = tempPlacementResult ?: lastPlacementResult,
                    onAuthSuccess = { profile ->
                        repository.loginUser(profile)
                        tempPlacementResult?.let { repository.savePlacementTestResult(it) }
                        currentFlow = MainAppFlow.MAIN_NAV
                    },
                    onContinueAsGuest = {
                        repository.continueAsGuest()
                        tempPlacementResult?.let { repository.savePlacementTestResult(it) }
                        currentFlow = MainAppFlow.MAIN_NAV
                    },
                    onBack = { currentFlow = MainAppFlow.MAIN_NAV }
                )
            }

            MainAppFlow.MAIN_NAV -> {
                Scaffold(
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("shenhan_main_scaffold"),
                    topBar = {
                        ShenHanTopAppBar(
                            scriptMode = scriptMode,
                            streakDays = streakDays,
                            userXp = userXp,
                            subscriptionTier = subscriptionTier,
                            onToggleScript = { repository.toggleScriptMode() },
                            onOpenHumanGates = { showHumanGatesModal = true },
                            onOpenSubscription = { showSubscriptionModal = true }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = FrostedGlassWhite80,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = FrostedGlassBorder,
                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                )
                                .testTag("bottom_navigation_bar")
                        ) {
                            navItems.forEachIndexed { index, (label, icon, testTag) ->
                                val isSelected = selectedNavIndex == index
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedNavIndex = index },
                                    modifier = Modifier.testTag(testTag),
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) FrostedPurplePrimary else FrostedSlateMuted
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = FrostedPurplePrimary,
                                        selectedTextColor = FrostedPurplePrimary,
                                        indicatorColor = FrostedPurpleContainer,
                                        unselectedIconColor = FrostedSlateMuted,
                                        unselectedTextColor = FrostedSlateMuted
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedNavIndex) {
                            0 -> QuestMapScreen(
                                questZones = questZones,
                                scriptMode = scriptMode,
                                streakDays = streakDays,
                                isCheckedInToday = isCheckedInToday,
                                userXp = userXp,
                                userLevel = userLevel,
                                onDailyCheckIn = { repository.performDailyCheckIn() },
                                onCompleteNode = { nodeId, stars -> repository.completeQuestNode(nodeId, stars) },
                                onNavigateToLab = { selectedNavIndex = 1 },
                                onNavigateToCinema = { selectedNavIndex = 2 }
                            )
                            1 -> VocabLabScreen(
                                toneSyllables = toneSyllables,
                                vocabList = vocabList,
                                scriptMode = scriptMode,
                                audioSpeed = audioSpeed,
                                isRecording = isPitchRecording,
                                liveFrequencyHz = liveFrequencyHz,
                                liveSpectrum = liveSpectrum,
                                recordedPitchPoints = recordedPitchPoints,
                                lastPitchResult = lastPitchResult,
                                onPlayNativeAudio = { text -> audioEngine.speak(text, audioSpeed) },
                                onStartPitchRecording = { targetTone ->
                                    audioEngine.startPitchRecording(targetTone)
                                },
                                onStopPitchRecording = {
                                    audioEngine.stopPitchRecording()
                                },
                                onSrsRateWord = { wordId, ease -> repository.updateVocabSrs(wordId, ease) },
                                onToggleBookmark = { wordId -> repository.toggleBookmarkVocab(wordId) },
                                customPacks = customPacks,
                                onImportVocabFile = { uri, name -> repository.importVocabPackFromUri(uri, name) },
                                onImportVocabText = { name, raw -> repository.importVocabPackFromText(name, raw) },
                                onDeleteCustomPack = { packId -> repository.deleteCustomVocabPack(packId) },
                                onLaunchCinemaWithPack = { packId, modeIdx ->
                                    CinemaGameBridge.openCustomPackInCinema(packId, modeIdx)
                                    selectedNavIndex = 2
                                }
                            )
                            2 -> AICinemaScreen(
                                scriptMode = scriptMode,
                                isTtsSpeaking = isTtsSpeaking,
                                isAudioRecording = isPitchRecording,
                                audioSpeed = audioSpeed,
                                onSpeak = { text -> audioEngine.speak(text, audioSpeed) },
                                onChangeAudioSpeed = { speed ->
                                    repository.setAudioSpeed(speed)
                                    audioEngine.setSpeechRate(speed)
                                },
                                onSimulateUserSpeech = { text -> audioEngine.speak(text, audioSpeed) },
                                onAddXp = { amount -> repository.addXp(amount) },
                                onCompleteNode = { nodeId, stars -> repository.completeQuestNode(nodeId, stars) }
                            )
                            3 -> ProfileScreen(
                                currentUser = currentUser,
                                currentLanguage = currentLanguage,
                                scriptMode = scriptMode,
                                subscriptionTier = subscriptionTier,
                                streakDays = streakDays,
                                userXp = userXp,
                                userLevel = userLevel,
                                readinessScore = readinessScore,
                                audioSpeed = audioSpeed,
                                onLanguageChange = { lang -> repository.setLanguage(lang) },
                                onToggleScript = { repository.toggleScriptMode() },
                                onAudioSpeedChange = { speed ->
                                    repository.setAudioSpeed(speed)
                                    audioEngine.setSpeechRate(speed)
                                },
                                onOpenSubscription = { showSubscriptionModal = true },
                                onOpenHumanGates = { showHumanGatesModal = true },
                                onRetakePlacementTest = { currentFlow = MainAppFlow.PLACEMENT_TEST },
                                onSwitchAccount = { repository.logout(); currentFlow = MainAppFlow.AUTH },
                                onLinkThirdParty = { provider -> repository.linkThirdPartyProvider(provider) }
                            )
                        }
                    }

                    // TOR Specifications & Human Gates Dialog
                    if (showHumanGatesModal) {
                        HumanGatesModal(
                            onDismiss = { showHumanGatesModal = false }
                        )
                    }

                    // Subscription Tier Selector Dialog
                    if (showSubscriptionModal) {
                        SubscriptionDialog(
                            currentTier = subscriptionTier,
                            onSelectTier = { tier -> repository.setSubscriptionTier(tier) },
                            onDismiss = { showSubscriptionModal = false }
                        )
                    }
                }
            }
        }
    }
}

private const val APP_CHECK_TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
            }
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                }
            }
        }
    } catch (e: Exception) {
        Log.e(APP_CHECK_TAG, "Failed to initialize Firebase App Check", e)
    }
}



